package com.jash.files.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Article
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Photo
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Slideshow
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jash.files.data.FileCategory
import com.jash.files.data.FileItem
import com.jash.files.data.FileKind
import com.jash.files.ui.AppViewModel
import com.jash.files.ui.theme.IOS
import com.jash.files.ui.theme.T
import com.jash.files.util.Fmt
import com.jash.files.util.Thumbs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun kindColor(k: FileKind): Color {
    val c = IOS.colors
    return when (k) {
        FileKind.Folder -> c.folder
        FileKind.Image -> c.purple
        FileKind.Video -> c.pink
        FileKind.Audio -> c.red
        FileKind.Pdf -> c.red
        FileKind.Doc -> c.blue
        FileKind.Sheet -> c.green
        FileKind.Slides -> c.orange
        FileKind.Text -> c.gray
        FileKind.Archive -> c.indigo
        FileKind.Apk -> Color(0xFF3DDC84)
        FileKind.Code -> c.teal
        FileKind.Other -> c.gray
    }
}

fun kindIcon(k: FileKind): ImageVector = when (k) {
    FileKind.Folder -> Icons.Rounded.Folder
    FileKind.Image -> Icons.Rounded.Image
    FileKind.Video -> Icons.Rounded.Movie
    FileKind.Audio -> Icons.Rounded.MusicNote
    FileKind.Pdf -> Icons.Rounded.PictureAsPdf
    FileKind.Doc -> Icons.Rounded.Description
    FileKind.Sheet -> Icons.Rounded.TableChart
    FileKind.Slides -> Icons.Rounded.Slideshow
    FileKind.Text -> Icons.Rounded.Article
    FileKind.Archive -> Icons.Rounded.FolderZip
    FileKind.Apk -> Icons.Rounded.Android
    FileKind.Code -> Icons.Rounded.Code
    FileKind.Other -> Icons.Rounded.InsertDriveFile
}

@Composable
fun categoryColor(cat: FileCategory): Color {
    val c = IOS.colors
    return when (cat) {
        FileCategory.Images -> c.purple
        FileCategory.Videos -> c.pink
        FileCategory.Audio -> c.red
        FileCategory.Documents -> c.blue
        FileCategory.Archives -> c.indigo
        FileCategory.Apps -> c.green
    }
}

fun categoryIcon(cat: FileCategory): ImageVector = when (cat) {
    FileCategory.Images -> Icons.Rounded.Photo
    FileCategory.Videos -> Icons.Rounded.Movie
    FileCategory.Audio -> Icons.Rounded.MusicNote
    FileCategory.Documents -> Icons.Rounded.Description
    FileCategory.Archives -> Icons.Rounded.FolderZip
    FileCategory.Apps -> Icons.Rounded.Android
}

fun FileItem.subtitle(): String =
    Fmt.date(modified) + "  ·  " + if (isDir) Fmt.plural(childCount, "item") else Fmt.size(size)

@Composable
fun DocIcon(item: FileItem, size: Dp) {
    val c = IOS.colors
    val color = kindColor(item.kind)
    val shape = RoundedCornerShape(size * 0.1f)
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(width = size * 0.76f, height = size * 0.94f)
                .shadow(1.dp, shape)
                .clip(shape)
                .background(c.docBg)
                .border(0.5.dp, c.separator, shape),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(kindIcon(item.kind), null, tint = color, modifier = Modifier.size(size * 0.4f))
                if (size >= 56.dp && item.ext.isNotEmpty()) {
                    Text(
                        item.ext.uppercase().take(4),
                        fontSize = (size.value * 0.13f).sp,
                        fontWeight = FontWeight.Bold,
                        color = color,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
fun FileIcon(item: FileItem, size: Dp) {
    val c = IOS.colors
    when {
        item.isDir -> Icon(Icons.Rounded.Folder, null, tint = c.folder, modifier = Modifier.size(size))
        item.kind == FileKind.Image || item.kind == FileKind.Video -> {
            val px = with(LocalDensity.current) { size.roundToPx() }
            val thumb by produceState<ImageBitmap?>(Thumbs.peek(item.path, px), item.path, item.modified) {
                if (value == null) value = withContext(Dispatchers.IO) { Thumbs.load(item.file, item.kind, px) }
            }
            Box(Modifier.size(size), contentAlignment = Alignment.Center) {
                val t = thumb
                if (t != null) {
                    val shape = RoundedCornerShape(size * 0.1f)
                    Image(
                        t, null, contentScale = ContentScale.Crop,
                        modifier = Modifier.size(size * 0.94f).clip(shape).border(0.5.dp, c.separator, shape),
                    )
                    if (item.kind == FileKind.Video) {
                        Icon(Icons.Rounded.PlayCircle, null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(size * 0.36f))
                    }
                } else {
                    DocIcon(item, size)
                }
            }
        }
        else -> DocIcon(item, size)
    }
}

@Composable
fun FileRow(
    vm: AppViewModel,
    item: FileItem,
    divider: Boolean = true,
    value: String? = null,
    modifier: Modifier = Modifier,
) {
    val ctx = LocalContext.current
    val c = IOS.colors
    val selected = vm.selecting && item.path in vm.selected
    Column(
        modifier
            .fillMaxWidth()
            .background(if (selected) c.blue.copy(alpha = 0.12f) else Color.Transparent)
            .rowPress(
                onClick = { vm.tapItem(ctx, item) },
                onLongClick = { if (vm.selecting) vm.toggleSelect(item.path) else vm.showItemMenu(ctx, item) },
            ),
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 62.dp).padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (vm.selecting) {
                SelectCircle(selected)
                Spacer(Modifier.width(12.dp))
            }
            FileIcon(item, 42.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f).padding(vertical = 9.dp)) {
                Text(item.name, style = T.body, color = c.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(value ?: item.subtitle(), style = T.footnote, color = c.label2, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (!vm.selecting) {
                Box(Modifier.size(42.dp).tap { vm.showItemMenu(ctx, item) }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.MoreHoriz, null, tint = c.label2, modifier = Modifier.size(22.dp))
                }
            } else {
                Spacer(Modifier.width(12.dp))
            }
        }
        if (divider) Hairline(Modifier.padding(start = if (vm.selecting) 105.dp else 70.dp))
    }
}

@Composable
fun FileCell(vm: AppViewModel, item: FileItem, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val c = IOS.colors
    val selected = vm.selecting && item.path in vm.selected
    Column(
        modifier
            .padding(vertical = 8.dp)
            .pressScale(
                onClick = { vm.tapItem(ctx, item) },
                onLongClick = { if (vm.selecting) vm.toggleSelect(item.path) else vm.showItemMenu(ctx, item) },
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selected) c.fill else Color.Transparent)
                    .padding(4.dp),
            ) { FileIcon(item, 80.dp) }
            if (vm.selecting) Box(Modifier.padding(2.dp)) { SelectCircle(selected) }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            item.name, fontSize = 13.sp, color = c.label, maxLines = 2, textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis, lineHeight = 16.sp, modifier = Modifier.padding(horizontal = 6.dp),
        )
        Text(Fmt.shortDate(item.modified), fontSize = 12.sp, color = c.label2, maxLines = 1)
        Text(
            if (item.isDir) Fmt.plural(item.childCount, "item") else Fmt.size(item.size),
            fontSize = 12.sp, color = c.label2, maxLines = 1,
        )
    }
}

/** Plain list (iOS Files list view). With [inset] rows sit in a rounded grouped card. */
fun LazyListScope.fileList(vm: AppViewModel, list: List<FileItem>, inset: Boolean = false, value: ((FileItem) -> String)? = null) {
    itemsIndexed(list, key = { _, it -> it.path }) { i, it ->
        val c = IOS.colors
        val mod = if (inset) {
            val shape: Shape = when {
                list.size == 1 -> RoundedCornerShape(12.dp)
                i == 0 -> RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                i == list.lastIndex -> RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
                else -> RectangleShape
            }
            Modifier.padding(horizontal = 16.dp).clip(shape).background(c.card)
        } else Modifier
        FileRow(vm, it, divider = i < list.lastIndex, value = value?.invoke(it), modifier = mod)
    }
}

/** Icon grid (iOS Files icon view), rendered as rows inside a LazyColumn. */
fun LazyListScope.fileGrid(vm: AppViewModel, list: List<FileItem>, columns: Int = 3) {
    val rows = list.chunked(columns)
    items(rows, key = { "g:" + it.first().path }) { row ->
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
            row.forEach { FileCell(vm, it, Modifier.weight(1f)) }
            repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}
