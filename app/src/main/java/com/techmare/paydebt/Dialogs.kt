@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.techmare.paydebt

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import com.techmare.paydebt.core.Budget
import com.techmare.paydebt.core.Fmt
import java.time.LocalDate
import java.time.ZoneId

/** Shows whichever dialog [dlg] describes. */
@Composable
fun Dialogs(state: AppState, dlg: Dlg, onClose: () -> Unit, say: (String) -> Unit) {
    when (dlg) {
        Dlg.Setup -> SetupDialog(state, onClose)
        Dlg.GotPaid -> GotPaidDialog(state, onClose)
        Dlg.PaidDebt -> PaidDebtDialog(state, onClose, say)
        Dlg.Spent -> SpentDialog(state, onClose)
        is Dlg.DebtForm -> DebtFormDialog(state, dlg.debt, onClose)
        is Dlg.DeleteDebt -> ConfirmDialog(
            title = "Delete ${dlg.debt.name}?",
            message = "This also removes the payments logged to it. This cannot be undone.",
            okLabel = "Delete",
            onClose = onClose,
        ) {
            state.data.removeDebt(dlg.debt.id)
            state.changed()
        }
        is Dlg.DeleteEntry -> ConfirmDialog(
            title = "Delete this entry?",
            message = describe(dlg.entry, state.data),
            okLabel = "Delete",
            onClose = onClose,
        ) {
            state.data.entries.remove(dlg.entry)
            state.changed()
        }
        Dlg.Restore -> RestoreDialog(state, onClose)
        Dlg.Erase -> ConfirmDialog(
            title = "Erase everything?",
            message = "All debts, entries and settings on this phone will be deleted. Share a backup first if you may need it.",
            okLabel = "Erase",
            onClose = onClose,
        ) { state.replace(Data()) }
    }
}

private fun describe(e: Data.Entry, d: Data): String {
    val what = when (e.type) {
        Data.Entry.PAY -> "Got paid"
        Data.Entry.DEBT -> "Paid " + (d.debt(e.debtId)?.name ?: "a deleted debt")
        else -> if (e.note.isEmpty()) "Spent" else "Spent: ${e.note}"
    }
    val when_ = Fmt.shortDate(com.techmare.paydebt.core.Weeks.dateOf(e.time, ZoneId.systemDefault()))
    return "$what\n$when_\n${Fmt.dh(e.amount)}"
}

/** Debts that still have a balance, the one to pay first on top. */
private fun openDebts(d: Data): List<Data.Debt> {
    val focus = d.focusDebt()
    val list = ArrayList<Data.Debt>()
    if (focus != null) list.add(focus)
    for (x in d.debts) if (x !== focus && d.balance(x) > 0) list.add(x)
    return list
}

@Composable
private fun ConfirmDialog(title: String, message: String, okLabel: String, onClose: () -> Unit, onOk: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = { onOk(); onClose() }) { Text(okLabel) } },
        dismissButton = { TextButton(onClick = onClose) { Text("Cancel") } },
    )
}

@Composable
private fun DebtPicker(d: Data, debts: List<Data.Debt>, selected: Int, onSelect: (Int) -> Unit) {
    Column {
        debts.forEachIndexed { i, x ->
            Row(
                Modifier.fillMaxWidth().clickable { onSelect(i) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = selected == i, onClick = null)
                Text(
                    "${x.name} (${Fmt.dh(d.balance(x))})",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 12.dp),
                )
            }
        }
    }
}

// ---------- first run ----------

@Composable
private fun SetupDialog(state: AppState, onClose: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var owed by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    var pay by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Set up") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "A few numbers to start. You can change them later.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextField1(name, { name = it }, "What do you owe? (name, optional)", focusRequester = focus)
                AmountField(owed, { owed = it }, "How much do you owe now?")
                AmountField(target, { target = it }, "Paid toward it each week")
                AmountField(pay, { pay = it }, "Roughly earned each week")
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val o = owed.toLongOrNull() ?: 0L
                if (o > 0) {
                    val debt = Data.Debt()
                    debt.id = state.data.newId()
                    debt.name = name.trim().ifEmpty { "Debt" }
                    debt.start = o
                    state.data.debts.add(debt)
                }
                state.data.settings.weeklyDebtTarget = target.toLongOrNull() ?: 0L
                state.data.settings.weeklyPay = pay.toLongOrNull() ?: 0L
                state.changed()
                onClose()
            }) { Text("Start") }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("Not now") } },
    )
}

// ---------- got paid ----------

@Composable
private fun GotPaidDialog(state: AppState, onClose: () -> Unit) {
    val d = state.data
    val today = LocalDate.now()
    val zone = ZoneId.systemDefault()
    val paidWeek = d.sumThisWeek(Data.Entry.DEBT, today, zone)
    val due = Budget.debtDueThisWeek(d.settings.weeklyDebtTarget, d.totalRemaining(), paidWeek)
    val reserved = Budget.reserved(due, paidWeek)
    val open = openDebts(d)
    val canLog = open.isNotEmpty() && reserved > 0

    var amount by remember { mutableStateOf("") }
    var source by remember { mutableStateOf(0) }
    var alsoLog by remember { mutableStateOf(true) }
    var pick by remember { mutableStateOf(0) }
    var showError by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    val amt = amount.toLongOrNull() ?: 0L
    val split = Budget.splitPay(amt, reserved)
    val sources = listOf("Salary", "Commission", "Other")

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Got paid") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AmountField(
                    amount, { amount = it }, "Amount received",
                    errorText = if (showError && amt <= 0) "Enter an amount" else null,
                    focusRequester = focus,
                )
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    sources.forEachIndexed { i, label ->
                        SegmentedButton(
                            selected = source == i,
                            onClick = { source = i },
                            shape = SegmentedButtonDefaults.itemShape(index = i, count = sources.size),
                        ) { Text(label) }
                    }
                }
                Text(
                    "Set aside for debt: ${Fmt.dh(split[0])}\nFree to spend: ${Fmt.dh(split[1])}",
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (canLog) {
                    Row(
                        Modifier.fillMaxWidth().clickable { alsoLog = !alsoLog },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = alsoLog, onCheckedChange = null)
                        Text(
                            "Also log ${Fmt.dh(split[0])} as paid to debt now",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 12.dp),
                        )
                    }
                    if (alsoLog && open.size > 1) DebtPicker(d, open, pick) { pick = it }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (amt <= 0) {
                    showError = true
                } else {
                    d.add(Data.Entry.PAY, amt, 0L, "", sources[source])
                    val toDebt = split[0]
                    if (canLog && alsoLog && toDebt > 0) {
                        val chosen = open[pick.coerceIn(0, open.size - 1)]
                        val pay = minOf(toDebt, d.balance(chosen))
                        if (pay > 0) d.add(Data.Entry.DEBT, pay, chosen.id, "", "")
                    }
                    state.changed()
                    onClose()
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("Cancel") } },
    )
}

// ---------- paid debt ----------

@Composable
private fun PaidDebtDialog(state: AppState, onClose: () -> Unit, say: (String) -> Unit) {
    val d = state.data
    val open = openDebts(d)
    if (open.isEmpty()) {
        LaunchedEffect(Unit) {
            say(if (d.debts.isEmpty()) "Add a debt first" else "Nothing left to pay")
            onClose()
        }
        return
    }
    var pick by remember { mutableStateOf(0) }
    val suggested = minOf(d.settings.weeklyDebtTarget, d.balance(open[0]))
    var amount by remember { mutableStateOf(if (suggested > 0) suggested.toString() else "") }
    var error by remember { mutableStateOf<String?>(null) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Paid debt") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (open.size > 1) DebtPicker(d, open, pick) { pick = it; error = null }
                AmountField(
                    amount, { amount = it; error = null }, "Amount paid",
                    errorText = error,
                    focusRequester = focus,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val amt = amount.toLongOrNull() ?: 0L
                val target = open[pick.coerceIn(0, open.size - 1)]
                val left = d.balance(target)
                when {
                    amt <= 0 -> error = "Enter an amount"
                    amt > left -> error = "That is more than the ${Fmt.dh(left)} left on this debt"
                    else -> {
                        d.add(Data.Entry.DEBT, amt, target.id, "", "")
                        state.changed()
                        onClose()
                    }
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("Cancel") } },
    )
}

// ---------- spent ----------

@Composable
private fun SpentDialog(state: AppState, onClose: () -> Unit) {
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val amt = amount.toLongOrNull() ?: 0L

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Spent") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AmountField(
                    amount, { amount = it }, "Amount spent",
                    errorText = if (showError && amt <= 0) "Enter an amount" else null,
                    focusRequester = focus,
                )
                TextField1(note, { note = it }, "What for (optional)")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (label in listOf("Food", "Transport", "Phone", "Other")) {
                        AssistChip(onClick = { note = label }, label = { Text(label) })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (amt <= 0) {
                    showError = true
                } else {
                    state.data.add(Data.Entry.SPEND, amt, 0L, note.trim(), "")
                    state.changed()
                    onClose()
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("Cancel") } },
    )
}

// ---------- debts ----------

@Composable
private fun DebtFormDialog(state: AppState, existing: Data.Debt?, onClose: () -> Unit) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var amount by remember { mutableStateOf(existing?.start?.toString() ?: "") }
    var showError by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val amt = amount.toLongOrNull() ?: 0L

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(if (existing == null) "Add debt" else "Edit debt") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TextField1(name, { name = it }, "Name", focusRequester = focus)
                AmountField(
                    amount, { amount = it },
                    if (existing == null) "Amount you owe now" else "Original amount",
                    errorText = if (showError && amt <= 0) "Enter how much you owe" else null,
                )
                if (existing != null) {
                    Text(
                        "Payments you logged are subtracted from the original amount.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (amt <= 0) {
                    showError = true
                } else {
                    val n = name.trim().ifEmpty { "Debt" }
                    if (existing == null) {
                        val x = Data.Debt()
                        x.id = state.data.newId()
                        x.name = n
                        x.start = amt
                        state.data.debts.add(x)
                    } else {
                        existing.name = n
                        existing.start = amt
                    }
                    state.changed()
                    onClose()
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("Cancel") } },
    )
}

// ---------- backup ----------

@Composable
private fun RestoreDialog(state: AppState, onClose: () -> Unit) {
    var text by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var pending by remember { mutableStateOf<Data?>(null) }

    val fresh = pending
    if (fresh != null) {
        ConfirmDialog(
            title = "Replace everything?",
            message = "The backup has ${fresh.debts.size} debt(s) and ${fresh.entries.size} entries. " +
                "What is in the app now will be lost.",
            okLabel = "Replace",
            onClose = onClose,
        ) { state.replace(fresh) }
        return
    }

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Restore backup") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Paste the backup text you saved earlier. It replaces everything in the app.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextField1(
                    text, { text = it; error = null }, "Paste backup here",
                    errorText = error, singleLine = false, minLines = 4,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                try {
                    pending = Store.fromJson(text)
                } catch (e: Exception) {
                    error = "That is not a valid backup"
                }
            }) { Text("Restore") }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("Cancel") } },
    )
}
