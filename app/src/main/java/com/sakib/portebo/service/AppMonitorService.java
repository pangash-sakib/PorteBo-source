package com.sakib.portebo.service;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import com.sakib.portebo.data.AppTierManager;
import com.sakib.portebo.data.PrefsManager;
import com.sakib.portebo.lockdown.LockdownManager;
import com.sakib.portebo.model.AppItem;
import com.sakib.portebo.overlay.DebugLog;
import com.sakib.portebo.overlay.OverlayManager;

public class AppMonitorService extends AccessibilityService {
    private static AppMonitorService instance;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private String stablePkg,pendingPkg,activePkg,activeDay;
    private volatile boolean ready;
    private AppItem activeApp;
    private long sessionStarted,usedMillis;
    private int ticks;
    private final Runnable stabilize=this::onStableForeground;
    private final Runnable tick=new Runnable(){@Override public void run(){
        if(activeApp==null)return;
        long now=SystemClock.elapsedRealtime();String day=PrefsManager.getCurrentDayKey();
        if(activeDay!=null&&!activeDay.equals(day)){usedMillis=0;activeApp.setUsedSeconds(0);PrefsManager.saveGreylistUsedSeconds(activePkg,0);}
        activeDay=day;usedMillis=Math.min(activeApp.getQuotaSeconds()*1000L,usedMillis+Math.max(0,now-sessionStarted));sessionStarted=now;
        int used=(int)(usedMillis/1000L);activeApp.setUsedSeconds(Math.min(activeApp.getQuotaSeconds(),used));
        int remain=Math.max(0,activeApp.getQuotaSeconds()-activeApp.getUsedSeconds());OverlayManager.getInstance(AppMonitorService.this).updateBubbleTimer(remain);
        if(++ticks%5==0)PrefsManager.saveGreylistUsedSeconds(activePkg,activeApp.getUsedSeconds());
        if(remain<=0){String pkg=activePkg,name=activeApp.getName();PrefsManager.saveGreylistUsedSeconds(pkg,activeApp.getQuotaSeconds());DebugLog.add("QUOTA expired package="+pkg+" tier=GREYLIST lockdown="+(LockdownManager.isLockdownActive(AppMonitorService.this)?"yes":"no"));activeApp=null;activePkg=null;OverlayManager o=OverlayManager.getInstance(AppMonitorService.this);o.hideFloatingBubble();o.showGreylistExpiredOverlay(name,PrefsManager.getGreylistWarning(),()->homeAfterOverlay("quota",pkg));return;}
        handler.postDelayed(this,1000);
    }};
    @Override public void onCreate(){super.onCreate();instance=this;}
    @Override protected void onServiceConnected(){super.onServiceConnected();instance=this;new Thread(()->{PrefsManager.init(this);AppTierManager.init(this);handler.post(()->{ready=true;checkImmediateForegroundLock(this);if(pendingPkg!=null)onStableForeground();});},"portebo-accessibility-init").start();}
    @Override public void onAccessibilityEvent(AccessibilityEvent e){
        if(e==null)return;int type=e.getEventType();
        if(type!=AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED&&type!=AccessibilityEvent.TYPE_WINDOWS_CHANGED)return;
        CharSequence packageName=e.getPackageName();String pkg=packageName==null?null:packageName.toString();
        if(pkg==null||pkg.isEmpty()){try{AccessibilityNodeInfo root=getRootInActiveWindow();if(root!=null&&root.getPackageName()!=null)pkg=root.getPackageName().toString();}catch(Exception ignored){}}
        if(pkg==null||pkg.isEmpty())return;DebugLog.add("EVENT type="+AccessibilityEvent.eventTypeToString(type)+" package="+pkg);
        if(isTransient(pkg))return;
        if(!pkg.equals(pendingPkg)){pendingPkg=pkg;handler.removeCallbacks(stabilize);handler.postDelayed(stabilize,250);}
    }
    private boolean isTransient(String p){return p.equals(getPackageName())||p.contains("systemui")||p.contains("inputmethod")||p.contains("permissioncontroller")||p.contains("packageinstaller");}
    private void onStableForeground(){
        if(!ready)return;String pkg=pendingPkg;if(pkg==null)return;stablePkg=pkg;
        if(activeApp!=null&&!pkg.equals(activePkg))pauseGreylist();
        boolean locked=LockdownManager.isLockdownActive(this);OverlayManager overlays=OverlayManager.getInstance(this);
        if(!locked||AppTierManager.isProtectedSystemApp(this,pkg)){DebugLog.add("CHECK package="+pkg+" tier="+(AppTierManager.isProtectedSystemApp(this,pkg)?"PROTECTED":"none")+" lockdown="+(locked?"yes":"no")+" overlay=none");overlays.hideBlockOverlay();overlays.hideFloatingBubble();return;}
        AppItem item=AppTierManager.getApp(pkg);if(item==null){DebugLog.add("CHECK package="+pkg+" tier=missing lockdown=yes overlay=none");overlays.hideBlockOverlay();overlays.hideFloatingBubble();return;}
        DebugLog.add("CHECK package="+pkg+" tier="+item.getTier().name()+" lockdown=yes");
        if(item.getTier()==AppItem.Tier.BLACKLIST){overlays.hideFloatingBubble();overlays.showBlacklistOverlay(item.getName(),PrefsManager.getBlacklistWarning(),()->homeAfterOverlay("blacklist",pkg));return;}
        if(item.getTier()==AppItem.Tier.GREYLIST){overlays.hideBlockOverlay();if(pkg.equals(activePkg)&&activeApp!=null)return;int used=PrefsManager.getGreylistUsedSeconds(pkg);item.setUsedSeconds(Math.min(item.getQuotaSeconds(),used));if(used>=item.getQuotaSeconds()){DebugLog.add("QUOTA already exhausted package="+pkg+" tier=GREYLIST lockdown=yes");overlays.hideFloatingBubble();overlays.showGreylistExpiredOverlay(item.getName(),PrefsManager.getGreylistWarning(),()->homeAfterOverlay("quota",pkg));return;}activeApp=item;activePkg=pkg;usedMillis=used*1000L;activeDay=PrefsManager.getCurrentDayKey();sessionStarted=SystemClock.elapsedRealtime();ticks=0;overlays.showFloatingTimerBubble(item);handler.removeCallbacks(tick);handler.postDelayed(tick,1000);return;}
        overlays.hideBlockOverlay();overlays.hideFloatingBubble();
    }
    private void homeAfterOverlay(String reason,String pkg){boolean ok=performGlobalAction(GLOBAL_ACTION_HOME);DebugLog.add("HOME after overlay reason="+reason+" package="+pkg+" result="+(ok?"yes":"no"));}
    private void pauseGreylist(){if(activeApp==null)return;handler.removeCallbacks(tick);long now=SystemClock.elapsedRealtime();usedMillis=Math.min(activeApp.getQuotaSeconds()*1000L,usedMillis+Math.max(0,now-sessionStarted));int used=(int)(usedMillis/1000L);activeApp.setUsedSeconds(Math.min(activeApp.getQuotaSeconds(),used));PrefsManager.saveGreylistUsedSeconds(activePkg,activeApp.getUsedSeconds());DebugLog.add("PAUSE package="+activePkg+" tier=GREYLIST used="+activeApp.getUsedSeconds()+" lockdown="+(LockdownManager.isLockdownActive(this)?"yes":"no"));activeApp=null;activePkg=null;activeDay=null;OverlayManager.getInstance(this).hideFloatingBubble();}
    public static void onQuotaChanged(Context c,String pkg){AppMonitorService s=instance;if(s==null||pkg==null)return;s.handler.post(()->{if(s.activeApp!=null&&pkg.equals(s.activePkg)){s.handler.removeCallbacks(s.tick);s.tick.run();}});}
    public static void checkImmediateForegroundLock(Context c){AppMonitorService s=instance;if(s==null||!LockdownManager.isLockdownActive(c))return;s.handler.post(()->{String pkg=s.stablePkg;try{AccessibilityNodeInfo root=s.getRootInActiveWindow();if(root!=null&&root.getPackageName()!=null)pkg=root.getPackageName().toString();}catch(Exception ignored){}if(pkg!=null){s.pendingPkg=pkg;s.onStableForeground();}});}
    @Override public void onInterrupt(){pauseGreylist();}
    @Override public void onDestroy(){pauseGreylist();OverlayManager.getInstance(this).cleanup();instance=null;super.onDestroy();}
    public static AppMonitorService getInstance(){return instance;}
}
