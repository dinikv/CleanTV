package org.cleantv.launcher;
import android.content.Context;
import android.graphics.*;
import android.os.SystemClock;
import android.view.View;
/** Original procedural ambient animation. No external assets or network access. */
public final class DreamView extends View {
 private final Paint p=new Paint(3); private boolean running; private long start;
 public DreamView(Context c){super(c);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
 public void setRunning(boolean v){if(v&&!running)start=SystemClock.uptimeMillis();running=v;if(v)invalidate();}
 protected void onDraw(Canvas c){float w=getWidth(),h=getHeight(),t=(SystemClock.uptimeMillis()-start)/1000f;
 p.setShader(new LinearGradient(0,0,w,h,new int[]{0xff0b1529,0xff133755,0xff231d48},null,Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,p);p.setShader(null);
 for(int i=0;i<5;i++){float x=w*(.5f+.38f*(float)Math.sin(t*.035f+i*1.2f)),y=h*(.5f+.25f*(float)Math.cos(t*.05f+i));p.setColor(Color.argb(15+i*5,90,180+i*10,240));c.drawCircle(x,y,h*(.16f+i*.035f),p);}
 if(running)postInvalidateDelayed(50);
 }
}
