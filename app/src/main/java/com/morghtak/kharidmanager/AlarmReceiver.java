package com.morghtak.kharidmanager;

import android.app.*;
import android.content.*;
import android.media.*;
import android.net.*;
import android.os.*;
import androidx.core.app.NotificationCompat;

public class AlarmReceiver extends BroadcastReceiver{
    static Ringtone currentRingtone;
    public void onReceive(Context c,Intent i){
        String id=i.getStringExtra("id"),title=i.getStringExtra("title");
        NotificationManager nm=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
        String ch="deadlines";
        if(Build.VERSION.SDK_INT>=26)nm.createNotificationChannel(new NotificationChannel(ch,"سررسیدها",NotificationManager.IMPORTANCE_HIGH));
        try{play(c);}catch(Exception ignored){}
        Intent open=new Intent(c,MainActivity.class);open.putExtra("openPurchaseId",id);open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pi=PendingIntent.getActivity(c,id==null?1:id.hashCode(),open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        int icon=c.getResources().getIdentifier("ic_launcher_foreground","drawable",c.getPackageName());
        if(icon==0)icon=android.R.drawable.ic_lock_idle_alarm;
        Notification n=new NotificationCompat.Builder(c,ch)
            .setSmallIcon(icon)
            .setContentTitle("هشدار سررسید خرید")
            .setContentText(title==null?"سررسید خرید فرا رسیده است":title)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setVibrate(new long[]{0,700,300,1000})
            .build();
        nm.notify(id==null?1:id.hashCode(),n);
        JSONObjectHelper.scheduleNextIfNeeded(c,id);
    }
    static void play(Context c){currentRingtone=RingtoneManager.getRingtone(c,RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM));if(currentRingtone!=null)currentRingtone.play();}
    public static void stopSound(){try{if(currentRingtone!=null&&currentRingtone.isPlaying())currentRingtone.stop();}catch(Exception ignored){}}
    static class JSONObjectHelper{
        static void scheduleNextIfNeeded(Context c,String id){try{org.json.JSONObject p=MainActivity.findStatic(c,id);if(p!=null&&p.optBoolean("alarmRepeat")&&!p.optBoolean("collected"))MainActivity.scheduleNext(c,p);}catch(Exception ignored){}}
    }
}
