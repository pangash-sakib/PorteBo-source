package com.sakib.portebo.overlay;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import com.google.android.material.button.MaterialButton;
import com.sakib.portebo.R;
import com.sakib.portebo.data.PrefsManager;
import com.sakib.portebo.model.AppItem;
import com.sakib.portebo.service.AppMonitorService;
import com.sakib.portebo.ui.ThemePaletteManager;

public final class OverlayManager {
    public enum OverlayState { NONE, BUBBLE, BLOCK }
    private static OverlayManager instance;
    private final Context appContext;
    private final AccessibilityService service;
    private final Handler main=new Handler(Looper.getMainLooper());
    private View blockView,fallbackView,bubble;
    private WindowManager blockWm,bubbleWm;
    private WindowManager.LayoutParams bubbleParams;
    private boolean blockAttached;
    private OverlayState state=OverlayState.NONE;
    private OverlayManager(Context context){
        appContext=context.getApplicationContext();
        service=context instanceof AccessibilityService?(AccessibilityService)context:AppMonitorService.getInstance();
    }
    public static synchronized OverlayManager getInstance(Context c){if(instance==null)instance=new OverlayManager(c);return instance;}
    private int appWindowType(){return Build.VERSION.SDK_INT>=26?WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY:WindowManager.LayoutParams.TYPE_PHONE;}
    private WindowManager appWm(){return (WindowManager)appContext.getSystemService(Context.WINDOW_SERVICE);}
    private WindowManager serviceWm(){return service==null?null:(WindowManager)service.getSystemService(Context.WINDOW_SERVICE);}
    private WindowManager.LayoutParams blockParams(int type){WindowManager.LayoutParams p=new WindowManager.LayoutParams(-1,-1,type,WindowManager.LayoutParams.FLAG_FULLSCREEN|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,PixelFormat.TRANSLUCENT);p.gravity=Gravity.CENTER;return p;}
    public void showBlacklistOverlay(String name,String warning,Runnable attached){main.post(()->showBlockOnMain(name,warning,attached));}
    private void showBlockOnMain(String name,String warning,Runnable attached){
        try{
            if(blockView==null){ContextThemeWrapper themed=new ContextThemeWrapper(service!=null?service:appContext,ThemePaletteManager.getSelectedThemeStyle(appContext));blockView=LayoutInflater.from(themed).inflate(R.layout.overlay_fake_battery,null);}
            setBlockText(blockView,name,warning);
            if(blockAttached){View target=fallbackView!=null?fallbackView:blockView;if(attached!=null&&target!=null)target.post(attached);return;}
            WindowManager swm=serviceWm();
            if(swm!=null){
                try{swm.addView(blockView,blockParams(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY));blockWm=swm;markAttached(name,attached);return;}
                catch(WindowManager.BadTokenException|SecurityException ex){DebugLog.add("OVERLAY accessibility add failed package="+name+" lockdown="+(com.sakib.portebo.lockdown.LockdownManager.isLockdownActive(appContext)?"yes":"no")+" exception="+ex.getClass().getSimpleName()+": "+ex.getMessage());}
            }
            try{WindowManager awm=appWm();awm.addView(blockView,blockParams(appWindowType()));blockWm=awm;markAttached(name,attached);return;}
            catch(WindowManager.BadTokenException|SecurityException ex){DebugLog.add("OVERLAY app-window add failed package="+name+" lockdown="+(com.sakib.portebo.lockdown.LockdownManager.isLockdownActive(appContext)?"yes":"no")+" exception="+ex.getClass().getSimpleName()+": "+ex.getMessage());}
            showProgrammaticFallback(name,warning,attached);
        }catch(Exception ex){DebugLog.add("OVERLAY inflate/setup failed package="+name+" exception="+ex.getClass().getSimpleName()+": "+ex.getMessage());showProgrammaticFallback(name,warning,attached);}
    }
    private void setBlockText(View v,String name,String warning){TextView title=v.findViewById(R.id.tvOverlayBlockTitle),desc=v.findViewById(R.id.tvOverlayBlockDesc);if(title!=null)title.setText(name+" ব্লক করা হয়েছে");if(desc!=null)desc.setText(warning);View b=v.findViewById(R.id.btnCloseBlockedApp);if(b!=null)b.setOnClickListener(x->closeBlockedApp());}
    private void markAttached(String name,Runnable attached){blockAttached=true;state=OverlayState.BLOCK;DebugLog.add("OVERLAY attached package="+name+" lockdown="+(com.sakib.portebo.lockdown.LockdownManager.isLockdownActive(appContext)?"yes":"no"));if(attached!=null&&blockView!=null)blockView.post(attached);}
    private void showProgrammaticFallback(String name,String warning,Runnable attached){
        try{
            ContextThemeWrapper themed=new ContextThemeWrapper(appContext,ThemePaletteManager.getSelectedThemeStyle(appContext));android.widget.LinearLayout root=new android.widget.LinearLayout(themed);root.setOrientation(1);root.setGravity(Gravity.CENTER);root.setBackgroundColor(0xff000000);root.setPadding(36,36,36,36);
            TextView title=new TextView(themed);title.setText(name+" ব্লক করা হয়েছে\n"+warning);title.setTextColor(0xffffffff);title.setTextSize(20);title.setGravity(Gravity.CENTER);root.addView(title);
            android.widget.Button close=new android.widget.Button(themed);close.setText(appContext.getString(R.string.fake_battery_btn_close));close.setOnClickListener(v->closeBlockedApp());root.addView(close);
            WindowManager wm=appWm();wm.addView(root,blockParams(appWindowType()));fallbackView=root;blockWm=wm;blockAttached=true;state=OverlayState.BLOCK;DebugLog.add("OVERLAY programmatic fallback attached package="+name+" lockdown="+(com.sakib.portebo.lockdown.LockdownManager.isLockdownActive(appContext)?"yes":"no"));if(attached!=null)root.post(attached);
        }catch(Exception ex){DebugLog.add("OVERLAY fallback failed package="+name+" exception="+ex.getClass().getSimpleName()+": "+ex.getMessage());}
    }
    public void showGreylistExpiredOverlay(String name,String warning,Runnable attached){showBlacklistOverlay(name+" (টাইম শেষ)",warning,attached);}
    public void showGreylistExpiredOverlay(String name,String warning){showGreylistExpiredOverlay(name,warning,null);}
    public void showFloatingTimerBubble(AppItem app){main.post(()->{
        if(bubble!=null){updateBubbleInfo(app);return;}
        try{
            Context ctx=service!=null?service:appContext;ContextThemeWrapper themed=new ContextThemeWrapper(ctx,ThemePaletteManager.getSelectedThemeStyle(appContext));View v=LayoutInflater.from(themed).inflate(R.layout.overlay_floating_bubble,null);
            TextView timer=v.findViewById(R.id.tvBubbleTimer),label=v.findViewById(R.id.tvBubbleAppName);if(label!=null)label.setText(app.getName());if(timer!=null)setTimer(timer,app.getQuotaSeconds()-app.getUsedSeconds());
            WindowManager.LayoutParams p=new WindowManager.LayoutParams(-2,-2,Build.VERSION.SDK_INT>=26?WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY:WindowManager.LayoutParams.TYPE_PHONE,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,PixelFormat.TRANSLUCENT);p.gravity=Gravity.TOP|Gravity.START;p.x=PrefsManager.getBubbleX();p.y=PrefsManager.getBubbleY();
            v.setOnTouchListener(new View.OnTouchListener(){int x,y;float tx,ty;public boolean onTouch(View view,MotionEvent e){if(e.getAction()==MotionEvent.ACTION_DOWN){x=bubbleParams.x;y=bubbleParams.y;tx=e.getRawX();ty=e.getRawY();return true;}if(e.getAction()==MotionEvent.ACTION_MOVE){bubbleParams.x=x+(int)(e.getRawX()-tx);bubbleParams.y=y+(int)(e.getRawY()-ty);try{bubbleWm.updateViewLayout(view,bubbleParams);}catch(Exception ignored){}return true;}if(e.getAction()==MotionEvent.ACTION_UP){PrefsManager.saveBubblePosition(bubbleParams.x,bubbleParams.y);return true;}return true;}});
            WindowManager swm=serviceWm();try{if(swm==null)throw new SecurityException("service window manager unavailable");swm.addView(v,p);bubbleWm=swm;}catch(WindowManager.BadTokenException|SecurityException first){DebugLog.add("BUBBLE accessibility add failed package="+app.getPackageName()+" exception="+first.getClass().getSimpleName());WindowManager awm=appWm();WindowManager.LayoutParams ap=new WindowManager.LayoutParams(-2,-2,appWindowType(),p.flags,p.format);ap.gravity=p.gravity;ap.x=p.x;ap.y=p.y;awm.addView(v,ap);bubbleParams=ap;bubbleWm=awm;}
            if(bubbleParams==null)bubbleParams=p;bubble=v;state=OverlayState.BUBBLE;DebugLog.add("BUBBLE attached package="+app.getPackageName());
        }catch(Exception ex){DebugLog.add("BUBBLE failed exception="+ex.getClass().getSimpleName()+": "+ex.getMessage());}
    });}
    private void setTimer(TextView t,int sec){int n=Math.max(0,sec);t.setText(String.format(java.util.Locale.US,"%02d:%02d",n/60,n%60));}
    private void updateBubbleInfo(AppItem a){if(bubble==null)return;TextView label=bubble.findViewById(R.id.tvBubbleAppName),t=bubble.findViewById(R.id.tvBubbleTimer);if(label!=null)label.setText(a.getName());if(t!=null)setTimer(t,a.getQuotaSeconds()-a.getUsedSeconds());}
    public void updateBubbleTimer(int remaining){main.post(()->{if(bubble!=null){TextView t=bubble.findViewById(R.id.tvBubbleTimer);if(t!=null)setTimer(t,remaining);}});}
    public void hideFloatingBubble(){main.post(()->{if(bubble!=null){try{if(bubbleWm!=null)bubbleWm.removeView(bubble);}catch(Exception ignored){}bubble=null;bubbleWm=null;bubbleParams=null;if(state==OverlayState.BUBBLE)state=OverlayState.NONE;}});}
    public void hideBlockOverlay(){main.post(()->{if(fallbackView!=null){try{if(blockWm!=null)blockWm.removeView(fallbackView);}catch(Exception ignored){}fallbackView=null;}if(blockAttached&&blockView!=null){try{if(blockWm!=null)blockWm.removeView(blockView);}catch(Exception ignored){}blockAttached=false;}blockWm=null;if(state==OverlayState.BLOCK)state=OverlayState.NONE;});}
    public void cleanup(){hideBlockOverlay();hideFloatingBubble();}
    private void closeBlockedApp(){
        boolean moved=false;
        try{if(service!=null)moved=service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME);}catch(Exception ignored){}
        if(!moved){try{Intent home=new Intent(Intent.ACTION_MAIN);home.addCategory(Intent.CATEGORY_HOME);home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);appContext.startActivity(home);moved=true;}catch(Exception ex){DebugLog.add("OVERLAY close-to-home failed exception="+ex.getClass().getSimpleName());}}
        if(moved){DebugLog.add("OVERLAY close requested; moved to launcher");main.postDelayed(this::hideBlockOverlay,350);}else DebugLog.add("OVERLAY close could not open launcher");
    }
}
