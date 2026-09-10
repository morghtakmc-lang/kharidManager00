package com.morghtak.kharidmanager;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ColorDrawable;
import android.content.res.ColorStateList;
import android.os.*;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import android.text.*;
import androidx.core.content.FileProvider;
import org.json.*;
import java.io.*;
import java.util.*;

public class MainActivity extends Activity {
    LinearLayout root;
    JSONObject data;
    ArrayList<EditText> inputs = new ArrayList<>();
    Spinner buyerSp, commoditySp, paymentSp, companySp;
    final ArrayDeque<Runnable> history = new ArrayDeque<>();
    Runnable currentPage;
    final String[] labels = {"نام خریدار","نهاده","شماره خرید","وزن (کیلوگرم)","فی (ریال)","نوع پرداخت","مبلغ خرید (ریال)","تاریخ خرید","مقدار ذرت (کیلوگرم)","مقدار سویا (کیلوگرم)","مقدار ریزمغذی (کیلوگرم)","نام شرکت","تاریخ اصلی سررسید"};
    final String[] keys = {"buyer","commodity","purchaseNo","weight","fee","payment","amount","buyDate","corn","soy","micronutrient","company","mainDue"};

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        data=AppData.root(this);
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},22);
        String id=getIntent().getStringExtra("openPurchaseId");
        if(id!=null){ JSONObject p=findPurchase(id); if(p!=null) details(p); else home(); }
        else openPage(()->home(),false);
    }

    @Override protected void onNewIntent(Intent i){
        super.onNewIntent(i); setIntent(i); AlarmReceiver.stopSound();
        String id=i.getStringExtra("openPurchaseId");
        if(id!=null){JSONObject p=findPurchase(id);if(p!=null)details(p);}
    }

    @Override public void onBackPressed(){
        if(!history.isEmpty()) back();
        else super.onBackPressed();
    }

    void openPage(Runnable page){openPage(page,true);}
    void openPage(Runnable page,boolean push){
        if(page==null)return;
        if(push&&currentPage!=null)history.push(currentPage);
        currentPage=page; page.run();
    }
    void back(){if(!history.isEmpty()){Runnable p=history.pop();currentPage=p;p.run();}else super.onBackPressed();}
    void goHome(){history.clear();currentPage=()->home();home();}

    void home(){
        history.clear(); base("مدیریت خرید و سررسید");
        TextView s=tv("",16); add(s);
        JSONArray a=AppData.arr(data,"purchases"); int open=0;
        for(int i=0;i<a.length();i++)if(!a.optJSONObject(i).optBoolean("collected",false))open++;
        s.setText("تعداد خریدها: "+a.length()+"\nخریدهای در انتظار وصول: "+open);
        Button b=btn("➕ ثبت خرید جدید"); b.setOnClickListener(v->openPage(()->form(null))); add(b);
        b=btn("🛒 خریدهای ثبت‌شده"); b.setOnClickListener(v->openPage(()->listPurchases(null))); add(b);
        b=btn("👤 خریداران و پرونده هر خریدار"); b.setOnClickListener(v->openPage(this::buyersPage)); add(b);
        b=btn("🔔 سررسیدها"); b.setOnClickListener(v->openPage(this::deadlines)); add(b);
        b=btn("🏢 شرکت‌ها"); b.setOnClickListener(v->openPage(()->manage("companies","شرکت‌ها"))); add(b);
        b=btn("🌾 نهاده‌ها"); b.setOnClickListener(v->openPage(()->manage("commodities","نهاده‌ها"))); add(b);
        b=btn("📊 گزارش‌ها و خروجی Excel"); b.setOnClickListener(v->openPage(this::reports)); add(b);
        b=btn("💾 پشتیبان‌گیری / بازیابی"); b.setOnClickListener(v->openPage(this::backup)); add(b);
        finishScreen("مدیریت خرید و سررسید");
    }

    void base(String title){
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
        TextView h=tv(title,21);h.setGravity(Gravity.CENTER);
        root.addView(h,new LinearLayout.LayoutParams(-1,UiManager.dp(this,64)));setContentView(root);
    }
    void finishScreen(String title){UiManager.decorate(this,root,title);hideKeyboard();}
    void hideKeyboard(){try{((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(root.getWindowToken(),0);}catch(Exception ignored){}}
    void add(View v){root.addView(v,new LinearLayout.LayoutParams(-1,ViewGroup.LayoutParams.WRAP_CONTENT));}
    TextView tv(String s,int z){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setPadding(18,12,18,12);return t;}
    Button btn(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);return b;}
    EditText input(String hint){EditText e=new EditText(this);e.setHint(hint);e.setSingleLine(true);e.setTextSize(UiManager.fieldSize(this));e.setPadding(14,8,14,8);inputs.add(e);add(e);return e;}
    void label(String s){add(tv(s,14));}
    Spinner spinner(JSONArray a){ArrayList<String>x=new ArrayList<>();for(int i=0;i<a.length();i++)x.add(a.optString(i));return spinner(x.toArray(new String[0]));}
    Spinner spinner(String[] a){Spinner s=new Spinner(this);s.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,a));return s;}
    void setSpinner(Spinner s,String v){if(s==null)return;for(int i=0;i<s.getCount();i++)if(String.valueOf(s.getItemAtPosition(i)).equals(v)){s.setSelection(i);break;}}
    void hidden(){EditText e=new EditText(this);e.setVisibility(View.GONE);inputs.add(e);}
    void addGrouping(EditText e){e.addTextChangedListener(new TextWatcher(){boolean busy;public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int b,int c){}public void afterTextChanged(Editable ed){if(busy)return;String r=AppData.digits(ed.toString());if(r.isEmpty())return;busy=true;String f=AppData.fmt(r);e.setText(f);e.setSelection(f.length());busy=false;}});}

    // Persian calendar: full current-year/month grid, with day selection.
    void pickDate(EditText target){
        final PersianCalendarDialog dlg=new PersianCalendarDialog(this,target.getText().toString());
        dlg.show();
    }

    void form(JSONObject old){
        base(old==null?"ثبت خرید جدید":"ویرایش خرید");inputs.clear();
        for(int i=0;i<labels.length;i++){
            label((i+1)+". "+labels[i]);
            if(i==0){buyerSp=spinnerWithBlank(AppData.arr(data,"buyers"),"انتخاب خریدار");add(buyerSp);hidden();}
            else if(i==1){commoditySp=spinnerWithBlank(AppData.arr(data,"commodities"),"انتخاب نهاده");add(commoditySp);hidden();}
            else if(i==5){paymentSp=spinnerWithBlank(AppData.PAYMENTS,"انتخاب نوع پرداخت");add(paymentSp);hidden();}
            else if(i==11){companySp=spinnerWithBlank(AppData.arr(data,"companies"),"انتخاب شرکت");add(companySp);hidden();}
            else{
                EditText e=input("");
                if(i==7||i==12)e.setOnClickListener(v->pickDate(e));
                if(i==3||i==4||i==6||i==8||i==9||i==10){e.setInputType(2);addGrouping(e);}
            }
        }
        inputs.get(6).setFocusable(false);inputs.get(6).setClickable(false);
        TextWatcher calcW=new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int b,int c){}public void afterTextChanged(Editable e){calcAmount();validateForm(null);}};
        inputs.get(3).addTextChangedListener(calcW);inputs.get(4).addTextChangedListener(calcW);
        inputs.get(8).addTextChangedListener(calcW);inputs.get(9).addTextChangedListener(calcW);inputs.get(10).addTextChangedListener(calcW);
        if(old!=null)fill(old);else inputs.get(7).setText(PersianDate.today());

        add(tv("هشدار سررسید اصلی",16));
        LinearLayout al=new LinearLayout(this);al.setOrientation(LinearLayout.VERTICAL);add(al);
        EditText days=new EditText(this);days.setHint("چند روز قبل از سررسید؟");days.setInputType(2);al.addView(days);
        EditText time=new EditText(this);time.setHint("ساعت هشدار مثل 10:00");time.setSingleLine(true);al.addView(time);
        Spinner repeat=spinner(new String[]{"فقط یک بار","هر روز تا سررسید"});al.addView(repeat);
        TextView preview=tv("هشدار تنظیم نشده است",13);al.addView(preview);
        if(old!=null){days.setText(old.optBoolean("alarm",false)?""+old.optInt("alarmDays",1):"");time.setText(old.optBoolean("alarm",false)?old.optString("alarmTime",""):"");repeat.setSelection(old.optBoolean("alarmRepeat",false)?1:0);}
        TextWatcher aw=new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int b,int c){}public void onTextChanged(CharSequence s,int a,int b,int c){updateAlarmStatus(preview,days.getText().toString(),time.getText().toString());}public void afterTextChanged(Editable e){}};
        days.addTextChangedListener(aw);time.addTextChangedListener(aw);
        Button clear=btn("🔕 حذف هشدار");clear.setOnClickListener(v->{days.setText("");time.setText("");if(old!=null)cancelAlarm(this,old);updateAlarmStatus(preview,"","");});add(clear);
        Button save=btn("✓ ذخیره خرید و هشدار");save.setOnClickListener(v->savePurchase(old,days.getText().toString(),time.getText().toString(),repeat.getSelectedItemPosition()==1,save));add(save);
        Button back=btn("← بازگشت");back.setOnClickListener(v->back());add(back);
        finishScreen(old==null?"ثبت خرید جدید":"ویرایش خرید");updateAlarmStatus(preview,days.getText().toString(),time.getText().toString());setupValidationListeners(save);validateForm(save);
    }
    Spinner spinnerWithBlank(JSONArray a,String placeholder){
        ArrayList<String>x=new ArrayList<>();x.add("");for(int i=0;i<a.length();i++)x.add(a.optString(i));
        Spinner s=new Spinner(this);ArrayAdapter<String> ad=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,x){@Override public View getView(int position,View convertView,android.view.ViewGroup parent){TextView v=(TextView)super.getView(position,convertView,parent);v.setText(position==0?"":String.valueOf(getItem(position)));return v;}};s.setAdapter(ad);return s;
    }
    Spinner spinnerWithBlank(String[] a,String placeholder){ArrayList<String>x=new ArrayList<>();x.add("");Collections.addAll(x,a);Spinner s=new Spinner(this);ArrayAdapter<String> ad=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,x){@Override public View getView(int position,View convertView,android.view.ViewGroup parent){TextView v=(TextView)super.getView(position,convertView,parent);v.setText(position==0?"":String.valueOf(getItem(position)));return v;}};s.setAdapter(ad);return s;}

    void setupValidationListeners(Button save){
        AdapterView.OnItemSelectedListener l=new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> p){}public void onItemSelected(AdapterView<?> p,View v,int pos,long id){validateForm(save);}};
        if(buyerSp!=null)buyerSp.setOnItemSelectedListener(l);if(commoditySp!=null)commoditySp.setOnItemSelectedListener(l);if(paymentSp!=null)paymentSp.setOnItemSelectedListener(l);if(companySp!=null)companySp.setOnItemSelectedListener(l);
        for(int i:new int[]{2,3,4,7,8,9,10,12})inputs.get(i).addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int b,int c){validateForm(save);}public void afterTextChanged(Editable e){}});
    }
    boolean validateForm(Button save){
        boolean ok=true;
        ok &= markSpinner(buyerSp,buyerSp!=null&&buyerSp.getSelectedItemPosition()>0);
        ok &= markSpinner(commoditySp,commoditySp!=null&&commoditySp.getSelectedItemPosition()>0);
        ok &= markSpinner(paymentSp,paymentSp!=null&&paymentSp.getSelectedItemPosition()>0);
        ok &= markSpinner(companySp,companySp!=null&&companySp.getSelectedItemPosition()>0);
        int[] req={2,3,4,7,8,9,10,12};for(int i:req)ok &= markField(inputs.get(i),!inputs.get(i).getText().toString().trim().isEmpty());
        String ws=AppData.digits(inputs.get(3).getText().toString()),cs=AppData.digits(inputs.get(8).getText().toString()),ss=AppData.digits(inputs.get(9).getText().toString()),ms=AppData.digits(inputs.get(10).getText().toString());
        boolean nums=!ws.isEmpty()&&!cs.isEmpty()&&!ss.isEmpty()&&!ms.isEmpty();
        if(nums){try{long w=Long.parseLong(ws),c=Long.parseLong(cs),so=Long.parseLong(ss),mi=Long.parseLong(ms);boolean match=(c+so+mi)==w;markField(inputs.get(3),match);markField(inputs.get(8),match);markField(inputs.get(9),match);markField(inputs.get(10),match);ok&=match;}catch(Exception e){ok=false;for(int i:new int[]{3,8,9,10})markField(inputs.get(i),false);}}
        else ok=false;
        if(save!=null){save.setEnabled(ok);save.setAlpha(ok?1f:0.5f);}return ok;
    }
    boolean markField(EditText e,boolean valid){e.setBackground(fieldBg(valid));return valid;}
    boolean markSpinner(Spinner s,boolean valid){if(s!=null)s.setBackground(fieldBg(valid));return valid;}
    GradientDrawable fieldBg(boolean valid){GradientDrawable g=new GradientDrawable();g.setColor(UiManager.card(this));g.setCornerRadius(UiManager.dp(this,UiManager.radius(this)));g.setStroke(UiManager.dp(this,1),valid?UiManager.primary(this):Color.rgb(210,50,50));return g;}
    void updateAlarmStatus(TextView v,String days,String time){String d=days.trim(),t=time.trim();if(d.isEmpty()&&t.isEmpty()){v.setText("هشدار تنظیم نشده است");return;}if(!d.matches("\\d+")||!t.matches("([01]\\d|2[0-3]):[0-5]\\d")){v.setText("⚠ تنظیم هشدار کامل نیست");return;}v.setText("🔔 هشدار فعال: "+d+" روز قبل، ساعت "+t);}

    void calcAmount(){try{long w=Long.parseLong(AppData.digits(inputs.get(3).getText().toString()));long f=Long.parseLong(AppData.digits(inputs.get(4).getText().toString()));inputs.get(6).setText(AppData.fmt(""+(w*f)));}catch(Exception ignored){inputs.get(6).setText("");}}
    void fill(JSONObject p){for(int i=0;i<keys.length;i++)if(i!=0&&i!=1&&i!=5&&i!=11)inputs.get(i).setText(p.optString(keys[i],""));setSpinner(buyerSp,p.optString("buyer"));setSpinner(commoditySp,p.optString("commodity"));setSpinner(paymentSp,p.optString("payment"));setSpinner(companySp,p.optString("company"));calcAmount();}
    void savePurchase(JSONObject old,String ds,String tm,boolean repeat,Button saveButton){
        if(!validateForm(saveButton)){Toast.makeText(this,"اطلاعات ناقص یا جمع وزن مواد با وزن خرید برابر نیست",Toast.LENGTH_LONG).show();return;}
        try{
            JSONObject r=new JSONObject();for(int i=0;i<keys.length;i++){String v=inputs.get(i).getText().toString();if(i==0)v=String.valueOf(buyerSp.getSelectedItem());if(i==1)v=String.valueOf(commoditySp.getSelectedItem());if(i==5)v=String.valueOf(paymentSp.getSelectedItem());if(i==11)v=String.valueOf(companySp.getSelectedItem());r.put(keys[i],v);}
            r.put("id",old==null?UUID.randomUUID().toString():old.optString("id"));
            r.put("collected",old!=null&&old.optBoolean("collected",false));r.put("collectedDate",old==null?"":old.optString("collectedDate",""));
            r.put("allocated",old!=null&&old.optBoolean("allocated",false));r.put("inventoryFunded",old!=null&&old.optBoolean("inventoryFunded",false));
            JSONArray fr=old!=null?old.optJSONArray("fundings"):null;r.put("fundings",fr==null?new JSONArray():fr);
            boolean alarmConfigured=ds.trim().matches("\\d+")&&tm.trim().matches("([01]\\d|2[0-3]):[0-5]\\d");
            r.put("alarm",alarmConfigured);r.put("alarmDays",parseInt(ds,1));r.put("alarmTime",tm.trim());r.put("alarmRepeat",repeat);
            JSONArray a=AppData.arr(data,"purchases");boolean replaced=false;for(int i=0;i<a.length();i++)if(a.optJSONObject(i).optString("id").equals(r.optString("id"))){a.put(i,r);replaced=true;break;}if(!replaced)a.put(r);
            data.put("purchases",a);addUnique("buyers",r.optString("buyer"));addUnique("companies",r.optString("company"));AppData.save(this,data);
            if(alarmConfigured)schedule(this,r);else cancelAlarm(this,r);Toast.makeText(this,"خرید با موفقیت ذخیره شد",Toast.LENGTH_SHORT).show();goHome();
        }catch(Exception e){Toast.makeText(this,"خطا در ذخیره اطلاعات",Toast.LENGTH_LONG).show();}
    }
    int parseInt(String s,int d){try{return Integer.parseInt(s.trim());}catch(Exception e){return d;}}
    void addUnique(String key,String val)throws Exception{if(val==null||val.trim().isEmpty())return;JSONArray a=AppData.arr(data,key);for(int i=0;i<a.length();i++)if(a.optString(i).equals(val))return;a.put(val);}

    void listPurchases(String fixedBuyer){listPurchases(fixedBuyer,0);}
    void listPurchases(String fixedBuyer,int initialStatus){
        base(fixedBuyer==null?"خریدهای ثبت‌شده":"پرونده "+fixedBuyer);EditText q=input("");q.setHint("جستجو: نام، شماره خرید، شرکت یا نهاده");
        LinearLayout filters=new LinearLayout(this);filters.setOrientation(LinearLayout.VERTICAL);add(filters);
        final int[] cf={initialStatus==1?1:initialStatus==2?0:-1},af={initialStatus==3?1:initialStatus==4?0:-1},ff={-1};
        Button c1=btn("وصول شده"),c2=btn("وصول نشده"),a1=btn("تخصیص شده"),a2=btn("تخصیص نشده"),f1=btn("تأمین شده"),f2=btn("تأمین نشده");
        LinearLayout r1=new LinearLayout(this),r2=new LinearLayout(this),r3=new LinearLayout(this);for(Button b:new Button[]{c1,c2})r1.addView(b,new LinearLayout.LayoutParams(0,UiManager.dp(this,52),1));for(Button b:new Button[]{a1,a2})r2.addView(b,new LinearLayout.LayoutParams(0,UiManager.dp(this,52),1));for(Button b:new Button[]{f1,f2})r3.addView(b,new LinearLayout.LayoutParams(0,UiManager.dp(this,52),1));filters.addView(r1);filters.addView(r2);filters.addView(r3);
        LinearLayout dates=new LinearLayout(this);EditText from=new EditText(this),to=new EditText(this);from.setHint("از تاریخ");to.setHint("تا تاریخ");from.setSingleLine(true);to.setSingleLine(true);from.setOnClickListener(v->pickDate(from));to.setOnClickListener(v->pickDate(to));dates.addView(from,new LinearLayout.LayoutParams(0,UiManager.dp(this,54),1));dates.addView(to,new LinearLayout.LayoutParams(0,UiManager.dp(this,54),1));add(dates);Button go=btn("🔎 اعمال فیلترها");add(go);TextView summary=tv("",14);add(summary);LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);add(list);
        Runnable styles=()->{Button[] bs={c1,c2,a1,a2,f1,f2};int[] selected={cf[0],cf[0],af[0],af[0],ff[0],ff[0]};int[] wanted={1,0,1,0,1,0};for(int i=0;i<6;i++){boolean active=selected[i]==wanted[i];bs[i].setBackgroundColor(active?UiManager.primary(this):UiManager.card(this));bs[i].setTextColor(active?Color.WHITE:UiManager.text(this));}};
        final ArrayList<JSONObject> shown=new ArrayList<>();
        Runnable render=()->{list.removeAllViews();shown.clear();JSONArray a=sortedPurchases();String s=q.getText().toString().trim(),f=from.getText().toString(),t=to.getText().toString();int n=0,cntC=0,cntCN=0,cntA=0,cntAN=0,cntF=0,cntFN=0;long amtAll=0,amtC=0,amtCN=0,amtA=0,amtAN=0,amtF=0,amtFN=0;for(int i=0;i<a.length();i++){JSONObject p=a.optJSONObject(i);if(p==null)continue;if(fixedBuyer!=null&&!fixedBuyer.equals(p.optString("buyer")))continue;boolean cv=p.optBoolean("collected"),av=p.optBoolean("allocated"),fv=p.optBoolean("inventoryFunded")||fundedTotal(p)>=toLong(p.optString("amount"));String blob=p.optString("buyer")+" "+p.optString("purchaseNo")+" "+p.optString("company")+" "+p.optString("commodity");if(!s.isEmpty()&&!blob.contains(s))continue;if(!f.isEmpty()&&p.optString("buyDate").compareTo(f)<0)continue;if(!t.isEmpty()&&p.optString("buyDate").compareTo(t)>0)continue;if(cf[0]==1&&!cv||cf[0]==0&&cv||af[0]==1&&!av||af[0]==0&&av||ff[0]==1&&!fv||ff[0]==0&&fv)continue;shown.add(p);n++;long pa=toLong(p.optString("amount"));amtAll+=pa;if(cv){cntC++;amtC+=pa;}else{cntCN++;amtCN+=pa;}if(av){cntA++;amtA+=pa;}else{cntAN++;amtAN+=pa;}if(fv){cntF++;amtF+=pa;}else{cntFN++;amtFN+=Math.max(0,pa-fundedTotal(p));}Button item=btn("👤 "+p.optString("buyer")+" | خرید "+p.optString("purchaseNo")+"\nتاریخ خرید: "+p.optString("buyDate")+"\nوصول: "+(cv?"وصول شده":"وصول نشده")+" | تخصیص: "+(av?"تخصیص شده":"تخصیص نشده")+" | تأمین: "+(fv?"تکمیل شده":"تکمیل نشده"));item.setOnClickListener(v->openPage(()->details(p)));list.addView(item);}summary.setText("تعداد کل پرونده‌ها: "+n+" | مبلغ کل: "+AppData.fmt(""+amtAll)+" ریال\nوصول شده: "+cntC+" پرونده | مبلغ: "+AppData.fmt(""+amtC)+" | وصول نشده: "+cntCN+" پرونده | مبلغ: "+AppData.fmt(""+amtCN)+"\nتخصیص شده: "+cntA+" پرونده | مبلغ: "+AppData.fmt(""+amtA)+" | تخصیص نشده: "+cntAN+" پرونده | مبلغ: "+AppData.fmt(""+amtAN)+"\nتأمین موجودی شده: "+cntF+" پرونده | مبلغ: "+AppData.fmt(""+amtF)+" | تأمین نشده: "+cntFN+" پرونده | مانده: "+AppData.fmt(""+amtFN)+" ریال");};
        c1.setOnClickListener(v->{cf[0]=cf[0]==1?-1:1;styles.run();render.run();});c2.setOnClickListener(v->{cf[0]=cf[0]==0?-1:0;styles.run();render.run();});a1.setOnClickListener(v->{af[0]=af[0]==1?-1:1;styles.run();render.run();});a2.setOnClickListener(v->{af[0]=af[0]==0?-1:0;styles.run();render.run();});f1.setOnClickListener(v->{ff[0]=ff[0]==1?-1:1;styles.run();render.run();});f2.setOnClickListener(v->{ff[0]=ff[0]==0?-1:0;styles.run();render.run();});go.setOnClickListener(v->render.run());styles.run();render.run();Button back=btn("← بازگشت");back.setOnClickListener(v->back());add(back);finishScreen("خریدهای ثبت‌شده");
    }
    JSONArray sortedPurchases(){JSONArray src=AppData.arr(data,"purchases");ArrayList<JSONObject> l=new ArrayList<>();for(int i=0;i<src.length();i++)l.add(src.optJSONObject(i));Collections.sort(l,(a,b)->b.optString("buyDate").compareTo(a.optString("buyDate")));JSONArray r=new JSONArray();for(JSONObject p:l)r.put(p);return r;}

    void details(JSONObject p){
        base("جزئیات کامل خرید");add(tv("👤 "+p.optString("buyer")+"\nشماره خرید: "+p.optString("purchaseNo"),19));for(int i=0;i<keys.length;i++)add(tv(labels[i]+": "+p.optString(keys[i],"-"),15));
        long required=toLong(p.optString("amount")),funded=fundedTotal(p),remaining=Math.max(0,required-funded);boolean fundDone=funded>=required&&required>=0;
        add(tv("وضعیت وصول: "+(p.optBoolean("collected")?"✅ وصول شده":"⏳ وصول نشده")+(p.optString("collectedDate").isEmpty()?"":" | تاریخ وصول: "+p.optString("collectedDate")),16));add(tv("وضعیت تخصیص: "+(p.optBoolean("allocated")?"✅ تخصیص شده":"⏳ تخصیص نشده"),16));add(tv("وضعیت تأمین موجودی: "+(fundDone?"✅ تأمین تکمیل شده":"⏳ تأمین تکمیل نشده")+"\nمبلغ موردنیاز: "+AppData.fmt(""+required)+" ریال | جمع تأمین: "+AppData.fmt(""+funded)+" ریال | مانده: "+AppData.fmt(""+remaining)+" ریال",15));
        JSONArray fr=p.optJSONArray("fundings");if(fr!=null&&fr.length()>0){add(tv("سوابق تأمین موجودی",16));long sum=0;for(int i=0;i<fr.length();i++){JSONObject x=fr.optJSONObject(i);if(x==null)continue;sum+=toLong(x.optString("amount"));add(tv("مرحله "+(i+1)+" | تاریخ: "+x.optString("date")+" | مبلغ: "+AppData.fmt(x.optString("amount"))+" ریال\nمبدأ: "+x.optString("source")+" | مقصد: "+x.optString("destination")+"\nجمع تأمین: "+AppData.fmt(""+sum)+" ریال | مانده: "+AppData.fmt(""+Math.max(0,required-sum))+" ریال",14));}}
        if(p.optBoolean("alarm"))add(tv("🔔 هشدار: "+p.optInt("alarmDays",1)+" روز قبل، ساعت "+p.optString("alarmTime")+" - "+(p.optBoolean("alarmRepeat")?"روزانه تا سررسید":"یک‌بار"),15));
        Button c=btn(p.optBoolean("collected")?"↩️ لغو وصول":"💰 وصول شد");c.setOnClickListener(v->confirmStatusChange(p,"collected","وصول"));add(c);Button inv=btn("💳 "+(fundDone?"مشاهده/ثبت تأمین موجودی":"تأمین موجودی"));inv.setOnClickListener(v->fundingDialog(p));add(inv);Button al=btn(p.optBoolean("allocated")?"↩️ لغو تخصیص":"📦 تخصیص شد");al.setOnClickListener(v->confirmStatusChange(p,"allocated","تخصیص"));add(al);
        Button rem=btn("🔕 حذف آلارم");rem.setOnClickListener(v->{try{p.put("alarm",false);AppData.save(this,data);cancelAlarm(this,p);details(p);}catch(Exception ignored){}});add(rem);Button ex=btn("📊 خروجی Excel همین خرید");ex.setOnClickListener(v->exportExcel(new JSONArray().put(p),"purchase_"+p.optString("purchaseNo")));add(ex);Button ed=btn("✏️ ویرایش");ed.setOnClickListener(v->openPage(()->form(p)));add(ed);Button same=btn("👤 پرونده "+p.optString("buyer"));same.setOnClickListener(v->openPage(()->listPurchases(p.optString("buyer"))));add(same);Button del=btn("🗑 حذف");del.setOnClickListener(v->confirmDelete(p));add(del);Button back=btn("← بازگشت");back.setOnClickListener(v->back());add(back);finishScreen("جزئیات کامل خرید");
    }
    void confirmStatusChange(JSONObject p,String key,String title){boolean next=!p.optBoolean(key,false);if("collected".equals(key)&&next){final EditText date=new EditText(this);date.setHint("تاریخ وصول");date.setSingleLine(true);date.setOnClickListener(v->pickDate(date));new AlertDialog.Builder(this).setTitle("ثبت تاریخ وصول").setView(date).setPositiveButton("ثبت",(d,w)->{if(date.getText().toString().trim().isEmpty()){Toast.makeText(this,"تاریخ وصول را انتخاب کنید",Toast.LENGTH_SHORT).show();return;}try{p.put("collected",true);p.put("collectedDate",date.getText().toString());AppData.save(this,data);details(p);}catch(Exception ignored){}}).setNegativeButton("لغو",null).show();return;}String action=next?"فعال‌سازی":"غیرفعال‌سازی";new AlertDialog.Builder(this).setTitle("تأیید تغییر وضعیت").setMessage("آیا از "+action+" "+title+" این خرید مطمئن هستید؟").setPositiveButton("بله",(d,w)->{try{p.put(key,next);if("collected".equals(key)&&!next)p.put("collectedDate","");AppData.save(this,data);details(p);}catch(Exception ignored){}}).setNegativeButton("خیر",null).show();}
    long fundedTotal(JSONObject p){long s=0;JSONArray a=p==null?null:p.optJSONArray("fundings");if(a!=null)for(int i=0;i<a.length();i++){JSONObject z=a.optJSONObject(i);if(z!=null)s+=toLong(z.optString("amount"));}return s;}
    void fundingDialog(JSONObject p){long required=toLong(p.optString("amount")),funded=fundedTotal(p),remaining=Math.max(0,required-funded);if(remaining==0){Toast.makeText(this,"مبلغ تأمین موجودی تکمیل شد.",Toast.LENGTH_SHORT).show();return;}LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(UiManager.dp(this,10),UiManager.dp(this,4),UiManager.dp(this,10),UiManager.dp(this,4));EditText date=new EditText(this),amount=new EditText(this),source=new EditText(this),dest=new EditText(this);date.setHint("تاریخ");amount.setHint("مبلغ تأمین");source.setHint("مبدأ (حساب مبدأ)");dest.setHint("مقصد (حساب مقصد)");date.setOnClickListener(v->pickDate(date));amount.setInputType(2);addGrouping(amount);box.addView(date);box.addView(amount);box.addView(source);box.addView(dest);box.addView(tv("مبلغ موردنیاز: "+AppData.fmt(""+required)+" ریال\nجمع تأمین‌شده: "+AppData.fmt(""+funded)+" ریال\nمانده: "+AppData.fmt(""+remaining)+" ریال",14));new AlertDialog.Builder(this).setTitle("ثبت مرحله تأمین موجودی").setView(box).setPositiveButton("ثبت مرحله",(d,w)->{try{String ds=date.getText().toString().trim(),as=AppData.digits(amount.getText().toString()),ss=source.getText().toString().trim(),dd=dest.getText().toString().trim();long av=Long.parseLong(as);if(ds.isEmpty()||as.isEmpty()||ss.isEmpty()||dd.isEmpty()||av<=0){Toast.makeText(this,"همه اطلاعات را کامل وارد کنید",Toast.LENGTH_SHORT).show();return;}if(av>remaining){Toast.makeText(this,"مبلغ این مرحله بیشتر از مانده تأمین است",Toast.LENGTH_LONG).show();return;}JSONArray arr=p.optJSONArray("fundings");if(arr==null)arr=new JSONArray();JSONObject rec=new JSONObject();rec.put("date",ds);rec.put("amount",av);rec.put("source",ss);rec.put("destination",dd);arr.put(rec);p.put("fundings",arr);long total=fundedTotal(p);p.put("inventoryFunded",total>=required);AppData.save(this,data);if(total>=required)Toast.makeText(this,"مبلغ تأمین موجودی تکمیل شد.",Toast.LENGTH_LONG).show();else Toast.makeText(this,"مرحله ثبت شد. مانده: "+AppData.fmt(""+(required-total))+" ریال",Toast.LENGTH_LONG).show();details(p);}catch(Exception e){Toast.makeText(this,"اطلاعات تأمین موجودی معتبر نیست",Toast.LENGTH_SHORT).show();}}).setNegativeButton("لغو",null).show();}

    void buyersPage(){
        base("خریداران و پرونده هر خریدار");EditText q=input("جستجوی نام خریدار");Button go=btn("🔎 جستجو");add(go);LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);add(list);
        Runnable render=()->{list.removeAllViews();JSONArray b=AppData.arr(data,"buyers");String s=q.getText().toString().trim();for(int i=0;i<b.length();i++){String name=b.optString(i);if(!s.isEmpty()&&!name.contains(s))continue;Button x=btn("👤 "+name);x.setOnClickListener(v->openPage(()->buyerFile(name)));list.addView(x);}};go.setOnClickListener(v->render.run());render.run();Button addb=btn("➕ افزودن خریدار");addb.setOnClickListener(v->addEntry("buyers","خریدار جدید",this::buyersPage));add(addb);Button back=btn("← بازگشت");back.setOnClickListener(v->back());add(back);finishScreen("خریداران");
    }
    void buyerFile(String buyer){
        base("پرونده خریدار: "+buyer);JSONArray a=AppData.arr(data,"purchases");int n=0,coll=0,alloc=0;long total=0,colAmt=0;ArrayList<JSONObject> noColl=new ArrayList<>(),noAlloc=new ArrayList<>();
        for(int i=0;i<a.length();i++){JSONObject p=a.optJSONObject(i);if(p==null||!buyer.equals(p.optString("buyer")))continue;n++;long amt=toLong(p.optString("amount"));total+=amt;if(p.optBoolean("collected")){coll++;colAmt+=amt;}else noColl.add(p);if(p.optBoolean("allocated"))alloc++;else noAlloc.add(p);}
        add(tv("آمار کل\nتعداد خرید: "+n+"\nمجموع مبلغ: "+AppData.fmt(""+total)+" ریال\nوصول‌شده: "+coll+" خرید، "+AppData.fmt(""+colAmt)+" ریال\nوصول‌نشده: "+(n-coll)+" خرید\nتخصیص‌شده: "+alloc+" خرید\nتخصیص‌نشده: "+(n-alloc)+" خرید",15));
        add(tv("شماره‌های وصول‌نشده",15));addPurchaseNumberList(noColl);add(tv("شماره‌های تخصیص‌نشده",15));addPurchaseNumberList(noAlloc);
        add(tv("فیلتر سریع خریدهای این خریدار",16));Button all=btn("همه خریدها"),c1=btn("وصول شده"),c2=btn("وصول نشده"),c3=btn("تخصیص شده"),c4=btn("تخصیص نشده");add(all);add(c1);add(c2);add(c3);add(c4);
        all.setOnClickListener(v->openPage(()->listPurchases(buyer)));c1.setOnClickListener(v->openPage(()->listPurchasesWithStatus(buyer,1)));c2.setOnClickListener(v->openPage(()->listPurchasesWithStatus(buyer,2)));c3.setOnClickListener(v->openPage(()->listPurchasesWithStatus(buyer,3)));c4.setOnClickListener(v->openPage(()->listPurchasesWithStatus(buyer,4)));
        Button stat=btn("📊 آمار بازه زمانی");stat.setOnClickListener(v->buyerStats(buyer));add(stat);Button back=btn("← بازگشت");back.setOnClickListener(v->back());add(back);finishScreen("پرونده خریدار");
    }
    void addPurchaseNumberList(ArrayList<JSONObject> items){if(items.isEmpty()){add(tv("ندارد",14));return;}for(JSONObject p:items){Button b=btn("📄 "+p.optString("purchaseNo"));b.setOnClickListener(v->openPage(()->details(p)));add(b);}}
    void listPurchasesWithStatus(String buyer,int status){listPurchases(buyer,status);}
    void buyerStats(String buyer){base("آمار بازه‌ای: "+buyer);EditText f=input("از تاریخ");EditText t=input("تا تاریخ");f.setOnClickListener(v->pickDate(f));t.setOnClickListener(v->pickDate(t));Button b=btn("نمایش آمار");add(b);TextView out=tv("",15);add(out);b.setOnClickListener(v->{int n=0,c=0,a=0;long total=0;JSONArray p=AppData.arr(data,"purchases");for(int i=0;i<p.length();i++){JSONObject x=p.optJSONObject(i);if(x==null||!buyer.equals(x.optString("buyer")))continue;String d=x.optString("buyDate");if(!f.getText().toString().isEmpty()&&d.compareTo(f.getText().toString())<0)continue;if(!t.getText().toString().isEmpty()&&d.compareTo(t.getText().toString())>0)continue;n++;total+=toLong(x.optString("amount"));if(x.optBoolean("collected"))c++;if(x.optBoolean("allocated"))a++;}out.setText("تعداد خرید: "+n+"\nمجموع مبلغ: "+AppData.fmt(""+total)+" ریال\nوصول‌شده: "+c+"\nتخصیص‌شده: "+a);});Button back=btn("← بازگشت");back.setOnClickListener(v->back());add(back);finishScreen("آمار");}
    long toLong(String s){try{return Long.parseLong(AppData.digits(s));}catch(Exception e){return 0;}}

    void deadlines(){
        base("سررسیدها");add(tv("حالت نمایش",16));Spinner mode=spinner(new String[]{"نزدیک‌ترین سررسیدها","بازه تاریخی"});add(mode);
        LinearLayout dates=new LinearLayout(this);EditText f=new EditText(this),t=new EditText(this);f.setHint("از تاریخ");t.setHint("تا تاریخ");f.setOnClickListener(v->pickDate(f));t.setOnClickListener(v->pickDate(t));dates.addView(f,new LinearLayout.LayoutParams(0,60,1));dates.addView(t,new LinearLayout.LayoutParams(0,60,1));add(dates);Button show=btn("🔎 نمایش");add(show);LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);add(list);
        Runnable render=()->{list.removeAllViews();ArrayList<JSONObject> l=new ArrayList<>();JSONArray a=AppData.arr(data,"purchases");for(int i=0;i<a.length();i++){JSONObject p=a.optJSONObject(i);if(p==null||p.optBoolean("collected")||p.optString("mainDue").isEmpty())continue;if(mode.getSelectedItemPosition()==1){if(!f.getText().toString().isEmpty()&&p.optString("mainDue").compareTo(f.getText().toString())<0)continue;if(!t.getText().toString().isEmpty()&&p.optString("mainDue").compareTo(t.getText().toString())>0)continue;}l.add(p);}Collections.sort(l,(x,y)->x.optString("mainDue").compareTo(y.optString("mainDue")));for(JSONObject p:l){Button b=btn("👤 "+p.optString("buyer")+" | خرید "+p.optString("purchaseNo")+"\nسررسید اصلی: "+p.optString("mainDue")+(isSoon(p)?"  ⚠ نزدیک":""));b.setOnClickListener(v->openPage(()->details(p)));list.addView(b);}};show.setOnClickListener(v->render.run());render.run();Button back=btn("← بازگشت");back.setOnClickListener(v->back());add(back);finishScreen("سررسیدها");
    }
    boolean isSoon(JSONObject p){String d=p==null?"":p.optString("mainDue");if(d.isEmpty()||p.optBoolean("collected",false))return false;long m=PersianDate.millis(d,"23:59");return m>=System.currentTimeMillis()&&m-System.currentTimeMillis()<7L*86400000L;}

    void manage(String key,String title){base(title);JSONArray a=AppData.arr(data,key);for(int i=0;i<a.length();i++){final int ix=i;LinearLayout r=new LinearLayout(this);TextView t=tv(a.optString(i),16);r.addView(t,new LinearLayout.LayoutParams(0,60,1));Button d=btn("حذف");d.setOnClickListener(v->{a.remove(ix);try{data.put(key,a);AppData.save(this,data);}catch(Exception ignored){}manage(key,title);});r.addView(d,new LinearLayout.LayoutParams(110,60));add(r);}Button ad=btn("➕ افزودن");ad.setOnClickListener(v->addEntry(key,"مورد جدید",()->manage(key,title)));add(ad);Button back=btn("← بازگشت");back.setOnClickListener(v->back());add(back);finishScreen(title);}
    void addEntry(String key,String title,Runnable after){EditText e=new EditText(this);new AlertDialog.Builder(this).setTitle(title).setView(e).setPositiveButton("ذخیره",(d,w)->{try{String v=e.getText().toString().trim();if(v.isEmpty())return;JSONArray a=AppData.arr(data,key);for(int i=0;i<a.length();i++)if(v.equals(a.optString(i)))return;a.put(v);data.put(key,a);AppData.save(this,data);after.run();}catch(Exception ignored){}}).setNegativeButton("لغو",null).show();}

    void reports(){
        base("گزارش‌ها و خروجی Excel");EditText q=input("جستجوی خریدار، شرکت یا شماره خرید");Button b=btn("🔎 جستجو");add(b);LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);add(list);
        Runnable render=()->{list.removeAllViews();String s=q.getText().toString().trim();TreeSet<String> buyers=new TreeSet<>(),companies=new TreeSet<>();JSONArray a=sortedPurchases();for(int i=0;i<a.length();i++){JSONObject p=a.optJSONObject(i);String blob=p.optString("buyer")+" "+p.optString("company")+" "+p.optString("purchaseNo");if(s.isEmpty()||blob.contains(s)){buyers.add(p.optString("buyer"));companies.add(p.optString("company"));}}for(String x:buyers){Button z=btn("👤 "+x);z.setOnClickListener(vv->openPage(()->reportSelection("خریدار: "+x,"buyer",x)));list.addView(z);}for(String x:companies){Button z=btn("🏢 "+x);z.setOnClickListener(vv->openPage(()->reportSelection("شرکت: "+x,"company",x)));list.addView(z);}};
        b.setOnClickListener(v->render.run());render.run();add(tv("ابتدا خریدار یا شرکت را انتخاب کنید؛ سپس خریدها را تیک بزنید و خروجی بگیرید.",13));Button back=btn("← بازگشت");back.setOnClickListener(v->back());add(back);finishScreen("گزارش‌ها و خروجی Excel");
    }
    void reportSelection(String title,String filterKey,String filterVal){
        base(title);EditText q=input("");q.setHint("جستجو: شماره خرید / تاریخ / نام");LinearLayout dates=new LinearLayout(this);EditText f=new EditText(this);f.setHint("از تاریخ");f.setSingleLine(true);EditText t=new EditText(this);t.setHint("تا تاریخ");t.setSingleLine(true);f.setOnClickListener(v->pickDate(f));t.setOnClickListener(v->pickDate(t));dates.addView(f,new LinearLayout.LayoutParams(0,60,1));dates.addView(t,new LinearLayout.LayoutParams(0,60,1));add(dates);
        LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);add(list);ArrayList<CheckBox> checks=new ArrayList<>();ArrayList<JSONObject> objs=new ArrayList<>();JSONArray a=sortedPurchases();
        for(int i=0;i<a.length();i++){JSONObject p=a.optJSONObject(i);if(!filterVal.equals(p.optString(filterKey)))continue;CheckBox c=new CheckBox(this);c.setText("خرید "+p.optString("purchaseNo")+" | "+p.optString("buyDate")+" | "+AppData.fmt(p.optString("amount")));styleReportCheck(c);checks.add(c);objs.add(p);list.addView(c);}
        Button filter=btn("🔎 اعمال فیلتر");filter.setOnClickListener(v->{String s=q.getText().toString().trim();for(int i=0;i<checks.size();i++){JSONObject p=objs.get(i);String blob=p.optString("purchaseNo")+" "+p.optString("buyDate")+" "+p.optString("buyer")+" "+p.optString("company");boolean ok=s.isEmpty()||blob.contains(s);if(!f.getText().toString().isEmpty()&&p.optString("buyDate").compareTo(f.getText().toString())<0)ok=false;if(!t.getText().toString().isEmpty()&&p.optString("buyDate").compareTo(t.getText().toString())>0)ok=false;checks.get(i).setVisibility(ok?View.VISIBLE:View.GONE);}});add(filter);
        Button all=btn("☑ انتخاب همه موارد نمایش‌داده‌شده");all.setOnClickListener(v->{boolean select=false;for(CheckBox c:checks)if(c.getVisibility()==View.VISIBLE&&!c.isChecked()){select=true;break;}for(CheckBox c:checks)if(c.getVisibility()==View.VISIBLE)c.setChecked(select);all.setText(select?"☐ لغو انتخاب همه موارد":"☑ انتخاب همه موارد نمایش‌داده‌شده");});add(all);
        for(CheckBox c:checks){c.setOnCheckedChangeListener((v,checked)->{if(Build.VERSION.SDK_INT>=21)v.setBackgroundColor(checked?UiManager.secondary(this):Color.TRANSPARENT);boolean anyVisible=false,allVisibleChecked=true;for(CheckBox x:checks)if(x.getVisibility()==View.VISIBLE){anyVisible=true;if(!x.isChecked())allVisibleChecked=false;}all.setText(anyVisible&&allVisibleChecked?"☐ لغو انتخاب همه موارد":"☑ انتخاب همه موارد نمایش‌داده‌شده");});}
        Button ex=btn("📊 خروجی Excel موارد انتخاب‌شده");ex.setOnClickListener(v->{JSONArray r=new JSONArray();for(int i=0;i<checks.size();i++)if(checks.get(i).isChecked()&&checks.get(i).getVisibility()==View.VISIBLE)r.put(objs.get(i));if(r.length()==0){for(int i=0;i<checks.size();i++)if(checks.get(i).getVisibility()==View.VISIBLE)r.put(objs.get(i));}exportExcel(r,"report_"+safe(filterVal));});add(ex);
        Button back=btn("← بازگشت");back.setOnClickListener(v->back());add(back);finishScreen(title);for(CheckBox c:checks)styleReportCheck(c);
    }
    void styleReportCheck(CheckBox c){c.setTextColor(UiManager.text(this));c.setTextSize(UiManager.bodySize(this));c.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);c.setPadding(UiManager.dp(this,10),UiManager.dp(this,8),UiManager.dp(this,10),UiManager.dp(this,8));if(Build.VERSION.SDK_INT>=21){int[][] states={{android.R.attr.state_checked},{-android.R.attr.state_checked}};int[] colors={UiManager.primary(this),Color.GRAY};c.setButtonTintList(new ColorStateList(states,colors));}c.setOnCheckedChangeListener((b,checked)->{if(Build.VERSION.SDK_INT>=21){b.setBackgroundColor(checked?UiManager.secondary(this):Color.TRANSPARENT);}});}
    String safe(String s){return s.replaceAll("[\\\\/:*?\"<>|]","_");}
    void exportExcel(JSONArray a,String name){try{String[] h={"نام خریدار","نهاده","شماره خرید","وزن","فی","نوع پرداخت","مبلغ خرید","تاریخ خرید","مقدار ذرت","مقدار سویا","مقدار ریزمغذی","نام شرکت","تاریخ اصلی سررسید","وصول شد","تاریخ وصول","تخصیص شد","تأمین موجودی","مبلغ تأمین‌شده","مانده تأمین","تعداد مراحل تأمین","جزئیات تأمین"};StringBuilder x=new StringBuilder("\uFEFF");for(String z:h)x.append(z).append("\t");x.append("\n");for(int i=0;i<a.length();i++){JSONObject p=a.optJSONObject(i);String[] v={p.optString("buyer"),p.optString("commodity"),p.optString("purchaseNo"),p.optString("weight"),p.optString("fee"),p.optString("payment"),p.optString("amount"),p.optString("buyDate"),p.optString("corn"),p.optString("soy"),p.optString("micronutrient"),p.optString("company"),p.optString("mainDue"),p.optBoolean("collected")?"وصول شده":"وصول نشده",p.optString("collectedDate"),p.optBoolean("allocated")?"تخصیص شده":"تخصیص نشده",(p.optBoolean("inventoryFunded")||fundedTotal(p)>=toLong(p.optString("amount")))?"تأمین تکمیل شده":"تأمین تکمیل نشده",String.valueOf(fundedTotal(p)),String.valueOf(Math.max(0,toLong(p.optString("amount"))-fundedTotal(p))),String.valueOf(p.optJSONArray("fundings")==null?0:p.optJSONArray("fundings").length()),fundingText(p)};for(String z:v)x.append(xml(z)).append("\t");x.append("\n");}File f=new File(getCacheDir(),name+".xls");FileOutputStream o=new FileOutputStream(f);o.write(x.toString().getBytes("UTF-8"));o.close();Intent in=new Intent(Intent.ACTION_SEND);in.setType("application/vnd.ms-excel");in.putExtra(Intent.EXTRA_STREAM,FileProvider.getUriForFile(this,"com.morghtak.kharidmanager.fileprovider",f));in.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(Intent.createChooser(in,"ارسال / ذخیره Excel"));}catch(Exception e){Toast.makeText(this,"خطا در ساخت Excel",Toast.LENGTH_LONG).show();}}
    String fundingText(JSONObject p){StringBuilder b=new StringBuilder();JSONArray a=p.optJSONArray("fundings");if(a!=null)for(int i=0;i<a.length();i++){JSONObject z=a.optJSONObject(i);if(z!=null)b.append("مرحله ").append(i+1).append(": ").append(z.optString("date")).append(" | ").append(z.optString("amount")).append(" | ").append(z.optString("source")).append(" -> ").append(z.optString("destination")).append(" ; ");}return b.toString();}
    String xml(String s){return s.replace("&","&amp;").replace("\t"," ").replace("\n"," ").replace("\r"," ");}

    void backup(){base("پشتیبان‌گیری و بازیابی");add(tv("فایل پشتیبان واقعی روی حافظه گوشی ذخیره می‌شود و می‌توانید آن را به گوشی دیگری منتقل کنید.",16));Button b=btn("💾 ذخیره فایل پشتیبان در گوشی");b.setOnClickListener(v->{try{Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.setType("application/json");i.putExtra(Intent.EXTRA_TITLE,"kharidmanager_backup.json");startActivityForResult(i,90);}catch(Exception e){Toast.makeText(this,"امکان ساخت فایل پشتیبان وجود ندارد",Toast.LENGTH_LONG).show();}});add(b);b=btn("📥 بازیابی از فایل پشتیبان");b.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("application/json");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,91);});add(b);Button back=btn("← بازگشت");back.setOnClickListener(v->back());add(back);finishScreen("پشتیبان‌گیری و بازیابی");}
    @Override protected void onActivityResult(int r,int c,Intent i){super.onActivityResult(r,c,i);UiManager.handleLogoResult(this,r,c,i);if(r==90&&c==RESULT_OK&&i!=null&&i.getData()!=null)try{OutputStream out=getContentResolver().openOutputStream(i.getData());if(out==null)throw new IOException();out.write(data.toString(2).getBytes("UTF-8"));out.close();Toast.makeText(this,"فایل پشتیبان با موفقیت ذخیره شد",Toast.LENGTH_LONG).show();}catch(Exception e){Toast.makeText(this,"خطا در ذخیره فایل پشتیبان",Toast.LENGTH_LONG).show();}else if(r==91&&c==RESULT_OK&&i!=null&&i.getData()!=null){final Intent ri=i;new AlertDialog.Builder(this).setTitle("تأیید بازیابی").setMessage("بازیابی اطلاعات، اطلاعات فعلی این گوشی را با فایل پشتیبان جایگزین می‌کند. ادامه می‌دهید؟").setPositiveButton("بله، بازیابی شود",(d,w)->restoreBackup(ri.getData())).setNegativeButton("لغو",null).show();}}
    void restoreBackup(android.net.Uri uri){try{InputStream in=getContentResolver().openInputStream(uri);ByteArrayOutputStream o=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;while((n=in.read(b))>0)o.write(b,0,n);in.close();JSONObject restored=new JSONObject(new String(o.toByteArray(),"UTF-8"));if(!restored.has("purchases")||!restored.has("buyers")||!restored.has("companies")||!restored.has("commodities"))throw new Exception();data=restored;AppData.save(this,data);JSONArray a=AppData.arr(data,"purchases");for(int k=0;k<a.length();k++){JSONObject p=a.optJSONObject(k);if(p!=null&&p.optBoolean("alarm"))schedule(this,p);}Toast.makeText(this,"بازیابی با موفقیت انجام شد",Toast.LENGTH_LONG).show();goHome();}catch(Exception e){Toast.makeText(this,"فایل پشتیبان معتبر نیست",Toast.LENGTH_LONG).show();}}
    public static JSONObject findStatic(Context c,String id){try{JSONArray a=AppData.arr(AppData.root(c),"purchases");for(int i=0;i<a.length();i++){JSONObject p=a.optJSONObject(i);if(p!=null&&id!=null&&id.equals(p.optString("id")))return p;}}catch(Exception ignored){}return null;}
    public static void schedule(Context c,JSONObject p){try{if(!p.optBoolean("alarm"))return;long at=PersianDate.millis(p.optString("mainDue"),p.optString("alarmTime","10:00"))-p.optInt("alarmDays",1)*86400000L;if(p.optBoolean("alarmRepeat")&&at<=System.currentTimeMillis()){while(at<=System.currentTimeMillis())at+=86400000L;}if(at<=System.currentTimeMillis())return;AlarmManager am=(AlarmManager)c.getSystemService(ALARM_SERVICE);Intent in=new Intent(c,AlarmReceiver.class);in.putExtra("id",p.optString("id"));in.putExtra("title","خرید "+p.optString("purchaseNo")+" - "+p.optString("buyer"));PendingIntent pi=PendingIntent.getBroadcast(c,p.optString("id").hashCode(),in,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);if(Build.VERSION.SDK_INT>=31&&am.canScheduleExactAlarms())am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,pi);else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,pi);}catch(Exception ignored){}}
    public static void scheduleNext(Context c,JSONObject p){try{if(!p.optBoolean("alarmRepeat"))return;String tm=p.optString("alarmTime","10:00");long due=PersianDate.millis(p.optString("mainDue"),tm);AlarmManager am=(AlarmManager)c.getSystemService(ALARM_SERVICE);Calendar cal=Calendar.getInstance();cal.add(Calendar.DAY_OF_YEAR,1);String[] hm=tm.split(":");try{cal.set(Calendar.HOUR_OF_DAY,Integer.parseInt(hm[0]));cal.set(Calendar.MINUTE,Integer.parseInt(hm[1]));}catch(Exception ignored){}cal.set(Calendar.SECOND,0);cal.set(Calendar.MILLISECOND,0);long at=cal.getTimeInMillis();if(at>due)return;Intent in=new Intent(c,AlarmReceiver.class);in.putExtra("id",p.optString("id"));in.putExtra("title","خرید "+p.optString("purchaseNo")+" - "+p.optString("buyer"));PendingIntent pi=PendingIntent.getBroadcast(c,p.optString("id").hashCode(),in,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);if(Build.VERSION.SDK_INT>=31&&am.canScheduleExactAlarms())am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,pi);else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,pi);}catch(Exception ignored){}}
    public static void cancelAlarm(Context c,JSONObject p){try{AlarmManager am=(AlarmManager)c.getSystemService(ALARM_SERVICE);Intent in=new Intent(c,AlarmReceiver.class);PendingIntent pi=PendingIntent.getBroadcast(c,p.optString("id").hashCode(),in,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);am.cancel(pi);pi.cancel();}catch(Exception ignored){}}

    // Full Persian calendar dialog.
    class PersianCalendarDialog extends Dialog {
        EditText target; int jy,jm,jd; TextView title; LinearLayout grid;
        PersianCalendarDialog(Context c,EditText t){super(c);target=t;String current=t==null?"":t.getText().toString();int[] d=parseJ(current);if(d==null)d=parseJ(PersianDate.today());jy=d[0];jm=d[1];jd=d[2];}
        int[] parseJ(String s){try{String[] a=s.split("/");if(a.length==3)return new int[]{Integer.parseInt(AppData.digits(a[0])),Integer.parseInt(AppData.digits(a[1])),Integer.parseInt(AppData.digits(a[2]))};}catch(Exception ignored){}return null;}
        @Override protected void onCreate(Bundle b){
            super.onCreate(b);LinearLayout box=new LinearLayout(MainActivity.this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(UiManager.dp(MainActivity.this,10),UiManager.dp(MainActivity.this,8),UiManager.dp(MainActivity.this,10),UiManager.dp(MainActivity.this,8));box.setBackgroundColor(UiManager.background(MainActivity.this));
            title=new TextView(MainActivity.this);title.setGravity(Gravity.CENTER);title.setTextSize(UiManager.titleSize(MainActivity.this));title.setTypeface(UiManager.selectedTypeface(MainActivity.this,Typeface.BOLD));title.setTextColor(UiManager.text(MainActivity.this));box.addView(title,new LinearLayout.LayoutParams(-1,UiManager.dp(MainActivity.this,56)));
            LinearLayout nav=new LinearLayout(MainActivity.this);Button prevYear=btn("‹ سال قبل"),prev=btn("‹ ماه قبل"),next=btn("ماه بعد ›"),nextYear=btn("سال بعد ›");nav.addView(prevYear,new LinearLayout.LayoutParams(0,UiManager.dp(thisContext(),48),1));nav.addView(prev,new LinearLayout.LayoutParams(0,UiManager.dp(thisContext(),48),1));nav.addView(next,new LinearLayout.LayoutParams(0,UiManager.dp(thisContext(),48),1));nav.addView(nextYear,new LinearLayout.LayoutParams(0,UiManager.dp(thisContext(),48),1));box.addView(nav);
            ScrollView sv=new ScrollView(MainActivity.this);grid=new LinearLayout(MainActivity.this);grid.setOrientation(LinearLayout.VERTICAL);sv.addView(grid,new ViewGroup.LayoutParams(-1,-2));box.addView(sv,new LinearLayout.LayoutParams(-1,UiManager.dp(MainActivity.this,330)));
            LinearLayout bottom=new LinearLayout(MainActivity.this);Button cancel=btn("لغو"),ok=btn("تأیید");bottom.addView(cancel,new LinearLayout.LayoutParams(0,UiManager.dp(MainActivity.this,50),1));bottom.addView(ok,new LinearLayout.LayoutParams(0,UiManager.dp(MainActivity.this,50),1));box.addView(bottom);
            prevYear.setOnClickListener(v->{jy--;jd=Math.min(jd,monthDays(jy,jm));render();});nextYear.setOnClickListener(v->{jy++;jd=Math.min(jd,monthDays(jy,jm));render();});prev.setOnClickListener(v->{jm--;if(jm<1){jm=12;jy--;}jd=Math.min(jd,monthDays(jy,jm));render();});next.setOnClickListener(v->{jm++;if(jm>12){jm=1;jy++;}jd=Math.min(jd,monthDays(jy,jm));render();});cancel.setOnClickListener(v->dismiss());ok.setOnClickListener(v->{if(target!=null)target.setText(String.format(Locale.US,"%04d/%02d/%02d",jy,jm,jd));dismiss();});setContentView(box);Window w=getWindow();if(w!=null){w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));w.setLayout(-1,-2);}render();
        }
        Context thisContext(){return MainActivity.this;}
        int monthDays(int y,int m){return m<=6?31:(m<=11?30:(isLeap(y)?30:29));}
        boolean isLeap(int y){long a=PersianDate.millis(String.format(Locale.US,"%04d/%02d/%02d",y,12,1),"00:00");long z=PersianDate.millis(String.format(Locale.US,"%04d/%02d/%02d",y+1,1,1),"00:00");return z-a>365L*86400000L;}
        void render(){
            int body=Math.max(12,Math.round(UiManager.bodySize(MainActivity.this)));int button=Math.max(12,Math.round(UiManager.buttonSize(MainActivity.this)));String font=UiManager.font(MainActivity.this);title.setText("تقویم شمسی  "+jy+" / "+monthName(jm));title.setTypeface(Typeface.create(font,Typeface.BOLD));grid.removeAllViews();String[] wd={"ش","ی","د","س","چ","پ","ج"};LinearLayout heads=new LinearLayout(MainActivity.this);for(String s:wd){TextView x=new TextView(MainActivity.this);x.setText(s);x.setTextSize(body-1);x.setTypeface(Typeface.create(font,Typeface.BOLD));x.setTextColor(UiManager.text(MainActivity.this));x.setGravity(Gravity.CENTER);heads.addView(x,new LinearLayout.LayoutParams(0,UiManager.dp(MainActivity.this,36),1));}grid.addView(heads);
            int[] g=PersianDate.toGregorian(jy,jm,1);Calendar c=Calendar.getInstance();c.set(g[0],g[1]-1,g[2]);int first=(c.get(Calendar.DAY_OF_WEEK)%7);int days=monthDays(jy,jm),day=1;
            for(int row=0;row<6&&day<=days;row++){LinearLayout r=new LinearLayout(MainActivity.this);for(int col=0;col<7;col++){Button d=btn("");d.setTextSize(button);d.setTypeface(Typeface.create(font,Typeface.BOLD));d.setMinHeight(UiManager.dp(MainActivity.this,42));if((row==0&&col<first)||day>days)d.setEnabled(false);else{final int dd=day;d.setText(""+day);if(day==jd){d.setSelected(true);d.setBackgroundColor(UiManager.primary(MainActivity.this));d.setTextColor(Color.WHITE);}else{d.setBackgroundColor(UiManager.card(MainActivity.this));d.setTextColor(UiManager.text(MainActivity.this));}d.setOnClickListener(v->{jd=dd;render();});day++;}r.addView(d,new LinearLayout.LayoutParams(0,UiManager.dp(MainActivity.this,46),1));}grid.addView(r);}
        }
        String monthName(int m){String[] n={"فروردین","اردیبهشت","خرداد","تیر","مرداد","شهریور","مهر","آبان","آذر","دی","بهمن","اسفند"};return n[m-1];}
    }
}
