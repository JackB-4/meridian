package app.meridian;

import android.content.Context;
import android.graphics.*;
import android.view.View;

/** Decorative dial; the adjacent native text readout exposes heading to accessibility. */
public final class DialView extends View {
    final Paint paint=new Paint(3);
    double heading=Double.NaN,target=Double.NaN,pitch=0,roll=0;
    double visualHeading=Double.NaN;long frameTime;
    boolean level;
    int ink,muted,line,accent,north,surface;
    public DialView(Context c) { super(c); setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO); }
    @Override protected void onMeasure(int w,int h) { int size=Math.min(MeasureSpec.getSize(w),(int)(254*getResources().getDisplayMetrics().density)); setMeasuredDimension(MeasureSpec.getSize(w),size); }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        long now=android.os.SystemClock.elapsedRealtimeNanos();
        if(!Double.isFinite(heading))visualHeading=Double.NaN;
        else if(!Double.isFinite(visualHeading))visualHeading=heading;
        else {double dt=frameTime==0?.016:Math.max(0,Math.min(.1,(now-frameTime)/1e9));double delta=CompassMath.delta(visualHeading,heading);visualHeading=Math.abs(delta)<.015?heading:CompassMath.wrap(visualHeading+delta*(1-Math.exp(-dt/.022)));if(Math.abs(CompassMath.delta(visualHeading,heading))>.015)postInvalidateOnAnimation();}
        frameTime=now;
        float unit=Math.min(getWidth(),getHeight())/340f;
        canvas.save(); canvas.translate(getWidth()/2f,getHeight()/2f); canvas.scale(unit,unit);
        paint.setStyle(Paint.Style.FILL); paint.setColor(surface); canvas.drawCircle(0,0,149,paint);
        paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(1); paint.setColor(line); canvas.drawCircle(0,0,149,paint);
        canvas.save(); canvas.rotate((float)-(Double.isFinite(visualHeading)?visualHeading:0));
        for(int i=0;i<120;i++) {
            int deg=i*3; paint.setColor(deg==0?north:(i%10==0?ink:line)); paint.setStrokeWidth(i%10==0?2:1);
            canvas.drawLine(0,-141,0,i%10==0?-124:(i%5==0?-129:-135),paint); canvas.rotate(3);
        }
        paint.setStyle(Paint.Style.FILL); paint.setTextAlign(Paint.Align.CENTER); paint.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        for(int i=0;i<4;i++) { canvas.save(); canvas.rotate(i*90); canvas.translate(0,-111); canvas.rotate((float)(Double.isFinite(visualHeading)?visualHeading:0)-i*90); paint.setTextSize(16); paint.setColor(i==0?north:ink); canvas.drawText(new String[]{"N","E","S","W"}[i],0,6,paint); canvas.restore(); }
        if(Double.isFinite(target)) { canvas.save(); canvas.rotate((float)target); paint.setColor(accent); Path t=new Path();t.moveTo(0,-121);t.lineTo(-5,-114);t.lineTo(5,-114);t.close();canvas.drawPath(t,paint);canvas.restore(); }
        canvas.restore();
        paint.setColor(north); Path pointer=new Path();pointer.moveTo(0,-164);pointer.lineTo(-5,-154);pointer.lineTo(5,-154);pointer.close();canvas.drawPath(pointer,paint);
        paint.setColor(line); paint.setStrokeWidth(1);canvas.drawLine(-16,0,16,0,paint);canvas.drawLine(0,-16,0,16,paint);
        paint.setStyle(Paint.Style.STROKE);if(level)canvas.drawCircle(0,0,39,paint);
        if(level && Double.isFinite(pitch)&&Double.isFinite(roll)) {
            float x=(float)Math.max(-32,Math.min(32,roll*1.7)),y=(float)Math.max(-32,Math.min(32,-pitch*1.7));
            paint.setColor(accent);paint.setStyle(Paint.Style.FILL);paint.setAlpha(65);canvas.drawCircle(x,y,11,paint);paint.setAlpha(255);paint.setStyle(Paint.Style.STROKE);canvas.drawCircle(x,y,11,paint);
        } else { paint.setStyle(Paint.Style.FILL);paint.setColor(accent);canvas.drawCircle(0,0,3,paint); }
        canvas.restore();
    }
}
