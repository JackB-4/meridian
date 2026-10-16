package app.meridian;
import android.content.Context;
import android.view.*;
/** Equal-width, content-height tiles. Rows expand with text; the parent scrolls. */
public final class TileGrid extends ViewGroup {
    int columns=1; int gap; int[] rowHeights=new int[0];
    public TileGrid(Context c){super(c);gap=Math.round(10*getResources().getDisplayMetrics().density);}
    @Override protected void onMeasure(int w,int h){
        int width=MeasureSpec.getSize(w);float density=getResources().getDisplayMetrics().density;
        columns=TileOrder.columns(width/density,getResources().getConfiguration().fontScale);
        int cell=(width-gap*(columns-1))/columns;rowHeights=new int[(getChildCount()+columns-1)/columns];
        for(int i=0;i<getChildCount();i++){View v=getChildAt(i);v.measure(MeasureSpec.makeMeasureSpec(cell,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(0,MeasureSpec.UNSPECIFIED));rowHeights[i/columns]=Math.max(rowHeights[i/columns],v.getMeasuredHeight());}
        int height=0;for(int n:rowHeights)height+=n;setMeasuredDimension(width,height+Math.max(0,rowHeights.length-1)*gap);
    }
    @Override protected void onLayout(boolean changed,int l,int t,int r,int b){
        int y=0,cell=(getWidth()-gap*(columns-1))/columns;
        for(int i=0;i<getChildCount();i++){int col=i%columns;int x=col*(cell+gap);if(getLayoutDirection()==LAYOUT_DIRECTION_RTL)x=getWidth()-cell-x;getChildAt(i).layout(x,y,x+cell,y+rowHeights[i/columns]);if(col==columns-1)y+=rowHeights[i/columns]+gap;}
    }
    String at(float x,float y){for(int i=0;i<getChildCount();i++){View v=getChildAt(i);if(x>=v.getLeft()&&x<=v.getRight()&&y>=v.getTop()&&y<=v.getBottom())return (String)v.getTag();}return null;}
}
