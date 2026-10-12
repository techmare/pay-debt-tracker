package com.techmare.paydebt;

import com.techmare.paydebt.core.Weeks;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/** Everything the app knows. All money is whole dirhams. */
public final class Data {

    public static final class Debt {
        public long id;
        public String name = "";
        /** Amount owed when the debt was added. */
        public long start;
    }

    public static final class Entry {
        public static final int PAY = 0;
        public static final int DEBT = 1;
        public static final int SPEND = 2;

        public long id;
        public int type;
        public long amount;
        /** Only for DEBT entries. */
        public long debtId;
        public String note = "";
        /** Only for PAY entries: Salary, Commission or Other. */
        public String source = "";
        public long time;
    }

    public static final class Settings {
        public long weeklyDebtTarget;
        public long weeklyPay;
        public long startingCash;
        public boolean reminderOn;
        /** 1 = Monday ... 7 = Sunday. */
        public int reminderDay = 5;
        public int reminderHour = 18;
    }

    public final List<Debt> debts = new ArrayList<>();
    public final List<Entry> entries = new ArrayList<>();
    public Settings settings = new Settings();
    public long nextId = 1;

    public long newId() {
        return nextId++;
    }

    public Entry add(int type, long amount, long debtId, String note, String source) {
        Entry e = new Entry();
        e.id = newId();
        e.type = type;
        e.amount = amount;
        e.debtId = debtId;
        e.note = note == null ? "" : note;
        e.source = source == null ? "" : source;
        e.time = System.currentTimeMillis();
        entries.add(e);
        return e;
    }

    public Debt debt(long id) {
        for (Debt d : debts) if (d.id == id) return d;
        return null;
    }

    public long paidTo(long debtId) {
        long sum = 0;
        for (Entry e : entries) if (e.type == Entry.DEBT && e.debtId == debtId) sum += e.amount;
        return sum;
    }

    public long balance(Debt d) {
        return Math.max(0, d.start - paidTo(d.id));
    }

    public long totalStart() {
        long sum = 0;
        for (Debt d : debts) sum += d.start;
        return sum;
    }

    public long totalRemaining() {
        long sum = 0;
        for (Debt d : debts) sum += balance(d);
        return sum;
    }

    /** The debt to pay next: the smallest one that is not paid off yet (null if all are paid). */
    public Debt focusDebt() {
        Debt best = null;
        for (Debt d : debts) {
            long b = balance(d);
            if (b <= 0) continue;
            if (best == null || b < balance(best)) best = d;
        }
        return best;
    }

    public long sum(int type) {
        long sum = 0;
        for (Entry e : entries) if (e.type == type) sum += e.amount;
        return sum;
    }

    public long sumThisWeek(int type, LocalDate today, ZoneId zone) {
        long sum = 0;
        for (Entry e : entries) {
            if (e.type == type && Weeks.isThisWeek(e.time, today, zone)) sum += e.amount;
        }
        return sum;
    }

    /** Number of weeks since the first entry, counting this week. 0 if there are no entries. */
    public int weeksTracked(LocalDate today, ZoneId zone) {
        if (entries.isEmpty()) return 0;
        long first = Long.MAX_VALUE;
        for (Entry e : entries) first = Math.min(first, e.time);
        return Math.max(1, Weeks.weeksAgo(first, today, zone) + 1);
    }

    /** Debt paid per week, index 0 = this week. */
    public long[] debtPaidPerWeek(LocalDate today, ZoneId zone, int weeks) {
        long[] out = new long[Math.max(0, weeks)];
        for (Entry e : entries) {
            if (e.type != Entry.DEBT) continue;
            int ago = Weeks.weeksAgo(e.time, today, zone);
            if (ago >= 0 && ago < out.length) out[ago] += e.amount;
        }
        return out;
    }

    public List<Entry> newestFirst() {
        List<Entry> copy = new ArrayList<>(entries);
        Collections.sort(copy, (a, b) -> a.time != b.time ? Long.compare(b.time, a.time) : Long.compare(b.id, a.id));
        return copy;
    }

    /** Removes a debt and every payment made to it. */
    public void removeDebt(long id) {
        Iterator<Entry> it = entries.iterator();
        while (it.hasNext()) {
            Entry e = it.next();
            if (e.type == Entry.DEBT && e.debtId == id) it.remove();
        }
        Iterator<Debt> di = debts.iterator();
        while (di.hasNext()) if (di.next().id == id) di.remove();
    }
}
