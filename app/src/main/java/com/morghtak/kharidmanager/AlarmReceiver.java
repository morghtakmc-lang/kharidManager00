package com.morghtak.kharidmanager;

import android.app.*;
import android.content.*;
import android.media.*;
import android.os.*;
import androidx.core.app.NotificationCompat;
import org.json.*;

public class AlarmReceiver extends BroadcastReceiver {
    static Ringtone ringtone;
    public void onReceive(Context c,Intent i){
        String id=i.getStringExtra("id"),title=i.getStringExtra("title");
        NotificationManager nm=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
        String ch="deadlines";
        if(Build.VERSION.SDK_INT>=26)nm.createNotificationChannel(new NotificationChannel(ch,"هشدار سررسید",NotificationManager.IMPORTANCE_HIGH));
        try{
            ringtone=RingtoneManager.getRingtone(c,RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM));
            if(ringtone!=null)ringtone.play();
        }catch(Exception ignored){}
        Intent open=new Intent(c,MainActivity.class);
        open.putExtra("openPurchaseId",id);open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent tap=PendingIntent.getActivity(c,id==null?1:id.hashCode(),open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification n=new NotificationCompat.Builder(c,ch)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("هشدار سررسید خرید")
                .setContentText(title==null?"یک خرید نیاز به پیگیری دارد":title)
                .setPriority(NotificationCompat.PRIORITY_MAX).setAutoCancel(true)
                .setContentIntent(tap).setVibrate(new long[]{0,700,300,1000}).build();
        nm.notify(id==null?1:id.hashCode(),n);
        try{JSONObject p=MainActivity.findStatic(c,id);if(p!=null&&p.optBoolean("alarmRepeat")&&!p.optBoolean("collected"))MainActivity.scheduleNext(c,p);}catch(Exception ignored){}
    }
    public static void stopSound(){try{if(ringtone!=null&&ringtone.isPlaying())ringtone.stop();}catch(Exception ignored){}ringtone=null;}
}
