@file:OptIn(ExperimentalMaterial3Api::class)

package com.techmare.paydebt

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.launch

/** Every pop-up the app can show. */
sealed interface Dlg {
    data object Setup : Dlg
    data object GotPaid : Dlg
    data object PaidDebt : Dlg
    data object Spent : Dlg
    data class DebtForm(val debt: Data.Debt?) : Dlg
    data class DeleteDebt(val debt: Data.Debt) : Dlg
    data class DeleteEntry(val entry: Data.Entry) : Dlg
    data object Restore : Dlg
    data object Erase : Dlg
}

private class Tab(val title: String, val label: String, val icon: Int)

private val TABS = listOf(
    Tab("Pay & Debt", "Home", R.drawable.ic_home),
    Tab("History", "History", R.drawable.ic_history),
    Tab("Debts", "Debts", R.drawable.ic_bank),
    Tab("Settings", "Settings", R.drawable.ic_settings),
)

@Composable
fun App(state: AppState, wallpaperColours: Boolean, onWallpaperColours: (Boolean) -> Unit) {
    var tab by rememberSaveable { mutableStateOf(0) }
    var dialog by remember { mutableStateOf<Dlg?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val say: (String) -> Unit = { message -> scope.launch { snackbar.showSnackbar(message) } }

    LaunchedEffect(Unit) {
        val d = state.data
        if (d.debts.isEmpty() && d.entries.isEmpty() && d.settings.weeklyDebtTarget == 0L) dialog = Dlg.Setup
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(TABS[tab].title) }) },
        bottomBar = {
            NavigationBar {
                TABS.forEachIndexed { i, t ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Icon(painterResource(t.icon), contentDescription = null) },
                        label = { Text(t.label) },
                    )
                }
            }
        },
        floatingActionButton = {
            when (tab) {
                0 -> ExtendedFloatingActionButton(
                    onClick = { dialog = Dlg.GotPaid },
                    icon = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
                    text = { Text("Got paid") },
                )
                2 -> ExtendedFloatingActionButton(
                    onClick = { dialog = Dlg.DebtForm(null) },
                    icon = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
                    text = { Text("Add debt") },
                )
                else -> {}
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        val modifier = Modifier.padding(padding)
        when (tab) {
            0 -> HomeScreen(state, modifier, onDialog = { dialog = it }, onOpenSettings = { tab = 3 })
            1 -> HistoryScreen(state, modifier, onDialog = { dialog = it })
            2 -> DebtsScreen(state, modifier, onDialog = { dialog = it })
            else -> SettingsScreen(state, modifier, wallpaperColours, onWallpaperColours, onDialog = { dialog = it }, say = say)
        }
    }

    dialog?.let { Dialogs(state, it, onClose = { dialog = null }, say = say) }
}
