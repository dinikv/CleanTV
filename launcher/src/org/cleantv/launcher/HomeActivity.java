package org.cleantv.launcher;

import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.*;
import android.view.animation.*;
import android.widget.*;
import android.animation.*;
import android.renderscript.*;
import java.util.*;

/** Offline, native TV home. Artwork is bundled; installed applications supply their own icons. */
public class HomeActivity extends Activity {
  final String[] favorites={"com.apple.atve.amazon.appletv","com.netflix.ninja","com.amazon.firetv.youtube","com.stremio.one","com.amazon.firebat","com.spotify.tv.android"};
  final ArrayList<FrameLayout> cards=new ArrayList<>();
  final ArrayList<TextView> labels=new ArrayList<>();
  final ArrayList<ImageView> icons=new ArrayList<>();
  ArrayList<ResolveInfo> entries;
  FrameLayout root,panel; Backdrop backdrop; DockGlass dock; TextView hint; LinearLayout status,weatherBlock;
  Weather weather; TextView weatherLabel;
  final Runnable weatherTick=new Runnable(){public void run(){if(!foreground)return;weather.refresh(()->runOnUiThread(()->updateWeatherLabel()));idleHandler.postDelayed(this,Weather.REFRESH);}};
  void updateWeatherLabel(){if(weatherLabel!=null){String value=weather.compact();weatherLabel.setText(value.isEmpty()?"—":value);}}
  void weatherPanel(){String network="Offline";try{android.net.ConnectivityManager cm=(android.net.ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);android.net.NetworkCapabilities caps=cm.getNetworkCapabilities(cm.getActiveNetwork());if(caps!=null)network=caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED)?"Internet connected":"Network connected · Internet unverified";}catch(Exception ignored){}java.text.SimpleDateFormat date=new java.text.SimpleDateFormat("EEEE, MMMM d",java.util.Locale.CANADA);date.setTimeZone(java.util.TimeZone.getDefault());new AlertDialog.Builder(this).setTitle("Local information").setMessage(date.format(new java.util.Date())+"\n\n"+weather.details()+"\n\n"+network).setPositiveButton("Done",null).show();}
  DreamView dream; boolean foreground=false; int wakeKey=-1;
  final Handler idleHandler=new Handler();
  final Runnable idle=()->{if(foreground&&hasWindowFocus()){artMode=true;setScene(true);}};
  void resetIdle(){idleHandler.removeCallbacks(idle);if(foreground&&!artMode)idleHandler.postDelayed(idle,60000);}
  Bitmap artwork,blurred; int width,height,tileW,tileH,rowStep,picked=0,lastDock=0; float homeY; boolean artMode=false,longSelect=false; String signature="";
  final android.view.animation.Interpolator ease=new PathInterpolator(.2f,.8f,.2f,1f);
  int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
  TextView text(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setTypeface(Typeface.create("sans-serif",0));return t;}
  GradientDrawable box(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
  FrameLayout.LayoutParams place(int w,int h,int x,int y){FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(w,h);p.leftMargin=x;p.topMargin=y;return p;}
  @Override public void onCreate(Bundle state){super.onCreate(state);getWindow().getDecorView().setSystemUiVisibility(5894);weather=new Weather(this);updateDock();loadArtwork();render();}
  void updateDock(){if(getPreferences(0).getBoolean("primeSpotifyDock",false))return;String saved=getPreferences(0).getString("order","");if(!saved.isEmpty()){ArrayList<String> order=new ArrayList<>(Arrays.asList(saved.split(",")));String[] before={"com.plexapp.android","ca.bellmedia.cravetv"},after={"com.amazon.firebat","com.spotify.tv.android"};for(int i=0;i<2;i++){int old=order.indexOf(before[i]),replacement=order.indexOf(after[i]);if(old>=0){if(replacement>=0)Collections.swap(order,old,replacement);else order.set(old,after[i]);}}getPreferences(0).edit().putString("order",String.join(",",order)).apply();}getPreferences(0).edit().putBoolean("primeSpotifyDock",true).apply();}
  int wallpaperGeneration=0; boolean wallpaperLoading=false;
  final Runnable wallpaperTick=new Runnable(){public void run(){if(!foreground)return;int next=(getPreferences(0).getInt("wallpaper",0)+1)%4;changeWallpaper(next);idleHandler.postDelayed(this,60000);}};
  void changeWallpaper(int selected){final int generation=++wallpaperGeneration;wallpaperLoading=true;new Thread(()->{try{Bitmap[] images=readArtwork(selected);runOnUiThread(()->{if(generation!=wallpaperGeneration||isDestroyed())return;artwork=images[0];blurred=images[1];getPreferences(0).edit().putInt("wallpaper",selected).apply();wallpaperLoading=false;backdrop.invalidate();dock.invalidate();});}catch(Exception error){runOnUiThread(()->{if(generation==wallpaperGeneration)wallpaperLoading=false;});}},"CleanTV-wallpaper").start();}
  void loadArtwork(){Bitmap[] images=readArtwork(getPreferences(0).getInt("wallpaper",0));artwork=images[0];blurred=images[1];}
  Bitmap[] readArtwork(int selected){int[] wallpapers={R.drawable.wallpaper_0,R.drawable.wallpaper_1,R.drawable.wallpaper_2,R.drawable.wallpaper_3};BitmapFactory.Options options=new BitmapFactory.Options();options.inScaled=false;options.inSampleSize=1;options.inPreferredConfig=Bitmap.Config.ARGB_8888;Bitmap artwork=BitmapFactory.decodeResource(getResources(),wallpapers[Math.max(0,Math.min(3,selected))],options);Bitmap small=Bitmap.createScaledBitmap(artwork,420,236,true);Bitmap blurred=Bitmap.createBitmap(420,236,Bitmap.Config.ARGB_8888);try{RenderScript rs=RenderScript.create(this);Allocation input=Allocation.createFromBitmap(rs,small);Allocation output=Allocation.createFromBitmap(rs,blurred);ScriptIntrinsicBlur blur=ScriptIntrinsicBlur.create(rs,Element.U8_4(rs));blur.setRadius(9);blur.setInput(input);blur.forEach(output);output.copyTo(blurred);input.destroy();output.destroy();blur.destroy();rs.destroy();small.recycle();}catch(Exception e){blurred=small;}return new Bitmap[]{artwork,blurred};}
  java.util.List<String> favoriteOrder(){ArrayList<String> result=new ArrayList<>();for(String pkg:favorites)result.add(TvApps.installed(getPackageManager(),pkg));return result;}
  ArrayList<ResolveInfo> apps(){LinkedHashMap<String,ResolveInfo> map=new LinkedHashMap<>();for(String cat:new String[]{Intent.CATEGORY_LEANBACK_LAUNCHER,Intent.CATEGORY_LAUNCHER})for(ResolveInfo r:getPackageManager().queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(cat),0)){String p=r.activityInfo.packageName;if(p.equals(getPackageName()))continue;if(!map.containsKey(p))map.put(p,r);}ArrayList<ResolveInfo> result=new ArrayList<>(map.values());String saved=getPreferences(0).getString("order","");final java.util.List<String> order=saved.isEmpty()?favoriteOrder():Arrays.asList(saved.split(","));Collections.sort(result,(a,b)->{int x=order.indexOf(a.activityInfo.packageName),y=order.indexOf(b.activityInfo.packageName);if(x<0)x=999;if(y<0)y=999;return x!=y?x-y:a.loadLabel(getPackageManager()).toString().compareToIgnoreCase(b.loadLabel(getPackageManager()).toString());});return result;}
  String appSignature(ArrayList<ResolveInfo> list){StringBuilder b=new StringBuilder();for(ResolveInfo r:list)b.append(r.activityInfo.packageName);return b.toString();}
  @Override protected void onResume(){super.onResume();foreground=true;idleHandler.removeCallbacks(wallpaperTick);idleHandler.postDelayed(wallpaperTick,60000);idleHandler.removeCallbacks(weatherTick);idleHandler.post(weatherTick);if(dream!=null)dream.setRunning(artMode);resetIdle();getWindow().getDecorView().setSystemUiVisibility(5894);if(entries!=null&&!signature.equals(appSignature(apps())))render();}
  @Override protected void onPause(){foreground=false;++wallpaperGeneration;idleHandler.removeCallbacks(wallpaperTick);idleHandler.removeCallbacks(weatherTick);idleHandler.removeCallbacks(idle);if(dream!=null)dream.setRunning(false);super.onPause();}
  @Override public void onWindowFocusChanged(boolean focused){super.onWindowFocusChanged(focused);if(focused)resetIdle();else idleHandler.removeCallbacks(idle);}
  void render(){
    entries=apps();signature=appSignature(entries);cards.clear();labels.clear();icons.clear();picked=Math.min(picked,Math.max(0,entries.size()-1));
    width=getResources().getDisplayMetrics().widthPixels;height=getResources().getDisplayMetrics().heightPixels;homeY=height*.728f;
    int inset=dp(45),gap=dp(24);tileW=(width-inset*2-gap*5)/6;tileH=(int)(tileW*.60f);rowStep=tileH+dp(52);
    root=new FrameLayout(this);root.setClipChildren(false);backdrop=new Backdrop(this);root.addView(backdrop,new FrameLayout.LayoutParams(-1,-1));
    panel=new FrameLayout(this);panel.setClipChildren(false);panel.setClipToPadding(false);root.addView(panel,place(width,Math.max(height,rowStep*((entries.size()+5)/6)+dp(80)),0,0));
    dock=new DockGlass(this);panel.addView(dock,place(width-dp(40),tileH+dp(38),dp(20),0));
    for(int i=0;i<entries.size();i++){
      final int idx=i;ResolveInfo r=entries.get(i);String name=r.loadLabel(getPackageManager()).toString();int row=i/6,col=i%6;
      FrameLayout card=new FrameLayout(this);card.setId(View.generateViewId());card.setFocusable(true);card.setClickable(true);card.setClipToOutline(true);card.setBackground(box(0xff20252e,13));card.setElevation(dp(4));card.setCameraDistance(dp(1800));card.setContentDescription(name);
      panel.addView(card,place(tileW,tileH,inset+col*(tileW+gap),dp(13)+row*rowStep));
      Drawable banner=null;try{banner=r.activityInfo.loadBanner(getPackageManager());}catch(Exception ignored){}
      ImageView icon=new ImageView(this);icon.setImageDrawable(banner!=null?banner:r.loadIcon(getPackageManager()));icon.setScaleType(banner!=null?ImageView.ScaleType.CENTER_CROP:ImageView.ScaleType.FIT_CENTER);if(banner==null)icon.setPadding(dp(22),dp(12),dp(22),dp(12));card.addView(icon,new FrameLayout.LayoutParams(-1,-1));icons.add(icon);
      TextView label=text(name,12,0xfff5f7fc);label.setSingleLine(true);label.setEllipsize(android.text.TextUtils.TruncateAt.END);label.setGravity(Gravity.CENTER);label.setAlpha(0);label.setShadowLayer(dp(2),0,dp(1),0xff000000);panel.addView(label,place(tileW+dp(24),dp(24),inset+col*(tileW+gap)-dp(12),dp(18)+row*rowStep+tileH));labels.add(label);cards.add(card);
      card.setOnFocusChangeListener((v,focused)->focusVisual(idx,focused));card.setOnClickListener(v->{picked=idx;launch();});card.setOnLongClickListener(v->{picked=idx;tileMenu();return true;});
    }
    hint=text("⌃  Pick for tonight",13,0xbbe6eef6);hint.setGravity(Gravity.CENTER);hint.setShadowLayer(dp(3),0,dp(1),Color.BLACK);root.addView(hint,place(dp(220),dp(28),width/2-dp(110),(int)homeY-dp(35)));
    weatherBlock=new LinearLayout(this);weatherBlock.setOrientation(LinearLayout.VERTICAL);weatherBlock.setGravity(Gravity.LEFT);
    weatherLabel=text("",25,0xfff4f5f7);weatherLabel.setTypeface(Typeface.create("sans-serif-light",0));weatherLabel.setIncludeFontPadding(false);weatherLabel.setShadowLayer(dp(4),0,dp(1),0x88000000);weatherBlock.addView(weatherLabel,new LinearLayout.LayoutParams(-1,dp(33)));
    TextView city=text("Offline by design",11,0xccf4f5f7);city.setIncludeFontPadding(false);city.setLetterSpacing(.045f);city.setShadowLayer(dp(3),0,dp(1),0x99000000);weatherBlock.addView(city);updateWeatherLabel();weatherBlock.setOnClickListener(v->weatherPanel());root.addView(weatherBlock,place(dp(190),dp(62),dp(45),dp(27)));
    status=new LinearLayout(this);status.setOrientation(LinearLayout.VERTICAL);status.setGravity(Gravity.RIGHT);
    TextClock clock=new TextClock(this);clock.setFormat12Hour("h:mm");clock.setFormat24Hour("HH:mm");clock.setTextColor(0xfff4f5f7);clock.setTextSize(25);clock.setTypeface(Typeface.create("sans-serif-light",0));clock.setIncludeFontPadding(false);clock.setGravity(Gravity.RIGHT);clock.setShadowLayer(dp(4),0,dp(1),0x88000000);status.addView(clock,new LinearLayout.LayoutParams(-1,dp(33)));
    TextClock date=new TextClock(this);date.setFormat12Hour("EEE, MMM d");date.setFormat24Hour("EEE, MMM d");date.setTextColor(0xccf4f5f7);date.setTextSize(11);date.setIncludeFontPadding(false);date.setLetterSpacing(.045f);date.setGravity(Gravity.RIGHT);date.setShadowLayer(dp(3),0,dp(1),0x99000000);status.addView(date,new LinearLayout.LayoutParams(-1,-2));status.setContentDescription("Time and date");status.setOnClickListener(v->showMenu());root.addView(status,place(dp(190),dp(62),width-dp(235),dp(27)));
    dream=new DreamView(this);dream.setVisibility(View.GONE);root.addView(dream,new FrameLayout.LayoutParams(-1,-1));
    setContentView(root);panel.setTranslationY(homeY);if(!cards.isEmpty())cards.get(picked).requestFocus();setScene(false);
  }
  void focusVisual(int i,boolean f){FrameLayout card=cards.get(i);card.animate().cancel();card.animate().scaleX(f?1.15f:1).scaleY(f?1.15f:1).translationZ(dp(f?18:0)).rotationY(0).rotationX(0).setDuration(f?210:170).setInterpolator(ease).start();GradientDrawable sheen=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{f?0x22ffffff:0x00ffffff,0x00ffffff});sheen.setCornerRadius(dp(13));sheen.setStroke(dp(1),f?0x85ffffff:0x10ffffff);card.setForeground(sheen);labels.get(i).animate().alpha(f?1:0).setDuration(150).start();}
  void select(int next){if(next<0||next>=cards.size())return;int old=picked;picked=next;if(picked<6)lastDock=picked;int direction=Integer.compare(next%6,old%6);FrameLayout card=cards.get(next);card.setRotationY(direction*3);icons.get(next).setTranslationX(dp(direction*2));icons.get(next).animate().translationX(0).setDuration(240).setInterpolator(ease).start();card.requestFocus();setScene(true);}
  void setScene(boolean animate){if(dream!=null){dream.animate().cancel();if(artMode){dream.setVisibility(View.VISIBLE);dream.setAlpha(0);dream.setRunning(foreground);dream.animate().alpha(1).setDuration(650).start();}else{dream.setRunning(false);dream.setVisibility(View.GONE);}}resetIdle();float target=artMode?height+dp(30):picked<6?homeY:dp(28)-Math.max(0,picked/6-2)*rowStep;int duration=animate?380:0;panel.animate().translationY(target).setDuration(duration).setInterpolator(ease).start();hint.animate().alpha(!artMode&&picked<6?.8f:0).setDuration(180).start();status.animate().alpha(!artMode&&picked<6?1:0).setDuration(200).start();weatherBlock.animate().alpha(!artMode&&picked<6?1:0).setDuration(200).start();backdrop.transition(artMode?0:picked<6?.025f:.68f,artMode?1.025f:1f,duration);}
  @Override public boolean dispatchKeyEvent(KeyEvent e){int key=e.getKeyCode();if(wakeKey==key){if(e.getAction()==KeyEvent.ACTION_UP)wakeKey=-1;return true;}if(artMode&&(key==19||key==20||key==21||key==22||key==23||key==66||key==4||key==82)){if(e.getAction()==KeyEvent.ACTION_DOWN){artMode=false;wakeKey=key;setScene(true);}return true;}if(e.getAction()==KeyEvent.ACTION_DOWN)resetIdle();if(key==KeyEvent.KEYCODE_DPAD_UP||key==KeyEvent.KEYCODE_DPAD_DOWN||key==KeyEvent.KEYCODE_DPAD_LEFT||key==KeyEvent.KEYCODE_DPAD_RIGHT||key==KeyEvent.KEYCODE_DPAD_CENTER||key==KeyEvent.KEYCODE_ENTER){if(e.getAction()==KeyEvent.ACTION_DOWN)return onKeyDown(key,e);if(e.getAction()==KeyEvent.ACTION_UP)return onKeyUp(key,e);}return super.dispatchKeyEvent(e);}
  @Override public boolean onKeyDown(int key,KeyEvent e){
    if(key==KeyEvent.KEYCODE_MENU){showMenu();return true;}
    if(key==KeyEvent.KEYCODE_DPAD_CENTER||key==KeyEvent.KEYCODE_ENTER){if(e.getRepeatCount()==0)longSelect=false;else if(e.getRepeatCount()==1){longSelect=true;tileMenu();}return true;}
    if(key==KeyEvent.KEYCODE_DPAD_UP){if(artMode)return true;if(picked<6){if(e.getRepeatCount()==0)startActivity(new Intent(this,PickActivity.class));}else select(picked-6);return true;}
    if(key==KeyEvent.KEYCODE_DPAD_DOWN){if(artMode){artMode=false;setScene(true);}else if(picked+6<cards.size())select(picked+6);else if(picked/6<(cards.size()-1)/6)select(cards.size()-1);return true;}
    if(key==KeyEvent.KEYCODE_DPAD_LEFT||key==KeyEvent.KEYCODE_DPAD_RIGHT){if(artMode)return true;int step=key==KeyEvent.KEYCODE_DPAD_LEFT?-1:1;int next=picked+step;if(next>=0&&next<cards.size()&&next/6==picked/6)select(next);return true;}
    return super.onKeyDown(key,e);
  }
  @Override public boolean onKeyUp(int key,KeyEvent e){if(key==KeyEvent.KEYCODE_DPAD_CENTER||key==KeyEvent.KEYCODE_ENTER){if(!longSelect){if(artMode){artMode=false;setScene(true);}else launch();}return true;}return super.onKeyUp(key,e);}
  @Override public void onBackPressed(){if(artMode){artMode=false;setScene(true);}else if(picked>=6)select(lastDock);else{artMode=true;setScene(true);}}
  void launch(){if(entries.isEmpty())return;ResolveInfo r=entries.get(picked);try{startActivity(new Intent(Intent.ACTION_MAIN).setComponent(new ComponentName(r.activityInfo.packageName,r.activityInfo.name)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}catch(Exception e){Toast.makeText(this,"This app could not be opened",Toast.LENGTH_SHORT).show();}}
  void tileMenu(){if(entries.isEmpty())return;new AlertDialog.Builder(this).setTitle(entries.get(picked).loadLabel(getPackageManager())).setItems(new String[]{"Open","Move left","Move right","Move to first favorite"},(d,w)->{if(w==0){launch();return;}int to=w==1?Math.max(0,picked-1):w==2?Math.min(entries.size()-1,picked+1):0;ResolveInfo moving=entries.remove(picked);entries.add(to,moving);StringBuilder order=new StringBuilder();for(ResolveInfo r:entries){if(order.length()>0)order.append(',');order.append(r.activityInfo.packageName);}getPreferences(0).edit().putString("order",order.toString()).apply();picked=to;render();}).show();}
  void wallpaperMenu(){new AlertDialog.Builder(this).setTitle("Wallpaper · changes every minute").setSingleChoiceItems(new String[]{"Neon city","Thor","Batman — red","Batman — blue"},getPreferences(0).getInt("wallpaper",0),(dialog,which)->{dialog.dismiss();changeWallpaper(which);idleHandler.removeCallbacks(wallpaperTick);idleHandler.postDelayed(wallpaperTick,60000);resetIdle();}).setNegativeButton("Cancel",null).show();}
  void showMenu(){new AlertDialog.Builder(this).setTitle("CleanTV").setItems(new String[]{"TV settings","Start screensaver","Refresh apps","Wallpaper","Weather & information","About"},(d,w)->{if(w==0){try{startActivity(new Intent("android.settings.SETTINGS"));}catch(Exception ex){Toast.makeText(this,"Open Settings from your TV’s system menu",Toast.LENGTH_LONG).show();}}else if(w==1){artMode=true;setScene(true);}else if(w==2)render();else if(w==3)wallpaperMenu();else if(w==4)weatherPanel();else new AlertDialog.Builder(this).setTitle("CleanTV").setMessage("Local wallpapers. This public build makes no weather requests. No location or microphone access, ads or analytics.\n\nUp: Pick for tonight\nDown: apps\nHold Select: arrange apps\nMenu: settings").setPositiveButton("Done",null).show();}).show();}
  void drawCover(Canvas c,Bitmap bitmap,Paint p){float scale=Math.max(width/(float)bitmap.getWidth(),height/(float)bitmap.getHeight());float w=bitmap.getWidth()*scale,h=bitmap.getHeight()*scale;c.drawBitmap(bitmap,null,new RectF((width-w)/2,(height-h)/2,(width+w)/2,(height+h)/2),p);}
  class Backdrop extends View{
    Paint p=new Paint(3);float darkness=.1f,zoom=1;ValueAnimator motion;Backdrop(Context c){super(c);}
    void transition(float dim,float scale,int duration){if(motion!=null)motion.cancel();float old=darkness,oldZoom=zoom;motion=ValueAnimator.ofFloat(0,1);motion.setDuration(duration);motion.setInterpolator(ease);motion.addUpdateListener(a->{float t=(Float)a.getAnimatedValue();darkness=old+(dim-old)*t;zoom=oldZoom+(scale-oldZoom)*t;invalidate();dock.invalidate();});motion.start();}
    protected void onDraw(Canvas c){p.setColor(Color.WHITE);c.save();c.scale(zoom,zoom,width/2f,height/2f);drawCover(c,artwork,p);if(darkness>.15f){p.setAlpha((int)(Math.min(1,(darkness-.15f)*1.4f)*255));drawCover(c,blurred,p);p.setAlpha(255);}c.restore();p.setColor(Color.argb((int)(darkness*255),3,7,12));c.drawRect(0,0,width,height,p);p.setShader(new LinearGradient(0,height*.55f,0,height,new int[]{0x00000000,0x60030a13},null,Shader.TileMode.CLAMP));c.drawRect(0,0,width,height,p);p.setShader(null);}
  }
  class DockGlass extends View{
    Paint p=new Paint(3);Path shape=new Path();DockGlass(Context c){super(c);}
    protected void onDraw(Canvas c){p.setColor(Color.WHITE);RectF bounds=new RectF(0,0,getWidth(),getHeight());shape.reset();shape.addRoundRect(bounds,dp(24),dp(24),Path.Direction.CW);c.save();c.clipPath(shape);c.save();c.translate(-getLeft(),-panel.getTranslationY());drawCover(c,blurred,p);c.restore();p.setColor(0x25262d37);c.drawRect(bounds,p);p.setShader(new LinearGradient(0,0,0,getHeight(),new int[]{0x24ffffff,0x06ffffff},null,Shader.TileMode.CLAMP));c.drawRect(bounds,p);p.setShader(null);c.restore();p.setColor(0x35ffffff);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(1));c.drawRoundRect(new RectF(1,1,getWidth()-1,getHeight()-1),dp(24),dp(24),p);p.setStyle(Paint.Style.FILL);}
  }
}
