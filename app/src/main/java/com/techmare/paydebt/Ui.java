package com.techmare.paydebt;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Small helpers so the screens read like a description of the layout. */
final class Ui {
    private Ui() {}

    static final int BG = Color.parseColor("#F4F6F5");
    static final int CARD = Color.WHITE;
    static final int TEXT = Color.parseColor("#14201A");
    static final int MUTED = Color.parseColor("#5B6B63");
    static final int GREEN = Color.parseColor("#1B7F4B");
    static final int RED = Color.parseColor("#C0392B");
    static final int BLUE = Color.parseColor("#2F5D8A");
    static final int AMBER = Color.parseColor("#B7791F");
    static final int TRACK = Color.parseColor("#DDE5E0");
    static final int CHIP = Color.parseColor("#E6ECE8");

    static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;

    interface Action {
        /** Return true to close the dialog, false to keep it open (for example when input is invalid). */
        boolean run();
    }

    abstract static class Watcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(Editable s) {
            changed();
        }

        abstract void changed();
    }

    static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    static TextView text(Context c, String s, float sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    static LinearLayout vbox(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    static LinearLayout hbox(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        return l;
    }

    static View gap(Context c, int dpHeight) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(MATCH, dp(c, dpHeight)));
        return v;
    }

    static LinearLayout card(Context c) {
        LinearLayout l = vbox(c);
        GradientDrawable g = new GradientDrawable();
        g.setColor(CARD);
        g.setCornerRadius(dp(c, 16));
        l.setBackground(g);
        int p = dp(c, 16);
        l.setPadding(p, p, p, p);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(MATCH, WRAP);
        lp.setMargins(0, 0, 0, dp(c, 12));
        l.setLayoutParams(lp);
        return l;
    }

    static Button button(Context c, String label, int color, View.OnClickListener click) {
        Button b = new Button(c);
        b.setText(label);
        b.setTextColor(Color.WHITE);
        b.setAllCaps(false);
        b.setTextSize(16);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(c, 14));
        b.setBackground(g);
        b.setMinHeight(dp(c, 54));
        b.setMinimumHeight(dp(c, 54));
        b.setOnClickListener(click);
        return b;
    }

    static Button chip(Context c, String label, View.OnClickListener click) {
        Button b = new Button(c);
        b.setText(label);
        b.setTextColor(TEXT);
        b.setAllCaps(false);
        b.setTextSize(13);
        GradientDrawable g = new GradientDrawable();
        g.setColor(CHIP);
        g.setCornerRadius(dp(c, 18));
        b.setBackground(g);
        b.setMinHeight(dp(c, 38));
        b.setMinimumHeight(dp(c, 38));
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setPadding(dp(c, 14), 0, dp(c, 14), 0);
        b.setOnClickListener(click);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(WRAP, WRAP);
        lp.setMargins(0, 0, dp(c, 8), 0);
        b.setLayoutParams(lp);
        return b;
    }

    static EditText numberField(Context c, String hint) {
        EditText e = new EditText(c);
        e.setHint(hint);
        e.setInputType(InputType.TYPE_CLASS_NUMBER);
        e.setSingleLine(true);
        return e;
    }

    static EditText textField(Context c, String hint) {
        EditText e = new EditText(c);
        e.setHint(hint);
        e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        e.setSingleLine(true);
        return e;
    }

    /** Padded column used as the body of a dialog. */
    static LinearLayout form(Context c) {
        LinearLayout l = vbox(c);
        l.setPadding(dp(c, 22), dp(c, 8), dp(c, 22), dp(c, 4));
        return l;
    }

    /** Horizontal progress bar drawn with two plain views (percent 0-100). */
    static View bar(Context c, int percent, int color) {
        int p = Math.max(0, Math.min(100, percent));
        LinearLayout track = hbox(c);
        track.setWeightSum(100);
        GradientDrawable tg = new GradientDrawable();
        tg.setColor(TRACK);
        tg.setCornerRadius(dp(c, 5));
        track.setBackground(tg);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(MATCH, dp(c, 10));
        tlp.setMargins(0, dp(c, 10), 0, dp(c, 6));
        track.setLayoutParams(tlp);

        View fill = new View(c);
        GradientDrawable fg = new GradientDrawable();
        fg.setColor(color);
        fg.setCornerRadius(dp(c, 5));
        fill.setBackground(fg);
        track.addView(fill, new LinearLayout.LayoutParams(0, MATCH, p));
        return track;
    }

    /** Dialog with OK and Cancel. The dialog stays open while {@code action} returns false. */
    static AlertDialog showForm(Activity a, String title, View body, String okLabel, final Action action) {
        ScrollView sv = new ScrollView(a);
        sv.addView(body);
        final AlertDialog dlg = new AlertDialog.Builder(a)
                .setTitle(title)
                .setView(sv)
                .setPositiveButton(okLabel, null)
                .setNegativeButton("Cancel", null)
                .create();
        dlg.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        dlg.show();
        dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (action.run()) dlg.dismiss();
        });
        return dlg;
    }
}
