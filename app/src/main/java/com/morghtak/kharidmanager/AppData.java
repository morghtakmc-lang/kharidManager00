package com.morghtak.kharidmanager;

import android.content.*;import org.json.*;import java.util.*;

public class AppData {
 public static final String PREF="data";
 public static final String[] DEFAULT_COMMODITIES={"دان آماده پیش آغازین","دان آماده آغازین","دان آماده رشد","دان آماده پایانی 1","دان آماده پایانی 2","ذرت","سویا"};
 public static final String[] PAYMENTS={"توافقی","نقد","نسیه"};
 public static JSONObject root(Context c){ try{return new JSONObject(c.getSharedPreferences(PREF,0).getString("root", "{\"buyers\":[],\"companies\":[\"شرکت شاهدانه مارلیک\"],\"commodities\":[\"دان آماده پیش آغازین\",\"دان آماده آغازین\",\"دان آماده رشد\",\"دان آماده پایانی 1\",\"دان آماده پایانی 2\",\"ذرت\",\"سویا\"],\"purchases\":[]}"));}catch(Exception e){return new JSONObject();}}
 public static void save(Context c,JSONObject o){c.getSharedPreferences(PREF,0).edit().putString("root",o.toString()).apply();}
 public static JSONArray arr(JSONObject r,String k){return r.optJSONArray(k)==null?new JSONArray():r.optJSONArray(k);}
 public static String money(String s){return fmt(s);}
 public static String fmt(String s){try{String x=s.replace(",","").replace("٬","").replace("،","").trim(); if(x.isEmpty())return ""; long n=Long.parseLong(x);return String.format(Locale.US,"%,d",n).replace(',','،');}catch(Exception e){return s;}}
 public static String digits(String s){return s.replace('۰','0').replace('۱','1').replace('۲','2').replace('۳','3').replace('۴','4').replace('۵','5').replace('۶','6').replace('۷','7').replace('۸','8').replace('۹','9').replace("،","").replace(",","");}
}
