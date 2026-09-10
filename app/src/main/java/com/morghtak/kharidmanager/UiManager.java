package com.morghtak.kharidmanager;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;
import android.text.*;
import android.util.TypedValue;
import androidx.core.widget.NestedScrollView;
import java.io.*;

public final class UiManager {
    private static final String PREF="ui_settings";
    private static final String DEFAULT_PIN="1234";
    public static final int PICK_LOGO_REQUEST=72;
    private UiManager(){}
    public static int primary(Context c){return p(c).getInt("primary",Color.rgb(8,127,91));}
    public static int secondary(Context c){return dark(c)?Color.rgb(55,65,61):p(c).getInt("secondary",Color.rgb(235,246,241));}
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
    private static void uiTextSize(TextView v,float size){v.setTextSize(TypedValue.COMPLEX_UNIT_DIP,size);v.setIncludeFontPadding(false);}
    public static void styleText(Context c,TextView v){
        v.setFontFeatureSettings("kern");
        v.setIncludeFontPadding(false);
        if(v instanceof Button){
            uiTextSize(v,Math.max(13,Math.min(17,buttonSize(c))));v.setTypeface(selectedTypeface(c,Typeface.BOLD));v.setTextColor(Color.WHITE);
            v.setGravity(Gravity.CENTER);v.setPadding(dp(c,10),dp(c,8),dp(c,10),dp(c,8));v.setMinHeight(dp(c,52));v.setMinimumHeight(dp(c,52));v.setMaxLines(3);v.setEllipsize(null);v.setAllCaps(false);v.setSingleLine(false);
            v.setBackground(bg(primary(c),radius(c),primary(c),false));
        } else if(v instanceof EditText){
            uiTextSize(v,Math.max(13,Math.min(17,fieldSize(c))));v.setTypeface(selectedTypeface(c,Typeface.NORMAL));v.setTextColor(text(c));v.setHintTextColor(textSecondary(c));
            v.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);v.setPadding(dp(c,12),dp(c,9),dp(c,12),dp(c,9));v.setSingleLine(true);v.setIncludeFontPadding(false);
            v.setBackground(bg(card(c),radius(c),primary(c),true));
        } else {
            uiTextSize(v,Math.max(13,Math.min(18,bodySize(c))));v.setTypeface(selectedTypeface(c,Typeface.NORMAL));v.setTextColor(text(c));
            v.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);v.setLineSpacing(0,1.15f);v.setMaxLines(Integer.MAX_VALUE);
        }
    }
    public static void decorate(final Activity a,final LinearLayout root,final String title){
        final Context c=a;root.setBackgroundColor(background(c));root.setPadding(dp(c,8),dp(c,6),dp(c,8),dp(c,14));
        if(root.getChildCount()>0&&root.getChildAt(0) instanceof TextView){
            TextView h=(TextView)root.getChildAt(0);h.setTextSize(titleSize(c));h.setTypeface(selectedTypeface(c,Typeface.BOLD));h.setTextColor(Color.WHITE);h.setGravity(Gravity.CENTER);
            h.setPadding(dp(c,16),dp(c,8),dp(c,16),dp(c,8));h.setBackground(bg(primary(c),radius(c),primary(c),false));
            if(h.getLayoutParams()!=null){h.getLayoutParams().height=dp(c,58);h.getLayoutParams().width=ViewGroup.LayoutParams.MATCH_PARENT;}
        }
        if("مدیریت خرید و سررسید".equals(title)){
            ImageView logo=new ImageView(c);logo.setImageDrawable(loadLogo(c));logo.setAdjustViewBounds(true);logo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);logo.setPadding(0,dp(c,6),0,dp(c,6));
            root.addView(logo,1,new LinearLayout.LayoutParams(-1,dp(c,140)));
            Button settings=new Button(c);settings.setText("⚙ تنظیمات مدیر");styleText(c,settings);settings.setOnClickListener(v->showSettings(a));root.addView(settings,new LinearLayout.LayoutParams(-1,dp(c,56)));
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
        addTitle(box,a,"تنظیمات ظاهری و مدیریتی");addInfo(box,a,"تغییرات را ابتدا در پیش‌نمایش ببینید؛ ذخیره اعمال می‌کند و لغو هیچ تغییری نمی‌دهد.");
        addInfo(box,a,"نام شرکت (فقط نمونه پیش‌نمایش)");final EditText sampleName=edit(a,"مثال: نام شرکت نمونه");sampleName.setText("نام شرکت نمونه");box.addView(sampleName,lp());
        addInfo(box,a,"پیش‌نمایش زنده");final LinearLayout preview=new LinearLayout(a);preview.setOrientation(LinearLayout.VERTICAL);preview.setPadding(dp(a,12),dp(a,12),dp(a,12),dp(a,12));box.addView(preview,lp());
        final String[] fontValues={"sans-serif","sans-serif-medium","sans-serif-condensed","sans-serif-light","serif","monospace"};
        final String[] fontNames={"پیش‌فرض","متوسط","فشرده","باریک","کلاسیک","تک‌فاصله"};
        final Spinner font=spinner(a,fontNames);selectFont(font,fontValues,font(a));box.addView(label(a,"نوع فونت"),lp());box.addView(font,lp());
        final SeekBar body=seek(a,11,26,(int)bodySize(a));TextView bodyL=label(a,"اندازه متن: "+(int)bodySize(a));box.addView(bodyL,lp());box.addView(body,lp());
        final SeekBar title=seek(a,17,32,(int)titleSize(a));TextView titleL=label(a,"اندازه عنوان: "+(int)titleSize(a));box.addView(titleL,lp());box.addView(title,lp());
        final SeekBar button=seek(a,12,23,(int)buttonSize(a));TextView buttonL=label(a,"اندازه نوشته دکمه: "+(int)buttonSize(a));box.addView(buttonL,lp());box.addView(button,lp());
        final SeekBar field=seek(a,12,23,(int)fieldSize(a));TextView fieldL=label(a,"اندازه نوشته فیلد: "+(int)fieldSize(a));box.addView(fieldL,lp());box.addView(field,lp());
        final SeekBar radius=seek(a,0,32,(int)radius(a));TextView radiusL=label(a,"گردی گوشه‌ها: "+(int)radius(a));box.addView(radiusL,lp());box.addView(radius,lp());
        final SeekBar spacing=seek(a,4,24,(int)spacing(a));TextView spacingL=label(a,"فاصله عناصر: "+(int)spacing(a));box.addView(spacingL,lp());box.addView(spacing,lp());
        final int[] tempPrimary={primary(a)},tempBackground={background(a)},tempCard={card(a)},tempText={text(a)};final Runnable[] refreshRef={null};
        addPalette(box,a,"رنگ اصلی",tempPrimary,refreshRef);addPalette(box,a,"رنگ پس‌زمینه",tempBackground,refreshRef);addPalette(box,a,"رنگ کارت/فیلد",tempCard,refreshRef);addPalette(box,a,"رنگ متن",tempText,refreshRef);
        final CheckBox darkBox=new CheckBox(a);darkBox.setText("حالت تاریک");darkBox.setChecked(dark(a));box.addView(darkBox,lp());
        Button logo=button(a,"🖼 انتخاب لوگوی داخل برنامه");logo.setOnClickListener(v->{a.startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE),PICK_LOGO_REQUEST);});box.addView(logo,lp());
        final boolean wasDark=dark(a);final boolean[] resetLogoRequested={false};Button resetLogo=button(a,"↺ بازگردانی لوگوی اصلی شرکت");resetLogo.setOnClickListener(v->{resetLogoRequested[0]=true;Toast.makeText(a,"بازگردانی لوگو برای زمان ذخیره انتخاب شد",Toast.LENGTH_SHORT).show();});box.addView(resetLogo,lp());
        final EditText newPin=edit(a,"رمز جدید مدیر (خالی = بدون تغییر)");newPin.setInputType(2|0x00000010);box.addView(newPin,lp());
        Button save=button(a,"✓ ذخیره و اعمال تنظیمات");Button cancel=button(a,"لغو");box.addView(save,lp());box.addView(cancel,lp());
        final AlertDialog dlg=new AlertDialog.Builder(a).setTitle("تنظیمات مدیر").setView(sv).create();
        final Runnable refreshPreview=()->{
            preview.removeAllViews();boolean isDark=darkBox.isChecked();int previewBg=isDark?Color.rgb(18,18,18):tempBackground[0];int previewText=isDark?Color.WHITE:tempText[0];int previewCard=isDark?Color.rgb(30,30,30):tempCard[0];
            String selectedFont=fontValues[Math.max(0,Math.min(font.getSelectedItemPosition(),fontValues.length-1))];preview.setBackgroundColor(previewBg);
            TextView h=label(a,sampleName.getText().toString());h.setTextSize(TypedValue.COMPLEX_UNIT_DIP,Math.max(17,Math.min(28,title.getProgress()+17)));h.setTypeface(Typeface.create(selectedFont,Typeface.BOLD));h.setTextColor(Color.WHITE);h.setGravity(Gravity.CENTER);h.setBackground(bg(tempPrimary[0],radius.getProgress()+0f,tempPrimary[0],false));h.setPadding(dp(a,8),dp(a,8),dp(a,8),dp(a,8));preview.addView(h,lp());
            TextView bodyV=label(a,"این متن نمونه برای مشاهده تغییر فونت، اندازه و رنگ است.\nAa 12345");bodyV.setTextSize(TypedValue.COMPLEX_UNIT_DIP,Math.max(13,Math.min(20,body.getProgress()+11)));bodyV.setTypeface(Typeface.create(selectedFont,Typeface.NORMAL));bodyV.setTextColor(previewText);preview.addView(bodyV,lp());
            Button bv=button(a,"دکمه نمونه");bv.setTextSize(TypedValue.COMPLEX_UNIT_DIP,Math.max(13,Math.min(17,button.getProgress()+12)));bv.setTypeface(Typeface.create(selectedFont,Typeface.BOLD));bv.setBackground(bg(tempPrimary[0],radius.getProgress()+0f,tempPrimary[0],false));preview.addView(bv,lp());
            EditText ev=edit(a,"نمونه فیلد");ev.setTextSize(TypedValue.COMPLEX_UNIT_DIP,Math.max(13,Math.min(17,field.getProgress()+12)));ev.setTypeface(Typeface.create(selectedFont,Typeface.NORMAL));ev.setTextColor(previewText);ev.setHintTextColor(isDark?Color.rgb(190,190,190):textSecondary(a));ev.setBackground(bg(previewCard,radius.getProgress()+0f,tempPrimary[0],true));preview.addView(ev,lp());
        };
        refreshRef[0]=refreshPreview;
        SeekBar.OnSeekBarChangeListener listener=new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int pos,boolean fromUser){bodyL.setText("اندازه متن: "+(body==s?pos+11:body.getProgress()+11));titleL.setText("اندازه عنوان: "+(title==s?pos+17:title.getProgress()+17));buttonL.setText("اندازه نوشته دکمه: "+(button==s?pos+12:button.getProgress()+12));fieldL.setText("اندازه نوشته فیلد: "+(field==s?pos+12:field.getProgress()+12));radiusL.setText("گردی گوشه‌ها: "+radius.getProgress());spacingL.setText("فاصله عناصر: "+(spacing.getProgress()+4));refreshPreview.run();}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}};
        body.setOnSeekBarChangeListener(listener);title.setOnSeekBarChangeListener(listener);button.setOnSeekBarChangeListener(listener);field.setOnSeekBarChangeListener(listener);radius.setOnSeekBarChangeListener(listener);spacing.setOnSeekBarChangeListener(listener);
        font.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> p){}public void onItemSelected(AdapterView<?> p,View v,int pos,long id){refreshPreview.run();}});
        sampleName.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){refreshPreview.run();}public void afterTextChanged(Editable e){}});
        darkBox.setOnCheckedChangeListener((b,v)->refreshPreview.run());refreshPreview.run();
        save.setOnClickListener(v->{SharedPreferences.Editor e=p(a).edit();int fontPos=Math.max(0,Math.min(font.getSelectedItemPosition(),fontValues.length-1));e.putString("font",fontValues[fontPos]);e.putFloat("body",body.getProgress()+11);e.putFloat("title",title.getProgress()+17);e.putFloat("button",button.getProgress()+12);e.putFloat("field",field.getProgress()+12);e.putFloat("radius",radius.getProgress());e.putFloat("spacing",spacing.getProgress()+4);e.putBoolean("dark",darkBox.isChecked());e.putInt("primary",tempPrimary[0]);if(!darkBox.isChecked()&&wasDark){e.putInt("background",Color.WHITE);e.putInt("card",Color.WHITE);e.putInt("text",Color.rgb(35,45,40));}else{e.putInt("background",tempBackground[0]);e.putInt("card",tempCard[0]);e.putInt("text",tempText[0]);}String np=newPin.getText().toString().trim();if(!np.isEmpty())e.putString("pin",np);e.apply();if(resetLogoRequested[0]){File f=new File(a.getFilesDir(),"company_logo.png");if(f.exists())f.delete();}dlg.dismiss();a.recreate();});
        cancel.setOnClickListener(v->dlg.dismiss());dlg.setOnCancelListener(d->{});dlg.show();
    }
    private static void addPalette(LinearLayout box,Activity a,String title,int[] holder,final Runnable[] refreshRef){
        TextView l=label(a,title);box.addView(l,lp());LinearLayout row=new LinearLayout(a);row.setOrientation(LinearLayout.HORIZONTAL);int[] colors={Color.WHITE,Color.BLACK,Color.rgb(8,127,91),Color.rgb(235,246,241),Color.rgb(35,45,40),Color.rgb(245,245,245),Color.rgb(30,90,150),Color.rgb(170,60,60),Color.rgb(220,170,50)};
        for(int col:colors){Button b=new Button(a);b.setText(" ");b.setBackgroundColor(col);b.setContentDescription(title);b.setOnClickListener(v->{holder[0]=col;if(refreshRef[0]!=null)refreshRef[0].run();Toast.makeText(a,title+" انتخاب شد",Toast.LENGTH_SHORT).show();});row.addView(b,new LinearLayout.LayoutParams(0,46,1));}box.addView(row,lp());
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
