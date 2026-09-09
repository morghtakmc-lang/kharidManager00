package com.morghtak.kharidmanager;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
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

    final String[] labels = {
            "نام خریدار",
            "نهاده",
            "شماره خرید",
            "وزن (کیلوگرم)",
            "فی (ریال)",
            "نوع پرداخت",
            "مبلغ خرید (ریال)",
            "تاریخ خرید",
            "مقدار ذرت (کیلوگرم)",
            "مقدار سویا (کیلوگرم)",
            "نام شرکت",
            "تاریخ اصلی سررسید"
    };

    final String[] keys = {
            "buyer",
            "commodity",
            "purchaseNo",
            "weight",
            "fee",
            "payment",
            "amount",
            "buyDate",
            "corn",
            "soy",
            "company",
            "mainDue"
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

        String id = getIntent().getStringExtra("openPurchaseId");

        if (id != null) {
            JSONObject p = findPurchase(id);
            if (p != null) {
                details(p);
            } else {
                home();
            }
        } else {
            openPage(() -> home(), false);
        }
    }

    @Override
    protected void onNewIntent(Intent i) {
        super.onNewIntent(i);
        setIntent(i);
        AlarmReceiver.stopSound();

        String id = i.getStringExtra("openPurchaseId");

        if (id != null) {
            JSONObject p = findPurchase(id);
            if (p != null) {
                details(p);
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (!history.isEmpty()) {
            back();
        } else {
            super.onBackPressed();
        }
    }

    void openPage(Runnable page) {
        openPage(page, true);
    }

    void openPage(Runnable page, boolean push) {
        if (page == null) return;

        if (push && currentPage != null) {
            history.push(currentPage);
        }

        currentPage = page;
        page.run();
    }

    void back() {
        if (!history.isEmpty()) {
            Runnable p = history.pop();
            currentPage = p;
            p.run();
        } else {
            super.onBackPressed();
        }
    }

    void goHome() {
        history.clear();
        currentPage = () -> home();
        home();
    }

    void home() {
        history.clear();

        base("مدیریت خرید و سررسید");

        TextView s = tv("", 16);
        add(s);

        JSONArray a = AppData.arr(data, "purchases");

        int open = 0;

        for (int i = 0; i < a.length(); i++) {
            if (!a.optJSONObject(i).optBoolean("collected", false)) {
                open++;
            }
        }

        s.setText(
                "تعداد خریدها: " + a.length() +
                        "\nخریدهای در انتظار وصول: " + open
        );

        Button b = btn("➕ ثبت خرید جدید");
        b.setOnClickListener(v -> openPage(() -> form(null)));
        add(b);

        b = btn("🛒 خریدهای ثبت‌شده");
        b.setOnClickListener(v -> openPage(() -> listPurchases(null)));
        add(b);

        b = btn("👤 خریداران و پرونده هر خریدار");
        b.setOnClickListener(v -> openPage(this::buyersPage));
        add(b);

        b = btn("🔔 سررسیدها");
        b.setOnClickListener(v -> openPage(this::deadlines));
        add(b);

        b = btn("🏢 شرکت‌ها");
        b.setOnClickListener(v ->
                openPage(() -> manage("companies", "شرکت‌ها")));
        add(b);

        b = btn("🌾 نهاده‌ها");
        b.setOnClickListener(v ->
                openPage(() -> manage("commodities", "نهاده‌ها")));
        add(b);

        b = btn("📊 گزارش‌ها و خروجی Excel");
        b.setOnClickListener(v -> openPage(this::reports));
        add(b);

        b = btn("💾 پشتیبان‌گیری / بازیابی");
        b.setOnClickListener(v -> openPage(this::backup));
        add(b);

        finishScreen("مدیریت خرید و سررسید");
    }

    void base(String title) {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        TextView h = tv(title, 21);
        h.setGravity(Gravity.CENTER);

        root.addView(
                h,
                new LinearLayout.LayoutParams(
                        -1,
                        UiManager.dp(this, 64)
                )
        );

        setContentView(root);
    }

    void finishScreen(String title) {
        UiManager.decorate(this, root, title);
        hideKeyboard();
    }

    void hideKeyboard() {
        try {
            ((InputMethodManager)
                    getSystemService(INPUT_METHOD_SERVICE))
                    .hideSoftInputFromWindow(
                            root.getWindowToken(),
                            0
                    );
        } catch (Exception ignored) {
        }
    }

    void add(View v) {
        root.addView(
                v,
                new LinearLayout.LayoutParams(
                        -1,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );
    }

    TextView tv(String s, int z) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(z);
        t.setPadding(18, 12, 18, 12);
        return t;
    }

    Button btn(String s) {
        Button b = new Button(this);
        b.setText(s);
        b.setAllCaps(false);
        return b;
    }

    EditText input(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setSingleLine(true);
        e.setTextSize(UiManager.fieldSize(this));
        e.setPadding(14, 8, 14, 8);
        inputs.add(e);
        add(e);
        return e;
    }

    void label(String s) {
        add(tv(s, 14));
    }

    Spinner spinner(JSONArray a) {
        ArrayList<String> x = new ArrayList<>();

        for (int i = 0; i < a.length(); i++) {
            x.add(a.optString(i));
        }

        return spinner(x.toArray(new String[0]));
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

    void setSpinner(Spinner s, String v) {
        if (s == null) return;

        for (int i = 0; i < s.getCount(); i++) {
            if (String.valueOf(s.getItemAtPosition(i)).equals(v)) {
                s.setSelection(i);
                break;
            }
        }
    }

    void hidden() {
        EditText e = new EditText(this);
        e.setVisibility(View.GONE);
        inputs.add(e);
    }

    void addGrouping(EditText e) {
        e.addTextChangedListener(
                new TextWatcher() {

                    boolean busy;

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
                        if (busy) return;

                        String r = AppData.digits(ed.toString());

                        if (r.isEmpty()) return;

                        busy = true;

                        String f = AppData.fmt(r);

                        e.setText(f);
                        e.setSelection(f.length());

                        busy = false;
                    }
                }
        );
    }

    // تقویم شمسی گرافیکی
    void pickDate(EditText target) {
        PersianCalendarDialog dlg =
                new PersianCalendarDialog(
                        this,
                        target
                );

        dlg.show();
    }

    void form(JSONObject old) {

        base(
                old == null
                        ? "ثبت خرید جدید"
                        : "ویرایش خرید"
        );

        inputs.clear();

        for (int i = 0; i < labels.length; i++) {

            label((i + 1) + ". " + labels[i]);

            if (i == 0) {

                buyerSp = spinner(
                        AppData.arr(data, "buyers")
                );

                add(buyerSp);
                hidden();

            } else if (i == 1) {

                commoditySp = spinner(
                        AppData.arr(data, "commodities")
                );

                add(commoditySp);
                hidden();

            } else if (i == 5) {

                paymentSp = spinner(AppData.PAYMENTS);

                add(paymentSp);
                hidden();

            } else if (i == 10) {

                companySp = spinner(
                        AppData.arr(data, "companies")
                );

                add(companySp);
                hidden();

            } else {

                EditText e = input(labels[i]);

                if (i == 7 || i == 11) {
                    e.setFocusable(false);
                    e.setClickable(true);
                    e.setOnClickListener(v -> pickDate(e));
                }

                if (
                        i == 3 ||
                        i == 4 ||
                        i == 6 ||
                        i == 8 ||
                        i == 9
                ) {
                    e.setInputType(2);
                    addGrouping(e);
                }
            }
        }

        // مبلغ خرید فقط محاسبه خودکار است
        inputs.get(6).setFocusable(false);
        inputs.get(6).setClickable(false);

        TextWatcher calcW =
                new TextWatcher() {

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
                            Editable e
                    ) {
                        calcAmount();
                    }
                };

        inputs.get(3).addTextChangedListener(calcW);
        inputs.get(4).addTextChangedListener(calcW);

        if (old != null) {
            fill(old);
        } else {
            inputs.get(7).setText(PersianDate.today());
        }

        add(tv("وضعیت خرید", 16));

        Button collected =
                toggleButton(
                        "وصول",
                        old != null &&
                                old.optBoolean("collected", false)
                );

        add(collected);

        Button allocated =
                toggleButton(
                        "تخصیص",
                        old != null &&
                                old.optBoolean("allocated", false)
                );

        add(allocated);

        add(tv("هشدار سررسید اصلی", 16));

        Button alarm =
                toggleButton(
                        "هشدار",
                        old != null &&
                                old.optBoolean("alarm", false)
                );

        add(alarm);

        LinearLayout al = new LinearLayout(this);
        al.setOrientation(LinearLayout.VERTICAL);
        add(al);

        EditText days = new EditText(this);
        days.setHint("چند روز قبل از سررسید؟");
        days.setInputType(2);
        al.addView(days);

        EditText time = new EditText(this);
        time.setHint("ساعت هشدار مثل 10:00");
        time.setSingleLine(true);
        al.addView(time);

        Spinner repeat =
                spinner(
                        new String[]{
                                "فقط یک بار",
                                "هر روز تا زمان وصول"
                        }
                );

        al.addView(repeat);

        TextView preview =
                tv(
                        "هشدار تنظیم نشده است",
                        13
                );

        al.addView(preview);

        if (old != null) {

            days.setText(
                    "" + old.optInt("alarmDays", 1)
            );

            time.setText(
                    old.optString(
                            "alarmTime",
                            "10:00"
                    )
            );

            repeat.setSelection(
                    old.optBoolean(
                            "alarmRepeat",
                            false
                    )
                            ? 1
                            : 0
            );

            updateAlarmPreview(
                    preview,
                    old.optString("mainDue"),
                    days.getText().toString(),
                    time.getText().toString(),
                    alarm.isSelected()
            );
        }

        TextWatcher aw =
                new TextWatcher() {

                    public void beforeTextChanged(
                            CharSequence s,
                            int a,
                            int b,
                            int c
                    ) {
                    }

                    public void onTextChanged(
                            CharSequence s,
                            int a,
                            int b,
                            int c
                    ) {
                        updateAlarmPreview(
                                preview,
                                inputs.get(11)
                                        .getText()
                                        .toString(),
                                days.getText().toString(),
                                time.getText().toString(),
                                alarm.isSelected()
                        );
                    }

                    public void afterTextChanged(
                            Editable e
                    ) {
                    }
                };

        days.addTextChangedListener(aw);
        time.addTextChangedListener(aw);
        inputs.get(11).addTextChangedListener(aw);

        alarm.setOnClickListener(v -> {

            alarm.setSelected(
                    !alarm.isSelected()
            );

            styleToggle(alarm);

            updateAlarmPreview(
                    preview,
                    inputs.get(11)
                            .getText()
                            .toString(),
                    days.getText().toString(),
                    time.getText().toString(),
                    alarm.isSelected()
            );
        });

        collected.setOnClickListener(v -> {
            collected.setSelected(
                    !collected.isSelected()
            );
            styleToggle(collected);
        });

        allocated.setOnClickListener(v -> {
            allocated.setSelected(
                    !allocated.isSelected()
            );
            styleToggle(allocated);
        });

        Button clear =
                btn("🔕 حذف هشدار");

        clear.setOnClickListener(v -> {

            alarm.setSelected(false);
            styleToggle(alarm);

            if (old != null) {
                try {
                    old.remove("alarm");
                    old.remove("alarmRepeat");
                } catch (Exception ignored) {
                }

                cancelAlarm(this, old);
            }
        });

        add(clear);

        Button save =
                btn("✓ ذخیره خرید و هشدار");

        save.setOnClickListener(v ->
                savePurchase(
                        old,
                        collected.isSelected(),
                        allocated.isSelected(),
                        alarm.isSelected(),
                        days.getText().toString(),
                        time.getText().toString(),
                        repeat.getSelectedItemPosition() == 1
                )
        );

        add(save);

        Button back =
                btn("← بازگشت");

        back.setOnClickListener(v -> back());

        add(back);

        finishScreen(
                old == null
                        ? "ثبت خرید جدید"
                        : "ویرایش خرید"
        );

        // بعد از اعمال ظاهر عمومی، ظاهر کلیدهای وضعیت را دوباره اعمال می‌کنیم
        styleToggle(collected);
        styleToggle(allocated);
        styleToggle(alarm);
    }

    Button toggleButton(
            String label,
            boolean on
    ) {
        Button b =
                btn(
                        on
                                ? label + " شد"
                                : label
                );

        b.setSelected(on);
        styleToggle(b);

        return b;
    }

    void styleToggle(Button b) {

        boolean on = b.isSelected();

        String text =
                baseToggleText(
                        b.getText().toString()
                );

        b.setText(
                on
                        ? text + " شد"
                        : text
        );

        b.setTextColor(
                on
                        ? Color.WHITE
                        : UiManager.text(this)
        );

        GradientDrawable bg =
                new GradientDrawable();

        bg.setCornerRadius(
                UiManager.dp(
                        this,
                        18
                )
        );

        bg.setColor(
                on
                        ? UiManager.primary(this)
                        : UiManager.secondary(this)
        );

        if (!on) {
            bg.setStroke(
                    UiManager.dp(this, 1),
                    UiManager.primary(this)
            );
        }

        b.setBackground(bg);

        b.setAllCaps(false);

        b.setGravity(Gravity.CENTER);

        b.setTypeface(
                UiManager.selectedTypeface(
                        this,
                        Typeface.BOLD
                )
        );
    }

    String baseToggleText(String s) {
        return s
                .replace(" شد", "")
                .replace("✓ ", "")
                .trim();
    }

    void calcAmount() {

        try {

            long w =
                    Long.parseLong(
                            AppData.digits(
                                    inputs.get(3)
                                            .getText()
                                            .toString()
                            )
                    );

            long f =
                    Long.parseLong(
                            AppData.digits(
                                    inputs.get(4)
                                            .getText()
                                            .toString()
                            )
                    );

            inputs.get(6).setText(
                    AppData.fmt(
                            "" + (w * f)
                    )
            );

        } catch (Exception ignored) {

            inputs.get(6).setText("");
        }
    }

    void updateAlarmPreview(
            TextView v,
            String due,
            String days,
            String time,
            boolean on
    ) {

        if (!on) {
            v.setText(
                    "هشدار تنظیم نشده است"
            );
            return;
        }

        try {

            int n =
                    Integer.parseInt(days);

            v.setText(
                    "🔔 هشدار: " +
                            due +
                            " ، " +
                            n +
                            " روز قبل، ساعت " +
                            time
            );

        } catch (Exception e) {

            v.setText(
                    "🔔 تنظیم هشدار را کامل کنید"
            );
        }
    }

    void fill(JSONObject p) {

        for (int i = 0; i < keys.length; i++) {

            if (
                    i != 0 &&
                    i != 1 &&
                    i != 5 &&
                    i != 10
            ) {
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

        calcAmount();
    }

    void savePurchase(
            JSONObject old,
            boolean collected,
            boolean allocated,
            boolean alarm,
            String ds,
            String tm,
            boolean repeat
    ) {

        try {

            JSONObject r =
                    new JSONObject();

            for (int i = 0; i < keys.length; i++) {

                String v =
                        inputs.get(i)
                                .getText()
                                .toString();

                if (i == 0)
                    v = String.valueOf(
                            buyerSp.getSelectedItem()
                    );

                if (i == 1)
                    v = String.valueOf(
                            commoditySp.getSelectedItem()
                    );

                if (i == 5)
                    v = String.valueOf(
                            paymentSp.getSelectedItem()
                    );

                if (i == 10)
                    v = String.valueOf(
                            companySp.getSelectedItem()
                    );

                r.put(keys[i], v);
            }

            if (
                    r.optString("buyer")
                            .trim()
                            .isEmpty()
            ) {

                Toast.makeText(
                        this,
                        "نام خریدار را انتخاب کنید",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            if (
                    r.optString("purchaseNo")
                            .trim()
                            .isEmpty()
            ) {

                Toast.makeText(
                        this,
                        "شماره خرید را وارد کنید",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            r.put(
                    "id",
                    old == null
                            ? UUID.randomUUID().toString()
                            : old.optString("id")
            );

            r.put("collected", collected);
            r.put("allocated", allocated);
            r.put("alarm", alarm);

            r.put(
                    "alarmDays",
                    parseInt(ds, 1)
            );

            r.put(
                    "alarmTime",
                    tm.trim().isEmpty()
                            ? "10:00"
                            : tm.trim()
            );

            r.put(
                    "alarmRepeat",
                    repeat
            );

            JSONArray a =
                    AppData.arr(
                            data,
                            "purchases"
                    );

            boolean replaced = false;

            for (int i = 0; i < a.length(); i++) {

                if (
                        a.optJSONObject(i)
                                .optString("id")
                                .equals(
                                        r.optString("id")
                                )
                ) {

                    a.put(i, r);
                    replaced = true;
                    break;
                }
            }

            if (!replaced)
                a.put(r);

            data.put("purchases", a);

            addUnique(
                    "buyers",
                    r.optString("buyer")
            );

            addUnique(
                    "companies",
                    r.optString("company")
            );

            AppData.save(this, data);

            if (alarm)
                schedule(this, r);
            else
                cancelAlarm(this, r);

            Toast.makeText(
                    this,
                    "خرید و تنظیمات آن ذخیره شد",
                    Toast.LENGTH_SHORT
            ).show();

            goHome();

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "خطا در ذخیره اطلاعات",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    int parseInt(String s, int d) {

        try {
            return Integer.parseInt(
                    s.trim()
            );
        } catch (Exception e) {
            return d;
        }
    }

    void addUnique(
            String key,
            String val
    ) throws Exception {

        if (
                val == null ||
                val.trim().isEmpty()
        )
            return;

        JSONArray a =
                AppData.arr(data, key);

        for (int i = 0; i < a.length(); i++) {

            if (
                    a.optString(i)
                            .equals(val)
            )
                return;
        }

        a.put(val);
    }

    void listPurchases(
            String fixedBuyer
    ) {

        base(
                fixedBuyer == null
                        ? "خریدهای ثبت‌شده"
                        : "پرونده " + fixedBuyer
        );

        EditText q =
                input(
                        "جستجو: نام، شماره خرید، شرکت یا نهاده"
                );

        LinearLayout ds =
                new LinearLayout(this);

        EditText from =
                new EditText(this);

        EditText to =
                new EditText(this);

        from.setHint("از تاریخ");
        to.setHint("تا تاریخ");

        from.setFocusable(false);
        to.setFocusable(false);

        from.setOnClickListener(
                v -> pickDate(from)
        );

        to.setOnClickListener(
                v -> pickDate(to)
        );

        ds.addView(
                from,
                new LinearLayout.LayoutParams(
                        0,
                        60,
                        1
                )
        );

        ds.addView(
                to,
                new LinearLayout.LayoutParams(
                        0,
                        60,
                        1
                )
        );

        add(ds);

        Button go =
                btn("🔎 جستجو / فیلتر");

        add(go);

        LinearLayout list =
                new LinearLayout(this);

        list.setOrientation(
                LinearLayout.VERTICAL
        );

        add(list);

        Runnable render = () -> {

            list.removeAllViews();

            JSONArray a =
                    sortedPurchases();

            String s =
                    q.getText()
                            .toString()
                            .trim();

            String f =
                    from.getText().toString();

            String t =
                    to.getText().toString();

            for (int i = 0; i < a.length(); i++) {

                JSONObject p =
                        a.optJSONObject(i);

                if (p == null)
                    continue;

                if (
                        fixedBuyer != null &&
                        !fixedBuyer.equals(
                                p.optString("buyer")
                        )
                )
                    continue;

                String blob =
                        p.optString("buyer") +
                                " " +
                                p.optString("purchaseNo") +
                                " " +
                                p.optString("company") +
                                " " +
                                p.optString("commodity");

                if (
                        !s.isEmpty() &&
                        !blob.contains(s)
                )
                    continue;

                if (
                        !f.isEmpty() &&
                        p.optString("buyDate")
                                .compareTo(f) < 0
                )
                    continue;

                if (
                        !t.isEmpty() &&
                        p.optString("buyDate")
                                .compareTo(t) > 0
                )
                    continue;

                Button b =
                        btn(
                                "👤 " +
                                        p.optString("buyer") +
                                        "   |   خرید " +
                                        p.optString("purchaseNo") +
                                        "\nتاریخ خرید: " +
                                        p.optString("buyDate")
                        );

                b.setOnClickListener(
                        v -> openPage(
                                () -> details(p)
                        )
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
                v -> back()
        );

        add(back);

        finishScreen(
                "خریدهای ثبت‌شده"
        );
    }

    JSONArray sortedPurchases() {

        JSONArray src =
                AppData.arr(
                        data,
                        "purchases"
                );

        ArrayList<JSONObject> l =
                new ArrayList<>();

        for (int i = 0; i < src.length(); i++) {
            l.add(src.optJSONObject(i));
        }

        Collections.sort(
                l,
                (a, b) ->
                        b.optString("buyDate")
                                .compareTo(
                                        a.optString("buyDate")
                                )
        );

        JSONArray r =
                new JSONArray();

        for (JSONObject p : l)
            r.put(p);

        return r;
    }

    void details(JSONObject p) {

        base("جزئیات کامل خرید");

        add(
                tv(
                        "👤 " +
                                p.optString("buyer") +
                                "\nشماره خرید: " +
                                p.optString("purchaseNo"),
                        19
                )
        );

        for (int i = 0; i < keys.length; i++) {

            add(
                    tv(
                            labels[i] +
                                    ": " +
                                    p.optString(
                                            keys[i],
                                            "-"
                                    ),
                            15
                    )
            );
        }

        add(
                tv(
                        "وضعیت وصول: " +
                                (
                                        p.optBoolean("collected")
                                                ? "✅ وصول شد"
                                                : "⏳ وصول نشده"
                                ),
                        16
                )
        );

        add(
                tv(
                        "وضعیت تخصیص: " +
                                (
                                        p.optBoolean("allocated")
                                                ? "✅ تخصیص شد"
                                                : "⏳ تخصیص نشده"
                                ),
                        16
                )
        );

        if (p.optBoolean("alarm")) {

            add(
                    tv(
                            "🔔 هشدار: " +
                                    p.optInt(
                                            "alarmDays",
                                            1
                                    ) +
                                    " روز قبل، ساعت " +
                                    p.optString(
                                            "alarmTime"
                                    ) +
                                    " - " +
                                    (
                                            p.optBoolean(
                                                    "alarmRepeat"
                                            )
                                                    ? "روزانه تا وصول"
                                                    : "یک‌بار"
                                    ),
                            15
                    )
            );
        }

        Button c =
                btn(
                        p.optBoolean("collected")
                                ? "↩️ لغو وصول"
                                : "💰 وصول شد"
                );

        c.setOnClickListener(v -> {

            try {

                p.put(
                        "collected",
                        !p.optBoolean("collected")
                );

                AppData.save(
                        this,
                        data
                );

                if (p.optBoolean("collected"))
                    cancelAlarm(this, p);

                details(p);

            } catch (Exception ignored) {
            }
        });

        add(c);

        Button al =
                btn(
                        p.optBoolean("allocated")
                                ? "↩️ لغو تخصیص"
                                : "📦 تخصیص شد"
                );

        al.setOnClickListener(v -> {

            try {

                p.put(
                        "allocated",
                        !p.optBoolean("allocated")
                );

                AppData.save(
                        this,
                        data
                );

                details(p);

            } catch (Exception ignored) {
            }
        });

        add(al);

        Button rem =
                btn("🔕 حذف آلارم");

        rem.setOnClickListener(v -> {

            try {

                p.put("alarm", false);

                AppData.save(
                        this,
                        data
                );

                cancelAlarm(
                        this,
                        p
                );

                details(p);

            } catch (Exception ignored) {
            }
        });

        add(rem);

        Button ex =
                btn("📊 خروجی Excel همین خرید");

        ex.setOnClickListener(
                v ->
                        exportExcel(
                                new JSONArray().put(p),
                                "purchase_" +
                                        p.optString(
                                                "purchaseNo"
                                        )
                        )
        );

        add(ex);

        Button ed =
                btn("✏️ ویرایش");

        ed.setOnClickListener(
                v -> openPage(
                        () -> form(p)
                )
        );

        add(ed);

        Button same =
                btn(
                        "👤 پرونده " +
                                p.optString("buyer")
                );

        same.setOnClickListener(
                v ->
                        openPage(
                                () ->
                                        listPurchases(
                                                p.optString(
                                                        "buyer"
                                                )
                                        )
                        )
        );

        add(same);

        Button del =
                btn("🗑 حذف");

        del.setOnClickListener(
                v -> confirmDelete(p)
        );

        add(del);

        Button back =
                btn("← بازگشت");

        back.setOnClickListener(
                v -> back()
        );

        add(back);

        finishScreen(
                "جزئیات کامل خرید"
        );
    }

    JSONObject findPurchase(String id) {

        JSONArray a =
                AppData.arr(
                        data,
                        "purchases"
                );

        for (int i = 0; i < a.length(); i++) {

            JSONObject p =
                    a.optJSONObject(i);

            if (
                    p != null &&
                    id.equals(
                            p.optString("id")
                    )
            )
                return p;
        }

        return null;
    }

    void confirmDelete(JSONObject p) {

        new AlertDialog.Builder(this)
                .setTitle("حذف خرید")
                .setMessage("این خرید حذف شود؟")
                .setPositiveButton(
                        "حذف",
                        (d, w) -> delete(p)
                )
                .setNegativeButton(
                        "لغو",
                        null
                )
                .show();
    }

    void delete(JSONObject p) {

        try {

            cancelAlarm(this, p);

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

            goHome();

        } catch (Exception ignored) {
        }
    }

    void buyersPage() {

        base(
                "خریداران و پرونده هر خریدار"
        );

        EditText q =
                input("جستجوی نام خریدار");

        Button go =
                btn("🔎 جستجو");

        add(go);

        LinearLayout list =
                new LinearLayout(this);

        list.setOrientation(
                LinearLayout.VERTICAL
        );

        add(list);

        Runnable render = () -> {

            list.removeAllViews();

            JSONArray b =
                    AppData.arr(
                            data,
                            "buyers"
                    );

            String s =
                    q.getText()
                            .toString()
                            .trim();

            for (int i = 0; i < b.length(); i++) {

                String name =
                        b.optString(i);

                if (
                        !s.isEmpty() &&
                        !name.contains(s)
                )
                    continue;

                Button x =
                        btn("👤 " + name);

                x.setOnClickListener(
                        v ->
                                openPage(
                                        () ->
                                                buyerFile(name)
                                )
                );

                list.addView(x);
            }
        };

        go.setOnClickListener(
                v -> render.run()
        );

        render.run();

        Button addb =
                btn("➕ افزودن خریدار");

        addb.setOnClickListener(
                v ->
                        addEntry(
                                "buyers",
                                "خریدار جدید",
                                this::buyersPage
                        )
        );

        add(addb);

        Button back =
                btn("← بازگشت");

        back.setOnClickListener(
                v -> back()
        );

        add(back);

        finishScreen("خریداران");
    }

    void buyerFile(String buyer) {

        base(
                "پرونده خریدار: " + buyer
        );

        JSONArray a =
                AppData.arr(
                        data,
                        "purchases"
                );

        int n = 0;
        int coll = 0;
        int alloc = 0;

        long total = 0;
        long colAmt = 0;

        StringBuilder noColl =
                new StringBuilder();

        StringBuilder noAlloc =
                new StringBuilder();

        for (int i = 0; i < a.length(); i++) {

            JSONObject p =
                    a.optJSONObject(i);

            if (
                    p == null ||
                    !buyer.equals(
                            p.optString("buyer")
                    )
            )
                continue;

            n++;

            long amt =
                    toLong(
                            p.optString("amount")
                    );

            total += amt;

            if (
                    p.optBoolean("collected")
            ) {

                coll++;
                colAmt += amt;

            } else {

                if (noColl.length() > 0)
                    noColl.append("، ");

                noColl.append(
                        p.optString(
                                "purchaseNo"
                        )
                );
            }

            if (
                    p.optBoolean("allocated")
            ) {

                alloc++;

            } else {

                if (noAlloc.length() > 0)
                    noAlloc.append("، ");

                noAlloc.append(
                        p.optString(
                                "purchaseNo"
                        )
                );
            }
        }

        add(
                tv(
                        "آمار کل\n" +
                                "تعداد خرید: " + n +
                                "\nمجموع مبلغ: " +
                                AppData.fmt(
                                        "" + total
                                ) +
                                " ریال\n" +
                                "وصول‌شده: " +
                                coll +
                                " خرید، " +
                                AppData.fmt(
                                        "" + colAmt
                                ) +
                                " ریال\n" +
                                "وصول‌نشده: " +
                                (n - coll) +
                                " خرید\n" +
                                "شماره‌های وصول‌نشده: " +
                                (
                                        noColl.length() == 0
                                                ? "ندارد"
                                                : noColl
                                ) +
                                "\nتخصیص‌شده: " +
                                alloc +
                                " خرید\n" +
                                "تخصیص‌نشده: " +
                                (n - alloc) +
                                " خرید\n" +
                                "شماره‌های تخصیص‌نشده: " +
                                (
                                        noAlloc.length() == 0
                                                ? "ندارد"
                                                : noAlloc
                                ),
                        15
                )
        );

        Button all =
                btn(
                        "📋 همه خریدهای این خریدار"
                );

        all.setOnClickListener(
                v ->
                        openPage(
                                () ->
                                        listPurchases(
                                                buyer
                                        )
                        )
        );

        add(all);

        Button stat =
                btn("📊 آمار بازه زمانی");

        stat.setOnClickListener(
                v -> buyerStats(buyer)
        );

        add(stat);

        Button back =
                btn("← بازگشت");

        back.setOnClickListener(
                v -> back()
        );

        add(back);

        finishScreen(
                "پرونده خریدار"
        );
    }

    void buyerStats(String buyer) {

        base(
                "آمار بازه‌ای: " + buyer
        );

        EditText f =
                input("از تاریخ");

        EditText t =
                input("تا تاریخ");

        f.setFocusable(false);
        t.setFocusable(false);

        f.setOnClickListener(
                v -> pickDate(f)
        );

        t.setOnClickListener(
                v -> pickDate(t)
        );

        Button b =
                btn("نمایش آمار");

        add(b);

        TextView out =
                tv("", 15);

        add(out);

        b.setOnClickListener(v -> {

            int n = 0;
            int c = 0;
            int aCount = 0;

            long total = 0;

            JSONArray p =
                    AppData.arr(
                            data,
                            "purchases"
                    );

            for (int i = 0; i < p.length(); i++) {

                JSONObject x =
                        p.optJSONObject(i);

                if (
                        x == null ||
                        !buyer.equals(
                                x.optString("buyer")
                        )
                )
                    continue;

                String d =
                        x.optString("buyDate");

                if (
                        !f.getText().toString().isEmpty() &&
                        d.compareTo(
                                f.getText().toString()
                        ) < 0
                )
                    continue;

                if (
                        !t.getText().toString().isEmpty() &&
                        d.compareTo(
                                t.getText().toString()
                        ) > 0
                )
                    continue;

                n++;

                total +=
                        toLong(
                                x.optString("amount")
                        );

                if (
                        x.optBoolean("collected")
                )
                    c++;

                if (
                        x.optBoolean("allocated")
                )
                    aCount++;
            }

            out.setText(
                    "تعداد خرید: " + n +
                            "\nمجموع مبلغ: " +
                            AppData.fmt(
                                    "" + total
                            ) +
                            " ریال\n" +
                            "وصول‌شده: " + c +
                            "\nتخصیص‌شده: " +
                            aCount
            );
        });

        Button back =
                btn("← بازگشت");

        back.setOnClickListener(
                v -> back()
        );

        add(back);

        finishScreen("آمار");
    }

    long toLong(String s) {

        try {
            return Long.parseLong(
                    AppData.digits(s)
            );
        } catch (Exception e) {
            return 0;
        }
    }

    void deadlines() {

        base("سررسیدها");

        add(
                tv(
                        "حالت نمایش",
                        16
                )
        );

        Spinner mode =
                spinner(
                        new String[]{
                                "نزدیک‌ترین سررسیدها",
                                "بازه تاریخی"
                        }
                );

        add(mode);

        LinearLayout dates =
                new LinearLayout(this);

        EditText f =
                new EditText(this);

        EditText t =
                new EditText(this);

        f.setHint("از تاریخ");
        t.setHint("تا تاریخ");

        f.setFocusable(false);
        t.setFocusable(false);

        f.setOnClickListener(
                v -> pickDate(f)
        );

        t.setOnClickListener(
                v -> pickDate(t)
        );

        dates.addView(
                f,
                new LinearLayout.LayoutParams(
                        0,
                        60,
                        1
                )
        );

        dates.addView(
                t,
                new LinearLayout.LayoutParams(
                        0,
                        60,
                        1
                )
        );

        add(dates);

        Button show =
                btn("🔎 نمایش");

        add(show);

        LinearLayout list =
                new LinearLayout(this);

        list.setOrientation(
                LinearLayout.VERTICAL
        );

        add(list);

        Runnable render = () -> {

            list.removeAllViews();

            ArrayList<JSONObject> l =
                    new ArrayList<>();

            JSONArray a =
                    AppData.arr(
                            data,
                            "purchases"
                    );

            for (int i = 0; i < a.length(); i++) {

                JSONObject p =
                        a.optJSONObject(i);

                if (
                        p == null ||
                        p.optBoolean("collected") ||
                        p.optString("mainDue")
                                .isEmpty()
                )
                    continue;

                if (
                        mode.getSelectedItemPosition()
                                == 1
                ) {

                    if (
                            !f.getText().toString().isEmpty() &&
                            p.optString("mainDue")
                                    .compareTo(
                                            f.getText().toString()
                                    ) < 0
                    )
                        continue;

                    if (
                            !t.getText().toString().isEmpty() &&
                            p.optString("mainDue")
                                    .compareTo(
                                            t.getText().toString()
                                    ) > 0
                    )
                        continue;
                }

                l.add(p);
            }

            Collections.sort(
                    l,
                    (x, y) ->
                            x.optString("mainDue")
                                    .compareTo(
                                            y.optString("mainDue")
                                    )
            );

            for (JSONObject p : l) {

                Button b =
                        btn(
                                "👤 " +
                                        p.optString("buyer") +
                                        " | خرید " +
                                        p.optString("purchaseNo") +
                                        "\nسررسید اصلی: " +
                                        p.optString("mainDue") +
                                        (
                                                isSoon(p)
                                                        ? "  ⚠ نزدیک"
                                                        : ""
                                        )
                        );

                b.setOnClickListener(
                        v ->
                                openPage(
                                        () ->
                                                details(p)
                                )
                );

                list.addView(b);
            }
        };

        show.setOnClickListener(
                v -> render.run()
        );

        render.run();

        Button back =
                btn("← بازگشت");

        back.setOnClickListener(
                v -> back()
        );

        add(back);

        finishScreen("سررسیدها");
    }

    boolean isSoon(JSONObject p) {

        String d =
                p == null
                        ? ""
                        : p.optString("mainDue");

        if (
                d.isEmpty() ||
                p.optBoolean(
                        "collected",
                        false
                )
        )
            return false;

        long m =
                PersianDate.millis(
                        d,
                        "23:59"
                );

        return
                m >= System.currentTimeMillis() &&
                        m - System.currentTimeMillis()
                                < 7L * 86400000L;
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

            LinearLayout r =
                    new LinearLayout(this);

            TextView t =
                    tv(
                            a.optString(i),
                            16
                    );

            r.addView(
                    t,
                    new LinearLayout.LayoutParams(
                            0,
                            60,
                            1
                    )
            );

            Button d =
                    btn("حذف");

            d.setOnClickListener(v -> {

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
            });

            r.addView(
                    d,
                    new LinearLayout.LayoutParams(
                            110,
                            60
                    )
            );

            add(r);
        }

        Button ad =
                btn("➕ افزودن");

        ad.setOnClickListener(
                v ->
                        addEntry(
                                key,
                                "مورد جدید",
                                () ->
                                        manage(
                                                key,
                                                title
                                        )
                        )
        );

        add(ad);

        Button back =
                btn("← بازگشت");

        back.setOnClickListener(
                v -> back()
        );

        add(back);

        finishScreen(title);
    }

    void addEntry(
            String key,
            String title,
            Runnable after
    ) {

        EditText e =
                new EditText(this);

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(e)
                .setPositiveButton(
                        "ذخیره",
                        (d, w) -> {

                            try {

                                String v =
                                        e.getText()
                                                .toString()
                                                .trim();

                                if (v.isEmpty())
                                    return;

                                JSONArray a =
                                        AppData.arr(
                                                data,
                                                key
                                        );

                                for (int i = 0; i < a.length(); i++) {

                                    if (
                                            v.equals(
                                                    a.optString(i)
                                            )
                                    )
                                        return;
                                }

                                a.put(v);

                                data.put(
                                        key,
                                        a
                                );

                                AppData.save(
                                        this,
                                        data
                                );

                                after.run();

                            } catch (Exception ignored) {
                            }
                        }
                )
                .setNegativeButton(
                        "لغو",
                        null
                )
                .show();
    }

    void reports() {

        base(
                "گزارش‌ها و خروجی Excel"
        );

        EditText q =
                input(
                        "جستجوی خریدار، شرکت یا شماره خرید"
                );

        Button b =
                btn("🔎 جستجو");

        add(b);

        LinearLayout list =
                new LinearLayout(this);

        list.setOrientation(
                LinearLayout.VERTICAL
        );

        add(list);

        Runnable render = () -> {

            list.removeAllViews();

            String s =
                    q.getText()
                            .toString()
                            .trim();

            TreeSet<String> buyers =
                    new TreeSet<>();

            TreeSet<String> companies =
                    new TreeSet<>();

            JSONArray a =
                    sortedPurchases();

            for (int i = 0; i < a.length(); i++) {

                JSONObject p =
                        a.optJSONObject(i);

                String blob =
                        p.optString("buyer") +
                                " " +
                                p.optString("company") +
                                " " +
                                p.optString("purchaseNo");

                if (
                        s.isEmpty() ||
                        blob.contains(s)
                ) {

                    buyers.add(
                            p.optString("buyer")
                    );

                    companies.add(
                            p.optString("company")
                    );
                }
            }

            for (String x : buyers) {

                Button z =
                        btn("👤 " + x);

                z.setOnClickListener(
                        vv ->
                                openPage(
                                        () ->
                                                reportSelection(
                                                        "خریدار: " + x,
                                                        "buyer",
                                                        x
                                                )
                                )
                );

                list.addView(z);
            }

            for (String x : companies) {

                Button z =
                        btn("🏢 " + x);

                z.setOnClickListener(
                        vv ->
                                openPage(
                                        () ->
                                                reportSelection(
                                                        "شرکت: " + x,
                                                        "company",
                                                        x
                                                )
                                )
                );

                list.addView(z);
            }
        };

        b.setOnClickListener(
                v -> render.run()
        );

        render.run();

        add(
                tv(
                        "ابتدا خریدار یا شرکت را انتخاب کنید؛ سپس خریدها را تیک بزنید و خروجی بگیرید.",
                        13
                )
        );

        Button back =
                btn("← بازگشت");

        back.setOnClickListener(
                v -> back()
        );

        add(back);

        finishScreen(
                "گزارش‌ها و خروجی Excel"
        );
    }

    void reportSelection(
            String title,
            String filterKey,
            String filterVal
    ) {

        base(title);

        EditText q =
                input(
                        "جستجو: شماره خرید / تاریخ / نام"
                );

        LinearLayout dates =
                new LinearLayout(this);

        EditText f =
                new EditText(this);

        EditText t =
                new EditText(this);

        f.setHint("از تاریخ");
        t.setHint("تا تاریخ");

        f.setFocusable(false);
        t.setFocusable(false);

        f.setOnClickListener(
                v -> pickDate(f)
        );

        t.setOnClickListener(
                v -> pickDate(t)
        );

        dates.addView(
                f,
                new LinearLayout.LayoutParams(
                        0,
                        60,
                        1
                )
        );

        dates.addView(
                t,
                new LinearLayout.LayoutParams(
                        0,
                        60,
                        1
                )
        );

        add(dates);

        LinearLayout list =
                new LinearLayout(this);

        list.setOrientation(
                LinearLayout.VERTICAL
        );

        add(list);

        ArrayList<CheckBox> checks =
                new ArrayList<>();

        ArrayList<JSONObject> objs =
                new ArrayList<>();

        JSONArray a =
                sortedPurchases();

        for (int i = 0; i < a.length(); i++) {

            JSONObject p =
                    a.optJSONObject(i);

            if (
                    !filterVal.equals(
                            p.optString(filterKey)
                    )
            )
                continue;

            CheckBox c =
                    new CheckBox(this);

            c.setText(
                    "خرید " +
                            p.optString("purchaseNo") +
                            " | " +
                            p.optString("buyDate") +
                            " | " +
                            AppData.fmt(
                                    p.optString("amount")
                            )
            );

            checks.add(c);
            objs.add(p);

            list.addView(c);
        }

        Button filter =
                btn("🔎 اعمال فیلتر");

        filter.setOnClickListener(v -> {

            String s =
                    q.getText()
                            .toString()
                            .trim();

            for (int i = 0; i < checks.size(); i++) {

                JSONObject p =
                        objs.get(i);

                String blob =
                        p.optString("purchaseNo") +
                                " " +
                                p.optString("buyDate") +
                                " " +
                                p.optString("buyer") +
                                " " +
                                p.optString("company");

                boolean ok =
                        s.isEmpty() ||
                                blob.contains(s);

                if (
                        !f.getText().toString().isEmpty() &&
                        p.optString("buyDate")
                                .compareTo(
                                        f.getText().toString()
                                ) < 0
                )
                    ok = false;

                if (
                        !t.getText().toString().isEmpty() &&
                        p.optString("buyDate")
                                .compareTo(
                                        t.getText().toString()
                                ) > 0
                )
                    ok = false;

                checks.get(i).setVisibility(
                        ok
                                ? View.VISIBLE
                                : View.GONE
                );
            }
        });

        add(filter);

        Button all =
                btn(
                        "☑ انتخاب همه موارد نمایش‌داده‌شده"
                );

        all.setOnClickListener(v -> {

            for (CheckBox c : checks) {

                if (
                        c.getVisibility()
                                == View.VISIBLE
                )
                    c.setChecked(true);
            }
        });

        add(all);

        Button ex =
                btn(
                        "📊 خروجی Excel موارد انتخاب‌شده"
                );

        ex.setOnClickListener(v -> {

            JSONArray r =
                    new JSONArray();

            for (int i = 0; i < checks.size(); i++) {

                if (
                        checks.get(i).isChecked() &&
                        checks.get(i).getVisibility()
                                == View.VISIBLE
                )
                    r.put(
                            objs.get(i)
                    );
            }

            if (r.length() == 0) {

                for (int i = 0; i < checks.size(); i++) {

                    if (
                            checks.get(i).getVisibility()
                                    == View.VISIBLE
                    )
                        r.put(
                                objs.get(i)
                        );
                }
            }

            exportExcel(
                    r,
                    "report_" +
                            safe(filterVal)
            );
        });

        add(ex);

        Button back =
                btn("← بازگشت");

        back.setOnClickListener(
                v -> back()
        );

        add(back);

        finishScreen(title);
    }

    String safe(String s) {

        return s.replaceAll(
                "[\\\\/:*?\"<>|]",
                "_"
        );
    }

    void exportExcel(
            JSONArray a,
            String name
    ) {

        try {

            String[] h = {
                    "نام خریدار",
                    "نهاده",
                    "شماره خرید",
                    "وزن",
                    "فی",
                    "نوع پرداخت",
                    "مبلغ خرید",
                    "تاریخ خرید",
                    "مقدار ذرت",
                    "مقدار سویا",
                    "نام شرکت",
                    "تاریخ اصلی سررسید",
                    "وصول شد",
                    "تخصیص شد"
            };

            String[] k = {
                    "buyer",
                    "commodity",
                    "purchaseNo",
                    "weight",
                    "fee",
                    "payment",
                    "amount",
                    "buyDate",
                    "corn",
                    "soy",
                    "company",
                    "mainDue",
                    "collected",
                    "allocated"
            };

            StringBuilder x =
                    new StringBuilder("\uFEFF");

            for (String s : h)
                x.append(s).append("\t");

            x.append("\n");

            for (int i = 0; i < a.length(); i++) {

                JSONObject p =
                        a.optJSONObject(i);

                for (String z : k) {

                    x.append(
                            xml(
                                    p.optString(
                                            z,
                                            ""
                                    )
                            )
                    ).append("\t");
                }

                x.append("\n");
            }

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

            in.putExtra(
                    Intent.EXTRA_STREAM,
                    FileProvider.getUriForFile(
                            this,
                            "com.morghtak.kharidmanager.fileprovider",
                            f
                    )
            );

            in.addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
            );

            startActivity(
                    Intent.createChooser(
                            in,
                            "ارسال / ذخیره Excel"
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
                .replace("\t", " ")
                .replace("\n", " ")
                .replace("\r", " ");
    }

    void backup() {

        base(
                "پشتیبان‌گیری و بازیابی"
        );

        add(
                tv(
                        "برای امنیت اطلاعات، نسخه پشتیبان تهیه کنید.",
                        16
                )
        );

        Button b =
                btn("💾 ساخت فایل پشتیبان");

        b.setOnClickListener(
                v -> doBackup()
        );

        add(b);

        b =
                btn(
                        "📥 بازیابی از فایل پشتیبان"
                );

        b.setOnClickListener(v -> {

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
                    91
            );
        });

        add(b);

        Button back =
                btn("← بازگشت");

        back.setOnClickListener(
                v -> back()
        );

        add(back);

        finishScreen(
                "پشتیبان‌گیری و بازیابی"
        );
    }

    void doBackup() {

        try {

            File f =
                    new File(
                            getCacheDir(),
                            "kharidmanager_backup.json"
                    );

            FileOutputStream o =
                    new FileOutputStream(f);

            o.write(
                    data.toString(2)
                            .getBytes("UTF-8")
            );

            o.close();

            Intent i =
                    new Intent(
                            Intent.ACTION_SEND
                    );

            i.setType(
                    "application/json"
            );

            i.putExtra(
                    Intent.EXTRA_STREAM,
                    FileProvider.getUriForFile(
                            this,
                            "com.morghtak.kharidmanager.fileprovider",
                            f
                    )
            );

            i.addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
            );

            startActivity(
                    Intent.createChooser(
                            i,
                            "ذخیره فایل پشتیبان"
                    )
            );

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "خطا در پشتیبان‌گیری",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    @Override
    protected void onActivityResult(
            int r,
            int c,
            Intent i
    ) {

        super.onActivityResult(r, c, i);

        UiManager.handleLogoResult(
                this,
                r,
                c,
                i
        );

        if (
                r == 91 &&
                c == RESULT_OK &&
                i != null &&
                i.getData() != null
        ) {

            try {

                InputStream in =
                        getContentResolver()
                                .openInputStream(
                                        i.getData()
                                );

                ByteArrayOutputStream o =
                        new ByteArrayOutputStream();

                byte[] b =
                        new byte[4096];

                int n;

                while (
                        (n = in.read(b)) > 0
                )
                    o.write(b, 0, n);

                in.close();

                data =
                        new JSONObject(
                                new String(
                                        o.toByteArray(),
                                        "UTF-8"
                                )
                        );

                AppData.save(
                        this,
                        data
                );

                goHome();

            } catch (Exception e) {

                Toast.makeText(
                        this,
                        "فایل پشتیبان معتبر نیست",
                        Toast.LENGTH_LONG
                ).show();
            }
        }
    }

    public static JSONObject findStatic(
            Context c,
            String id
    ) {

        try {

            JSONArray a =
                    AppData.arr(
                            AppData.root(c),
                            "purchases"
                    );

            for (int i = 0; i < a.length(); i++) {

                JSONObject p =
                        a.optJSONObject(i);

                if (
                        p != null &&
                        id != null &&
                        id.equals(
                                p.optString("id")
                        )
                )
                    return p;
            }

        } catch (Exception ignored) {
        }

        return null;
    }

    public static void schedule(
            Context c,
            JSONObject p
    ) {

        try {

            if (
                    !p.optBoolean("alarm") ||
                    p.optBoolean("collected")
            )
                return;

            long at =
                    PersianDate.millis(
                            p.optString("mainDue"),
                            p.optString(
                                    "alarmTime",
                                    "10:00"
                            )
                    )
                            -
                            p.optInt(
                                    "alarmDays",
                                    1
                            ) *
                                    86400000L;

            if (
                    p.optBoolean("alarmRepeat") &&
                    at <= System.currentTimeMillis()
            ) {

                while (
                        at <=
                                System.currentTimeMillis()
                )
                    at += 86400000L;
            }

            if (
                    at <=
                            System.currentTimeMillis()
            )
                return;

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
                    "خرید " +
                            p.optString("purchaseNo") +
                            " - " +
                            p.optString("buyer")
            );

            PendingIntent pi =
                    PendingIntent.getBroadcast(
                            c,
                            p.optString("id")
                                    .hashCode(),
                            in,
                            PendingIntent.FLAG_UPDATE_CURRENT |
                                    PendingIntent.FLAG_IMMUTABLE
                    );

            if (
                    Build.VERSION.SDK_INT >= 31 &&
                    am.canScheduleExactAlarms()
            ) {

                am.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        at,
                        pi
                );

            } else {

                am.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        at,
                        pi
                );
            }

        } catch (Exception ignored) {
        }
    }

    public static void scheduleNext(
            Context c,
            JSONObject p
    ) {

        try {

            if (
                    !p.optBoolean("alarmRepeat") ||
                    p.optBoolean("collected")
            )
                return;

            AlarmManager am =
                    (AlarmManager)
                            c.getSystemService(
                                    ALARM_SERVICE
                            );

            Calendar cal =
                    Calendar.getInstance();

            cal.add(
                    Calendar.DAY_OF_YEAR,
                    1
            );

            String[] hm =
                    p.optString(
                            "alarmTime",
                            "10:00"
                    ).split(":");

            try {

                cal.set(
                        Calendar.HOUR_OF_DAY,
                        Integer.parseInt(hm[0])
                );

                cal.set(
                        Calendar.MINUTE,
                        Integer.parseInt(hm[1])
                );

            } catch (Exception ignored) {
            }

            cal.set(
                    Calendar.SECOND,
                    0
            );

            cal.set(
                    Calendar.MILLISECOND,
                    0
            );

            long at =
                    cal.getTimeInMillis();

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
                    "خرید " +
                            p.optString("purchaseNo") +
                            " - " +
                            p.optString("buyer")
            );

            PendingIntent pi =
                    PendingIntent.getBroadcast(
                            c,
                            p.optString("id")
                                    .hashCode(),
                            in,
                            PendingIntent.FLAG_UPDATE_CURRENT |
                                    PendingIntent.FLAG_IMMUTABLE
                    );

            if (
                    Build.VERSION.SDK_INT >= 31 &&
                    am.canScheduleExactAlarms()
            ) {

                am.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        at,
                        pi
                );

            } else {

                am.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        at,
                        pi
                );
            }

        } catch (Exception ignored) {
        }
    }

    public static void cancelAlarm(
            Context c,
            JSONObject p
    ) {

        try {

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

            PendingIntent pi =
                    PendingIntent.getBroadcast(
                            c,
                            p.optString("id")
                                    .hashCode(),
                            in,
                            PendingIntent.FLAG_UPDATE_CURRENT |
                                    PendingIntent.FLAG_IMMUTABLE
                    );

            am.cancel(pi);
            pi.cancel();

        } catch (Exception ignored) {
        }
    }

    // تقویم شمسی گرافیکی
    class PersianCalendarDialog extends Dialog {

        EditText target;

        int jy;
        int jm;
        int jd;

        TextView title;
        LinearLayout grid;

        PersianCalendarDialog(
                Context c,
                EditText t
        ) {

            super(c);

            target = t;

            String current =
                    t == null
                            ? ""
                            : t.getText().toString();

            int[] d =
                    parseJ(current);

            if (d == null)
                d =
                        parseJ(
                                PersianDate.today()
                        );

            jy = d[0];
            jm = d[1];
            jd = d[2];
        }

        int[] parseJ(String s) {

            try {

                String[] a =
                        s.split("/");

                if (a.length == 3) {

                    return new int[]{
                            Integer.parseInt(
                                    AppData.digits(
                                            a[0]
                                    )
                            ),
                            Integer.parseInt(
                                    AppData.digits(
                                            a[1]
                                    )
                            ),
                            Integer.parseInt(
                                    AppData.digits(
                                            a[2]
                                    )
                            )
                    };
                }

            } catch (Exception ignored) {
            }

            return null;
        }

        @Override
        protected void onCreate(Bundle b) {

            super.onCreate(b);

            LinearLayout box =
                    new LinearLayout(
                            MainActivity.this
                    );

            box.setOrientation(
                    LinearLayout.VERTICAL
            );

            box.setPadding(
                    18,
                    12,
                    18,
                    12
            );

            title =
                    new TextView(
                            MainActivity.this
                    );

            title.setGravity(
                    Gravity.CENTER
            );

            title.setTextSize(18);

            box.addView(
                    title,
                    new LinearLayout.LayoutParams(
                            -1,
                            60
                    )
            );

            LinearLayout yearNav =
                    new LinearLayout(
                            MainActivity.this
                    );

            Button prevYear =
                    btn("‹ سال قبل");

            Button nextYear =
                    btn("سال بعد ›");

            yearNav.addView(
                    prevYear,
                    new LinearLayout.LayoutParams(
                            0,
                            52,
                            1
                    )
            );

            yearNav.addView(
                    nextYear,
                    new LinearLayout.LayoutParams(
                            0,
                            52,
                            1
                    )
            );

            box.addView(yearNav);

            LinearLayout nav =
                    new LinearLayout(
                            MainActivity.this
                    );

            Button prev =
                    btn("‹ ماه قبل");

            Button next =
                    btn("ماه بعد ›");

            nav.addView(
                    prev,
                    new LinearLayout.LayoutParams(
                            0,
                            58,
                            1
                    )
            );

            nav.addView(
                    next,
                    new LinearLayout.LayoutParams(
                            0,
                            58,
                            1
                    )
            );

            box.addView(nav);

            grid =
                    new LinearLayout(
                            MainActivity.this
                    );

            grid.setOrientation(
                    LinearLayout.VERTICAL
            );

            box.addView(grid);

            LinearLayout bottom =
                    new LinearLayout(
                            MainActivity.this
                    );

            Button cancel =
                    btn("لغو");

            Button ok =
                    btn("تأیید");

            bottom.addView(
                    cancel,
                    new LinearLayout.LayoutParams(
                            0,
                            58,
                            1
                    )
            );

            bottom.addView(
                    ok,
                    new LinearLayout.LayoutParams(
                            0,
                            58,
                            1
                    )
            );

            box.addView(bottom);

            prevYear.setOnClickListener(v -> {

                jy--;

                jd =
                        Math.min(
                                jd,
                                monthDays(
                                        jy,
                                        jm
                                )
                        );

                render();
            });

            nextYear.setOnClickListener(v -> {

                jy++;

                jd =
                        Math.min(
                                jd,
                                monthDays(
                                        jy,
                                        jm
                                )
                        );

                render();
            });

            prev.setOnClickListener(v -> {

                jm--;

                if (jm < 1) {
                    jm = 12;
                    jy--;
                }

                jd =
                        Math.min(
                                jd,
                                monthDays(
                                        jy,
                                        jm
                                )
                        );

                render();
            });

            next.setOnClickListener(v -> {

                jm++;

                if (jm > 12) {
                    jm = 1;
                    jy++;
                }

                jd =
                        Math.min(
                                jd,
                                monthDays(
                                        jy,
                                        jm
                                )
                        );

                render();
            });

            cancel.setOnClickListener(
                    v -> dismiss()
            );

            ok.setOnClickListener(v -> {

                if (target != null) {

                    target.setText(
                            String.format(
                                    Locale.US,
                                    "%04d/%02d/%02d",
                                    jy,
                                    jm,
                                    jd
                            )
                    );
                }

                dismiss();
            });

            setContentView(box);

            Window w =
                    getWindow();

            if (w != null)
                w.setLayout(
                        -1,
                        -2
                );

            render();
        }

        int monthDays(
                int y,
                int m
        ) {

            return
                    m <= 6
                            ? 31
                            : (
                            m <= 11
                                    ? 30
                                    : (
                                    isLeap(y)
                                            ? 30
                                            : 29
                            )
                    );
        }

        boolean isLeap(int y) {

            long a =
                    PersianDate.millis(
                            String.format(
                                    Locale.US,
                                    "%04d/%02d/%02d",
                                    y,
                                    12,
                                    1
                            ),
                            "00:00"
                    );

            long z =
                    PersianDate.millis(
                            String.format(
                                    Locale.US,
                                    "%04d/%02d/%02d",
                                    y + 1,
                                    1,
                                    1
                            ),
                            "00:00"
                    );

            return
                    z - a >
                            365L * 86400000L;
        }

        void render() {

            title.setText(
                    "تقویم شمسی  " +
                            jy +
                            " / " +
                            monthName(jm)
            );

            grid.removeAllViews();

            String[] wd = {
                    "ش",
                    "ی",
                    "د",
                    "س",
                    "چ",
                    "پ",
                    "ج"
            };

            LinearLayout heads =
                    new LinearLayout(
                            MainActivity.this
                    );

            for (String s : wd) {

                TextView x =
                        tv(s, 14);

                x.setGravity(
                        Gravity.CENTER
                );

                heads.addView(
                        x,
                        new LinearLayout.LayoutParams(
                                0,
                                45,
                                1
                        )
                );
            }

            grid.addView(heads);

            int[] g =
                    PersianDate.toGregorian(
                            jy,
                            jm,
                            1
                    );

            Calendar c =
                    Calendar.getInstance();

            c.set(
                    g[0],
                    g[1] - 1,
                    g[2]
            );

            // شنبه = ستون اول
            int first =
                    (c.get(Calendar.DAY_OF_WEEK) + 6) % 7;

            int days =
                    monthDays(
                            jy,
                            jm
                    );

            int day = 1;

            for (
                    int row = 0;
                    row < 6 && day <= days;
                    row++
            ) {

                LinearLayout r =
                        new LinearLayout(
                                MainActivity.this
                        );

                for (int col = 0; col < 7; col++) {

                    Button d =
                            btn("");

                    d.setTextColor(
                            UiManager.text(
                                    MainActivity.this
                            )
                    );

                    if (
                            (row == 0 && col < first) ||
                            day > days
                    ) {

                        d.setEnabled(false);

                    } else {

                        final int dd = day;

                        d.setText(
                                "" + day
                        );

                        if (day == jd) {

                            d.setSelected(true);

                            d.setBackgroundColor(
                                    UiManager.primary(
                                            MainActivity.this
                                    )
                            );

                            d.setTextColor(
                                    Color.WHITE
                            );
                        }

                        d.setOnClickListener(
                                v -> {

                                    jd = dd;
                                    render();
                                }
                        );

                        day++;
                    }

                    r.addView(
                            d,
                            new LinearLayout.LayoutParams(
                                    0,
                                    52,
                                    1
                            )
                    );
                }

                grid.addView(r);
            }
        }

        String monthName(int m) {

            String[] n = {
                    "فروردین",
                    "اردیبهشت",
                    "خرداد",
                    "تیر",
                    "مرداد",
                    "شهریور",
                    "مهر",
                    "آبان",
                    "آذر",
                    "دی",
                    "بهمن",
                    "اسفند"
            };

            return n[m - 1];
        }
    }
}
