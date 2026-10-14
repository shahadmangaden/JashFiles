package com.jash.files.data

import android.content.Context
import java.io.File

class Prefs(ctx: Context) {
    private val p = ctx.getSharedPreferences("jash_files", Context.MODE_PRIVATE)

    var sort: SortMode
        get() = runCatching { SortMode.valueOf(p.getString("sort", "Name")!!) }.getOrDefault(SortMode.Name)
        set(v) = p.edit().putString("sort", v.name).apply()

    var sortAsc: Boolean
        get() = p.getBoolean("sortAsc", true)
        set(v) = p.edit().putBoolean("sortAsc", v).apply()

    var grid: Boolean
        get() = p.getBoolean("grid", false)
        set(v) = p.edit().putBoolean("grid", v).apply()

    var showHidden: Boolean
        get() = p.getBoolean("hidden", false)
        set(v) = p.edit().putBoolean("hidden", v).apply()

    /** 0 = automatic, 1 = light, 2 = dark */
    var theme: Int
        get() = p.getInt("theme", 0)
        set(v) = p.edit().putInt("theme", v).apply()

    var confirmDelete: Boolean
        get() = p.getBoolean("confirmDelete", true)
        set(v) = p.edit().putBoolean("confirmDelete", v).apply()

    fun favorites(root: File): List<String> {
        val s = p.getString("favs", null) ?: return defaultFavorites(root)
        return s.split('\n').filter { it.isNotBlank() }
    }

    fun defaultFavorites(root: File): List<String> =
        listOf("Download", "Documents", "DCIM", "Pictures").map { File(root, it).absolutePath }

    fun setFavorites(list: List<String>) = p.edit().putString("favs", list.joinToString("\n")).apply()
}
