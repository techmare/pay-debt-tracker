@file:OptIn(ExperimentalMaterial3Api::class)

package com.techmare.paydebt

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

private fun Long.asField(): String = if (this > 0) toString() else ""

@Composable
fun SettingsScreen(
    state: AppState,
    modifier: Modifier,
    wallpaperColours: Boolean,
    onWallpaperColours: (Boolean) -> Unit,
    onDialog: (Dlg) -> Unit,
    say: (String) -> Unit,
) {
    val d = state.read()
    val s = d.settings
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current

    var target by remember(d) { mutableStateOf(s.weeklyDebtTarget.asField()) }
    var pay by remember(d) { mutableStateOf(s.weeklyPay.asField()) }
    var cash by remember(d) { mutableStateOf(s.startingCash.asField()) }
    var reminderOn by remember(d) { mutableStateOf(s.reminderOn) }
    var day by remember(d) { mutableStateOf(s.reminderDay) }
    var hour by remember(d) { mutableStateOf(s.reminderHour) }

    val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Section("Your plan")
        AmountField(target, { target = it }, "Debt payment each week")
        AmountField(pay, { pay = it }, "Pay each week (rough)")
        AmountField(cash, { cash = it }, "Cash you had when you started")

        Spacer(Modifier.height(8.dp))
        Section("Weekly reminder")
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Remind me on pay day",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Switch(checked = reminderOn, onCheckedChange = { reminderOn = it })
        }
        if (reminderOn) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (i in 1..7) {
                    FilterChip(
                        selected = day == i,
                        onClick = { day = i },
                        label = { Text(DayOfWeek.of(i).getDisplayName(TextStyle.SHORT, Locale.ENGLISH)) },
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("At", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.width(12.dp))
                IconButton(onClick = { hour = (hour + 23) % 24 }) {
                    Icon(painterResource(R.drawable.ic_remove), contentDescription = "One hour earlier")
                }
                Text(
                    String.format(Locale.ENGLISH, "%02d:00", hour),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.width(72.dp),
                )
                IconButton(onClick = { hour = (hour + 1) % 24 }) {
                    Icon(painterResource(R.drawable.ic_add), contentDescription = "One hour later")
                }
            }
        }

        if (Build.VERSION.SDK_INT >= 31) {
            Spacer(Modifier.height(8.dp))
            Section("Colours")
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Use wallpaper colours", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Off uses the teal and amber theme.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
                Switch(checked = wallpaperColours, onCheckedChange = onWallpaperColours)
            }
        }

        Button(
            onClick = {
                s.weeklyDebtTarget = target.toLongOrNull() ?: 0L
                s.weeklyPay = pay.toLongOrNull() ?: 0L
                s.startingCash = cash.toLongOrNull() ?: 0L
                s.reminderOn = reminderOn
                s.reminderDay = day
                s.reminderHour = hour
                if (reminderOn && Build.VERSION.SDK_INT >= 33 &&
                    context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                ) {
                    askPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                state.changed()
                say("Saved")
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) { Text("Save") }

        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        Section("Backup")
        Text(
            "Your data lives only on this phone. If you lose the phone you lose the data, so share a backup " +
                "to yourself now and then.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = {
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "Pay & Debt backup")
                    putExtra(Intent.EXTRA_TEXT, Store.toJson(d, true))
                }
                context.startActivity(Intent.createChooser(send, "Save backup"))
            }) { Text("Share backup") }
            TextButton(onClick = { onDialog(Dlg.Restore) }) { Text("Restore") }
            TextButton(
                onClick = { onDialog(Dlg.Erase) },
                colors = ButtonDefaults.textButtonColors(contentColor = colors.error),
            ) { Text("Erase all") }
        }
        Text("Pay & Debt 3.0", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Section(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
}
