package app.meridian;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.res.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.*;
import android.hardware.SensorManager;
import android.location.Location;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import java.util.*;

public final class MainActivity extends Activity {
    Prefs prefs; CompassEngine engine;
    LinearLayout root,content; TileGrid readouts; ScrollView mainScroll; DialView dial;
    RegionalUnits regional; Haptics feedback; boolean elevationShortcut; long dragScrollTime;
    TextView headingText,northText,statusText,targetText,locationState;
    Button holdButton,locationButton;
    final Map<String,TextView[]> cards=new LinkedHashMap<>();
    final Handler handler=new Handler(Looper.getMainLooper());
    boolean dark,held=false; double heldHeading,target=Double.NaN; String targetNorth="",heldNorth="";
    int bg,ink,muted,line,surface,accent,north;
    boolean settingsOpen; int settingsTab; ScrollView settingsScroll;
    long lastRender,lastHaptic; int cardinalLatch=-1;
    final Runnable ticker=new Runnable(){public void run(){render();handler.postDelayed(this,500);}};
    @Override public void onCreate(Bundle state) {
        super.onCreate(state); prefs=new Prefs(getSharedPreferences("meridian",MODE_PRIVATE));
        regional=new RegionalUnits(getApplicationContext(),prefs,()->{if(!isDestroyed())render();});regional.initialize();feedback=new Haptics(prefs);
        engine=new CompassEngine(this,prefs,()->{ if(SystemClock.uptimeMillis()-lastRender>15)render(); });
        if(state!=null && state.containsKey("target")){target=state.getDouble("target");targetNorth=state.getString("targetNorth","");}
        if(Build.VERSION.SDK_INT>=33)getOnBackInvokedDispatcher().registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,()->{if(settingsOpen)settingsBack();else finish();});
        if(state!=null){settingsOpen=state.getBoolean("settingsOpen");settingsTab=state.getInt("settingsTab");}
        elevationShortcut="app.meridian.OPEN_ELEVATION".equals(getIntent().getAction());
        if(settingsOpen)buildSettings();else buildUi();
        root.post(this::firstPermission);
    }
    @Override protected void onResume(){super.onResume();engine.start();handler.post(ticker);}
    @Override protected void onPause(){handler.removeCallbacks(ticker);engine.stop();held=false;feedback.reset();super.onPause();}
    @Override protected void onDestroy(){engine.close();super.onDestroy();}
    @Override protected void onSaveInstanceState(Bundle b){super.onSaveInstanceState(b);b.putDouble("target",target);b.putString("targetNorth",targetNorth);b.putBoolean("settingsOpen",settingsOpen);b.putInt("settingsTab",settingsTab);}
    @Override public void onConfigurationChanged(Configuration c){super.onConfigurationChanged(c);held=false;if(settingsOpen)buildSettings();else buildUi();}
    @Override public void onBackPressed(){if(settingsOpen)settingsBack();else super.onBackPressed();}
    int dp(float x){return Math.round(x*getResources().getDisplayMetrics().density);}
    void palette(){int theme=prefs.number("theme");dark=theme==1 || (theme==0 && (getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES);bg=Color.parseColor(dark?"#000000":"#FFFFFF");surface=Color.parseColor(dark?"#181818":"#F2F2F2");ink=Color.parseColor(dark?"#FFFFFF":"#000000");muted=Color.parseColor(dark?"#AAAAAA":"#606060");line=Color.parseColor(dark?"#383838":"#D8D8D8");accent=ink;north=ink;}
    GradientDrawable round(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    TextView text(String value,int size,int color){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setFontFeatureSettings("tnum");t.setIncludeFontPadding(false);return t;}
    LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    Button button(String label,Runnable action){Button b=new Button(this);b.setText(label);b.setTextSize(13);b.setAllCaps(false);b.setTextColor(ink);b.setMinHeight(dp(48));b.setMinimumWidth(0);b.setStateListAnimator(null);b.setElevation(0);b.setPadding(dp(14),dp(8),dp(14),dp(8));b.setBackground(new RippleDrawable(ColorStateList.valueOf(line),round(surface,6),null));b.setOnClickListener(v->{haptic();action.run();});return b;}
    void addSpace(LinearLayout l,int height){View s=new View(this);l.addView(s,new LinearLayout.LayoutParams(1,dp(height)));}
    void buildUi(){
        palette(); cards.clear();
        getWindow().setNavigationBarColor(bg);getWindow().setStatusBarColor(bg);
        getWindow().getDecorView().setSystemUiVisibility(dark?0:View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        if(prefs.on("awake"))getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        root=column();root.setBackgroundColor(bg);
        root.setOnApplyWindowInsetsListener((v,insets)->{ android.graphics.Insets i=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());v.setPadding(i.left,i.top,i.right,i.bottom);return insets; });
        // API 28/29 have legacy insets; modern Pixel uses the listener above.
        if(Build.VERSION.SDK_INT<30)root.setOnApplyWindowInsetsListener((v,i)->{v.setPadding(i.getSystemWindowInsetLeft(),i.getSystemWindowInsetTop(),i.getSystemWindowInsetRight(),i.getSystemWindowInsetBottom());return i;});
        ScrollView scroll=new ScrollView(this);mainScroll=scroll;scroll.setFillViewport(true);scroll.setClipToPadding(false);root.addView(scroll,new LinearLayout.LayoutParams(-1,-1));
        content=column();content.setPadding(dp(22),dp(18),dp(22),dp(24));scroll.addView(content,new ScrollView.LayoutParams(-1,-2));
        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);
        TextView title=text("Meridian",18,ink);title.setTypeface(Typeface.create("sans-serif-medium",0));header.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        Button settings=button("Settings",this::showSettings);settings.setContentDescription("Open compass settings");header.addView(settings);content.addView(header);addSpace(content,4);
        statusText=text("Waiting for sensors",12,muted);statusText.setGravity(Gravity.CENTER);statusText.setPadding(dp(10),dp(12),dp(10),dp(12));statusText.setMinHeight(dp(48));statusText.setOnClickListener(v->showAccuracy());
        dial=new DialView(this);dial.ink=ink;dial.muted=muted;dial.line=line;dial.accent=accent;dial.north=north;dial.surface=bg;content.addView(dial,new LinearLayout.LayoutParams(-1,-2));
        headingText=text("—",58,ink);headingText.setTypeface(Typeface.create("sans-serif-light",0));headingText.setGravity(Gravity.CENTER);content.addView(headingText);
        northText=text("MAGNETIC NORTH",11,muted);northText.setGravity(Gravity.CENTER);content.addView(northText);content.addView(statusText,new LinearLayout.LayoutParams(-1,-2));addSpace(content,4);
        LinearLayout actions=new LinearLayout(this);holdButton=button("Hold",()->{if(held){held=false;}else if(engine.headingFresh()){held=true;heldHeading=engine.shownHeading();heldNorth=northName();}render();});
        actions.addView(holdButton,new LinearLayout.LayoutParams(0,dp(48),1));View gap=new View(this);actions.addView(gap,new LinearLayout.LayoutParams(dp(10),1));actions.addView(button("Bearing",this::showTarget),new LinearLayout.LayoutParams(0,dp(48),1));content.addView(actions);
        actions.setVisibility(prefs.on("controls")?View.VISIBLE:View.GONE);
        targetText=text("",13,accent);targetText.setGravity(Gravity.CENTER);targetText.setPadding(0,dp(12),0,0);content.addView(targetText);
        addSpace(content,14);
        locationButton=button("Enable location",this::enableLocation);content.addView(locationButton,new LinearLayout.LayoutParams(-1,-2));
        locationState=text("",12,muted);locationState.setPadding(0,dp(12),0,0);content.addView(locationState);
        readouts=new TileGrid(this);LinearLayout.LayoutParams gridParams=new LinearLayout.LayoutParams(-1,-2);gridParams.topMargin=dp(14);content.addView(readouts,gridParams);rebuildTiles();
        readouts.setOnDragListener((v,event)->{
            if(!(event.getLocalState() instanceof String))return false;
            String from=(String)event.getLocalState();String to=readouts.at(event.getX(),event.getY());
            if(event.getAction()==DragEvent.ACTION_DRAG_STARTED)return true;
            if(event.getAction()==DragEvent.ACTION_DRAG_LOCATION){
                for(int i=0;i<readouts.getChildCount();i++){View tile=readouts.getChildAt(i);tile.setAlpha(tile.getTag().equals(from)?.45f:to!=null&&tile.getTag().equals(to)?.7f:1);}
                int y=(int)(readouts.getTop()+event.getY())-mainScroll.getScrollY();
                if(SystemClock.uptimeMillis()-dragScrollTime>70){if(y>mainScroll.getHeight()-dp(90))mainScroll.smoothScrollBy(0,dp(40));else if(y<dp(90))mainScroll.smoothScrollBy(0,-dp(40));dragScrollTime=SystemClock.uptimeMillis();}
            }
            if(event.getAction()==DragEvent.ACTION_DROP&&to!=null){prefs.move(from,to);feedback.fire(root,"drag",SystemClock.uptimeMillis());rebuildTiles();render();}
            if(event.getAction()==DragEvent.ACTION_DRAG_ENDED)for(int i=0;i<readouts.getChildCount();i++)readouts.getChildAt(i).setAlpha(1);
            return true;
        });
        if(elevationShortcut)scroll.post(()->scroll.smoothScrollTo(0,readouts.getTop()));
        setContentView(root);root.requestApplyInsets();root.post(()->{if(Build.VERSION.SDK_INT>=30){WindowInsetsController c=getWindow().getInsetsController();if(c!=null){int mask=WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS|WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;c.setSystemBarsAppearance(dark?0:mask,mask);}}});render();
    }
    void rebuildTiles(){
        cards.clear();readouts.removeAllViews();
        for(String key:prefs.order())if(prefs.on(key)||(elevationShortcut&&key.equals("elevation"))){int i=Arrays.asList(Prefs.READOUTS).indexOf(key);addCard(key,Prefs.LABELS[i]);}
    }
    void addCard(String key,String label){
        LinearLayout tile=column();tile.setTag(key);tile.setPadding(dp(12),dp(14),dp(12),dp(14));tile.setMinimumHeight(dp(102));tile.setBackground(new RippleDrawable(ColorStateList.valueOf(line),round(surface,8),null));
        tile.addView(text(label,12,muted));addSpace(tile,8);
        TextView value=text("-",key.equals("coordinates")?15:23,ink);value.setTypeface(Typeface.create("sans-serif-medium",0));tile.addView(value);addSpace(tile,5);
        TextView detail=text("",11,muted);detail.setLineSpacing(dp(2),1);tile.addView(detail);cards.put(key,new TextView[]{value,detail});readouts.addView(tile);
        tile.setFocusable(true);tile.setOnClickListener(v->tileActions(key,label));
        tile.setOnLongClickListener(v->{feedback.fire(v,"drag",SystemClock.uptimeMillis());return v.startDragAndDrop(ClipData.newPlainText("Meridian tile",key),new View.DragShadowBuilder(v),key,0);});
        tile.setAccessibilityDelegate(new View.AccessibilityDelegate(){
            public void onInitializeAccessibilityNodeInfo(View host,android.view.accessibility.AccessibilityNodeInfo info){super.onInitializeAccessibilityNodeInfo(host,info);info.addAction(new android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction(0x01020001,"Move earlier"));info.addAction(new android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction(0x01020002,"Move later"));}
            public boolean performAccessibilityAction(View host,int action,Bundle args){if(action==0x01020001||action==0x01020002){moveTile(key,action==0x01020001?-1:1);return true;}return super.performAccessibilityAction(host,action,args);}
        });
    }
    void moveTile(String key,int offset){List<String> visible=new ArrayList<>(cards.keySet());int i=visible.indexOf(key),j=i+offset;if(i>=0&&j>=0&&j<visible.size()){prefs.move(key,visible.get(j));rebuildTiles();render();readouts.post(()->{View v=readouts.findViewWithTag(key);if(v!=null){v.requestFocus();v.announceForAccessibility("Moved "+(offset<0?"earlier":"later"));}});}}
    void tileActions(String key,String label){String[] items=key.equals("relative")?new String[]{"Move earlier","Move later","Set zero"}:new String[]{"Move earlier","Move later"};dialogBuilder().setTitle(label).setItems(items,(d,w)->{if(w<2)moveTile(key,w==0?-1:1);else if(Double.isFinite(engine.pressure)&&CompassMath.fresh(SystemClock.elapsedRealtimeNanos(),engine.pressureTime)){engine.reference=engine.pressure;render();}else toast("Waiting for barometer");}).show();}
    void firstPermission(){
        if(prefs.store.getBoolean("permission_prompted",false))return;
        prefs.store.edit().putBoolean("permission_prompted",true).apply();
        boolean update=false;try{android.content.pm.PackageInfo p=getPackageManager().getPackageInfo(getPackageName(),0);update=p.lastUpdateTime-p.firstInstallTime>1000;}catch(Exception ignored){}
        if(!update&&prefs.on("location")&&!engine.permitted())requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},10);
    }
    @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);elevationShortcut="app.meridian.OPEN_ELEVATION".equals(intent.getAction());settingsOpen=false;buildUi();}
    void settingsBack(){if(settingsTab==2){settingsTab=0;buildSettings();}else closeSettings();}
    void card(String key,String value,String detail){TextView[] c=cards.get(key);if(c!=null){c[0].setText(value);c[1].setText(detail);c[1].setVisibility(detail.isEmpty()?View.GONE:View.VISIBLE);}}
    String northName(){return prefs.on("trueNorth")&&engine.trueReady()?"True":"Magnetic";}
    String length(double meters){return String.format(Locale.US,"%.0f %s",CompassMath.distance(meters,prefs.number("units")==1),prefs.number("units")==1?"ft":"m");}
    String f(String format,Object...args){return String.format(Locale.US,format,args);}
    void render(){
        if(settingsOpen||dial==null)return;lastRender=SystemClock.uptimeMillis();boolean ready=engine.headingFresh();double h=held?heldHeading:engine.shownHeading();
        String heading=(held||ready)?((prefs.on("degrees")?CompassMath.degrees(h):"")+(prefs.on("degrees")&&prefs.on("cardinal")?"  ":"")+(prefs.on("cardinal")?CompassMath.cardinal(h):"")):"—";
        headingText.setText(heading);headingText.setVisibility(prefs.on("degrees")||prefs.on("cardinal")?View.VISIBLE:View.GONE);
        northText.setText((held?"Held · "+heldNorth:northName())+" north"+(prefs.on("trueNorth")&&!engine.trueReady()&&!held?" · True pending":""));
        dial.setVisibility(prefs.on("dial")?View.VISIBLE:View.GONE);dial.heading=held||ready?h:Double.NaN;dial.target=targetNorth.equals(northName())?target:Double.NaN;dial.level=prefs.on("level")&&ready;dial.pitch=engine.pitch;dial.roll=engine.roll;dial.invalidate();
        holdButton.setText(held?"Resume":"Hold");holdButton.setEnabled(held||ready);
        String status;
        if(!engine.hasCompass())status="Compass sensor unavailable";
        else if(!ready)status="Waiting for sensor";
        else if(Math.abs(engine.pitch)>75)status="Hold phone flat";
        else if(Double.isFinite(engine.field)&&(engine.field<25||engine.field>65))status="Magnetic interference";
        else if(engine.accuracy<=SensorManager.SENSOR_STATUS_ACCURACY_LOW || (engine.magneticAccuracy>=0&&engine.magneticAccuracy<=SensorManager.SENSOR_STATUS_ACCURACY_LOW))status="Calibration needed";
        else status=(engine.accuracy==SensorManager.SENSOR_STATUS_ACCURACY_HIGH&&(engine.magneticAccuracy<0||engine.magneticAccuracy==SensorManager.SENSOR_STATUS_ACCURACY_HIGH)?"Confidence: high":"Confidence: medium");
        statusText.setText(status);statusText.setVisibility(prefs.on("status")?View.VISIBLE:View.GONE);
        if(Double.isFinite(target)&&prefs.on("controls")) {
            targetText.setVisibility(View.VISIBLE);
            double diff=CompassMath.delta(h,target);
            targetText.setText("Bearing "+CompassMath.degrees(target)+" "+targetNorth+" · "+(!targetNorth.equals(northName())?"north reference changed":!ready?"waiting":Math.abs(diff)<2?"on bearing":f("%s %.0f°",diff<0?"left":"right",Math.abs(diff))));
        } else targetText.setVisibility(View.GONE);
        if(!held&&ready&&Math.abs(engine.pitch)<75&&status.startsWith("Confidence"))feedback.heading(root,h,targetNorth.equals(northName())?target:Double.NaN,lastRender);else feedback.reset();
        if(engine.fresh())regional.locate(engine.fix);
        boolean loc=prefs.on("location");locationButton.setVisibility(loc&&(!engine.permitted()||!engine.locations.isLocationEnabled())?View.VISIBLE:View.GONE);locationButton.setText(!engine.permitted()?"Enable location":"Location is off");
        String unavailable=!loc?"Location disabled":!engine.permitted()?"Location permission needed":!engine.locationProblem.isEmpty()?engine.locationProblem:engine.fix==null?"Waiting for location":"Stale location";
        boolean showLocation=false;for(String key:new String[]{"elevation","coordinates","accuracy","speed","course","declination"})if(cards.containsKey(key))showLocation=true;
        locationState.setText(unavailable);locationState.setVisibility(showLocation&&!engine.fresh()?View.VISIBLE:View.GONE);
        if(!engine.fresh())for(String key:new String[]{"elevation","coordinates","accuracy","speed","course","declination"})card(key,"—","");
        else {
            Location l=engine.fix;boolean msl=Build.VERSION.SDK_INT>=34&&l.hasMslAltitude();
            double alt=msl?l.getMslAltitudeMeters():l.getAltitude();
            boolean av=msl?l.hasMslAltitudeAccuracy():l.hasVerticalAccuracy();
            double va=msl?(av?l.getMslAltitudeAccuracyMeters():0):(av?l.getVerticalAccuracyMeters():0);
            String source=msl?"Mean sea level":"WGS84 ellipsoid";
            card("elevation",(msl||l.hasAltitude())?length(alt):"—",(msl||l.hasAltitude())?source+" · "+(av?"±"+length(va):"Accuracy unavailable"):"Altitude unavailable");
            card("coordinates",CompassMath.coordinate(l.getLatitude(),true,prefs.number("coords"))+"\n"+CompassMath.coordinate(l.getLongitude(),false,prefs.number("coords")),engine.precise()?"WGS84":"Approximate location");
            long age=Math.max(0,(SystemClock.elapsedRealtimeNanos()-l.getElapsedRealtimeNanos())/1_000_000_000);
            card("accuracy",l.hasAccuracy()?"±"+length(l.getAccuracy()):"—",(engine.precise()?"Horizontal · ":"Approximate · ")+age+"s ago");
            card("speed",l.hasSpeed()?f("%.1f %s",CompassMath.speed(l.getSpeed(),prefs.number("units")!=0),prefs.number("units")!=0?"mph":"km/h"):"—",l.hasSpeedAccuracy()?f("±%.1f %s",CompassMath.speed(l.getSpeedAccuracyMetersPerSecond(),prefs.number("units")!=0),prefs.number("units")!=0?"mph":"km/h"):"Accuracy unavailable");
            boolean moving=l.hasSpeed()&&l.getSpeed()>Math.max(.7,l.hasSpeedAccuracy()?l.getSpeedAccuracyMetersPerSecond():.7);
            card("course",l.hasBearing()&&moving?CompassMath.degrees(l.getBearing()):"—",l.hasBearing()&&moving?"True north · travel"+(l.hasBearingAccuracy()?f(" · ±%.0f°",l.getBearingAccuracyDegrees()):""):"Waiting for motion");
            card("declination",Float.isFinite(engine.declination)?f("%.1f° %s",Math.abs(engine.declination),engine.declination<0?"W":"E"):"—",Float.isFinite(engine.declination)?"Magnetic model"+(!msl?"\nEllipsoid altitude approximation":""):"Location with altitude needed");
        }
        boolean pFresh=Double.isFinite(engine.pressure)&&CompassMath.fresh(SystemClock.elapsedRealtimeNanos(),engine.pressureTime);
        card("pressure",pFresh?f("%.1f hPa",engine.pressure):"—",engine.barometer==null?"Barometer unavailable":pFresh?"Ambient":"Waiting for barometer");
        card("relative",pFresh&&Double.isFinite(engine.reference)?length(CompassMath.relativeHeight(engine.reference,engine.pressure)):"—",engine.barometer==null?"Barometer unavailable":Double.isFinite(engine.reference)?"Relative · weather drift":"Set zero");
        card("level",ready?f("%.1f° / %.1f°",engine.pitch,engine.roll):"—","Pitch / roll");
        boolean mf=Double.isFinite(engine.field)&&CompassMath.fresh(SystemClock.elapsedRealtimeNanos(),engine.fieldTime);
        card("field",mf?f("%.1f µT",engine.field):"—",engine.magnetometer==null?"Magnetometer unavailable":"");
    }
    void haptic(){feedback.fire(root,"controls",SystemClock.uptimeMillis());}
    void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
    AlertDialog.Builder dialogBuilder(){return new AlertDialog.Builder(new ContextThemeWrapper(this,dark?R.style.DialogDark:R.style.DialogLight));}
    void enableLocation(){
        if(!engine.permitted())dialogBuilder().setTitle("Location").setMessage("Used while open for elevation, position and true north.").setPositiveButton("Continue",(d,w)->requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},10)).setNeutralButton("App permissions",(d,w)->startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName())))).setNegativeButton("Not now",null).show();
        else if(!engine.locations.isLocationEnabled())startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
    }
    @Override public void onRequestPermissionsResult(int code,String[] permissions,int[] results){super.onRequestPermissionsResult(code,permissions,results);engine.startLocation();render();}
    void showTarget(){
        EditText input=new EditText(new ContextThemeWrapper(this,dark?R.style.DialogDark:R.style.DialogLight));input.setTextColor(ink);input.setHintTextColor(muted);input.setBackgroundTintList(ColorStateList.valueOf(ink));input.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);input.setHint("0–359.9°");input.setSelectAllOnFocus(true);if(Double.isFinite(target))input.setText(f("%.0f",target));
        LinearLayout box=column();box.setPadding(dp(24),dp(12),dp(24),0);box.addView(input);
        AlertDialog dialog=dialogBuilder().setTitle("Bearing · "+northName()+" north").setView(box).setPositiveButton("Set",null).setNeutralButton("Clear",(d,w)->{target=Double.NaN;render();}).setNegativeButton("Cancel",null).create();
        dialog.setOnShowListener(d->dialog.getButton(-1).setOnClickListener(v->{try{double value=Double.parseDouble(input.getText().toString());if(!Double.isFinite(value)||value<0||value>=360)throw new NumberFormatException();target=value;targetNorth=northName();held=false;dialog.dismiss();render();}catch(NumberFormatException e){toast("Enter 0–359.9°");}}));dialog.show();
    }
    void showAccuracy(){dialogBuilder().setTitle("Calibration").setMessage("Move away from magnets and metal. Move the phone in a figure eight, then hold it flat.\n\nSensor confidence is not a guaranteed heading error.").setPositiveButton("Close",null).show();}
    void showSettings(){settingsOpen=true;settingsTab=0;buildSettings();}
    void closeSettings(){settingsOpen=false;engine.stop();engine.start();held=false;buildUi();}
    void buildSettings(){
        palette();root=column();root.setBackgroundColor(bg);
        getWindow().setNavigationBarColor(bg);getWindow().setStatusBarColor(bg);
        getWindow().getDecorView().setSystemUiVisibility(dark?0:View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        root.setOnApplyWindowInsetsListener((v,i)->{if(Build.VERSION.SDK_INT>=30){android.graphics.Insets b=i.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());v.setPadding(b.left,b.top,b.right,b.bottom);}else v.setPadding(i.getSystemWindowInsetLeft(),i.getSystemWindowInsetTop(),i.getSystemWindowInsetRight(),i.getSystemWindowInsetBottom());return i;});
        LinearLayout header=new LinearLayout(this);header.setPadding(dp(20),dp(10),dp(20),dp(8));header.setGravity(Gravity.CENTER_VERTICAL);
        Button back=button("Back",this::settingsBack);header.addView(back);
        TextView title=text(settingsTab==2?"Haptics":"Settings",20,ink);title.setPadding(dp(18),0,0,0);header.addView(title);root.addView(header);
        LinearLayout tabs=new LinearLayout(this);tabs.setPadding(dp(20),dp(4),dp(20),dp(12));
        for(int i=0;i<2;i++){final int tab=i;Button b=button(i==0?"General":"Readouts",()->{settingsTab=tab;buildSettings();});b.setSelected(settingsTab==i);b.setTextColor(settingsTab==i?bg:muted);b.setBackground(new RippleDrawable(ColorStateList.valueOf(line),round(settingsTab==i?ink:surface,6),null));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(48),1);if(i>0)lp.leftMargin=dp(6);tabs.addView(b,lp);}if(settingsTab!=2)root.addView(tabs);
        settingsScroll=new ScrollView(this);settingsScroll.setFillViewport(true);LinearLayout list=column();list.setPadding(dp(24),0,dp(24),dp(24));settingsScroll.addView(list);root.addView(settingsScroll,new LinearLayout.LayoutParams(-1,0,1));
        if(settingsTab==0){
            segments(list,"Theme",new String[]{"Auto","Dark","Light"},"theme",prefs.number("theme"));
            segments(list,"Units",new String[]{"Metric","Imperial","UK"},"units",prefs.number("units"));
            TextView unitNote=text(prefs.number("units")==2?"UK: metres · mph":prefs.number("units")==1?"Feet · mph":"Metres · km/h",11,muted);unitNote.setPadding(0,dp(6),0,0);list.addView(unitNote);
            segments(list,"North",new String[]{"Magnetic","True"},"trueNorth",prefs.on("trueNorth")?1:0);
            segments(list,"Coordinates",new String[]{"DD","DMS","DDM"},"coords",prefs.number("coords"));
            addSpace(list,8);toggle(list,"Location","location");toggle(list,"Keep screen on","awake");
            Button haptics=button("Haptics",()->{settingsTab=2;buildSettings();});haptics.setTag("haptic_settings");haptics.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);list.addView(haptics,new LinearLayout.LayoutParams(-1,dp(52)));
            Button calibration=button("Calibration",this::showAccuracy);calibration.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);list.addView(calibration,new LinearLayout.LayoutParams(-1,dp(52)));
        }else if(settingsTab==2){
            toggle(list,"Haptics","haptics");segments(list,"Feedback",new String[]{"Light","Standard"},"haptic_strength",prefs.number("haptic_strength"));segments(list,"Dial interval",new String[]{"5°","10°","15°","30°"},"haptic_cadence",prefs.number("haptic_cadence"));
            for(String[] item:new String[][]{{"Dial ticks","dial"},{"Cardinal points","cardinal"},{"On bearing","target"},{"Reordering","drag"},{"Controls","controls"}})toggle(list,item[0],"haptic_"+item[1]);
        }else{
            section(list,"Compass");toggle(list,"Dial","dial");toggle(list,"Degrees","degrees");toggle(list,"Direction","cardinal");toggle(list,"Confidence","status");toggle(list,"Hold & bearing","controls");
            section(list,"Data");for(int i=0;i<Prefs.READOUTS.length;i++)toggle(list,Prefs.LABELS[i],Prefs.READOUTS[i]);
        }
        setContentView(root);root.requestApplyInsets();root.post(()->{if(Build.VERSION.SDK_INT>=30){WindowInsetsController c=getWindow().getInsetsController();if(c!=null){int mask=WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS|WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;c.setSystemBarsAppearance(dark?0:mask,mask);}}});
    }
    void section(LinearLayout list,String label){addSpace(list,16);TextView t=text(label,12,muted);list.addView(t);addSpace(list,4);}
    void toggle(LinearLayout list,String label,String key){
        Switch sw=new Switch(this);sw.setTag(key);sw.setText(label);sw.setTextColor(ink);sw.setTextSize(15);sw.setPadding(0,dp(12),0,dp(12));sw.setMinHeight(dp(56));sw.setChecked(prefs.on(key));
        int[][] states={{android.R.attr.state_checked},{}};
        sw.setTrackTintList(new ColorStateList(states,new int[]{ink,line}));sw.setThumbTintList(new ColorStateList(states,new int[]{ink,muted}));
        sw.setOnCheckedChangeListener((v,on)->{prefs.put(key,on);if(key.equals("location"))engine.startLocation();});list.addView(sw,new LinearLayout.LayoutParams(-1,-2));
    }
    void segments(LinearLayout list,String label,String[] options,String key,int selected){
        TextView name=text(label,12,muted);name.setPadding(0,dp(12),0,dp(8));list.addView(name);LinearLayout row=new LinearLayout(this);boolean stacked=getResources().getConfiguration().fontScale>1.25f||(getResources().getConfiguration().screenWidthDp<350&&options.length>2);if(stacked)row.setOrientation(LinearLayout.VERTICAL);
        for(int i=0;i<options.length;i++){final int index=i;Button b=button(options[i],()->{if(key.equals("trueNorth"))prefs.put(key,index==1);else prefs.put(key,index);int y=settingsScroll.getScrollY();buildSettings();settingsScroll.post(()->settingsScroll.scrollTo(0,y));});b.setTag(key+":"+i);b.setSelected(i==selected);b.setTextColor(i==selected?bg:ink);b.setBackground(new RippleDrawable(ColorStateList.valueOf(line),round(i==selected?ink:surface,6),null));b.setContentDescription(label+": "+(key.equals("coords")?new String[]{"Decimal degrees","Degrees minutes seconds","Degrees decimal minutes"}[i]:options[i]));LinearLayout.LayoutParams lp=stacked?new LinearLayout.LayoutParams(-1,-2):new LinearLayout.LayoutParams(0,dp(48),1);if(i>0){if(stacked)lp.topMargin=dp(4);else lp.leftMargin=dp(4);}row.addView(b,lp);}list.addView(row);
    }
}
