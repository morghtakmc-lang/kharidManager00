package com.morghtak.kharidmanager;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import androidx.core.widget.NestedScrollView;
import java.io.*;
import java.util.*;

public final class UiManager {
    private static final String PREF="ui_settings";
    private static final String DEFAULT_PIN="1234";
    public static final int PICK_LOGO_REQUEST=72;
    private UiManager(){}

    public static int primary(Context c){return p(c).getInt("primary",Color.rgb(8,127,91));}
    public static int secondary(Context c){return p(c).getInt("secondary",Color.rgb(235,246,241));}
    public static int background(Context c){return dark(c)?Color.rgb(18,18,18):p(c).getInt("background",Color.WHITE);}
    public static int card(Context c){return dark(c)?Color.rgb(30,30,30):p(c).getInt("card",Color.WHITE);}
    public static int text(Context c){return dark(c)?Color.WHITE:p(c).getInt("text",Color.rgb(35,45,40));}
    public static int textSecondary(Context c){return dark(c)?Color.rgb(200,200,200):p(c).getInt("text2",Color.rgb(90,100,95));}
    public static float bodySize(Context c){return p(c).getFloat("body",16f);}
    public static float titleSize(Context c){return p(c).getFloat("title",21f);}
    public static float buttonSize(Context c){return p(c).getFloat("button",15f);}
    public static float fieldSize(Context c){return p(c).getFloat("field",15f);}
    public static float radius(Context c){return p(c).getFloat("radius",18f);}
    public static float spacing(Context c){return p(c).getFloat("spacing",10f);}
    public static String font(Context c){return p(c).getString("font","sans-serif");}
    public static boolean dark(Context c){return p(c).getBoolean("dark",false);}
    public static String companyName(Context c){return p(c).getString("companyName","شرکت شاهدانه طیور مارلیک");}
    private static SharedPreferences p(Context c){return c.getSharedPreferences(PREF,0);}
    public static int dp(Context c,float v){return Math.round(v*c.getResources().getDisplayMetrics().density);}

    static GradientDrawable bg(int color,float radiusDp,int strokeColor,boolean stroke){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(radiusDp);if(stroke)g.setStroke(dpRadiusStroke(strokeColor),strokeColor);return g;}
    private static int dpRadiusStroke(int c){return 1;}

    public static void styleText(Context c,TextView v){
        v.setFontFeatureSettings("kern");
        if(v instanceof Button){v.setTextSize(buttonSize(c));v.setTypeface(Typeface.create(font(c),Typeface.BOLD));v.setTextColor(Color.WHITE);v.setGravity(Gravity.CENTER);v.setPadding(dp(c,12),dp(c,8),dp(c,12),dp(c,8));v.setMinHeight(dp(c,52));v.setBackground(bg(primary(c),dp(c,radius(c)),primary(c),false));}
        else if(v instanceof EditText){v.setTextSize(fieldSize(c));v.setTypeface(Typeface.create(font(c),Typeface.NORMAL));v.setTextColor(text(c));v.setHintTextColor(textSecondary(c));v.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);v.setPadding(dp(c,14),dp(c,8),dp(c,14),dp(c,8));v.setSingleLine(true);v.setBackground(bg(card(c),dp(c,radius(c)),secondary(c),true));}
        else {v.setTextSize(bodySize(c));v.setTypeface(Typeface.create(font(c),Typeface.NORMAL));v.setTextColor(text(c));v.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);v.setLineSpacing(0,1.18f);}
    }

    public static void decorate(final Activity a,final LinearLayout root,final String title){
        final Context c=a;root.setBackgroundColor(background(c));root.setPadding(dp(c,10),dp(c,8),dp(c,10),dp(c,18));normalizeVerticalWeights(root);
        if(root.getChildCount()>0&&root.getChildAt(0) instanceof TextView){TextView h=(TextView)root.getChildAt(0);h.setTextSize(titleSize(c));h.setTypeface(Typeface.create(font(c),Typeface.BOLD));h.setTextColor(Color.WHITE);h.setGravity(Gravity.CENTER);h.setPadding(dp(c,16),dp(c,8),dp(c,16),dp(c,8));h.setBackground(bg(primary(c),dp(c,radius(c)),primary(c),false));h.getLayoutParams().height=dp(c,64);}
        if("مدیریت خرید و سررسید".equals(title)){
            ImageView logo=new ImageView(c);logo.setImageDrawable(loadLogo(c));logo.setAdjustViewBounds(true);logo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);logo.setPadding(0,dp(c,6),0,dp(c,6));
            root.addView(logo,1,new LinearLayout.LayoutParams(-1,dp(c,155)));
            Button settings=new Button(c);settings.setText("⚙ تنظیمات مدیر");styleText(c,settings);settings.setOnClickListener(v->showSettings(a));root.addView(settings,new LinearLayout.LayoutParams(-1,dp(c,58)));
        }
        styleTree(c,root);
        if(root.getChildCount()>0&&root.getChildAt(0) instanceof TextView){TextView h=(TextView)root.getChildAt(0);h.setTextSize(titleSize(c));h.setTypeface(Typeface.create(font(c),Typeface.BOLD));h.setTextColor(Color.WHITE);h.setGravity(Gravity.CENTER);h.setBackground(bg(primary(c),dp(c,radius(c)),primary(c),false));}
        NestedScrollView scroll=new NestedScrollView(c);scroll.setFillViewport(true);scroll.setClipToPadding(false);ViewGroup parent=(ViewGroup)root.getParent();if(parent!=null)parent.removeView(root);scroll.addView(root,new ViewGroup.LayoutParams(-1,-2));a.setContentView(scroll);
        if(Build.VERSION.SDK_INT>=21){a.getWindow().setStatusBarColor(primary(c));a.getWindow().setNavigationBarColor(dark(c)?Color.BLACK:background(c));}
        if(Build.VERSION.SDK_INT>=23){int flags=a.getWindow().getDecorView().getSystemUiVisibility();if(!dark(c))flags|=View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;else flags&=~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;a.getWindow().getDecorView().setSystemUiVisibility(flags);}
    }

    private static Drawable loadLogo(Context c){
        File f=new File(c.getFilesDir(),"company_logo.png");
        if(f.exists())return Drawable.createFromPath(f.getAbsolutePath());
        int id=c.getResources().getIdentifier("company_logo","drawable",c.getPackageName());
        return id==0?new ColorDrawable(background(c)):c.getResources().getDrawable(id);
    }
    private static void styleTree(Context c,View v){if(v instanceof TextView)styleText(c,(TextView)v);if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)styleTree(c,g.getChildAt(i));}}
    private static void normalizeVerticalWeights(View v){if(!(v instanceof ViewGroup))return;ViewGroup g=(ViewGroup)v;if(g instanceof LinearLayout&&((LinearLayout)g).getOrientation()==LinearLayout.VERTICAL){for(int i=0;i<g.getChildCount();i++){View child=g.getChildAt(i);ViewGroup.LayoutParams p=child.getLayoutParams();if(p instanceof LinearLayout.LayoutParams){LinearLayout.LayoutParams lp=(LinearLayout.LayoutParams)p;if(lp.height==0&&lp.weight>0){lp.height=ViewGroup.LayoutParams.WRAP_CONTENT;lp.weight=0;child.setLayoutParams(lp);}}}}for(int i=0;i<g.getChildCount();i++)normalizeVerticalWeights(g.getChildAt(i));}

    public static void showSettings(final Activity a){
        final String storedPin=p(a).getString("pin",DEFAULT_PIN);final EditText pin=edit(a,"رمز مدیر");pin.setInputType(2|0x00000010);
        new AlertDialog.Builder(a).setTitle("دسترسی مدیر").setMessage("این بخش مخصوص مدیر است.").setView(pin).setPositiveButton("ورود",(d,w)->{if(!storedPin.equals(pin.getText().toString()))Toast.makeText(a,"رمز مدیر اشتباه است",Toast.LENGTH_SHORT).show();else openSettingsPanel(a);}).setNegativeButton("لغو",null).show();
    }
    private static void openSettingsPanel(final Activity a){
        final LinearLayout box=new LinearLayout(a);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(a,12),dp(a,4),dp(a,12),dp(a,12));ScrollView sv=new ScrollView(a);sv.addView(box);
        addTitle(box,a,"تنظیمات ظاهری و مدیریتی");addInfo(box,a,"تغییرات این بخش روی ظاهر تمام صفحات برنامه اعمال می‌شود.");
        final EditText name=edit(a,"نام شرکت");name.setText(companyName(a));box.addView(name,lp());
        final Spinner font=spinner(a,new String[]{"sans-serif","sans-serif-medium","sans-serif-condensed","serif","monospace"});select(font,font(a));box.addView(label(a,"نوع فونت"),lp());box.addView(font,lp());
        final SeekBar body=seek(a,11,26,(int)bodySize(a));TextView bodyL=label(a,"اندازه متن: "+(int)bodySize(a));box.addView(bodyL,lp());box.addView(body,lp());
        final SeekBar title=seek(a,17,32,(int)titleSize(a));TextView titleL=label(a,"اندازه عنوان: "+(int)titleSize(a));box.addView(titleL,lp());box.addView(title,lp());
        final SeekBar button=seek(a,12,23,(int)buttonSize(a));TextView buttonL=label(a,"اندازه نوشته دکمه: "+(int)buttonSize(a));box.addView(buttonL,lp());box.addView(button,lp());
        final SeekBar field=seek(a,12,23,(int)fieldSize(a));TextView fieldL=label(a,"اندازه نوشته فیلد: "+(int)fieldSize(a));box.addView(fieldL,lp());box.addView(field,lp());
        final SeekBar radius=seek(a,0,32,(int)radius(a));TextView radiusL=label(a,"گردی گوشه‌ها: "+(int)radius(a));box.addView(radiusL,lp());box.addView(radius,lp());
        final SeekBar spacing=seek(a,4,24,(int)spacing(a));TextView spacingL=label(a,"فاصله عناصر: "+(int)spacing(a));box.addView(spacingL,lp());box.addView(spacing,lp());
        body.setOnSeekBarChangeListener(labelListener(bodyL,"اندازه متن: ",11));title.setOnSeekBarChangeListener(labelListener(titleL,"اندازه عنوان: ",17));button.setOnSeekBarChangeListener(labelListener(buttonL,"اندازه نوشته دکمه: ",12));field.setOnSeekBarChangeListener(labelListener(fieldL,"اندازه نوشته فیلد: ",12));radius.setOnSeekBarChangeListener(labelListener(radiusL,"گردی گوشه‌ها: ",0));spacing.setOnSeekBarChangeListener(labelListener(spacingL,"فاصله عناصر: ",4));
        addColorField(box,a,"رنگ اصلی",primary(a),"primary");addColorField(box,a,"رنگ پس‌زمینه",background(a),"background");addColorField(box,a,"رنگ کارت/فیلد",card(a),"card");addColorField(box,a,"رنگ متن",text(a),"text");
        final CheckBox darkBox=new CheckBox(a);darkBox.setText("حالت تاریک");darkBox.setChecked(dark(a));styleText(a,darkBox);box.addView(darkBox,lp());
        Button logo=button(a,"🖼 انتخاب لوگوی داخل برنامه");logo.setOnClickListener(v->{a.startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE),PICK_LOGO_REQUEST);});box.addView(logo,lp());
        Button resetLogo=button(a,"↺ بازگردانی لوگوی اصلی شرکت");resetLogo.setOnClickListener(v->{File f=new File(a.getFilesDir(),"company_logo.png");if(f.exists())f.delete();Toast.makeText(a,"لوگوی اصلی فعال شد",Toast.LENGTH_SHORT).show();a.recreate();});box.addView(resetLogo,lp());
        final EditText newPin=edit(a,"رمز جدید مدیر (خالی = بدون تغییر)");newPin.setInputType(2|0x00000010);box.addView(newPin,lp());
        Button save=button(a,"✓ ذخیره و اعمال تنظیمات");Button reset=button(a,"↺ بازگردانی همه تنظیمات اولیه");box.addView(save,lp());box.addView(reset,lp());
        AlertDialog dlg=new AlertDialog.Builder(a).setTitle("تنظیمات مدیر").setView(sv).setNegativeButton("بستن",null).create();
        save.setOnClickListener(v->{SharedPreferences.Editor e=p(a).edit();e.putString("font",String.valueOf(font.getSelectedItem()));e.putFloat("body",body.getProgress()+11);e.putFloat("title",title.getProgress()+17);e.putFloat("button",button.getProgress()+12);e.putFloat("field",field.getProgress()+12);e.putFloat("radius",radius.getProgress());e.putFloat("spacing",spacing.getProgress()+4);e.putBoolean("dark",darkBox.isChecked());e.putString("companyName",name.getText().toString().trim());String np=newPin.getText().toString().trim();if(!np.isEmpty())e.putString("pin",np);e.apply();dlg.dismiss();a.recreate();});
        reset.setOnClickListener(v->{p(a).edit().clear().apply();File f=new File(a.getFilesDir(),"company_logo.png");if(f.exists())f.delete();dlg.dismiss();a.recreate();});dlg.show();
    }
    public static void handleLogoResult(Activity a,int requestCode,int resultCode,Intent data){if(requestCode!=PICK_LOGO_REQUEST||resultCode!=Activity.RESULT_OK||data==null||data.getData()==null)return;try{InputStream in=a.getContentResolver().openInputStream(data.getData());File f=new File(a.getFilesDir(),"company_logo.png");FileOutputStream out=new FileOutputStream(f);byte[] buf=new byte[8192];int n;while((n=in.read(buf))>0)out.write(buf,0,n);in.close();out.close();Toast.makeText(a,"لوگو با موفقیت تغییر کرد",Toast.LENGTH_SHORT).show();a.recreate();}catch(Exception e){Toast.makeText(a,"خطا در انتخاب لوگو",Toast.LENGTH_LONG).show();}}

    private static SeekBar.OnSeekBarChangeListener labelListener(final TextView label,final String prefix,final int min){return new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int p,boolean f){label.setText(prefix+(p+min));}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}};}
    private static void addColorField(LinearLayout box,Activity a,String title,int color,String key){LinearLayout row=new LinearLayout(a);row.setGravity(Gravity.CENTER_VERTICAL);TextView t=label(a,title);row.addView(t,new LinearLayout.LayoutParams(0,dp(a,52),1));Button b=button(a,String.format("#%06X",0xFFFFFF&color));b.setOnClickListener(v->{EditText e=edit(a,"کد رنگ مثل #087F5B");e.setText(String.format("#%06X",0xFFFFFF&color));new AlertDialog.Builder(a).setTitle(title).setView(e).setPositiveButton("ذخیره",(d,w)->{try{p(a).edit().putInt(key,Color.parseColor(e.getText().toString().trim())).apply();a.recreate();}catch(Exception ignored){Toast.makeText(a,"کد رنگ معتبر نیست",Toast.LENGTH_SHORT).show();}}).setNegativeButton("لغو",null).show();});row.addView(b,new LinearLayout.LayoutParams(dp(a,145),dp(a,52)));box.addView(row,lp());}
    private static EditText edit(Activity a,String hint){EditText e=new EditText(a);e.setHint(hint);e.setSingleLine(true);e.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);styleText(a,e);return e;}
    private static Button button(Activity a,String s){Button b=new Button(a);b.setText(s);b.setAllCaps(false);styleText(a,b);return b;}
    private static TextView label(Activity a,String s){TextView t=new TextView(a);t.setText(s);styleText(a,t);return t;}
    private static void addTitle(LinearLayout b,Activity a,String s){TextView t=label(a,s);t.setTextSize(titleSize(a));t.setTypeface(Typeface.create(font(a),Typeface.BOLD));b.addView(t,lp());}
    private static void addInfo(LinearLayout b,Activity a,String s){b.addView(label(a,s),lp());}
    private static LinearLayout.LayoutParams lp(){return new LinearLayout.LayoutParams(-1,ViewGroup.LayoutParams.WRAP_CONTENT);}
    private static Spinner spinner(Activity a,String[] values){Spinner s=new Spinner(a);s.setAdapter(new ArrayAdapter<String>(a,android.R.layout.simple_spinner_dropdown_item,values));return s;}
    private static void select(Spinner s,String value){for(int i=0;i<s.getCount();i++)if(String.valueOf(s.getItemAtPosition(i)).equals(value)){s.setSelection(i);break;}}
    private static SeekBar seek(Activity a,int min,int max,int current){SeekBar s=new SeekBar(a);s.setMax(max-min);s.setProgress(Math.max(0,Math.min(max-min,current-min)));return s;}
}
