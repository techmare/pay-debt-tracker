package com.techmare.paydebt.core;

/** The pay-day rule: the debt payment is set aside before anything is spendable. */
public final class Budget {
    private Budget() {}

    /**
     * What must go to debt this week in total: the weekly target, but never more than what was
     * owed at the start of the week.
     *
     * @param remainingNow     total still owed right now
     * @param paidThisWeek     debt payments already made this week
     */
    public static long debtDueThisWeek(long target, long remainingNow, long paidThisWeek) {
        long owedAtWeekStart = Math.max(0, remainingNow) + Math.max(0, paidThisWeek);
        return Math.max(0, Math.min(target, owedAtWeekStart));
    }

    /** Money that still has to go to debt this week. */
    public static long reserved(long due, long paidThisWeek) {
        return Math.max(0, due - paidThisWeek);
    }

    /**
     * Cash you can spend right now without touching the debt payment.
     * Can be negative, meaning you are overspending.
     */
    public static long freeCash(long startingCash, long totalIncome, long totalDebtPaid,
                                long totalSpent, long due, long paidThisWeek) {
        return startingCash + totalIncome - totalDebtPaid - totalSpent - reserved(due, paidThisWeek);
    }

    /** Splits new income into {debt part, spendable part} using what is still reserved. */
    public static long[] splitPay(long amount, long reservedNow) {
        long a = Math.max(0, amount);
        long toDebt = Math.min(a, Math.max(0, reservedNow));
        return new long[] {toDebt, a - toDebt};
    }
}
