package com.jash.files.util

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.LruCache
import android.util.Size
import android.webkit.MimeTypeMap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.jash.files.data.FileKind
import java.io.File
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object Fmt {
    fun size(bytes: Long): String {
        if (bytes < 1000) return if (bytes == 1L) "1 byte" else "$bytes bytes"
        val units = arrayOf("KB", "MB", "GB", "TB")
        var v = bytes.toDouble()
        var i = -1
        while (v >= 1000 && i < units.size - 1) {
            v /= 1000
            i++
        }
        return if (v >= 100) String.format(Locale.US, "%.0f %s", v, units[i])
        else String.format(Locale.US, "%.1f %s", v, units[i])
    }

    private fun sameDay(a: Calendar, b: Calendar) =
        a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)

    private fun dayLabel(ms: Long): String? {
        val now = Calendar.getInstance()
        val c = Calendar.getInstance().apply { timeInMillis = ms }
        if (sameDay(now, c)) return "Today"
        now.add(Calendar.DAY_OF_YEAR, -1)
        if (sameDay(now, c)) return "Yesterday"
        return null
    }

    fun date(ms: Long): String {
        val time = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(ms))
        return dayLabel(ms)?.let { "$it at $time" } ?: DateFormat.getDateInstance(DateFormat.SHORT).format(Date(ms))
    }

    fun shortDate(ms: Long): String = dayLabel(ms) ?: DateFormat.getDateInstance(DateFormat.SHORT).format(Date(ms))

    fun longDate(ms: Long): String =
        DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT).format(Date(ms))

    fun plural(n: Int, word: String) = if (n == 1) "1 $word" else "$n ${word}s"
}

object Perms {
    fun has(ctx: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Environment.isExternalStorageManager()
        else ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_EXTERNAL_STORAGE) ==
            PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
            PackageManager.PERMISSION_GRANTED

    fun openAllFilesSettings(ctx: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        try {
            ctx.startActivity(
                Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:${ctx.packageName}"))
            )
        } catch (e: ActivityNotFoundException) {
            ctx.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
        }
    }

    fun openAppSettings(ctx: Context) {
        ctx.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

object Intents {
    fun mime(f: File): String =
        MimeTypeMap.getSingleton().getMimeTypeFromExtension(f.extension.lowercase()) ?: "*/*"

    private fun uri(ctx: Context, f: File): Uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.provider", f)

    fun open(ctx: Context, f: File, chooser: Boolean): Boolean = try {
        val i = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri(ctx, f), mime(f))
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(if (chooser) Intent.createChooser(i, "Open With").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) else i)
        true
    } catch (e: Exception) {
        false
    }

    fun share(ctx: Context, files: List<File>): Boolean = try {
        val uris = ArrayList(files.map { uri(ctx, it) })
        val i = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, uris[0]).setType(mime(files[0]))
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris).setType("*/*")
        }
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        ctx.startActivity(Intent.createChooser(i, "Share").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (e: Exception) {
        false
    }
}

object Thumbs {
    private val cache = object : LruCache<String, Bitmap>(32 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    private fun key(path: String, px: Int) = "$path@$px"

    @Suppress("DEPRECATION")
    private fun videoThumb(f: File, px: Int): Bitmap? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ThumbnailUtils.createVideoThumbnail(f, Size(px, px), null)
        else ThumbnailUtils.createVideoThumbnail(f.path, MediaStore.Images.Thumbnails.MINI_KIND)

    fun peek(path: String, px: Int): ImageBitmap? = cache.get(key(path, px))?.asImageBitmap()

    fun load(f: File, kind: FileKind, px: Int): ImageBitmap? {
        val k = key(f.absolutePath, px)
        cache.get(k)?.let { return it.asImageBitmap() }
        val bmp: Bitmap? = try {
            when (kind) {
                FileKind.Image -> {
                    val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(f.path, o)
                    var s = 1
                    while (o.outWidth / (s * 2) >= px && o.outHeight / (s * 2) >= px) s *= 2
                    BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inSampleSize = s })
                }
                FileKind.Video -> videoThumb(f, px)
                else -> null
            }
        } catch (e: Throwable) {
            null
        }
        bmp?.let { cache.put(k, it) }
        return bmp?.asImageBitmap()
    }
}
