package com.sakib.portebo.message;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.os.Handler;
import android.os.Looper;
import com.sakib.portebo.AppConfig;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
public final class MessageManager {
    private static final String PREF="portebo_messages"; private static final ExecutorService IO=Executors.newSingleThreadExecutor(); private static final Handler MAIN=new Handler(Looper.getMainLooper());
    private static long lastLikePollAt; private static boolean likePollInFlight;
    private static synchronized String getOrCreateSenderId(Context context){SharedPreferences p=context.getApplicationContext().getSharedPreferences(PREF,0);String id=p.getString("anonymous_sender_id","");if(id==null||id.isEmpty()){id=java.util.UUID.randomUUID().toString();p.edit().putString("anonymous_sender_id",id).apply();}return id;}
    private static String senderFromMessageId(String id){if(id==null)return "";int split=id.indexOf("__");return split>0?id.substring(0,split):"";}
    private MessageManager(){}
    public interface Result { void done(boolean ok,String message); }
    public interface AuthResult { void done(boolean ok,String message); }
    public static class MessageItem {public String id,title,content,date,category,senderId;public boolean liked,hasImage;public MessageItem(String i,String t,String c,String d){id=i;title=t;content=c;date=d;category="Update";}public MessageItem(String i,String t,String c,String d,String cat){this(i,t,c,d);category=cat;}}
    private static String db(String path){String base=AppConfig.FIREBASE_DB_URL;if(base==null||base.equals("PASTE_HERE")||base.trim().isEmpty())return null;return base.replaceAll("/$","")+"/"+path+".json";}
    private static final int MAX_IMAGE_BASE64_CHARS=300000;
    public static void submitAnonymous(Context context,String category,String text,Result cb){submitAnonymous(context,category,text,null,cb);}
    public static void submitAnonymous(Context context,String category,String text,Uri imageUri,Result cb){
        Context app=context.getApplicationContext();IO.execute(()->{
            boolean ok=false;String msg="মেসেজ যায়নি। পরে আবার চেষ্টা করো।";HttpURLConnection c=null;
            try{
                String clean=text==null?"":text.trim();String cat=category==null?"":category.trim().toLowerCase(Locale.ROOT);
                if(clean.length()<1||clean.length()>1000||!(cat.equals("issue")||cat.equals("suggestion")||cat.equals("feedback")))throw new IllegalArgumentException("payload");
                String senderId=getOrCreateSenderId(app);String id=senderId+"__"+java.util.UUID.randomUUID().toString();String messageUrl=db("messages/"+id);if(messageUrl==null)throw new IllegalStateException("firebase config");
                String imageData=imageUri==null?null:compressIssueImage(app,imageUri);
                JSONObject payload=new JSONObject();payload.put("text",clean);payload.put("category",cat);payload.put("ts",System.currentTimeMillis());payload.put("liked",false);if(imageData!=null)payload.put("hasImage",true);
                URL target;if(imageData==null){target=new URL(messageUrl);}else{JSONObject updates=new JSONObject();updates.put("messages/"+id,payload);updates.put("feedbackImages/"+id,imageData);String base=AppConfig.FIREBASE_DB_URL.replaceAll("/$","");target=new URL(base+"/.json");}
                c=(HttpURLConnection)target.openConnection();c.setConnectTimeout(15000);c.setReadTimeout(20000);c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json; charset=UTF-8");
                if(imageData==null)c.setRequestMethod("PUT");else{c.setRequestMethod("POST");c.setRequestProperty("X-HTTP-Method-Override","PATCH");}
                try(OutputStream out=c.getOutputStream()){if(imageData==null)out.write(payload.toString().getBytes("UTF-8"));else{JSONObject updates=new JSONObject();updates.put("messages/"+id,payload);updates.put("feedbackImages/"+id,imageData);out.write(updates.toString().getBytes("UTF-8"));}}
                int code=c.getResponseCode();if(code>=200&&code<300){ok=true;msg=imageData==null?"মেসেজ পাঠানো হয়েছে।":"মেসেজ ও ছবি পাঠানো হয়েছে।";}else if(code==401||code==403)msg="Firebase Rules-এ ছবির অনুমতি যোগ করো (কোড "+code+")।";else msg="মেসেজ যায়নি (কোড "+code+")";
            }catch(Exception e){
                if(e instanceof java.net.UnknownHostException||e instanceof java.net.ConnectException||e instanceof java.net.NoRouteToHostException||e instanceof java.net.SocketException||e instanceof java.net.SocketTimeoutException)msg="ইন্টারনেট সংযোগ নেই। পরে আবার চেষ্টা করো।";
                else if(e instanceof IllegalStateException)msg="Firebase সেটিংস ঠিক নেই।";
                else if(e instanceof IllegalArgumentException&&"image_too_large".equals(e.getMessage()))msg="ছবিটি বেশি বড়। ছোট বা crop করা ছবি বেছে নাও।";
                else msg="মেসেজ বা ছবি পাঠানো যায়নি। পরে আবার চেষ্টা করো।";
            }finally{if(c!=null)c.disconnect();}
            final boolean f=ok;final String m=msg;if(cb!=null)MAIN.post(()->cb.done(f,m));
        });
    }
    private static String compressIssueImage(Context context,Uri uri)throws Exception{
        BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;try(java.io.InputStream in=context.getContentResolver().openInputStream(uri)){if(in==null)throw new java.io.IOException("image_open");BitmapFactory.decodeStream(in,null,bounds);}
        int longest=Math.max(bounds.outWidth,bounds.outHeight);if(longest<=0)throw new java.io.IOException("image_decode");int sample=1;while(longest/sample>1280)sample*=2;
        BitmapFactory.Options opts=new BitmapFactory.Options();opts.inSampleSize=sample;Bitmap bitmap;try(java.io.InputStream in=context.getContentResolver().openInputStream(uri)){if(in==null)throw new java.io.IOException("image_open");bitmap=BitmapFactory.decodeStream(in,null,opts);}if(bitmap==null)throw new java.io.IOException("image_decode");
        try{for(int scale=0;scale<6;scale++){for(int quality:new int[]{78,68,58,48}){java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();bitmap.compress(Bitmap.CompressFormat.JPEG,quality,out);byte[] data=out.toByteArray();if(((data.length+2L)/3L)*4L<=MAX_IMAGE_BASE64_CHARS)return Base64.encodeToString(data,Base64.NO_WRAP);}if(scale<5){int w=Math.max(1,(int)(bitmap.getWidth()*0.78f)),h=Math.max(1,(int)(bitmap.getHeight()*0.78f));Bitmap smaller=Bitmap.createScaledBitmap(bitmap,w,h,true);if(smaller!=bitmap)bitmap.recycle();bitmap=smaller;}}}finally{if(!bitmap.isRecycled())bitmap.recycle();}
        throw new IllegalArgumentException("image_too_large");
    }
    public static void signInWithPassword(Context context,String email,String password,AuthResult cb){
        Context app=context.getApplicationContext();IO.execute(()->{boolean ok=false;String msg="লগইন করা যায়নি।";HttpURLConnection c=null;
            try{
                if(AppConfig.FIREBASE_API_KEY==null||"PASTE_HERE".equals(AppConfig.FIREBASE_API_KEY))throw new IllegalStateException("key");
                URL u=new URL("https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key="+AppConfig.FIREBASE_API_KEY);c=(HttpURLConnection)u.openConnection();c.setRequestMethod("POST");c.setConnectTimeout(8000);c.setReadTimeout(8000);c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");
                JSONObject body=new JSONObject();body.put("email",email);body.put("password",password);body.put("returnSecureToken",true);try(OutputStream out=c.getOutputStream()){out.write(body.toString().getBytes("UTF-8"));}
                int code=c.getResponseCode();String response=read(c,code);
                if(code>=200&&code<300){JSONObject auth=new JSONObject(response);String idToken=auth.optString("idToken","");String refreshToken=auth.optString("refreshToken","");long expires=seconds(auth.optString("expiresIn","3600"));if(!idToken.isEmpty()&&!refreshToken.isEmpty()){app.getSharedPreferences("dev_auth",0).edit().putString("id_token",idToken).putString("refresh_token",refreshToken).putString("email",email.trim()).putLong("expires_at",System.currentTimeMillis()+expires*1000L).apply();ok=true;msg=idToken;}else msg="Firebase login token পাওয়া যায়নি।";
                }else{String err=new JSONObject(response).optJSONObject("error")!=null?new JSONObject(response).getJSONObject("error").optString("message",""):"";if(err.contains("EMAIL_NOT_FOUND")||err.contains("INVALID_PASSWORD")||err.contains("INVALID_LOGIN_CREDENTIALS"))msg="ইমেইল বা পাসওয়ার্ড ঠিক নয়।";else if(err.contains("INVALID_EMAIL"))msg="ইমেইল ঠিক নয়।";else if(err.contains("USER_DISABLED"))msg="এই অ্যাকাউন্ট বন্ধ আছে।";else if(err.contains("TOO_MANY_ATTEMPTS_TRY_LATER"))msg="অনেকবার চেষ্টা হয়েছে। একটু পরে আবার চেষ্টা করো।";else msg="লগইন করা যায়নি। Firebase সেটিংস দেখো।";}
            }catch(Exception e){msg="লগইন করা যায়নি। Firebase সেটিংস দেখো।";}finally{if(c!=null)c.disconnect();}
            final boolean result=ok;final String message=msg;if(cb!=null)MAIN.post(()->cb.done(result,message));
        });
    }
    private static long seconds(String value){try{return Math.max(60,Long.parseLong(value));}catch(Exception e){return 3600L;}}
    private static String refreshDevToken(Context context)throws Exception{
        Context app=context.getApplicationContext();SharedPreferences prefs=app.getSharedPreferences("dev_auth",0);String refresh=prefs.getString("refresh_token","");if(refresh.isEmpty())return prefs.getString("id_token","");
        if(AppConfig.FIREBASE_API_KEY==null||"PASTE_HERE".equals(AppConfig.FIREBASE_API_KEY))throw new IllegalStateException("Firebase API key missing");
        HttpURLConnection conn=null;try{URL url=new URL("https://securetoken.googleapis.com/v1/token?key="+AppConfig.FIREBASE_API_KEY);conn=(HttpURLConnection)url.openConnection();conn.setRequestMethod("POST");conn.setConnectTimeout(8000);conn.setReadTimeout(8000);conn.setDoOutput(true);conn.setRequestProperty("Content-Type","application/x-www-form-urlencoded");String form="grant_type=refresh_token&refresh_token="+java.net.URLEncoder.encode(refresh,"UTF-8");try(OutputStream out=conn.getOutputStream()){out.write(form.getBytes("UTF-8"));}int code=conn.getResponseCode();String response=read(conn,code);if(code<200||code>=300)throw new java.io.IOException("refresh_http_"+code);JSONObject auth=new JSONObject(response);String id=auth.optString("id_token","");String nextRefresh=auth.optString("refresh_token",refresh);if(id.isEmpty())throw new java.io.IOException("refresh_empty");long expires=seconds(auth.optString("expires_in","3600"));prefs.edit().putString("id_token",id).putString("refresh_token",nextRefresh).putLong("expires_at",System.currentTimeMillis()+expires*1000L).apply();return id;}finally{if(conn!=null)conn.disconnect();}
    }
    private static String getValidDevToken(Context context)throws Exception{
        Context app=context.getApplicationContext();SharedPreferences prefs=app.getSharedPreferences("dev_auth",0);String id=prefs.getString("id_token","");if(id.isEmpty())return "";String refresh=prefs.getString("refresh_token","");long expires=prefs.getLong("expires_at",0L);if(!refresh.isEmpty()&&(expires==0L||System.currentTimeMillis()+120000L>=expires))return refreshDevToken(app);return id;
    }
    public static void getFreshDevToken(Context context,AuthResult cb){Context app=context.getApplicationContext();IO.execute(()->{boolean ok=false;String result="ডেভেলপার লগইন মেয়াদ শেষ। আবার লগইন করো।";try{String token=getValidDevToken(app);if(!token.isEmpty()){ok=true;result=token;}}catch(Exception ignored){}final boolean success=ok;final String message=result;if(cb!=null)MAIN.post(()->cb.done(success,message));});}
    public static List<MessageItem> getAnnouncements(Context context){return getHistory(context);}
    public static List<MessageItem> getHistory(Context context){SharedPreferences p=context.getSharedPreferences(PREF,0);List<MessageItem> out=new ArrayList<>();try{JSONArray a=new JSONArray(p.getString("history","[]"));for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);out.add(new MessageItem(o.optString("id"),o.optString("title"),o.optString("content"),o.optString("date"),o.optString("category","Update")));}}catch(Exception ignored){}return out;}
    public static synchronized void addHistory(Context c,String id,String title,String body,String category){addHistory(c,id,title,body,category,new SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.getDefault()).format(new Date()));}
    private static synchronized void addHistory(Context c,String id,String title,String body,String category,String date){try{List<MessageItem> old=getHistory(c);for(MessageItem x:old)if(id.equals(x.id))return;JSONArray a=new JSONArray();JSONObject o=new JSONObject();o.put("id",id);o.put("title",title);o.put("content",body);o.put("category",category);o.put("date",date);a.put(o);for(MessageItem x:old){if(id.equals(x.id))continue;JSONObject j=new JSONObject();j.put("id",x.id);j.put("title",x.title);j.put("content",x.content);j.put("category",x.category);j.put("date",x.date);a.put(j);if(a.length()>=100)break;}SharedPreferences pref=c.getSharedPreferences(PREF,0);int unread=pref.getInt("unread",old.size());pref.edit().putString("history",a.toString()).putInt("unread",unread+1).apply();}catch(Exception ignored){}}
    public static int getUnreadCount(Context c){return c.getSharedPreferences(PREF,0).getInt("unread",getHistory(c).size());}
    public static void markAllAsRead(Context c){c.getSharedPreferences(PREF,0).edit().putInt("unread",0).apply();}
    public static void clearUnreadCount(Context c){markAllAsRead(c);}
    public static void refreshReleases(Context c){IO.execute(()->{HttpURLConnection conn=null;try{URL u=new URL("https://api.github.com/repos/"+AppConfig.GITHUB_OWNER+"/"+AppConfig.GITHUB_REPO+"/releases?per_page=100");conn=(HttpURLConnection)u.openConnection();conn.setConnectTimeout(7000);conn.setReadTimeout(7000);conn.setRequestProperty("Accept","application/vnd.github+json");if(conn.getResponseCode()!=200)return;JSONArray a=new JSONArray(read(conn,200));for(int i=a.length()-1;i>=0;i--){JSONObject r=a.getJSONObject(i);String tag=r.optString("tag_name");String title=r.optString("name",tag);String body=r.optString("body","");String date=r.optString("published_at","");String id="release_"+tag;addHistory(c,id,title,body,"Update",date);}}catch(Exception ignored){}finally{if(conn!=null)conn.disconnect();}});}
    public interface InboxResult { void done(List<MessageItem> items,int httpCode,boolean noInternet); }
    private static final class InboxFetch{List<MessageItem> items=new ArrayList<>();int code;boolean offline;}
    private static InboxFetch fetchInbox(String idToken){InboxFetch result=new InboxFetch();HttpURLConnection c=null;try{String u=db("messages");if(u==null)return result;c=(HttpURLConnection)new URL(u+"?auth="+java.net.URLEncoder.encode(idToken==null?"":idToken,"UTF-8")).openConnection();c.setConnectTimeout(8000);c.setReadTimeout(8000);result.code=c.getResponseCode();if(result.code==200){JSONObject root=new JSONObject(read(c,result.code));for(java.util.Iterator<String> it=root.keys();it.hasNext();){String id=it.next();JSONObject x=root.optJSONObject(id);if(x==null)continue;MessageItem item=new MessageItem(id,x.optString("category"),x.optString("text"),String.valueOf(x.optLong("ts")),x.optString("category"));item.senderId=x.optString("senderId",senderFromMessageId(id));item.liked=x.optBoolean("liked",false);item.hasImage=x.optBoolean("hasImage",false);result.items.add(item);}}}catch(Exception e){result.offline=e instanceof java.net.UnknownHostException||e instanceof java.net.ConnectException||e instanceof java.net.NoRouteToHostException||e instanceof java.net.SocketException||e instanceof java.net.SocketTimeoutException;}finally{if(c!=null)c.disconnect();}java.util.Collections.sort(result.items,(a,b)->{try{return Long.compare(Long.parseLong(b.date),Long.parseLong(a.date));}catch(Exception e){return b.date.compareTo(a.date);}});return result;}
    private static void deliverInbox(InboxFetch result,InboxResult cb){if(cb!=null)MAIN.post(()->cb.done(result.items,result.code,result.offline));}
    public static void getDeveloperInboxAsync(String idToken,InboxResult cb){IO.execute(()->deliverInbox(fetchInbox(idToken),cb));}
    public static void getDeveloperInboxAsync(Context context,InboxResult cb){Context app=context.getApplicationContext();IO.execute(()->{InboxFetch result;try{String token=getValidDevToken(app);result=fetchInbox(token);if(result.code==401&&!app.getSharedPreferences("dev_auth",0).getString("refresh_token","").isEmpty()){String refreshed=refreshDevToken(app);result=fetchInbox(refreshed);}}catch(Exception e){result=new InboxFetch();boolean net=e instanceof java.net.UnknownHostException||e instanceof java.net.ConnectException||e instanceof java.net.NoRouteToHostException||e instanceof java.net.SocketException||e instanceof java.net.SocketTimeoutException;result.offline=net;result.code=net?0:401;}deliverInbox(result,cb);});}
    public interface ImageResult{void done(byte[] image,int httpCode,boolean noInternet);}
    private static final class ImageFetch{byte[] image;int code;boolean offline;}
    private static ImageFetch fetchFeedbackImage(String idToken,String messageId){ImageFetch result=new ImageFetch();HttpURLConnection c=null;try{String u=db("feedbackImages/"+messageId);if(u==null)return result;c=(HttpURLConnection)new URL(u+"?auth="+java.net.URLEncoder.encode(idToken==null?"":idToken,"UTF-8")).openConnection();c.setConnectTimeout(10000);c.setReadTimeout(15000);result.code=c.getResponseCode();if(result.code==200){Object value=new org.json.JSONTokener(read(c,result.code)).nextValue();if(value instanceof String&&((String)value).length()<=MAX_IMAGE_BASE64_CHARS)result.image=Base64.decode((String)value,Base64.DEFAULT);else result.code=422;}}catch(Exception e){result.offline=e instanceof java.net.UnknownHostException||e instanceof java.net.ConnectException||e instanceof java.net.NoRouteToHostException||e instanceof java.net.SocketException||e instanceof java.net.SocketTimeoutException;}finally{if(c!=null)c.disconnect();}return result;}
    public static void getFeedbackImageAsync(Context context,String messageId,ImageResult cb){Context app=context.getApplicationContext();IO.execute(()->{ImageFetch result;try{String token=getValidDevToken(app);result=fetchFeedbackImage(token,messageId);if(result.code==401&&!app.getSharedPreferences("dev_auth",0).getString("refresh_token","").isEmpty())result=fetchFeedbackImage(refreshDevToken(app),messageId);}catch(Exception e){result=new ImageFetch();boolean net=e instanceof java.net.UnknownHostException||e instanceof java.net.ConnectException||e instanceof java.net.NoRouteToHostException||e instanceof java.net.SocketException||e instanceof java.net.SocketTimeoutException;result.offline=net;result.code=net?0:401;}final ImageFetch f=result;if(cb!=null)MAIN.post(()->cb.done(f.image,f.code,f.offline));});}
    public static List<MessageItem> getDeveloperInbox(String idToken){List<MessageItem> out=new ArrayList<>();String u=db("messages");if(u==null||idToken==null)return out;HttpURLConnection c=null;try{c=(HttpURLConnection)new URL(u+"?auth="+java.net.URLEncoder.encode(idToken,"UTF-8")).openConnection();c.setConnectTimeout(7000);c.setReadTimeout(7000);int code=c.getResponseCode();if(code!=200)return out;JSONObject root=new JSONObject(read(c,code));for(java.util.Iterator<String> it=root.keys();it.hasNext();){String id=it.next();JSONObject x=root.optJSONObject(id);if(x!=null){MessageItem item=new MessageItem(id,x.optString("category"),x.optString("text"),String.valueOf(x.optLong("ts")),x.optString("category"));item.senderId=x.optString("senderId",senderFromMessageId(id));item.liked=x.optBoolean("liked",false);item.hasImage=x.optBoolean("hasImage",false);out.add(item);}}}catch(Exception ignored){}finally{if(c!=null)c.disconnect();}java.util.Collections.sort(out,(a,b)->{try{return Long.compare(Long.parseLong(b.date),Long.parseLong(a.date));}catch(Exception e){return b.date.compareTo(a.date);}});return out;}
    public static void likeMessage(String id,String token,Result cb){likeMessage(id,"","","",token,cb);}
    public static void likeMessage(String id,String senderId,String category,String token,Result cb){likeMessage(id,senderId,category,"",token,cb);}
    public static void likeMessage(String id,String senderId,String category,String original,String token,Result cb){
        IO.execute(()->{
            boolean ok=false;String msg="Like পাঠানো যায়নি।";HttpURLConnection c=null;
            try{
                String messageUrl=db("messages/"+id);if(messageUrl==null)throw new IllegalStateException("Firebase config");
                String normalizedCategory=category==null?"feedback":category.toLowerCase(Locale.ROOT);
                if(!normalizedCategory.equals("issue")&&!normalizedCategory.equals("suggestion")&&!normalizedCategory.equals("feedback"))normalizedCategory="feedback";
                String notice=likeNotice(normalizedCategory);
                JSONObject patch=new JSONObject();patch.put("liked",true);patch.put("likedAt",System.currentTimeMillis());
                c=(HttpURLConnection)new URL(messageUrl+"?auth="+java.net.URLEncoder.encode(token==null?"":token,"UTF-8")).openConnection();c.setRequestMethod("POST");c.setRequestProperty("X-HTTP-Method-Override","PATCH");c.setConnectTimeout(8000);c.setReadTimeout(8000);c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json; charset=UTF-8");
                try(OutputStream out=c.getOutputStream()){out.write(patch.toString().getBytes("UTF-8"));}
                int code=c.getResponseCode();c.disconnect();c=null;
                if(code<200||code>=300){msg="Like পাঠানো যায়নি (কোড "+code+")";}
                else{
                    String route=(senderId==null||senderId.isEmpty())?senderFromMessageId(id):senderId;
                    if(route.isEmpty()){ok=true;msg="Like দেওয়া হয়েছে। পুরোনো মেসেজে নোটিফিকেশন পাঠানো যাবে না।";}
                    else{
                        String responseUrl=db("likeResponses/"+route+"/"+id);
                        if(responseUrl==null)throw new IllegalStateException("Firebase config");
                        JSONObject note=new JSONObject();note.put("messageId",id);note.put("response",notice);note.put("category",normalizedCategory);note.put("originalMessage",original==null?"":original);note.put("createdAt",System.currentTimeMillis());
                        c=(HttpURLConnection)new URL(responseUrl+"?auth="+java.net.URLEncoder.encode(token==null?"":token,"UTF-8")).openConnection();c.setRequestMethod("PUT");c.setConnectTimeout(8000);c.setReadTimeout(8000);c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json; charset=UTF-8");
                        try(OutputStream out=c.getOutputStream()){out.write(note.toString().getBytes("UTF-8"));}
                        int responseCode=c.getResponseCode();ok=responseCode>=200&&responseCode<300;msg=ok?"Like আর নোটিফিকেশন পাঠানো হয়েছে।":"Like হয়েছে, নোটিফিকেশন যায়নি (কোড "+responseCode+")";
                    }
                }
            }catch(Exception e){if(e instanceof java.net.UnknownHostException||e instanceof java.net.ConnectException||e instanceof java.net.SocketException||e instanceof java.net.SocketTimeoutException)msg="ইন্টারনেট সংযোগ নেই। পরে আবার চেষ্টা করো।";else msg="Like বা নোটিফিকেশন পাঠানো যায়নি।";}
            finally{if(c!=null)c.disconnect();}
            final boolean f=ok;final String m=msg;if(cb!=null)MAIN.post(()->cb.done(f,m));
        });
    }
    private static String likeNotice(String category){
        if("issue".equals(category))return "Your issue has been noted.\nকিছুদিনের মধ্যে সমাধান করছি।";
        if("suggestion".equals(category))return "Thank you for your suggestion.";
        return "Thank you for your feedback.";
    }
    /** Fetches developer-like responses for this installation and adds unseen ones to bell history. */
    public static void pollLikeResponses(Context context){
        Context app=context.getApplicationContext();
        synchronized(MessageManager.class){long now=System.currentTimeMillis();if(likePollInFlight||now-lastLikePollAt<10000)return;likePollInFlight=true;lastLikePollAt=now;}
        IO.execute(()->{
            HttpURLConnection c=null;
            try{
                String sender=getOrCreateSenderId(app);String url=db("likeResponses/"+sender);if(url==null)return;
                c=(HttpURLConnection)new URL(url).openConnection();c.setConnectTimeout(7000);c.setReadTimeout(7000);int code=c.getResponseCode();if(code!=200)return;
                JSONObject root=new JSONObject(read(c,code));SharedPreferences prefs=app.getSharedPreferences(PREF,0);String seen=prefs.getString("seen_responses","");
                for(java.util.Iterator<String> it=root.keys();it.hasNext();){
                    String id=it.next();if(seen.contains("|"+id+"|"))continue;JSONObject item=root.optJSONObject(id);if(item==null)continue;
                    String category=item.optString("category","feedback").toLowerCase(Locale.ROOT);if(!category.equals("issue")&&!category.equals("suggestion")&&!category.equals("feedback"))category="feedback";
                    String original=item.optString("originalMessage","");String text=item.optString("response",likeNotice(category));if(!original.isEmpty())text+="\n\nতোমার মেসেজ:\n"+original;
                    addHistory(app,"like_"+id,"Your message has been received",text,category,String.valueOf(item.optLong("createdAt",System.currentTimeMillis())));seen+="|"+id+"|";
                }
                prefs.edit().putString("seen_responses",seen).apply();
            }catch(Exception ignored){}finally{if(c!=null)c.disconnect();synchronized(MessageManager.class){likePollInFlight=false;}}
        });
    }
    private static String read(HttpURLConnection c,int code)throws Exception{java.io.InputStream in=code>=400?c.getErrorStream():c.getInputStream();if(in==null)return "{}";BufferedReader r=new BufferedReader(new InputStreamReader(in,"UTF-8"));StringBuilder b=new StringBuilder();String l;while((l=r.readLine())!=null)b.append(l);r.close();return b.toString();}
}
