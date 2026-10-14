package com.jash.files

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jash.files.ui.AppViewModel
import com.jash.files.ui.Tab
import com.jash.files.ui.components.ActionSheetHost
import com.jash.files.ui.components.AlertHost
import com.jash.files.ui.components.BottomBar
import com.jash.files.ui.components.BusyHost
import com.jash.files.ui.components.HudHost
import com.jash.files.ui.components.PasteBar
import com.jash.files.ui.components.SelectionToolbar
import com.jash.files.ui.components.TabItem
import com.jash.files.ui.screens.BrowseTab
import com.jash.files.ui.screens.PermissionScreen
import com.jash.files.ui.screens.RecentsScreen
import com.jash.files.ui.screens.SettingsScreen
import com.jash.files.ui.screens.StorageScreen
import com.jash.files.ui.theme.IOS
import com.jash.files.ui.theme.JashTheme
import com.jash.files.util.Perms

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: AppViewModel = viewModel()
            val dark = when (vm.themeMode) {
                1 -> false
                2 -> true
                else -> isSystemInDarkTheme()
            }
            JashTheme(dark) { JashApp(vm) }
        }
    }
}

private fun tabIcon(t: Tab): ImageVector = when (t) {
    Tab.Recents -> Icons.Rounded.Schedule
    Tab.Browse -> Icons.Rounded.Folder
    Tab.Storage -> Icons.Rounded.PieChart
    Tab.Settings -> Icons.Rounded.Settings
}

@Composable
fun JashApp(vm: AppViewModel) {
    val c = IOS.colors
    val ctx = LocalContext.current
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) vm.refreshPermission() }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
    val legacyLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        vm.refreshPermission()
    }

    Box(Modifier.fillMaxSize().background(c.bg)) {
        if (!vm.hasPermission) {
            PermissionScreen {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Perms.openAllFilesSettings(ctx)
                else legacyLauncher.launch(
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                )
            }
        } else {
            BackHandler(enabled = vm.canGoBack) { vm.back() }
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f)) {
                    when (vm.tab) {
                        Tab.Recents -> RecentsScreen(vm)
                        Tab.Browse -> BrowseTab(vm)
                        Tab.Storage -> StorageScreen(vm)
                        Tab.Settings -> SettingsScreen(vm)
                    }
                    if (!vm.selecting) PasteBar(vm, Modifier.align(Alignment.BottomCenter))
                }
                if (vm.selecting) {
                    SelectionToolbar(vm)
                } else {
                    BottomBar {
                        Tab.entries.forEach { t ->
                            TabItem(tabIcon(t), t.title, t == vm.tab, Modifier.weight(1f)) { vm.selectTab(t) }
                        }
                    }
                }
            }
            ActionSheetHost(vm.sheet) { vm.sheet = null }
            HudHost(vm.hud)
            BusyHost(vm.busy)
        }
        AlertHost(vm.alert) { vm.alert = null }
    }
}
