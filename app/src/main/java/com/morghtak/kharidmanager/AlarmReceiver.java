package com.morghtak.kharidmanager;
import android.app.*;import android.content.*;import android.media.*;import android.net.*;import android.os.*;import androidx.core.app.NotificationCompat;
public class AlarmReceiver extends BroadcastReceiver{
 public void onReceive(Context c,Intent i){String id=i.getStringExtra("id"),title=i.getStringExtra("title");NotificationManager nm=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);String ch="deadlines";if(Build.VERSION.SDK_INT>=26)nm.createNotificationChannel(new NotificationChannel(ch,"سررسیدها",NotificationManager.IMPORTANCE_HIGH));
  try{UriSound.play(c);}catch(Exception ignored){}
  Notification n=new NotificationCompat.Builder(c,ch).setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle("هشدار سررسید خرید").setContentText(title==null?"یک سررسید فرعی فرا رسیده است":title).setPriority(NotificationCompat.PRIORITY_MAX).setAutoCancel(true).setVibrate(new long[]{0,700,300,1000}).setDefaults(NotificationCompat.DEFAULT_SOUND).build();nm.notify(id==null?1:id.hashCode(),n);
 }
 static class UriSound{static void play(Context c){final Ringtone r=RingtoneManager.getRingtone(c,RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM));if(r!=null){r.play();new Handler(Looper.getMainLooper()).postDelayed(()->{try{if(r.isPlaying())r.stop();}catch(Exception ignored){}},30000);}}}
}
