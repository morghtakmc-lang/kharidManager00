package com.morghtak.kharidmanager;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
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
    static SharedPreferences p(Context c){return c.getSharedPreferences(PREF,0);}
    public static int primary(Context c){return p(c).getInt("primary",Color.rgb(8,127,91));}
    public static int secondary(Context c){return p(c).getInt("secondary",Color.rgb(232,245,239));}
    public static int background(Context c){return p(c).getInt("background",Color.WHITE);}
    public static int card(Context c){return p(c).getInt("card",Color.WHITE);}
    public static int text(Context c){return p(c).getInt("text",Color.rgb(35,45,40));}
    public static float fieldSize(Context c){return p(c).getFloat("field",15);}
    public static float bodySize(Context c){return p(c).getFloat("body",16);}
    public static float titleSize(Context c){return p(c).getFloat("title",21);}
    public static float buttonSize(Context c){return p(c).getFloat("button",15);}
    public static float radius(Context c){return p(c).getFloat("radius",18);}
    public static float spacing(Context c){return p(c).getFloat("spacing",10);}
    public static String font(Context c){return p(c).getString("font","sans-serif");}
    public static int dp(Context c,float v){return Math.round(v*c.getResources().getDisplayMetrics().density);}
    static GradientDrawable bg(int color,float r){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dpDummy(r));return g;}
    static float dpDummy(float x){return x*3f;}
    public static void styleText(Context c,TextView v){
        v.setTypeface(Typeface.create(font(c),v instanceof Button?Typeface.BOLD:Typeface.NORMAL));
        if(v instanceof Button){v.setTextSize(buttonSize(c));v.setTextColor(Color.WHITE);v.setPadding(dp(c,12),dp(c,8),dp(c,12),dp(c,8));v.setMinHeight(dp(c,50));v.setBackground(bg(primary(c),radius(c)));}
        else if(v instanceof EditText){v.setTextSize(fieldSize(c));v.setTextColor(text(c));v.setHintTextColor(Color.GRAY);v.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);v.setBackground(bg(card(c),radius(c)));}
        else {v.setTextSize(bodySize(c));v.setTextColor(text(c));}
    }
    public static void decorate(Activity a,LinearLayout root,String title){
        root.setBackgroundColor(background(a));root.setPadding(dp(a,10),dp(a,8),dp(a,10),dp(a,18));styleTree(a,root);
        if(root.getChildCount()>0&&root.getChildAt(0) instanceof TextView){TextView h=(TextView)root.getChildAt(0);h.setTextSize(titleSize(a));h.setTextColor(Color.WHITE);h.setGravity(Gravity.CENTER);h.setBackground(bg(primary(a),radius(a)));h.getLayoutParams().height=dp(a,64);}
        if("مدیریت خرید و سررسید".equals(title)){
            ImageView logo=new ImageView(a);logo.setImageDrawable(loadLogo(a));logo.setAdjustViewBounds(true);logo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);root.addView(logo,1,new LinearLayout.LayoutParams(-1,dp(a,150)));
            Button settings=new Button(a);settings.setText("⚙ تنظیمات مدیر");styleText(a,settings);settings.setOnClickListener(v->showSettings(a));root.addView(settings,new LinearLayout.LayoutParams(-1,dp(a,58)));
        }
        styleTree(a,root);
        NestedScrollView scroll=new NestedScrollView(a);scroll.setFillViewport(true);ViewGroup parent=(ViewGroup)root.getParent();if(parent!=null)parent.removeView(root);scroll.addView(root,new ViewGroup.LayoutParams(-1,-2));a.setContentView(scroll);
    }
    static void styleTree(Context c,View v){if(v instanceof TextView)styleText(c,(TextView)v);if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)styleTree(c,g.getChildAt(i));}}
    static Drawable loadLogo(Context c){File f=new File(c.getFilesDir(),"company_logo.png");if(f.exists())return Drawable.createFromPath(f.getAbsolutePath());int id=c.getResources().getIdentifier("company_logo","drawable",c.getPackageName());return id==0?new ColorDrawable(Color.WHITE):c.getResources().getDrawable(id);}
    public static void showSettings(Activity a){
        final EditText pin=new EditText(a);pin.setHint("رمز مدیر");pin.setInputType(2|0x10);
        new AlertDialog.Builder(a).setTitle("دسترسی مدیر").setMessage("این بخش مخصوص مدیر است.").setView(pin).setPositiveButton("ورود",(d,w)->{if(!p(a).getString("pin",DEFAULT_PIN).equals(pin.getText().toString()))Toast.makeText(a,"رمز مدیر اشتباه است",Toast.LENGTH_SHORT).show();else panel(a);}).setNegativeButton("لغو",null).show();
    }
    static void panel(final Activity a){
        LinearLayout box=new LinearLayout(a);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(a,12),dp(a,8),dp(a,12),dp(a,12));ScrollView sv=new ScrollView(a);sv.addView(box);
        TextView preview=new TextView(a);preview.setText("متن نمونه برای پیش‌نمایش");preview.setGravity(Gravity.CENTER);preview.setPadding(dp(a,10),dp(a,15),dp(a,10),dp(a,15));box.addView(preview);
        Spinner font=spin(a,new String[]{"sans-serif","sans-serif-medium","sans-serif-condensed","serif","monospace"});select(font,font(a));box.addView(label(a,"نوع فونت"));box.addView(font);
        SeekBar body=seek(a,11,26,(int)bodySize(a)),title=seek(a,17,32,(int)titleSize(a)),button=seek(a,12,23,(int)buttonSize(a));
        TextView bl=label(a,"اندازه متن"),tl=label(a,"اندازه عنوان"),bul=label(a,"اندازه دکمه");box.addView(bl);box.addView(body);box.addView(tl);box.addView(title);box.addView(bul);box.addView(button);
        addPreviewListeners(preview,font,body,title,button);
        final int[] tempColors={primary(a),background(a),text(a),card(a)};
        addColor(box,a,"رنگ اصلی",tempColors,0,preview);addColor(box,a,"رنگ پس‌زمینه",tempColors,1,preview);addColor(box,a,"رنگ متن",tempColors,2,preview);addColor(box,a,"رنگ کارت/فیلد",tempColors,3,preview);
        Button logo=new Button(a);logo.setText("🖼 انتخاب لوگوی داخل برنامه");box.addView(logo);logo.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("image/*");i.addCategory(Intent.CATEGORY_OPENABLE);a.startActivityForResult(i,PICK_LOGO_REQUEST);});
        Button resetLogo=new Button(a);resetLogo.setText("↺ بازگردانی لوگوی اصلی");box.addView(resetLogo);resetLogo.setOnClickListener(v->{File f=new File(a.getFilesDir(),"company_logo.png");if(f.exists())f.delete();a.recreate();});
        EditText newPin=new EditText(a);newPin.setHint("رمز جدید مدیر (اختیاری)");newPin.setInputType(2|0x10);box.addView(newPin);
        Button save=new Button(a);save.setText("✓ ذخیره و اعمال");Button cancel=new Button(a);cancel.setText("لغو");box.addView(save);box.addView(cancel);AlertDialog dlg=new AlertDialog.Builder(a).setTitle("تنظیمات مدیر").setView(sv).create();
        cancel.setOnClickListener(v->dlg.dismiss());
        save.setOnClickListener(v->{SharedPreferences.Editor e=p(a).edit();e.putString("font",String.valueOf(font.getSelectedItem()));e.putFloat("body",body.getProgress()+11);e.putFloat("title",title.getProgress()+17);e.putFloat("button",button.getProgress()+12);e.putInt("primary",tempColors[0]);e.putInt("background",tempColors[1]);e.putInt("text",tempColors[2]);e.putInt("card",tempColors[3]);String np=newPin.getText().toString().trim();if(!np.isEmpty())e.putString("pin",np);e.apply();dlg.dismiss();a.recreate();});
        dlg.show();previewUpdate(preview,font,body,title,button,tempColors);
    }
    static void addPreviewListeners(TextView pv,Spinner f,SeekBar b,SeekBar t,SeekBar bu){AdapterView.OnItemSelectedListener l=new AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?> a,View v,int p,long id){pv.setTypeface(Typeface.create(String.valueOf(f.getSelectedItem()),Typeface.BOLD));}public void onNothingSelected(AdapterView<?> a){}};f.setOnItemSelectedListener(l);SeekBar.OnSeekBarChangeListener s=new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar x,int p,boolean u){pv.setTextSize(x==b?p+11:x==t?p+17:p+12);}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}};b.setOnSeekBarChangeListener(s);t.setOnSeekBarChangeListener(s);bu.setOnSeekBarChangeListener(s);}
    static void previewUpdate(TextView p,Spinner f,SeekBar b,SeekBar t,SeekBar bu,int[] c){p.setTypeface(Typeface.create(String.valueOf(f.getSelectedItem()),Typeface.BOLD));p.setTextSize(b.getProgress()+11);p.setTextColor(c[2]);p.setBackgroundColor(c[1]);}
    static void addColor(LinearLayout box,Activity a,String title,int[] colors,int idx,TextView preview){Button b=new Button(a);b.setText(title+"  ●");b.setTextColor(Color.WHITE);b.setBackgroundColor(colors[idx]);b.setOnClickListener(v->colorDialog(a,title,colors,idx,preview));box.addView(b);}
    static void colorDialog(Activity a,String title,int[] colors,int idx,TextView preview){
        int[] pal={0xFF087F5B,0xFF056044,0xFF2E7D32,0xFF1565C0,0xFF6A1B9A,0xFFC62828,0xFFEF8F00,0xFFF9A825,0xFF455A64,0xFF000000,0xFF555555,0xFFFFFFFF,0xFFF5F5F5,0xFFE8F5EF,0xFFFFF3E0,0xFFE3F2FD};
        LinearLayout grid=new LinearLayout(a);grid.setOrientation(LinearLayout.VERTICAL);
        for(int r=0;r<4;r++){LinearLayout row=new LinearLayout(a);for(int c=0;c<4;c++){final int col=pal[r*4+c];Button q=new Button(a);q.setText("  ");q.setBackgroundColor(col);q.setOnClickListener(v->{colors[idx]=col;if(idx==1)preview.setBackgroundColor(col);if(idx==2)preview.setTextColor(col);});row.addView(q,new LinearLayout.LayoutParams(0,55,1));}grid.addView(row);}
        new AlertDialog.Builder(a).setTitle("انتخاب "+title).setView(grid).setNegativeButton("بستن",null).show();
    }
    static TextView label(Context c,String s){TextView t=new TextView(c);t.setText(s);styleText(c,t);return t;}
    static Spinner spin(Context c,String[] a){Spinner s=new Spinner(c);s.setAdapter(new ArrayAdapter<String>(c,android.R.layout.simple_spinner_dropdown_item,a));return s;}
    static void select(Spinner s,String v){for(int i=0;i<s.getCount();i++)if(v.equals(String.valueOf(s.getItemAtPosition(i))))s.setSelection(i);}
    static SeekBar seek(Context c,int min,int max,int cur){SeekBar s=new SeekBar(c);s.setMax(max-min);s.setProgress(Math.max(0,Math.min(max-min,cur-min)));return s;}
    public static void handleLogoResult(Activity a,int r,int c,Intent d){
        if(r!=PICK_LOGO_REQUEST||c!=Activity.RESULT_OK||d==null||d.getData()==null)return;
        try{InputStream in=a.getContentResolver().openInputStream(d.getData());File f=new File(a.getFilesDir(),"company_logo.png");FileOutputStream out=new FileOutputStream(f);byte[] b=new byte[8192];int n;while((n=in.read(b))>0)out.write(b,0,n);in.close();out.close();Toast.makeText(a,"لوگو با موفقیت تغییر کرد",Toast.LENGTH_SHORT).show();a.recreate();}catch(Exception e){Toast.makeText(a,"خطا در انتخاب لوگو",Toast.LENGTH_LONG).show();}
    }
}
