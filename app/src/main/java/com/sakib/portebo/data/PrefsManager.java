package com.sakib.portebo.data;
import android.content.Context;
import android.content.SharedPreferences;
import com.sakib.portebo.security.ParentControlManager;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
public final class PrefsManager {
    private static final String PREF_NAME = "portebo_prefs";
    private static SharedPreferences prefs;
    private static Context appContext;
    private PrefsManager() { }
    public static synchronized void init(Context context) {
        if (prefs == null) { appContext = context.getApplicationContext(); prefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE); }
        checkDailyMidnightReset();
    }
    public static com.sakib.portebo.model.DailyRoutine getDailyRoutine(Context context) { SharedPreferences r=context.getApplicationContext().getSharedPreferences("routine_data",Context.MODE_PRIVATE);return new com.sakib.portebo.model.DailyRoutine(r.getBoolean("daily_enabled",false),r.getString("start","18:00"),r.getString("end","22:00")); }
    public static void saveDailyRoutine(Context context,com.sakib.portebo.model.DailyRoutine routine) { if(context==null||routine==null||!ParentControlManager.canEdit(context))return;context.getApplicationContext().getSharedPreferences("routine_data",Context.MODE_PRIVATE).edit().putBoolean("daily_enabled",routine.isEnabled()).putString("start",routine.getStartTime()).putString("end",routine.getEndTime()).apply(); }
    public static String getCurrentDayKey() { return new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date()); }
    private static void checkDailyMidnightReset() {
        if (prefs == null) return;
        String today = getCurrentDayKey();
        if (!today.equals(prefs.getString("last_reset_date", ""))) {
            SharedPreferences.Editor e = prefs.edit();
            for (String key : prefs.getAll().keySet()) if (key.startsWith("greylist_used_")) e.remove(key);
            e.putString("last_reset_date", today).apply();
        }
    }
    public static void saveGreylistUsedSeconds(String pkg, int seconds) { if (prefs != null && pkg != null) { checkDailyMidnightReset(); prefs.edit().putInt("greylist_used_"+pkg, Math.max(0,seconds)).apply(); } }
    public static int getGreylistUsedSeconds(String pkg) { if (prefs == null || pkg == null) return 0; checkDailyMidnightReset(); return Math.max(0,prefs.getInt("greylist_used_"+pkg,0)); }
    public static void saveBubblePosition(int x,int y) { if(prefs!=null)prefs.edit().putInt("bubble_x",x).putInt("bubble_y",y).apply(); }
    public static int getBubbleX(){return prefs!=null?prefs.getInt("bubble_x",100):100;} public static int getBubbleY(){return prefs!=null?prefs.getInt("bubble_y",300):300;}
    public static String getGreylistWarning(){return prefs!=null?prefs.getString("warning_greylist","টাইম শেষ। এবার পড়তে বসো।"):"টাইম শেষ। এবার পড়তে বসো।";}
    public static void saveGreylistWarning(String s){if(prefs!=null&&ParentControlManager.canEdit(appContext))prefs.edit().putString("warning_greylist",s).apply();}
    public static String getBlacklistWarning(){return prefs!=null?prefs.getString("warning_blacklist","এই অ্যাপ লকডাউনে বন্ধ।"):"এই অ্যাপ লকডাউনে বন্ধ।";}
    public static void saveBlacklistWarning(String s){if(prefs!=null&&ParentControlManager.canEdit(appContext))prefs.edit().putString("warning_blacklist",s).apply();}
    public static String getThemePaletteId(){return prefs!=null?prefs.getString("theme_palette_id","red"):"red";}
    public static void setThemePaletteId(String id){if(prefs!=null&&id!=null&&!id.isEmpty()&&ParentControlManager.canEdit(appContext))prefs.edit().putString("theme_palette_id",id).apply();}
    public static boolean isDarkMode(){return prefs==null || prefs.getBoolean("dark_mode",true);} public static void setDarkMode(boolean b){if(prefs!=null&&ParentControlManager.canEdit(appContext))prefs.edit().putBoolean("dark_mode",b).apply();}
    public static boolean isDevModeEnabled(){return prefs!=null&&prefs.getBoolean("dev_mode_unlocked",false);} public static void setDevModeEnabled(boolean b){if(prefs!=null)prefs.edit().putBoolean("dev_mode_unlocked",b).apply();}
    public static long getLastFeedbackMillis(){return prefs!=null?prefs.getLong("feedback_last_ms",0):0;} public static void recordFeedback(long now){if(prefs!=null){String day=new SimpleDateFormat("yyyyMMdd",Locale.US).format(new Date(now));String old=prefs.getString("feedback_day","");int count=day.equals(old)?prefs.getInt("feedback_count",0):0;prefs.edit().putLong("feedback_last_ms",now).putString("feedback_day",day).putInt("feedback_count",count+1).apply();}}
    public static int getFeedbackCountToday(){if(prefs==null)return 0;String day=new SimpleDateFormat("yyyyMMdd",Locale.US).format(new Date());return day.equals(prefs.getString("feedback_day",""))?prefs.getInt("feedback_count",0):0;}
}
