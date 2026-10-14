package com.jash.files.ui

import android.app.Application
import android.content.Context
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DriveFileMove
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FileCopy
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material.icons.rounded.ViewList
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jash.files.data.FileCategory
import com.jash.files.data.FileItem
import com.jash.files.data.FileRepo
import com.jash.files.data.Prefs
import com.jash.files.data.SortMode
import com.jash.files.data.StorageStats
import com.jash.files.util.Fmt
import com.jash.files.util.Intents
import com.jash.files.util.Perms
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class Tab(val title: String) { Recents("Recents"), Browse("Browse"), Storage("Storage"), Settings("Settings") }

sealed interface Route {
    val key: String

    data class Folder(val path: String) : Route {
        override val key: String get() = "f:$path"
    }

    data class Category(val cat: FileCategory) : Route {
        override val key: String get() = "c:${cat.name}"
    }

    data class Info(val path: String) : Route {
        override val key: String get() = "i:$path"
    }
}

data class SheetAction(
    val label: String,
    val icon: ImageVector? = null,
    val destructive: Boolean = false,
    val onClick: () -> Unit,
)

data class SheetSpec(
    val title: String? = null,
    val message: String? = null,
    val header: FileItem? = null,
    val actions: List<SheetAction>,
)

data class AlertSpec(
    val title: String,
    val message: String? = null,
    val input: String? = null,
    val selectBase: Boolean = false,
    val confirm: String = "OK",
    val cancel: String? = "Cancel",
    val destructive: Boolean = false,
    val onConfirm: (String) -> Unit = {},
)

data class Clip(val paths: List<String>, val move: Boolean)

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = Prefs(app)
    val root: File = FileRepo.root
    val volumes: List<File> = FileRepo.removableRoots(app)

    var hasPermission by mutableStateOf(Perms.has(app)); private set
    var tab by mutableStateOf(Tab.Browse); private set
    val stack = mutableStateListOf<Route>()
    var forward by mutableStateOf(true); private set
    private val listStates = HashMap<String, LazyListState>()

    var refreshTick by mutableIntStateOf(0); private set
    var sort by mutableStateOf(prefs.sort); private set
    var sortAsc by mutableStateOf(prefs.sortAsc); private set
    var grid by mutableStateOf(prefs.grid); private set
    var showHidden by mutableStateOf(prefs.showHidden); private set
    var themeMode by mutableIntStateOf(prefs.theme); private set
    var confirmDelete by mutableStateOf(prefs.confirmDelete); private set
    val favorites = mutableStateListOf<String>().also { it.addAll(prefs.favorites(root)) }

    var selecting by mutableStateOf(false); private set
    val selected = mutableStateListOf<String>()
    var clipboard by mutableStateOf<Clip?>(null); private set

    var sheet by mutableStateOf<SheetSpec?>(null)
    var alert by mutableStateOf<AlertSpec?>(null)
    var hud by mutableStateOf<String?>(null); private set
    var busy by mutableStateOf<String?>(null); private set

    var storage by mutableStateOf<StorageStats?>(null); private set
    var storageLoading by mutableStateOf(false); private set

    var searchQuery by mutableStateOf(""); private set
    var searchResults by mutableStateOf<List<FileItem>?>(null); private set
    var searching by mutableStateOf(false); private set
    private var searchJob: Job? = null
    private var hudJob: Job? = null

    val overlayPadding: Dp get() = if (clipboard != null) 84.dp else 0.dp

    // ---------- Navigation ----------

    fun listState(key: String): LazyListState = listStates.getOrPut(key) { LazyListState() }

    fun refreshPermission() {
        hasPermission = Perms.has(getApplication())
        refreshTick++
    }

    fun refresh() {
        refreshTick++
    }

    fun selectTab(t: Tab) {
        endSelection()
        if (t == tab && t == Tab.Browse && stack.isNotEmpty()) {
            forward = false
            stack.clear()
        }
        tab = t
    }

    fun push(r: Route) {
        endSelection()
        forward = true
        tab = Tab.Browse
        stack.add(r)
    }

    fun openFolder(path: String) = push(Route.Folder(path))

    val canGoBack: Boolean
        get() = sheet != null || selecting || stack.isNotEmpty() || tab != Tab.Browse

    fun back() {
        when {
            sheet != null -> sheet = null
            selecting -> endSelection()
            stack.isNotEmpty() -> {
                forward = false
                val depth = stack.size
                val r = stack.removeAt(stack.lastIndex)
                listStates.remove("${r.key}#$depth")
            }
            tab != Tab.Browse -> tab = Tab.Browse
        }
    }

    fun titleOf(r: Route): String = when (r) {
        is Route.Folder -> folderTitle(r.path)
        is Route.Category -> r.cat.title
        is Route.Info -> File(r.path).name
    }

    fun folderTitle(path: String): String = when {
        path == root.absolutePath -> "Internal Storage"
        volumes.any { it.absolutePath == path } -> "SD Card"
        else -> File(path).name
    }

    // ---------- Items ----------

    fun tapItem(ctx: Context, item: FileItem) {
        if (selecting) {
            toggleSelect(item.path)
            return
        }
        when {
            item.isDir -> openFolder(item.path)
            item.ext == "zip" -> sheet = SheetSpec(
                header = item,
                actions = listOf(
                    SheetAction("Extract Here", Icons.Rounded.Unarchive) { extract(item.file) },
                    SheetAction("Open With…", Icons.Rounded.OpenInNew) { open(ctx, item.file, true) },
                ),
            )
            else -> open(ctx, item.file, false)
        }
    }

    fun open(ctx: Context, f: File, chooser: Boolean) {
        if (!Intents.open(ctx, f, chooser)) {
            alert = AlertSpec("No App Available", "There's no app on this device that can open “${f.name}”.", cancel = null)
        }
    }

    fun share(ctx: Context, files: List<File>) {
        val onlyFiles = files.filter { it.isFile }
        if (onlyFiles.isEmpty()) {
            alert = AlertSpec("Can't Share Folders", "Compress the folder first, then share the ZIP file.", cancel = null)
            return
        }
        Intents.share(ctx, onlyFiles)
    }

    fun showItemMenu(ctx: Context, item: FileItem) {
        val f = item.file
        val a = mutableListOf<SheetAction>()
        if (item.isDir) {
            a += SheetAction("Open", Icons.Rounded.FolderOpen) { openFolder(item.path) }
        } else {
            a += SheetAction("Open", Icons.Rounded.OpenInNew) { open(ctx, f, false) }
            a += SheetAction("Open With…", Icons.Rounded.OpenInNew) { open(ctx, f, true) }
            a += SheetAction("Share", Icons.Rounded.IosShare) { share(ctx, listOf(f)) }
        }
        a += SheetAction("Rename", Icons.Rounded.Edit) { askRename(item) }
        a += SheetAction("Copy", Icons.Rounded.ContentCopy) { setClip(listOf(item.path), false) }
        a += SheetAction("Move", Icons.Rounded.DriveFileMove) { setClip(listOf(item.path), true) }
        a += SheetAction("Duplicate", Icons.Rounded.FileCopy) { duplicate(f) }
        a += SheetAction("Compress", Icons.Rounded.FolderZip) { compress(listOf(f)) }
        if (item.ext == "zip") a += SheetAction("Extract", Icons.Rounded.Unarchive) { extract(f) }
        if (item.isDir) {
            val fav = item.path in favorites
            a += SheetAction(
                if (fav) "Remove from Favorites" else "Add to Favorites",
                if (fav) Icons.Rounded.StarOutline else Icons.Rounded.Star,
            ) { toggleFavorite(item.path) }
        }
        a += SheetAction("Get Info", Icons.Rounded.Info) { push(Route.Info(item.path)) }
        a += SheetAction("Select", Icons.Rounded.CheckCircleOutline) { startSelection(item.path) }
        a += SheetAction("Delete", Icons.Rounded.Delete, destructive = true) { askDelete(listOf(f)) }
        sheet = SheetSpec(header = item, actions = a)
    }

    private fun sortActions(): List<SheetAction> = SortMode.entries.map { m ->
        val arrow = if (sort == m) (if (sortAsc) "  ↑" else "  ↓") else ""
        SheetAction("Sort by ${m.title}$arrow", if (sort == m) Icons.Rounded.Check else null) { setSortMode(m) }
    }

    fun showFolderMenu(dir: File) {
        val a = mutableListOf<SheetAction>()
        a += SheetAction("Select", Icons.Rounded.CheckCircleOutline) { startSelection() }
        a += SheetAction("New Folder", Icons.Rounded.CreateNewFolder) { askNewFolder(dir) }
        clipboard?.let { c ->
            a += SheetAction("${if (c.move) "Move" else "Paste"} ${Fmt.plural(c.paths.size, "Item")} Here", Icons.Rounded.ContentPaste) { paste(dir) }
        }
        a += SheetAction(
            if (grid) "View as List" else "View as Icons",
            if (grid) Icons.Rounded.ViewList else Icons.Rounded.GridView,
        ) { toggleGrid() }
        a += sortActions()
        a += SheetAction(
            if (showHidden) "Hide Hidden Files" else "Show Hidden Files",
            if (showHidden) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
        ) { toggleHidden() }
        val path = dir.absolutePath
        val fav = path in favorites
        a += SheetAction(
            if (fav) "Remove from Favorites" else "Add to Favorites",
            if (fav) Icons.Rounded.StarOutline else Icons.Rounded.Star,
        ) { toggleFavorite(path) }
        a += SheetAction("Get Info", Icons.Rounded.Info) { push(Route.Info(path)) }
        sheet = SheetSpec(title = folderTitle(path), actions = a)
    }

    fun showViewMenu(withSort: Boolean, withSelect: Boolean = true) {
        val a = mutableListOf<SheetAction>()
        if (withSelect) a += SheetAction("Select", Icons.Rounded.CheckCircleOutline) { startSelection() }
        a += SheetAction(
            if (grid) "View as List" else "View as Icons",
            if (grid) Icons.Rounded.ViewList else Icons.Rounded.GridView,
        ) { toggleGrid() }
        if (withSort) a += sortActions()
        a += SheetAction(
            if (showHidden) "Hide Hidden Files" else "Show Hidden Files",
            if (showHidden) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
        ) { toggleHidden() }
        sheet = SheetSpec(actions = a)
    }

    fun showSortMenu() {
        sheet = SheetSpec(title = "Sort By", actions = sortActions())
    }

    fun showFavoriteMenu(path: String) {
        sheet = SheetSpec(
            title = File(path).name,
            actions = listOf(
                SheetAction("Open", Icons.Rounded.FolderOpen) { openFolder(path) },
                SheetAction("Remove from Favorites", Icons.Rounded.StarOutline, destructive = true) { toggleFavorite(path) },
            ),
        )
    }

    // ---------- File operations ----------

    private fun op(busyLabel: String, done: String?, then: () -> Unit = {}, block: () -> Unit) {
        viewModelScope.launch {
            busy = busyLabel
            try {
                withContext(Dispatchers.IO) { block() }
                done?.let { showHud(it) }
                then()
            } catch (e: Exception) {
                alert = AlertSpec("Couldn't Complete", e.message ?: "Something went wrong.", cancel = null)
            } finally {
                busy = null
                refreshTick++
                storage = null
            }
        }
    }

    fun askRename(item: FileItem) {
        alert = AlertSpec(
            title = "Rename",
            message = "Enter a new name for “${item.name}”.",
            input = item.name,
            selectBase = !item.isDir,
            confirm = "Save",
        ) { n ->
            val name = n.trim()
            when {
                name.isEmpty() || name == item.name -> Unit
                name.contains('/') -> alert = AlertSpec("Invalid Name", "Names can't contain “/”.", cancel = null)
                else -> {
                    val wasFav = item.path in favorites
                    op("Renaming…", null, then = {
                        if (wasFav) {
                            val i = favorites.indexOf(item.path)
                            if (i >= 0) favorites[i] = File(item.file.parentFile, name).absolutePath
                            prefs.setFavorites(favorites)
                        }
                    }) { FileRepo.rename(item.file, name) }
                }
            }
        }
    }

    fun askNewFolder(dir: File) {
        alert = AlertSpec("New Folder", "Enter a name for this folder.", input = "untitled folder", confirm = "Create") { n ->
            val name = n.trim().ifEmpty { "untitled folder" }.replace("/", "-")
            op("Creating…", null) { FileRepo.createFolder(dir, name) }
        }
    }

    fun askDelete(files: List<File>, then: () -> Unit = {}) {
        if (files.isEmpty()) return
        val doIt = {
            val paths = files.map { it.absolutePath }
            favorites.removeAll { p -> paths.any { p == it || p.startsWith("$it/") } }
            prefs.setFavorites(favorites)
            endSelection()
            op("Deleting…", if (files.size == 1) "Deleted" else "Deleted ${files.size} items", then) { FileRepo.delete(files) }
        }
        if (!confirmDelete) {
            doIt()
            return
        }
        val title = if (files.size == 1) "Delete “${files[0].name}”?" else "Delete ${files.size} Items?"
        alert = AlertSpec(title, "This action can't be undone.", confirm = "Delete", destructive = true) { doIt() }
    }

    fun setClip(paths: List<String>, move: Boolean) {
        clipboard = Clip(paths, move)
        endSelection()
        showHud(if (move) "Open a folder to move into" else "Copied ${Fmt.plural(paths.size, "item")}")
    }

    fun cancelClip() {
        clipboard = null
    }

    fun paste(dest: File) {
        val clip = clipboard ?: return
        val files = clip.paths.map(::File)
        clipboard = null
        op(
            if (clip.move) "Moving…" else "Copying…",
            if (clip.move) "Moved ${Fmt.plural(files.size, "item")}" else "Pasted ${Fmt.plural(files.size, "item")}",
        ) { if (clip.move) FileRepo.move(files, dest) else FileRepo.copy(files, dest) }
    }

    fun duplicate(f: File) {
        val parent = f.parentFile ?: return
        op("Duplicating…", "Duplicated") { FileRepo.copy(listOf(f), parent) }
    }

    fun compress(files: List<File>) {
        if (files.isEmpty()) return
        val parent = files[0].parentFile ?: return
        endSelection()
        op("Compressing…", "Compressed") { FileRepo.compress(files, parent) }
    }

    fun extract(f: File) {
        val parent = f.parentFile ?: return
        op("Extracting…", "Extracted") { FileRepo.extract(f, parent) }
    }

    // ---------- Selection ----------

    fun startSelection(first: String? = null) {
        selecting = true
        selected.clear()
        first?.let { selected.add(it) }
    }

    fun endSelection() {
        selecting = false
        selected.clear()
    }

    fun toggleSelect(path: String) {
        if (path in selected) selected.remove(path) else selected.add(path)
    }

    fun selectAll(paths: List<String>) {
        if (selected.size == paths.size) {
            selected.clear()
        } else {
            selected.clear()
            selected.addAll(paths)
        }
    }

    fun selectedFiles(): List<File> = selected.map(::File)

    // ---------- Settings ----------

    fun setSortMode(m: SortMode) {
        if (sort == m) {
            sortAsc = !sortAsc
        } else {
            sort = m
            sortAsc = m == SortMode.Name || m == SortMode.Kind
        }
        prefs.sort = sort
        prefs.sortAsc = sortAsc
    }

    fun toggleGrid() {
        grid = !grid
        prefs.grid = grid
    }

    fun toggleHidden() {
        showHidden = !showHidden
        prefs.showHidden = showHidden
        if (searchQuery.isNotBlank()) search(searchQuery)
    }

    fun setTheme(mode: Int) {
        themeMode = mode
        prefs.theme = mode
    }

    fun toggleConfirmDelete() {
        confirmDelete = !confirmDelete
        prefs.confirmDelete = confirmDelete
    }

    fun toggleFavorite(path: String) {
        if (path in favorites) {
            favorites.remove(path)
            showHud("Removed from Favorites")
        } else {
            favorites.add(path)
            showHud("Added to Favorites")
        }
        prefs.setFavorites(favorites)
    }

    fun resetFavorites() {
        favorites.clear()
        favorites.addAll(prefs.defaultFavorites(root))
        prefs.setFavorites(favorites)
        showHud("Favorites Reset")
    }

    // ---------- HUD, search, storage ----------

    fun showHud(text: String) {
        hud = text
        hudJob?.cancel()
        hudJob = viewModelScope.launch {
            delay(1800)
            hud = null
        }
    }

    fun search(q: String) {
        searchQuery = q
        searchJob?.cancel()
        if (q.isBlank()) {
            searchResults = null
            searching = false
            return
        }
        val hidden = showHidden
        searchJob = viewModelScope.launch {
            delay(350)
            searching = true
            val r = withContext(Dispatchers.IO) { FileRepo.search(q, hidden) }
            searchResults = r
            searching = false
        }
    }

    fun loadStorage(force: Boolean = false) {
        if (storageLoading || (storage != null && !force)) return
        storageLoading = true
        viewModelScope.launch {
            try {
                storage = withContext(Dispatchers.IO) { FileRepo.storageStats() }
            } finally {
                storageLoading = false
            }
        }
    }
}
