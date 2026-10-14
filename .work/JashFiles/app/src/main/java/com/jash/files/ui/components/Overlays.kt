package com.jash.files.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DriveFileMove
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.jash.files.ui.AppViewModel
import com.jash.files.ui.AlertSpec
import com.jash.files.ui.Route
import com.jash.files.ui.SheetSpec
import com.jash.files.ui.theme.IOS
import com.jash.files.ui.theme.T
import com.jash.files.util.Fmt
import kotlinx.coroutines.delay
import java.io.File

/** iOS context-menu style action sheet sliding up from the bottom. */
@Composable
fun BoxScope.ActionSheetHost(spec: SheetSpec?, onDismiss: () -> Unit) {
    val c = IOS.colors
    val holder = remember { arrayOfNulls<SheetSpec>(1) }
    if (spec != null) holder[0] = spec
    val s = holder[0]
    BackHandler(enabled = spec != null) { onDismiss() }

    AnimatedVisibility(spec != null, Modifier.matchParentSize(), enter = fadeIn(), exit = fadeOut()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        )
    }
    AnimatedVisibility(
        spec != null,
        Modifier.align(Alignment.BottomCenter),
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
    ) {
        if (s != null) {
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 8.dp, vertical = 8.dp)) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 560.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(c.sheet)
                        .verticalScroll(rememberScrollState()),
                ) {
                    val item = s.header
                    if (item != null) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            FileIcon(item, 44.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(item.name, style = T.headline, color = c.label, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text(item.subtitle(), style = T.footnote, color = c.label2, maxLines = 1)
                            }
                        }
                        Hairline()
                    } else if (s.title != null || s.message != null) {
                        Column(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            s.title?.let { Text(it, style = T.footnote, fontWeight = FontWeight.SemiBold, color = c.label2, textAlign = TextAlign.Center) }
                            s.message?.let { Text(it, style = T.footnote, color = c.label2, textAlign = TextAlign.Center) }
                        }
                        Hairline()
                    }
                    s.actions.forEachIndexed { i, a ->
                        val color = if (a.destructive) c.red else c.label
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .rowPress(onClick = {
                                    onDismiss()
                                    a.onClick()
                                })
                                .height(50.dp)
                                .padding(horizontal = 18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(a.label, style = T.body, color = color, modifier = Modifier.weight(1f), maxLines = 1)
                            if (a.icon != null) Icon(a.icon, null, tint = color, modifier = Modifier.size(22.dp))
                        }
                        if (i < s.actions.lastIndex) Hairline()
                    }
                }
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(c.sheet)
                        .rowPress(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) { Text("Cancel", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = c.blue) }
            }
        }
    }
}

/** UIAlertController-style alert, optionally with a text field. */
@Composable
fun AlertHost(spec: AlertSpec?, onDismiss: () -> Unit) {
    if (spec == null) return
    val c = IOS.colors
    val initial = spec.input ?: ""
    val end = if (spec.selectBase && initial.lastIndexOf('.') > 0) initial.lastIndexOf('.') else initial.length
    var text by remember(spec) { mutableStateOf(TextFieldValue(initial, selection = TextRange(0, end))) }
    val focus = remember { FocusRequester() }

    Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.width(270.dp).clip(RoundedCornerShape(14.dp)).background(c.sheet)) {
            Column(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 19.dp, bottom = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(spec.title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = c.label, textAlign = TextAlign.Center)
                if (spec.message != null) {
                    Spacer(Modifier.height(3.dp))
                    Text(spec.message, fontSize = 13.sp, color = c.label, textAlign = TextAlign.Center)
                }
                if (spec.input != null) {
                    Spacer(Modifier.height(14.dp))
                    BasicTextField(
                        value = text,
                        onValueChange = { text = it },
                        singleLine = true,
                        textStyle = T.subhead.copy(color = c.label),
                        cursorBrush = SolidColor(c.blue),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focus)
                            .clip(RoundedCornerShape(8.dp))
                            .background(c.plainBg)
                            .border(0.5.dp, c.separator, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                    )
                    LaunchedEffect(spec) {
                        delay(150)
                        runCatching { focus.requestFocus() }
                    }
                }
            }
            Hairline()
            Row(Modifier.fillMaxWidth().height(44.dp)) {
                if (spec.cancel != null) {
                    AlertButton(spec.cancel, Modifier.weight(1f), bold = spec.destructive, color = c.blue, onClick = onDismiss)
                    Box(Modifier.width(0.5.dp).fillMaxHeight().background(c.separator))
                }
                AlertButton(
                    spec.confirm,
                    Modifier.weight(1f),
                    bold = !spec.destructive,
                    color = if (spec.destructive) c.red else c.blue,
                ) {
                    onDismiss()
                    spec.onConfirm(text.text)
                }
            }
        }
    }
}

@Composable
private fun AlertButton(text: String, modifier: Modifier, bold: Boolean, color: Color, onClick: () -> Unit) {
    Box(modifier.fillMaxHeight().rowPress(onClick = onClick), contentAlignment = Alignment.Center) {
        Text(text, fontSize = 17.sp, fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal, color = color)
    }
}

@Composable
fun BoxScope.HudHost(text: String?) {
    val c = IOS.colors
    val holder = remember { arrayOfNulls<String>(1) }
    if (text != null) holder[0] = text
    AnimatedVisibility(
        text != null,
        Modifier.align(Alignment.TopCenter),
        enter = slideInVertically { -it * 2 } + fadeIn(),
        exit = slideOutVertically { -it * 2 } + fadeOut(),
    ) {
        Row(
            Modifier
                .statusBarsPadding()
                .padding(top = 6.dp)
                .shadow(18.dp, CircleShape)
                .clip(CircleShape)
                .background(c.sheet)
                .padding(horizontal = 18.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.CheckCircle, null, tint = c.green, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(holder[0] ?: "", style = T.subhead, fontWeight = FontWeight.SemiBold, color = c.label)
        }
    }
}

@Composable
fun BusyHost(text: String?) {
    if (text == null) return
    val c = IOS.colors
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.15f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.size(140.dp).clip(RoundedCornerShape(16.dp)).background(c.sheet),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator(Modifier.size(34.dp), color = c.label2, strokeWidth = 3.dp)
            Spacer(Modifier.height(14.dp))
            Text(text, style = T.subhead, color = c.label)
        }
    }
}

/** Floating bar shown while items are on the clipboard. */
@Composable
fun PasteBar(vm: AppViewModel, modifier: Modifier = Modifier) {
    val clip = vm.clipboard ?: return
    val c = IOS.colors
    val dest = (vm.stack.lastOrNull() as? Route.Folder)?.path
    Row(
        modifier
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .fillMaxWidth()
            .shadow(14.dp, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(c.sheet)
            .height(62.dp)
            .padding(start = 16.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(if (clip.move) Icons.Rounded.DriveFileMove else Icons.Rounded.ContentPaste, null, tint = c.blue, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                (if (clip.move) "Move " else "Paste ") + Fmt.plural(clip.paths.size, "item"),
                style = T.headline, color = c.label, maxLines = 1,
            )
            Text(
                if (dest == null || vm.tab != com.jash.files.ui.Tab.Browse) "Open a folder to drop here" else "Into “${vm.folderTitle(dest)}”",
                style = T.footnote, color = c.label2, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        NavTextButton("Cancel", color = c.label2) { vm.cancelClip() }
        if (dest != null && vm.tab == com.jash.files.ui.Tab.Browse) {
            PillButton(if (clip.move) "Move Here" else "Paste") { vm.paste(File(dest)) }
        }
    }
}

@Composable
fun SelectionToolbar(vm: AppViewModel) {
    val ctx = LocalContext.current
    val c = IOS.colors
    val enabled = vm.selected.isNotEmpty()
    BottomBar {
        ToolButton(Icons.Rounded.IosShare, "Share", enabled, c.blue, Modifier.weight(1f)) { vm.share(ctx, vm.selectedFiles()) }
        ToolButton(Icons.Rounded.ContentCopy, "Copy", enabled, c.blue, Modifier.weight(1f)) { vm.setClip(vm.selected.toList(), false) }
        ToolButton(Icons.Rounded.DriveFileMove, "Move", enabled, c.blue, Modifier.weight(1f)) { vm.setClip(vm.selected.toList(), true) }
        ToolButton(Icons.Rounded.FolderZip, "Compress", enabled, c.blue, Modifier.weight(1f)) { vm.compress(vm.selectedFiles()) }
        ToolButton(Icons.Rounded.Delete, "Delete", enabled, c.red, Modifier.weight(1f)) { vm.askDelete(vm.selectedFiles()) }
    }
}

@Composable
private fun ToolButton(icon: ImageVector, label: String, enabled: Boolean, tint: Color, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.fillMaxHeight().tap(enabled, onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(24.dp))
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = tint)
    }
}
