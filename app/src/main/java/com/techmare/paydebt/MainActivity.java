package com.techmare.paydebt;

import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.snackbar.Snackbar;

/** One activity, four tabs: Home, History, Debts, Settings. */
public class MainActivity extends AppCompatActivity {
    private static final String KEY_TAB = "tab";

    Data data;
    private int tab = 0;
    private ScrollView scroll;
    private BottomNavigationView nav;
    private int[] navIds;

    @Override
    protected void onCreate(Bundle state) {
        // Use the wallpaper colours on Android 12+; older phones keep the green theme.
        // To always use the green theme, delete this line.
        DynamicColors.applyToActivityIfAvailable(this);
        super.onCreate(state);

        data = Store.load(this);
        Reminders.createChannel(this);
        Reminders.schedule(this, data.settings);
        if (state != null) tab = state.getInt(KEY_TAB, 0);

        LinearLayout root = Ui.vbox(this);
        root.setBackgroundColor(Ui.color(this, com.google.android.material.R.attr.colorSurface));

        scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        root.addView(scroll, new LinearLayout.LayoutParams(Ui.MATCH, 0, 1f));

        nav = new BottomNavigationView(this);
        nav.inflateMenu(R.menu.bottom_nav);
        navIds = new int[] {R.id.nav_home, R.id.nav_history, R.id.nav_debts, R.id.nav_settings};
        nav.setOnItemSelectedListener((MenuItem item) -> {
            for (int i = 0; i < navIds.length; i++) {
                if (navIds[i] == item.getItemId()) {
                    show(i);
                    return true;
                }
            }
            return false;
        });
        root.addView(nav, new LinearLayout.LayoutParams(Ui.MATCH, Ui.WRAP));

        setContentView(root);

        Menu menu = nav.getMenu();
        menu.findItem(navIds[tab]).setChecked(true);
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
        LinearLayout page = Ui.vbox(this);
        int side = Ui.dp(this, 16);
        page.setPadding(side, Ui.dp(this, 24), side, side);
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

    void snack(String message) {
        Snackbar.make(nav, message, Snackbar.LENGTH_SHORT).setAnchorView(nav).show();
    }

    void askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] {"android.permission.POST_NOTIFICATIONS"}, 1);
        }
    }
}
