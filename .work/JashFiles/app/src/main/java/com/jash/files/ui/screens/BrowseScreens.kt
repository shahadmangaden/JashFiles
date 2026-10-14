package com.jash.files.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SdStorage
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jash.files.data.FileCategory
import com.jash.files.data.FileItem
import com.jash.files.data.FileRepo
import com.jash.files.data.kindName
import com.jash.files.ui.AppViewModel
import com.jash.files.ui.Route
import com.jash.files.ui.Tab
import com.jash.files.ui.components.FileIcon
import com.jash.files.ui.components.IOSGroup
import com.jash.files.ui.components.IOSRow
import com.jash.files.ui.components.IOSScreen
import com.jash.files.ui.components.IOSSearchField
import com.jash.files.ui.components.NavIconButton
import com.jash.files.ui.components.NavTextButton
import com.jash.files.ui.components.categoryColor
import com.jash.files.ui.components.categoryIcon
import com.jash.files.ui.components.emptyItem
import com.jash.files.ui.components.fileGrid
import com.jash.files.ui.components.fileList
import com.jash.files.ui.components.loadingItem
import com.jash.files.ui.theme.IOS
import com.jash.files.ui.theme.T
import com.jash.files.util.Fmt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private val iosEase = CubicBezierEasing(0.2f, 0.9f, 0.3f, 1f)

/** Browse tab with an iOS push/pop navigation stack. */
@Composable
fun BrowseTab(vm: AppViewModel) {
    val depth = vm.stack.size
    val top = vm.stack.lastOrNull()
    AnimatedContent(
        targetState = depth to top,
        transitionSpec = {
            val spec = tween<androidx.compose.ui.unit.IntOffset>(380, easing = iosEase)
            if (vm.forward) {
                (slideInHorizontally(spec) { it } togetherWith slideOutHorizontally(spec) { -it / 3 })
                    .apply { targetContentZIndex = 1f }
            } else {
                (slideInHorizontally(spec) { -it / 3 } togetherWith slideOutHorizontally(spec) { it })
                    .apply { targetContentZIndex = -1f }
            }
        },
        label = "nav",
    ) { state ->
        val d = state.first
        val route = state.second
        val backLabel = if (d <= 1) "Browse" else vm.stack.getOrNull(d - 2)?.let { vm.titleOf(it) } ?: "Back"
        when (route) {
            null -> BrowseHome(vm)
            is Route.Folder -> FolderScreen(vm, route.path, d, backLabel)
            is Route.Category -> CategoryScreen(vm, route.cat, d, backLabel)
            is Route.Info -> InfoScreen(vm, route.path, d, backLabel)
        }
    }
}

@Composable
private fun selectionLeading(vm: AppViewModel, all: List<FileItem>?): (@Composable () -> Unit)? =
    if (vm.selecting) {
        { NavTextButton(if (all != null && vm.selected.size == all.size && all.isNotEmpty()) "Deselect All" else "Select All") { vm.selectAll(all.orEmpty().map { it.path }) } }
    } else null

@Composable
fun BrowseHome(vm: AppViewModel) {
    val c = IOS.colors
    val space = remember(vm.refreshTick) { FileRepo.space(vm.root) }
    IOSScreen(
        title = "Browse",
        listState = vm.listState("home"),
        leading = selectionLeading(vm, vm.searchResults),
        actions = {
            if (vm.selecting) NavTextButton("Done", bold = true) { vm.endSelection() }
            else NavIconButton(Icons.Rounded.MoreHoriz, circled = true) { vm.showViewMenu(withSort = false, withSelect = vm.searchResults != null) }
        },
        header = { IOSSearchField(vm.searchQuery, vm::search, "Search all files") },
        bottomPadding = vm.overlayPadding,
    ) {
        if (vm.searchQuery.isNotBlank()) {
            val r = vm.searchResults
            when {
                r == null -> loadingItem()
                r.isEmpty() -> emptyItem("No Results", "Nothing matches “${vm.searchQuery}”.")
                else -> {
                    item(key = "__rhdr") {
                        Text(
                            if (vm.searching) "Searching…" else Fmt.plural(r.size, "Result"),
                            style = T.footnote, color = c.label2,
                            modifier = Modifier.padding(start = 32.dp, top = 8.dp, bottom = 7.dp),
                        )
                    }
                    fileList(vm, r, inset = true)
                }
            }
        } else {
            item(key = "loc") {
                Spacer(Modifier.height(8.dp))
                IOSGroup(header = "Locations", big = true) {
                    IOSRow(
                        "Internal Storage",
                        subtitle = "${Fmt.size(space.second)} free of ${Fmt.size(space.first)}",
                        icon = Icons.Rounded.PhoneAndroid,
                        chevron = true,
                        onClick = { vm.openFolder(vm.root.absolutePath) },
                    )
                    vm.volumes.forEach { v ->
                        val s = FileRepo.space(v)
                        IOSRow(
                            "SD Card",
                            subtitle = "${Fmt.size(s.second)} free of ${Fmt.size(s.first)}",
                            icon = Icons.Rounded.SdStorage,
                            chevron = true,
                            onClick = { vm.openFolder(v.absolutePath) },
                        )
                    }
                    IOSRow(
                        "Downloads",
                        icon = Icons.Rounded.Download,
                        chevron = true,
                        divider = false,
                        onClick = { vm.openFolder(File(vm.root, "Download").absolutePath) },
                    )
                }
            }
            item(key = "fav") {
                IOSGroup(header = "Favorites", big = true) {
                    if (vm.favorites.isEmpty()) {
                        IOSRow("No Favorites", subtitle = "Tap ••• on any folder, then Add to Favorites.", titleColor = c.label2, divider = false)
                    }
                    vm.favorites.forEachIndexed { i, p ->
                        IOSRow(
                            File(p).name,
                            icon = Icons.Rounded.Folder,
                            iconTint = c.folder,
                            chevron = true,
                            divider = i < vm.favorites.lastIndex,
                            onLongClick = { vm.showFavoriteMenu(p) },
                            onClick = { vm.openFolder(p) },
                        )
                    }
                }
            }
            item(key = "cat") {
                IOSGroup(header = "Categories", big = true) {
                    FileCategory.entries.forEachIndexed { i, cat ->
                        IOSRow(
                            cat.title,
                            icon = categoryIcon(cat),
                            iconBg = categoryColor(cat),
                            chevron = true,
                            divider = i < FileCategory.entries.lastIndex,
                            onClick = { vm.push(Route.Category(cat)) },
                        )
                    }
                }
            }
            item(key = "tools") {
                IOSGroup(header = "Tools", big = true) {
                    IOSRow("Storage Analyzer", icon = Icons.Rounded.PieChart, iconBg = c.teal, chevron = true, onClick = { vm.selectTab(Tab.Storage) })
                    IOSRow("Recent Files", icon = Icons.Rounded.Schedule, iconBg = c.indigo, chevron = true, divider = false, onClick = { vm.selectTab(Tab.Recents) })
                }
            }
        }
    }
}

@Composable
fun FolderScreen(vm: AppViewModel, path: String, depth: Int, backLabel: String) {
    val dir = remember(path) { File(path) }
    val hidden = vm.showHidden
    val sort = vm.sort
    val asc = vm.sortAsc
    var query by remember(path) { mutableStateOf("") }
    val items by produceState<List<FileItem>?>(null, path, vm.refreshTick, sort, asc, hidden) {
        value = withContext(Dispatchers.IO) { FileRepo.list(dir, hidden, sort, asc) }
    }
    val shown = remember(items, query) {
        items?.let { l -> if (query.isBlank()) l else l.filter { it.name.contains(query, ignoreCase = true) } }
    }
    val free = remember(path, vm.refreshTick) { FileRepo.freeSpace(dir) }
    val title = vm.folderTitle(path)

    IOSScreen(
        title = if (vm.selecting) "${vm.selected.size} Selected" else title,
        listState = vm.listState("f:$path#$depth"),
        plain = true,
        backLabel = backLabel,
        onBack = { vm.back() },
        leading = selectionLeading(vm, shown),
        actions = {
            if (vm.selecting) NavTextButton("Done", bold = true) { vm.endSelection() }
            else NavIconButton(Icons.Rounded.MoreHoriz, circled = true) { vm.showFolderMenu(dir) }
        },
        header = { IOSSearchField(query, { query = it }) },
        bottomPadding = vm.overlayPadding,
    ) {
        val l = shown
        when {
            l == null -> loadingItem()
            l.isEmpty() -> emptyItem(
                if (query.isBlank()) "Folder Is Empty" else "No Results",
                if (query.isBlank()) "Tap ••• to create a new folder or paste items here." else null,
            )
            vm.grid -> fileGrid(vm, l)
            else -> fileList(vm, l)
        }
        if (!l.isNullOrEmpty()) {
            item(key = "__footer") {
                Text(
                    "${Fmt.plural(l.size, "item")}, ${Fmt.size(free)} available",
                    style = T.footnote, color = IOS.colors.label2, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
                )
            }
        }
    }
}

@Composable
fun CategoryScreen(vm: AppViewModel, cat: FileCategory, depth: Int, backLabel: String) {
    val hidden = vm.showHidden
    val sort = vm.sort
    val asc = vm.sortAsc
    var query by remember(cat) { mutableStateOf("") }
    val raw by produceState<List<FileItem>?>(null, cat, vm.refreshTick, hidden) {
        value = withContext(Dispatchers.IO) { FileRepo.category(cat, hidden) }
    }
    val shown = remember(raw, query, sort, asc) {
        raw?.let { l ->
            val f = if (query.isBlank()) l else l.filter { it.name.contains(query, ignoreCase = true) }
            FileRepo.sortItems(f, sort, asc)
        }
    }
    val useGrid = vm.grid || cat == FileCategory.Images || cat == FileCategory.Videos

    IOSScreen(
        title = if (vm.selecting) "${vm.selected.size} Selected" else cat.title,
        listState = vm.listState("c:${cat.name}#$depth"),
        plain = true,
        backLabel = backLabel,
        onBack = { vm.back() },
        leading = selectionLeading(vm, shown),
        actions = {
            if (vm.selecting) NavTextButton("Done", bold = true) { vm.endSelection() }
            else NavIconButton(Icons.Rounded.MoreHoriz, circled = true) { vm.showViewMenu(withSort = true) }
        },
        header = { IOSSearchField(query, { query = it }, "Search ${cat.title}") },
        bottomPadding = vm.overlayPadding,
    ) {
        val l = shown
        when {
            l == null -> loadingItem()
            l.isEmpty() -> emptyItem("No ${cat.title}", if (query.isBlank()) "Nothing found on this device yet." else null)
            useGrid -> fileGrid(vm, l)
            else -> fileList(vm, l)
        }
        if (!l.isNullOrEmpty()) {
            item(key = "__footer") {
                Text(
                    "${Fmt.plural(l.size, "item")} · ${Fmt.size(l.sumOf { it.size })}",
                    style = T.footnote, color = IOS.colors.label2, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
                )
            }
        }
    }
}

@Composable
fun InfoScreen(vm: AppViewModel, path: String, depth: Int, backLabel: String) {
    val ctx = LocalContext.current
    val c = IOS.colors
    val f = remember(path) { File(path) }
    val info by produceState<FileItem?>(null, path, vm.refreshTick) {
        value = withContext(Dispatchers.IO) { if (f.exists()) FileRepo.toItem(f) else null }
    }
    val folderSize by produceState<Long?>(null, path, vm.refreshTick) {
        value = withContext(Dispatchers.IO) { if (f.isDirectory) FileRepo.folderSize(f) else null }
    }
    val created = remember(path) { FileRepo.created(f) }

    IOSScreen(
        title = "Info",
        listState = vm.listState("i:$path#$depth"),
        backLabel = backLabel,
        onBack = { vm.back() },
    ) {
        val itm = info
        if (itm == null) {
            emptyItem("Item Not Found", "It may have been moved or deleted.")
            return@IOSScreen
        }
        item(key = "hero") {
            Column(
                Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 22.dp, start = 24.dp, end = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                FileIcon(itm, 96.dp)
                Spacer(Modifier.height(12.dp))
                Text(itm.name, style = T.title3, color = c.label, textAlign = TextAlign.Center)
                Spacer(Modifier.height(2.dp))
                val sizeText = if (itm.isDir) folderSize?.let { s -> Fmt.size(s) } ?: "Calculating…" else Fmt.size(itm.size)
                Text("${kindName(itm)} – $sizeText", style = T.subhead, color = c.label2, textAlign = TextAlign.Center)
            }
        }
        item(key = "infoGroup") {
            IOSGroup(header = "Information") {
                IOSRow("Kind", value = kindName(itm))
                IOSRow(
                    "Size",
                    value = if (itm.isDir) (folderSize?.let { s -> Fmt.size(s) } ?: "Calculating…") else Fmt.size(itm.size),
                )
                if (itm.isDir) IOSRow("Contents", value = Fmt.plural(itm.childCount, "item"))
                IOSRow("Where", subtitle = itm.file.parent ?: "/")
                created?.let { ms -> IOSRow("Created", value = Fmt.longDate(ms)) }
                IOSRow("Modified", value = Fmt.longDate(itm.modified), divider = false)
            }
        }
        item(key = "actions") {
            IOSGroup {
                if (itm.isDir) {
                    IOSRow("Open", icon = Icons.Rounded.FolderOpen, onClick = { vm.openFolder(itm.path) })
                    val fav = itm.path in vm.favorites
                    IOSRow(
                        if (fav) "Remove from Favorites" else "Add to Favorites",
                        icon = if (fav) Icons.Rounded.StarOutline else Icons.Rounded.Star,
                        onClick = { vm.toggleFavorite(itm.path) },
                    )
                } else {
                    IOSRow("Open", icon = Icons.Rounded.OpenInNew, onClick = { vm.open(ctx, itm.file, false) })
                    IOSRow("Share", icon = Icons.Rounded.IosShare, onClick = { vm.share(ctx, listOf(itm.file)) })
                }
                IOSRow("Rename", icon = Icons.Rounded.Edit, divider = false, onClick = { vm.askRename(itm) })
            }
        }
        item(key = "delete") {
            IOSGroup {
                IOSRow(
                    "Delete",
                    icon = Icons.Rounded.Delete,
                    iconTint = c.red,
                    titleColor = c.red,
                    divider = false,
                    onClick = { vm.askDelete(listOf(itm.file)) { vm.back() } },
                )
            }
        }
    }
}
