import com.techmare.paydebt.core.*;

import java.time.LocalDate;
import java.time.ZoneId;

/** Dependency-free tests for the money logic. Run: scripts/test-core.sh */
public class CoreTests {
    static int passed = 0, failed = 0;

    static void check(String name, boolean ok) {
        if (ok) { passed++; } else { failed++; System.out.println("FAIL: " + name); }
    }

    static void eq(String name, long expected, long actual) {
        boolean ok = expected == actual;
        if (!ok) System.out.println("  expected " + expected + " got " + actual);
        check(name, ok);
    }

    public static void main(String[] args) {
        // --- Payoff
        eq("10000 at 300 = 34 weeks", 34, Payoff.weeksToPayoff(10000, 300));
        eq("10000 at 400 = 25 weeks", 25, Payoff.weeksToPayoff(10000, 400));
        eq("10000 at 500 = 20 weeks", 20, Payoff.weeksToPayoff(10000, 500));
        eq("exact multiple 900/300 = 3", 3, Payoff.weeksToPayoff(900, 300));
        eq("901/300 rounds up = 4", 4, Payoff.weeksToPayoff(901, 300));
        eq("nothing owed = 0", 0, Payoff.weeksToPayoff(0, 300));
        eq("negative balance = 0", 0, Payoff.weeksToPayoff(-5, 300));
        eq("no plan = -1", -1, Payoff.weeksToPayoff(1000, 0));
        check("payoff date null without plan", Payoff.payoffDate(LocalDate.of(2026, 10, 12), 1000, 0) == null);
        check("payoff date 32 weeks from 12 Oct 2026",
                Payoff.payoffDate(LocalDate.of(2026, 10, 12), 10000, 300).equals(LocalDate.of(2027, 6, 7)));
        eq("percent paid half", 50, Payoff.percentPaid(1000, 500));
        eq("percent paid none", 0, Payoff.percentPaid(1000, 1000));
        eq("percent paid all", 100, Payoff.percentPaid(1000, 0));
        eq("percent paid clamps over", 0, Payoff.percentPaid(1000, 1200));
        eq("percent paid no start", 0, Payoff.percentPaid(0, 0));
        eq("weeks saved paying +100", 9, Payoff.weeksSaved(10000, 300, 100));
        eq("weeks saved no plan", -1, Payoff.weeksSaved(10000, 0, 100));

        // --- Budget
        eq("due is the target", 300, Budget.debtDueThisWeek(300, 10000, 0));
        eq("due keeps counting after a payment", 300, Budget.debtDueThisWeek(300, 9800, 200));
        eq("due shrinks near the end", 120, Budget.debtDueThisWeek(300, 120, 0));
        eq("due is zero when paid off", 0, Budget.debtDueThisWeek(300, 0, 0));
        eq("reserved nothing paid", 300, Budget.reserved(300, 0));
        eq("reserved partly paid", 100, Budget.reserved(300, 200));
        eq("reserved overpaid", 0, Budget.reserved(300, 450));
        // paid 500 this week, owe 300 reserved => 200 free
        eq("free cash after pay day", 200, Budget.freeCash(0, 500, 0, 0, 300, 0));
        // paid 500, paid 300 to debt, spent 50 => 150 free
        eq("free cash after paying debt and spending", 150, Budget.freeCash(0, 500, 300, 50, 300, 300));
        eq("free cash can go negative", -100, Budget.freeCash(0, 200, 0, 0, 300, 0));
        eq("starting cash counts", 1200, Budget.freeCash(1000, 500, 0, 0, 300, 0));
        long[] s1 = Budget.splitPay(500, 300);
        eq("split to debt", 300, s1[0]); eq("split to spend", 200, s1[1]);
        long[] s2 = Budget.splitPay(200, 300);
        eq("short pay all to debt", 200, s2[0]); eq("short pay none to spend", 0, s2[1]);
        long[] s3 = Budget.splitPay(500, 0);
        eq("nothing reserved all spendable", 500, s3[1]);
        long[] s4 = Budget.splitPay(-5, 300);
        eq("negative pay is ignored", 0, s4[0] + s4[1]);

        // --- Streak
        eq("streak current week met + 2 before", 3, Streak.onTimeWeeks(new long[]{300, 300, 400, 0}, 4, 300));
        eq("streak current week pending is ok", 2, Streak.onTimeWeeks(new long[]{0, 300, 300, 0}, 4, 300));
        eq("streak broken in the middle", 1, Streak.onTimeWeeks(new long[]{300, 100, 300}, 3, 300));
        eq("streak limited by weeks tracked", 2, Streak.onTimeWeeks(new long[]{300, 300, 300, 300}, 2, 300));
        eq("streak zero target", 0, Streak.onTimeWeeks(new long[]{300}, 1, 0));
        eq("streak empty", 0, Streak.onTimeWeeks(new long[]{}, 0, 300));

        // --- Weeks
        ZoneId z = ZoneId.of("Africa/Casablanca");
        LocalDate monday = LocalDate.of(2026, 10, 12);
        check("start of week from Monday", Weeks.startOfWeek(monday).equals(monday));
        check("start of week from Sunday", Weeks.startOfWeek(LocalDate.of(2026, 10, 18)).equals(monday));
        long sat = LocalDate.of(2026, 10, 10).atTime(15, 0).atZone(z).toInstant().toEpochMilli();
        eq("last Saturday is 1 week ago", 1, Weeks.weeksAgo(sat, monday, z));
        long tue = LocalDate.of(2026, 10, 13).atTime(9, 0).atZone(z).toInstant().toEpochMilli();
        eq("Tuesday is this week", 0, Weeks.weeksAgo(tue, monday, z));
        check("isThisWeek", Weeks.isThisWeek(tue, LocalDate.of(2026, 10, 18), z));
        check("is not this week", !Weeks.isThisWeek(sat, monday, z));

        // --- Fmt
        check("fmt 9500", Fmt.dh(9500).equals("9 500 dh"));
        check("fmt 0", Fmt.dh(0).equals("0 dh"));
        check("fmt 999", Fmt.dh(999).equals("999 dh"));
        check("fmt 1000", Fmt.dh(1000).equals("1 000 dh"));
        check("fmt 1234567", Fmt.dh(1234567).equals("1 234 567 dh"));
        check("fmt negative", Fmt.dh(-1200).equals("-1 200 dh"));
        check("fmt date", Fmt.date(LocalDate.of(2027, 5, 24)).equals("24 May 2027"));
        check("fmt weeks 1", Fmt.weeks(1).equals("1 week"));
        check("fmt weeks 24", Fmt.weeks(24).equals("24 weeks"));
        eq("parse plain", 300, Fmt.parseAmount("300"));
        eq("parse spaced", 9500, Fmt.parseAmount("9 500"));
        eq("parse comma", 9500, Fmt.parseAmount("9,500"));
        eq("parse blank", -1, Fmt.parseAmount("  "));
        eq("parse letters", -1, Fmt.parseAmount("12a"));
        eq("parse decimal rejected", -1, Fmt.parseAmount("12.5"));
        eq("parse null", -1, Fmt.parseAmount(null));
        eq("parse zero", 0, Fmt.parseAmount("0"));

        System.out.println(passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }
}
