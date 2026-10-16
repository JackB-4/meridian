package app.meridian;
import android.os.Build;
import android.view.*;

/** Platform feedback respects system haptic settings; never requests override flags. */
public final class Haptics {
    final Prefs prefs;long last;double lastHeading=Double.NaN;int cardinal=-1;boolean onTarget;
    public Haptics(Prefs p){prefs=p;}
    public static int spacing(int setting){return new int[]{5,10,15,30}[Math.max(0,Math.min(3,setting))];}
    public void reset(){lastHeading=Double.NaN;cardinal=-1;onTarget=false;}
    public void fire(View view,String trigger,long now){
        if(view==null||!prefs.on("haptics")||!prefs.on("haptic_"+trigger)||now-last<90)return;
        int soft=Build.VERSION.SDK_INT>=34?HapticFeedbackConstants.SEGMENT_FREQUENT_TICK:HapticFeedbackConstants.CLOCK_TICK;
        int tick=Build.VERSION.SDK_INT>=34?HapticFeedbackConstants.SEGMENT_TICK:HapticFeedbackConstants.CLOCK_TICK;
        int effect=tick;
        if(trigger.equals("drag"))effect=Build.VERSION.SDK_INT>=34?HapticFeedbackConstants.DRAG_START:HapticFeedbackConstants.LONG_PRESS;
        else if(trigger.equals("target"))effect=Build.VERSION.SDK_INT>=30?HapticFeedbackConstants.CONFIRM:HapticFeedbackConstants.LONG_PRESS;
        else if(trigger.equals("cardinal")||trigger.equals("controls"))effect=tick;
        if(prefs.number("haptic_strength")==0)effect=soft;
        view.performHapticFeedback(effect);last=now;
    }
    public void heading(View view,double h,double target,long now){
        if(!Double.isFinite(lastHeading)){lastHeading=h;return;}
        double moved=Math.abs(CompassMath.delta(lastHeading,h));
        if(Double.isFinite(target)){
            double d=Math.abs(CompassMath.delta(h,target));
            if(d<1.5&&!onTarget){fire(view,"target",now);onTarget=true;}
            else if(d>5)onTarget=false;
        }else onTarget=false;
        int q=(int)Math.round(h/90)%4;double distance=Math.abs(CompassMath.delta(h,q*90));
        if(distance<1.5&&cardinal!=q){fire(view,"cardinal",now);cardinal=q;lastHeading=h;}
        else if(distance>6)cardinal=-1;
        int cadence=spacing(prefs.number("haptic_cadence"));
        // Near a target, half-spacing cues aid fine alignment; at most ~6 ticks/s.
        double step=Double.isFinite(target)&&Math.abs(CompassMath.delta(h,target))<15?Math.max(2.5,cadence/2.0):cadence;
        if(moved>=step&&now-last>=160){fire(view,"dial",now);lastHeading=h;}
    }
}
