package com.jash.files.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jash.files.BuildConfig
import com.jash.files.data.FileCategory
import com.jash.files.data.FileItem
import com.jash.files.data.FileRepo
import com.jash.files.ui.AlertSpec
import com.jash.files.ui.AppViewModel
import com.jash.files.ui.Route
import com.jash.files.ui.components.AppIconBadge
import com.jash.files.ui.components.IOSGroup
import com.jash.files.ui.components.IOSRow
import com.jash.files.ui.components.IOSScreen
import com.jash.files.ui.components.IOSSearchField
import com.jash.files.ui.components.IOSSwitch
import com.jash.files.ui.components.NavIconButton
import com.jash.files.ui.components.NavTextButton
import com.jash.files.ui.components.PrimaryButton
import com.jash.files.ui.components.SegmentedControl
import com.jash.files.ui.components.categoryColor
import com.jash.files.ui.components.categoryIcon
import com.jash.files.ui.components.emptyItem
import com.jash.files.ui.components.fileGrid
import com.jash.files.ui.components.fileList
import com.jash.files.ui.components.loadingItem
import com.jash.files.ui.theme.IOS
import com.jash.files.ui.theme.T
import com.jash.files.util.Fmt
import com.jash.files.util.Perms
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ---------------- Recents ----------------

@Composable
fun RecentsScreen(vm: AppViewModel) {
    val hidden = vm.showHidden
    var query by remember { mutableStateOf("") }
    val items by produceState<List<FileItem>?>(null, vm.refreshTick, hidden) {
        value = withContext(Dispatchers.IO) { FileRepo.recents(hidden) }
    }
    val shown = remember(items, query) {
        items?.let { l -> if (query.isBlank()) l else l.filter { it.name.contains(query, ignoreCase = true) } }
    }
    val leading: (@Composable () -> Unit)? = if (vm.selecting) {
        { NavTextButton("Select All") { vm.selectAll(shown.orEmpty().map { it.path }) } }
    } else null

    IOSScreen(
        title = if (vm.selecting) "${vm.selected.size} Selected" else "Recents",
        listState = vm.listState("recents"),
        plain = true,
        leading = leading,
        actions = {
            if (vm.selecting) NavTextButton("Done", bold = true) { vm.endSelection() }
            else NavIconButton(Icons.Rounded.MoreHoriz, circled = true) { vm.showViewMenu(withSort = false) }
        },
        header = { IOSSearchField(query, { query = it }, "Search Recents") },
        bottomPadding = vm.overlayPadding,
    ) {
        val l = shown
        when {
            l == null -> loadingItem()
            l.isEmpty() -> emptyItem(
                if (query.isBlank()) "No Recent Files" else "No Results",
                if (query.isBlank()) "Files you add or change in the last 30 days appear here." else null,
            )
            vm.grid -> fileGrid(vm, l)
            else -> fileList(vm, l)
        }
    }
}

// ---------------- Storage ----------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StorageScreen(vm: AppViewModel) {
    val c = IOS.colors
    LaunchedEffect(vm.storage) { vm.loadStorage() }
    val s = vm.storage
    val space = remember(vm.refreshTick) { FileRepo.space(vm.root) }
    val total = space.first
    val free = space.second
    val used = (total - free).coerceAtLeast(0)

    IOSScreen(
        title = "Storage",
        listState = vm.listState("storage"),
        actions = { NavIconButton(Icons.Rounded.Refresh) { vm.loadStorage(force = true) } },
        bottomPadding = vm.overlayPadding,
    ) {
        item(key = "summary") {
            Spacer(Modifier.height(8.dp))
            IOSGroup {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("Internal Storage", style = T.headline, color = c.label, modifier = Modifier.weight(1f))
                        Text("${Fmt.size(used)} of ${Fmt.size(total)} Used", style = T.subhead, color = c.label2)
                    }
                    Spacer(Modifier.height(14.dp))
                    val segments: List<Pair<Color, Long>> = if (s == null) emptyList() else
                        FileCategory.entries.map { categoryColor(it) to (s.byCategory[it] ?: 0L) } + (c.gray to s.other)
                    Row(
                        Modifier.fillMaxWidth().height(24.dp).clip(RoundedCornerShape(6.dp)).background(c.fill),
                    ) {
                        if (total > 0) {
                            if (s == null) {
                                Box(Modifier.weight((used.toFloat() / total).coerceAtLeast(0.001f)).fillMaxHeight().background(c.gray.copy(alpha = 0.5f)))
                            } else {
                                segments.forEach { (color, bytes) ->
                                    val w = bytes.toFloat() / total
                                    if (w > 0.003f) {
                                        Box(Modifier.weight(w).fillMaxHeight().background(color))
                                        Box(Modifier.width(1.dp).fillMaxHeight().background(c.card))
                                    }
                                }
                            }
                            Box(Modifier.weight((free.toFloat() / total).coerceAtLeast(0.001f)).fillMaxHeight())
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    if (s == null) {
                        Text("Calculating…", style = T.footnote, color = c.label2)
                    } else {
                        FlowRow(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(14.dp)) {
                            FileCategory.entries.forEach { Legend(categoryColor(it), it.title) }
                            Legend(c.gray, "System & Other")
                        }
                    }
                }
            }
        }
        item(key = "cats") {
            IOSGroup(header = "Categories", footer = "Free space: ${Fmt.size(free)}") {
                FileCategory.entries.forEach { cat ->
                    IOSRow(
                        cat.title,
                        icon = categoryIcon(cat),
                        iconBg = categoryColor(cat),
                        value = s?.let { Fmt.size(it.byCategory[cat] ?: 0L) } ?: "—",
                        chevron = true,
                        onClick = { vm.push(Route.Category(cat)) },
                    )
                }
                IOSRow(
                    "System & Other",
                    icon = Icons.Rounded.Folder,
                    iconBg = c.gray,
                    value = s?.let { Fmt.size(it.other) } ?: "—",
                    divider = false,
                )
            }
        }
        if (s != null && s.large.isNotEmpty()) {
            item(key = "largeHdr") {
                Text(
                    "LARGE FILES", style = T.footnote, color = c.label2,
                    modifier = Modifier.padding(start = 32.dp, bottom = 7.dp),
                )
            }
            fileList(vm, s.large, inset = true) { "${Fmt.size(it.size)}  ·  ${it.file.parentFile?.name ?: ""}" }
            item(key = "largeFtr") {
                Text(
                    "Files over 10 MB, biggest first. Tap ••• to delete or move them.",
                    style = T.footnote, color = c.label2,
                    modifier = Modifier.padding(start = 32.dp, end = 32.dp, top = 7.dp),
                )
            }
        }
    }
}

@Composable
private fun Legend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Box(Modifier.size(9.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(5.dp))
        Text(label, style = T.caption, color = IOS.colors.label2)
    }
}

// ---------------- Settings ----------------

@Composable
fun SettingsScreen(vm: AppViewModel) {
    val c = IOS.colors
    val ctx = LocalContext.current
    IOSScreen(title = "Settings", listState = vm.listState("settings")) {
        item(key = "profile") {
            Spacer(Modifier.height(8.dp))
            IOSGroup {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    AppIconBadge(60.dp)
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text("Jash Files", style = T.title2, color = c.label)
                        Text("File Manager · Version ${BuildConfig.VERSION_NAME}", style = T.subhead, color = c.label2)
                    }
                }
            }
        }
        item(key = "appearance") {
            IOSGroup(header = "Appearance") {
                Box(Modifier.padding(12.dp)) {
                    SegmentedControl(listOf("Automatic", "Light", "Dark"), vm.themeMode) { vm.setTheme(it) }
                }
            }
        }
        item(key = "files") {
            IOSGroup(header = "Files", footer = "Hidden files start with a dot (.) and are usually settings or app data.") {
                IOSRow(
                    "Show Hidden Files", icon = Icons.Rounded.Visibility, iconBg = c.gray,
                    trailing = { IOSSwitch(vm.showHidden) { vm.toggleHidden() } },
                )
                IOSRow(
                    "Confirm Before Deleting", icon = Icons.Rounded.Delete, iconBg = c.red,
                    trailing = { IOSSwitch(vm.confirmDelete) { vm.toggleConfirmDelete() } },
                )
                IOSRow(
                    "View As", icon = Icons.Rounded.GridView, iconBg = c.blue,
                    value = if (vm.grid) "Icons" else "List", chevron = true, onClick = { vm.toggleGrid() },
                )
                IOSRow(
                    "Sort By", icon = Icons.Rounded.Sort, iconBg = c.orange,
                    value = vm.sort.title + if (vm.sortAsc) " ↑" else " ↓", chevron = true, divider = false,
                    onClick = { vm.showSortMenu() },
                )
            }
        }
        item(key = "favs") {
            IOSGroup(header = "Favorites") {
                IOSRow(
                    "Reset Favorites", icon = Icons.Rounded.Star, iconBg = c.yellow, divider = false,
                    onClick = {
                        vm.alert = AlertSpec("Reset Favorites?", "Your favorites will go back to Download, Documents, DCIM and Pictures.", confirm = "Reset") {
                            vm.resetFavorites()
                        }
                    },
                )
            }
        }
        item(key = "about") {
            IOSGroup(header = "About", footer = "Jash Files works fully offline. Your files never leave this device.") {
                IOSRow(
                    "Storage Access", icon = Icons.Rounded.Lock, iconBg = c.green, value = "Allowed", chevron = true,
                    onClick = { Perms.openAppSettings(ctx) },
                )
                IOSRow("Storage Root", icon = Icons.Rounded.Info, iconBg = c.indigo, subtitle = vm.root.absolutePath)
                IOSRow("Features", icon = Icons.Rounded.FolderZip, iconBg = c.teal, subtitle = "Browse · Search · Copy · Move · Rename · ZIP · Extract · Share · Storage analyzer", divider = false)
            }
        }
    }
}

// ---------------- Permission / onboarding ----------------

@Composable
fun PermissionScreen(onContinue: () -> Unit) {
    val c = IOS.colors
    Column(
        Modifier
            .fillMaxSize()
            .background(c.plainBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        AppIconBadge(96.dp)
        Spacer(Modifier.height(24.dp))
        Text("Welcome to\nJash Files", style = T.largeTitle, color = c.label, textAlign = TextAlign.Center)
        Spacer(Modifier.height(36.dp))
        Feature(Icons.Rounded.Folder, c.blue, "Browse Everything", "Internal storage, SD cards, downloads and categories in one place.")
        Feature(Icons.Rounded.ContentCopy, c.orange, "Organize Fast", "Copy, move, rename, share and delete with a tap.")
        Feature(Icons.Rounded.FolderZip, c.green, "Compress & Extract", "Create and open ZIP archives right on your phone.")
        Spacer(Modifier.weight(1f))
        Text(
            "Jash Files needs “All files access” to manage your files. Nothing leaves your device.",
            style = T.footnote, color = c.label2, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        PrimaryButton("Continue", onClick = onContinue)
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun Feature(icon: ImageVector, tint: Color, title: String, text: String) {
    val c = IOS.colors
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(38.dp))
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, style = T.headline, fontWeight = FontWeight.SemiBold, color = c.label)
            Text(text, style = T.subhead, color = c.label2)
        }
    }
}
