package com.jash.files.data

import android.content.Context
import android.os.Environment
import android.os.StatFs
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** All blocking file-system work. Call from Dispatchers.IO. */
object FileRepo {

    val root: File get() = Environment.getExternalStorageDirectory()

    fun removableRoots(ctx: Context): List<File> =
        ctx.getExternalFilesDirs(null).drop(1).filterNotNull().mapNotNull { d ->
            val p = d.absolutePath
            val i = p.indexOf("/Android/data")
            if (i > 0) File(p.substring(0, i)) else null
        }

    fun toItem(f: File, countChildren: Boolean = true): FileItem {
        val dir = f.isDirectory
        return FileItem(
            file = f,
            isDir = dir,
            size = if (dir) 0L else f.length(),
            modified = f.lastModified(),
            childCount = if (dir && countChildren) (f.list()?.size ?: 0) else 0,
        )
    }

    fun list(dir: File, showHidden: Boolean, sort: SortMode, asc: Boolean): List<FileItem> {
        val files = dir.listFiles()?.filter { showHidden || !it.name.startsWith(".") } ?: emptyList()
        return sortItems(files.map { toItem(it) }, sort, asc)
    }

    fun sortItems(items: List<FileItem>, sort: SortMode, asc: Boolean): List<FileItem> {
        val byName: Comparator<FileItem> = compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }
        val cmp: Comparator<FileItem> = when (sort) {
            SortMode.Name -> byName
            SortMode.Date -> compareBy<FileItem> { it.modified }.then(byName)
            SortMode.Size -> compareBy<FileItem> { if (it.isDir) it.childCount.toLong() else it.size }.then(byName)
            SortMode.Kind -> compareBy<FileItem> { it.ext }.then(byName)
        }
        val directed = if (asc) cmp else cmp.reversed()
        return items.sortedWith(compareByDescending<FileItem> { it.isDir }.then(directed))
    }

    private fun walk(showHidden: Boolean, includeDirs: Boolean, limit: Int, accept: (File) -> Boolean): List<File> {
        val out = ArrayList<File>()
        val rootPath = root.absolutePath
        val skip = "$rootPath/Android"
        val seq = root.walkTopDown().onEnter { d ->
            d.absolutePath == rootPath ||
                ((showHidden || !d.name.startsWith(".")) && d.absolutePath != skip)
        }
        for (f in seq) {
            if (f.absolutePath == rootPath) continue
            if (!showHidden && f.name.startsWith(".")) continue
            if ((includeDirs || f.isFile) && accept(f)) {
                out.add(f)
                if (out.size >= limit) break
            }
        }
        return out
    }

    fun category(cat: FileCategory, showHidden: Boolean): List<FileItem> =
        walk(showHidden, includeDirs = false, limit = 5000) { it.extension.lowercase() in cat.exts }
            .map { toItem(it, false) }
            .sortedByDescending { it.modified }

    fun recents(showHidden: Boolean, days: Int = 30, limit: Int = 120): List<FileItem> {
        val since = System.currentTimeMillis() - days * 24L * 3600_000L
        return walk(showHidden, includeDirs = false, limit = 20_000) { it.lastModified() >= since }
            .sortedByDescending { it.lastModified() }
            .take(limit)
            .map { toItem(it, false) }
    }

    fun search(query: String, showHidden: Boolean, limit: Int = 300): List<FileItem> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        return walk(showHidden, includeDirs = true, limit = limit) { it.name.contains(q, ignoreCase = true) }
            .map { toItem(it) }
            .sortedWith(compareByDescending<FileItem> { it.isDir }.thenBy { it.name.lowercase() })
    }

    fun space(f: File): Pair<Long, Long> =
        runCatching { StatFs(f.path).let { it.totalBytes to it.availableBytes } }.getOrDefault(0L to 0L)

    fun freeSpace(f: File): Long = space(f).second

    fun storageStats(): StorageStats {
        val (total, free) = space(root)
        val sizes = HashMap<FileCategory, Long>()
        val large = ArrayList<File>()
        walk(showHidden = true, includeDirs = false, limit = Int.MAX_VALUE) { f ->
            val len = f.length()
            FileCategory.of(f.extension.lowercase())?.let { sizes[it] = (sizes[it] ?: 0L) + len }
            if (len >= 10_000_000L) large.add(f)
            false
        }
        val top = large.sortedByDescending { it.length() }.take(20).map { toItem(it, false) }
        return StorageStats(total, free, sizes, top)
    }

    fun folderSize(dir: File): Long = dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }

    fun created(f: File): Long? = runCatching {
        Files.readAttributes(f.toPath(), BasicFileAttributes::class.java).creationTime().toMillis()
    }.getOrNull()

    // ---------- Operations ----------

    /** iOS-style unique names: "Photo.jpg" -> "Photo 2.jpg". */
    fun uniqueTarget(dir: File, name: String, isDir: Boolean): File {
        val first = File(dir, name)
        if (!first.exists()) return first
        val dot = name.lastIndexOf('.')
        val base = if (!isDir && dot > 0) name.substring(0, dot) else name
        val ext = if (!isDir && dot > 0) name.substring(dot) else ""
        var i = 2
        while (true) {
            val t = File(dir, "$base $i$ext")
            if (!t.exists()) return t
            i++
        }
    }

    fun createFolder(dir: File, name: String): File {
        val t = uniqueTarget(dir, name, true)
        if (!t.mkdirs()) throw IOException("Couldn't create the folder “$name”.")
        return t
    }

    fun rename(f: File, newName: String): File {
        val parent = f.parentFile ?: throw IOException("Can't rename this item.")
        val t = File(parent, newName)
        if (t.exists()) throw IOException("The name “$newName” is already taken. Please choose a different name.")
        if (!f.renameTo(t)) throw IOException("Couldn't rename “${f.name}”.")
        return t
    }

    fun delete(files: List<File>) {
        for (f in files) if (f.exists() && !f.deleteRecursively()) throw IOException("Couldn't delete “${f.name}”.")
    }

    private fun checkNotInside(f: File, dest: File) {
        val src = f.canonicalPath
        val d = dest.canonicalPath
        if (d == src || d.startsWith("$src/")) throw IOException("Can't put “${f.name}” inside itself.")
    }

    fun copy(files: List<File>, dest: File) {
        for (f in files) {
            checkNotInside(f, dest)
            val t = uniqueTarget(dest, f.name, f.isDirectory)
            if (!f.copyRecursively(t, overwrite = false)) throw IOException("Couldn't copy “${f.name}”.")
        }
    }

    fun move(files: List<File>, dest: File) {
        for (f in files) {
            if (f.parentFile?.canonicalPath == dest.canonicalPath) continue
            checkNotInside(f, dest)
            val t = uniqueTarget(dest, f.name, f.isDirectory)
            if (!f.renameTo(t)) {
                if (!f.copyRecursively(t, overwrite = false)) throw IOException("Couldn't move “${f.name}”.")
                f.deleteRecursively()
            }
        }
    }

    fun compress(files: List<File>, dir: File): File {
        val name = if (files.size == 1) {
            val f = files[0]
            (if (f.isDirectory) f.name else f.name.substringBeforeLast('.')) + ".zip"
        } else "Archive.zip"
        val out = uniqueTarget(dir, name, false)
        ZipOutputStream(BufferedOutputStream(FileOutputStream(out))).use { zos ->
            for (f in files) {
                val base = f.parentFile ?: continue
                f.walkTopDown().forEach { x ->
                    if (x.absolutePath == out.absolutePath) return@forEach
                    val rel = x.relativeTo(base).invariantSeparatorsPath
                    if (x.isDirectory) {
                        zos.putNextEntry(ZipEntry("$rel/"))
                        zos.closeEntry()
                    } else {
                        zos.putNextEntry(ZipEntry(rel))
                        x.inputStream().use { it.copyTo(zos) }
                        zos.closeEntry()
                    }
                }
            }
        }
        return out
    }

    fun extract(zip: File, dir: File): File {
        val target = uniqueTarget(dir, zip.nameWithoutExtension, true)
        target.mkdirs()
        val targetPath = target.canonicalPath + File.separator
        ZipInputStream(zip.inputStream().buffered()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val out = File(target, entry.name)
                if (!out.canonicalPath.startsWith(targetPath)) throw IOException("This archive contains unsafe paths.")
                if (entry.isDirectory) {
                    out.mkdirs()
                } else {
                    out.parentFile?.mkdirs()
                    FileOutputStream(out).use { zis.copyTo(it) }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        return target
    }
}
