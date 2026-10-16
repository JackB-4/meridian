package app.meridian;
import android.content.SharedPreferences;

public final class Prefs {
    public static final String[] READOUTS={"elevation","coordinates","accuracy","speed","course","pressure","relative","level","field","declination"};
    public static final String[] LABELS={"Elevation","Coordinates","Accuracy","Speed","Course","Pressure","Relative elevation","Pitch & roll","Magnetic field","Declination"};
    final SharedPreferences store;
    public Prefs(SharedPreferences store) { this.store=store; }
    public boolean on(String key) { return store.getBoolean(key, defaultOn(key)); }
    public static boolean defaultOn(String key) { return key.startsWith("haptic_")||key.equals("elevation")||key.equals("coordinates")||key.equals("accuracy")||key.equals("dial")||key.equals("degrees")||key.equals("cardinal")||key.equals("status")||key.equals("haptics")||key.equals("controls")||key.equals("location"); }
    public void put(String key, boolean value) { store.edit().putBoolean(key,value).apply(); }
    public int number(String key) { return store.getInt(key,key.equals("haptic_strength")||key.equals("haptic_cadence")?1:0); }
    public void put(String key,int value) { SharedPreferences.Editor e=store.edit().putInt(key,value);if(key.equals("units"))e.putBoolean("units_explicit",true).putBoolean("units_pending",false);e.apply(); }
    public java.util.List<String> order(){return TileOrder.normalize(store.getString("tile_order",""));}
    public void move(String from,String to){store.edit().putString("tile_order",String.join(",",TileOrder.move(order(),from,to))).apply();}
}
