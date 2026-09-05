package com.morghtak.kharidmanager;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import android.text.*;
import androidx.core.app.ActivityCompat;
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
    boolean restoring = false;

    final String[] labels = {
            "نام خریدار", "نهاده", "شماره خرید", "وزن (کیلوگرم)",
            "فی (ریال)", "قیمت توافقی (ریال)", "نوع پرداخت", "مبلغ خرید (ریال)",
            "تاریخ خرید", "مقدار ذرت (کیلوگرم)", "مقدار سویا (کیلوگرم)", "نام شرکت",
            "تاریخ اصلی سررسید", "تاریخ فرعی سررسید"
    };

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        data = AppData.root(this);
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 22);
        openPage(() -> home(), false);
    }

    void openPage(Runnable page) { openPage(page, true); }
    void openPage(Runnable page, boolean push) {
        if (page == null) return;
        if (push && currentPage != null && !restoring) history.push(currentPage);
        currentPage = page;
        page.run();
    }
    void goBackPage() {
        if (!history.isEmpty()) {
            Runnable p = history.pop();
            restoring = true;
            currentPage = p;
            p.run();
            restoring = false;
        } else {
            finish();
        }
    }
    void goHome() {
        history.clear();
        openPage(() -> home(), false);
    }
    @Override public void onBackPressed() {
        if (!history.isEmpty()) goBackPage(); else super.onBackPressed();
    }

    TextView tv(String s, int size) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextSize(size); t.setPadding(18, 12, 18, 12);
        return t;
    }
    Button btn(String s) {
        Button b = new Button(this); b.setText(s); b.setAllCaps(false); return b;
    }
    void base(String title) {
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        TextView h = tv(title, 21); h.setTextColor(Color.WHITE); h.setGravity(Gravity.CENTER);
        root.addView(h, new LinearLayout.LayoutParams(-1, UiManager.dp(this, 64)));
        setContentView(root);
    }
    void finishScreen(String title) {
        UiManager.decorate(this, root, title);
        hideKeyboard();
    }
    void hideKeyboard() {
        try { ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(root.getWindowToken(),0); } catch(Exception ignored) {}
    }
    void addView(View v) { root.addView(v, new LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT)); }

    void home() {
        base("مدیریت خرید و سررسید");
        TextView summary = tv("", 16); addView(summary);
        JSONArray p = AppData.arr(data,"purchases"); int due=0;
        for(int i=0;i<p.length();i++) if(isSoon(p.optJSONObject(i))) due++;
        summary.setText("تعداد خریدهای ثبت‌شده: " + p.length() + "\nسررسیدهای نزدیک: " + due);

        Button n=btn("➕ ثبت خرید جدید"); n.setOnClickListener(v->openPage(()->form(null))); addView(n);
        Button l=btn("🛒 خریدهای ثبت‌شده"); l.setOnClickListener(v->openPage(()->listPurchases(null))); addView(l);
        Button b=btn("👤 خریداران و پرونده هر خریدار"); b.setOnClickListener(v->openPage(()->buyersPage())); addView(b);
        Button r=btn("🔔 سررسیدها"); r.setOnClickListener(v->openPage(()->deadlines())); addView(r);
        Button c=btn("🏢 شرکت‌ها"); c.setOnClickListener(v->openPage(()->manage("companies","شرکت‌ها"))); addView(c);
        Button m=btn("🌾 نهاده‌ها"); m.setOnClickListener(v->openPage(()->manage("commodities","نهاده‌ها"))); addView(m);
        Button rep=btn("📊 گزارش‌ها و خروجی Excel"); rep.setOnClickListener(v->openPage(()->reports())); addView(rep);
        Button bk=btn("💾 پشتیبان‌گیری / بازیابی"); bk.setOnClickListener(v->openPage(()->backup())); addView(bk);
        finishScreen("مدیریت خرید و سررسید");
    }

    boolean isSoon(JSONObject p) {
        if(p==null) return false; String d=p.optString("subDue",""); if(d.isEmpty()) return false;
        long m=PersianDate.millis(d,"23:59"); return m>System.currentTimeMillis() && m-System.currentTimeMillis()<7L*86400000L;
    }

    EditText input(String hint) {
        EditText e=new EditText(this); e.setHint(hint); e.setSingleLine(true); e.setPadding(16,8,16,8);
        e.setTextSize(UiManager.fieldSize(this));
        inputs.add(e); addView(e); return e;
    }
    void addLabel(String s){ addView(tv(s,14)); }

    void form(JSONObject old) {
        base(old==null?"ثبت خرید جدید":"ویرایش خرید"); inputs.clear(); JSONObject p=old;
        for(int i=0;i<labels.length;i++){
            addLabel((i+1)+". "+labels[i]);
            if(i==0){ buyerSp=spinner(AppData.arr(data,"buyers")); addView(buyerSp); addHidden(); }
            else if(i==1){ commoditySp=spinner(AppData.arr(data,"commodities")); addView(commoditySp); addHidden(); }
            else if(i==6){ paymentSp=spinner(AppData.PAYMENTS); addView(paymentSp); addHidden(); }
            else if(i==11){ companySp=spinner(AppData.arr(data,"companies")); addView(companySp); addHidden(); }
            else {
                EditText e=input(labels[i]);
                if(i==8||i==12||i==13) e.setOnClickListener(v->pickDate(e));
                if(i==3||i==4||i==5||i==7||i==9||i==10){ e.setInputType(2); addGrouping(e); }
            }
        }
        if(p!=null) fill(p); else if(inputs.size()>8) inputs.get(8).setText(PersianDate.today());
        Button alarm=btn("🔔 تنظیم هشدار تاریخ فرعی"); alarm.setOnClickListener(v->alarmDialog(inputs.get(13),p)); addView(alarm);
        Button save=btn("✓ ذخیره خرید"); save.setOnClickListener(v->savePurchase(old)); addView(save);
        Button back=btn("← بازگشت"); back.setOnClickListener(v->goBackPage()); addView(back);
        finishScreen(old==null?"ثبت خرید جدید":"ویرایش خرید");
    }
    void addHidden(){ EditText e=new EditText(this); e.setVisibility(View.GONE); inputs.add(e); }

    Spinner spinner(JSONArray a){ ArrayList<String>x=new ArrayList<>(); for(int i=0;i<a.length();i++)x.add(a.optString(i)); return spinner(x.toArray(new String[0])); }
    Spinner spinner(String[] a){ Spinner s=new Spinner(this); s.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,a)); return s; }
    void setSpinner(Spinner s,String v){ if(s==null)return; for(int i=0;i<s.getCount();i++) if(String.valueOf(s.getItemAtPosition(i)).equals(v)){s.setSelection(i);break;} }
    void addGrouping(final EditText e){ e.addTextChangedListener(new TextWatcher(){boolean busy; public void beforeTextChanged(CharSequence s,int st,int c,int a){} public void onTextChanged(CharSequence s,int st,int b,int c){} public void afterTextChanged(Editable ed){if(busy)return;String raw=AppData.digits(ed.toString());if(raw.isEmpty())return;busy=true;String f=AppData.fmt(raw);e.setText(f);e.setSelection(f.length());busy=false;}}); }

    void pickDate(EditText e){
        LinearLayout l=new LinearLayout(this); l.setPadding(16,8,16,8);
        EditText y=new EditText(this),m=new EditText(this),d=new EditText(this); y.setHint("سال");m.setHint("ماه");d.setHint("روز");
        for(EditText x:new EditText[]{y,m,d}){x.setInputType(2);l.addView(x,new LinearLayout.LayoutParams(0,60,1));}
        new AlertDialog.Builder(this).setTitle("انتخاب تاریخ شمسی").setView(l).setPositiveButton("تأیید",(di,w)->{try{String ys=y.getText().toString().trim();if(ys.isEmpty())ys=PersianDate.today().substring(0,4);String ms=m.getText().toString().trim();if(ms.isEmpty())ms="01";String ds=d.getText().toString().trim();if(ds.isEmpty())ds="01";e.setText(String.format(Locale.US,"%s/%02d/%02d",ys,Integer.parseInt(ms),Integer.parseInt(ds)));}catch(Exception ignored){Toast.makeText(this,"تاریخ واردشده معتبر نیست",Toast.LENGTH_SHORT).show();}}).setNegativeButton("لغو",null).show();
    }

    void fill(JSONObject p){
        String[] k={"buyer","commodity","purchaseNo","weight","fee","agreed","payment","amount","buyDate","corn","soy","company","mainDue","subDue"};
        for(int i=0;i<k.length;i++) if(i!=0&&i!=1&&i!=6&&i!=11) inputs.get(i).setText(p.optString(k[i],""));
        setSpinner(buyerSp,p.optString("buyer")); setSpinner(commoditySp,p.optString("commodity")); setSpinner(paymentSp,p.optString("payment")); setSpinner(companySp,p.optString("company"));
    }

    void savePurchase(JSONObject old){
        try{
            JSONObject r=new JSONObject();
            String[] k={"buyer","commodity","purchaseNo","weight","fee","agreed","payment","amount","buyDate","corn","soy","company","mainDue","subDue"};
            for(int i=0;i<k.length;i++){
                String v=inputs.get(i).getText().toString();
                if(i==0)v=buyerSp==null?"":String.valueOf(buyerSp.getSelectedItem());
                if(i==1)v=commoditySp==null?"":String.valueOf(commoditySp.getSelectedItem());
                if(i==6)v=paymentSp==null?"":String.valueOf(paymentSp.getSelectedItem());
                if(i==11)v=companySp==null?"":String.valueOf(companySp.getSelectedItem());
                r.put(k[i],v);
            }
            if(r.optString("buyer").trim().isEmpty()){Toast.makeText(this,"نام خریدار را انتخاب کنید",Toast.LENGTH_SHORT).show();return;}
            r.put("id",old==null?UUID.randomUUID().toString():old.optString("id"));
            r.put("alarm",old!=null&&old.optBoolean("alarm",false)); r.put("alarmDays",old==null?1:old.optInt("alarmDays",1)); r.put("alarmTime",old==null?"10:00":old.optString("alarmTime","10:00"));
            JSONArray a=AppData.arr(data,"purchases"); boolean replaced=false;
            if(old!=null) for(int i=0;i<a.length();i++) if(a.getJSONObject(i).optString("id").equals(r.optString("id"))){a.put(i,r);replaced=true;break;}
            if(!replaced)a.put(r); data.put("purchases",a);
            addUnique("buyers",r.optString("buyer")); addUnique("companies",r.optString("company")); AppData.save(this,data);
            if(r.optBoolean("alarm"))schedule(this,r);
            Toast.makeText(this,"خرید با موفقیت ذخیره شد",Toast.LENGTH_SHORT).show(); goHome();
        }catch(Exception e){Toast.makeText(this,"خطا در ذخیره اطلاعات",Toast.LENGTH_LONG).show();}
    }
    void addUnique(String key,String val)throws Exception{if(val==null||val.trim().isEmpty())return;JSONArray a=AppData.arr(data,key);for(int i=0;i<a.length();i++)if(a.optString(i).equals(val))return;a.put(val);}

    void listPurchases(String fixedBuyer){
        base(fixedBuyer==null?"خریدهای ثبت‌شده":"خریدهای "+fixedBuyer);
        EditText q=input("جستجو: نام، شماره خرید، شرکت یا نهاده");
        LinearLayout dates=new LinearLayout(this); dates.setOrientation(LinearLayout.HORIZONTAL);
        EditText from=new EditText(this),to=new EditText(this); from.setHint("از تاریخ");to.setHint("تا تاریخ");from.setSingleLine(true);to.setSingleLine(true);from.setOnClickListener(v->pickDate(from));to.setOnClickListener(v->pickDate(to));
        dates.addView(from,new LinearLayout.LayoutParams(0,60,1)); dates.addView(to,new LinearLayout.LayoutParams(0,60,1)); root.addView(dates);
        Button go=btn("🔎 جستجو / فیلتر");addView(go);
        LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);root.addView(list,new LinearLayout.LayoutParams(-1,ViewGroup.LayoutParams.WRAP_CONTENT));
        Runnable render=()->{
            list.removeAllViews(); String s=q.getText().toString().trim(); String f=from.getText().toString().trim(),t=to.getText().toString().trim(); JSONArray a=AppData.arr(data,"purchases"); int count=0;
            for(int i=a.length()-1;i>=0;i--){JSONObject p=a.optJSONObject(i);if(p==null)continue; if(fixedBuyer!=null&&!fixedBuyer.equals(p.optString("buyer")))continue;String blob=p.optString("buyer")+" "+p.optString("purchaseNo")+" "+p.optString("company")+" "+p.optString("commodity");if(!s.isEmpty()&&!blob.contains(s))continue;String d=p.optString("buyDate");if(!f.isEmpty()&&!d.isEmpty()&&d.compareTo(f)<0)continue;if(!t.isEmpty()&&!d.isEmpty()&&d.compareTo(t)>0)continue;count++;
                LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(10,8,10,8);
                Button b=btn("خرید شماره "+p.optString("purchaseNo")+"\n"+p.optString("buyer")+" | "+p.optString("commodity")+" | تاریخ "+p.optString("buyDate"));b.setOnClickListener(v->openPage(()->details(p)));card.addView(b);list.addView(card);
            }
            TextView total=tv("تعداد نتایج: "+count,15);list.addView(total,0);
        };
        go.setOnClickListener(v->render.run());render.run();
        Button back=btn("← بازگشت");back.setOnClickListener(v->goBackPage());addView(back);
        finishScreen(fixedBuyer==null?"خریدهای ثبت‌شده":"خریدهای "+fixedBuyer);
    }

    void buyersPage(){
        base("خریداران"); EditText q=input("جستجوی نام خریدار"); Button go=btn("🔎 جستجو");addView(go);
        LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);root.addView(list,new LinearLayout.LayoutParams(-1,ViewGroup.LayoutParams.WRAP_CONTENT));
        Runnable render=()->{list.removeAllViews();JSONArray buyers=AppData.arr(data,"buyers"),p=AppData.arr(data,"purchases");String s=q.getText().toString().trim();
            for(int i=0;i<buyers.length();i++){String buyer=buyers.optString(i);if(!s.isEmpty()&&!buyer.contains(s))continue;int n=0;long total=0;for(int j=0;j<p.length();j++){JSONObject x=p.optJSONObject(j);if(x!=null&&buyer.equals(x.optString("buyer"))){n++;try{total+=Long.parseLong(AppData.digits(x.optString("amount","0")));}catch(Exception ignored){}}}
                Button b=btn("👤 "+buyer+"\nتعداد خرید: "+n+" | مجموع مبلغ: "+AppData.fmt(""+total)+" ریال");b.setOnClickListener(v->openPage(()->listPurchases(buyer)));list.addView(b);
            }
        };go.setOnClickListener(v->render.run());render.run();
        Button add=btn("➕ افزودن خریدار");add.setOnClickListener(v->addEntry("buyers","خریدار جدید",()->buyersPage()));addView(add);
        Button back=btn("← بازگشت");back.setOnClickListener(v->goBackPage());addView(back);finishScreen("خریداران");
    }

    void details(JSONObject p){
        base("جزئیات کامل خرید");
        TextView buyer=tv("👤 "+p.optString("buyer")+"\nشماره خرید: "+p.optString("purchaseNo"),19);addView(buyer);
        String[] k={"buyer","commodity","purchaseNo","weight","fee","agreed","payment","amount","buyDate","corn","soy","company","mainDue","subDue"};
        for(int i=0;i<labels.length;i++) addView(tv(labels[i]+": "+p.optString(k[i],"-"),16));
        addView(tv("هشدار تاریخ فرعی: "+(p.optBoolean("alarm")?"فعال - "+p.optInt("alarmDays")+" روز قبل، ساعت "+p.optString("alarmTime"):"غیرفعال"),16));
        Button ex=btn("📊 خروجی Excel همین سفارش");ex.setOnClickListener(v->exportExcel(new JSONArray().put(p),"purchase_"+p.optString("purchaseNo")));addView(ex);
        Button ed=btn("✏️ ویرایش");ed.setOnClickListener(v->openPage(()->form(p)));addView(ed);
        Button same=btn("👤 همه خریدهای این خریدار");same.setOnClickListener(v->openPage(()->listPurchases(p.optString("buyer"))));addView(same);
        Button del=btn("🗑 حذف");del.setOnClickListener(v->confirmDelete(p));addView(del);
        Button back=btn("← بازگشت");back.setOnClickListener(v->goBackPage());addView(back);finishScreen("جزئیات کامل خرید");
    }
    void confirmDelete(JSONObject p){new AlertDialog.Builder(this).setTitle("حذف خرید").setMessage("این خرید حذف شود؟").setPositiveButton("حذف",(d,w)->delete(p)).setNegativeButton("لغو",null).show();}
    void delete(JSONObject p){try{JSONArray a=AppData.arr(data,"purchases"),b=new JSONArray();for(int i=0;i<a.length();i++)if(!a.getJSONObject(i).optString("id").equals(p.optString("id")))b.put(a.getJSONObject(i));data.put("purchases",b);AppData.save(this,data);Toast.makeText(this,"خرید حذف شد",Toast.LENGTH_SHORT).show();goHome();}catch(Exception ignored){}}

    void deadlines(){
        base("سررسیدها"); JSONArray a=AppData.arr(data,"purchases");
        for(int i=0;i<a.length();i++){JSONObject p=a.optJSONObject(i);if(p==null)continue;String d=p.optString("subDue");if(d.isEmpty())continue;String status=isSoon(p)?"⚠ نزدیک سررسید":"";Button b=btn("خرید "+p.optString("purchaseNo")+" | "+p.optString("buyer")+"\nسررسید فرعی: "+d+"  "+status+"\n"+(p.optBoolean("alarm")?"🔔 آلارم فعال":"بدون آلارم"));b.setOnClickListener(v->openPage(()->details(p)));addView(b);}
        Button back=btn("← بازگشت");back.setOnClickListener(v->goBackPage());addView(back);finishScreen("سررسیدها");
    }

    void manage(String key,String title){
        base(title); JSONArray a=AppData.arr(data,key);LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);root.addView(list,new LinearLayout.LayoutParams(-1,ViewGroup.LayoutParams.WRAP_CONTENT));
        for(int i=0;i<a.length();i++){final int ix=i;LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);TextView t=tv(a.optString(i),16);row.addView(t,new LinearLayout.LayoutParams(0,60,1));Button d=btn("حذف");d.setOnClickListener(v->{a.remove(ix);try{data.put(key,a);AppData.save(this,data);}catch(Exception ignored){}manage(key,title);});row.addView(d,new LinearLayout.LayoutParams(120,60));list.addView(row);}
        Button add=btn("➕ افزودن");add.setOnClickListener(v->addEntry(key,"مورد جدید",()->manage(key,title)));addView(add);Button back=btn("← بازگشت");back.setOnClickListener(v->goBackPage());addView(back);finishScreen(title);
    }
    void addEntry(String key,String title,Runnable after){EditText e=new EditText(this);e.setHint(title);new AlertDialog.Builder(this).setTitle(title).setView(e).setPositiveButton("ذخیره",(d,w)->{String v=e.getText().toString().trim();if(!v.isEmpty())try{JSONArray a=AppData.arr(data,key);for(int i=0;i<a.length();i++)if(a.optString(i).equals(v))return;a.put(v);data.put(key,a);AppData.save(this,data);after.run();}catch(Exception ignored){}}).setNegativeButton("لغو",null).show();}

    void reports(){
        base("گزارش‌ها و خروجی Excel");EditText buyer=input("خریدار (اختیاری)");EditText company=input("شرکت (اختیاری)");EditText from=input("از تاریخ (اختیاری)");EditText to=input("تا تاریخ (اختیاری)");from.setOnClickListener(v->pickDate(from));to.setOnClickListener(v->pickDate(to));Button show=btn("نمایش گزارش");addView(show);TextView out=tv("",16);addView(out);
        show.setOnClickListener(v->{JSONArray r=new JSONArray();long total=0;JSONArray a=AppData.arr(data,"purchases");for(int i=0;i<a.length();i++){JSONObject p=a.optJSONObject(i);if(p==null)continue;String d=p.optString("buyDate");if(!buyer.getText().toString().trim().isEmpty()&&!p.optString("buyer").contains(buyer.getText().toString().trim()))continue;if(!company.getText().toString().trim().isEmpty()&&!p.optString("company").contains(company.getText().toString().trim()))continue;if(!from.getText().toString().trim().isEmpty()&&d.compareTo(from.getText().toString().trim())<0)continue;if(!to.getText().toString().trim().isEmpty()&&d.compareTo(to.getText().toString().trim())>0)continue;r.put(p);try{total+=Long.parseLong(AppData.digits(p.optString("amount","0")));}catch(Exception ignored){}}out.setText("تعداد خرید: "+r.length()+"\nمجموع مبلغ خرید: "+AppData.fmt(""+total)+" ریال");show.setTag(r);});
        Button ex=btn("📊 خروجی Excel گزارش");ex.setOnClickListener(v->{Object tag=show.getTag();if(tag instanceof JSONArray)exportExcel((JSONArray)tag,"report");else Toast.makeText(this,"ابتدا گزارش را نمایش دهید",Toast.LENGTH_SHORT).show();});addView(ex);Button back=btn("← بازگشت");back.setOnClickListener(v->goBackPage());addView(back);finishScreen("گزارش‌ها و خروجی Excel");
    }

    public static void schedule(Context c,JSONObject p){try{if(!p.optBoolean("alarm",false))return;long due=PersianDate.millis(p.optString("subDue"),p.optString("alarmTime","10:00"))-p.optInt("alarmDays",1)*86400000L;if(due<=System.currentTimeMillis())return;AlarmManager am=(AlarmManager)c.getSystemService(ALARM_SERVICE);Intent in=new Intent(c,AlarmReceiver.class);in.putExtra("id",p.optString("id"));in.putExtra("title","خرید شماره "+p.optString("purchaseNo")+" - "+p.optString("buyer"));PendingIntent pi=PendingIntent.getBroadcast(c,p.optString("id").hashCode(),in,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);if(Build.VERSION.SDK_INT>=31&&am.canScheduleExactAlarms())am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,due,pi);else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,due,pi);}catch(Exception ignored){}}
    void alarmDialog(EditText sub,JSONObject old){if(sub.getText().toString().trim().isEmpty()){Toast.makeText(this,"ابتدا تاریخ فرعی را وارد کنید",Toast.LENGTH_SHORT).show();return;}LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);EditText days=new EditText(this);days.setHint("چند روز قبل؟");days.setInputType(2);EditText time=new EditText(this);time.setHint("ساعت هشدار مثل 10:00");l.addView(days);l.addView(time);new AlertDialog.Builder(this).setTitle("هشدار این سفارش").setView(l).setPositiveButton("فعال",(d,w)->{try{if(old==null){Toast.makeText(this,"ابتدا خرید را ذخیره کنید و سپس هشدار را تنظیم کنید",Toast.LENGTH_SHORT).show();return;}old.put("alarm",true);old.put("alarmDays",Integer.parseInt(days.getText().toString()));old.put("alarmTime",time.getText().toString());old.put("subDue",sub.getText().toString());saveAlarmOnly(old);}catch(Exception e){Toast.makeText(this,"مقادیر هشدار نامعتبر است",Toast.LENGTH_SHORT).show();}}).setNegativeButton("لغو",null).show();}
    void saveAlarmOnly(JSONObject p)throws Exception{JSONArray a=AppData.arr(data,"purchases");for(int i=0;i<a.length();i++)if(a.getJSONObject(i).optString("id").equals(p.optString("id")))a.put(i,p);data.put("purchases",a);AppData.save(this,data);if(Build.VERSION.SDK_INT>=31){AlarmManager am=(AlarmManager)getSystemService(ALARM_SERVICE);if(!am.canScheduleExactAlarms())startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM));}schedule(this,p);Toast.makeText(this,"آلارم این سفارش فعال شد",Toast.LENGTH_SHORT).show();}

    void exportExcel(JSONArray a,String name){try{StringBuilder x=new StringBuilder("<?xml version=\"1.0\"?><Workbook xmlns=\"urn:schemas-microsoft-com:office:spreadsheet\"><Worksheet ss:Name=\"خریدها\" xmlns:ss=\"urn:schemas-microsoft-com:office:spreadsheet\"><Table>");String[] h={"نام خریدار","نهاده","شماره خرید","وزن","فی","قیمت توافقی","نوع پرداخت","مبلغ خرید","تاریخ خرید","مقدار ذرت","مقدار سویا","نام شرکت","تاریخ اصلی سررسید","تاریخ فرعی سررسید"};x.append("<Row>");for(String s:h)x.append("<Cell><Data ss:Type=\"String\">").append(xml(s)).append("</Data></Cell>");x.append("</Row>");String[] k={"buyer","commodity","purchaseNo","weight","fee","agreed","payment","amount","buyDate","corn","soy","company","mainDue","subDue"};for(int i=0;i<a.length();i++){JSONObject p=a.optJSONObject(i);x.append("<Row>");for(String z:k)x.append("<Cell><Data ss:Type=\"String\">").append(xml(p.optString(z,""))).append("</Data></Cell>");x.append("</Row>");}x.append("</Table></Worksheet></Workbook>");File f=new File(getCacheDir(),name+".xls");FileOutputStream o=new FileOutputStream(f);o.write(x.toString().getBytes("UTF-8"));o.close();Intent in=new Intent(Intent.ACTION_SEND);in.setType("application/vnd.ms-excel");Uri u=FileProvider.getUriForFile(this,"com.morghtak.kharidmanager.fileprovider",f);in.putExtra(Intent.EXTRA_STREAM,u);in.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(Intent.createChooser(in,"ارسال / ذخیره فایل Excel"));}catch(Exception e){Toast.makeText(this,"خطا در ساخت Excel",Toast.LENGTH_LONG).show();}}
    String xml(String s){return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");}

    void backup(){
        base("پشتیبان‌گیری و بازیابی");addView(tv("برای امنیت اطلاعات خریدها، هر چند وقت یک‌بار پشتیبان بگیرید.",16));Button save=btn("💾 ساخت فایل پشتیبان");save.setOnClickListener(v->doBackup());addView(save);Button restore=btn("📥 بازیابی از فایل پشتیبان");restore.setOnClickListener(v->{Intent in=new Intent(Intent.ACTION_OPEN_DOCUMENT);in.setType("application/json");in.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(in,91);});addView(restore);Button back=btn("← بازگشت");back.setOnClickListener(v->goBackPage());addView(back);finishScreen("پشتیبان‌گیری و بازیابی");
    }
    void doBackup(){try{File f=new File(getCacheDir(),"kharidmanager_backup.json");FileOutputStream o=new FileOutputStream(f);o.write(data.toString(2).getBytes("UTF-8"));o.close();Intent in=new Intent(Intent.ACTION_SEND);in.setType("application/json");in.putExtra(Intent.EXTRA_STREAM,FileProvider.getUriForFile(this,"com.morghtak.kharidmanager.fileprovider",f));in.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(Intent.createChooser(in,"ذخیره فایل پشتیبان"));}catch(Exception e){Toast.makeText(this,"خطا در پشتیبان‌گیری",Toast.LENGTH_LONG).show();}}
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent intent){super.onActivityResult(requestCode,resultCode,intent);UiManager.handleLogoResult(this,requestCode,resultCode,intent);if(requestCode==91&&resultCode==RESULT_OK&&intent!=null&&intent.getData()!=null){try{InputStream in=getContentResolver().openInputStream(intent.getData());ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] buf=new byte[4096];int n;while((n=in.read(buf))>0)b.write(buf,0,n);in.close();data=new JSONObject(new String(b.toByteArray(),"UTF-8"));AppData.save(this,data);Toast.makeText(this,"بازیابی با موفقیت انجام شد",Toast.LENGTH_LONG).show();goHome();}catch(Exception e){Toast.makeText(this,"فایل پشتیبان معتبر نیست",Toast.LENGTH_LONG).show();}}}
}
