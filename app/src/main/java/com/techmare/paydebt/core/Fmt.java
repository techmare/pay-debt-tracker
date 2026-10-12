package com.techmare.paydebt.core;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Text formatting helpers. */
public final class Fmt {
    private Fmt() {}

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter SHORT = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH);

    /** 9500 -> "9 500 dh", -120 -> "-120 dh". */
    public static String dh(long value) {
        return number(value) + " dh";
    }

    public static String number(long value) {
        boolean neg = value < 0;
        String digits = Long.toString(Math.abs(value));
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (int i = digits.length() - 1; i >= 0; i--) {
            sb.append(digits.charAt(i));
            count++;
            if (count % 3 == 0 && i > 0) sb.append(' ');
        }
        sb.reverse();
        return neg ? "-" + sb : sb.toString();
    }

    public static String date(LocalDate d) {
        return d == null ? "-" : DATE.format(d);
    }

    public static String shortDate(LocalDate d) {
        return d == null ? "-" : SHORT.format(d);
    }

    /** "1 week" / "24 weeks". */
    public static String weeks(int n) {
        return n == 1 ? "1 week" : n + " weeks";
    }

    /** Parses a whole-number amount typed by the user; spaces and commas are ignored. Returns -1 if invalid. */
    public static long parseAmount(String text) {
        if (text == null) return -1;
        String t = text.replace(" ", "").replace(",", "").replace(" ", "").trim();
        if (t.isEmpty() || t.length() > 12) return -1;
        for (int i = 0; i < t.length(); i++) {
            if (!Character.isDigit(t.charAt(i))) return -1;
        }
        return Long.parseLong(t);
    }
}
