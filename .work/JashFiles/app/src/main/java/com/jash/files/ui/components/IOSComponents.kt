package com.jash.files.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jash.files.ui.theme.IOS
import com.jash.files.ui.theme.T

// ---------- Touch feedback (no Android ripples — iOS style) ----------

/** Fades to 40% while pressed, like iOS bar buttons. */
@Composable
fun Modifier.tap(enabled: Boolean = true, onClick: () -> Unit): Modifier {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    return this
        .graphicsLayer { alpha = if (!enabled) 0.35f else if (pressed) 0.4f else 1f }
        .clickable(interactionSource = src, indication = null, enabled = enabled, onClick = onClick)
}

/** Grey highlight while pressed, like a UITableView cell. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.rowPress(onClick: (() -> Unit)?, onLongClick: (() -> Unit)? = null): Modifier {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val c = IOS.colors
    if (onClick == null && onLongClick == null) return this
    return this
        .background(if (pressed) c.pressed else Color.Transparent)
        .combinedClickable(
            interactionSource = src,
            indication = null,
            onLongClick = onLongClick,
            onClick = onClick ?: {},
        )
}

/** Slight shrink while pressed, used by grid icons. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.pressScale(onClick: () -> Unit, onLongClick: (() -> Unit)? = null): Modifier {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    return this
        .graphicsLayer {
            val s = if (pressed) 0.95f else 1f
            scaleX = s; scaleY = s; alpha = if (pressed) 0.75f else 1f
        }
        .combinedClickable(interactionSource = src, indication = null, onLongClick = onLongClick, onClick = onClick)
}

// ---------- Basic pieces ----------

@Composable
fun Hairline(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(0.5.dp).background(IOS.colors.separator))
}

@Composable
fun NavTextButton(text: String, bold: Boolean = false, color: Color? = null, enabled: Boolean = true, onClick: () -> Unit) {
    Text(
        text,
        style = if (bold) T.headline else T.body,
        color = color ?: IOS.colors.blue,
        maxLines = 1,
        modifier = Modifier.tap(enabled, onClick).padding(horizontal = 8.dp, vertical = 10.dp),
    )
}

@Composable
fun NavIconButton(icon: ImageVector, circled: Boolean = false, onClick: () -> Unit) {
    val c = IOS.colors
    Box(Modifier.size(44.dp).tap(onClick = onClick), contentAlignment = Alignment.Center) {
        if (circled) {
            Box(
                Modifier.size(25.dp).border(1.6.dp, c.blue, CircleShape),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, tint = c.blue, modifier = Modifier.size(17.dp)) }
        } else {
            Icon(icon, null, tint = c.blue, modifier = Modifier.size(25.dp))
        }
    }
}

@Composable
fun PrimaryButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = IOS.colors
    Box(
        modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(14.dp)).background(c.blue).tap(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = T.headline, color = Color.White) }
}

@Composable
fun PillButton(text: String, onClick: () -> Unit) {
    val c = IOS.colors
    Box(
        Modifier.clip(CircleShape).background(c.blue).tap(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = T.subhead, fontWeight = FontWeight.SemiBold, color = Color.White) }
}

@Composable
fun AppIconBadge(size: Dp) {
    Box(
        Modifier
            .size(size)
            .shadow(6.dp, RoundedCornerShape(size * 0.225f))
            .clip(RoundedCornerShape(size * 0.225f))
            .background(Brush.verticalGradient(listOf(Color(0xFF5AC8FA), Color(0xFF007AFF)))),
        contentAlignment = Alignment.Center,
    ) { Icon(Icons.Rounded.Folder, null, tint = Color.White, modifier = Modifier.size(size * 0.6f)) }
}

@Composable
fun SelectCircle(selected: Boolean) {
    val c = IOS.colors
    if (selected) {
        Box(Modifier.size(23.dp).clip(CircleShape).background(c.blue), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
    } else {
        Box(Modifier.size(23.dp).border(1.5.dp, c.label3, CircleShape).background(Color.Black.copy(alpha = 0.04f), CircleShape))
    }
}

// ---------- Navigation bar & screen ----------

@Composable
private fun IOSNavTitle(title: String, visible: Boolean, color: Color, modifier: Modifier) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Text(
            title, style = T.headline, color = color, maxLines = 1,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 190.dp),
        )
    }
}

@Composable
fun IOSNavBar(
    title: String,
    showTitle: Boolean,
    backLabel: String?,
    onBack: () -> Unit,
    leading: (@Composable () -> Unit)?,
    actions: @Composable RowScope.() -> Unit,
    pageBg: Color,
) {
    val c = IOS.colors
    Column(Modifier.fillMaxWidth().background(if (showTitle) c.barBg else pageBg).statusBarsPadding()) {
        Box(Modifier.fillMaxWidth().height(44.dp).padding(horizontal = 4.dp)) {
            Row(Modifier.align(Alignment.CenterStart), verticalAlignment = Alignment.CenterVertically) {
                when {
                    leading != null -> leading()
                    backLabel != null -> Row(
                        Modifier.tap(onClick = onBack).padding(end = 8.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Rounded.ArrowBackIosNew, null, tint = c.blue,
                            modifier = Modifier.padding(start = 4.dp, end = 2.dp).size(21.dp),
                        )
                        Text(
                            backLabel, style = T.body, color = c.blue, maxLines = 1,
                            overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 110.dp),
                        )
                    }
                }
            }
            IOSNavTitle(title, showTitle, c.label, Modifier.align(Alignment.Center))
            Row(Modifier.align(Alignment.CenterEnd), verticalAlignment = Alignment.CenterVertically, content = actions)
        }
        if (showTitle) Hairline()
    }
}

/** A screen with an iOS large title that collapses into the nav bar on scroll. */
@Composable
fun IOSScreen(
    title: String,
    listState: LazyListState,
    plain: Boolean = false,
    backLabel: String? = null,
    onBack: () -> Unit = {},
    leading: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    header: (@Composable () -> Unit)? = null,
    bottomPadding: Dp = 0.dp,
    content: LazyListScope.() -> Unit,
) {
    val c = IOS.colors
    val pageBg = if (plain) c.plainBg else c.bg
    val collapsed by remember(listState) {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 40 }
    }
    Column(Modifier.fillMaxSize().background(pageBg)) {
        IOSNavBar(title, collapsed, backLabel, onBack, leading, actions, pageBg)
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = bottomPadding + 24.dp),
        ) {
            item(key = "__title") {
                Text(
                    title, style = T.largeTitle, color = c.label, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 6.dp),
                )
            }
            if (header != null) item(key = "__header") { header() }
            content()
        }
    }
}

// ---------- Grouped lists ----------

@Composable
fun IOSGroup(
    header: String? = null,
    footer: String? = null,
    big: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = IOS.colors
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        if (header != null) {
            if (big) Text(header, style = T.title3, fontWeight = FontWeight.Bold, color = c.label, modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 8.dp))
            else Text(header.uppercase(), style = T.footnote, color = c.label2, modifier = Modifier.padding(start = 16.dp, top = 6.dp, bottom = 7.dp))
        }
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.card), content = content)
        if (footer != null) {
            Text(footer, style = T.footnote, color = c.label2, modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 7.dp))
        }
        Spacer(Modifier.height(22.dp))
    }
}

@Composable
fun IOSRow(
    title: String,
    subtitle: String? = null,
    value: String? = null,
    icon: ImageVector? = null,
    iconBg: Color? = null,
    iconTint: Color? = null,
    titleColor: Color? = null,
    chevron: Boolean = false,
    divider: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val c = IOS.colors
    val textStart = if (icon != null) 59.dp else 16.dp
    Column(Modifier.fillMaxWidth().rowPress(onClick, onLongClick)) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 46.dp).padding(start = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                if (iconBg != null) {
                    Box(
                        Modifier.size(29.dp).clip(RoundedCornerShape(7.dp)).background(iconBg),
                        contentAlignment = Alignment.Center,
                    ) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(19.dp)) }
                } else {
                    Box(Modifier.size(29.dp), contentAlignment = Alignment.Center) {
                        Icon(icon, null, tint = iconTint ?: c.blue, modifier = Modifier.size(25.dp))
                    }
                }
                Spacer(Modifier.width(14.dp))
            }
            Column(Modifier.weight(1f).padding(vertical = if (subtitle != null) 8.dp else 11.dp)) {
                Text(title, style = T.body, color = titleColor ?: c.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitle != null) {
                    Text(subtitle, style = T.footnote, color = c.label2, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            if (value != null) {
                Text(
                    value, style = T.body, color = c.label2, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End, modifier = Modifier.padding(start = 8.dp).widthIn(max = 200.dp),
                )
            }
            if (trailing != null) {
                Spacer(Modifier.width(8.dp))
                trailing()
            }
            if (chevron) Icon(Icons.Rounded.ChevronRight, null, tint = c.label3, modifier = Modifier.padding(start = 2.dp).size(24.dp))
            Spacer(Modifier.width(if (chevron) 8.dp else 16.dp))
        }
        if (divider) Hairline(Modifier.padding(start = textStart))
    }
}

@Composable
fun IOSSearchField(value: String, onChange: (String) -> Unit, placeholder: String = "Search") {
    val c = IOS.colors
    Row(
        Modifier
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .fillMaxWidth()
            .height(37.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(c.fill)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Search, null, tint = c.label2, modifier = Modifier.size(21.dp))
        Spacer(Modifier.width(6.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) Text(placeholder, style = T.body, color = c.label2, maxLines = 1)
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = T.body.copy(color = c.label),
                cursorBrush = SolidColor(c.blue),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (value.isNotEmpty()) {
            Icon(
                Icons.Rounded.Cancel, null, tint = c.label3,
                modifier = Modifier.size(20.dp).tap { onChange("") },
            )
        }
    }
}

@Composable
fun SegmentedControl(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val c = IOS.colors
    Row(
        Modifier.fillMaxWidth().height(34.dp).clip(RoundedCornerShape(9.dp)).background(c.fill).padding(2.dp),
    ) {
        options.forEachIndexed { i, s ->
            val sel = i == selected
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .then(
                        if (sel) Modifier.shadow(2.dp, RoundedCornerShape(7.dp)).background(c.segSel, RoundedCornerShape(7.dp))
                        else Modifier
                    )
                    .tap { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) {
                Text(s, fontSize = 13.sp, fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Medium, color = c.label)
            }
        }
    }
}

@Composable
fun IOSSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    val c = IOS.colors
    val offset by animateDpAsState(if (checked) 20.dp else 0.dp, label = "switch")
    val track by animateColorAsState(if (checked) c.green else c.switchOff, label = "track")
    Box(
        Modifier
            .size(51.dp, 31.dp)
            .clip(CircleShape)
            .background(track)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onChange(!checked) }
            .padding(2.dp),
    ) {
        Box(Modifier.offset(x = offset).size(27.dp).shadow(3.dp, CircleShape).background(Color.White, CircleShape))
    }
}

@Composable
fun TabItem(icon: ImageVector, label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val c = IOS.colors
    Column(
        modifier.fillMaxHeight().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = if (selected) c.blue else c.gray, modifier = Modifier.size(27.dp))
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = if (selected) c.blue else c.gray)
    }
}

@Composable
fun BottomBar(content: @Composable RowScope.() -> Unit) {
    val c = IOS.colors
    Column(Modifier.fillMaxWidth().background(c.barBg)) {
        Hairline()
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().height(52.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

// ---------- Lazy list helpers ----------

fun LazyListScope.loadingItem() = item(key = "__loading") {
    Box(Modifier.fillMaxWidth().padding(56.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(Modifier.size(28.dp), color = IOS.colors.label2, strokeWidth = 2.5.dp)
    }
}

fun LazyListScope.emptyItem(title: String, message: String? = null) = item(key = "__empty") {
    val c = IOS.colors
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 40.dp, vertical = 72.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = T.title3, color = c.label2, textAlign = TextAlign.Center)
        if (message != null) {
            Spacer(Modifier.height(6.dp))
            Text(message, style = T.subhead, color = c.label2, textAlign = TextAlign.Center)
        }
    }
}
