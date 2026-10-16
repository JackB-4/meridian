package app.meridian;
import java.util.*;
public final class TileOrder {
    public static final String[] DEFAULT={"elevation","accuracy","coordinates","speed","course","pressure","relative","level","field","declination"};
    public static List<String> normalize(String saved){
        List<String> result=new ArrayList<>();List<String> allowed=Arrays.asList(DEFAULT);
        if(saved!=null)for(String s:saved.split(","))if(allowed.contains(s)&&!result.contains(s))result.add(s);
        for(String s:DEFAULT)if(!result.contains(s))result.add(s);return result;
    }
    public static List<String> move(List<String> source,String from,String to){
        List<String> result=new ArrayList<>(source);int target=result.indexOf(to);
        if(target<0||!result.contains(from)||from.equals(to))return result;
        result.remove(from);result.add(Math.min(target,result.size()),from);return result;
    }
    public static int columns(float widthDp,float fontScale){return Math.max(1,Math.min(3,(int)((widthDp+10)/(138*Math.max(1,fontScale)+10))));}
}
