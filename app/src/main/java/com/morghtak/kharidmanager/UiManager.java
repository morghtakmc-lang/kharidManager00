package com.morghtak.kharidmanager;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.os.Build;
import android.view.*;
import android.widget.*;
import android.text.*;
import androidx.core.widget.NestedScrollView;
import java.io.*;

public final class UiManager {
    private static final String PREF="ui_settings";
    private static final String DEFAULT_PIN="1234";
    public static final int PICK_LOGO_REQUEST=72;
    private UiManager(){}

    public static int primary(Context c){return p(c).getInt("primary",Color.rgb(8,127,91));}
    public static int secondary(Context c){return dark(c)?Color.rgb(55,95,80):p(c).getInt("secondary",Color.rgb(235,246,241));}
    public static int background(Context c){return dark(c)?Color.rgb(18,18,18):p(c).getInt("background",Color.WHITE);}
    public static int card(Context c){return dark(c)?Color.rgb(30,30,30):p(c).getInt("card",Color.WHITE);}
    public static int text(Context c){return dark(c)?Color.WHITE:p(c).getInt("text",Color.rgb(35,45,40));}
    public static int textSecondary(Context c){return dark(c)?Color.rgb(205,205,205):p(c).getInt("text2",Color.rgb(90,100,95));}

    // Automatic sizes. Manager settings never control these values.
    private static float widthDp(Context c){
        float d=c.getResources().getDisplayMetrics().density;
        return c.getResources().getDisplayMetrics().widthPixels/Math.max(d,1f);
    }
    private static float clamp(float v,float lo,float hi){return Math.max(lo,Math.min(hi,v));}
    public static float bodySize(Context c){return clamp(15.2f*(widthDp(c)/360f),13.2f,17f);}
    public static float titleSize(Context c){return clamp(20.5f*(widthDp(c)/360f),18f,23f);}
    public static float buttonSize(Context c){return clamp(14.3f*(widthDp(c)/360f),12.6f,16.2f);}
    public static float fieldSize(Context c){return clamp(14.5f*(widthDp(c)/360f),13f,16.2f);}
    public static float radius(Context c){return clamp(14f*(widthDp(c)/360f),10f,18f);}
    public static float spacing(Context c){return clamp(8f*(widthDp(c)/360f),6f,11f);}
    public static String font(Context c){return "sans-serif";}
    public static boolean dark(Context c){return p(c).getBoolean("dark",false);}
    private static SharedPreferences p(Context c){return c.getSharedPreferences(PREF,0);}
    public static int dp(Context c,float v){return Math.round(v*c.getResources().getDisplayMetrics().density);}
    private static GradientDrawable makeBg(Context c,int color,float radiusDp,int strokeColor,boolean stroke){
        GradientDrawable g=new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(c,radiusDp));
        if(stroke)g.setStroke(dp(c,1),strokeColor);
        return g;
    }
    // Kept for source compatibility; callers in MainActivity use fieldBg directly.
    static GradientDrawable bgCompat(Context c,int color,float radiusDp,int strokeColor,boolean stroke){
        return makeBg(c,color,radiusDp,strokeColor,stroke);
    }
    public static Typeface selectedTypeface(Context c,int style){return Typeface.create("sans-serif",style);}

    public static void styleText(Context c,TextView v){
        v.setFontFeatureSettings("kern");
        v.setIncludeFontPadding(true);
        v.setBreakStrategy(android.text.Layout.BREAK_STRATEGY_HIGH_QUALITY);
        if(v instanceof Button){
            v.setTextSize(buttonSize(c));
            v.setTypeface(selectedTypeface(c,Typeface.BOLD));
            v.setTextColor(Color.WHITE);
            v.setGravity(Gravity.CENTER);
            v.setPadding(dp(c,10),dp(c,8),dp(c,10),dp(c,8));
            v.setMinHeight(dp(c,48));
            v.setMinWidth(0);
            v.setMaxLines(4);
            v.setHorizontallyScrolling(false);
            v.setEllipsize(null);
            v.setBackground(makeBg(c,primary(c),radius(c),primary(c),false));
            v.setAllCaps(false);
        } else if(v instanceof EditText){
            v.setTextSize(fieldSize(c));
            v.setTypeface(selectedTypeface(c,Typeface.NORMAL));
            v.setTextColor(text(c));
            v.setHintTextColor(textSecondary(c));
            v.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
            v.setPadding(dp(c,12),dp(c,8),dp(c,12),dp(c,8));
            v.setSingleLine(true);
            v.setMinHeight(dp(c,48));
            v.setHorizontallyScrolling(false);
            v.setBackground(makeBg(c,card(c),radius(c),secondary(c),true));
        } else {
            v.setTextSize(bodySize(c));
            v.setTypeface(selectedTypeface(c,Typeface.NORMAL));
            v.setTextColor(text(c));
            v.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
            v.setLineSpacing(0,1.15f);
            v.setMaxLines(10);
            v.setHorizontallyScrolling(false);
        }
    }

    public static void decorate(final Activity a,final LinearLayout root,final String title){
        final Context c=a;
        root.setBackgroundColor(background(c));
        root.setPadding(dp(c,8),dp(c,6),dp(c,8),dp(c,14));
        if(root.getChildCount()>0&&root.getChildAt(0) instanceof TextView){
            TextView h=(TextView)root.getChildAt(0);
            h.setTextSize(titleSize(c));
            h.setTypeface(selectedTypeface(c,Typeface.BOLD));
            h.setTextColor(Color.WHITE);
            h.setGravity(Gravity.CENTER);
            h.setPadding(dp(c,12),dp(c,7),dp(c,12),dp(c,7));
            h.setMinHeight(dp(c,54));
            h.setMaxLines(2);
            h.setHorizontallyScrolling(false);
            h.setBackground(makeBg(c,primary(c),radius(c),primary(c),false));
            ViewGroup.LayoutParams hp=h.getLayoutParams();
            if(hp!=null)hp.height=ViewGroup.LayoutParams.WRAP_CONTENT;
        }
        if("مدیریت خرید و سررسید".equals(title)){
            ImageView logo=new ImageView(c);
            logo.setImageDrawable(loadLogo(c));
            logo.setAdjustViewBounds(true);
            logo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            logo.setPadding(0,dp(c,5),0,dp(c,5));
            root.addView(logo,1,new LinearLayout.LayoutParams(-1,dp(c,Math.min(155,Math.max(105,widthDp(c)*0.40f)))));
            Button settings=new Button(c);
            settings.setText("⚙ تنظیمات مدیر");
            styleText(c,settings);
            settings.setOnClickListener(v->showSettings(a));
            root.addView(settings,new LinearLayout.LayoutParams(-1,ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        styleTree(c,root);
        NestedScrollView scroll=new NestedScrollView(c);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        ViewGroup parent=(ViewGroup)root.getParent();
        if(parent!=null)parent.removeView(root);
        scroll.addView(root,new ViewGroup.LayoutParams(-1,-2));
        a.setContentView(scroll);
        if(Build.VERSION.SDK_INT>=21){
            a.getWindow().setStatusBarColor(primary(c));
            a.getWindow().setNavigationBarColor(dark(c)?Color.BLACK:background(c));
        }
    }

    private static Drawable loadLogo(Context c){
        File f=new File(c.getFilesDir(),"company_logo.png");
        if(f.exists())return Drawable.createFromPath(f.getAbsolutePath());
        int id=c.getResources().getIdentifier("company_logo","drawable",c.getPackageName());
        return id==0?new ColorDrawable(background(c)):c.getResources().getDrawable(id);
    }

    private static void styleTree(Context c,View v){
        if(v instanceof TextView)styleText(c,(TextView)v);
        if(v instanceof Spinner){
            v.setMinimumHeight(dp(c,46));
            v.setBackground(makeBg(c,card(c),radius(c),secondary(c),true));
        }
        if(v instanceof ViewGroup){
            ViewGroup g=(ViewGroup)v;
            for(int i=0;i<g.getChildCount();i++){
                View child=g.getChildAt(i);
                styleTree(c,child);
                ViewGroup.LayoutParams lp=child.getLayoutParams();
                if(lp instanceof ViewGroup.MarginLayoutParams){
                    ViewGroup.MarginLayoutParams mlp=(ViewGroup.MarginLayoutParams)lp;
                    mlp.bottomMargin=dp(c,spacing(c));
                    child.setLayoutParams(mlp);
                }
                if(child instanceof Button){
                    ViewGroup.LayoutParams bp=child.getLayoutParams();
                    if(bp!=null && !(g instanceof LinearLayout && ((LinearLayout)g).getOrientation()==LinearLayout.HORIZONTAL && bp.width==0)){
                        bp.height=ViewGroup.LayoutParams.WRAP_CONTENT;
                        child.setLayoutParams(bp);
                    }
                }
            }
        }
    }

    public static void showSettings(final Activity a){
        final String storedPin=p(a).getString("pin",DEFAULT_PIN);
        final EditText pin=edit(a,"رمز مدیر");
        pin.setInputType(2|0x00000010);
        AlertDialog d=new AlertDialog.Builder(a).setTitle("دسترسی مدیر").setMessage("این بخش مخصوص مدیر است.")
            .setView(pin).setPositiveButton("ورود",(x,w)->{
                if(!storedPin.equals(pin.getText().toString()))
                    Toast.makeText(a,"رمز مدیر اشتباه است",Toast.LENGTH_SHORT).show();
                else openSettingsPanel(a);
            }).setNegativeButton("لغو",null).create();
        styleDialog(d,a);d.show();
    }

    private static void openSettingsPanel(final Activity a){
        final LinearLayout box=new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(a,10),dp(a,4),dp(a,10),dp(a,12));
        ScrollView sv=new ScrollView(a);sv.addView(box);
        addTitle(box,a,"تنظیمات مدیر");
        addInfo(box,a,"اندازه نوشته‌ها، کلیدها و فاصله‌ها در نرم‌افزار به‌صورت خودکار با اندازه صفحه تنظیم می‌شوند و نیازی به تنظیم دستی ندارند.");
        addInfo(box,a,"تنظیمات رنگ، حالت تاریک و لوگو در این بخش قابل تغییر است.");
        final int[] tempPrimary={primary(a)},tempBackground={background(a)},tempCard={card(a)},tempText={text(a)};
        addPalette(box,a,"رنگ اصلی",tempPrimary);
        addPalette(box,a,"رنگ پس‌زمینه",tempBackground);
        addPalette(box,a,"رنگ کارت/فیلد",tempCard);
        addPalette(box,a,"رنگ متن",tempText);

        final CheckBox darkBox=new CheckBox(a);
        darkBox.setText("حالت تاریک");
        darkBox.setTextColor(text(a));
        darkBox.setChecked(dark(a));
        box.addView(darkBox,lp());

        Button logo=button(a,"🖼 انتخاب لوگوی داخل برنامه");
        logo.setOnClickListener(v->a.startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE),PICK_LOGO_REQUEST));
        box.addView(logo,lp());

        Button resetLogo=button(a,"↺ بازگردانی لوگوی اصلی شرکت");
        resetLogo.setOnClickListener(v->{File f=new File(a.getFilesDir(),"company_logo.png");if(f.exists())f.delete();Toast.makeText(a,"لوگوی اصلی فعال شد",Toast.LENGTH_SHORT).show();a.recreate();});
        box.addView(resetLogo,lp());

        final EditText newPin=edit(a,"رمز جدید مدیر (خالی = بدون تغییر)");
        newPin.setInputType(2|0x00000010);box.addView(newPin,lp());

        Button save=button(a,"✓ ذخیره و اعمال تنظیمات");
        Button cancel=button(a,"لغو");
        box.addView(save,lp());box.addView(cancel,lp());

        AlertDialog dlg=new AlertDialog.Builder(a).setTitle("تنظیمات مدیر").setView(sv).create();
        save.setOnClickListener(v->{
            SharedPreferences.Editor e=p(a).edit();
            e.putBoolean("dark",darkBox.isChecked());
            e.putInt("primary",tempPrimary[0]);
            e.putInt("background",tempBackground[0]);
            e.putInt("card",tempCard[0]);
            e.putInt("text",tempText[0]);
            String np=newPin.getText().toString().trim();
            if(!np.isEmpty())e.putString("pin",np);
            e.apply();dlg.dismiss();a.recreate();
        });
        cancel.setOnClickListener(v->dlg.dismiss());
        dlg.setOnShowListener(x->{
            Window w=dlg.getWindow();
            if(w!=null)w.setBackgroundDrawable(makeBg(a,background(a),radius(a),textSecondary(a),true));
            Button pos=dlg.getButton(AlertDialog.BUTTON_POSITIVE),neg=dlg.getButton(AlertDialog.BUTTON_NEGATIVE);
            if(pos!=null){pos.setTextColor(primary(a));pos.setAllCaps(false);}
            if(neg!=null){neg.setTextColor(textSecondary(a));neg.setAllCaps(false);}
        });
        dlg.show();
    }

    private static void addPalette(LinearLayout box,Activity a,String title,int[] holder){
        TextView l=label(a,title);box.addView(l,lp());
        LinearLayout row=new LinearLayout(a);row.setOrientation(LinearLayout.HORIZONTAL);
        int[] colors={Color.WHITE,Color.BLACK,Color.rgb(8,127,91),Color.rgb(235,246,241),Color.rgb(35,45,40),Color.rgb(245,245,245),Color.rgb(30,90,150),Color.rgb(170,60,60),Color.rgb(220,170,50)};
        for(int col:colors){
            Button b=new Button(a);b.setText(" ");b.setContentDescription(title);
            b.setOnClickListener(v->{holder[0]=col;Toast.makeText(a,title+" انتخاب شد",Toast.LENGTH_SHORT).show();});
            row.addView(b,new LinearLayout.LayoutParams(0,dp(a,42),1));
        }
        box.addView(row,lp());
    }

    public static void styleDialog(AlertDialog d,Context c){
        if(d==null)return;
        d.setOnShowListener(x->{
            Window w=d.getWindow();
            if(w!=null)w.setBackgroundDrawable(makeBg(c,background(c),radius(c),textSecondary(c),true));
            TextView msg=d.findViewById(android.R.id.message);
            if(msg!=null){msg.setTextColor(text(c));msg.setTextSize(bodySize(c));msg.setGravity(Gravity.RIGHT);msg.setLineSpacing(0,1.15f);}
            Button p=d.getButton(AlertDialog.BUTTON_POSITIVE),n=d.getButton(AlertDialog.BUTTON_NEGATIVE),ne=d.getButton(AlertDialog.BUTTON_NEUTRAL);
            if(p!=null){p.setTextColor(primary(c));p.setAllCaps(false);p.setMaxLines(2);p.setHorizontallyScrolling(false);}
            if(n!=null){n.setTextColor(textSecondary(c));n.setAllCaps(false);n.setMaxLines(2);n.setHorizontallyScrolling(false);}
            if(ne!=null){ne.setTextColor(textSecondary(c));ne.setAllCaps(false);ne.setMaxLines(2);ne.setHorizontallyScrolling(false);}
        });
    }

    public static void handleLogoResult(Activity a,int requestCode,int resultCode,Intent data){
        if(requestCode!=PICK_LOGO_REQUEST||resultCode!=Activity.RESULT_OK||data==null||data.getData()==null)return;
        try{
            InputStream in=a.getContentResolver().openInputStream(data.getData());
            if(in==null)throw new IOException("Cannot open image");
            File f=new File(a.getFilesDir(),"company_logo.png");
            FileOutputStream out=new FileOutputStream(f);
            byte[] buf=new byte[4096];int n;
            while((n=in.read(buf))>0)out.write(buf,0,n);
            in.close();out.close();
            Toast.makeText(a,"لوگو با موفقیت انتخاب شد",Toast.LENGTH_SHORT).show();
            a.recreate();
        }catch(Exception e){Toast.makeText(a,"خطا در انتخاب لوگو",Toast.LENGTH_SHORT).show();}
    }

    private static EditText edit(Activity a,String hint){
        EditText e=new EditText(a);e.setHint(hint);e.setSingleLine(true);e.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);styleText(a,e);return e;
    }
    private static Button button(Activity a,String s){Button b=new Button(a);b.setText(s);b.setAllCaps(false);styleText(a,b);return b;}
    private static TextView label(Activity a,String s){TextView t=new TextView(a);t.setText(s);styleText(a,t);return t;}
    private static void addTitle(LinearLayout b,Activity a,String s){TextView t=label(a,s);t.setTextSize(titleSize(a));t.setTypeface(selectedTypeface(a,Typeface.BOLD));b.addView(t,lp());}
    private static void addInfo(LinearLayout b,Activity a,String s){b.addView(label(a,s),lp());}
    private static LinearLayout.LayoutParams lp(){return new LinearLayout.LayoutParams(-1,ViewGroup.LayoutParams.WRAP_CONTENT);}
}
