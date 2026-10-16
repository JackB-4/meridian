package app.meridian;
import android.content.Context;
import android.location.*;
import android.os.*;
import android.telephony.TelephonyManager;
import java.util.*;

/** One initial country resolution, never a travel monitor. No coordinates are persisted. */
public final class RegionalUnits {
    final Context context;final Prefs prefs;final Runnable changed;boolean started;
    public RegionalUnits(Context c,Prefs p,Runnable changed){context=c;prefs=p;this.changed=changed;}
    public static int forCountry(String country){
        String c=country==null?"":country.toUpperCase(Locale.ROOT);
        if(c.equals("GB"))return 2; // metres for elevation / mph for travel
        // CLDR's usage-specific length/speed data uses metric for LR/MM.
        return c.equals("US")?1:0;
    }
    public void initialize(){
        if(prefs.store.contains("units_initialized"))return;
        if(prefs.store.contains("units")){prefs.store.edit().putBoolean("units_initialized",true).putBoolean("units_explicit",true).putBoolean("units_pending",false).apply();return;}
        String country=Locale.getDefault().getCountry();
        prefs.store.edit().putInt("units",forCountry(country)).putBoolean("units_initialized",true).putBoolean("units_pending",true).putString("units_source","Locale · "+country).apply();
    }
    public void locate(Location fix){
        if(started||!prefs.store.getBoolean("units_pending",false)||prefs.store.getBoolean("units_explicit",false))return;
        started=true;
        // Mark before dispatch: no repeated remote lookup after relaunch or a failed provider.
        prefs.store.edit().putBoolean("units_pending",false).apply();
        Handler main=new Handler(Looper.getMainLooper());
        final boolean[] done={false};
        java.util.function.Consumer<String> complete=country->main.post(()->{
            if(done[0])return;done[0]=true;
            if(prefs.store.getBoolean("units_explicit",false))return;
            String c=country;String source="Location";
            if(c==null||c.length()!=2){source="Mobile network";try{TelephonyManager tm=(TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);c=tm==null?"":tm.getNetworkCountryIso();}catch(Exception ignored){c="";}}
            if(c!=null&&c.length()==2){prefs.store.edit().putInt("units",forCountry(c)).putString("units_source",source+" · "+c.toUpperCase(Locale.ROOT)).apply();changed.run();}
        });
        main.postDelayed(()->complete.accept(null),8000);
        if(!Geocoder.isPresent()){complete.accept(null);return;}
        Geocoder geo=new Geocoder(context,Locale.US);
        if(Build.VERSION.SDK_INT>=33){
            try{geo.getFromLocation(fix.getLatitude(),fix.getLongitude(),1,new Geocoder.GeocodeListener(){
                public void onGeocode(List<Address> addresses){complete.accept(addresses.isEmpty()?null:addresses.get(0).getCountryCode());}
                public void onError(String message){complete.accept(null);}
            });}catch(Exception e){complete.accept(null);}
        }else{
            new Thread(()->{try{List<Address> a=geo.getFromLocation(fix.getLatitude(),fix.getLongitude(),1);complete.accept(a==null||a.isEmpty()?null:a.get(0).getCountryCode());}catch(Exception e){complete.accept(null);}},"Meridian-country").start();
        }
    }
}
