package app.meridian;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.hardware.*;
import android.location.*;
import android.location.altitude.AltitudeConverter;
import android.os.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Main-thread sensor state. No background service; location is held only in memory. */
public final class CompassEngine implements SensorEventListener, LocationListener {
    final Activity context; final Prefs prefs; final Runnable changed;
    final SensorManager sensors; final LocationManager locations;
    final ExecutorService worker=Executors.newSingleThreadExecutor();
    AltitudeConverter altitudeConverter;
    final Handler main=new Handler(Looper.getMainLooper());
    final Sensor orientation, magnetometer, accelerometer, barometer;
    float[] acceleration, magnetic;
    final float[] matrix=new float[9], remapped=new float[9], angles=new float[3];
    public double heading=Double.NaN, pitch=Double.NaN, roll=Double.NaN, field=Double.NaN, pressure=Double.NaN, reference=Double.NaN;
    public float declination=Float.NaN;
    public int accuracy=-1, magneticAccuracy=-1;
    public long headingTime, pressureTime, fieldTime, lastEvent;
    public Location fix;
    public boolean active;
    int generation;
    public String locationProblem="";
    public CompassEngine(Activity c, Prefs p, Runnable callback) {
        context=c; prefs=p; changed=callback;
        sensors=(SensorManager)c.getSystemService(Activity.SENSOR_SERVICE);
        locations=(LocationManager)c.getSystemService(Activity.LOCATION_SERVICE);
        Sensor rv=sensors.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        if(rv==null) rv=sensors.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR);
        orientation=rv;
        magnetometer=sensors.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        accelerometer=sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        barometer=sensors.getDefaultSensor(Sensor.TYPE_PRESSURE);
    }
    public boolean hasCompass() { return orientation!=null || (magnetometer!=null && accelerometer!=null); }
    public boolean precise() { return context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED; }
    public boolean permitted() { return precise() || context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED; }
    public boolean fresh() { return prefs.on("location") && permitted() && locations.isLocationEnabled() && fix!=null && CompassMath.fresh(SystemClock.elapsedRealtimeNanos(),fix.getElapsedRealtimeNanos()); }
    public boolean headingFresh() { return Double.isFinite(heading) && CompassMath.fresh(SystemClock.elapsedRealtimeNanos(),headingTime) && SystemClock.elapsedRealtimeNanos()-headingTime<2_000_000_000L; }
    public boolean trueReady() { return fresh() && Float.isFinite(declination); }
    public double shownHeading() { return CompassMath.wrap(heading+(prefs.on("trueNorth")&&trueReady()?declination:0)); }
    public String source() { return orientation==null?"Accelerometer + magnetometer":orientation.getType()==Sensor.TYPE_ROTATION_VECTOR?"Fused rotation vector":"Geomagnetic rotation vector"; }
    public void start() {
        if(active)return; active=true; generation++;
        heading=Double.NaN; lastEvent=0; accuracy=-1; magneticAccuracy=-1; acceleration=null; magnetic=null;
        if(orientation!=null) sensors.registerListener(this,orientation,20_000);
        else if(accelerometer!=null) sensors.registerListener(this,accelerometer,20_000);
        if(magnetometer!=null) sensors.registerListener(this,magnetometer,40_000);
        if(barometer!=null && (prefs.on("pressure")||prefs.on("relative"))) sensors.registerListener(this,barometer,200_000);
        startLocation();
    }
    public void stop() { active=false; generation++; sensors.unregisterListener(this); locations.removeUpdates(this); }
    public void close() { stop(); worker.shutdownNow(); }
    public void startLocation() {
        locations.removeUpdates(this); locationProblem="";
        if(!active||!prefs.on("location")||!permitted()) { fix=null; declination=Float.NaN; return; }
        if(!locations.isLocationEnabled()) { locationProblem="Location is off"; return; }
        boolean registered=false;
        for(String provider:new String[]{LocationManager.GPS_PROVIDER,LocationManager.NETWORK_PROVIDER}) {
            if(provider.equals(LocationManager.GPS_PROVIDER)&&!precise()) continue;
            try {
                if(locations.isProviderEnabled(provider)) { locations.requestLocationUpdates(provider,1000L,0f,this,Looper.getMainLooper()); registered=true; }
            } catch(SecurityException e) { locationProblem="Location permission changed"; }
              catch(IllegalArgumentException e) { /* Provider absent on this device. */ }
        }
        if(!registered && locationProblem.isEmpty()) locationProblem="No location provider available";
    }
    @Override public void onSensorChanged(SensorEvent e) {
        if(!active)return;
        int type=e.sensor.getType();
        if(type==Sensor.TYPE_PRESSURE) {
            pressure=Double.isFinite(pressure)?pressure*.8+e.values[0]*.2:e.values[0]; pressureTime=e.timestamp; changed.run(); return;
        }
        if(type==Sensor.TYPE_MAGNETIC_FIELD) {
            magnetic=e.values.clone(); magneticAccuracy=e.accuracy;
            field=Math.sqrt(magnetic[0]*magnetic[0]+magnetic[1]*magnetic[1]+magnetic[2]*magnetic[2]); fieldTime=e.timestamp;
            if(orientation!=null) { changed.run(); return; }
        }
        if(type==Sensor.TYPE_ACCELEROMETER) {
            if(acceleration==null) acceleration=e.values.clone();
            else for(int i=0;i<3;i++) acceleration[i]=acceleration[i]*.8f+e.values[i]*.2f;
        }
        if(orientation!=null) {
            if(e.sensor!=orientation)return;
            SensorManager.getRotationMatrixFromVector(matrix,e.values); accuracy=e.accuracy;
        } else {
            if(acceleration==null||magnetic==null||!SensorManager.getRotationMatrix(matrix,null,acceleration,magnetic))return;
            accuracy=magneticAccuracy;
        }
        int rotation=context.getWindowManager().getDefaultDisplay().getRotation();
        int[] axes=CompassMath.axes(rotation);
        SensorManager.remapCoordinateSystem(matrix,axes[0],axes[1],remapped);
        SensorManager.getOrientation(remapped,angles);
        double raw=CompassMath.wrap(Math.toDegrees(angles[0]));
        double dt=lastEvent==0?.02:(e.timestamp-lastEvent)/1e9;
        heading=CompassMath.smooth(heading,raw,dt); lastEvent=e.timestamp; headingTime=e.timestamp;
        pitch=Math.toDegrees(angles[1]); roll=Math.toDegrees(angles[2]);
        changed.run();
    }
    @Override public void onAccuracyChanged(Sensor s,int a) { if(s==orientation)accuracy=a; if(s==magnetometer)magneticAccuracy=a; }
    @Override public void onLocationChanged(Location incoming) {
        if(!active || !prefs.on("location") || !permitted())return;
        if(fix!=null && incoming.getElapsedRealtimeNanos()<fix.getElapsedRealtimeNanos())return;
        // Do not replace a recent precise GNSS fix with a much coarser network estimate.
        if(fix!=null && CompassMath.fresh(SystemClock.elapsedRealtimeNanos(),fix.getElapsedRealtimeNanos()) && incoming.hasAccuracy() && fix.hasAccuracy() && incoming.getAccuracy()>fix.getAccuracy()*2 && incoming.getElapsedRealtimeNanos()-fix.getElapsedRealtimeNanos()<10_000_000_000L)return;
        fix=new Location(incoming); locationProblem=""; updateDeclination(); changed.run();
        if(Build.VERSION.SDK_INT>=34 && fix.hasAltitude() && !fix.hasMslAltitude()) {
            Location copy=new Location(fix); int token=generation;
            worker.execute(()->{
                try { if(altitudeConverter==null)altitudeConverter=new AltitudeConverter();altitudeConverter.addMslAltitudeToLocation(context,copy); }
                catch(Exception ignored) { return; } // Explicit ellipsoid fallback remains visible.
                main.post(()->{ if(active && token==generation && fix!=null && fix.getElapsedRealtimeNanos()==copy.getElapsedRealtimeNanos()) { fix=copy; updateDeclination(); changed.run(); } });
            });
        }
    }
    void updateDeclination() {
        declination=Float.NaN;
        if(fix==null||!fix.hasAltitude())return;
        double alt=Build.VERSION.SDK_INT>=34&&fix.hasMslAltitude()?fix.getMslAltitudeMeters():fix.getAltitude();
        declination=new GeomagneticField((float)fix.getLatitude(),(float)fix.getLongitude(),(float)alt,System.currentTimeMillis()).getDeclination();
    }
    @Override public void onProviderEnabled(String p) { startLocation(); changed.run(); }
    @Override public void onProviderDisabled(String p) { startLocation(); changed.run(); }
    @Override public void onStatusChanged(String p,int s,Bundle b) {}
}
