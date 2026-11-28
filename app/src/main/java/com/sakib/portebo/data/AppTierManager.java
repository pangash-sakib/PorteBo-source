package com.sakib.portebo.data;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Handler;
import android.os.HandlerThread;
import com.sakib.portebo.model.AppItem;
import com.sakib.portebo.security.ParentControlManager;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
public final class AppTierManager {
    private static final Map<String,AppItem> appMap=new LinkedHashMap<>();
    private static final Set<String> HOME_PACKAGES=new HashSet<>();
    private static final Set<String> PROTECTED=new HashSet<>(Arrays.asList("com.sakib.portebo","com.android.settings","com.google.android.dialer","com.android.dialer","com.android.phone","com.android.server.telecom","com.google.android.apps.messaging","com.android.mms","com.google.android.deskclock","com.android.deskclock","com.google.android.GoogleCamera","com.android.camera","com.android.camera2","com.google.android.contacts","com.android.contacts","com.android.systemui"));
    private static boolean initialized; private static HandlerThread saveThread; private static Handler saveHandler; private static Context appContext;
    private AppTierManager(){}
    public static synchronized void init(Context c){if(initialized)return;appContext=c.getApplicationContext();PrefsManager.init(appContext);loadSavedTiers();reconcileWithInstalledApps(appContext);initialized=true;}
    public static synchronized boolean isProtectedSystemApp(Context c,String pkg){if(pkg==null)return false;if(PROTECTED.contains(pkg)||HOME_PACKAGES.contains(pkg)||(c!=null&&pkg.equals(c.getPackageName())))return true;String p=pkg.toLowerCase(java.util.Locale.ROOT);return p.contains("systemui")||p.contains("settings")||p.contains("dialer")||p.contains("telecom")||p.contains("phone")||p.contains("contacts")||p.contains("messaging")||p.contains(".mms")||p.contains("camera")||p.contains("deskclock")||p.contains("clock");}
    public static synchronized List<AppItem> getAllApps(){return new ArrayList<>(appMap.values());} public static synchronized AppItem getApp(String p){return appMap.get(p);}
    public static synchronized void setTier(Context c,String pkg,AppItem.Tier tier){if(!ParentControlManager.canEdit(c))return;AppItem x=appMap.get(pkg);if(x==null||x.isProtected()||tier==null)return;x.setTier(tier);saveTiersDebounced(c);}
    public static synchronized void addApp(Context c,String pkg,String name){if(!ParentControlManager.canEdit(c)||appMap.containsKey(pkg))return;boolean p=isProtectedSystemApp(c,pkg);appMap.put(pkg,new AppItem(pkg,name,p?AppItem.Tier.WHITELIST:AppItem.Tier.GREYLIST,300,0,p));saveTiersDebounced(c);}
    public static synchronized void reconcileWithInstalledApps(Context c){
        if(c==null)return;PackageManager pm=c.getPackageManager();
        Intent home=new Intent(Intent.ACTION_MAIN);home.addCategory(Intent.CATEGORY_HOME);HOME_PACKAGES.clear();
        for(ResolveInfo r:pm.queryIntentActivities(home,0))if(r.activityInfo!=null)HOME_PACKAGES.add(r.activityInfo.packageName);
        Intent launch=new Intent(Intent.ACTION_MAIN);launch.addCategory(Intent.CATEGORY_LAUNCHER);
        Map<String,ResolveInfo> installed=new LinkedHashMap<>();
        for(ResolveInfo r:pm.queryIntentActivities(launch,0))if(r.activityInfo!=null)installed.putIfAbsent(r.activityInfo.packageName,r);
        // Do not purge saved data if Android returned no launcher apps unexpectedly.
        if(installed.isEmpty())return;
        appMap.keySet().retainAll(installed.keySet());
        for(Map.Entry<String,ResolveInfo> entry:installed.entrySet()){
            String pkg=entry.getKey();ResolveInfo resolve=entry.getValue();AppItem existing=appMap.get(pkg);
            if(existing==null){boolean prot=isProtectedSystemApp(c,pkg);String label=String.valueOf(resolve.loadLabel(pm));appMap.put(pkg,new AppItem(pkg,label,prot?AppItem.Tier.WHITELIST:AppItem.Tier.GREYLIST,300,PrefsManager.getGreylistUsedSeconds(pkg),prot));}
        }
        for(AppItem item:appMap.values())if(isProtectedSystemApp(c,item.getPackageName())){item.setProtected(true);item.setTier(AppItem.Tier.WHITELIST);}
        saveTiersDebounced(c);
    }
    private static void loadSavedTiers(){if(appContext==null)return;String s=appContext.getSharedPreferences("portebo_tiers",0).getString("tiers_json",null);if(s!=null){try{JSONArray a=new JSONArray(s);for(int n=0;n<a.length();n++){JSONObject o=a.getJSONObject(n);String p=o.getString("pkg");boolean prot=isProtectedSystemApp(appContext,p);AppItem.Tier t=prot?AppItem.Tier.WHITELIST:AppItem.Tier.valueOf(o.getString("tier"));AppItem item=new AppItem(p,o.optString("name",p),t,o.optInt("quota",300),PrefsManager.getGreylistUsedSeconds(p),prot);item.setMuted(o.optBoolean("muted",false));appMap.put(p,item);}}catch(Exception ignored){appMap.clear();}}if(appMap.isEmpty()){preset("com.google.android.calculator","Calculator",AppItem.Tier.WHITELIST,true);preset("com.adobe.reader","PDF reader",AppItem.Tier.WHITELIST,false);preset("com.google.android.apps.docs.editors.docs","Google Docs",AppItem.Tier.WHITELIST,false);preset("com.google.android.keep","Keep",AppItem.Tier.WHITELIST,false);preset("com.whatsapp","WhatsApp",AppItem.Tier.GREYLIST,false);preset("org.telegram.messenger","Telegram",AppItem.Tier.GREYLIST,false);preset("com.instagram.android","Instagram",AppItem.Tier.BLACKLIST,false);preset("com.facebook.katana","Facebook",AppItem.Tier.BLACKLIST,false);}}
    private static void preset(String p,String n,AppItem.Tier t,boolean ignored){boolean prot=isProtectedSystemApp(appContext,p);appMap.put(p,new AppItem(p,n,prot?AppItem.Tier.WHITELIST:t,300,PrefsManager.getGreylistUsedSeconds(p),prot));}
    public static synchronized void saveTiers(Context c){if(c!=null)writeSnapshot(c.getApplicationContext());}
    public static synchronized void saveTiersDebounced(Context c){if(c==null)return;if(saveThread==null){saveThread=new HandlerThread("tier-save");saveThread.start();saveHandler=new Handler(saveThread.getLooper());}saveHandler.removeCallbacksAndMessages(null);Context ac=c.getApplicationContext();saveHandler.postDelayed(()->{synchronized(AppTierManager.class){writeSnapshot(ac);}},250);}
    private static void writeSnapshot(Context c){try{JSONArray a=new JSONArray();for(AppItem x:appMap.values()){JSONObject o=new JSONObject();o.put("pkg",x.getPackageName());o.put("name",x.getName());o.put("tier",x.getTier().name());o.put("quota",x.getQuotaSeconds());o.put("muted",x.isMuted());a.put(o);}c.getSharedPreferences("portebo_tiers",0).edit().putString("tiers_json",a.toString()).apply();}catch(Exception ignored){}}
}