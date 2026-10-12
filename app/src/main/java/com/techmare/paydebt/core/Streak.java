package com.techmare.paydebt.core;

/** Counts weeks in a row where the debt target was met. */
public final class Streak {
    private Streak() {}

    /**
     * @param paidPerWeek  debt paid per week, index 0 = this week, 1 = last week, ...
     * @param weeksTracked how many weeks the app has been in use (including this one)
     * @param target       weekly debt target
     *
     * The current week does not break the streak while it is still in progress, but it adds to it
     * once the target is reached.
     */
    public static int onTimeWeeks(long[] paidPerWeek, int weeksTracked, long target) {
        if (target <= 0 || paidPerWeek == null || paidPerWeek.length == 0) return 0;
        int limit = Math.min(weeksTracked, paidPerWeek.length);
        int count = 0;
        int start = paidPerWeek[0] >= target ? 0 : 1;
        for (int i = start; i < limit; i++) {
            if (paidPerWeek[i] >= target) count++;
            else break;
        }
        return count;
    }
}
