package com.servoc.app.chemcore.nextintegration;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.util.Base64;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.net.ssl.HttpsURLConnection;
import org.json.JSONObject;

/**
 * User-consented public GitHub Contents API sync. Runs only against a
 * validated internet Network, never ESP32's unvalidated local AP. A token is
 * read from AndroidKeyStore at request time; it is NOT part of this APK's
 * source and it is never logged or copied into a Feedback file.
 *
 * No fabricated remote receipt: SENT only after 201 with verified path, or
 * after authenticated 200 readback with SHA256 equal to the local payload.
 * Runs when app is open and connectivity is available; process-dead background
 * scheduling is NOT supplied by this class.
 */
public final class FeedbackGithubUploader {
  private FeedbackGithubUploader(){}
  public static final String REPO="HazRob/HazRob-Tank",BRANCH="main";
  private static final String API="https://api.github.com/repos/"+REPO+"/contents/";
  private static final AtomicBoolean uploading=new AtomicBoolean(false);
  private static volatile long nextAttemptElapsed=0L;
  private static String sha(String value)throws Exception{
    byte[] digest=MessageDigest.getInstance("SHA-256")
      .digest(value.getBytes(StandardCharsets.UTF_8));
    StringBuilder out=new StringBuilder(64);
    for(byte b:digest)out.append(String.format(Locale.ROOT,"%02x",b&255));
    return out.toString();
  }
  private static String originalPath(NextFeedbackQueue.Record r)throws Exception{
    JSONObject message=new JSONObject(r.payloadJson);
    long when=message.getLong("created_utc_ms");
    String canonical=message.getString("feedback_id");
    if(when<0||!canonical.equals(r.id)||!canonical.matches("FB-[a-f0-9-]{36}"))
      throw new IllegalStateException("Feedback id/time corrupted");
    SimpleDateFormat folder=new SimpleDateFormat("yyyy/MM",Locale.ROOT);
    folder.setTimeZone(TimeZone.getTimeZone("UTC"));
    return "feedback/records/"+folder.format(new Date(when))+"/"+canonical+".json";
  }
  private static HttpsURLConnection connection(Network network,String url,
      String token,String method)throws Exception{
    if(!url.startsWith(API))throw new IllegalArgumentException("Only feedback repo API permitted");
    HttpsURLConnection c=(HttpsURLConnection)network.openConnection(new URL(url));
    c.setRequestMethod(method);
    c.setInstanceFollowRedirects(false);
    c.setConnectTimeout(12000);c.setReadTimeout(16000);
    c.setRequestProperty("Authorization","Bearer "+token);
    c.setRequestProperty("Accept","application/vnd.github+json");
    c.setRequestProperty("X-GitHub-Api-Version","2022-11-28");
    c.setRequestProperty("User-Agent","HazRob-Tank-V8-Feedback");
    return c;
  }
  private static String body(InputStream in)throws Exception{
    if(in==null)return "";
    try(InputStream input=in;ByteArrayOutputStream out=new ByteArrayOutputStream()){
      byte[] block=new byte[4096];int n;int count=0;
      while((n=input.read(block))!=-1){
        count+=n;if(count>512000)throw new IllegalStateException("Oversized API response");
        out.write(block,0,n);
      }
      return new String(out.toByteArray(),StandardCharsets.UTF_8);
    }
  }
  /**
   * Idempotent existence check: never overwrite a different user's file.
   * A remote file with the exact content SHA256 may be accepted as a receipt.
   */
  private static String remoteReceiptIfSame(Network network,String path,String token,
                                             String expectedSha)throws Exception{
    HttpsURLConnection c=connection(network,API+path+"?ref="+BRANCH,token,"GET");
    try{
      int status=c.getResponseCode();
      if(status==404)return null;
      if(status!=200)throw new IllegalStateException("GITHUB_LOOKUP_HTTP_"+status);
      JSONObject data=new JSONObject(body(c.getInputStream()));
      if(!path.equals(data.optString("path","")))
        throw new IllegalStateException("GITHUB_FILE_ID_MISMATCH");
      String content=data.getString("content").replace("\n","").replace("\r","");
      String received=new String(Base64.decode(content,Base64.DEFAULT),StandardCharsets.UTF_8);
      if(!expectedSha.equals(sha(received)))
        throw new IllegalStateException("GITHUB_EXISTING_CONTENT_HASH_MISMATCH");
      String href=data.optString("html_url","");
      if(!href.startsWith("https://github.com/"+REPO+"/blob/"))
        throw new IllegalStateException("GITHUB_RECEIPT_HOST_MISMATCH");
      return href;
    }finally{c.disconnect();}
  }
  private static String write(Network network,NextFeedbackQueue.Record record,
                              String token)throws Exception{
    if(!record.publicConsent||!NextFeedbackQueue.PENDING_AUTH.equals(record.status))
      throw new IllegalArgumentException("Only explicitly consented feedback can leave phone");
    if(!sha(record.payloadJson).equals(record.payloadSha256))
      throw new IllegalStateException("LOCAL_FEEDBACK_SOURCE_HASH_CHANGED");
    String path=originalPath(record);
    String previously=remoteReceiptIfSame(network,path,token,record.payloadSha256);
    if(previously!=null)return previously;
    HttpsURLConnection c=connection(network,API+path,token,"PUT");
    c.setDoOutput(true);
    c.setRequestProperty("Content-Type","application/json; charset=utf-8");
    JSONObject put=new JSONObject();
    put.put("branch",BRANCH);
    put.put("message","feedback(v8): append original user-consented research observation "+record.id);
    // GitHub stores the same UTF-8 bytes as the local SHA-256 digest.
    put.put("content",Base64.encodeToString(
      record.payloadJson.getBytes(StandardCharsets.UTF_8),Base64.NO_WRAP));
    try{
      try(OutputStream os=c.getOutputStream()){
        os.write(put.toString().getBytes(StandardCharsets.UTF_8));
      }
      int status=c.getResponseCode();
      if(status==409||status==422){
        String present=remoteReceiptIfSame(network,path,token,record.payloadSha256);
        if(present!=null)return present;
        throw new IllegalStateException("GITHUB_FEEDBACK_CONFLICT_HTTP_"+status);
      }
      if(status!=201)throw new IllegalStateException("GITHUB_WRITE_HTTP_"+status);
      JSONObject data=new JSONObject(body(c.getInputStream()));
      JSONObject file=data.getJSONObject("content");
      JSONObject commit=data.getJSONObject("commit");
      if(!path.equals(file.optString("path"))||
         !commit.optString("sha","").matches("[a-fA-F0-9]{40}"))
        throw new IllegalStateException("GITHUB_REMOTE_WRITE_UNVERIFIED");
      String url=file.optString("html_url","");
      if(!url.startsWith("https://github.com/"+REPO+"/blob/"))
        throw new IllegalStateException("GITHUB_FEEDBACK_RECEIPT_UNVERIFIED");
      return url;
    }finally{c.disconnect();}
  }
  private static Network internet(Context context){
    ConnectivityManager cm=(ConnectivityManager)context.getSystemService(
       Context.CONNECTIVITY_SERVICE);
    if(cm==null)return null;
    for(Network n:cm.getAllNetworks()){
      NetworkCapabilities cap=cm.getNetworkCapabilities(n);
      if(cap!=null&&cap.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)&&
         cap.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
        return n;
    }
    return null;
  }
  public static void submitWhenAvailable(Context context,Executor executor){
    if(context==null||executor==null||!FeedbackGithubCredential.isConfigured(context))
      return;
    if(!uploading.compareAndSet(false,true))return;
    executor.execute(()->{
      try{
        if(android.os.SystemClock.elapsedRealtime()<nextAttemptElapsed)return;
        Context app=context.getApplicationContext();
        Network net=internet(app);
        if(net==null)return;
        String token=FeedbackGithubCredential.forAuthenticatedRequest(app);
        if(token==null)return;
        try(NextFeedbackQueue queue=new NextFeedbackQueue(app)){
          List<NextFeedbackQueue.Record> batch=queue.pending(10);
          for(var item:batch){
            try{
              String receipt=write(net,item,token);
              if(!queue.confirmRemoteReceipt(item.id,receipt,item.payloadSha256))
                throw new IllegalStateException("LOCAL_UPLOAD_RECEIPT_REJECTED");
            }catch(Exception failure){
              String code=failure instanceof IllegalStateException?
                failure.getMessage():"NETWORK_OR_AUTH_ERROR_"+failure.getClass().getSimpleName();
              queue.recordFailure(item.id,code);
              nextAttemptElapsed=android.os.SystemClock.elapsedRealtime()+60000L;
              break; // Rate-limit retry, never storm the GitHub API.
            }
          }
        }
      }catch(Exception ignored){
        nextAttemptElapsed=android.os.SystemClock.elapsedRealtime()+60000L;
      }finally{uploading.set(false);}
    });
  }
}
