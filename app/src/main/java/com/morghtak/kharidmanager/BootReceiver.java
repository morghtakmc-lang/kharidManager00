package com.morghtak.kharidmanager;
import android.content.*;import android.app.*;import org.json.*;import java.util.*;
public class BootReceiver extends BroadcastReceiver{
 public void onReceive(Context c,Intent i){try{JSONArray a=AppData.arr(AppData.root(c),"purchases");for(int k=0;k<a.length();k++){JSONObject p=a.getJSONObject(k);if(p.optBoolean("alarm",false))MainActivity.schedule(c,p);}}catch(Exception ignored){}}
}
