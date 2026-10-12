@file:OptIn(ExperimentalMaterial3Api::class)

package com.techmare.paydebt

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.techmare.paydebt.core.Budget
import com.techmare.paydebt.core.Fmt
import com.techmare.paydebt.core.Payoff
import com.techmare.paydebt.core.Streak
import java.time.LocalDate
import java.time.ZoneId

private const val WHAT_IF_EXTRA = 100L
private const val MAX_WEEK_CELLS = 104

@Composable
fun HomeScreen(
    state: AppState,
    modifier: Modifier,
    onDialog: (Dlg) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val d = state.read()
    val today = LocalDate.now()
    val zone = ZoneId.systemDefault()
    val colors = MaterialTheme.colorScheme

    val remaining = d.totalRemaining()
    val target = d.settings.weeklyDebtTarget
    val paidWeek = d.sumThisWeek(Data.Entry.DEBT, today, zone)
    val due = Budget.debtDueThisWeek(target, remaining, paidWeek)
    val reserved = Budget.reserved(due, paidWeek)
    val free = Budget.freeCash(
        d.settings.startingCash, d.sum(Data.Entry.PAY), d.sum(Data.Entry.DEBT),
        d.sum(Data.Entry.SPEND), due, paidWeek,
    )

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Text(Fmt.date(today), style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
        Spacer(Modifier.height(20.dp))

        // ----- Free to spend: the number you check most -----
        Text("Free to spend", style = MaterialTheme.typography.titleMedium, color = colors.onSurfaceVariant)
        Money(free, if (free >= 0) colors.primary else colors.error)
        val hero = when {
            free < 0 -> "You have spent more than you have. Check History for mistakes."
            d.debts.isEmpty() -> "Add a debt in the Debts tab to start setting money aside."
            reserved > 0 -> "After ${Fmt.dh(reserved)} set aside for debt this week."
            remaining <= 0 -> "You have no debt left."
            else -> "This week's debt payment is done."
        }
        Text(hero, style = MaterialTheme.typography.bodyLarge, color = colors.onSurface)

        if (due > 0) {
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Debt this week", style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
                Text(
                    "${Fmt.number(paidWeek)} of ${Fmt.dh(due)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (paidWeek.toFloat() / due.toFloat()).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = colors.tertiary,
                trackColor = colors.surfaceContainerHighest,
                strokeCap = StrokeCap.Round,
            )
        }

        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilledTonalButton(onClick = { onDialog(Dlg.PaidDebt) }, modifier = Modifier.weight(1f).height(52.dp)) {
                Icon(painterResource(R.drawable.ic_check), contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Paid debt")
            }
            OutlinedButton(onClick = { onDialog(Dlg.Spent) }, modifier = Modifier.weight(1f).height(52.dp)) {
                Icon(painterResource(R.drawable.ic_remove), contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Spent")
            }
        }

        Spacer(Modifier.height(24.dp))

        // ----- Debt-free plan -----
        if (d.debts.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.primaryContainer),
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("Start here", style = MaterialTheme.typography.titleLarge, color = colors.onPrimaryContainer)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Add what you owe and how much you will pay each week. The app shows when you will be " +
                            "debt-free and how much you can safely spend.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onPrimaryContainer,
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { onDialog(Dlg.Setup) }) { Text("Set up") }
                }
            }
        } else {
            PlanCard(d, today, zone, remaining, target, paidWeek, due, onOpenSettings)
        }

        Spacer(Modifier.height(96.dp)) // room for the floating button
    }
}

@Composable
private fun PlanCard(
    d: Data,
    today: LocalDate,
    zone: ZoneId,
    remaining: Long,
    target: Long,
    paidWeek: Long,
    due: Long,
    onOpenSettings: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Card(
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow),
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            if (remaining <= 0) {
                Text("Debt free", style = MaterialTheme.typography.headlineMedium, color = colors.primary)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Everything you added is paid off.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
                return@Column
            }

            val weeks = Payoff.weeksToPayoff(remaining, target)
            val percent = Payoff.percentPaid(d.totalStart(), remaining)
            if (weeks < 0) {
                Text("Payoff date", style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Set a weekly debt payment to see when you will be done.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
                TextButton(onClick = onOpenSettings) { Text("Open settings") }
                return@Column
            }

            Text(Fmt.weeks(weeks) + " to go", style = MaterialTheme.typography.headlineMedium, color = colors.onSurface)
            Spacer(Modifier.height(4.dp))
            Text(
                "Debt-free around " + Fmt.date(Payoff.payoffDate(today, remaining, target)),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurface,
            )
            Text(
                "You owe ${Fmt.dh(remaining)}, ${percent}% paid back.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
            val focus = d.focusDebt()
            if (focus != null && d.debts.size > 1) {
                Text(
                    "Pay off next: ${focus.name} (${Fmt.dh(d.balance(focus))}).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            }

            // One square per week, 13 to a row.
            val tracked = d.weeksTracked(today, zone)
            val paid = d.debtPaidPerWeek(today, zone, tracked)
            val cells = ArrayList<Wk>()
            for (ago in tracked - 1 downTo 1) cells.add(if (paid[ago] >= target) Wk.MET else Wk.MISSED)
            val thisWeekDone = due > 0 && paidWeek >= due
            cells.add(if (thisWeekDone) Wk.MET else Wk.NOW)
            val ahead = (weeks - (if (thisWeekDone) 0 else 1)).coerceAtLeast(0)
            repeat(ahead) { cells.add(Wk.AHEAD) }
            val shown = if (cells.size > MAX_WEEK_CELLS) cells.takeLast(MAX_WEEK_CELLS) else cells

            Spacer(Modifier.height(20.dp))
            WeekGrid(shown)
            Spacer(Modifier.height(12.dp))
            WeekLegend(showMissed = shown.contains(Wk.MISSED))

            val streak = Streak.onTimeWeeks(paid, tracked, target)
            if (streak > 0) {
                Spacer(Modifier.height(12.dp))
                Text(
                    Fmt.weeks(streak) + " in a row on time",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.primary,
                )
            }
            val saved = Payoff.weeksSaved(remaining, target, WHAT_IF_EXTRA)
            if (saved > 0) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Pay ${Fmt.dh(WHAT_IF_EXTRA)} more each week and finish ${Fmt.weeks(saved)} sooner.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.tertiary,
                )
            }
        }
    }
}
