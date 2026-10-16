package app.meridian;
public final class MathTests {
    static int checks;
    static void close(double expected,double actual,double tolerance) { checks++; if(Math.abs(expected-actual)>tolerance||!Double.isFinite(actual))throw new AssertionError(expected+" != "+actual); }
    static void eq(Object expected,Object actual){checks++;if(!expected.equals(actual))throw new AssertionError(expected+" != "+actual);}
    public static void main(String[] args) {
        close(359,CompassMath.wrap(-1),1e-9);close(0,CompassMath.wrap(720),1e-9);
        close(2,CompassMath.delta(359,1),1e-9);close(-2,CompassMath.delta(1,359),1e-9);
        close(180,Math.abs(CompassMath.delta(0,180)),1e-9);
        double a=359;for(int i=0;i<100;i++)a=CompassMath.smooth(a,1,.02);close(1,a,.01);
        a=1;for(int i=0;i<100;i++)a=CompassMath.smooth(a,359,.02);close(359,a,.01);
        close(123,CompassMath.smooth(Double.NaN,123,.02),1e-9);
        close(40,CompassMath.smooth(40,90,0),1e-9);
        for(int i=0;i<360;i++){close(i,CompassMath.wrap(i+360*5),1e-9);close(0,CompassMath.delta(i,i+360),1e-9);}
        eq("N",CompassMath.cardinal(359));eq("E",CompassMath.cardinal(90));eq("SW",CompassMath.cardinal(225));eq("000°",CompassMath.degrees(359.8));
        close(328.0839895,CompassMath.distance(100,true),1e-6);close(36,CompassMath.speed(10,false),1e-9);close(22.36936292,CompassMath.speed(10,true),1e-9);
        close(0,CompassMath.relativeHeight(1000,1000),1e-9);eq(true,CompassMath.relativeHeight(1000,990)>0);eq(true,CompassMath.relativeHeight(1000,1010)<0);eq(true,Double.isNaN(CompassMath.relativeHeight(0,1000)));
        eq("90° 00′ 00.00″ N",CompassMath.coordinate(89.999999999,true,1));eq("73.50000° W",CompassMath.coordinate(-73.5,false,0));eq("12° 30.000′ N",CompassMath.coordinate(12.5,true,2));
        int[][] axes={{1,2},{2,129},{129,130},{130,1}};for(int i=0;i<4;i++){eq(axes[i][0],CompassMath.axes(i)[0]);eq(axes[i][1],CompassMath.axes(i)[1]);}
        eq(true,CompassMath.fresh(31_000_000_000L,1_000_000_000L));eq(false,CompassMath.fresh(31_000_000_001L,1_000_000_000L));eq(false,CompassMath.fresh(1,2));eq(false,CompassMath.fresh(1,0));
        eq(true,Prefs.defaultOn("elevation"));eq(false,Prefs.defaultOn("pressure"));eq(false,Prefs.defaultOn("trueNorth"));
        eq(10,TileOrder.normalize("speed,speed,unknown").size());eq("speed",TileOrder.normalize("speed,speed,unknown").get(0));
        java.util.List<String> order=TileOrder.normalize("");eq("elevation",TileOrder.move(order,"elevation","coordinates").get(2));eq("coordinates",TileOrder.move(order,"coordinates","elevation").get(0));eq(order,TileOrder.move(order,"unknown","elevation"));
        eq(2,TileOrder.columns(367,1));eq(1,TileOrder.columns(276,1));eq(1,TileOrder.columns(367,1.5f));eq(3,TileOrder.columns(700,1));
        eq(1,RegionalUnits.forCountry("us"));eq(2,RegionalUnits.forCountry("GB"));eq(0,RegionalUnits.forCountry("DE"));eq(0,RegionalUnits.forCountry("LR"));eq(0,RegionalUnits.forCountry("MM"));eq(0,RegionalUnits.forCountry(null));
        eq(5,Haptics.spacing(-1));eq(30,Haptics.spacing(99));eq(10,Haptics.spacing(1));
        System.out.println("PASS: "+checks+" math, coordinate, rotation-map, freshness, grid, regional and preference checks");
    }
}
