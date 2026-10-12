package com.techmare.paydebt;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** One activity, four tabs: Home, History, Debts, Settings. */
public class MainActivity extends Activity {
    private static final String[] TAB_NAMES = {"Home", "History", "Debts", "Settings"};
    private static final String KEY_TAB = "tab";

    Data data;
    private int tab = 0;
    private ScrollView scroll;
    private final TextView[] tabViews = new TextView[TAB_NAMES.length];

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        data = Store.load(this);
        Reminders.createChannel(this);
        Reminders.schedule(this, data.settings);
        if (state != null) tab = state.getInt(KEY_TAB, 0);

        LinearLayout root = Ui.vbox(this);
        root.setBackgroundColor(Ui.BG);

        scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        root.addView(scroll, new LinearLayout.LayoutParams(Ui.MATCH, 0, 1f));

        LinearLayout bar = Ui.hbox(this);
        bar.setBackgroundColor(Color.WHITE);
        for (int i = 0; i < TAB_NAMES.length; i++) {
            final int index = i;
            TextView t = Ui.text(this, TAB_NAMES[i], 14, Ui.MUTED, true);
            t.setGravity(Gravity.CENTER);
            t.setPadding(0, Ui.dp(this, 16), 0, Ui.dp(this, 16));
            t.setOnClickListener(v -> show(index));
            tabViews[i] = t;
            bar.addView(t, new LinearLayout.LayoutParams(0, Ui.WRAP, 1f));
        }
        root.addView(bar, new LinearLayout.LayoutParams(Ui.MATCH, Ui.WRAP));

        setContentView(root);
        show(tab);

        boolean firstRun = data.debts.isEmpty() && data.entries.isEmpty() && data.settings.weeklyDebtTarget == 0;
        if (state == null && firstRun) {
            root.post(() -> Dialogs.setup(this));
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putInt(KEY_TAB, tab);
    }

    void show(int index) {
        tab = index;
        for (int i = 0; i < tabViews.length; i++) {
            boolean on = i == index;
            tabViews[i].setTextColor(on ? Ui.GREEN : Ui.MUTED);
            tabViews[i].setTypeface(on ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        }
        LinearLayout page = Ui.vbox(this);
        int p = Ui.dp(this, 16);
        page.setPadding(p, Ui.dp(this, 20), p, p);
        switch (index) {
            case 0: Screens.home(this, page); break;
            case 1: Screens.history(this, page); break;
            case 2: Screens.debts(this, page); break;
            default: Screens.settings(this, page); break;
        }
        scroll.removeAllViews();
        scroll.addView(page, new ScrollView.LayoutParams(Ui.MATCH, Ui.WRAP));
        scroll.scrollTo(0, 0);
    }

    /** Call after any change: saves, resets the reminder and redraws the current tab. */
    void changed() {
        Store.save(this, data);
        Reminders.schedule(this, data.settings);
        show(tab);
    }

    /** Replaces everything (used by restore and erase). */
    void replaceData(Data fresh) {
        data = fresh;
        changed();
    }

    void askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] {"android.permission.POST_NOTIFICATIONS"}, 1);
        }
    }
}
