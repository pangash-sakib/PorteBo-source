package com.sakib.portebo.lockdown;

import android.content.Context;
import android.content.SharedPreferences;
import com.sakib.portebo.service.AppMonitorService;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

public final class LockdownManager {
    public enum LockdownSource { NONE, QUICK, DAILY_ROUTINE, EXAM_ROUTINE }
    private static final String LOCK="portebo_lockdown", ROUTINE="routine_data";
    private LockdownManager() { }

    public static boolean isLockdownActive() {
        Context c=com.sakib.portebo.PorteBoApp.getInstance();
        return c!=null && isLockdownActive(c);
    }
    public static boolean isLockdownActive(Context c) { return getLockdownEndTimestamp(c)>System.currentTimeMillis(); }
    public static long getRemainingSeconds() {
        Context c=com.sakib.portebo.PorteBoApp.getInstance();
        return c==null?0:getRemainingSeconds(c);
    }
    public static long getRemainingSeconds(Context c) { return Math.max(0,(getLockdownEndTimestamp(c)-System.currentTimeMillis())/1000L); }
    public static long getLockdownEndTimestamp() {
        Context c=com.sakib.portebo.PorteBoApp.getInstance();
        return c==null?0:getLockdownEndTimestamp(c);
    }
    /** Always reads the current persisted quick, daily and exam schedules. */
    public static long getLockdownEndTimestamp(Context c) {
        if(c==null)return 0;
        Context app=c.getApplicationContext();long now=System.currentTimeMillis();
        long end=app.getSharedPreferences(LOCK,Context.MODE_PRIVATE).getLong("end_timestamp",0L);
        SharedPreferences r=app.getSharedPreferences(ROUTINE,Context.MODE_PRIVATE);
        if(r.getBoolean("daily_enabled",false))end=Math.max(end,dailyWindowEnd(now,r.getString("start","18:00"),r.getString("end","22:00")));
        end=Math.max(end,examWindowEnd(now,r.getString("exams","[]")));
        return end>now?end:0;
    }
    public static LockdownSource getActiveSource() {
        Context c=com.sakib.portebo.PorteBoApp.getInstance();
        if(c==null)return LockdownSource.NONE;
        long now=System.currentTimeMillis();
        long quick=c.getSharedPreferences(LOCK,0).getLong("end_timestamp",0L);
        if(quick>now)return LockdownSource.valueOf(c.getSharedPreferences(LOCK,0).getString("lockdown_source","QUICK"));
        SharedPreferences r=c.getSharedPreferences(ROUTINE,0);
        if(r.getBoolean("daily_enabled",false)&&dailyWindowEnd(now,r.getString("start","18:00"),r.getString("end","22:00"))>now)return LockdownSource.DAILY_ROUTINE;
        return examWindowEnd(now,r.getString("exams","[]"))>now?LockdownSource.EXAM_ROUTINE:LockdownSource.NONE;
    }
    private static long dailyWindowEnd(long now,String start,String finish) {
        try {
            int[] a=parse(start),b=parse(finish);Calendar base=Calendar.getInstance();base.setTimeInMillis(now);
            for(int back=0;back<=1;back++){
                Calendar s=(Calendar)base.clone();s.add(Calendar.DAY_OF_YEAR,-back);s.set(Calendar.HOUR_OF_DAY,a[0]);s.set(Calendar.MINUTE,a[1]);s.set(Calendar.SECOND,0);s.set(Calendar.MILLISECOND,0);
                Calendar e=(Calendar)s.clone();e.set(Calendar.HOUR_OF_DAY,b[0]);e.set(Calendar.MINUTE,b[1]);if(b[0]*60+b[1]<=a[0]*60+a[1])e.add(Calendar.DAY_OF_YEAR,1);
                if(now>=s.getTimeInMillis()&&now<e.getTimeInMillis())return e.getTimeInMillis();
            }
        }catch(Exception ignored){}
        return 0;
    }
    private static long examWindowEnd(long now,String json) {
        try {
            JSONArray a=new JSONArray(json);SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd",Locale.US);f.setLenient(false);
            for(int i=0;i<a.length();i++){
                JSONObject o=a.optJSONObject(i);if(o==null)continue;Date date=f.parse(o.optString("date",""));if(date==null)continue;
                Calendar s=Calendar.getInstance();s.setTime(date);s.add(Calendar.DAY_OF_YEAR,-1);s.set(Calendar.HOUR_OF_DAY,18);s.set(Calendar.MINUTE,0);s.set(Calendar.SECOND,0);s.set(Calendar.MILLISECOND,0);
                Calendar e=(Calendar)s.clone();e.add(Calendar.HOUR_OF_DAY,4);
                if(now>=s.getTimeInMillis()&&now<e.getTimeInMillis())return e.getTimeInMillis();
            }
        }catch(Exception ignored){}
        return 0;
    }
    private static int[] parse(String s){String[] p=s.split(":");if(p.length!=2)throw new IllegalArgumentException();return new int[]{Math.max(0,Math.min(23,Integer.parseInt(p[0]))),Math.max(0,Math.min(59,Integer.parseInt(p[1])))};}
    public static void startLockdown(Context c,int minutes,LockdownSource source) {
        if(c==null||minutes<=0)return;
        long end=System.currentTimeMillis()+minutes*60L*1000L;
        c.getApplicationContext().getSharedPreferences(LOCK,Context.MODE_PRIVATE).edit().putLong("end_timestamp",end).putString("lockdown_source",source==null?LockdownSource.QUICK.name():source.name()).apply();
        AppMonitorService.checkImmediateForegroundLock(c);
    }
}
