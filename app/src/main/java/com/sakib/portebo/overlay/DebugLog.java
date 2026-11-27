package com.sakib.portebo.overlay;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
public final class DebugLog {
    private static final int LIMIT=100;
    private static final ArrayDeque<String> LINES=new ArrayDeque<>();
    private DebugLog(){}
    public static synchronized void add(String line){String stamp=new SimpleDateFormat("HH:mm:ss.SSS",Locale.getDefault()).format(new Date());if(LINES.size()>=LIMIT)LINES.removeFirst();LINES.addLast(stamp+"  "+line);}
    public static synchronized String snapshot(){StringBuilder b=new StringBuilder();for(String s:LINES){if(b.length()>0)b.append('\n');b.append(s);}return b.toString();}
    public static synchronized void clear(){LINES.clear();}
}
