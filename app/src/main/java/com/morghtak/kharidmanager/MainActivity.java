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
    static final String SHAHEDANEH_UNIT = "شاهدانه طیور مارلیک";
    LinearLayout root;
    JSONObject data;
    ArrayList<EditText> inputs = new ArrayList<>();
    Spinner unitSp, commoditySp, paymentSp, paymentDetailSp, companySp, transferSourceSp;
    EditText cardDateInput;
    TextView transferSourceLabel;
    TextView cardDateLabel;
    final ArrayDeque<Runnable> history = new ArrayDeque<>();
    Runnable currentPage;
    int initialPurchaseStatus=0;
    int initialCollectedFilter=0, initialAllocatedFilter=0, initialFundingFilter=0, initialQuotaFilter=0;
    JSONObject formOldPurchase;
    final String[] labels = {"نام واحد","گواهی بهداشتی","تعداد جوجه‌ریزی","سهمیه ذرت (کیلوگرم)","سهمیه سویا (کیلوگرم)","تاریخ جوجه‌ریزی","تاریخ اعتبار","نهاده","شماره خرید","وزن (کیلوگرم)","فی (ریال)","نوع پرداخت","مبلغ خرید (ریال)","تاریخ خرید","مقدار ذرت (کیلوگرم)","مقدار سویا (کیلوگرم)","مقدار ریز مغذی و افت (کیلوگرم)","نام شرکت","تاریخ سررسید"};
    final String[] keys = {"unit","healthCertificate","chickCount","cornQuota","soyQuota","placementDate","quotaExpiry","commodity","purchaseNo","weight","fee","payment","amount","buyDate","corn","soy","micronutrient","company","mainDue"};

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        data=AppData.root(this);
        normalizeData();
        rescheduleAll();
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},22);
        String id=getIntent().getStringExtra("openPurchaseId");
        if(id!=null){ JSONObject p=findPurchase(id); if(p!=null) details(p); else home(); }
        else openPage(()->home(),false);
    }

    void normalizeData(){
        try{
            JSONArray a=AppData.arr(data,"purchases");
            boolean changed=false;
            for(int i=0;i<a.length();i++){
                JSONObject p=a.optJSONObject(i);
                if(p!=null&&p.has("subDue")){p.remove("subDue");changed=true;}
            }
            if(!data.has("sourceAccounts")){data.put("sourceAccounts",new JSONArray());changed=true;}
            if(!data.has("destinationAccounts")){data.put("destinationAccounts",new JSONArray());changed=true;}
            if(!data.has("quotaTransfers")){data.put("quotaTransfers",new JSONArray());changed=true;}
            JSONArray oldUnits=data.has("units")?data.optJSONArray("units"):null;
            if(oldUnits==null){oldUnits=new JSONArray();JSONArray oldBuyers=AppData.arr(data,"buyers");for(int i=0;i<oldBuyers.length();i++)oldUnits.put(oldBuyers.optString(i));data.put("units",oldUnits);changed=true;}
            boolean hasSpecial=false;for(int i=0;i<oldUnits.length();i++)if(SHAHEDANEH_UNIT.equals(oldUnits.optString(i))){hasSpecial=true;break;}if(!hasSpecial){oldUnits.put(SHAHEDANEH_UNIT);changed=true;}
            for(int i=0;i<a.length();i++){JSONObject p=a.optJSONObject(i);if(p==null)continue;if(!p.has("unit")&&p.has("buyer")){p.put("unit",p.optString("buyer"));changed=true;}if(!p.has("healthCertificate")){p.put("healthCertificate","");changed=true;}if(!p.has("chickCount")){p.put("chickCount","");changed=true;}if(!p.has("cornQuota")){p.put("cornQuota","");changed=true;}if(!p.has("soyQuota")){p.put("soyQuota","");changed=true;}if(!p.has("placementDate")){p.put("placementDate","");changed=true;}if(!p.has("quotaExpiry")){p.put("quotaExpiry","");changed=true;}if(!p.has("cardRegistrationDate")){p.put("cardRegistrationDate","");changed=true;}if(!p.has("paymentDetail")){p.put("paymentDetail","");changed=true;}if(!p.has("allocatedDate")){p.put("allocatedDate","");changed=true;}}
            if(changed)AppData.save(this,data);
        }catch(Exception ignored){}
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
        for(int i=0;i<a.length();i++){JSONObject p=a.optJSONObject(i);if(p!=null&&!p.optBoolean("collected",false))open++;}
        s.setText("تعداد خریدها: "+a.length()+"\nخریدهای در انتظار وصول: "+open);
        Button b=btn("➕ ثبت خرید جدید"); b.setOnClickListener(v->openPage(()->form(null))); add(b);
        b=btn("🛒 خریدهای ثبت‌شده"); b.setOnClickListener(v->openPage(()->listPurchases(null))); add(b);
        b=btn("🏠 واحدها"); b.setOnClickListener(v->openPage(this::buyersPage)); add(b);
        b=btn("💳 تأمین موجودی"); b.setOnClickListener(v->openPage(this::incompleteFundingPage)); add(b);
        b=btn("💰 وصول"); b.setOnClickListener(v->openPage(this::incompleteCollectionPage)); add(b);
        b=btn("📦 تخصیص"); b.setOnClickListener(v->openPage(this::incompleteAllocationPage)); add(b);
        b=btn("🔔 سررسیدها"); b.setOnClickListener(v->openPage(this::deadlines)); add(b);
        b=btn("📂 خرید بر اساس گواهی بهداشتی"); b.setOnClickListener(v->openPage(this::certificateHome)); add(b);
        b=btn("🔄 انتقال مانده سهمیه به شاهدانه"); b.setOnClickListener(v->openPage(this::quotaTransferPlaceholder)); add(b);
        b=btn("🏢 شرکت‌ها"); b.setOnClickListener(v->openPage(()->manage("companies","شرکت‌ها"))); add(b);
        b=btn("🌾 نهاده‌ها"); b.setOnClickListener(v->openPage(()->manage("commodities","نهاده‌ها"))); add(b);
        b=btn("🏦 حساب‌ها"); b.setOnClickListener(v->openPage(this::accountsPage)); add(b);
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
        final PersianCalendarDialog dlg=new PersianCalendarDialog(this,target);
        dlg.show();
    }

    void form(JSONObject old){
        formOldPurchase=old;
        base(old==null?"ثبت خرید جدید":"ویرایش خرید");inputs.clear();final TextView[] quotaStatusHolder={null}; final TextView[] formLabels=new TextView[labels.length];
        for(int i=0;i<labels.length;i++){
            formLabels[i]=tv((i+1)+". "+labels[i],14); add(formLabels[i]);
            if(i==0){unitSp=spinnerWithBlank(AppData.arr(data,"units"),"انتخاب واحد");add(unitSp);hidden(); transferSourceLabel=tv("منبع انتقال سهمیه",14); transferSourceLabel.setVisibility(View.GONE); add(transferSourceLabel); transferSourceSp=spinnerWithBlankArray(new String[]{""}); transferSourceSp.setVisibility(View.GONE); add(transferSourceSp);}
            else if(i==7){commoditySp=spinnerWithBlank(AppData.arr(data,"commodities"),"انتخاب نهاده");add(commoditySp);hidden();}
            else if(i==11){
                paymentSp=spinnerWithBlank(new String[]{"توافقی","نقد"},"انتخاب نوع پرداخت");add(paymentSp);hidden();
                paymentDetailSp=spinnerWithBlank(new String[]{""},"انتخاب جزئیات پرداخت");paymentDetailSp.setVisibility(View.GONE);add(paymentDetailSp);
            }
            else if(i==17){companySp=spinnerWithBlank(AppData.arr(data,"companies"),"انتخاب شرکت");add(companySp);hidden();}
            else{
                EditText e=input("");
                if(i==5)e.setOnClickListener(v->{pickDate(e);updateQuotaFields();});
                if(i==6){
                    e.setFocusable(false);e.setClickable(false);
                    cardDateLabel=tv("تاریخ ثبت کارت",14); add(cardDateLabel);
                    cardDateInput=new EditText(this);
                    cardDateInput.setSingleLine(true);
                    cardDateInput.setTextSize(UiManager.fieldSize(this));
                    cardDateInput.setTextColor(UiManager.text(this));
                    cardDateInput.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
                    cardDateInput.setPadding(UiManager.dp(this,14),UiManager.dp(this,8),UiManager.dp(this,14),UiManager.dp(this,8));
                    cardDateInput.setBackground(fieldBg(true));
                    cardDateInput.setOnClickListener(v->pickDate(cardDateInput));
                    add(cardDateInput);
                }
                if(i==13||i==18)e.setOnClickListener(v->pickDate(e));
                if(i==2||i==3||i==4||i==9||i==10||i==12||i==14||i==15||i==16){e.setInputType(2);addGrouping(e);}
                if(i==3||i==4||i==6||i==12||i==14||i==15||i==16)e.setFocusable(false);
                if(i==16){TextView qst=tv("ابتدا اطلاعات گواهی را وارد کنید.",14);quotaStatusHolder[0]=qst;add(qst);}
            }
        }
        inputs.get(12).setFocusable(false);inputs.get(12).setClickable(false);
        inputs.get(3).setFocusable(false);inputs.get(4).setFocusable(false);inputs.get(6).setFocusable(false);

        TextWatcher calcW=new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int b,int c){ }public void afterTextChanged(Editable e){calcPurchaseComposition();calcAmount();validateForm(null);}};
        inputs.get(2).addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int b,int c){updateQuotaFields();validateForm(null);}public void afterTextChanged(Editable e){}});
        inputs.get(5).addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int b,int c){updateQuotaFields();validateForm(null);}public void afterTextChanged(Editable e){}});
        inputs.get(9).addTextChangedListener(calcW);inputs.get(10).addTextChangedListener(calcW);
        if(commoditySp!=null)commoditySp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> p){}public void onItemSelected(AdapterView<?> p,View v,int pos,long id){calcPurchaseComposition();validateForm(null);}});
        if(old!=null)fill(old);else {inputs.get(5).setText(PersianDate.today());inputs.get(13).setText(PersianDate.today());inputs.get(18).setText(PersianDate.today());}

        TextView quotaStatus=quotaStatusHolder[0];
        Runnable quotaRender=()->{if(!isShahedaneh(unitSp==null?"":String.valueOf(unitSp.getSelectedItem())))updateQuotaFields();quotaStatus.setText(quotaStatusText(old));};
        TextWatcher qw=new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int b,int c){}public void onTextChanged(CharSequence s,int a,int b,int c){quotaRender.run();}public void afterTextChanged(Editable e){}};
        inputs.get(1).addTextChangedListener(qw);inputs.get(2).addTextChangedListener(qw);inputs.get(5).addTextChangedListener(qw);
        inputs.get(1).addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int b,int c){}public void onTextChanged(CharSequence s,int a,int b,int c){updateTransferSourceOptions();refreshShahedanehForm(formLabels);}public void afterTextChanged(Editable e){}});
        if(unitSp!=null)unitSp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> p){}public void onItemSelected(AdapterView<?> p,View v,int pos,long id){updateTransferSourceOptions();refreshShahedanehForm(formLabels);quotaRender.run();validateForm(null);}});
        if(transferSourceSp!=null)transferSourceSp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> p){}public void onItemSelected(AdapterView<?> p,View v,int pos,long id){applyTransferSourceFields();quotaRender.run();validateForm(null);}});
        updateTransferSourceOptions();
        refreshShahedanehForm(formLabels);
        quotaRender.run();
        TextWatcher qrw=new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int b,int c){}public void onTextChanged(CharSequence s,int a,int b,int c){quotaRender.run();}public void afterTextChanged(Editable e){}};
        inputs.get(9).addTextChangedListener(qrw);inputs.get(14).addTextChangedListener(qrw);inputs.get(15).addTextChangedListener(qrw);

        add(tv("هشدار بر اساس تاریخ سررسید",16));
        LinearLayout al=new LinearLayout(this);al.setOrientation(LinearLayout.VERTICAL);add(al);
        EditText days=new EditText(this);days.setHint("چند روز قبل از سررسید؟");days.setInputType(2);al.addView(days);
        EditText time=new EditText(this);time.setHint("انتخاب ساعت هشدار");time.setSingleLine(true);time.setFocusable(false);time.setClickable(true);time.setInputType(0);time.setOnClickListener(v->pickAlarmTime(time));al.addView(time);
        Spinner repeat=spinner(new String[]{"فقط یک بار","هر روز تا سررسید"});al.addView(repeat);
        TextView preview=tv("هشدار تنظیم نشده است",13);al.addView(preview);
        if(old!=null){days.setText(old.optBoolean("alarm",false)?""+old.optInt("alarmDays",1):"");time.setText(old.optBoolean("alarm",false)?old.optString("alarmTime",""):"");repeat.setSelection(old.optBoolean("alarmRepeat",false)?1:0);}
        TextWatcher aw=new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int b,int c){}public void onTextChanged(CharSequence s,int a,int b,int c){updateAlarmStatus(preview,days.getText().toString(),time.getText().toString(),inputs.get(18).getText().toString());}public void afterTextChanged(Editable e){}};
        days.addTextChangedListener(aw);time.addTextChangedListener(aw);inputs.get(18).addTextChangedListener(aw);
        Button clear=btn("🔕 حذف هشدار");clear.setOnClickListener(v->{days.setText("");time.setText("");updateAlarmStatus(preview,"","",inputs.get(18).getText().toString());if(old!=null){try{old.remove("alarm");old.remove("alarmRepeat");}catch(Exception ignored){}cancelAlarm(this,old);}});add(clear);
        Button save=btn("✓ ذخیره خرید و هشدار");save.setOnClickListener(v->savePurchase(old,days.getText().toString(),time.getText().toString(),repeat.getSelectedItemPosition()==1,save));add(save);
        Button back=btn("← بازگشت");back.setOnClickListener(v->back());add(back);
        finishScreen(old==null?"ثبت خرید جدید":"ویرایش خرید");
        updateAlarmStatus(preview,days.getText().toString(),time.getText().toString(),inputs.get(18).getText().toString());setupValidationListeners(save);validateForm(save);
    }

    boolean isShahedaneh(String unit){return SHAHEDANEH_UNIT.equals(unit==null?"":unit.trim());}
    void refreshShahedanehForm(TextView[] formLabels){
        if(formLabels==null||formLabels.length<7||unitSp==null||inputs.size()<7)return;
        boolean special=isShahedaneh(String.valueOf(unitSp.getSelectedItem()==null?"":unitSp.getSelectedItem()).trim());
        formLabels[2].setVisibility(special?View.GONE:View.VISIBLE); inputs.get(2).setVisibility(special?View.GONE:View.VISIBLE);
        formLabels[5].setText(special?"6. تاریخ انتقال":"6. تاریخ جوجه‌ریزی");
        inputs.get(5).setFocusable(!special); inputs.get(5).setClickable(!special);
        if(special){ inputs.get(5).setFocusable(false); inputs.get(5).setClickable(false); }
        formLabels[6].setVisibility(View.VISIBLE);
        formLabels[6].setText("7. تاریخ اعتبار");
        if(cardDateLabel!=null)cardDateLabel.setVisibility(special?View.GONE:View.VISIBLE);
        if(cardDateInput!=null)cardDateInput.setVisibility(special?View.GONE:View.VISIBLE);
        if(special){
            applyTransferSourceFields();
            inputs.get(6).setFocusable(false); inputs.get(6).setClickable(false);
        }
    }

    void applyTransferSourceFields(){
        if(transferSourceSp==null||inputs.size()<7||!isShahedaneh(unitSp==null?"":String.valueOf(unitSp.getSelectedItem())))return;
        String src=selectedTransferSource(),cert=inputs.get(1).getText().toString().trim(); if(src.isEmpty()||cert.isEmpty())return;
        JSONObject cb=certificateBase(src+"|"+cert,null); if(cb==null)return;
        double corn=transferRemaining(src,cert,"corn"), soy=transferRemaining(src,cert,"soy");
        inputs.get(3).setText(fmtDecimal(corn)); inputs.get(4).setText(fmtDecimal(soy));
        String td=firstTransferDate(src,cert);
        inputs.get(5).setText(td); inputs.get(6).setText(addPersianDays(td,60));
        refreshShahedanehForm(null);
    }

    String firstTransferDate(String source,String cert){
        String best=""; JSONArray tr=quotaTransfers();
        for(int i=0;i<tr.length();i++){JSONObject x=tr.optJSONObject(i);if(x==null)continue;if(source.equals(x.optString("unit").trim())&&cert.equals(x.optString("healthCertificate").trim())){String com=x.optString("commodity");if(transferRemaining(source,cert,com)<=0.0001)continue;String d=x.optString("date").trim();if(!d.isEmpty()&&(best.isEmpty()||d.compareTo(best)<0))best=d;}}
        return best.isEmpty()?PersianDate.today():best;
    }

    void updateTransferSourceOptions(){
        if(transferSourceSp==null||unitSp==null||inputs.size()<2)return;
        String unit=String.valueOf(unitSp.getSelectedItem()==null?"":unitSp.getSelectedItem()).trim();
        boolean show=isShahedaneh(unit);
        if(transferSourceLabel!=null)transferSourceLabel.setVisibility(show?View.VISIBLE:View.GONE);
        transferSourceSp.setVisibility(show?View.VISIBLE:View.GONE);
        if(!show)return;
        String cert=inputs.get(1).getText().toString().trim();
        String current=transferSourceSp.getSelectedItem()==null?"":String.valueOf(transferSourceSp.getSelectedItem());
        LinkedHashSet<String> sources=new LinkedHashSet<>();
        if(!cert.isEmpty()){
            JSONArray tr=quotaTransfers();
            for(int i=0;i<tr.length();i++){
                JSONObject x=tr.optJSONObject(i);if(x==null)continue;
                if(cert.equals(x.optString("healthCertificate").trim())){
                    String src=x.optString("unit").trim();
                    String com=x.optString("commodity");
                    if(!src.isEmpty()&&transferRemaining(src,cert,com)>0.0001)sources.add(src);
                }
            }
        }
        ArrayList<String> opts=new ArrayList<>();opts.add("");opts.addAll(sources);
        ArrayAdapter<String> ad=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,opts);
        transferSourceSp.setAdapter(ad);
        if(!current.isEmpty()&&sources.contains(current))setSpinner(transferSourceSp,current);
        else if(sources.size()==1)transferSourceSp.setSelection(1);
        applyTransferSourceFields();
    }
    String selectedTransferSource(){return transferSourceSp==null||transferSourceSp.getSelectedItem()==null?"":String.valueOf(transferSourceSp.getSelectedItem()).trim();}
    double transferredAvailable(String source,String cert,String commodity){return transferRemaining(source,cert,commodity);}
    double shahedanehUsed(String source,String cert,String commodity){
        double s=0;JSONArray a=AppData.arr(data,"purchases");
        for(int i=0;i<a.length();i++){JSONObject p=a.optJSONObject(i);if(p==null)continue;if(!isShahedaneh(p.optString("unit",p.optString("buyer"))))continue;if(!source.equals(p.optString("transferSourceUnit")))continue;if(!cert.equals(p.optString("healthCertificate")))continue;s+=toDouble(p.optString(commodity));}
        return s;
    }
    boolean shahedanehTransferValid(JSONObject old,String source,String cert){
        if(!isShahedaneh(String.valueOf(unitSp.getSelectedItem())))return true;
        if(source.isEmpty())return false;
        double c=toDouble(inputs.get(14).getText().toString()),so=toDouble(inputs.get(15).getText().toString());
        return c<=transferredAvailable(source,cert,"corn")+0.001 && so<=transferredAvailable(source,cert,"soy")+0.001;
    }

    void updatePaymentDetailOptions(){
        if(paymentSp==null||paymentDetailSp==null)return;
        int pos=paymentSp.getSelectedItemPosition();
        String current=paymentDetailSp.getSelectedItem()==null?"":String.valueOf(paymentDetailSp.getSelectedItem());
        String[] opts=pos==1?new String[]{"نسیه","چک","پالیز","برات"}:pos==2?new String[]{"از بانک","واریز"}:new String[]{""};
        ArrayList<String> x=new ArrayList<>();x.add("");Collections.addAll(x,opts);
        ArrayAdapter<String> ad=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,x);
        paymentDetailSp.setAdapter(ad);
        paymentDetailSp.setVisibility(pos>0?View.VISIBLE:View.GONE);
        if(!current.isEmpty())setSpinner(paymentDetailSp,current);
    }

    Spinner spinnerWithBlank(JSONArray a,String placeholder){ArrayList<String>x=new ArrayList<>();x.add("");for(int i=0;i<a.length();i++)x.add(a.optString(i));return spinnerWithBlankArray(x.toArray(new String[0]));}
    Spinner spinnerWithBlank(String[] a,String placeholder){ArrayList<String>x=new ArrayList<>();x.add("");Collections.addAll(x,a);return spinnerWithBlankArray(x.toArray(new String[0]));}
    Spinner spinnerWithBlankArray(String[] x){Spinner s=new Spinner(this);ArrayAdapter<String> ad=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,x){@Override public View getView(int position,View convertView,android.view.ViewGroup parent){TextView v=(TextView)super.getView(position,convertView,parent);v.setText(position==0?"":String.valueOf(getItem(position)));return v;}};s.setAdapter(ad);return s;}

    void updateQuotaFields(){
        if(inputs.size()<7)return;
        if(isShahedaneh(unitSp==null?"":String.valueOf(unitSp.getSelectedItem())))return;
        String d=AppData.digits(inputs.get(2).getText().toString());
        if(d.isEmpty()){inputs.get(3).setText("");inputs.get(4).setText("");inputs.get(6).setText("");return;}
        try{double chicks=Double.parseDouble(d);inputs.get(3).setText(fmtDecimal(chicks*2.650));inputs.get(4).setText(fmtDecimal(chicks*1.310));String pd=inputs.get(5).getText().toString().trim();inputs.get(6).setText(addPersianDays(pd,60));}catch(Exception ignored){inputs.get(3).setText("");inputs.get(4).setText("");inputs.get(6).setText("");}
    }

    void calcPurchaseComposition(){
        if(inputs.size()<19)return;
        String ws=AppData.digits(inputs.get(9).getText().toString()).replace(",","");if(ws.isEmpty()){setCalcInput(14,0);setCalcInput(15,0);setCalcInput(16,0);return;}
        try{
            double w=Double.parseDouble(ws),c=0,so=0,mi=0;String commodity=commoditySp==null?"":String.valueOf(commoditySp.getSelectedItem());
            if("ذرت".equals(commodity)){c=w;so=0;mi=0;}
            else if("سویا".equals(commodity)){c=0;so=w;mi=0;}
            else if(commodity.contains("پیش آغازین")){c=w*560.0/1000.0;so=w*380.0/1000.0;mi=w-c-so;}
            else if(commodity.contains("آغازین")){c=w*580.0/1000.0;so=w*350.0/1000.0;mi=w-c-so;}
            else if(commodity.contains("رشد")){c=w*610.0/1000.0;so=w*330.0/1000.0;mi=w-c-so;}
            else if(commodity.contains("پایانی 1")||commodity.contains("پایانی ۱")){c=w*640.0/1000.0;so=w*300.0/1000.0;mi=w-c-so;}
            else if(commodity.contains("پایانی 2")||commodity.contains("پایانی ۲")){c=w*650.0/1000.0;so=w*290.0/1000.0;mi=w-c-so;}
            else {setCalcInput(14,0);setCalcInput(15,0);setCalcInput(16,0);return;}
            c=roundCompositionValue(c);so=roundCompositionValue(so);mi=w-c-so;
            setCalcInput(14,c);setCalcInput(15,so);setCalcInput(16,mi);
        }catch(Exception ignored){}
    }
    double roundCompositionValue(double v){
        if(!Double.isFinite(v))return 0;
        double whole=Math.floor(v),frac=v-whole;
        if(frac>0.5)return whole+1;
        if(frac<0.5)return whole;
        return whole+1;
    }
    void setCalcInput(int idx,double value){String v=fmtDecimal(value);if(!v.equals(inputs.get(idx).getText().toString()))inputs.get(idx).setText(v);}
    String fmtDecimal(double v){if(Math.abs(v-Math.rint(v))<0.0000001)return AppData.fmt(String.valueOf((long)Math.rint(v)));return String.format(Locale.US,"%,.3f",v).replace(".000","");}
    String addPersianDays(String date,int days){try{String[] a=date.split("/");if(a.length!=3)return "";int y=Integer.parseInt(AppData.digits(a[0])),m=Integer.parseInt(AppData.digits(a[1])),d=Integer.parseInt(AppData.digits(a[2]));for(int i=0;i<days;i++){d++;if(d>persianMonthDays(y,m)){d=1;m++;if(m>12){m=1;y++;}}}return String.format(Locale.US,"%04d/%02d/%02d",y,m,d);}catch(Exception e){return "";}}
    int persianMonthDays(int y,int m){if(m<=6)return 31;if(m<=11)return 30;long a=PersianDate.millis(String.format(Locale.US,"%04d/12/01",y),"00:00");long z=PersianDate.millis(String.format(Locale.US,"%04d/01/01",y+1),"00:00");return z-a>365L*86400000L?30:29;}
    String certificateKey(){String u=unitSp==null?"":String.valueOf(unitSp.getSelectedItem());String c=inputs.size()>1?inputs.get(1).getText().toString().trim():"";return u+"|"+c;}
    JSONObject certificateBase(String key,JSONObject exclude){JSONArray a=AppData.arr(data,"purchases");for(int i=0;i<a.length();i++){JSONObject p=a.optJSONObject(i);if(p==null||p==exclude)continue;String k=p.optString("unit",p.optString("buyer"))+"|"+p.optString("healthCertificate");if(k.equals(key)&&!p.optString("healthCertificate").isEmpty())return p;}return null;}
    double quotaOriginal(JSONObject p,String key){return p==null?0:toDouble(p.optString(key,"0"));}
    double usedQuota(String key,String unit,String cert,JSONObject exclude){
        double s=0;JSONArray a=AppData.arr(data,"purchases");
        String consumeKey="cornQuota".equals(key)?"corn":"soyQuota".equals(key)?"soy":key;
        for(int i=0;i<a.length();i++){JSONObject p=a.optJSONObject(i);if(p==null||p==exclude)continue;
            if(!unit.equals(p.optString("unit",p.optString("buyer")))||!cert.equals(p.optString("healthCertificate")))continue;
            s+=toDouble(p.optString(consumeKey));
        }
        return s;
    }
    double usedCorn(String unit,String cert,JSONObject exclude){double s=0;JSONArray a=AppData.arr(data,"purchases");for(int i=0;i<a.length();i++){JSONObject p=a.optJSONObject(i);if(p==null||p==exclude)continue;if(unit.equals(p.optString("unit",p.optString("buyer")))&&cert.equals(p.optString("healthCertificate")))s+=toDouble(p.optString("corn"));}return s;}
    double usedSoy(String unit,String cert,JSONObject exclude){double s=0;JSONArray a=AppData.arr(data,"purchases");for(int i=0;i<a.length();i++){JSONObject p=a.optJSONObject(i);if(p==null||p==exclude)continue;if(unit.equals(p.optString("unit",p.optString("buyer")))&&cert.equals(p.optString("healthCertificate")))s+=toDouble(p.optString("soy"));}return s;}
    double toDouble(String s){try{return Double.parseDouble(AppData.digits(s).replace(",",""));}catch(Exception e){try{return Double.parseDouble(s.replace(",",""));}catch(Exception x){return 0;}}}
    String quotaStatusText(JSONObject old){
        String unit=unitSp==null?"":String.valueOf(unitSp.getSelectedItem()),cert=inputs.size()>1?inputs.get(1).getText().toString().trim():"";
        if(unit.trim().isEmpty()||cert.isEmpty())return "ابتدا واحد و شماره گواهی بهداشتی را مشخص کنید.";
        if(isShahedaneh(unit)){
            String src=selectedTransferSource(); if(src.isEmpty())return "برای شاهدانه، ابتدا منبع انتقال سهمیه را انتخاب کنید.";
            return "منبع سهمیه: "+src+"\nذرت منتقل‌شده و قابل مصرف: "+fmtDecimal(transferRemaining(src,cert,"corn"))+" کیلوگرم\nسویا منتقل‌شده و قابل مصرف: "+fmtDecimal(transferRemaining(src,cert,"soy"))+" کیلوگرم";
        }
        JSONObject base=certificateBase(unit+"|"+cert,old);double cq=base==null?toDouble(inputs.get(3).getText().toString()):quotaOriginal(base,"cornQuota"),sq=base==null?toDouble(inputs.get(4).getText().toString()):quotaOriginal(base,"soyQuota");double uc=usedCorn(unit,cert,old),us=usedSoy(unit,cert,old),cc=toDouble(inputs.get(14).getText().toString()),cs=toDouble(inputs.get(15).getText().toString());double rc=Math.max(0,cq-uc-cc-transferAmount(unit,cert,"corn")),rs=Math.max(0,sq-us-cs-transferAmount(unit,cert,"soy"));boolean full=rc<=0.0001&&rs<=0.0001;return "سهمیه اولیه ذرت: "+fmtDecimal(cq)+" کیلوگرم\nمانده سهمیه ذرت پس از این خرید: "+fmtDecimal(rc)+" کیلوگرم\nسهمیه اولیه سویا: "+fmtDecimal(sq)+" کیلوگرم\nمانده سهمیه سویا پس از این خرید: "+fmtDecimal(rs)+" کیلوگرم\n"+(full?"✅ تکمیل خرید سهمیه":"⏳ سهمیه هنوز تکمیل نشده است");
    }

    void setupValidationListeners(Button save){
        AdapterView.OnItemSelectedListener l=new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> p){}public void onItemSelected(AdapterView<?> p,View v,int pos,long id){if(p==paymentSp)updatePaymentDetailOptions();calcPurchaseComposition();validateForm(save);}};
        if(unitSp!=null)unitSp.setOnItemSelectedListener(l);if(commoditySp!=null)commoditySp.setOnItemSelectedListener(l);if(paymentSp!=null)paymentSp.setOnItemSelectedListener(l);if(companySp!=null)companySp.setOnItemSelectedListener(l);
        for(int i:new int[]{1,2,5,8,9,10,13,18})inputs.get(i).addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int b,int c){validateForm(save);}public void afterTextChanged(Editable e){}});
    }
    boolean validateForm(Button save){
        boolean ok=true;
        ok &= markSpinner(unitSp,unitSp!=null&&unitSp.getSelectedItemPosition()>0);ok &= markSpinner(commoditySp,commoditySp!=null&&commoditySp.getSelectedItemPosition()>0);ok &= markSpinner(paymentSp,paymentSp!=null&&paymentSp.getSelectedItemPosition()>0);ok &= markSpinner(companySp,companySp!=null&&companySp.getSelectedItemPosition()>0);
        if(paymentDetailSp!=null&&paymentSp!=null&&paymentSp.getSelectedItemPosition()>0)ok &= markSpinner(paymentDetailSp,paymentDetailSp.getSelectedItemPosition()>0);
        int[] req=isShahedaneh(unitSp==null?"":String.valueOf(unitSp.getSelectedItem()))?new int[]{1,5,8,9,10,13,18}:new int[]{1,2,5,8,9,10,13,18};for(int i:req)ok &= markField(inputs.get(i),!inputs.get(i).getText().toString().trim().isEmpty());
        String cert=inputs.get(1).getText().toString().trim(),unit=unitSp==null?"":String.valueOf(unitSp.getSelectedItem());
        if(cert.isEmpty()||unit.trim().isEmpty())ok=false;
        String ws=AppData.digits(inputs.get(9).getText().toString()),cs=AppData.digits(inputs.get(14).getText().toString()),ss=AppData.digits(inputs.get(15).getText().toString()),ms=AppData.digits(inputs.get(16).getText().toString());
        boolean nums=!ws.isEmpty()&&!cs.isEmpty()&&!ss.isEmpty()&&!ms.isEmpty();
        if(nums){try{double w=Double.parseDouble(ws),c=Double.parseDouble(cs),so=Double.parseDouble(ss),mi=Double.parseDouble(ms);boolean match=Math.abs((c+so+mi)-w)<0.001;markField(inputs.get(9),match);markField(inputs.get(14),match);markField(inputs.get(15),match);markField(inputs.get(16),match);ok&=match;}catch(Exception e){ok=false;}}
        else ok=false;
        String cert2=inputs.get(1).getText().toString().trim(); String unit2=unitSp==null?"":String.valueOf(unitSp.getSelectedItem()).trim();
        if(isShahedaneh(unit2)){String src=selectedTransferSource(); if(src.isEmpty())ok=false; if(!shahedanehTransferValid(formOldPurchase,src,cert2))ok=false;}
        else if(!checkQuotaLimits(formOldPurchase))ok=false;
        if(save!=null){save.setEnabled(ok);save.setAlpha(ok?1f:0.5f);}return ok;
    }
    boolean markField(EditText e,boolean valid){e.setBackground(fieldBg(valid));return valid;}
    boolean markSpinner(Spinner s,boolean valid){if(s!=null)s.setBackground(fieldBg(valid));return valid;}
    GradientDrawable fieldBg(boolean valid){GradientDrawable g=new GradientDrawable();g.setColor(UiManager.card(this));g.setCornerRadius(UiManager.dp(this,UiManager.radius(this)));g.setStroke(UiManager.dp(this,1),valid?UiManager.secondary(this):Color.rgb(210,50,50));return g;}
    boolean checkQuotaLimits(JSONObject old){
        String unit=unitSp==null?"":String.valueOf(unitSp.getSelectedItem()),cert=inputs.get(1).getText().toString().trim();
        if(unit.trim().isEmpty()||cert.isEmpty())return false;
        if(isShahedaneh(unit))return shahedanehTransferValid(old,selectedTransferSource(),cert);
        JSONObject base=certificateBase(unit+"|"+cert,old);double cq=base==null?toDouble(inputs.get(3).getText().toString()):quotaOriginal(base,"cornQuota"),sq=base==null?toDouble(inputs.get(4).getText().toString()):quotaOriginal(base,"soyQuota");if(cq<=0||sq<=0)return false;double uc=usedCorn(unit,cert,old),us=usedSoy(unit,cert,old),c=toDouble(inputs.get(14).getText().toString()),so=toDouble(inputs.get(15).getText().toString());return uc+c<=cq-transferAmount(unit,cert,"corn")+0.001&&us+so<=sq-transferAmount(unit,cert,"soy")+0.001;
    }
    void pickAlarmTime(EditText target){String value=target.getText().toString().trim();int hour=10,minute=0;try{String[] parts=value.split(":");if(parts.length==2){hour=Integer.parseInt(AppData.digits(parts[0]));minute=Integer.parseInt(AppData.digits(parts[1]));if(hour<0||hour>23)hour=10;if(minute<0||minute>59)minute=0;}}catch(Exception ignored){}TimePickerDialog dlg=new TimePickerDialog(this,(view,h,m)->target.setText(String.format(Locale.US,"%02d:%02d",h,m)),hour,minute,true);dlg.setTitle("انتخاب ساعت هشدار");dlg.show();}
    void updateAlarmStatus(TextView v,String days,String time,String due){String d=days.trim(),t=time.trim();if(d.isEmpty()&&t.isEmpty()){v.setText("هشدار تنظیم نشده است");return;}if(!d.matches("\\d+")||!t.matches("([01]\\d|2[0-3]):[0-5]\\d")||due.trim().isEmpty()){v.setText("⚠ تنظیم هشدار کامل نیست");return;}v.setText("🔔 هشدار فعال: "+due+" ، "+d+" روز قبل، ساعت "+t);}
    void calcAmount(){try{double w=toDouble(inputs.get(9).getText().toString()),f=toDouble(inputs.get(10).getText().toString());if(w<=0||f<=0){inputs.get(12).setText("");return;}double total=w*f;if(!Double.isFinite(total)||total>Long.MAX_VALUE){inputs.get(12).setText("");return;}inputs.get(12).setText(AppData.fmt(String.valueOf(Math.round(total))));}catch(Exception ignored){inputs.get(12).setText("");}}
    void fill(JSONObject p){
        for(int i=0;i<keys.length;i++){if(i==0||i==7||i==11||i==17)continue;inputs.get(i).setText(p.optString(keys[i],""));}
        setSpinner(unitSp,p.optString("unit",p.optString("buyer")));
        updateTransferSourceOptions(); if(isShahedaneh(p.optString("unit",p.optString("buyer")))){String src=p.optString("transferSourceUnit",""); if(!src.isEmpty())setSpinner(transferSourceSp,src);}
        setSpinner(commoditySp,p.optString("commodity"));
        String pay=p.optString("payment",""),detail=p.optString("paymentDetail","");
        if(pay.contains(" - ")){String[] pp=pay.split(" - ",2);pay=pp[0].trim();if(detail.isEmpty())detail=pp[1].trim();}
        if("نسیه".equals(pay)){pay="توافقی";if(detail.isEmpty())detail="نسیه";}
        setSpinner(paymentSp,pay);updatePaymentDetailOptions();setSpinner(paymentDetailSp,detail);
        setSpinner(companySp,p.optString("company"));
        if(cardDateInput!=null)cardDateInput.setText(p.optString("cardRegistrationDate",p.optString("cardDate","")));
        updateQuotaFields();calcPurchaseComposition();calcAmount();
    }
    void savePurchase(JSONObject old,String ds,String tm,boolean repeat,Button saveButton){
        if(!validateForm(saveButton)){Toast.makeText(this,"اطلاعات ناقص است، ترکیب خرید یا سهمیه مجاز نیست",Toast.LENGTH_LONG).show();return;}
        try{
            calcPurchaseComposition();String unit=String.valueOf(unitSp.getSelectedItem()).trim(),cert=inputs.get(1).getText().toString().trim();String transferSource=selectedTransferSource();JSONObject base=isShahedaneh(unit)?certificateBase(transferSource+"|"+cert,null):certificateBase(unit+"|"+cert,old);
            if(isShahedaneh(unit)){if(transferSource.isEmpty()||!shahedanehTransferValid(old,transferSource,cert)){Toast.makeText(this,"برای این خرید، منبع و مانده سهمیه منتقل‌شده به شاهدانه کافی نیست.",Toast.LENGTH_LONG).show();return;} inputs.get(3).setText(fmtDecimal(transferRemaining(transferSource,cert,"corn"))); inputs.get(4).setText(fmtDecimal(transferRemaining(transferSource,cert,"soy"))); String td=firstTransferDate(transferSource,cert); inputs.get(5).setText(td); inputs.get(6).setText(addPersianDays(td,60));}
            else if(base!=null){double cq=quotaOriginal(base,"cornQuota"),sq=quotaOriginal(base,"soyQuota");String oldChick=base.optString("chickCount"),oldDate=base.optString("placementDate");if(Math.abs(cq-toDouble(inputs.get(3).getText().toString()))>0.001||Math.abs(sq-toDouble(inputs.get(4).getText().toString()))>0.001||(!oldChick.isEmpty()&&!oldChick.equals(inputs.get(2).getText().toString()))||(!oldDate.isEmpty()&&!oldDate.equals(inputs.get(5).getText().toString()))){Toast.makeText(this,"این گواهی قبلاً ثبت شده است؛ سهمیه، تعداد جوجه‌ریزی و تاریخ جوجه‌ریزی باید همان اطلاعات اولیه گواهی باشد.",Toast.LENGTH_LONG).show();return;}}
            if(!checkQuotaLimits(old)){Toast.makeText(this,"مقدار ذرت یا سویا از سهمیه این گواهی بیشتر می‌شود.",Toast.LENGTH_LONG).show();return;}
            JSONObject r=new JSONObject();for(int i=0;i<keys.length;i++){String v=inputs.get(i).getText().toString();if(i==0)v=unit;if(i==7)v=String.valueOf(commoditySp.getSelectedItem());if(i==11)v=String.valueOf(paymentSp.getSelectedItem());if(i==17)v=String.valueOf(companySp.getSelectedItem());r.put(keys[i],v);}
            r.put("paymentDetail",paymentDetailSp==null||paymentDetailSp.getSelectedItem()==null?"":String.valueOf(paymentDetailSp.getSelectedItem()));
            r.put("cardRegistrationDate",isShahedaneh(unit)?"":(cardDateInput==null?"":cardDateInput.getText().toString().trim())); if(isShahedaneh(unit)){ r.put("chickCount",""); r.put("placementDate","");r.put("transferSourceUnit",transferSource);r.put("transferSourceCertificate",cert);r.put("transferOrigin","quotaTransfer");}
            r.put("id",old==null?UUID.randomUUID().toString():old.optString("id"));r.put("buyer",unit);boolean alarmConfigured=ds.trim().matches("\\d+")&&tm.trim().matches("([01]\\d|2[0-3]):[0-5]\\d")&&!inputs.get(18).getText().toString().trim().isEmpty();r.remove("subDue");r.put("collected",old!=null&&old.optBoolean("collected",false));r.put("collectedDate",old==null?"":old.optString("collectedDate",""));r.put("allocated",old!=null&&old.optBoolean("allocated",false));r.put("allocatedDate",old==null?"":old.optString("allocatedDate",""));r.put("funding",old!=null&&old.has("funding")?old.optJSONArray("funding"):new JSONArray());r.put("alarm",alarmConfigured);r.put("alarmDays",parseInt(ds,1));r.put("alarmTime",tm.trim());r.put("alarmRepeat",repeat);
            JSONArray a=AppData.arr(data,"purchases");boolean replaced=false;for(int i=0;i<a.length();i++)if(a.optJSONObject(i).optString("id").equals(r.optString("id"))){a.put(i,r);replaced=true;break;}if(!replaced)a.put(r);data.put("purchases",a);if(!isShahedaneh(unit))updateCertificateCompletion(unit,cert);addUnique("units",unit);addUnique("buyers",unit);addUnique("companies",r.optString("company"));AppData.save(this,data);if(alarmConfigured)schedule(this,r);else cancelAlarm(this,r);boolean complete=isCertificateComplete(unit,cert);Toast.makeText(this,complete?"خرید با موفقیت ذخیره شد؛ خرید سهمیه این گواهی تکمیل شد.":"خرید با موفقیت ذخیره شد",Toast.LENGTH_LONG).show();goHome();
        }catch(Exception e){Toast.makeText(this,"خطا در ذخیره اطلاعات",Toast.LENGTH_LONG).show();}
    }
    int parseInt(String s,int d){try{return Integer.parseInt(s.trim());}catch(Exception e){return d;}}
    void addUnique(String key,String val)throws Exception{if(val==null||val.trim().isEmpty())return;JSONArray a=AppData.arr(data,key);for(int i=0;i<a.length();i++)if(a.optString(i).equals(val))return;a.put(val);}

    boolean isCertificateComplete(String unit,String cert){JSONObject base=certificateBase(unit+"|"+cert,null);if(base==null)return false;double cq=quotaOriginal(base,"cornQuota"),sq=quotaOriginal(base,"soyQuota");return usedCorn(unit,cert,null)+transferAmount(unit,cert,"corn")>=cq-0.001&&usedSoy(unit,cert,null)+transferAmount(unit,cert,"soy")>=sq-0.001;}
    void updateCertificateCompletion(String unit,String cert){try{boolean full=isCertificateComplete(unit,cert);JSONArray a=AppData.arr(data,"purchases");for(int i=0;i<a.length();i++){JSONObject p=a.optJSONObject(i);if(p!=null&&unit.equals(p.optString("unit",p.optString("buyer")))&&cert.equals(p.optString("healthCertificate"))){p.put("quotaCompleted",full);p.put("quotaRemainingCorn",fmtDecimal(Math.max(0,quotaOriginal(certificateBase(unit+"|"+cert,null),"cornQuota")-usedCorn(unit,cert,null)-transferAmount(unit,cert,"corn"))));p.put("quotaRemainingSoy",fmtDecimal(Math.max(0,quotaOriginal(certificateBase(unit+"|"+cert,null),"soyQuota")-usedSoy(unit,cert,null)-transferAmount(unit,cert,"soy"))));}}}catch(Exception ignored){}}

    void listPurchases(String fixedBuyer){
        base(fixedBuyer==null?"خریدهای ثبت‌شده":"پرونده "+fixedBuyer);

        EditText q=input("جستجو: نام واحد، گواهی، شماره خرید، شرکت یا نهاده");
        LinearLayout dates=new LinearLayout(this); dates.setOrientation(LinearLayout.HORIZONTAL);
        EditText from=new EditText(this),to=new EditText(this);
        from.setHint("از تاریخ");to.setHint("تا تاریخ");from.setSingleLine(true);to.setSingleLine(true);
        from.setOnClickListener(v->pickDate(from));to.setOnClickListener(v->pickDate(to));
        dates.addView(from,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));
        dates.addView(to,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));add(dates);

        LinearLayout row1=new LinearLayout(this),row2=new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);row2.setOrientation(LinearLayout.HORIZONTAL);
        Spinner cs=spinner(new String[]{"وصول: همه","وصول شده","وصول نشده"});
        Spinner as=spinner(new String[]{"تخصیص: همه","تخصیص شده","تخصیص نشده"});
        Spinner fs=spinner(new String[]{"تأمین: همه","تأمین کامل","تأمین ناقص"});
        Spinner qs=spinner(new String[]{"سهمیه: همه","سهمیه کامل","سهمیه مانده"});
        row1.addView(cs,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));
        row1.addView(as,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));
        row2.addView(fs,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));
        row2.addView(qs,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));
        add(row1);add(row2);

        if(initialCollectedFilter>=0&&initialCollectedFilter<=2)cs.setSelection(initialCollectedFilter);
        if(initialAllocatedFilter>=0&&initialAllocatedFilter<=2)as.setSelection(initialAllocatedFilter);
        if(initialFundingFilter>=0&&initialFundingFilter<=2)fs.setSelection(initialFundingFilter);
        if(initialQuotaFilter>=0&&initialQuotaFilter<=2)qs.setSelection(initialQuotaFilter);
        initialCollectedFilter=initialAllocatedFilter=initialFundingFilter=initialQuotaFilter=0;

        Button go=btn("🔎 اعمال فیلتر");add(go);

        LinearLayout summaryBox=new LinearLayout(this); summaryBox.setOrientation(LinearLayout.VERTICAL);
        summaryBox.setPadding(UiManager.dp(this,10),UiManager.dp(this,8),UiManager.dp(this,10),UiManager.dp(this,8));
        GradientDrawable summaryBg=new GradientDrawable();
        summaryBg.setColor(UiManager.card(this));
        summaryBg.setCornerRadius(UiManager.dp(this,14));
        summaryBg.setStroke(UiManager.dp(this,1),UiManager.secondary(this));
        summaryBox.setBackground(summaryBg);
        TextView summaryTitle=tv("📊  خلاصه نتایج نمایش‌داده‌شده",17);
        summaryTitle.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
        summaryTitle.setTypeface(UiManager.selectedTypeface(this,Typeface.BOLD));
        summaryTitle.setTextColor(UiManager.primary(this));
        summaryBox.addView(summaryTitle,new LinearLayout.LayoutParams(-1,UiManager.dp(this,48)));
        LinearLayout summaryGrid=new LinearLayout(this);summaryGrid.setOrientation(LinearLayout.VERTICAL);summaryBox.addView(summaryGrid);
        add(summaryBox);

        LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);add(list);

        Runnable render=()->{
            list.removeAllViews();
            summaryGrid.removeAllViews();
            int count=0,col=0,alloc=0,fullFund=0,fullQuota=0;
            long amount=0,funded=0,unfunded=0;
            double initialCorn=0,purchasedCorn=0,remainingCorn=0,initialSoy=0,purchasedSoy=0,remainingSoy=0,transferredCorn=0,transferredSoy=0;
            HashSet<String> certificateKeys=new HashSet<>();
            JSONArray a=sortedPurchases();
            String search=q.getText().toString().trim(),f=from.getText().toString().trim(),t=to.getText().toString().trim();
            ArrayList<JSONObject> shown=new ArrayList<>();

            for(int i=0;i<a.length();i++){
                JSONObject p=a.optJSONObject(i);if(p==null)continue;
                String unit=p.optString("unit",p.optString("buyer"));
                if(fixedBuyer!=null&&!fixedBuyer.equals(unit))continue;

                boolean collected=p.optBoolean("collected"),allocated=p.optBoolean("allocated");
                boolean fundComplete=fundingRemaining(p)<=0;
                String cert=p.optString("healthCertificate").trim();
                JSONObject certBase=cert.isEmpty()?null:certificateBase(unit+"|"+cert,null);
                boolean hasQuota=certBase!=null&&quotaOriginal(certBase,"cornQuota")>0&&quotaOriginal(certBase,"soyQuota")>0;
                boolean quotaComplete=hasQuota&&isCertificateComplete(unit,cert);

                if(cs.getSelectedItemPosition()==1&&!collected)continue;
                if(cs.getSelectedItemPosition()==2&&collected)continue;
                if(as.getSelectedItemPosition()==1&&!allocated)continue;
                if(as.getSelectedItemPosition()==2&&allocated)continue;
                if(fs.getSelectedItemPosition()==1&&!fundComplete)continue;
                if(fs.getSelectedItemPosition()==2&&fundComplete)continue;
                if(qs.getSelectedItemPosition()==1&&(!hasQuota||!quotaComplete))continue;
                if(qs.getSelectedItemPosition()==2&&(!hasQuota||quotaComplete))continue;

                String blob=unit+" "+cert+" "+p.optString("purchaseNo")+" "+p.optString("company")+" "+p.optString("commodity");
                if(!search.isEmpty()&&!blob.contains(search))continue;
                if(!f.isEmpty()&&!p.optString("buyDate").isEmpty()&&p.optString("buyDate").compareTo(f)<0)continue;
                if(!t.isEmpty()&&!p.optString("buyDate").isEmpty()&&p.optString("buyDate").compareTo(t)>0)continue;

                shown.add(p);count++;amount+=toLong(p.optString("amount"));
                long pf=totalFunded(p);funded+=pf;unfunded+=fundingRemaining(p);
                if(collected)col++; if(allocated)alloc++; if(fundComplete)fullFund++; if(quotaComplete)fullQuota++;

                if(!cert.isEmpty()){
                    String ck=unit+"|"+cert;
                    if(certificateKeys.add(ck)){
                        JSONObject baseCert=certificateBase(ck,null);
                        if(baseCert!=null){
                            double cq=quotaOriginal(baseCert,"cornQuota"),sq=quotaOriginal(baseCert,"soyQuota");
                            double remC=Math.max(0,cq-usedCorn(unit,cert,null)-transferAmount(unit,cert,"corn"));
                            double remS=Math.max(0,sq-usedSoy(unit,cert,null)-transferAmount(unit,cert,"soy"));
                            initialCorn+=cq;purchasedCorn+=usedCorn(unit,cert,null);remainingCorn+=remC;
                            initialSoy+=sq;purchasedSoy+=usedSoy(unit,cert,null);remainingSoy+=remS;
                            transferredCorn+=transferAmount(unit,cert,"corn");transferredSoy+=transferAmount(unit,cert,"soy");
                        }
                    }
                }
            }

            addSummaryStatRow(summaryGrid,
                    new String[]{"📄 تعداد خریدها","💰 مبلغ کل","✓ وصول شده","👥 تخصیص شده","✓ تأمین کامل","✓ سهمیه کامل"},
                    new String[]{""+count,AppData.fmt(""+amount)+" ریال",""+col,""+alloc,""+fullFund,""+fullQuota});
            addSummaryStatRow(summaryGrid,
                    new String[]{"💳 مجموع تأمین‌شده","⏳ مجموع تأمین‌نشده","⚖ وزن خریدها"},
                    new String[]{AppData.fmt(""+funded)+" ریال",AppData.fmt(""+unfunded)+" ریال",fmtDecimal(totalPurchaseWeight(shown))+" کیلوگرم"});
            addSummaryCommodityRow(summaryGrid,"🌽 ذرت",
                    new String[]{"سهمیه اولیه","خرید شده","انتقال داده شده","مانده"},
                    new String[]{fmtDecimal(initialCorn),fmtDecimal(purchasedCorn),fmtDecimal(transferredCorn),fmtDecimal(remainingCorn)});
            addSummaryCommodityRow(summaryGrid,"🌱 سویا",
                    new String[]{"سهمیه اولیه","خرید شده","انتقال داده شده","مانده"},
                    new String[]{fmtDecimal(initialSoy),fmtDecimal(purchasedSoy),fmtDecimal(transferredSoy),fmtDecimal(remainingSoy)});
            TextView weightSummary=tv(inputWeightSummary(shown),11);
            weightSummary.setPadding(UiManager.dp(this,8),UiManager.dp(this,8),UiManager.dp(this,8),UiManager.dp(this,8));
            weightSummary.setGravity(Gravity.RIGHT);weightSummary.setTextColor(UiManager.text(this));
            summaryGrid.addView(weightSummary,new LinearLayout.LayoutParams(-1,ViewGroup.LayoutParams.WRAP_CONTENT));

            addPurchaseTable(list,shown);
        };
        go.setOnClickListener(v->render.run());render.run();
        Button back=btn("← بازگشت");back.setOnClickListener(v->back());add(back);
        finishScreen("خریدهای ثبت‌شده");
    }

    void addSummaryStatRow(LinearLayout parent,String[] titles,String[] values){
        LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);
        for(int i=titles.length-1;i>=0;i--){
            LinearLayout cell=summaryCell(titles[i],values[i],false);
            row.addView(cell,new LinearLayout.LayoutParams(0,UiManager.dp(this,82),1));
            if(i>0){View divider=new View(this);divider.setBackgroundColor(UiManager.secondary(this));row.addView(divider,new LinearLayout.LayoutParams(UiManager.dp(this,1),UiManager.dp(this,62)));}
        }
        parent.addView(row,new LinearLayout.LayoutParams(-1,UiManager.dp(this,86)));
    }

    void addSummaryCommodityRow(LinearLayout parent,String title,String[] labels,String[] values){
        LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout titleCell=summaryCell(title,"",true);row.addView(titleCell,new LinearLayout.LayoutParams(0,UiManager.dp(this,86),1));
        for(int i=0;i<labels.length;i++){
            View divider=new View(this);divider.setBackgroundColor(UiManager.secondary(this));row.addView(divider,new LinearLayout.LayoutParams(UiManager.dp(this,1),UiManager.dp(this,66)));
            LinearLayout cell=summaryCell(labels[i],values[i]+"\nکیلوگرم",false);row.addView(cell,new LinearLayout.LayoutParams(0,UiManager.dp(this,86),1));
        }
        parent.addView(row,new LinearLayout.LayoutParams(-1,UiManager.dp(this,90)));
    }

    LinearLayout summaryCell(String title,String value,boolean titleOnly){
        LinearLayout cell=new LinearLayout(this);cell.setOrientation(LinearLayout.VERTICAL);cell.setGravity(Gravity.CENTER);cell.setPadding(UiManager.dp(this,4),UiManager.dp(this,3),UiManager.dp(this,4),UiManager.dp(this,3));
        TextView t=tv(title,11);t.setGravity(Gravity.CENTER);t.setTextColor(UiManager.text(this));cell.addView(t,new LinearLayout.LayoutParams(-1,0,1));
        if(!titleOnly){TextView v=tv(value,13);v.setGravity(Gravity.CENTER);v.setTypeface(UiManager.selectedTypeface(this,Typeface.BOLD));v.setTextColor(UiManager.primary(this));cell.addView(v,new LinearLayout.LayoutParams(-1,0,1));}
        return cell;
    }

    double totalPurchaseWeight(List<JSONObject> purchases){double s=0;for(JSONObject p:purchases)if(p!=null)s+=toDouble(p.optString("weight"));return s;}

    void addPurchaseTable(LinearLayout list,ArrayList<JSONObject> shown){
        TextView title=tv("📋  جزئیات خریدهای نمایش‌داده‌شده",16);title.setTypeface(UiManager.selectedTypeface(this,Typeface.BOLD));title.setTextColor(UiManager.primary(this));addTo(list,title);
        HorizontalScrollView hsv=new HorizontalScrollView(this);hsv.setFillViewport(true);hsv.setHorizontalScrollBarEnabled(false);
        TableLayout table=new TableLayout(this);table.setStretchAllColumns(true);table.setShrinkAllColumns(false);table.setPadding(0,UiManager.dp(this,3),0,UiManager.dp(this,3));
        String[] heads={"ردیف","واحد","گواهی","شماره خرید","تاریخ خرید","وزن","مبلغ کل","پرداخت","عملیات"};
        TableRow head=new TableRow(this);for(String h:heads)head.addView(tableCell(h,true,false));table.addView(head);
        for(int i=0;i<shown.size();i++){
            JSONObject p=shown.get(i);String unit=p.optString("unit",p.optString("buyer"));
            boolean collected=p.optBoolean("collected"),fundComplete=fundingRemaining(p)<=0;
            String payment=p.optString("payment","-");String detail=p.optString("paymentDetail","");
            if(!detail.isEmpty())payment+="\n("+detail+")";
            TableRow r=new TableRow(this);
            r.addView(tableCell(""+(i+1),false,false));r.addView(tableCell(unit,false,false));
            r.addView(tableCell(p.optString("healthCertificate","-"),false,false));r.addView(tableCell(p.optString("purchaseNo","-"),false,false));
            r.addView(tableCell(p.optString("buyDate","-"),false,false));r.addView(tableCell(fmtDecimal(toDouble(p.optString("weight"))),false,false));
            r.addView(tableCell(AppData.fmt(p.optString("amount","0")),false,false));
            TextView pay=tableCell((collected?"پرداخت شده":"در انتظار")+"\\n"+payment,false,collected);r.addView(pay);
            LinearLayout actions=new LinearLayout(this);actions.setGravity(Gravity.CENTER);actions.setOrientation(LinearLayout.HORIZONTAL);
            Button view=miniAction("◉");view.setOnClickListener(v->openPage(()->details(p)));actions.addView(view);
            Button edit=miniAction("✎");edit.setOnClickListener(v->openPage(()->form(p)));actions.addView(edit);
            Button del=miniAction("🗑");del.setTextColor(Color.rgb(220,50,50));del.setOnClickListener(v->confirmDelete(p));actions.addView(del);r.addView(actions);
            table.addView(r);
        }
        hsv.addView(table,new ViewGroup.LayoutParams(-1,-2));list.addView(hsv,new LinearLayout.LayoutParams(-1,-2));
    }

    void addTo(LinearLayout parent,View v){parent.addView(v,new LinearLayout.LayoutParams(-1,ViewGroup.LayoutParams.WRAP_CONTENT));}
    TextView tableCell(String text,boolean header,boolean success){
        TextView v=tv(text,header?10:9);v.setGravity(Gravity.CENTER);v.setPadding(UiManager.dp(this,3),UiManager.dp(this,8),UiManager.dp(this,3),UiManager.dp(this,8));
        GradientDrawable g=new GradientDrawable();g.setColor(header?UiManager.primary(this):UiManager.card(this));g.setStroke(UiManager.dp(this,1),UiManager.secondary(this));g.setCornerRadius(header?UiManager.dp(this,6):0);v.setBackground(g);
        v.setTextColor(header?Color.WHITE:(success?UiManager.primary(this):UiManager.text(this)));if(success)v.setTypeface(UiManager.selectedTypeface(this,Typeface.BOLD));return v;
    }
    Button miniAction(String text){Button b=btn(text);b.setTextSize(12);b.setPadding(0,0,0,0);b.setMinWidth(UiManager.dp(this,30));b.setMinHeight(UiManager.dp(this,34));return b;}

    JSONArray sortedPurchases(){JSONArray src=AppData.arr(data,"purchases");ArrayList<JSONObject> l=new ArrayList<>();for(int i=0;i<src.length();i++)l.add(src.optJSONObject(i));Collections.sort(l,(a,b)->b.optString("buyDate").compareTo(a.optString("buyDate")));JSONArray r=new JSONArray();for(JSONObject p:l)r.put(p);return r;}

    void details(JSONObject p){
        base("جزئیات کامل خرید");
        String unitName=p.optString("unit",p.optString("buyer"));
        add(tv("🏠 "+unitName+"\nگواهی: "+p.optString("healthCertificate")+"\nشماره خرید: "+p.optString("purchaseNo"),19));
        for(int i=0;i<keys.length;i++)add(tv(labels[i]+": "+p.optString(keys[i],"-"),15));
        add(tv("جزئیات پرداخت: "+p.optString("paymentDetail","-"),15)); if(isShahedaneh(unitName)){add(tv("🔄 منبع سهمیه منتقل‌شده\nواحد مبدأ: "+p.optString("transferSourceUnit","-")+"\nگواهی مبدأ: "+p.optString("transferSourceCertificate",p.optString("healthCertificate","-"))+"\nنوع منشأ: انتقال مانده سهمیه به شاهدانه",15));}
        add(tv("تاریخ ثبت کارت: "+p.optString("cardRegistrationDate",p.optString("cardDate","-")),15));
        add(tv("نوع نهاده / وزن:\n• "+p.optString("commodity","-")+" : "+fmtDecimal(toDouble(p.optString("weight")))+" کیلوگرم",15));
        String cert=p.optString("healthCertificate");JSONObject cb=certificateBase(unitName+"|"+cert,null);if(cb!=null){double cq=quotaOriginal(cb,"cornQuota"),sq=quotaOriginal(cb,"soyQuota"),uc=usedCorn(unitName,cert,null),us=usedSoy(unitName,cert,null),tc=transferAmount(unitName,cert,"corn"),ts=transferAmount(unitName,cert,"soy");add(tv("📌 وضعیت سهمیه این گواهی\nسهمیه اولیه ذرت: "+fmtDecimal(cq)+" کیلوگرم\nخریدشده ذرت: "+fmtDecimal(uc)+" | مانده سهمیه: "+fmtDecimal(Math.max(0,cq-uc-tc))+" کیلوگرم\nذرت منتقل‌شده به شاهدانه: "+fmtDecimal(tc)+" کیلوگرم\nسهمیه اولیه سویا: "+fmtDecimal(sq)+" کیلوگرم\nخریدشده سویا: "+fmtDecimal(us)+" | مانده سهمیه: "+fmtDecimal(Math.max(0,sq-us-ts))+" کیلوگرم\nسویا منتقل‌شده به شاهدانه: "+fmtDecimal(ts)+" کیلوگرم\n"+(isCertificateComplete(unitName,cert)?"✅ تکمیل خرید سهمیه":"⏳ سهمیه هنوز تکمیل نشده است"),15));}
        add(tv("وضعیت وصول: "+(p.optBoolean("collected")?"✅ وصول شد":"⏳ وصول نشده")+
                (p.optString("collectedDate").isEmpty()?"":" | تاریخ وصول: "+p.optString("collectedDate")),16));
        add(tv("وضعیت تخصیص: "+(p.optBoolean("allocated")?"✅ تخصیص شد":"⏳ تخصیص نشده"),16));
        long req=toLong(p.optString("amount")),fund=totalFunded(p),rem=fundingRemaining(p);
        add(tv("وضعیت تأمین موجودی: "+(rem<=0?"✅ تأمین کامل":"⏳ تأمین تکمیل نشده")+
                "\nمبلغ موردنیاز: "+AppData.fmt(""+req)+" ریال"+
                "\nتأمین‌شده: "+AppData.fmt(""+fund)+" ریال"+
                "\nباقی‌مانده: "+AppData.fmt(""+rem)+" ریال"+
                "\nتعداد مراحل: "+funding(p).length(),16));
        JSONArray stages=funding(p);
        for(int i=0;i<stages.length();i++){
            JSONObject st=stages.optJSONObject(i);
            if(st!=null)add(tv("مرحله "+(i+1)+": "+st.optString("date")+
                    " | "+AppData.fmt(st.optString("amount"))+" ریال"+
                    "\nمنبع: "+st.optString("source")+" | مقصد: "+st.optString("destination"),14));
        }
        if(p.optBoolean("alarm"))add(tv("🔔 هشدار مستقل: "+p.optInt("alarmDays",1)+" روز قبل، ساعت "+
                p.optString("alarmTime")+" | "+(p.optBoolean("alarmRepeat")?"تکراری تا سررسید":"یک‌بار"),15));

        Button c=btn(p.optBoolean("collected")?"↩️ لغو وصول":"💰 وصول شد");
        c.setOnClickListener(v->changeCollected(p));add(c);
        Button al=btn(p.optBoolean("allocated")?"↩️ لغو تخصیص":"📦 تخصیص شد");
        al.setOnClickListener(v->changeBoolean(p,"allocated","تخصیص"));add(al);
        Button fu=btn("💳 تأمین موجودی");fu.setOnClickListener(v->fundingDialog(p));add(fu);
        Button remA=btn("🔕 حذف آلارم");remA.setOnClickListener(v->{try{p.put("alarm",false);AppData.save(this,data);cancelAlarm(this,p);details(p);}catch(Exception ignored){}});add(remA);
        Button ed=btn("✏️ ویرایش");ed.setOnClickListener(v->openPage(()->form(p)));add(ed);
        Button same=btn("🏠 پرونده "+p.optString("unit",p.optString("buyer")));same.setOnClickListener(v->openPage(()->listPurchases(p.optString("unit",p.optString("buyer")))));add(same);
        Button del=btn("🗑 حذف");del.setOnClickListener(v->confirmDelete(p));add(del);
        Button back=btn("← بازگشت");back.setOnClickListener(v->back());add(back);
        finishScreen("جزئیات کامل خرید");
    }

    void changeCollected(JSONObject p){
        if(!p.optBoolean("collected",false)){
            EditText d=new EditText(this);d.setHint("تاریخ وصول");d.setSingleLine(true);d.setText(PersianDate.today());d.setOnClickListener(v->pickDate(d));
            new AlertDialog.Builder(this).setTitle("تاریخ وصول").setView(d)
                .setPositiveButton("ثبت",(x,w)->{try{p.put("collected",true);p.put("collectedDate",d.getText().toString());AppData.save(this,data);details(p);}catch(Exception ignored){}})
                .setNegativeButton("لغو",null).show();
        }else{
            new AlertDialog.Builder(this).setTitle("لغو وصول").setMessage("وضعیت وصول لغو و تاریخ وصول پاک شود؟")
                .setPositiveButton("بله",(x,w)->{try{p.put("collected",false);p.put("collectedDate","");AppData.save(this,data);details(p);}catch(Exception ignored){}})
                .setNegativeButton("خیر",null).show();
        }
    }
    void changeBoolean(JSONObject p,String key,String title){
        boolean next=!p.optBoolean(key,false);
        new AlertDialog.Builder(this).setTitle("تأیید تغییر وضعیت")
            .setMessage("آیا از "+(next?"فعال‌سازی ":"غیرفعال‌سازی ")+title+" مطمئن هستید؟")
            .setPositiveButton("بله",(d,w)->{try{p.put(key,next);if("allocated".equals(key))p.put("allocatedDate",next?PersianDate.today():"");AppData.save(this,data);details(p);}catch(Exception ignored){}})
            .setNegativeButton("خیر",null).show();
    }

    void fundingDialog(JSONObject p){
        long remaining=fundingRemaining(p);
        if(remaining<=0){Toast.makeText(this,"مبلغ تأمین موجودی تکمیل شده است.",Toast.LENGTH_LONG).show();return;}
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);
        EditText date=new EditText(this);date.setHint("تاریخ مرحله");date.setText(PersianDate.today());date.setOnClickListener(v->pickDate(date));box.addView(date);
        EditText amount=new EditText(this);amount.setHint("مبلغ این مرحله (ریال)");amount.setInputType(2);box.addView(amount);
        Spinner source=accountSpinner("منبع تأمین","sourceAccounts");box.addView(source);
        Spinner dest=accountSpinner("مقصد","destinationAccounts");box.addView(dest);
        box.addView(tv("باقی‌مانده فعلی: "+AppData.fmt(""+remaining)+" ریال",13));
        new AlertDialog.Builder(this).setTitle("ثبت مرحله تأمین موجودی").setView(box)
            .setPositiveButton("ثبت مرحله",(d,w)->{
                try{
                    long a=toLong(amount.getText().toString());
                    String srcNo=selectedAccountNumber(source),dstNo=selectedAccountNumber(dest);
                    if(srcNo.isEmpty()||dstNo.isEmpty()){Toast.makeText(this,"منبع و مقصد را انتخاب کنید.",Toast.LENGTH_LONG).show();return;}
                    if(a<=0||a>remaining){Toast.makeText(this,"مبلغ مرحله باید بیشتر از صفر و حداکثر برابر باقی‌مانده باشد.",Toast.LENGTH_LONG).show();return;}
                    JSONArray arr=funding(p);JSONObject st=new JSONObject();
                    st.put("date",date.getText().toString());st.put("amount",a);
                    st.put("source",srcNo);st.put("destination",dstNo);arr.put(st);
                    p.put("funding",arr);AppData.save(this,data);
                    Toast.makeText(this,fundingRemaining(p)<=0?"مبلغ تأمین موجودی تکمیل شد.":"تأمین تکمیل نشده",Toast.LENGTH_LONG).show();details(p);
                }catch(Exception e){Toast.makeText(this,"ثبت مرحله انجام نشد.",Toast.LENGTH_LONG).show();}
            }).setNegativeButton("لغو",null).show();
    }
    JSONArray funding(JSONObject p){JSONArray a=p.optJSONArray("funding");return a==null?new JSONArray():a;}
    long totalFunded(JSONObject p){long s=0;JSONArray a=funding(p);for(int i=0;i<a.length();i++)s+=toLong(a.optJSONObject(i).optString("amount"));return s;}
    long fundingRemaining(JSONObject p){return Math.max(0,toLong(p.optString("amount"))-totalFunded(p));}

    JSONObject findPurchase(String id){JSONArray a=AppData.arr(data,"purchases");for(int i=0;i<a.length();i++){JSONObject p=a.optJSONObject(i);if(p!=null&&id.equals(p.optString("id")))return p;}return null;}
    void confirmDelete(JSONObject p){new AlertDialog.Builder(this).setTitle("حذف خرید").setMessage("این خرید حذف شود؟").setPositiveButton("حذف",(d,w)->delete(p)).setNegativeButton("لغو",null).show();}
    void delete(JSONObject p){try{cancelAlarm(this,p);JSONArray a=AppData.arr(data,"purchases"),b=new JSONArray();for(int i=0;i<a.length();i++)if(!a.getJSONObject(i).optString("id").equals(p.optString("id")))b.put(a.getJSONObject(i));data.put("purchases",b);AppData.save(this,data);goHome();}catch(Exception ignored){}}

    void buyersPage(){
        base("واحدها");EditText q=input("جستجوی نام واحد");Button go=btn("🔎 جستجو");add(go);LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);add(list);
        Runnable render=()->{list.removeAllViews();JSONArray b=AppData.arr(data,"units");String s=q.getText().toString().trim();for(int i=0;i<b.length();i++){String name=b.optString(i);if(!s.isEmpty()&&!name.contains(s))continue;Button x=btn("🏠 "+name);x.setOnClickListener(v->openPage(()->buyerFile(name)));list.addView(x);}};go.setOnClickListener(v->render.run());render.run();Button addb=btn("➕ افزودن واحد");addb.setOnClickListener(v->addEntry("units","واحد جدید",this::buyersPage));add(addb);Button back=btn("← بازگشت");back.setOnClickListener(v->back());add(back);finishScreen("واحدها");
    }
    void buyerFile(String buyer){
        base("پرونده واحد: "+buyer);
        JSONArray a=AppData.arr(data,"purchases");
        int n=0,coll=0,alloc=0,fullFund=0,fullQuota=0;
        long total=0,colAmt=0,fundedAmt=0,unfundedAmt=0;double initialCorn=0,purchasedCorn=0,remainingCorn=0,initialSoy=0,purchasedSoy=0,remainingSoy=0,transferredCorn=0,transferredSoy=0;
        ArrayList<JSONObject> noColl=new ArrayList<>(),noAlloc=new ArrayList<>(),noFund=new ArrayList<>();
        HashSet<String> certKeys=new HashSet<>();
        ArrayList<String> incompleteCerts=new ArrayList<>();

        for(int i=0;i<a.length();i++){
            JSONObject p=a.optJSONObject(i);if(p==null||!buyer.equals(p.optString("unit",p.optString("buyer"))))continue;
            n++;long amt=toLong(p.optString("amount"));total+=amt;
            long pf=totalFunded(p),rem=fundingRemaining(p);fundedAmt+=pf;unfundedAmt+=rem;
            if(p.optBoolean("collected")){coll++;colAmt+=amt;}else noColl.add(p);
            if(p.optBoolean("allocated"))alloc++;else noAlloc.add(p);
            if(rem<=0)fullFund++;else noFund.add(p);

            String cert=p.optString("healthCertificate").trim();
            if(!cert.isEmpty()){
                String ck=buyer+"|"+cert;
                if(certKeys.add(ck)){
                    JSONObject cb=certificateBase(ck,null);
                    if(cb!=null){
                        double cq=quotaOriginal(cb,"cornQuota"),sq=quotaOriginal(cb,"soyQuota");
                        double rc=Math.max(0,cq-usedCorn(buyer,cert,null)-transferAmount(buyer,cert,"corn"));
                        double rs=Math.max(0,sq-usedSoy(buyer,cert,null)-transferAmount(buyer,cert,"soy"));
                        initialCorn+=cq; purchasedCorn+=usedCorn(buyer,cert,null); transferredCorn+=transferAmount(buyer,cert,"corn"); remainingCorn+=rc; initialSoy+=sq; purchasedSoy+=usedSoy(buyer,cert,null); transferredSoy+=transferAmount(buyer,cert,"soy"); remainingSoy+=rs;
                        boolean complete=rc<=0.0001&&rs<=0.0001;
                        if(complete)fullQuota++;else incompleteCerts.add(cert);
                    }
                }
            }
        }

        add(tv("آمار کل\nتعداد خرید: "+n+
                "\nمجموع مبلغ: "+AppData.fmt(""+total)+" ریال"+
                "\nوصول‌شده: "+coll+" خرید، "+AppData.fmt(""+colAmt)+" ریال"+
                "\nوصول‌نشده: "+(n-coll)+" خرید"+
                "\nتخصیص‌شده: "+alloc+" خرید"+
                "\nتخصیص‌نشده: "+(n-alloc)+" خرید"+
                "\nتأمین کامل: "+fullFund+" خرید"+
                "\nتأمین ناقص: "+(n-fullFund)+" خرید"+
                "\nمجموع تأمین‌شده: "+AppData.fmt(""+fundedAmt)+" ریال"+
                "\nمجموع تأمین‌نشده: "+AppData.fmt(""+unfundedAmt)+" ریال"+
                "\n"+inputWeightSummary(purchasesInRange(buyer,"", ""))+
                "\nسهمیه اولیه ذرت: "+fmtDecimal(initialCorn)+" کیلوگرم"+
                "\nخرید شده ذرت: "+fmtDecimal(purchasedCorn)+" کیلوگرم"+
                "\nذرت انتقال داده: "+fmtDecimal(transferredCorn)+" کیلوگرم"+
                "\nمانده سهمیه ذرت: "+fmtDecimal(remainingCorn)+" کیلوگرم"+
                "\nسهمیه اولیه سویا: "+fmtDecimal(initialSoy)+" کیلوگرم"+
                "\nخرید شده سویا: "+fmtDecimal(purchasedSoy)+" کیلوگرم"+
                "\nسویا انتقال داده: "+fmtDecimal(transferredSoy)+" کیلوگرم"+
                "\nمانده سهمیه سویا: "+fmtDecimal(remainingSoy)+" کیلوگرم"+
                "\nگواهی‌های کامل: "+fullQuota+" | گواهی‌های دارای مانده: "+incompleteCerts.size(),15));

        add(tv("شماره‌های وصول‌نشده",15));addPurchaseNumberList(noColl);
        add(tv("شماره‌های تخصیص‌نشده",15));addPurchaseNumberList(noAlloc);
        add(tv("شماره‌های تأمین ناقص",15));addPurchaseNumberList(noFund);

        add(tv("گواهی‌های بهداشتی دارای مانده سهمیه",15));
        if(incompleteCerts.isEmpty())add(tv("ندارد",14));
        else{
            for(String cert:incompleteCerts){
                JSONObject cb=certificateBase(buyer+"|"+cert,null);
                double cq=cb==null?0:quotaOriginal(cb,"cornQuota"),sq=cb==null?0:quotaOriginal(cb,"soyQuota");
                double rc=Math.max(0,cq-usedCorn(buyer,cert,null)),rs=Math.max(0,sq-usedSoy(buyer,cert,null));
                Button cbn=btn("📁 گواهی بهداشتی "+cert+"\nمانده ذرت: "+fmtDecimal(rc)+" | مانده سویا: "+fmtDecimal(rs)+" کیلوگرم");
                cbn.setOnClickListener(v->openPage(()->certificateFile(buyer,cert)));add(cbn);
            }
        }

        add(tv("فیلتر سریع خریدهای این واحد",16));
        LinearLayout row1=new LinearLayout(this),row2=new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);row2.setOrientation(LinearLayout.HORIZONTAL);
        Spinner cs=spinner(new String[]{"وصول: همه","وصول شده","وصول نشده"});
        Spinner as=spinner(new String[]{"تخصیص: همه","تخصیص شده","تخصیص نشده"});
        Spinner fs=spinner(new String[]{"تأمین: همه","تأمین کامل","تأمین ناقص"});
        Spinner qs=spinner(new String[]{"سهمیه: همه","سهمیه کامل","سهمیه مانده"});
        row1.addView(cs,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));
        row1.addView(as,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));
        row2.addView(fs,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));
        row2.addView(qs,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));
        add(row1);add(row2);

        Button apply=btn("🔎 نمایش خریدهای فیلترشده");
        apply.setOnClickListener(v->{
            initialCollectedFilter=cs.getSelectedItemPosition();
            initialAllocatedFilter=as.getSelectedItemPosition();
            initialFundingFilter=fs.getSelectedItemPosition();
            initialQuotaFilter=qs.getSelectedItemPosition();
            openPage(()->listPurchases(buyer));
        });add(apply);

        Button stat=btn("📊 آمار بازه زمانی");stat.setOnClickListener(v->buyerStats(buyer));add(stat);
        Button back=btn("← بازگشت");back.setOnClickListener(v->back());add(back);finishScreen("پرونده واحد");
    }

    void incompleteFundingPage(){ simpleIncompletePage("تأمین موجودی",p->fundingRemaining(p)>0,"هنوز خریدی با تأمین موجودی ناقص وجود ندارد.","funding"); }
    void incompleteCollectionPage(){ simpleIncompletePage("وصول",p->!p.optBoolean("collected",false),"همه خریدها وصول شده‌اند.","collection"); }
    void incompleteAllocationPage(){ simpleIncompletePage("تخصیص",p->!p.optBoolean("allocated",false),"همه خریدها تخصیص شده‌اند.","allocation"); }

    interface PurchaseFilter{boolean accept(JSONObject p);}
    void simpleIncompletePage(String title,PurchaseFilter filter,String empty){simpleIncompletePage(title,filter,empty,title.equals("تأمین موجودی")?"funding":title.equals("وصول")?"collection":"allocation");}
    void simpleIncompletePage(String title,PurchaseFilter filter,String empty,String mode){
        base(title);
        LinearLayout dates=new LinearLayout(this);dates.setOrientation(LinearLayout.HORIZONTAL);
        EditText from=new EditText(this),to=new EditText(this);from.setHint("از تاریخ");to.setHint("تا تاریخ");from.setSingleLine(true);to.setSingleLine(true);from.setOnClickListener(v->pickDate(from));to.setOnClickListener(v->pickDate(to));
        dates.addView(from,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));dates.addView(to,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));add(dates);
        Button show=btn("🔎 نمایش در بازه");add(show);TextView summary=tv("",14);add(summary);LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);add(list);
        Runnable render=()->{list.removeAllViews();int count=0;long total=0,remaining=0;long funded=0;JSONArray a=sortedPurchases();String f=from.getText().toString().trim(),t=to.getText().toString().trim();
            for(int i=0;i<a.length();i++){JSONObject p=a.optJSONObject(i);if(p==null||!filter.accept(p))continue;String d=p.optString("buyDate");if(!f.isEmpty()&&d.compareTo(f)<0)continue;if(!t.isEmpty()&&d.compareTo(t)>0)continue;count++;long amt=toLong(p.optString("amount"));total+=amt;long rem=fundingRemaining(p);remaining+=rem;funded+=totalFunded(p);String unit=p.optString("unit",p.optString("buyer")),no=p.optString("purchaseNo"),cert=p.optString("healthCertificate");Button b=btn("🏠 "+unit+" | خرید "+no+"\nگواهی: "+(cert.isEmpty()?"-":cert)+" | تاریخ: "+d+"\nمبلغ: "+AppData.fmt(p.optString("amount"))+" ریال");b.setOnClickListener(v->openPage(()->details(p)));list.addView(b);}
            if(count==0)add(tv(empty,15));
            if("funding".equals(mode))summary.setText("آمار کل بازه\nتعداد خریدهای نیازمند تأمین: "+count+"\nمجموع مبلغ خرید: "+AppData.fmt(""+total)+" ریال\nمجموع تأمین‌شده: "+AppData.fmt(""+funded)+" ریال\nمبلغی که باید تأمین شود: "+AppData.fmt(""+remaining)+" ریال");
            else if("collection".equals(mode))summary.setText("آمار کل بازه\nتعداد خریدهای وصول‌نشده: "+count+"\nمجموع مبلغی که باید وصول شود: "+AppData.fmt(""+total)+" ریال");
            else summary.setText("آمار کل بازه\nتعداد خریدهای تخصیص‌نشده: "+count+"\nمجموع مبلغ خریدهای تخصیص‌نشده: "+AppData.fmt(""+total)+" ریال");
        };
        show.setOnClickListener(v->render.run());render.run();Button back=btn("← بازگشت");back.setOnClickListener(v->back());add(back);finishScreen(title);
    }

    void certificateHome(){
        base("خرید بر اساس گواهی بهداشتی");
        LinearLayout dates=new LinearLayout(this);dates.setOrientation(LinearLayout.HORIZONTAL);EditText from=new EditText(this),to=new EditText(this);from.setHint("از تاریخ");to.setHint("تا تاریخ");from.setSingleLine(true);to.setSingleLine(true);from.setOnClickListener(v->pickDate(from));to.setOnClickListener(v->pickDate(to));dates.addView(from,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));dates.addView(to,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));add(dates);
        Button show=btn("🔎 نمایش گواهی‌ها در بازه");add(show);TextView summary=tv("",14);add(summary);LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);add(list);
        Runnable render=()->{list.removeAllViews();int certCount=0;double ic=0,pc=0,tc=0,rc=0,is=0,ps=0,ts=0,rs=0;HashSet<String> done=new HashSet<>();String f=from.getText().toString().trim(),t=to.getText().toString().trim();JSONArray a=sortedPurchases();
            for(int i=0;i<a.length();i++){JSONObject p=a.optJSONObject(i);if(p==null)continue;String unit=p.optString("unit",p.optString("buyer")).trim(),cert=p.optString("healthCertificate").trim();if(unit.isEmpty()||cert.isEmpty()||isShahedaneh(unit))continue;String d=p.optString("buyDate");if(!f.isEmpty()&&d.compareTo(f)<0)continue;if(!t.isEmpty()&&d.compareTo(t)>0)continue;String k=unit+"|"+cert;if(!done.add(k))continue;JSONObject cb=certificateBase(k,null);if(cb==null)continue;double cq=quotaOriginal(cb,"cornQuota"),sq=quotaOriginal(cb,"soyQuota"),uc=usedCorn(unit,cert,null),us=usedSoy(unit,cert,null),rcc=Math.max(0,cq-uc-transferAmount(unit,cert,"corn")),rss=Math.max(0,sq-us-transferAmount(unit,cert,"soy"));ic+=cq;pc+=uc;tc+=transferAmount(unit,cert,"corn");rc+=rcc;is+=sq;ps+=us;ts+=transferAmount(unit,cert,"soy");rs+=rss;certCount++;Button b=btn("🏠 "+unit+"\n📁 گواهی "+cert+"\nمانده ذرت: "+fmtDecimal(rcc)+" | مانده سویا: "+fmtDecimal(rss));b.setOnClickListener(v->openPage(()->certificateFile(unit,cert)));list.addView(b);}
            summary.setText("آمار کل گواهی‌ها در بازه\nتعداد گواهی: "+certCount+"\n"+inputWeightSummary(purchasesInRange(null,f,t))+"\nسهمیه اولیه ذرت: "+fmtDecimal(ic)+" کیلوگرم\nخرید شده ذرت: "+fmtDecimal(pc)+" کیلوگرم\nذرت انتقال داده: "+fmtDecimal(ic-pc-rc)+" کیلوگرم\nمانده سهمیه ذرت: "+fmtDecimal(rc)+" کیلوگرم\nسهمیه اولیه سویا: "+fmtDecimal(is)+" کیلوگرم\nخرید شده سویا: "+fmtDecimal(ps)+" کیلوگرم\nسویا انتقال داده: "+fmtDecimal(is-ps-rs)+" کیلوگرم\nمانده سهمیه سویا: "+fmtDecimal(rs)+" کیلوگرم");if(certCount==0)add(tv("هنوز گواهی بهداشتی در این بازه ثبت نشده است.",15));};
        show.setOnClickListener(v->render.run());render.run();Button back=btn("← بازگشت");back.setOnClickListener(v->back());add(back);finishScreen("خرید بر اساس گواهی بهداشتی");
    }

    String inputWeightSummary(List<JSONObject> purchases){
        LinkedHashMap<String,Double> sums=new LinkedHashMap<>();
        for(JSONObject p:purchases){if(p==null)continue;String c=p.optString("commodity").trim();if(c.isEmpty())c="نامشخص";double w=toDouble(p.optString("weight"));sums.put(c,sums.getOrDefault(c,0d)+w);}
        if(sums.isEmpty())return "نوع نهاده / وزن: موردی ثبت نشده است";
        StringBuilder b=new StringBuilder("نوع نهاده / وزن:\n");
        for(Map.Entry<String,Double> e:sums.entrySet())b.append("• ").append(e.getKey()).append(" : ").append(fmtDecimal(e.getValue())).append(" کیلوگرم\n");
        return b.toString().trim();
    }
    List<JSONObject> purchasesInRange(String unit,String from,String to){
        ArrayList<JSONObject> r=new ArrayList<>();JSONArray a=AppData.arr(data,"purchases");
        for(int i=0;i<a.length();i++){JSONObject p=a.optJSONObject(i);if(p==null)continue;String u=p.optString("unit",p.optString("buyer")).trim();if(unit!=null&&!unit.equals(u))continue;String d=p.optString("buyDate").trim();if(from!=null&&!from.isEmpty()&&!d.isEmpty()&&d.compareTo(from)<0)continue;if(to!=null&&!to.isEmpty()&&!d.isEmpty()&&d.compareTo(to)>0)continue;r.add(p);}
        return r;
    }
    List<JSONObject> purchasesInRangeForCertificate(String unit,String cert){
        ArrayList<JSONObject> r=new ArrayList<>();JSONArray a=AppData.arr(data,"purchases");
        for(int i=0;i<a.length();i++){JSONObject p=a.optJSONObject(i);if(p!=null&&unit.equals(p.optString("unit",p.optString("buyer")))&&cert.equals(p.optString("healthCertificate")))r.add(p);}
        return r;
    }
    JSONArray quotaTransfers(){return AppData.arr(data,"quotaTransfers");}
    String transferCommodityName(String c){return "corn".equals(c)?"ذرت":"سویا";}
    double transferAmount(String unit,String cert,String commodity){
        double s=0;JSONArray a=quotaTransfers();
        for(int i=0;i<a.length();i++){
            JSONObject x=a.optJSONObject(i);if(x==null)continue;
            if(unit.equals(x.optString("unit"))&&cert.equals(x.optString("healthCertificate"))&&commodity.equals(x.optString("commodity")))s+=toDouble(x.optString("amount"));
        }
        return s;
    }
    ArrayList<JSONObject> transferLots(String unit,String cert,String commodity){
        ArrayList<JSONObject> r=new ArrayList<>();JSONArray a=quotaTransfers();
        for(int i=0;i<a.length();i++){JSONObject x=a.optJSONObject(i);if(x==null)continue;if(unit.equals(x.optString("unit"))&&cert.equals(x.optString("healthCertificate"))&&commodity.equals(x.optString("commodity")))r.add(x);}
        Collections.sort(r,(x,y)->x.optString("date").compareTo(y.optString("date")));
        return r;
    }
    double transferConsumedForPurchase(String unit,String cert,String commodity,JSONObject purchase){
        if(purchase==null||!isShahedaneh(purchase.optString("unit",purchase.optString("buyer"))))return 0;
        if(!unit.equals(purchase.optString("transferSourceUnit"))||!cert.equals(purchase.optString("healthCertificate")))return 0;
        double need=toDouble(purchase.optString(commodity));if(need<=0)return 0;
        ArrayList<JSONObject> lots=transferLots(unit,cert,commodity);if(lots.isEmpty())return 0;
        JSONArray a=AppData.arr(data,"purchases");ArrayList<JSONObject> purchases=new ArrayList<>();
        for(int i=0;i<a.length();i++){JSONObject x=a.optJSONObject(i);if(x==null)continue;if(isShahedaneh(x.optString("unit",x.optString("buyer")))&&unit.equals(x.optString("transferSourceUnit"))&&cert.equals(x.optString("healthCertificate"))&&toDouble(x.optString(commodity))>0)purchases.add(x);}
        Collections.sort(purchases,(x,y)->x.optString("buyDate").compareTo(y.optString("buyDate")));
        HashMap<String,Double> remaining=new HashMap<>();for(JSONObject l:lots)remaining.put(l.optString("id"),toDouble(l.optString("amount")));
        for(JSONObject q:purchases){if(q==purchase)break;double qneed=toDouble(q.optString(commodity));for(JSONObject l:lots){String id=l.optString("id");double lr=remaining.getOrDefault(id,0d);if(lr<=0)continue;if(q.optString("buyDate").compareTo(l.optString("date"))<0)continue;double take=Math.min(lr,qneed);remaining.put(id,lr-take);qneed-=take;if(qneed<=0.0001)break;}}
        double left=need,result=0;for(JSONObject l:lots){String id=l.optString("id");double lr=remaining.getOrDefault(id,0d);if(lr<=0)continue;if(purchase.optString("buyDate").compareTo(l.optString("date"))<0)continue;double take=Math.min(lr,left);left-=take;result+=take;if(left<=0.0001)break;}return result;
    }
    double transferConsumedTotal(String unit,String cert,String commodity){
        double s=0;JSONArray a=AppData.arr(data,"purchases");ArrayList<JSONObject> ps=new ArrayList<>();
        for(int i=0;i<a.length();i++){JSONObject p=a.optJSONObject(i);if(p!=null&&unit.equals(p.optString("unit",p.optString("buyer")))&&cert.equals(p.optString("healthCertificate")))ps.add(p);}
        Collections.sort(ps,(x,y)->x.optString("buyDate").compareTo(y.optString("buyDate")));
        for(JSONObject p:ps)s+=transferConsumedForPurchase(unit,cert,commodity,p);
        return s;
    }
    double transferRemaining(String unit,String cert,String commodity){return Math.max(0,transferAmount(unit,cert,commodity)-transferConsumedTotal(unit,cert,commodity));}
    double transferableRemaining(String unit,String cert,String commodity){
        JSONObject base=certificateBase(unit+"|"+cert,null);if(base==null)return 0;
        double original=quotaOriginal(base,"corn".equals(commodity)?"cornQuota":"soyQuota");
        double used="corn".equals(commodity)?usedCorn(unit,cert,null):usedSoy(unit,cert,null);
        return Math.max(0,original-used-transferAmount(unit,cert,commodity));
    }
    String transferPurchaseTrace(String unit,String cert,String commodity){
        ArrayList<JSONObject> lots=transferLots(unit,cert,commodity);if(lots.isEmpty())return "هنوز انتقال سهمیه‌ای برای این نهاده ثبت نشده است.";
        StringBuilder out=new StringBuilder("مسیر انتقال و مصرف "+transferCommodityName(commodity)+":\n");
        double total=0;
        for(JSONObject l:lots){double amt=toDouble(l.optString("amount")),rem=transferRemainingForLot(unit,cert,commodity,l.optString("id"));double used=Math.max(0,amt-rem);total+=used;out.append("• ").append(fmtDecimal(amt)).append(" کیلوگرم در تاریخ ").append(l.optString("date")).append(" از این گواهی به شاهدانه منتقل شد");if(used>0)out.append(" | مصرف‌شده در خریدهای بعدی: ").append(fmtDecimal(used));out.append(" | مانده: ").append(fmtDecimal(rem)).append("\n");}
        if(total<=0)out.append("هنوز از سهمیه منتقل‌شده در خرید بعدی مصرفی ثبت نشده است.\n");
        return out.toString().trim();
    }
    double transferRemainingForLot(String unit,String cert,String commodity,String lotId){
        ArrayList<JSONObject> lots=transferLots(unit,cert,commodity);double targetAmount=0;boolean found=false;
        for(JSONObject l:lots)if(lotId.equals(l.optString("id"))){targetAmount=toDouble(l.optString("amount"));found=true;break;}
        if(!found)return 0;
        HashMap<String,Double> rems=new HashMap<>();for(JSONObject l:lots)rems.put(l.optString("id"),toDouble(l.optString("amount")));
        JSONArray a=AppData.arr(data,"purchases");ArrayList<JSONObject> ps=new ArrayList<>();
        for(int i=0;i<a.length();i++){JSONObject q=a.optJSONObject(i);if(q!=null&&isShahedaneh(q.optString("unit",q.optString("buyer")))&&unit.equals(q.optString("transferSourceUnit"))&&cert.equals(q.optString("healthCertificate"))&&toDouble(q.optString(commodity))>0)ps.add(q);}
        Collections.sort(ps,(x,y)->x.optString("buyDate").compareTo(y.optString("buyDate")));
        for(JSONObject q:ps){double need=toDouble(q.optString(commodity));if(need<=0)continue;for(JSONObject l:lots){String id=l.optString("id");double rem=rems.getOrDefault(id,0d);if(rem<=0)continue;if(q.optString("buyDate").compareTo(l.optString("date"))<0)continue;double take=Math.min(rem,need);rems.put(id,rem-take);need-=take;if(need<=0.0001)break;}}
        return Math.max(0,rems.getOrDefault(lotId,targetAmount));
    }
    void quotaTransferPlaceholder(){
        base("انتقال مانده سهمیه به شاهدانه");
        LinearLayout dates=new LinearLayout(this);dates.setOrientation(LinearLayout.HORIZONTAL);EditText from=new EditText(this),to=new EditText(this);from.setHint("از تاریخ");to.setHint("تا تاریخ");from.setSingleLine(true);to.setSingleLine(true);from.setOnClickListener(v->pickDate(from));to.setOnClickListener(v->pickDate(to));dates.addView(from,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));dates.addView(to,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));add(dates);
        Button marlik=btn("🏢 شاهدانه طیور مارلیک");marlik.setOnClickListener(v->openPage(()->shahedanehFile(from.getText().toString().trim(),to.getText().toString().trim())));add(marlik);
        Button show=btn("🔎 نمایش گواهی‌های دارای مانده");add(show);TextView summary=tv("",14);add(summary);LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);add(list);
        Runnable render=()->{list.removeAllViews();LinkedHashSet<String> keys=new LinkedHashSet<>();double tc=0,ts=0;String f=from.getText().toString().trim(),t=to.getText().toString().trim();JSONArray a=sortedPurchases();
            for(int i=0;i<a.length();i++){JSONObject p=a.optJSONObject(i);if(p==null)continue;String unit=p.optString("unit",p.optString("buyer")).trim(),cert=p.optString("healthCertificate").trim(),d=p.optString("buyDate").trim();if(unit.isEmpty()||cert.isEmpty()||isShahedaneh(unit))continue;if(!f.isEmpty()&&!d.isEmpty()&&d.compareTo(f)<0)continue;if(!t.isEmpty()&&!d.isEmpty()&&d.compareTo(t)>0)continue;keys.add(unit+"|"+cert);}
            for(String key:keys){String[] z=key.split("\\|",2);if(z.length!=2)continue;String unit=z[0],cert=z[1];double rc=transferableRemaining(unit,cert,"corn"),rs=transferableRemaining(unit,cert,"soy");if(rc<=0.0001&&rs<=0.0001)continue;Button b=btn("🏠 "+unit+"\n📁 گواهی بهداشتی "+cert+"\nمانده ذرت: "+fmtDecimal(rc)+" | مانده سویا: "+fmtDecimal(rs)+" کیلوگرم");b.setOnClickListener(v->openPage(()->transferCertificateFile(unit,cert,f,t)));list.addView(b);}
            JSONArray tr=quotaTransfers();for(int i=0;i<tr.length();i++){JSONObject x=tr.optJSONObject(i);if(x==null)continue;String d=x.optString("date").trim();if(!f.isEmpty()&&d.compareTo(f)<0)continue;if(!t.isEmpty()&&d.compareTo(t)>0)continue;if("corn".equals(x.optString("commodity")))tc+=toDouble(x.optString("amount"));else if("soy".equals(x.optString("commodity")))ts+=toDouble(x.optString("amount"));}
            summary.setText("آمار انتقال در بازه\nذرت انتقال داده شده: "+fmtDecimal(tc)+" کیلوگرم\nسویا انتقال داده شده: "+fmtDecimal(ts)+" کیلوگرم");if(list.getChildCount()==0)list.addView(tv("هیچ گواهی با مانده قابل انتقال وجود ندارد.",15));};
        show.setOnClickListener(v->render.run());render.run();Button back=btn("← بازگشت");back.setOnClickListener(v->back());add(back);finishScreen("انتقال مانده سهمیه به شاهدانه");
    }

    void shahedanehFile(String from,String to){
        base("شاهدانه طیور مارلیک");String f=from==null?"":from.trim(),t=to==null?"":to.trim();JSONArray tr=quotaTransfers(),purchases=AppData.arr(data,"purchases");double cornTransfer=0,soyTransfer=0,cornPurchased=0,soyPurchased=0;int transferCount=0,purchaseCount=0;StringBuilder out=new StringBuilder("سوابق انتقال سهمیه به شاهدانه\n\n");
        for(int i=0;i<tr.length();i++){JSONObject x=tr.optJSONObject(i);if(x==null)continue;String d=x.optString("date").trim();if(!f.isEmpty()&&d.compareTo(f)<0)continue;if(!t.isEmpty()&&d.compareTo(t)>0)continue;double a=toDouble(x.optString("amount"));if("corn".equals(x.optString("commodity"))){cornTransfer+=a;out.append("🌽 ذرت: ").append(fmtDecimal(a));}else if("soy".equals(x.optString("commodity"))){soyTransfer+=a;out.append("🌱 سویا: ").append(fmtDecimal(a));}else continue;transferCount++;out.append(" کیلوگرم | از واحد: ").append(x.optString("unit","-")).append(" | گواهی: ").append(x.optString("healthCertificate","-")).append(" | تاریخ: ").append(d).append("\n");}
        out.append("سوابق خرید با سهمیه منتقل‌شده\n");for(int i=0;i<purchases.length();i++){JSONObject p=purchases.optJSONObject(i);if(p==null||!isShahedaneh(p.optString("unit",p.optString("buyer"))))continue;String d=p.optString("buyDate").trim();if(!f.isEmpty()&&d.compareTo(f)<0)continue;if(!t.isEmpty()&&d.compareTo(t)>0)continue;double c=toDouble(p.optString("corn")),s=toDouble(p.optString("soy"));if(c<=0&&s<=0)continue;cornPurchased+=c;soyPurchased+=s;purchaseCount++;out.append("📄 خرید ").append(p.optString("purchaseNo","-")).append(" | تاریخ: ").append(d).append(" | واحد مبدأ: ").append(p.optString("transferSourceUnit","-")).append(" | گواهی مبدأ: ").append(p.optString("transferSourceCertificate",p.optString("healthCertificate","-"))).append("\nذرت مصرفی: ").append(fmtDecimal(c)).append(" | سویا مصرفی: ").append(fmtDecimal(s)).append(" کیلوگرم\n");}
        if(purchaseCount==0)out.append("هنوز خریدی با سهمیه منتقل‌شده ثبت نشده است.\n");out.append("\nنوع نهاده / وزن در خریدهای مارلیک:\n").append(inputWeightSummary(purchasesInRange("شاهدانه طیور مارلیک",f,t)));out.append("\n\nآمار شاهدانه طیور مارلیک در بازه\nتعداد انتقال: ").append(transferCount).append("\nذرت انتقال داده شده: ").append(fmtDecimal(cornTransfer)).append(" کیلوگرم").append("\nسویا انتقال داده شده: ").append(fmtDecimal(soyTransfer)).append(" کیلوگرم").append("\nذرت مصرف‌شده از سهمیه انتقالی: ").append(fmtDecimal(cornPurchased)).append(" کیلوگرم").append("\nسویا مصرف‌شده از سهمیه انتقالی: ").append(fmtDecimal(soyPurchased)).append(" کیلوگرم").append("\nمانده سهمیه انتقالی ذرت: ").append(fmtDecimal(Math.max(0,cornTransfer-cornPurchased))).append(" کیلوگرم").append("\nمانده سهمیه انتقالی سویا: ").append(fmtDecimal(Math.max(0,soyTransfer-soyPurchased))).append(" کیلوگرم");
        add(tv(out.toString(),15));Button back=btn("← بازگشت");back.setOnClickListener(v->back());add(back);finishScreen("شاهدانه طیور مارلیک");
    }

    void transferCertificateFile(String unit,String cert,String from,String to){
        base("گواهی انتقال: "+cert);
        JSONObject cb=certificateBase(unit+"|"+cert,null);
        double cq=cb==null?0:quotaOriginal(cb,"cornQuota"),sq=cb==null?0:quotaOriginal(cb,"soyQuota");
        double uc=usedCorn(unit,cert,null),us=usedSoy(unit,cert,null),tc=transferAmount(unit,cert,"corn"),ts=transferAmount(unit,cert,"soy");
        double rc=Math.max(0,cq-uc-tc),rs=Math.max(0,sq-us-ts);
        JSONArray tr=quotaTransfers();StringBuilder out=new StringBuilder();
        out.append("واحد مبدأ: ").append(unit).append("\nگواهی بهداشتی: ").append(cert).append("\n\nسابقه انتقال‌ها:\n");
        for(int i=0;i<tr.length();i++){JSONObject x=tr.optJSONObject(i);if(x==null||!unit.equals(x.optString("unit"))||!cert.equals(x.optString("healthCertificate")))continue;String d=x.optString("date");if(!from.isEmpty()&&d.compareTo(from)<0)continue;if(!to.isEmpty()&&d.compareTo(to)>0)continue;double a=toDouble(x.optString("amount"));if("corn".equals(x.optString("commodity")))out.append("ذرت: ").append(fmtDecimal(a));else if("soy".equals(x.optString("commodity")))out.append("سویا: ").append(fmtDecimal(a));else continue;out.append(" کیلوگرم | تاریخ: ").append(d).append("\n");}
        out.append("\nنوع نهاده / وزن:\n").append(inputWeightSummary(purchasesInRangeForCertificate(unit,cert)));
        out.append("\nسهمیه اولیه ذرت: ").append(fmtDecimal(cq)).append(" کیلوگرم").append("\nذرت خرید شده: ").append(fmtDecimal(uc)).append(" کیلوگرم").append("\nذرت انتقال داده: ").append(fmtDecimal(tc)).append(" کیلوگرم").append("\nمانده سهمیه ذرت: ").append(fmtDecimal(rc)).append(" کیلوگرم").append("\nسهمیه اولیه سویا: ").append(fmtDecimal(sq)).append(" کیلوگرم").append("\nسویا خرید شده: ").append(fmtDecimal(us)).append(" کیلوگرم").append("\nسویا انتقال داده: ").append(fmtDecimal(ts)).append(" کیلوگرم").append("\nمانده سهمیه سویا: ").append(fmtDecimal(rs)).append(" کیلوگرم");
        add(tv(out.toString(),15));
        if(rc>0.0001){Button b=btn("⬆ انتقال مانده ذرت به شاهدانه");b.setOnClickListener(v->transferDialog(unit,cert,"corn",rc));add(b);}
        if(rs>0.0001){Button b=btn("⬆ انتقال مانده سویا به شاهدانه");b.setOnClickListener(v->transferDialog(unit,cert,"soy",rs));add(b);}
        Button back=btn("← بازگشت");back.setOnClickListener(v->back());add(back);finishScreen("پرونده گواهی انتقال");
    }

    void transferDialog(String unit,String cert,String commodity,double max){
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);EditText amount=new EditText(this);amount.setHint("مقدار انتقال به کیلوگرم");amount.setInputType(2|8192);box.addView(amount);EditText date=new EditText(this);date.setHint("تاریخ انتقال");date.setSingleLine(true);date.setFocusable(false);date.setOnClickListener(v->pickDate(date));date.setText(PersianDate.today());box.addView(date);
        AlertDialog dlg=new AlertDialog.Builder(this).setTitle("انتقال "+transferCommodityName(commodity)+" به شاهدانه").setMessage("واحد: "+unit+"\nگواهی: "+cert+"\nحداکثر قابل انتقال: "+fmtDecimal(max)+" کیلوگرم").setView(box).setNegativeButton("انصراف",null).setPositiveButton("انتقال",null).create();
        dlg.setOnShowListener(x->dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{double n=toDouble(amount.getText().toString());String d=date.getText().toString().trim();if(n<=0||n>max+0.0001||d.isEmpty()){Toast.makeText(this,"مقدار انتقال معتبر نیست.",Toast.LENGTH_LONG).show();return;}try{JSONObject r=new JSONObject();r.put("id",UUID.randomUUID().toString());r.put("unit",unit);r.put("healthCertificate",cert);r.put("commodity",commodity);r.put("amount",fmtDecimal(n));r.put("date",d);r.put("createdAt",System.currentTimeMillis());quotaTransfers().put(r);AppData.save(this,data);dlg.dismiss();Toast.makeText(this,"انتقال سهمیه با موفقیت ثبت شد.",Toast.LENGTH_LONG).show();quotaTransferPlaceholder();}catch(Exception e){Toast.makeText(this,"خطا در ثبت انتقال.",Toast.LENGTH_LONG).show();}}));dlg.show();
    }

    void certificateIndex(String unit){
        base("خرید بر اساس گواهی بهداشتی: "+unit);
        JSONArray a=AppData.arr(data,"purchases");LinkedHashMap<String,JSONObject> certs=new LinkedHashMap<>();
        for(int i=0;i<a.length();i++){
            JSONObject p=a.optJSONObject(i);if(p==null)continue;
            if(!unit.equals(p.optString("unit",p.optString("buyer"))))continue;
            String cert=p.optString("healthCertificate").trim();if(cert.isEmpty())continue;
            if(!certs.containsKey(cert))certs.put(cert,p);
        }
        if(certs.isEmpty())add(tv("هنوز گواهی بهداشتی برای این واحد ثبت نشده است.",15));
        for(String cert:certs.keySet()){
            JSONObject cb=certs.get(cert);double cq=quotaOriginal(cb,"cornQuota"),sq=quotaOriginal(cb,"soyQuota");
            double uc=usedCorn(unit,cert,null),us=usedSoy(unit,cert,null);
            double rc=Math.max(0,cq-uc-transferAmount(unit,cert,"corn")),rs=Math.max(0,sq-us-transferAmount(unit,cert,"soy"));
            Button b=btn("📁 گواهی "+cert+
                    "\nسهمیه ذرت: "+fmtDecimal(cq)+" | مانده: "+fmtDecimal(rc)+
                    "\nسهمیه سویا: "+fmtDecimal(sq)+" | مانده: "+fmtDecimal(rs)+
                    "\n"+((rc<=0.0001&&rs<=0.0001)?"✅ تکمیل شده":"⏳ مانده دارد"));
            b.setOnClickListener(v->openPage(()->certificateFile(unit,cert)));add(b);
        }
        Button back=btn("← بازگشت");back.setOnClickListener(v->back());add(back);finishScreen("خرید بر اساس گواهی بهداشتی");
    }

    void certificateFile(String unit,String cert){
        base("گواهی بهداشتی: "+cert);
        JSONObject cb=certificateBase(unit+"|"+cert,null);
        if(cb==null){add(tv("پرونده این گواهی پیدا نشد.",15));Button b=btn("← بازگشت");b.setOnClickListener(v->back());add(b);finishScreen("پرونده گواهی");return;}
        double cq=quotaOriginal(cb,"cornQuota"),sq=quotaOriginal(cb,"soyQuota");
        double uc=usedCorn(unit,cert,null),us=usedSoy(unit,cert,null);
        double rc=Math.max(0,cq-uc-transferAmount(unit,cert,"corn")),rs=Math.max(0,sq-us-transferAmount(unit,cert,"soy"));
        add(tv("اطلاعات گواهی\nشماره گواهی: "+cert+
                "\nتعداد جوجه‌ریزی: "+cb.optString("chickCount")+
                "\nتاریخ جوجه‌ریزی: "+cb.optString("placementDate")+
                "\nتاریخ اعتبار: "+cb.optString("quotaExpiry"),15));
        add(tv(inputWeightSummary(purchasesInRangeForCertificate(unit,cert)),15));
        double tc=transferAmount(unit,cert,"corn"),ts=transferAmount(unit,cert,"soy"); double tcr=transferRemaining(unit,cert,"corn"),tsr=transferRemaining(unit,cert,"soy");
        add(tv("وضعیت سهمیه\nذرت — اولیه: "+fmtDecimal(cq)+" | مصرف‌شده: "+fmtDecimal(uc)+" | مانده: "+fmtDecimal(rc)+
                "\nذرت منتقل‌شده به شاهدانه: "+fmtDecimal(tc)+" کیلوگرم | مصرف‌شده از انتقال: "+fmtDecimal(Math.max(0,tc-tcr))+" | مانده انتقال: "+fmtDecimal(tcr)+
                "\nسویا — اولیه: "+fmtDecimal(sq)+" | مصرف‌شده: "+fmtDecimal(us)+" | مانده: "+fmtDecimal(rs)+
                "\nسویا منتقل‌شده به شاهدانه: "+fmtDecimal(ts)+" کیلوگرم | مصرف‌شده از انتقال: "+fmtDecimal(Math.max(0,ts-tsr))+" | مانده انتقال: "+fmtDecimal(tsr)+
                "\n"+(rc<=0.0001&&rs<=0.0001?"✅ تکمیل خرید سهمیه":"⏳ سهمیه هنوز تکمیل نشده است"),15));

        Button transferCorn=btn("🌽 انتقال ذرت");
        transferCorn.setOnClickListener(v->transferDialog(unit,cert,"corn",transferableRemaining(unit,cert,"corn")));
        add(transferCorn);
        Button transferSoy=btn("🌱 انتقال سویا");
        transferSoy.setOnClickListener(v->transferDialog(unit,cert,"soy",transferableRemaining(unit,cert,"soy")));
        add(transferSoy);

        add(tv("خریدهای انجام‌شده با این گواهی",16));
        JSONArray a=sortedPurchases();int count=0;
        for(int i=0;i<a.length();i++){
            JSONObject p=a.optJSONObject(i);if(p==null)continue;
            if(!unit.equals(p.optString("unit",p.optString("buyer"))))continue;
            if(!cert.equals(p.optString("healthCertificate").trim()))continue;
            count++;
            Button b=btn("📄 خرید "+p.optString("purchaseNo")+
                    "\nتاریخ: "+p.optString("buyDate")+
                    " | وزن: "+p.optString("weight")+" کیلوگرم"+
                    "\nذرت مصرفی: "+p.optString("corn")+" | سویا مصرفی: "+p.optString("soy"));
            b.setOnClickListener(v->openPage(()->details(p)));add(b);
        }
        if(count==0)add(tv("خریدی ثبت نشده است.",14));
        Button back=btn("← بازگشت");back.setOnClickListener(v->back());add(back);finishScreen("پرونده گواهی");
    }

    long toLong(String s){try{return Long.parseLong(AppData.digits(s));}catch(Exception e){return 0;}}

    void addPurchaseNumberList(ArrayList<JSONObject> list){
        if(list==null||list.isEmpty()){ add(tv("موردی وجود ندارد",14)); return; }
        for(JSONObject obj:list){
            if(obj==null) continue;
            String no=obj.optString("purchaseNo","").trim();
            if(!no.isEmpty()){
                Button b=btn("• خرید "+no);
                b.setOnClickListener(v->openPage(()->details(obj)));
                add(b);
            }
        }
    }

    void buyerStats(String buyer){
        base("آمار بازه‌ای: "+buyer);
        EditText f=input("از تاریخ"),t=input("تا تاریخ");
        f.setOnClickListener(v->pickDate(f));t.setOnClickListener(v->pickDate(t));
        Button b=btn("نمایش آمار");add(b);TextView out=tv("",15);add(out);
        b.setOnClickListener(v->{
            int n=0,c=0,a=0,ff=0,qf=0;long total=0,funded=0,unfunded=0;
            double initialCorn=0,purchasedCorn=0,remainingCorn=0,transferredCorn=0,initialSoy=0,purchasedSoy=0,remainingSoy=0,transferredSoy=0;
            HashSet<String> certKeys=new HashSet<>();
            JSONArray p=AppData.arr(data,"purchases");
            for(int i=0;i<p.length();i++){
                JSONObject x=p.optJSONObject(i);if(x==null||!buyer.equals(x.optString("unit",x.optString("buyer"))))continue;
                String d=x.optString("buyDate");
                if(!f.getText().toString().isEmpty()&&d.compareTo(f.getText().toString())<0)continue;
                if(!t.getText().toString().isEmpty()&&d.compareTo(t.getText().toString())>0)continue;
                n++;total+=toLong(x.optString("amount"));
                if(x.optBoolean("collected"))c++;
                if(x.optBoolean("allocated"))a++;
                long rem=fundingRemaining(x);funded+=totalFunded(x);unfunded+=rem;if(rem<=0)ff++;
                String cert=x.optString("healthCertificate").trim();
                if(!cert.isEmpty()){
                    String ck=buyer+"|"+cert;
                    if(certKeys.add(ck)){
                        JSONObject cb=certificateBase(ck,null);
                        if(cb!=null){
                            double cq=quotaOriginal(cb,"cornQuota"),sq=quotaOriginal(cb,"soyQuota");
                            double remC=Math.max(0,cq-usedCorn(buyer,cert,null)-transferAmount(buyer,cert,"corn"));
                            double remS=Math.max(0,sq-usedSoy(buyer,cert,null)-transferAmount(buyer,cert,"soy"));
                            initialCorn+=cq; purchasedCorn+=usedCorn(buyer,cert,null); remainingCorn+=remC; initialSoy+=sq; purchasedSoy+=usedSoy(buyer,cert,null); remainingSoy+=remS; transferredCorn+=transferAmount(buyer,cert,"corn"); transferredSoy+=transferAmount(buyer,cert,"soy");
                            if(isCertificateComplete(buyer,cert))qf++;
                        }
                    }
                }
            }
            out.setText("تعداد خرید: "+n+
                    "\nمجموع مبلغ: "+AppData.fmt(""+total)+" ریال"+
                    "\nوصول: "+c+" خرید"+
                    "\nتخصیص: "+a+" خرید"+
                    "\nتأمین: "+ff+" خرید کامل"+
                    "\nسهمیه: "+qf+" گواهی کامل"+
                    "\nمجموع تأمین‌شده: "+AppData.fmt(""+funded)+" ریال"+
                    "\nمجموع تأمین‌نشده: "+AppData.fmt(""+unfunded)+" ریال"+
                    "\n"+inputWeightSummary(purchasesInRange(buyer,f.getText().toString(),t.getText().toString()))+
                    "\nسهمیه اولیه ذرت: "+fmtDecimal(initialCorn)+" کیلوگرم"+
                    "\nخرید شده ذرت: "+fmtDecimal(purchasedCorn)+" کیلوگرم"+
                    "\nذرت انتقال داده: "+fmtDecimal(transferredCorn)+" کیلوگرم"+
                    "\nمانده سهمیه ذرت: "+fmtDecimal(remainingCorn)+" کیلوگرم"+
                    "\nسهمیه اولیه سویا: "+fmtDecimal(initialSoy)+" کیلوگرم"+
                    "\nخرید شده سویا: "+fmtDecimal(purchasedSoy)+" کیلوگرم"+
                    "\nمانده سهمیه سویا: "+fmtDecimal(remainingSoy)+" کیلوگرم"+
                    "\nسویا انتقال داده: "+fmtDecimal(transferredSoy)+" کیلوگرم");
        });
        Button back=btn("← بازگشت");back.setOnClickListener(v->back());add(back);finishScreen("آمار");
    }

    void deadlines(){
        base("سررسیدها");add(tv("حالت نمایش",16));Spinner mode=spinner(new String[]{"نزدیک‌ترین سررسیدها","بازه تاریخی"});add(mode);
        LinearLayout dates=new LinearLayout(this);EditText f=new EditText(this),t=new EditText(this);f.setHint("از تاریخ");t.setHint("تا تاریخ");f.setOnClickListener(v->pickDate(f));t.setOnClickListener(v->pickDate(t));dates.addView(f,new LinearLayout.LayoutParams(0,60,1));dates.addView(t,new LinearLayout.LayoutParams(0,60,1));add(dates);Button show=btn("🔎 نمایش");add(show);LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);add(list);
        Runnable render=()->{list.removeAllViews();ArrayList<JSONObject> l=new ArrayList<>();JSONArray a=AppData.arr(data,"purchases");for(int i=0;i<a.length();i++){JSONObject p=a.optJSONObject(i);if(p==null||p.optBoolean("collected")||p.optString("mainDue").isEmpty())continue;if(mode.getSelectedItemPosition()==1){if(!f.getText().toString().isEmpty()&&p.optString("mainDue").compareTo(f.getText().toString())<0)continue;if(!t.getText().toString().isEmpty()&&p.optString("mainDue").compareTo(t.getText().toString())>0)continue;}l.add(p);}Collections.sort(l,(x,y)->x.optString("mainDue").compareTo(y.optString("mainDue")));for(JSONObject p:l){Button b=btn("🏠 "+p.optString("unit",p.optString("buyer"))+" | خرید "+p.optString("purchaseNo")+"\nسررسید: "+p.optString("mainDue")+(isSoon(p)?"  ⚠ نزدیک":""));b.setOnClickListener(v->openPage(()->details(p)));list.addView(b);}};show.setOnClickListener(v->render.run());render.run();Button back=btn("← بازگشت");back.setOnClickListener(v->back());add(back);finishScreen("سررسیدها");
    }
    boolean isSoon(JSONObject p){String d=p==null?"":p.optString("mainDue");if(d.isEmpty()||p.optBoolean("collected",false))return false;long m=PersianDate.millis(d,"23:59");return m>=System.currentTimeMillis()&&m-System.currentTimeMillis()<7L*86400000L;}

    void accountsPage(){
        base("حساب‌ها");
        add(tv("حساب‌های ثبت‌شده برای استفاده سریع در تأمین موجودی",15));
        Button src=btn("🏦 حساب‌های مبدا");
        src.setOnClickListener(v->openPage(()->manageAccounts("sourceAccounts","حساب‌های مبدا")));
        add(src);
        Button dst=btn("🏦 حساب‌های مقصد");
        dst.setOnClickListener(v->openPage(()->manageAccounts("destinationAccounts","حساب‌های مقصد")));
        add(dst);
        Button back=btn("← بازگشت");back.setOnClickListener(v->back());add(back);
        finishScreen("حساب‌ها");
    }

    void manageAccounts(String key,String title){
        base(title);
        JSONArray a=AppData.arr(data,key);
        for(int i=0;i<a.length();i++){
            JSONObject acc=a.optJSONObject(i); if(acc==null)continue;
            LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);
            TextView info=tv(acc.optString("number")+"\n"+acc.optString("bank")+" | "+acc.optString("owner"),14);
            row.addView(info,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,0.78f));
            Button del=btn("🗑 حذف");
            final int ix=i;
            del.setOnClickListener(v->{a.remove(ix);try{data.put(key,a);AppData.save(this,data);}catch(Exception ignored){}manageAccounts(key,title);});
            row.addView(del,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,0.22f));
            add(row);
        }
        Button add=btn("➕ افزودن حساب");
        add.setOnClickListener(v->addAccount(key,title));
        add(add);
        Button back=btn("← بازگشت");back.setOnClickListener(v->back());add(back);
        finishScreen(title);
    }

    void addAccount(String key,String title){
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);
        EditText bank=new EditText(this);bank.setHint("نام بانک");box.addView(bank);
        EditText number=new EditText(this);number.setHint("شماره حساب");number.setInputType(2);box.addView(number);
        EditText owner=new EditText(this);owner.setHint("صاحب حساب");box.addView(owner);
        new AlertDialog.Builder(this).setTitle("افزودن حساب").setView(box)
            .setPositiveButton("ذخیره",(d,w)->{
                try{
                    String bn=bank.getText().toString().trim(),no=number.getText().toString().trim(),ow=owner.getText().toString().trim();
                    if(bn.isEmpty()||no.isEmpty()||ow.isEmpty()){Toast.makeText(this,"هر سه مورد حساب الزامی است.",Toast.LENGTH_LONG).show();return;}
                    JSONArray a=AppData.arr(data,key);
                    for(int i=0;i<a.length();i++)if(no.equals(a.optJSONObject(i).optString("number"))){Toast.makeText(this,"این شماره حساب قبلاً ثبت شده است.",Toast.LENGTH_LONG).show();return;}
                    JSONObject acc=new JSONObject();acc.put("bank",bn);acc.put("number",no);acc.put("owner",ow);a.put(acc);data.put(key,a);AppData.save(this,data);manageAccounts(key,title);
                }catch(Exception e){Toast.makeText(this,"ثبت حساب انجام نشد.",Toast.LENGTH_LONG).show();}
            }).setNegativeButton("لغو",null).show();
    }

    Spinner accountSpinner(String hint,String key){
        JSONArray a=AppData.arr(data,key);
        ArrayList<String> nums=new ArrayList<>();nums.add("");
        for(int i=0;i<a.length();i++){JSONObject x=a.optJSONObject(i);if(x!=null)nums.add(x.optString("number"));}
        Spinner sp=new Spinner(this);
        ArrayAdapter<String> ad=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,nums){
            @Override public View getView(int position,View convertView,ViewGroup parent){
                TextView v=(TextView)super.getView(position,convertView,parent);
                v.setText(position==0?hint:String.valueOf(getItem(position)));
                v.setTextSize(UiManager.fieldSize(MainActivity.this));
                v.setTextColor(position==0?UiManager.textSecondary(MainActivity.this):UiManager.text(MainActivity.this));
                v.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
                v.setPadding(UiManager.dp(MainActivity.this,12),UiManager.dp(MainActivity.this,8),UiManager.dp(MainActivity.this,12),UiManager.dp(MainActivity.this,8));
                return v;
            }
        };
        sp.setAdapter(ad);
        return sp;
    }
    String selectedAccountNumber(Spinner sp){return sp==null||sp.getSelectedItem()==null?"":String.valueOf(sp.getSelectedItem());}

    void manage(String key,String title){base(title);JSONArray a=AppData.arr(data,key);for(int i=0;i<a.length();i++){final int ix=i;LinearLayout r=new LinearLayout(this);TextView t=tv(a.optString(i),16);r.addView(t,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,0.72f));Button d=btn("حذف");d.setOnClickListener(v->{a.remove(ix);try{data.put(key,a);AppData.save(this,data);}catch(Exception ignored){}manage(key,title);});r.addView(d,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,0.28f));add(r);}Button ad=btn("➕ افزودن");ad.setOnClickListener(v->addEntry(key,"مورد جدید",()->manage(key,title)));add(ad);Button back=btn("← بازگشت");back.setOnClickListener(v->back());add(back);finishScreen(title);}
    void addEntry(String key,String title,Runnable after){EditText e=new EditText(this);new AlertDialog.Builder(this).setTitle(title).setView(e).setPositiveButton("ذخیره",(d,w)->{try{String v=e.getText().toString().trim();if(v.isEmpty())return;JSONArray a=AppData.arr(data,key);for(int i=0;i<a.length();i++)if(v.equals(a.optString(i)))return;a.put(v);data.put(key,a);AppData.save(this,data);after.run();}catch(Exception ignored){}}).setNegativeButton("لغو",null).show();}

    void reports(){
        base("گزارش‌ها و خروجی Excel");EditText q=input("جستجوی خریدار، شرکت یا شماره خرید");Button b=btn("🔎 جستجو");add(b);LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);add(list);
        Runnable render=()->{list.removeAllViews();String s=q.getText().toString().trim();TreeSet<String> buyers=new TreeSet<>(),companies=new TreeSet<>();JSONArray a=sortedPurchases();for(int i=0;i<a.length();i++){JSONObject p=a.optJSONObject(i);String blob=p.optString("unit",p.optString("buyer"))+" "+p.optString("company")+" "+p.optString("purchaseNo");if(s.isEmpty()||blob.contains(s)){buyers.add(p.optString("unit",p.optString("buyer")));companies.add(p.optString("company"));}}for(String x:buyers){Button z=btn("👤 "+x);z.setOnClickListener(vv->openPage(()->reportSelection("خریدار: "+x,"buyer",x)));list.addView(z);}for(String x:companies){Button z=btn("🏢 "+x);z.setOnClickListener(vv->openPage(()->reportSelection("شرکت: "+x,"company",x)));list.addView(z);}};
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
    void exportExcel(JSONArray a,String name){
        try{
            String[] h={"نام واحد","گواهی بهداشتی","تعداد جوجه‌ریزی","سهمیه ذرت","سهمیه سویا","تاریخ جوجه‌ریزی","تاریخ اعتبار","نهاده","شماره خرید","وزن","فی","نوع پرداخت","مبلغ خرید","تاریخ خرید","ذرت","سویا","ریزمغذی","نام شرکت","تاریخ سررسید","وصول","تاریخ وصول","تخصیص","تأمین‌شده","باقی‌مانده","تعداد مراحل تأمین","جزئیات مراحل","مصرف تجمعی ذرت گواهی","مانده ذرت گواهی","مصرف تجمعی سویا گواهی","مانده سویا گواهی","وضعیت سهمیه"};
            StringBuilder x=new StringBuilder("\uFEFF");for(String z:h)x.append(z).append("\t");x.append("\n");
            for(int i=0;i<a.length();i++){
                JSONObject p=a.optJSONObject(i);
                String details="";JSONArray st=funding(p);
                for(int j=0;j<st.length();j++){JSONObject q=st.optJSONObject(j);if(j>0)details+=" | ";details+="مرحله "+(j+1)+": "+q.optString("date")+" / "+q.optString("amount")+" / "+q.optString("source")+" -> "+q.optString("destination");}
                String unit=p.optString("unit",p.optString("buyer")),cert=p.optString("healthCertificate").trim();JSONObject certObj=cert.isEmpty()?null:certificateBase(unit+"|"+cert,null);double uc=cert.isEmpty()?0:usedCorn(unit,cert,null),us=cert.isEmpty()?0:usedSoy(unit,cert,null),cq=certObj==null?0:quotaOriginal(certObj,"cornQuota"),sq=certObj==null?0:quotaOriginal(certObj,"soyQuota");String[] v={unit,cert,p.optString("chickCount"),p.optString("cornQuota"),p.optString("soyQuota"),p.optString("placementDate"),p.optString("quotaExpiry"),p.optString("commodity"),p.optString("purchaseNo"),p.optString("weight"),p.optString("fee"),p.optString("payment"),p.optString("amount"),p.optString("buyDate"),p.optString("corn"),p.optString("soy"),p.optString("micronutrient"),p.optString("company"),p.optString("mainDue"),p.optBoolean("collected")?"وصول شده":"وصول نشده",p.optString("collectedDate"),p.optBoolean("allocated")?"تخصیص شده":"تخصیص نشده",String.valueOf(totalFunded(p)),String.valueOf(fundingRemaining(p)),String.valueOf(st.length()),details,fmtDecimal(uc),fmtDecimal(Math.max(0,cq-uc)),fmtDecimal(us),fmtDecimal(Math.max(0,sq-us)),(cq>0&&sq>0&&cq-uc<=0.0001&&sq-us<=0.0001)?"تکمیل":"مانده"};
                for(String z:v)x.append(xml(z)).append("\t");x.append("\n");
            }
            File f=new File(getCacheDir(),name+".xls");FileOutputStream o=new FileOutputStream(f);o.write(x.toString().getBytes("UTF-8"));o.close();
            Intent in=new Intent(Intent.ACTION_SEND);in.setType("application/vnd.ms-excel");in.putExtra(Intent.EXTRA_STREAM,FileProvider.getUriForFile(this,"com.morghtak.kharidmanager.fileprovider",f));in.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(Intent.createChooser(in,"ارسال / ذخیره Excel"));
        }catch(Exception e){Toast.makeText(this,"خطا در ساخت Excel",Toast.LENGTH_LONG).show();}
    }

    String xml(String s){return s.replace("&","&amp;").replace("\t"," ").replace("\n"," ").replace("\r"," ");}

    void backup(){
        base("پشتیبان‌گیری و بازیابی");
        add(tv("نسخه پشتیبان شامل همه خریدها، خریداران، شرکت‌ها، نهاده‌ها، وضعیت وصول/تخصیص/تأمین، تاریخ وصول، مراحل تأمین و تنظیمات هشدار است.",15));
        Button b=btn("💾 ساخت فایل پشتیبان");b.setOnClickListener(v->doBackup());add(b);
        b=btn("📥 بازیابی از فایل پشتیبان");b.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("application/json");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,91);});add(b);
        Button back=btn("← بازگشت");back.setOnClickListener(v->back());add(back);finishScreen("پشتیبان‌گیری و بازیابی");
    }
    void doBackup(){Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.setType("application/json");i.putExtra(Intent.EXTRA_TITLE,"kharidmanager_backup.json");startActivityForResult(i,92);}
    @Override protected void onActivityResult(int r,int c,Intent i){
        super.onActivityResult(r,c,i);UiManager.handleLogoResult(this,r,c,i);
        if(c!=RESULT_OK||i==null||i.getData()==null)return;
        if(r==92){
            try{OutputStream out=getContentResolver().openOutputStream(i.getData());if(out==null)throw new IOException();out.write(data.toString(2).getBytes("UTF-8"));out.close();Toast.makeText(this,"فایل پشتیبان با موفقیت ذخیره شد",Toast.LENGTH_LONG).show();}
            catch(Exception e){Toast.makeText(this,"خطا در ذخیره پشتیبان",Toast.LENGTH_LONG).show();}
        }else if(r==91){
            try{
                InputStream in=getContentResolver().openInputStream(i.getData());ByteArrayOutputStream o=new ByteArrayOutputStream();byte[] b=new byte[4096];int n;while((n=in.read(b))>0)o.write(b,0,n);in.close();
                JSONObject incoming=new JSONObject(new String(o.toByteArray(),"UTF-8"));if(!incoming.has("purchases"))throw new Exception("invalid");
                new AlertDialog.Builder(this).setTitle("تأیید بازیابی")
                    .setMessage("بازیابی اطلاعات فعلی را با اطلاعات فایل پشتیبان جایگزین می‌کند. ادامه می‌دهید؟")
                    .setPositiveButton("بله، بازیابی شود",(d,w)->{try{data=incoming;AppData.save(this,data);rescheduleAll();goHome();Toast.makeText(this,"بازیابی با موفقیت انجام شد",Toast.LENGTH_LONG).show();}catch(Exception e){Toast.makeText(this,"بازیابی انجام نشد",Toast.LENGTH_LONG).show();}})
                    .setNegativeButton("خیر",null).show();
            }catch(Exception e){Toast.makeText(this,"فایل پشتیبان معتبر نیست",Toast.LENGTH_LONG).show();}
        }
    }
    void rescheduleAll(){JSONArray a=AppData.arr(data,"purchases");for(int i=0;i<a.length();i++){JSONObject p=a.optJSONObject(i);if(p==null)continue;if(p.optBoolean("alarm"))schedule(this,p);else cancelAlarm(this,p);}}

    public static JSONObject findStatic(Context c,String id){try{JSONArray a=AppData.arr(AppData.root(c),"purchases");for(int i=0;i<a.length();i++){JSONObject p=a.optJSONObject(i);if(p!=null&&id!=null&&id.equals(p.optString("id")))return p;}}catch(Exception ignored){}return null;}
    public static void schedule(Context c,JSONObject p){try{if(!p.optBoolean("alarm"))return;String due=p.optString("mainDue");long at=PersianDate.millis(due,p.optString("alarmTime","10:00"))-p.optInt("alarmDays",1)*86400000L;if(p.optBoolean("alarmRepeat")&&at<=System.currentTimeMillis()){while(at<=System.currentTimeMillis())at+=86400000L;}if(at<=System.currentTimeMillis())return;AlarmManager am=(AlarmManager)c.getSystemService(ALARM_SERVICE);Intent in=new Intent(c,AlarmReceiver.class);in.putExtra("id",p.optString("id"));in.putExtra("title","خرید "+p.optString("purchaseNo")+" - "+p.optString("unit",p.optString("buyer")));PendingIntent pi=PendingIntent.getBroadcast(c,p.optString("id").hashCode(),in,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);if(Build.VERSION.SDK_INT>=31&&am.canScheduleExactAlarms())am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,pi);else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,pi);}catch(Exception ignored){}}
    public static void scheduleNext(Context c,JSONObject p){try{if(!p.optBoolean("alarmRepeat"))return;AlarmManager am=(AlarmManager)c.getSystemService(ALARM_SERVICE);Calendar cal=Calendar.getInstance();cal.add(Calendar.DAY_OF_YEAR,1);String[] hm=p.optString("alarmTime","10:00").split(":");try{cal.set(Calendar.HOUR_OF_DAY,Integer.parseInt(hm[0]));cal.set(Calendar.MINUTE,Integer.parseInt(hm[1]));}catch(Exception ignored){}cal.set(Calendar.SECOND,0);cal.set(Calendar.MILLISECOND,0);long at=cal.getTimeInMillis();Intent in=new Intent(c,AlarmReceiver.class);in.putExtra("id",p.optString("id"));in.putExtra("title","خرید "+p.optString("purchaseNo")+" - "+p.optString("unit",p.optString("buyer")));PendingIntent pi=PendingIntent.getBroadcast(c,p.optString("id").hashCode(),in,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);if(Build.VERSION.SDK_INT>=31&&am.canScheduleExactAlarms())am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,pi);else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,pi);}catch(Exception ignored){}}
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
