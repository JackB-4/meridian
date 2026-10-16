package app.meridian;

import java.util.Locale;

public final class CompassMath {
    private CompassMath() {}
    public static double wrap(double x) { return ((x % 360) + 360) % 360; }
    public static double delta(double from, double to) { return wrap(to - from + 180) - 180; }
    public static double smooth(double from, double to, double dt) {
        if (!Double.isFinite(from)) return wrap(to);
        double d = delta(from, to);
        double tau = Math.abs(d) > 8 ? 0.055 : 0.14;
        return wrap(from + d * (1 - Math.exp(-Math.max(0, Math.min(dt, 0.5)) / tau)));
    }
    public static String cardinal(double angle) {
        return new String[]{"N","NNE","NE","ENE","E","ESE","SE","SSE","S","SSW","SW","WSW","W","WNW","NW","NNW"}[(int)Math.floor((wrap(angle)+11.25)/22.5)%16];
    }
    public static String degrees(double angle) { return String.format(Locale.US, "%03d°", Math.round(wrap(angle)) % 360); }
    public static double distance(double meters, boolean imperial) { return meters * (imperial ? 3.280839895 : 1); }
    public static double speed(double mps, boolean imperial) { return mps * (imperial ? 2.236936292 : 3.6); }
    public static double relativeHeight(double reference, double pressure) {
        return reference > 0 && pressure > 0 ? 44330 * (1-Math.pow(pressure/reference, 0.190294957)) : Double.NaN;
    }
    public static boolean fresh(long nowNanos, long fixNanos) { return fixNanos > 0 && nowNanos >= fixNanos && nowNanos-fixNanos <= 30_000_000_000L; }
    // Android SensorManager AXIS constants: X=1, Y=2, minus flag=128.
    public static int[] axes(int rotation) {
        switch(rotation) { case 1:return new int[]{2,129}; case 2:return new int[]{129,130}; case 3:return new int[]{130,1}; default:return new int[]{1,2}; }
    }
    public static String coordinate(double x, boolean latitude, int format) {
        String hemi = latitude ? (x<0?"S":"N") : (x<0?"W":"E");
        double a=Math.abs(x);
        if(format==1) {
            long hundredths=Math.round(a*3600*100); long deg=hundredths/360000, min=(hundredths/6000)%60;
            return String.format(Locale.US,"%d° %02d′ %05.2f″ %s",deg,min,(hundredths%6000)/100.0,hemi);
        }
        if(format==2) {
            long thousandths=Math.round(a*60*1000);
            return String.format(Locale.US,"%d° %06.3f′ %s",thousandths/60000,(thousandths%60000)/1000.0,hemi);
        }
        return String.format(Locale.US,"%.5f° %s",a,hemi);
    }
}
