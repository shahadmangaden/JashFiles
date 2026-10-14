package com.jash.files.data

import java.io.File

enum class FileKind { Folder, Image, Video, Audio, Pdf, Doc, Sheet, Slides, Text, Archive, Apk, Code, Other }

enum class SortMode(val title: String) { Name("Name"), Date("Date"), Size("Size"), Kind("Kind") }

enum class FileCategory(val title: String, val exts: Set<String>) {
    Images("Images", setOf("jpg", "jpeg", "png", "gif", "webp", "heic", "heif", "bmp")),
    Videos("Videos", setOf("mp4", "mkv", "webm", "avi", "mov", "3gp", "m4v")),
    Audio("Audio", setOf("mp3", "m4a", "aac", "wav", "flac", "ogg", "opus", "amr")),
    Documents(
        "Documents",
        setOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "rtf", "odt", "ods", "odp", "csv", "md", "epub")
    ),
    Archives("Archives", setOf("zip", "rar", "7z", "tar", "gz", "tgz", "bz2", "xz")),
    Apps("APKs", setOf("apk", "apks", "xapk"));

    companion object {
        fun of(ext: String): FileCategory? = entries.firstOrNull { ext in it.exts }
    }
}

private val docExts = setOf("doc", "docx", "odt", "rtf", "pages")
private val sheetExts = setOf("xls", "xlsx", "ods", "csv", "numbers")
private val slideExts = setOf("ppt", "pptx", "odp", "key")
private val textExts = setOf("txt", "md", "log", "ini", "cfg", "conf")
private val codeExts = setOf(
    "html", "htm", "css", "js", "json", "xml", "kt", "java", "py", "c", "cpp", "h", "sh", "ts", "yml", "yaml"
)

fun kindOf(ext: String, isDir: Boolean): FileKind = when {
    isDir -> FileKind.Folder
    ext in FileCategory.Images.exts -> FileKind.Image
    ext in FileCategory.Videos.exts -> FileKind.Video
    ext in FileCategory.Audio.exts -> FileKind.Audio
    ext == "pdf" -> FileKind.Pdf
    ext in docExts -> FileKind.Doc
    ext in sheetExts -> FileKind.Sheet
    ext in slideExts -> FileKind.Slides
    ext in textExts -> FileKind.Text
    ext in FileCategory.Archives.exts -> FileKind.Archive
    ext in FileCategory.Apps.exts -> FileKind.Apk
    ext in codeExts -> FileKind.Code
    else -> FileKind.Other
}

data class FileItem(
    val file: File,
    val isDir: Boolean,
    val size: Long,
    val modified: Long,
    val childCount: Int,
) {
    val name: String get() = file.name
    val path: String get() = file.absolutePath
    val ext: String get() = if (isDir) "" else file.extension.lowercase()
    val kind: FileKind get() = kindOf(ext, isDir)
}

fun kindName(item: FileItem): String {
    val e = item.ext.uppercase()
    return when (item.kind) {
        FileKind.Folder -> "Folder"
        FileKind.Image -> "$e Image"
        FileKind.Video -> "$e Video"
        FileKind.Audio -> "$e Audio"
        FileKind.Pdf -> "PDF Document"
        FileKind.Doc -> "$e Document"
        FileKind.Sheet -> "$e Spreadsheet"
        FileKind.Slides -> "$e Presentation"
        FileKind.Text -> "Plain Text"
        FileKind.Archive -> "$e Archive"
        FileKind.Apk -> "Android App"
        FileKind.Code -> "$e Source"
        FileKind.Other -> if (e.isEmpty()) "Document" else "$e File"
    }
}

data class StorageStats(
    val total: Long,
    val free: Long,
    val byCategory: Map<FileCategory, Long>,
    val large: List<FileItem>,
) {
    val used: Long get() = (total - free).coerceAtLeast(0)
    val categorized: Long get() = byCategory.values.sum()
    val other: Long get() = (used - categorized).coerceAtLeast(0)
}
