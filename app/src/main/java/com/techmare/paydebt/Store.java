package com.techmare.paydebt;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Saves everything as one JSON text inside the app's private storage. */
final class Store {
    private static final String PREFS = "paydebt";
    private static final String KEY = "data";
    private static final String KEY_CORRUPT = "data_unreadable_copy";

    private Store() {}

    static Data load(Context c) {
        SharedPreferences sp = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String s = sp.getString(KEY, null);
        if (s == null) return new Data();
        try {
            return fromJson(s);
        } catch (JSONException e) {
            // Never lose the original text: keep a copy before starting empty.
            sp.edit().putString(KEY_CORRUPT, s).apply();
            return new Data();
        }
    }

    static void save(Context c, Data d) {
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, toJson(d, false)).apply();
    }

    static String toJson(Data d, boolean pretty) {
        try {
            JSONObject o = new JSONObject();
            o.put("app", "pay-debt-tracker");
            o.put("v", 1);
            o.put("nextId", d.nextId);

            JSONObject s = new JSONObject();
            s.put("weeklyDebtTarget", d.settings.weeklyDebtTarget);
            s.put("weeklyPay", d.settings.weeklyPay);
            s.put("startingCash", d.settings.startingCash);
            s.put("reminderOn", d.settings.reminderOn);
            s.put("reminderDay", d.settings.reminderDay);
            s.put("reminderHour", d.settings.reminderHour);
            o.put("settings", s);

            JSONArray debts = new JSONArray();
            for (Data.Debt x : d.debts) {
                JSONObject j = new JSONObject();
                j.put("id", x.id);
                j.put("name", x.name);
                j.put("start", x.start);
                debts.put(j);
            }
            o.put("debts", debts);

            JSONArray entries = new JSONArray();
            for (Data.Entry x : d.entries) {
                JSONObject j = new JSONObject();
                j.put("id", x.id);
                j.put("type", x.type);
                j.put("amount", x.amount);
                j.put("debtId", x.debtId);
                j.put("note", x.note);
                j.put("source", x.source);
                j.put("time", x.time);
                entries.put(j);
            }
            o.put("entries", entries);
            return pretty ? o.toString(2) : o.toString();
        } catch (JSONException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Reads and validates a saved or backed-up text. Throws JSONException if it is not valid. */
    static Data fromJson(String text) throws JSONException {
        JSONObject o = new JSONObject(text.trim());
        if (o.optInt("v", 0) != 1) throw new JSONException("Unknown backup version");

        Data d = new Data();
        JSONObject s = o.optJSONObject("settings");
        if (s != null) {
            d.settings.weeklyDebtTarget = Math.max(0, s.optLong("weeklyDebtTarget", 0));
            d.settings.weeklyPay = Math.max(0, s.optLong("weeklyPay", 0));
            d.settings.startingCash = Math.max(0, s.optLong("startingCash", 0));
            d.settings.reminderOn = s.optBoolean("reminderOn", false);
            int day = s.optInt("reminderDay", 5);
            d.settings.reminderDay = day >= 1 && day <= 7 ? day : 5;
            int hour = s.optInt("reminderHour", 18);
            d.settings.reminderHour = hour >= 0 && hour <= 23 ? hour : 18;
        }

        long maxId = 0;
        JSONArray debts = o.optJSONArray("debts");
        if (debts != null) {
            for (int i = 0; i < debts.length(); i++) {
                JSONObject j = debts.getJSONObject(i);
                Data.Debt x = new Data.Debt();
                x.id = j.getLong("id");
                x.name = j.optString("name", "Debt");
                x.start = j.getLong("start");
                if (x.start < 0) throw new JSONException("Negative debt");
                maxId = Math.max(maxId, x.id);
                d.debts.add(x);
            }
        }
        JSONArray entries = o.optJSONArray("entries");
        if (entries != null) {
            for (int i = 0; i < entries.length(); i++) {
                JSONObject j = entries.getJSONObject(i);
                Data.Entry x = new Data.Entry();
                x.id = j.getLong("id");
                x.type = j.getInt("type");
                x.amount = j.getLong("amount");
                x.debtId = j.optLong("debtId", 0);
                x.note = j.optString("note", "");
                x.source = j.optString("source", "");
                x.time = j.getLong("time");
                if (x.type < 0 || x.type > 2) throw new JSONException("Bad entry type");
                if (x.amount <= 0) throw new JSONException("Bad amount");
                maxId = Math.max(maxId, x.id);
                d.entries.add(x);
            }
        }
        d.nextId = Math.max(o.optLong("nextId", 1), maxId + 1);
        return d;
    }
}
