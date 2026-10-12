package com.techmare.paydebt;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.List;

/** Material 3 building blocks, so the screens read like a description of the layout. */
final class Ui {
    private Ui() {}

    // ---------- colour roles (looked up in the current theme, so light/dark/dynamic all work) ----------
    static final int PRIMARY = com.google.android.material.R.attr.colorPrimary;
    static final int ON_PRIMARY = com.google.android.material.R.attr.colorOnPrimary;
    static final int PRIMARY_CONTAINER = com.google.android.material.R.attr.colorPrimaryContainer;
    static final int ON_PRIMARY_CONTAINER = com.google.android.material.R.attr.colorOnPrimaryContainer;
    static final int SECONDARY_CONTAINER = com.google.android.material.R.attr.colorSecondaryContainer;
    static final int ON_SECONDARY_CONTAINER = com.google.android.material.R.attr.colorOnSecondaryContainer;
    static final int TERTIARY_CONTAINER = com.google.android.material.R.attr.colorTertiaryContainer;
    static final int ON_TERTIARY_CONTAINER = com.google.android.material.R.attr.colorOnTertiaryContainer;
    static final int ERROR = com.google.android.material.R.attr.colorError;
    static final int ERROR_CONTAINER = com.google.android.material.R.attr.colorErrorContainer;
    static final int ON_ERROR_CONTAINER = com.google.android.material.R.attr.colorOnErrorContainer;
    static final int ON_SURFACE = com.google.android.material.R.attr.colorOnSurface;
    static final int ON_SURFACE_VARIANT = com.google.android.material.R.attr.colorOnSurfaceVariant;
    static final int CONTAINER_LOW = com.google.android.material.R.attr.colorSurfaceContainerLow;
    static final int CONTAINER_HIGH = com.google.android.material.R.attr.colorSurfaceContainerHigh;

    // ---------- type scale ----------
    static final int DISPLAY_MEDIUM = com.google.android.material.R.style.TextAppearance_Material3_DisplayMedium;
    static final int DISPLAY_SMALL = com.google.android.material.R.style.TextAppearance_Material3_DisplaySmall;
    static final int HEADLINE_MEDIUM = com.google.android.material.R.style.TextAppearance_Material3_HeadlineMedium;
    static final int HEADLINE_SMALL = com.google.android.material.R.style.TextAppearance_Material3_HeadlineSmall;
    static final int TITLE_LARGE = com.google.android.material.R.style.TextAppearance_Material3_TitleLarge;
    static final int TITLE_MEDIUM = com.google.android.material.R.style.TextAppearance_Material3_TitleMedium;
    static final int BODY_LARGE = com.google.android.material.R.style.TextAppearance_Material3_BodyLarge;
    static final int BODY_MEDIUM = com.google.android.material.R.style.TextAppearance_Material3_BodyMedium;
    static final int BODY_SMALL = com.google.android.material.R.style.TextAppearance_Material3_BodySmall;
    static final int LABEL_LARGE = com.google.android.material.R.style.TextAppearance_Material3_LabelLarge;

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

    /** Resolves a colour role such as {@link #PRIMARY} in the current theme. */
    static int color(Context c, int attr) {
        return MaterialColors.getColor(c, attr, Color.MAGENTA);
    }

    // ---------- text and layout ----------

    static TextView text(Context c, String s, int appearance, int colorAttr) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextAppearance(appearance);
        t.setTextColor(color(c, colorAttr));
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

    /** Layout params with a top margin in dp. */
    static LinearLayout.LayoutParams lp(Context c, int topDp) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(MATCH, WRAP);
        p.topMargin = dp(c, topDp);
        return p;
    }

    /** Adds {@code child} to {@code parent} with a top margin. */
    static void add(LinearLayout parent, View child, int topDp) {
        parent.addView(child, lp(parent.getContext(), topDp));
    }

    // ---------- cards ----------

    /** A filled M3 card tinted with the given container colour role. Use {@link #inner} to fill it. */
    static MaterialCardView card(Context c, int containerAttr) {
        MaterialCardView card = (MaterialCardView) LayoutInflater.from(c).inflate(R.layout.card_filled, null);
        card.setCardBackgroundColor(color(c, containerAttr));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(MATCH, WRAP);
        p.bottomMargin = dp(c, 12);
        card.setLayoutParams(p);
        return card;
    }

    static LinearLayout inner(MaterialCardView card) {
        return card.findViewById(R.id.card_body);
    }

    // ---------- buttons ----------

    private static MaterialButton button(Context c, int layout, String label, int iconRes, View.OnClickListener click) {
        MaterialButton b = (MaterialButton) LayoutInflater.from(c).inflate(layout, null);
        b.setText(label);
        b.setAllCaps(false);
        if (iconRes != 0) {
            b.setIconResource(iconRes);
            b.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);
            b.setIconPadding(dp(c, 8));
        }
        b.setOnClickListener(click);
        return b;
    }

    static MaterialButton filled(Context c, String label, int iconRes, View.OnClickListener click) {
        MaterialButton b = button(c, R.layout.btn_filled, label, iconRes, click);
        b.setMinimumHeight(dp(c, 56));
        return b;
    }

    static MaterialButton tonal(Context c, String label, int iconRes, View.OnClickListener click) {
        MaterialButton b = button(c, R.layout.btn_tonal, label, iconRes, click);
        b.setMinimumHeight(dp(c, 56));
        return b;
    }

    static MaterialButton outlined(Context c, String label, int iconRes, View.OnClickListener click) {
        MaterialButton b = button(c, R.layout.btn_outlined, label, iconRes, click);
        b.setMinimumHeight(dp(c, 56));
        return b;
    }

    static MaterialButton textButton(Context c, String label, View.OnClickListener click) {
        return button(c, R.layout.btn_text, label, 0, click);
    }

    // ---------- progress ----------

    static LinearProgressIndicator progress(Context c, int percent, int indicatorColor, int trackColor) {
        LinearProgressIndicator p = new LinearProgressIndicator(c);
        p.setIndeterminate(false);
        p.setMax(100);
        p.setTrackThickness(dp(c, 10));
        p.setTrackCornerRadius(dp(c, 5));
        p.setIndicatorColor(indicatorColor);
        p.setTrackColor(trackColor);
        p.setProgressCompat(Math.max(0, Math.min(100, percent)), false);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(MATCH, WRAP);
        lp.topMargin = dp(c, 12);
        lp.bottomMargin = dp(c, 8);
        p.setLayoutParams(lp);
        return p;
    }

    // ---------- list row pieces ----------

    /** Round tonal badge with an icon, used at the start of list rows. */
    static ImageView badge(Context c, int iconRes, int containerAttr, int onContainerAttr) {
        ImageView v = new ImageView(c);
        v.setImageResource(iconRes);
        v.setColorFilter(color(c, onContainerAttr));
        int pad = dp(c, 10);
        v.setPadding(pad, pad, pad, pad);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(color(c, containerAttr));
        v.setBackground(bg);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dp(c, 40), dp(c, 40));
        p.setMarginEnd(dp(c, 16));
        v.setLayoutParams(p);
        return v;
    }

    // ---------- inputs ----------

    static final int NUMBER = 0;
    static final int TEXT = 1;
    static final int MULTILINE = 2;

    /** Outlined M3 text field added to {@code form}. Returns the field itself. */
    static EditText input(Context c, LinearLayout form, String label, int kind, String suffix) {
        TextInputLayout til = new TextInputLayout(c);
        til.setHint(label);
        if (suffix != null) til.setSuffixText(suffix);
        TextInputEditText e = new TextInputEditText(til.getContext());
        if (kind == NUMBER) {
            e.setInputType(InputType.TYPE_CLASS_NUMBER);
            e.setSingleLine(true);
        } else if (kind == TEXT) {
            e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            e.setSingleLine(true);
        } else {
            e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
            e.setMinLines(4);
            e.setMaxLines(8);
        }
        til.addView(e, new LinearLayout.LayoutParams(MATCH, WRAP));
        add(form, til, 12);
        return e;
    }

    /** Exposed dropdown menu. Read {@link #index} after the user picks. */
    static final class Dropdown {
        final TextInputLayout layout;
        int index;

        Dropdown(TextInputLayout layout, int index) {
            this.layout = layout;
            this.index = index;
        }
    }

    static Dropdown dropdown(Context c, String label, List<String> items, int selected) {
        TextInputLayout til = new TextInputLayout(c, null,
                com.google.android.material.R.attr.textInputOutlinedExposedDropdownMenuStyle);
        til.setHint(label);
        final Dropdown d = new Dropdown(til, selected);
        MaterialAutoCompleteTextView ac = new MaterialAutoCompleteTextView(til.getContext());
        ac.setInputType(InputType.TYPE_NULL);
        ac.setSimpleItems(items.toArray(new String[0]));
        ac.setText(items.get(selected), false);
        ac.setOnItemClickListener((parent, view, position, id) -> d.index = position);
        til.addView(ac, new LinearLayout.LayoutParams(MATCH, WRAP));
        return d;
    }

    /** M3 segmented button (single choice). */
    static final class Segmented {
        final MaterialButtonToggleGroup group;
        final int[] ids;

        Segmented(MaterialButtonToggleGroup group, int[] ids) {
            this.group = group;
            this.ids = ids;
        }

        int index() {
            int checked = group.getCheckedButtonId();
            for (int i = 0; i < ids.length; i++) if (ids[i] == checked) return i;
            return 0;
        }
    }

    static Segmented segmented(Context c, String[] items, int selected) {
        MaterialButtonToggleGroup g = new MaterialButtonToggleGroup(c);
        g.setSingleSelection(true);
        g.setSelectionRequired(true);
        int[] ids = new int[items.length];
        for (int i = 0; i < items.length; i++) {
            MaterialButton b = new MaterialButton(c, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
            b.setText(items[i]);
            b.setAllCaps(false);
            ids[i] = View.generateViewId();
            b.setId(ids[i]);
            g.addView(b, new LinearLayout.LayoutParams(0, WRAP, 1f));
        }
        g.check(ids[Math.max(0, Math.min(selected, items.length - 1))]);
        return new Segmented(g, ids);
    }

    // ---------- dialogs ----------

    /** Padded column used as the body of a dialog. */
    static LinearLayout form(Context c) {
        LinearLayout l = vbox(c);
        l.setPadding(dp(c, 24), dp(c, 8), dp(c, 24), dp(c, 8));
        return l;
    }

    /** M3 dialog with OK and Cancel. It stays open while {@code action} returns false. */
    static AlertDialog showForm(Activity a, String title, View body, String okLabel, final Action action) {
        ScrollView sv = new ScrollView(a);
        sv.addView(body);
        final AlertDialog dlg = new MaterialAlertDialogBuilder(a)
                .setTitle(title)
                .setView(sv)
                .setPositiveButton(okLabel, null)
                .setNegativeButton("Cancel", null)
                .create();
        dlg.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
                | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        dlg.show();
        dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (action.run()) dlg.dismiss();
        });
        return dlg;
    }
}
