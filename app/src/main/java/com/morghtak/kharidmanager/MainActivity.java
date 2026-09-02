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
import android.widget.*;
import android.text.*;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import org.json.*;
import java.io.*;
import java.text.*;
import java.util.*;

public class MainActivity extends Activity {

    LinearLayout root;
    JSONObject data;
    int green = Color.rgb(8, 127, 91);
    ArrayList<EditText> inputs = new ArrayList<>();
    Spinner buyerSp, commoditySp, paymentSp, companySp;

    String[] labels = {
        "نام خریدار",
        "نهاده",
        "شماره خرید",
        "وزن (کیلوگرم)",
        "فی (ریال)",
        "قیمت توافقی (ریال)",
        "نوع پرداخت",
        "مبلغ خرید (ریال)",
        "تاریخ خرید",
        "مقدار ذرت (کیلوگرم)",
        "مقدار سویا (کیلوگرم)",
        "نام شرکت",
        "تاریخ اصلی سررسید",
        "تاریخ فرعی سررسید"
    };

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);

        data = AppData.root(this);

        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    22
            );
        }

        home();
    }

    TextView tv(String s, int size) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(Color.DKGRAY);
        t.setPadding(24, 18, 24, 18);
        return t;
    }

    Button btn(String s) {
        Button b = new Button(this);
        b.setText(s);
        b.setTextColor(Color.WHITE);
        b.setBackgroundColor(green);
        b.setAllCaps(false);
        b.setTextSize(15);
        return b;
    }

    void base(String title) {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);

        TextView h = tv(title, 21);
        h.setTextColor(Color.WHITE);
        h.setGravity(Gravity.CENTER);
        h.setBackgroundColor(green);

        root.addView(
                h,
                new LinearLayout.LayoutParams(-1, 70)
        );

        setContentView(root);
    }

    void home() {
        base("مدیریت خرید و سررسید");

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(20, 20, 20, 20);

        JSONArray p = AppData.arr(data, "purchases");

        int due = 0;

        for (int i = 0; i < p.length(); i++) {
            if (isSoon(p.optJSONObject(i))) {
                due++;
            }
        }

        body.addView(tv("خلاصه کلی", 19));

        body.addView(
                tv(
                        "تعداد خریدها: " + p.length()
                                + "\nسررسیدهای نزدیک: " + due,
                        17
                )
        );

        Button n = btn("➕ ثبت خرید جدید");
        n.setOnClickListener(v -> form(null));
        body.addView(n);

        Button l = btn("🛒 خریدهای ثبت‌شده");
        l.setOnClickListener(v -> listPurchases());
        body.addView(l);

        Button r = btn("🔔 سررسیدها");
        r.setOnClickListener(v -> deadlines());
        body.addView(r);

        Button b = btn("👤 خریداران");
        b.setOnClickListener(v -> manage("buyers", "خریداران"));
        body.addView(b);

        Button c = btn("🏢 شرکت‌ها");
        c.setOnClickListener(v -> manage("companies", "شرکت‌ها"));
        body.addView(c);

        Button m = btn("🌾 نهاده‌ها");
        m.setOnClickListener(v -> manage("commodities", "نهاده‌ها"));
        body.addView(m);

        Button rep = btn("📊 گزارش‌ها و خروجی Excel");
        rep.setOnClickListener(v -> reports());
        body.addView(rep);

        Button set = btn("💾 پشتیبان‌گیری / بازیابی");
        set.setOnClickListener(v -> backup());
        body.addView(set);

        root.addView(
                body,
                new LinearLayout.LayoutParams(-1, 0, 1)
        );
    }

    boolean isSoon(JSONObject p) {
        if (p == null) {
            return false;
        }

        String d = p.optString("subDue");

        if (d.isEmpty()) {
            return false;
        }

        long m = PersianDate.millis(d, "23:59");

        return m > System.currentTimeMillis()
                && m - System.currentTimeMillis()
                < 7L * 86400000L;
    }

    EditText input(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setTextSize(15);
        e.setSingleLine(true);
        e.setPadding(16, 8, 16, 8);

        inputs.add(e);

        root.addView(
                e,
                new LinearLayout.LayoutParams(-1, 58)
        );

        return e;
    }

    void form(JSONObject old) {

        base(
                old == null
                        ? "ثبت خرید جدید"
                        : "ویرایش خرید"
        );

        inputs.clear();

        JSONObject p = old;

        for (int i = 0; i < labels.length; i++) {

            TextView lab = tv(
                    (i + 1) + ". " + labels[i],
                    14
            );

            root.addView(lab);

            if (i == 0) {

                buyerSp = spinner(
                        AppData.arr(data, "buyers")
                );

                root.addView(buyerSp);

                EditText hidden = new EditText(this);
                hidden.setVisibility(View.GONE);
                inputs.add(hidden);

            } else if (i == 1) {

                commoditySp = spinner(
                        AppData.arr(data, "commodities")
                );

                root.addView(commoditySp);

                EditText hidden = new EditText(this);
                hidden.setVisibility(View.GONE);
                inputs.add(hidden);

            } else if (i == 6) {

                paymentSp = spinner(AppData.PAYMENTS);

                root.addView(paymentSp);

                EditText hidden = new EditText(this);
                hidden.setVisibility(View.GONE);
                inputs.add(hidden);

            } else if (i == 11) {

                companySp = spinner(
                        AppData.arr(data, "companies")
                );

                root.addView(companySp);

                EditText hidden = new EditText(this);
                hidden.setVisibility(View.GONE);
                inputs.add(hidden);

            } else {

                EditText e = input(labels[i]);

                if (i == 8 || i == 12 || i == 13) {
                    e.setOnClickListener(v -> pickDate(e));
                }

                if (i == 3 || i == 4 || i == 5 ||
                        i == 7 || i == 9 || i == 10) {

                    e.setInputType(2);
                    addGrouping(e);
                }
            }
        }

        if (p != null) {
            fill(p);
        } else {
            inputs.get(8).setText(
                    PersianDate.today()
            );
        }

        Button alarm = btn("🔔 تنظیم هشدار تاریخ فرعی");

        alarm.setOnClickListener(
                v -> alarmDialog(
                        inputs.get(13),
                        p
                )
        );

        root.addView(alarm);

        Button save = btn("✓ ذخیره خرید");

        save.setOnClickListener(
                v -> savePurchase(old)
        );

        root.addView(save);

        Button back = btn("← بازگشت");

        back.setOnClickListener(
                v -> home()
        );

        root.addView(back);
    }

    EditText asEdit(Spinner s) {
        EditText e = new EditText(this);
        e.setVisibility(View.GONE);
        return e;
    }

    Spinner spinner(JSONArray a) {

        Spinner s = new Spinner(this);

        ArrayList<String> x =
                new ArrayList<>();

        for (int i = 0; i < a.length(); i++) {
            x.add(a.optString(i));
        }

        s.setAdapter(
                new ArrayAdapter<String>(
                        this,
                        android.R.layout.simple_spinner_dropdown_item,
                        x
                )
        );

        s.setPadding(12, 0, 12, 0);

        return s;
    }

    Spinner spinner(String[] a) {

        Spinner s = new Spinner(this);

        s.setAdapter(
                new ArrayAdapter<String>(
                        this,
                        android.R.layout.simple_spinner_dropdown_item,
                        a
                )
        );

        return s;
    }

    void addGrouping(final EditText e) {

        e.addTextChangedListener(
                new TextWatcher() {

                    boolean busy = false;

                    public void beforeTextChanged(
                            CharSequence s,
                            int st,
                            int c,
                            int a
                    ) {
                    }

                    public void onTextChanged(
                            CharSequence s,
                            int st,
                            int b,
                            int c
                    ) {
                    }

                    public void afterTextChanged(
                            Editable ed
                    ) {

                        if (busy) {
                            return;
                        }

                        String raw =
                                AppData.digits(
                                        ed.toString()
                                );

                        if (raw.isEmpty()) {
                            return;
                        }

                        busy = true;

                        String f =
                                AppData.fmt(raw);

                        e.setText(f);
                        e.setSelection(f.length());

                        busy = false;
                    }
                }
        );
    }

    void pickDate(EditText e) {

        LinearLayout l =
                new LinearLayout(this);

        l.setPadding(20, 10, 20, 10);

        String[] a = {
                "سال",
                "ماه",
                "روز"
        };

        EditText[] x =
                new EditText[3];

        for (int i = 0; i < 3; i++) {

            x[i] = new EditText(this);

            x[i].setHint(a[i]);
            x[i].setInputType(2);

            l.addView(
                    x[i],
                    new LinearLayout.LayoutParams(
                            0,
                            60,
                            1
                    )
            );
        }

        new AlertDialog.Builder(this)
                .setTitle("انتخاب تاریخ شمسی")
                .setView(l)
                .setPositiveButton(
                        "تأیید",
                        (d, w) -> {

                            String y =
                                    x[0].getText().toString();

                            String m =
                                    x[1].getText().toString();

                            String day =
                                    x[2].getText().toString();

                            if (y.length() == 0) {
                                y = PersianDate.today()
                                        .substring(0, 4);
                            }

                            if (m.length() == 0) {
                                m = "01";
                            }

                            if (day.length() == 0) {
                                day = "01";
                            }

                            e.setText(
                                    String.format(
                                            Locale.US,
                                            "%s/%02d/%02d",
                                            y,
                                            Integer.parseInt(m),
                                            Integer.parseInt(day)
                                    )
                            );
                        }
                )
                .setNegativeButton(
                        "لغو",
                        null
                )
                .show();
    }

    void fill(JSONObject p) {

        String[] keys = {
                "buyer",
                "commodity",
                "purchaseNo",
                "weight",
                "fee",
                "agreed",
                "payment",
                "amount",
                "buyDate",
                "corn",
                "soy",
                "company",
                "mainDue",
                "subDue"
        };

        for (
                int i = 0;
                i < keys.length && i < inputs.size();
                i++
        ) {

            if (i != 0 &&
                    i != 1 &&
                    i != 6 &&
                    i != 11) {

                inputs.get(i).setText(
                        p.optString(
                                keys[i],
                                ""
                        )
                );
            }
        }

        setSpinner(
                buyerSp,
                p.optString("buyer")
        );

        setSpinner(
                commoditySp,
                p.optString("commodity")
        );

        setSpinner(
                paymentSp,
                p.optString("payment")
        );

        setSpinner(
                companySp,
                p.optString("company")
        );
    }

    void setSpinner(
            Spinner s,
            String v
    ) {

        if (s == null) {
            return;
        }

        for (int i = 0; i < s.getCount(); i++) {

            if (
                    String.valueOf(
                            s.getItemAtPosition(i)
                    ).equals(v)
            ) {

                s.setSelection(i);
                break;
            }
        }
    }

    void savePurchase(JSONObject old) {

        try {

            String buyer =
                    buyerSp == null
                            ? ""
                            : String.valueOf(
                                    buyerSp.getSelectedItem()
                            );

            String commodity =
                    commoditySp == null
                            ? ""
                            : String.valueOf(
                                    commoditySp.getSelectedItem()
                            );

            String company =
                    companySp == null
                            ? ""
                            : String.valueOf(
                                    companySp.getSelectedItem()
                            );

            JSONObject r =
                    new JSONObject();

            String[] keys = {
                    "buyer",
                    "commodity",
                    "purchaseNo",
                    "weight",
                    "fee",
                    "agreed",
                    "payment",
                    "amount",
                    "buyDate",
                    "corn",
                    "soy",
                    "company",
                    "mainDue",
                    "subDue"
            };

            for (int i = 0; i < keys.length; i++) {

                String v =
                        inputs.get(i)
                                .getText()
                                .toString();

                if (i == 0) {
                    v = buyer;
                }

                if (i == 1) {
                    v = commodity;
                }

                if (i == 6) {
                    v = String.valueOf(
                            paymentSp.getSelectedItem()
                    );
                }

                if (i == 11) {
                    v = company;
                }

                r.put(keys[i], v);
            }

            r.put(
                    "id",
                    old == null
                            ? UUID.randomUUID().toString()
                            : old.optString("id")
            );

            r.put(
                    "alarm",
                    old != null &&
                            old.optBoolean(
                                    "alarm",
                                    false
                            )
            );

            r.put(
                    "alarmDays",
                    old == null
                            ? 1
                            : old.optInt(
                                    "alarmDays",
                                    1
                            )
            );

            r.put(
                    "alarmTime",
                    old == null
                            ? "10:00"
                            : old.optString(
                                    "alarmTime",
                                    "10:00"
                            )
            );

            JSONArray a =
                    AppData.arr(
                            data,
                            "purchases"
                    );

            if (old != null) {

                for (int i = 0; i < a.length(); i++) {

                    if (
                            a.getJSONObject(i)
                                    .optString("id")
                                    .equals(
                                            r.optString("id")
                                    )
                    ) {

                        a.put(i, r);
                    }
                }

            } else {

                a.put(r);
            }

            data.put(
                    "purchases",
                    a
            );

            addUnique(
                    "buyers",
                    buyer
            );

            addUnique(
                    "companies",
                    company
            );

            AppData.save(
                    this,
                    data
            );

            if (r.optBoolean("alarm")) {
                schedule(this, r);
            }

            Toast.makeText(
                    this,
                    "خرید با موفقیت ذخیره شد",
                    Toast.LENGTH_SHORT
            ).show();

            home();

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "خطا در ذخیره اطلاعات",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    void addUnique(
            String key,
            String val
    ) throws Exception {

        if (val.trim().isEmpty()) {
            return;
        }

        JSONArray a =
                AppData.arr(
                        data,
                        key
                );

        for (int i = 0; i < a.length(); i++) {

            if (
                    a.optString(i)
                            .equals(val)
            ) {
                return;
            }
        }

        a.put(val);
    }

    public static void schedule(
            Context c,
            JSONObject p
    ) {

        try {

            if (!p.optBoolean("alarm", false)) {
                return;
            }

            long due =
                    PersianDate.millis(
                            p.optString("subDue"),
                            p.optString(
                                    "alarmTime",
                                    "10:00"
                            )
                    );

            due -=
                    p.optInt(
                            "alarmDays",
                            1
                    ) * 86400000L;

            if (due <= System.currentTimeMillis()) {
                return;
            }

            AlarmManager am =
                    (AlarmManager)
                            c.getSystemService(
                                    ALARM_SERVICE
                            );

            Intent in =
                    new Intent(
                            c,
                            AlarmReceiver.class
                    );

            in.putExtra(
                    "id",
                    p.optString("id")
            );

            in.putExtra(
                    "title",
                    "خرید شماره "
                            + p.optString("purchaseNo")
                            + " - "
                            + p.optString("buyer")
            );

            PendingIntent pi =
                    PendingIntent.getBroadcast(
                            c,
                            p.optString("id").hashCode(),
                            in,
                            PendingIntent.FLAG_UPDATE_CURRENT
                                    | PendingIntent.FLAG_IMMUTABLE
                    );

            if (
                    Build.VERSION.SDK_INT >= 31
                            && am.canScheduleExactAlarms()
            ) {

                am.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        due,
                        pi
                );

            } else {

                am.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        due,
                        pi
                );
            }

        } catch (Exception ignored) {
        }
    }

    void alarmDialog(
            EditText sub,
            JSONObject old
    ) {

        if (
                sub.getText()
                        .toString()
                        .trim()
                        .isEmpty()
        ) {

            Toast.makeText(
                    this,
                    "ابتدا تاریخ فرعی را وارد کنید",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        LinearLayout l =
                new LinearLayout(this);

        l.setOrientation(
                LinearLayout.VERTICAL
        );

        EditText days =
                new EditText(this);

        days.setHint(
                "چند روز قبل؟"
        );

        days.setInputType(2);

        EditText time =
                new EditText(this);

        time.setHint(
                "ساعت هشدار مثل 10:00"
        );

        time.setInputType(2);

        l.addView(days);
        l.addView(time);

        new AlertDialog.Builder(this)
                .setTitle("هشدار این سفارش")
                .setView(l)
                .setPositiveButton(
                        "فعال",
                        (d, w) -> {

                            try {

                                JSONObject p =
                                        old == null
                                                ? new JSONObject()
                                                : old;

                                p.put(
                                        "alarm",
                                        true
                                );

                                p.put(
                                        "alarmDays",
                                        Integer.parseInt(
                                                days.getText()
                                                        .toString()
                                        )
                                );

                                p.put(
                                        "alarmTime",
                                        time.getText()
                                                .toString()
                                );

                                p.put(
                                        "subDue",
                                        sub.getText()
                                                .toString()
                                );

                                saveAlarmOnly(p);

                            } catch (Exception e) {

                                Toast.makeText(
                                        this,
                                        "مقادیر هشدار نامعتبر است",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        }
                )
                .setNegativeButton(
                        "لغو",
                        null
                )
                .show();
    }

    void saveAlarmOnly(JSONObject p)
            throws Exception {

        if (
                p.optString("id")
                        .isEmpty()
        ) {

            Toast.makeText(
                    this,
                    "ابتدا خرید را ذخیره کنید و سپس هشدار را تنظیم کنید",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        JSONArray a =
                AppData.arr(
                        data,
                        "purchases"
                );

        for (int i = 0; i < a.length(); i++) {

            if (
                    a.getJSONObject(i)
                            .optString("id")
                            .equals(
                                    p.optString("id")
                            )
            ) {

                a.put(i, p);
            }
        }

        data.put(
                "purchases",
                a
        );

        AppData.save(
                this,
                data
        );

        if (Build.VERSION.SDK_INT >= 31) {

            AlarmManager am =
                    (AlarmManager)
                            getSystemService(
                                    ALARM_SERVICE
                            );

            if (!am.canScheduleExactAlarms()) {

                startActivity(
                        new Intent(
                                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                        )
                );
            }
        }

        schedule(
                this,
                p
        );

        Toast.makeText(
                this,
                "آلارم این سفارش فعال شد",
                Toast.LENGTH_SHORT
        ).show();
    }

    void listPurchases() {

        base("خریدهای ثبت‌شده");

        EditText q =
                input(
                        "جستجو بر اساس نام، شماره خرید یا شرکت"
                );

        Button go =
                btn("🔎 جستجو");

        root.addView(go);

        LinearLayout list =
                new LinearLayout(this);

        list.setOrientation(
                LinearLayout.VERTICAL
        );

        root.addView(
                list,
                new LinearLayout.LayoutParams(
                        -1,
                        0,
                        1
                )
        );

        Runnable render = () -> {

            list.removeAllViews();

            String s =
                    q.getText()
                            .toString();

            JSONArray a =
                    AppData.arr(
                            data,
                            "purchases"
                    );

            for (
                    int i = a.length() - 1;
                    i >= 0;
                    i--
            ) {

                JSONObject p =
                        a.optJSONObject(i);

                if (p == null) {
                    continue;
                }

                if (
                        !s.isEmpty()
                                && !(
                                p.optString("buyer")
                                        + p.optString("purchaseNo")
                                        + p.optString("company")
                        ).contains(s)
                ) {
                    continue;
                }

                Button b =
                        btn(
                                "خرید "
                                        + p.optString("purchaseNo")
                                        + " | "
                                        + p.optString("buyer")
                                        + " | "
                                        + p.optString("subDue")
                        );

                b.setOnClickListener(
                        v -> details(p)
                );

                list.addView(b);
            }
        };

        go.setOnClickListener(
                v -> render.run()
        );

        render.run();

        Button back =
                btn("← بازگشت");

        back.setOnClickListener(
                v -> home()
        );

        root.addView(back);
    }

    void details(JSONObject p) {

        base("جزئیات خرید");

        for (String k : labels) {

            String key =
                    mapKey(k);

            root.addView(
                    tv(
                            k + ": "
                                    + p.optString(
                                    key,
                                    "-"
                            ),
                            15
                    )
            );
        }

        root.addView(
                tv(
                        "هشدار تاریخ فرعی: "
                                + (
                                p.optBoolean("alarm")
                                        ? "فعال - "
                                        + p.optInt("alarmDays")
                                        + " روز قبل، ساعت "
                                        + p.optString("alarmTime")
                                        : "غیرفعال"
                        ),
                        15
                )
        );

        Button ex =
                btn(
                        "📊 خروجی Excel همین سفارش"
                );

        ex.setOnClickListener(
                v -> exportExcel(
                        new JSONArray().put(p),
                        "purchase_"
                                + p.optString("purchaseNo")
                )
        );

        root.addView(ex);

        Button ed =
                btn("✏️ ویرایش");

        ed.setOnClickListener(
                v -> form(p)
        );

        root.addView(ed);

        Button del =
                btn("🗑 حذف");

        del.setOnClickListener(
                v -> delete(p)
        );

        root.addView(del);

        Button back =
                btn("← بازگشت");

        back.setOnClickListener(
                v -> listPurchases()
        );

        root.addView(back);
    }

    String mapKey(String l) {

        if (l.startsWith("نام خریدار"))
            return "buyer";

        if (l.startsWith("نهاده"))
            return "commodity";

        if (l.startsWith("شماره"))
            return "purchaseNo";

        if (l.startsWith("وزن"))
            return "weight";

        if (l.startsWith("فی"))
            return "fee";

        if (l.startsWith("قیمت توافقی"))
            return "agreed";

        if (l.startsWith("نوع"))
            return "payment";

        if (l.startsWith("مبلغ"))
            return "amount";

        if (l.startsWith("تاریخ خرید"))
            return "buyDate";

        if (l.startsWith("مقدار ذرت"))
            return "corn";

        if (l.startsWith("مقدار سویا"))
            return "soy";

        if (l.startsWith("نام شرکت"))
            return "company";

        if (l.startsWith("تاریخ اصلی"))
            return "mainDue";

        return "subDue";
    }

    void delete(JSONObject p) {

        try {

            JSONArray a =
                    AppData.arr(
                            data,
                            "purchases"
                    );

            JSONArray b =
                    new JSONArray();

            for (int i = 0; i < a.length(); i++) {

                if (
                        !a.getJSONObject(i)
                                .optString("id")
                                .equals(
                                        p.optString("id")
                                )
                ) {

                    b.put(
                            a.getJSONObject(i)
                    );
                }
            }

            data.put(
                    "purchases",
                    b
            );

            AppData.save(
                    this,
                    data
            );

            Toast.makeText(
                    this,
                    "حذف شد",
                    Toast.LENGTH_SHORT
            ).show();

            home();

        } catch (Exception ignored) {
        }
    }

    void deadlines() {

        base("سررسیدها");

        JSONArray a =
                AppData.arr(
                        data,
                        "purchases"
                );

        for (int i = 0; i < a.length(); i++) {

            JSONObject p =
                    a.optJSONObject(i);

            if (p != null) {

                root.addView(
                        tv(
                                "خرید "
                                        + p.optString("purchaseNo")
                                        + " | "
                                        + p.optString("buyer")
                                        + "\nسررسید فرعی: "
                                        + p.optString("subDue")
                                        + " | "
                                        + (
                                        p.optBoolean("alarm")
                                                ? "آلارم فعال"
                                                : "بدون آلارم"
                                ),
                                15
                        )
                );
            }
        }

        Button back =
                btn("← بازگشت");

        back.setOnClickListener(
                v -> home()
        );

        root.addView(back);
    }

    void manage(
            String key,
            String title
    ) {

        base(title);

        JSONArray a =
                AppData.arr(
                        data,
                        key
                );

        for (int i = 0; i < a.length(); i++) {

            final int ix = i;

            LinearLayout row =
                    new LinearLayout(this);

            row.setGravity(
                    Gravity.CENTER_VERTICAL
            );

            TextView t =
                    tv(
                            a.optString(i),
                            16
                    );

            row.addView(
                    t,
                    new LinearLayout.LayoutParams(
                            0,
                            65,
                            1
                    )
            );

            Button d =
                    btn("حذف");

            d.setOnClickListener(
                    v -> {

                        a.remove(ix);

                        try {

                            data.put(
                                    key,
                                    a
                            );

                            AppData.save(
                                    this,
                                    data
                            );

                        } catch (Exception ignored) {
                        }

                        manage(
                                key,
                                title
                        );
                    }
            );

            row.addView(
                    d,
                    new LinearLayout.LayoutParams(
                            110,
                            60
                    )
            );

            root.addView(row);
        }

        Button add =
                btn("➕ افزودن");

        add.setOnClickListener(
                v -> {

                    EditText e =
                            new EditText(this);

                    new AlertDialog.Builder(this)
                            .setTitle("افزودن")
                            .setView(e)
                            .setPositiveButton(
                                    "ذخیره",
                                    (d, w) -> {

                                        try {

                                            if (
                                                    !e.getText()
                                                            .toString()
                                                            .trim()
                                                            .isEmpty()
                                            ) {

                                                a.put(
                                                        e.getText()
                                                                .toString()
                                                                .trim()
                                                );

                                                data.put(
                                                        key,
                                                        a
                                                );

                                                AppData.save(
                                                        this,
                                                        data
                                                );
                                            }

                                            manage(
                                                    key,
                                                    title
                                            );

                                        } catch (Exception ignored) {
                                        }
                                    }
                            )
                            .show();
                }
        );

        root.addView(add);

        Button back =
                btn("← بازگشت");

        back.setOnClickListener(
                v -> home()
        );

        root.addView(back);
    }

    void reports() {

        base("گزارش‌ها");

        EditText buyer =
                input("خریدار (اختیاری)");

        EditText company =
                input("شرکت (اختیاری)");

        EditText from =
                input("از تاریخ شمسی (اختیاری)");

        EditText to =
                input("تا تاریخ شمسی (اختیاری)");

        Button show =
                btn("نمایش گزارش");

        root.addView(show);

        TextView out =
                tv("", 15);

        root.addView(out);

        show.setOnClickListener(
                v -> {

                    JSONArray r =
                            new JSONArray();

                    JSONArray a =
                            AppData.arr(
                                    data,
                                    "purchases"
                            );

                    long total = 0;

                    for (int i = 0; i < a.length(); i++) {

                        JSONObject p =
                                a.optJSONObject(i);

                        if (p == null) {
                            continue;
                        }

                        if (
                                !buyer.getText()
                                        .toString()
                                        .isEmpty()
                                        && !p.optString("buyer")
                                        .equals(
                                                buyer.getText()
                                                        .toString()
                                        )
                        ) {
                            continue;
                        }

                        if (
                                !company.getText()
                                        .toString()
                                        .isEmpty()
                                        && !p.optString("company")
                                        .equals(
                                                company.getText()
                                                        .toString()
                                        )
                        ) {
                            continue;
                        }

                        String d =
                                p.optString("buyDate");

                        if (
                                !from.getText()
                                        .toString()
                                        .isEmpty()
                                        && d.compareTo(
                                        from.getText()
                                                .toString()
                                ) < 0
                        ) {
                            continue;
                        }

                        if (
                                !to.getText()
                                        .toString()
                                        .isEmpty()
                                        && d.compareTo(
                                        to.getText()
                                                .toString()
                                ) > 0
                        ) {
                            continue;
                        }

                        r.put(p);

                        try {

                            total +=
                                    Long.parseLong(
                                            AppData.digits(
                                                    p.optString(
                                                            "amount",
                                                            "0"
                                                    )
                                            )
                                    );

                        } catch (Exception ignored) {
                        }
                    }

                    out.setText(
                            "تعداد سفارش: "
                                    + r.length()
                                    + "\nمجموع مبلغ: "
                                    + AppData.fmt(
                                    "" + total
                            )
                                    + " ریال"
                    );

                    show.setTag(r);
                }
        );

        Button ex =
                btn("📊 خروجی Excel گزارش");

        ex.setOnClickListener(
                v -> {

                    Object tag =
                            show.getTag();

                    if (tag instanceof JSONArray) {

                        exportExcel(
                                (JSONArray) tag,
                                "report"
                        );

                    } else {

                        Toast.makeText(
                                this,
                                "ابتدا گزارش را نمایش دهید",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );

        root.addView(ex);

        Button back =
                btn("← بازگشت");

        back.setOnClickListener(
                v -> home()
        );

        root.addView(back);
    }

    void exportExcel(
            JSONArray a,
            String name
    ) {

        try {

            StringBuilder x =
                    new StringBuilder(
                            "<?xml version=\"1.0\"?>"
                                    + "<Workbook xmlns=\"urn:schemas-microsoft-com:office:spreadsheet\">"
                                    + "<Worksheet ss:Name=\"خریدها\" "
                                    + "xmlns:ss=\"urn:schemas-microsoft-com:office:spreadsheet\">"
                                    + "<Table>"
                    );

            String[] h = {
                    "نام خریدار",
                    "نهاده",
                    "شماره خرید",
                    "وزن",
                    "فی",
                    "قیمت توافقی",
                    "نوع پرداخت",
                    "مبلغ خرید",
                    "تاریخ خرید",
                    "مقدار ذرت",
                    "مقدار سویا",
                    "نام شرکت",
                    "تاریخ اصلی سررسید",
                    "تاریخ فرعی سررسید"
            };

            x.append("<Row>");

            for (String s : h) {

                x.append(
                        "<Cell><Data ss:Type=\"String\">"
                )
                        .append(xml(s))
                        .append(
                                "</Data></Cell>"
                        );
            }

            x.append("</Row>");

            String[] k = {
                    "buyer",
                    "commodity",
                    "purchaseNo",
                    "weight",
                    "fee",
                    "agreed",
                    "payment",
                    "amount",
                    "buyDate",
                    "corn",
                    "soy",
                    "company",
                    "mainDue",
                    "subDue"
            };

            for (int i = 0; i < a.length(); i++) {

                JSONObject p =
                        a.optJSONObject(i);

                x.append("<Row>");

                for (String z : k) {

                    x.append(
                            "<Cell><Data ss:Type=\"String\">"
                    )
                            .append(
                                    xml(
                                            p.optString(
                                                    z,
                                                    ""
                                            )
                                    )
                            )
                            .append(
                                    "</Data></Cell>"
                            );
                }

                x.append("</Row>");
            }

            x.append(
                    "</Table></Worksheet></Workbook>"
            );

            File f =
                    new File(
                            getCacheDir(),
                            name + ".xls"
                    );

            FileOutputStream o =
                    new FileOutputStream(f);

            o.write(
                    x.toString()
                            .getBytes("UTF-8")
            );

            o.close();

            Intent in =
                    new Intent(
                            Intent.ACTION_SEND
                    );

            in.setType(
                    "application/vnd.ms-excel"
            );

            Uri u =
                    FileProvider.getUriForFile(
                            this,
                            "com.morghtak.kharidmanager.fileprovider",
                            f
                    );

            in.putExtra(
                    Intent.EXTRA_STREAM,
                    u
            );

            in.addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
            );

            startActivity(
                    Intent.createChooser(
                            in,
                            "ارسال / ذخیره فایل Excel"
                    )
            );

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "خطا در ساخت Excel",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    String xml(String s) {

        return s
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    void backup() {

        base(
                "پشتیبان‌گیری و بازیابی"
        );

        Button ex =
                btn("💾 ساخت نسخه پشتیبان");

        ex.setOnClickListener(
                v -> {

                    try {

                        File f =
                                new File(
                                        getExternalFilesDir(null),
                                        "kharid_backup.json"
                                );

                        FileOutputStream o =
                                new FileOutputStream(f);

                        o.write(
                                data.toString()
                                        .getBytes("UTF-8")
                        );

                        o.close();

                        Toast.makeText(
                                this,
                                "نسخه پشتیبان ساخته شد: "
                                        + f.getAbsolutePath(),
                                Toast.LENGTH_LONG
                        ).show();

                    } catch (Exception e) {
                    }
                }
        );

        root.addView(ex);

        Button imp =
                btn("↩️ انتخاب فایل پشتیبان");

        imp.setOnClickListener(
                v -> {

                    Intent i =
                            new Intent(
                                    Intent.ACTION_OPEN_DOCUMENT
                            );

                    i.setType(
                            "application/json"
                    );

                    i.addCategory(
                            Intent.CATEGORY_OPENABLE
                    );

                    startActivityForResult(
                            i,
                            90
                    );
                }
        );

        root.addView(imp);

        Button back =
                btn("← بازگشت");

        back.setOnClickListener(
                v -> home()
        );

        root.addView(back);
    }

    @Override
    protected void onActivityResult(
            int r,
            int c,
            Intent d
    ) {

        super.onActivityResult(
                r,
                c,
                d
        );

        if (
                r == 90
                        && c == RESULT_OK
                        && d != null
        ) {

            try {

                InputStream in =
                        getContentResolver()
                                .openInputStream(
                                        d.getData()
                                );

                ByteArrayOutputStream o =
                        new ByteArrayOutputStream();

                byte[] b =
                        new byte[4096];

                int n;

                while (
                        (n = in.read(b)) > 0
                ) {
                    o.write(b, 0, n);
                }

                data =
                        new JSONObject(
                                o.toString("UTF-8")
                        );

                AppData.save(
                        this,
                        data
                );

                home();

                Toast.makeText(
                        this,
                        "بازیابی انجام شد",
                        Toast.LENGTH_SHORT
                ).show();

            } catch (Exception e) {

                Toast.makeText(
                        this,
                        "فایل پشتیبان معتبر نیست",
                        Toast.LENGTH_LONG
                ).show();
            }
        }
    }
}
