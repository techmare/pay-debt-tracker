package com.techmare.paydebt

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.techmare.paydebt.core.Fmt
import com.techmare.paydebt.core.Payoff

@Composable
fun DebtsScreen(state: AppState, modifier: Modifier, onDialog: (Dlg) -> Unit) {
    val d = state.read()
    val colors = MaterialTheme.colorScheme

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        if (d.debts.isEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text("No debts yet.", style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
            Text(
                "Add what you owe with the Add debt button.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
        } else {
            Text(
                "With several debts, finish the smallest one first.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
        }

        val focus = d.focusDebt()
        for (x in d.debts) {
            val balance = d.balance(x)
            val first = x == focus && d.debts.size > 1
            val container = if (first) colors.primaryContainer else colors.surfaceContainerLow
            val onContainer = if (first) colors.onPrimaryContainer else colors.onSurface
            val onVariant = if (first) colors.onPrimaryContainer else colors.onSurfaceVariant
            Card(
                colors = CardDefaults.cardColors(containerColor = container),
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            ) {
                Column(Modifier.padding(start = 20.dp, end = 12.dp, top = 20.dp, bottom = 8.dp)) {
                    Text(x.name, style = MaterialTheme.typography.titleLarge, color = onContainer)
                    Text(
                        if (balance > 0) Fmt.dh(balance) + " left" else "Paid off",
                        style = MaterialTheme.typography.headlineMedium,
                        color = onContainer,
                    )
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { Payoff.percentPaid(x.start, balance) / 100f },
                        modifier = Modifier.fillMaxWidth().height(8.dp).padding(end = 8.dp),
                        color = colors.primary,
                        trackColor = if (first) colors.surfaceContainerLowest else colors.surfaceContainerHighest,
                        strokeCap = StrokeCap.Round,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Started at " + Fmt.dh(x.start) + if (first) ". Pay this one first." else "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = onVariant,
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { onDialog(Dlg.DebtForm(x)) }) { Text("Edit") }
                        TextButton(
                            onClick = { onDialog(Dlg.DeleteDebt(x)) },
                            colors = ButtonDefaults.textButtonColors(contentColor = colors.error),
                        ) { Text("Delete") }
                    }
                }
            }
        }
        Spacer(Modifier.height(96.dp)) // room for the floating button
    }
}
