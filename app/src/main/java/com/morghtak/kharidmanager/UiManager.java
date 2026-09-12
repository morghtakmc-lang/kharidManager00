package com.morghtak.kharidmanager;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;
import android.text.*;
import android.os.Build;
import androidx.core.widget.NestedScrollView;
import java.io.*;

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
    private static SharedPreferences p(Context c){return c.getSharedPreferences(PREF,0);}
    public static int dp(Context c,float v){return Math.round(v*c.getResources().getDisplayMetrics().density);}
    static GradientDrawable bg(int color,float radiusDp,int strokeColor,boolean stroke){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(radiusDp*3f);if(stroke)g.setStroke(1,strokeColor);return g;}
    public static Typeface selectedTypeface(Context c,int style){return Typeface.create(font(c),style);}
    public static void styleText(Context c,TextView v){
        v.setFontFeatureSettings("kern");
        if(v instanceof Button){
            v.setTextSize(buttonSize(c));v.setTypeface(selectedTypeface(c,Typeface.BOLD));v.setTextColor(Color.WHITE);
            v.setGravity(Gravity.CENTER);v.setPadding(dp(c,12),dp(c,8),dp(c,12),dp(c,8));v.setMinHeight(dp(c,52));
            v.setBackground(bg(primary(c),radius(c),primary(c),false));
        } else if(v instanceof EditText){
            v.setTextSize(fieldSize(c));v.setTypeface(selectedTypeface(c,Typeface.NORMAL));v.setTextColor(text(c));v.setHintTextColor(textSecondary(c));
            v.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);v.setPadding(dp(c,14),dp(c,8),dp(c,14),dp(c,8));v.setSingleLine(true);
            v.setBackground(bg(card(c),radius(c),secondary(c),true));
        } else {
            v.setTextSize(bodySize(c));v.setTypeface(selectedTypeface(c,Typeface.NORMAL));v.setTextColor(text(c));
            v.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);v.setLineSpacing(0,1.18f);
        }
    }
    public static void decorate(final Activity a,final LinearLayout root,final String title){
        final Context c=a;root.setBackgroundColor(background(c));root.setPadding(dp(c,10),dp(c,8),dp(c,10),dp(c,18));
        if(root.getChildCount()>0&&root.getChildAt(0) instanceof TextView){
            TextView h=(TextView)root.getChildAt(0);h.setTextSize(titleSize(c));h.setTypeface(selectedTypeface(c,Typeface.BOLD));h.setTextColor(Color.WHITE);h.setGravity(Gravity.CENTER);
            h.setPadding(dp(c,16),dp(c,8),dp(c,16),dp(c,8));h.setBackground(bg(primary(c),radius(c),primary(c),false));
            if(h.getLayoutParams()!=null)h.getLayoutParams().height=dp(c,64);
        }
        if("مدیریت خرید و سررسید".equals(title)){
            ImageView logo=new ImageView(c);logo.setImageDrawable(loadLogo(c));logo.setAdjustViewBounds(true);logo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);logo.setPadding(0,dp(c,6),0,dp(c,6));
            root.addView(logo,1,new LinearLayout.LayoutParams(-1,dp(c,155)));
            Button settings=new Button(c);settings.setText("⚙ تنظیمات مدیر");styleText(c,settings);settings.setOnClickListener(v->showSettings(a));root.addView(settings,new LinearLayout.LayoutParams(-1,dp(c,58)));
        }
        styleTree(c,root);
        NestedScrollView scroll=new NestedScrollView(c);scroll.setFillViewport(true);scroll.setClipToPadding(false);ViewGroup parent=(ViewGroup)root.getParent();if(parent!=null)parent.removeView(root);scroll.addView(root,new ViewGroup.LayoutParams(-1,-2));a.setContentView(scroll);
        if(Build.VERSION.SDK_INT>=21){a.getWindow().setStatusBarColor(primary(c));a.getWindow().setNavigationBarColor(dark(c)?Color.BLACK:background(c));}
    }
    private static Drawable loadLogo(Context c){File f=new File(c.getFilesDir(),"company_logo.png");if(f.exists())return Drawable.createFromPath(f.getAbsolutePath());int id=c.getResources().getIdentifier("company_logo","drawable",c.getPackageName());return id==0?new ColorDrawable(background(c)):c.getResources().getDrawable(id);}
    private static void styleTree(Context c,View v){if(v instanceof TextView)styleText(c,(TextView)v);if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){View child=g.getChildAt(i);styleTree(c,child);if(child.getLayoutParams() instanceof ViewGroup.MarginLayoutParams){ViewGroup.MarginLayoutParams mlp=(ViewGroup.MarginLayoutParams)child.getLayoutParams();mlp.bottomMargin=dp(c,spacing(c));child.setLayoutParams(mlp);}}}}
    public static void showSettings(final Activity a){
        final String storedPin=p(a).getString("pin",DEFAULT_PIN);final EditText pin=edit(a,"رمز مدیر");pin.setInputType(2|0x00000010);
        new AlertDialog.Builder(a).setTitle("دسترسی مدیر").setMessage("این بخش مخصوص مدیر است.").setView(pin).setPositiveButton("ورود",(d,w)->{if(!storedPin.equals(pin.getText().toString()))Toast.makeText(a,"رمز مدیر اشتباه است",Toast.LENGTH_SHORT).show();else openSettingsPanel(a);}).setNegativeButton("لغو",null).show();
    }
    private static void openSettingsPanel(final Activity a){
        final LinearLayout box=new LinearLayout(a);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(a,12),dp(a,4),dp(a,12),dp(a,12));ScrollView sv=new ScrollView(a);sv.addView(box);
        addTitle(box,a,"تنظیمات مدیر");
        addInfo(box,a,"در این بخش فقط تنظیمات مدیریتی و ضروری برنامه قرار دارد.");
        final CheckBox darkBox=new CheckBox(a);darkBox.setText("حالت تاریک");darkBox.setChecked(dark(a));box.addView(darkBox,lp());
        Button logo=button(a,"🖼 انتخاب لوگوی داخل برنامه");logo.setOnClickListener(v->{a.startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE),PICK_LOGO_REQUEST);});box.addView(logo,lp());
        final boolean[] resetLogoRequested={false};Button resetLogo=button(a,"↺ بازگردانی لوگوی اصلی شرکت");resetLogo.setOnClickListener(v->{resetLogoRequested[0]=true;Toast.makeText(a,"بازگردانی لوگو برای زمان ذخیره انتخاب شد",Toast.LENGTH_SHORT).show();});box.addView(resetLogo,lp());
        final EditText newPin=edit(a,"رمز جدید مدیر (خالی = بدون تغییر)");newPin.setInputType(2|0x00000010);box.addView(newPin,lp());
        Button save=button(a,"✓ ذخیره و اعمال تنظیمات");Button cancel=button(a,"لغو");box.addView(save,lp());box.addView(cancel,lp());
        final AlertDialog dlg=new AlertDialog.Builder(a).setTitle("تنظیمات مدیر").setView(sv).create();
        save.setOnClickListener(v->{
            boolean newDark=darkBox.isChecked();
            SharedPreferences.Editor e=p(a).edit();e.putBoolean("dark",newDark);String np=newPin.getText().toString().trim();if(!np.isEmpty())e.putString("pin",np);e.apply();
            if(resetLogoRequested[0]){File f=new File(a.getFilesDir(),"company_logo.png");if(f.exists())f.delete();}
            dlg.dismiss();
            if(Build.VERSION.SDK_INT>=21){a.getWindow().setNavigationBarColor(newDark?Color.BLACK:Color.WHITE);a.getWindow().setStatusBarColor(primary(a));}
            a.recreate();
        });
        cancel.setOnClickListener(v->dlg.dismiss());dlg.setOnCancelListener(d->{});dlg.show();
    }
    public static void handleLogoResult(Activity a,int requestCode,int resultCode,Intent data){
        if(requestCode!=PICK_LOGO_REQUEST||resultCode!=Activity.RESULT_OK||data==null||data.getData()==null)return;try{InputStream in=a.getContentResolver().openInputStream(data.getData());if(in==null)throw new IOException("Cannot open image");File f=new File(a.getFilesDir(),"company_logo.png");FileOutputStream out=new FileOutputStream(f);byte[] buf=new byte[4096];int n;while((n=in.read(buf))>0)out.write(buf,0,n);in.close();out.close();Toast.makeText(a,"لوگو با موفقیت انتخاب شد",Toast.LENGTH_SHORT).show();a.recreate();}catch(Exception e){Toast.makeText(a,"خطا در انتخاب لوگو",Toast.LENGTH_SHORT).show();}
    }
    private static EditText edit(Activity a,String hint){EditText e=new EditText(a);e.setHint(hint);e.setSingleLine(true);e.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);styleText(a,e);return e;}
    private static Button button(Activity a,String s){Button b=new Button(a);b.setText(s);b.setAllCaps(false);styleText(a,b);return b;}
    private static TextView label(Activity a,String s){TextView t=new TextView(a);t.setText(s);styleText(a,t);return t;}
    private static void addTitle(LinearLayout b,Activity a,String s){TextView t=label(a,s);t.setTextSize(titleSize(a));t.setTypeface(selectedTypeface(a,Typeface.BOLD));b.addView(t,lp());}
    private static void addInfo(LinearLayout b,Activity a,String s){b.addView(label(a,s),lp());}
    private static LinearLayout.LayoutParams lp(){return new LinearLayout.LayoutParams(-1,ViewGroup.LayoutParams.WRAP_CONTENT);}
    private static Spinner spinner(Activity a,String[] values){Spinner s=new Spinner(a);s.setAdapter(new ArrayAdapter<String>(a,android.R.layout.simple_spinner_dropdown_item,values));return s;}
    private static void selectFont(Spinner s,String[] values,String selected){for(int i=0;i<values.length;i++)if(values[i].equals(selected)){s.setSelection(i);return;}s.setSelection(0);}
    private static SeekBar seek(Activity a,int min,int max,int current){SeekBar s=new SeekBar(a);s.setMax(max-min);s.setProgress(Math.max(0,Math.min(max-min,current-min)));return s;}
}
