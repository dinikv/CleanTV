package org.cleantv.launcher;
import android.content.Context;
/** Public build is offline. No location is configured and no weather request is made. */
final class Weather {
 static final long REFRESH=30*60*1000L;
 Weather(Context context){}
 void refresh(Runnable done){done.run();}
 String compact(){return "";}
 String details(){return "Offline launcher. Weather is not configured in this public build.";}
}
