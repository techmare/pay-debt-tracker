package com.techmare.paydebt

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.techmare.paydebt.core.Fmt
import com.techmare.paydebt.core.Weeks
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val HISTORY_LIMIT = 300

/** Either a week heading or one entry. */
private class HistRow(val title: String? = null, val totals: String? = null, val entry: Data.Entry? = null)

private val DAY_MONTH = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)

@Composable
fun HistoryScreen(state: AppState, modifier: Modifier, onDialog: (Dlg) -> Unit) {
    val d = state.read()
    val today = LocalDate.now()
    val zone = ZoneId.systemDefault()
    val colors = MaterialTheme.colorScheme

    val all = d.newestFirst().take(HISTORY_LIMIT)
    if (all.isEmpty()) {
        Column(modifier.fillMaxSize().padding(24.dp)) {
            Text("No entries yet.", style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
            Text(
                "Log your first pay with Got paid on the Home tab.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
        }
        return
    }

    // Group by week, newest first.
    val rows = ArrayList<HistRow>()
    val byWeek = all.groupBy { Weeks.weeksAgo(it.time, today, zone) }
    for (ago in byWeek.keys.sorted()) {
        val list = byWeek.getValue(ago)
        val start = Weeks.startOfWeek(today).minusWeeks(ago.toLong())
        val title = when (ago) {
            0 -> "This week"
            1 -> "Last week"
            else -> start.format(DAY_MONTH) + " to " + start.plusDays(6).format(DAY_MONTH)
        }
        val inc = list.filter { it.type == Data.Entry.PAY }.sumOf { it.amount }
        val debt = list.filter { it.type == Data.Entry.DEBT }.sumOf { it.amount }
        val spent = list.filter { it.type == Data.Entry.SPEND }.sumOf { it.amount }
        rows.add(HistRow(title = title, totals = "In ${Fmt.dh(inc)}, debt ${Fmt.dh(debt)}, spent ${Fmt.dh(spent)}"))
        list.forEach { rows.add(HistRow(entry = it)) }
    }

    LazyColumn(modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)) {
        items(rows, key = { it.entry?.id?.let { id -> "e$id" } ?: ("h" + it.title) }) { row ->
            val e = row.entry
            if (e == null) {
                Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 4.dp)) {
                    Text(row.title ?: "", style = MaterialTheme.typography.titleSmall, color = colors.primary)
                    Text(row.totals ?: "", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
            } else {
                EntryRow(d, e, zone, onClick = { onDialog(Dlg.DeleteEntry(e)) })
            }
        }
    }
}

@Composable
private fun EntryRow(d: Data, e: Data.Entry, zone: ZoneId, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val name: String
    val sign: String
    val icon: Int
    val container: Color
    val onContainer: Color
    val amountColor: Color
    when (e.type) {
        Data.Entry.PAY -> {
            name = "Got paid" + (if (e.source.isEmpty()) "" else " (${e.source})")
            sign = "+"
            icon = R.drawable.ic_add
            container = colors.primaryContainer
            onContainer = colors.onPrimaryContainer
            amountColor = colors.primary
        }
        Data.Entry.DEBT -> {
            val debt = d.debt(e.debtId)
            name = "Paid " + (debt?.name ?: "a deleted debt")
            sign = "-"
            icon = R.drawable.ic_check
            container = colors.tertiaryContainer
            onContainer = colors.onTertiaryContainer
            amountColor = colors.onSurface
        }
        else -> {
            name = if (e.note.isEmpty()) "Spent" else "Spent: ${e.note}"
            sign = "-"
            icon = R.drawable.ic_remove
            container = colors.secondaryContainer
            onContainer = colors.onSecondaryContainer
            amountColor = colors.onSurface
        }
    }
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = {
            Box(
                Modifier.size(40.dp).background(container, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(icon), contentDescription = null, tint = onContainer, modifier = Modifier.size(20.dp))
            }
        },
        headlineContent = { Text(name) },
        supportingContent = { Text(Fmt.shortDate(Weeks.dateOf(e.time, zone))) },
        trailingContent = {
            Text(sign + Fmt.dh(e.amount), style = MaterialTheme.typography.titleMedium, color = amountColor)
        },
    )
}
