package app.meridian;

import android.app.*;
import android.content.*;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.util.*;

/** Test-only instrumentation; never included in the delivered application. */
public class SmokeInstrumentation extends Instrumentation {
    int checks=0; StringBuilder report=new StringBuilder(); MainActivity activity;String mode="basic";
    void check(boolean result,String name){checks++;if(!result)throw new AssertionError(name);report.append("PASS ").append(name).append('\n');}
    void run(Runnable r){java.util.concurrent.atomic.AtomicReference<Throwable> error=new java.util.concurrent.atomic.AtomicReference<>();runOnMainSync(()->{try{r.run();}catch(Throwable t){error.set(t);}});waitForIdleSync();if(error.get()!=null)throw new AssertionError(error.get());}
    @Override public void onCreate(Bundle args){super.onCreate(args);mode=args==null?"basic":args.getString("mode","basic");start();}
    @Override public void onStart(){
        Bundle result=new Bundle();
        try {
            if(!mode.equals("first"))getTargetContext().getSharedPreferences("meridian",Context.MODE_PRIVATE).edit().putBoolean("permission_prompted",true).commit();
            Intent launch=new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity=(MainActivity)startActivitySync(launch);waitForIdleSync();
            if(mode.equals("drag")){dragFlow();result.putString("stream",report+"TOTAL "+checks+" passed");finish(Activity.RESULT_OK,result);return;}
            if(mode.equals("v12")){v12Flow();result.putString("stream",report+"TOTAL "+checks+" passed");finish(Activity.RESULT_OK,result);return;}
            if(mode.equals("layout")){layoutFlow();result.putString("stream",report+"TOTAL "+checks+" passed");finish(Activity.RESULT_OK,result);return;}
            if(mode.equals("ui")){uiFlow();result.putString("stream",report+"TOTAL "+checks+" passed");finish(Activity.RESULT_OK,result);return;}
            if(!mode.equals("basic")) {run(()->extra());result.putString("stream",report+"TOTAL "+checks+" passed");finish(Activity.RESULT_OK,result);return;}
            run(()->{
                check(activity.root.getWidth()>0,"activity laid out");
                check(!activity.engine.permitted(),"starts without location permission");
                check(activity.cards.get("elevation")[0].getText().toString().equals("—"),"no fabricated altitude without permission");
                check(activity.locationState.getText().toString().contains("permission"),"denied permission clearly labeled");
                check(activity.locationButton.getVisibility()==View.VISIBLE,"location opt-in visible");
                activity.prefs.put("pressure",true); activity.prefs.put("units",1);
                Prefs reread=new Prefs(getTargetContext().getSharedPreferences("meridian",Context.MODE_PRIVATE));
                check(reread.on("pressure")&&reread.number("units")==1,"preferences persisted and read back");
                activity.buildUi();check(activity.cards.containsKey("pressure"),"enabled readout rendered");
                activity.prefs.put("pressure",false);activity.prefs.put("coordinates",false);activity.buildUi();
                check(!activity.cards.containsKey("pressure")&&!activity.cards.containsKey("coordinates"),"disabled readouts removed");
                for(int i=0;i<4;i++){
                    float[] identity={1,0,0,0,1,0,0,0,1},out=new float[9],o=new float[3];int[] axes=CompassMath.axes(i);
                    check(SensorManager.remapCoordinateSystem(identity,axes[0],axes[1],out),"rotation "+i+" maps orthogonal axes");SensorManager.getOrientation(out,o);
                    check(Math.abs(CompassMath.delta(CompassMath.wrap(i*90),CompassMath.wrap(Math.toDegrees(o[0]))))<.01,"rotation "+i+" correct screen azimuth");
                }
                activity.engine.stop();activity.engine.heading=Double.NaN;activity.render();
                check(activity.headingText.getText().toString().equals("—"),"missing heading renders unavailable");
                check(!activity.holdButton.isEnabled(),"cannot hold missing heading");
                activity.prefs.put("trueNorth",true);activity.render();
                check(activity.northText.getText().toString().contains("True pending"),"true north never faked without location");
                activity.prefs.put("location",false);activity.render();
                check(activity.locationButton.getVisibility()==View.GONE,"location opt-in hidden when feature disabled");
                check(activity.locationState.getText().toString().contains("disabled"),"location disabled state");
                activity.prefs.put("theme",2);activity.buildUi();check(!activity.dark,"light theme applied");
                activity.prefs.put("theme",1);activity.buildUi();check(activity.dark,"dark theme applied");
                activity.engine.pressure=1000;activity.engine.reference=1000;activity.engine.pressureTime=SystemClock.elapsedRealtimeNanos();activity.prefs.put("relative",true);activity.buildUi();
                check(activity.cards.get("relative")[0].getText().toString().equals("0 ft"),"barometer reference is explicitly relative");
                activity.engine.pressureTime=1;activity.render();check(activity.cards.get("relative")[0].getText().toString().equals("—"),"stale pressure withheld");
                getTargetContext().getSharedPreferences("meridian",Context.MODE_PRIVATE).edit().clear().commit();
                activity.engine.heading=Double.NaN;activity.engine.pressure=Double.NaN;activity.engine.reference=Double.NaN;
                activity.buildUi();activity.engine.start();
            });
            report.append("TOTAL ").append(checks).append(" passed");result.putString("stream",report.toString());finish(Activity.RESULT_OK,result);
        } catch(Throwable t){result.putString("stream",report+"\nFAIL "+t);finish(Activity.RESULT_CANCELED,result);}
    }
    void extra(){
        if(mode.equals("first")){check(activity.prefs.store.getBoolean("permission_prompted",false),"first permission attempt recorded");return;}
        if(mode.equals("reorderRestore")){check(activity.prefs.order().get(0).equals("speed"),"tile order persists after process death");return;}
        if(mode.equals("general")||mode.equals("readouts")){activity.prefs.store.edit().putInt("theme",mode.equals("general")?2:1).commit();activity.showSettings();if(mode.equals("readouts")){activity.settingsTab=1;activity.buildSettings();}check(activity.settingsOpen,"settings screenshot ready");return;}
        if(mode.equals("save")){activity.prefs.put("theme",1);activity.prefs.put("pressure",true);activity.prefs.put("units",1);activity.prefs.store.edit().putBoolean("persistMarker",true).commit();check(true,"persisted selected settings");return;}
        if(mode.equals("restore")){check(activity.prefs.on("persistMarker")&&activity.prefs.number("theme")==1&&activity.prefs.number("units")==1&&activity.prefs.on("pressure"),"settings survive process death");check(activity.dark&&activity.cards.containsKey("pressure"),"persisted choices applied to UI");return;}
        if(mode.equals("dark")||mode.equals("light")){activity.prefs.store.edit().putInt("theme",mode.equals("dark")?1:2).commit();activity.buildUi();check(activity.dark==mode.equals("dark"),"theme preview ready");return;}
        if(mode.equals("sensors")){check(!activity.engine.hasCompass(),"hardware without compass detected");activity.prefs.put("pressure",true);activity.buildUi();check(activity.statusText.getText().toString().contains("unavailable"),"missing compass visible");check(activity.cards.get("pressure")[1].getText().toString().contains("unavailable"),"missing barometer visible");return;}
        if(mode.equals("location")){
            check(activity.engine.precise(),"precise location permission granted for test");
            activity.engine.stop();activity.engine.active=true;activity.engine.fix=null;
            for(String key:Prefs.READOUTS)activity.prefs.put(key,true);activity.prefs.put("trueNorth",true);activity.prefs.put("units",0);activity.buildUi();
            Location l=new Location("gps");l.setLatitude(38.9);l.setLongitude(-77.0);l.setAltitude(100);l.setAccuracy(4);l.setVerticalAccuracyMeters(8);l.setTime(System.currentTimeMillis());l.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
            // Assign explicit fixtures directly, keeping async conversion out of datum assertions.
            activity.engine.fix=l;activity.engine.updateDeclination();activity.render();
            check(activity.engine.trueReady(),"true north requires fresh location and altitude");
            check(activity.cards.get("elevation")[1].getText().toString().contains("WGS84 ellipsoid"),"unconverted altitude explicitly ellipsoid");
            check(activity.cards.get("elevation")[1].getText().toString().contains("±8 m"),"ellipsoid vertical uncertainty");
            l.setMslAltitudeMeters(132);l.setMslAltitudeAccuracyMeters(9);activity.render();
            check(activity.cards.get("elevation")[0].getText().toString().equals("132 m"),"MSL API altitude selected");
            check(activity.cards.get("elevation")[1].getText().toString().contains("Mean sea level")&&activity.cards.get("elevation")[1].getText().toString().contains("±9 m"),"MSL datum and uncertainty shown");
            l.setSpeed(0);l.setBearing(91);activity.render();check(activity.cards.get("course")[0].getText().toString().equals("—"),"stationary course suppressed");
            l.setSpeed(5);l.setSpeedAccuracyMetersPerSecond(.2f);activity.render();check(activity.cards.get("course")[0].getText().toString().equals("091°"),"moving course separate from compass");
            l.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos()-31_000_000_000L);activity.render();
            check(!activity.engine.trueReady(),"stale fix disallows true north");check(activity.cards.get("elevation")[0].getText().toString().equals("—"),"stale elevation withheld");check(activity.locationState.getText().toString().contains("Stale"),"stale position visibly labeled");
            activity.prefs.store.edit().clear().commit();activity.engine.fix=null;activity.engine.active=false;activity.buildUi();activity.engine.start();return;
        }
        if(mode.equals("coarse")){check(activity.engine.permitted()&&!activity.engine.precise(),"approximate-only permission recognized");Location l=new Location("network");l.setLatitude(38.9);l.setLongitude(-77);l.setAccuracy(1800);l.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());activity.engine.fix=l;activity.render();check(activity.cards.get("coordinates")[1].getText().toString().contains("Approximate"),"approximate position labeled");check(activity.cards.get("elevation")[0].getText().toString().equals("—"),"no altitude synthesized from coarse fix");return;}
        throw new AssertionError("Unknown test mode "+mode);
    }
    void uiFlow(){
        run(()->{activity.showSettings();check(activity.settingsOpen,"settings opens as screen");check(activity.settingsTab==0,"general tab is default");});
        run(()->{check(activity.root.findViewWithTag("theme:2")!=null,"theme choices directly accessible");activity.root.findViewWithTag("theme:2").performClick();check(!activity.dark,"light choice applies immediately");check(activity.bg==android.graphics.Color.WHITE&&activity.ink==android.graphics.Color.BLACK,"light uses pure white and black");});
        run(()->{activity.root.findViewWithTag("theme:1").performClick();check(activity.dark,"dark choice applies immediately");check(activity.bg==android.graphics.Color.BLACK&&activity.ink==android.graphics.Color.WHITE,"dark uses pure black and white");activity.root.findViewWithTag("units:1").performClick();check(activity.prefs.number("units")==1,"units choice persists");});
        run(()->{activity.settingsTab=1;activity.buildSettings();});
        run(()->{check(activity.root.findViewWithTag("field") instanceof Switch,"readouts have individual switches");Switch sw=activity.root.findViewWithTag("field");sw.setChecked(true);check(activity.prefs.on("field"),"readout switch changes preference");activity.settingsScroll.fullScroll(View.FOCUS_DOWN);});
        run(()->{check(activity.settingsScroll.getScrollY()>0,"readouts scroll to lower controls");activity.onBackPressed();check(!activity.settingsOpen,"back returns to compass");check(activity.cards.containsKey("field"),"enabled readout visible on compass");activity.showSettings();});
        run(()->{check(activity.settingsTab==0,"reopen starts on general");check(activity.root.findViewWithTag("units:1").isSelected(),"unit selection restored on reopen");activity.settingsTab=1;activity.buildSettings();});
        run(()->{Switch sw=activity.root.findViewWithTag("field");check(sw.isChecked(),"readout toggle restored on reopen");sw.setChecked(false);activity.closeSettings();check(!activity.cards.containsKey("field"),"disabled readout removed");});
        run(()->{activity.showSettings();activity.root.findViewWithTag("coords:1").performClick();check(activity.prefs.number("coords")==1,"coordinate format changes");activity.root.findViewWithTag("trueNorth:1").performClick();check(activity.prefs.on("trueNorth"),"north reference changes");activity.closeSettings();check(!activity.isFinishing(),"settings back keeps app active");activity.prefs.store.edit().clear().commit();activity.buildUi();});
    }
    void v12Flow(){
        run(()->{
            for(String key:Prefs.READOUTS)activity.prefs.put(key,true);activity.buildUi();
            check(activity.cards.size()==10,"all requested tiles enabled");
            activity.moveTile("speed",-1);activity.moveTile("speed",-1);activity.moveTile("speed",-1);
            check(activity.prefs.order().get(0).equals("speed"),"accessible reorder saves new order");
            check(activity.readouts.getChildAt(0).getTag().equals("speed"),"reorder updates visible grid");
            activity.prefs.put("speed",false);activity.buildUi();check(!activity.cards.containsKey("speed"),"hidden tile leaves no grid hole");
            activity.prefs.put("speed",true);activity.buildUi();check(activity.readouts.getChildAt(0).getTag().equals("speed"),"reenabled tile retains saved position");
            check(activity.prefs.store.getBoolean("permission_prompted",false),"permission decision recorded");
            activity.showSettings();activity.root.findViewWithTag("haptic_settings").performClick();check(activity.settingsTab==2,"haptics page opens");
            ((Switch)activity.root.findViewWithTag("haptic_cardinal")).setChecked(false);check(!activity.prefs.on("haptic_cardinal"),"cardinal feedback individually disabled");
            activity.root.findViewWithTag("haptic_strength:0").performClick();activity.root.findViewWithTag("haptic_cadence:3").performClick();check(activity.prefs.number("haptic_strength")==0&&activity.prefs.number("haptic_cadence")==3,"haptic strength and interval saved");
            activity.settingsBack();check(activity.settingsTab==0&&activity.settingsOpen,"haptics back returns to general");
            activity.root.findViewWithTag("units:2").performClick();check(activity.prefs.number("units")==2&&activity.prefs.store.getBoolean("units_explicit",false),"manual UK units sticky");
            check(!activity.prefs.store.getBoolean("units_pending",true),"manual units stop automatic lookup");activity.closeSettings();
            check(activity.length(100).equals("100 m"),"UK elevation remains metres");
            android.content.SharedPreferences hs=getTargetContext().getSharedPreferences("haptics_test",Context.MODE_PRIVATE);hs.edit().clear().commit();Prefs hp=new Prefs(hs);Haptics feedback=new Haptics(hp);RecordingView rv=new RecordingView(activity);
            feedback.fire(rv,"drag",1000);check(rv.calls==1&&rv.effect==HapticFeedbackConstants.DRAG_START,"platform drag feedback selected");feedback.fire(rv,"drag",1040);check(rv.calls==1,"feedback globally throttled");hp.put("haptics",false);feedback.fire(rv,"drag",1200);check(rv.calls==1,"master haptic switch respected");hp.put("haptics",true);hp.put("haptic_drag",false);feedback.fire(rv,"drag",1400);check(rv.calls==1,"per-trigger haptic switch respected");hp.put("haptic_strength",0);feedback.fire(rv,"controls",1600);check(rv.effect==HapticFeedbackConstants.SEGMENT_FREQUENT_TICK,"light uses platform soft tick");feedback.reset();feedback.heading(rv,350,Double.NaN,2000);int count=rv.calls;feedback.heading(rv,0,Double.NaN,2200);check(rv.calls==count+1,"wrap through north gives one cardinal cue");feedback.heading(rv,.5,Double.NaN,2400);check(rv.calls==count+1,"cardinal hysteresis avoids jitter buzz");feedback.heading(rv,20,20,2600);check(rv.calls==count+2,"bearing-entry cue fires once");hs.edit().clear().commit();
            android.content.SharedPreferences test=getTargetContext().getSharedPreferences("regional_test",Context.MODE_PRIVATE);test.edit().clear().commit();Prefs p=new Prefs(test);RegionalUnits units=new RegionalUnits(getTargetContext(),p,()->{});units.initialize();check(test.getBoolean("units_pending",false),"fresh units start with locale and pending location");int initial=p.number("units");java.util.Locale old=java.util.Locale.getDefault();java.util.Locale.setDefault(java.util.Locale.JAPAN);units.initialize();java.util.Locale.setDefault(old);check(p.number("units")==initial,"locale changes do not flip initialized units");p.put("units",1);units.locate(new Location("test"));check(p.number("units")==1&&!units.started,"manual unit override avoids location lookup");test.edit().clear().putInt("units",0).commit();units.initialize();check(test.getBoolean("units_explicit",false)&&p.number("units")==0,"legacy units preserved on upgrade");test.edit().clear().commit();
            android.content.pm.ShortcutManager shortcuts=activity.getSystemService(android.content.pm.ShortcutManager.class);check(shortcuts.getManifestShortcuts().size()==2,"two legitimate launcher shortcuts installed");
            try{android.graphics.drawable.Drawable icon=activity.getPackageManager().getApplicationIcon("app.meridian");check(icon instanceof android.graphics.drawable.AdaptiveIconDrawable,"launcher icon is adaptive");check(((android.graphics.drawable.AdaptiveIconDrawable)icon).getMonochrome()!=null,"themed monochrome layer available");android.content.pm.ActivityInfo info=activity.getPackageManager().getActivityInfo(activity.getComponentName(),0);check(info.loadLabel(activity.getPackageManager()).toString().contains("Compass & Elevation"),"launcher activity label contains both search terms");}catch(Exception e){throw new AssertionError(e);}
        });
        layoutFlow();
    }
    void layoutFlow(){
        run(()->{for(String key:Prefs.READOUTS)activity.prefs.put(key,true);activity.buildUi();});
        run(()->{check(activity.readouts.getWidth()>0,"grid measured");for(int i=0;i<activity.readouts.getChildCount();i++){View tile=activity.readouts.getChildAt(i);check(tile.getLeft()>=0&&tile.getRight()<=activity.readouts.getWidth()&&tile.getHeight()>=activity.dp(102),"tile "+i+" fits grid bounds");}check(activity.readouts.columns==TileOrder.columns(activity.readouts.getWidth()/activity.getResources().getDisplayMetrics().density,activity.getResources().getConfiguration().fontScale),"grid responds to width and font scale");activity.mainScroll.scrollTo(0,activity.content.getHeight());});
        run(()->{View last=activity.readouts.getChildAt(activity.readouts.getChildCount()-1);android.graphics.Rect rect=new android.graphics.Rect();check(last.getGlobalVisibleRect(rect),"last tile reachable by scrolling");});
    }
    static class RecordingView extends View{int calls,effect;RecordingView(Context c){super(c);}public boolean performHapticFeedback(int value){calls++;effect=value;return true;}}
    void dragFlow(){
        run(()->{activity.prefs.store.edit().putString("tile_order","").commit();for(String key:Prefs.READOUTS)activity.prefs.put(key,true);activity.buildUi();});
        run(()->activity.mainScroll.scrollTo(0,activity.readouts.getTop()));
        final int[] from=new int[2],to=new int[2];
        run(()->{View a=activity.readouts.findViewWithTag("elevation"),b=activity.readouts.findViewWithTag("accuracy");a.getLocationOnScreen(from);b.getLocationOnScreen(to);from[0]+=a.getWidth()/2;from[1]+=a.getHeight()/2;to[0]+=b.getWidth()/2;to[1]+=b.getHeight()/2;});
        long down=SystemClock.uptimeMillis();MotionEvent event=MotionEvent.obtain(down,down,MotionEvent.ACTION_DOWN,from[0],from[1],0);event.setSource(android.view.InputDevice.SOURCE_TOUCHSCREEN);getUiAutomation().injectInputEvent(event,true);event.recycle();SystemClock.sleep(700);
        for(int i=1;i<=12;i++){long now=SystemClock.uptimeMillis();event=MotionEvent.obtain(down,now,MotionEvent.ACTION_MOVE,from[0]+(to[0]-from[0])*i/12f,from[1]+(to[1]-from[1])*i/12f,0);event.setSource(android.view.InputDevice.SOURCE_TOUCHSCREEN);getUiAutomation().injectInputEvent(event,true);event.recycle();SystemClock.sleep(35);}
        event=MotionEvent.obtain(down,SystemClock.uptimeMillis(),MotionEvent.ACTION_UP,to[0],to[1],0);event.setSource(android.view.InputDevice.SOURCE_TOUCHSCREEN);getUiAutomation().injectInputEvent(event,true);event.recycle();SystemClock.sleep(200);waitForIdleSync();
        run(()->{check(activity.prefs.order().get(0).equals("accuracy"),"real long-press drag changes persisted order");check(activity.readouts.getChildAt(0).getTag().equals("accuracy"),"drop rebuilds grid in new order");});
    }
}
