package com.techmare.paydebt.core;

import java.time.LocalDate;

/** How long until the debt is gone. All money is whole dirhams (long). */
public final class Payoff {
    private Payoff() {}

    /** Whole weeks needed. 0 if nothing is owed, -1 if the weekly payment is zero or less. */
    public static int weeksToPayoff(long balance, long weeklyPayment) {
        if (balance <= 0) return 0;
        if (weeklyPayment <= 0) return -1;
        return (int) ((balance + weeklyPayment - 1) / weeklyPayment);
    }

    /** Date the last payment lands, or null if there is no payment plan. */
    public static LocalDate payoffDate(LocalDate today, long balance, long weeklyPayment) {
        int weeks = weeksToPayoff(balance, weeklyPayment);
        if (weeks < 0) return null;
        return today.plusWeeks(weeks);
    }

    /** Percentage (0-100) of the starting amount already paid back. */
    public static int percentPaid(long startTotal, long balanceNow) {
        if (startTotal <= 0) return 0;
        long paid = startTotal - balanceNow;
        if (paid < 0) paid = 0;
        if (paid > startTotal) paid = startTotal;
        return (int) (paid * 100 / startTotal);
    }

    /** Weeks saved by paying {@code extra} more each week. Negative if there is no base plan. */
    public static int weeksSaved(long balance, long weeklyPayment, long extra) {
        int base = weeksToPayoff(balance, weeklyPayment);
        int faster = weeksToPayoff(balance, weeklyPayment + extra);
        if (base < 0 || faster < 0) return -1;
        return base - faster;
    }
}
