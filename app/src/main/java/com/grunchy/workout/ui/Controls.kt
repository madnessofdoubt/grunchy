@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.grunchy.workout.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grunchy.workout.model.WeightUnit
import com.grunchy.workout.util.display
import com.grunchy.workout.util.fmtNumber
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.menus.DropdownMenuMMD
import com.mudita.mmd.components.menus.DropdownMenuItemMMD
import com.mudita.mmd.components.cards.CardMMD
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.text_field.TextFieldMMD
import com.mudita.mmd.components.top_app_bar.TopAppBarMMD

/**
 * E Ink ground rules these controls follow:
 *  * every target is at least [ControlHeight] tall but no taller than it needs to be,
 *    because the Kompakt panel is only ~600 dp high in portrait,
 *  * state changes swap content instead of animating it (no AnimatedVisibility anywhere),
 *  * one tap changes one number: no keyboards, no spinners, no dialogs that fade in.
 */
val ControlHeight: Dp = 44.dp
val BarHeight: Dp = 40.dp
val Gap: Dp = 6.dp

/** Bottom tab buttons: bigger than the app bar buttons, matching the native bar's weight. */
val TabHeight: Dp = 54.dp

/**
 * The rule under every app bar. Measured off the native Mudita apps: 4 px on a 213 dpi
 * panel, i.e. exactly 3 dp, full width.
 */
val RuleHeight: Dp = 3.dp

/** App bar title size. The native bars run 1.25x MMD's own 20 sp titleMedium. */
val TitleSize: TextUnit = 25.sp

/** Subtitle under an app bar title (the "writings above the bar"). */
val SubtitleSize: TextUnit = 15.sp

/** App bar height: the native bars put their rule 101 px below the status bar, i.e. 76 dp. */
val AppBarHeight: Dp = 76.dp

/** Compact button sized for app bars and inside list rows. */
@Composable
fun BarButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
    enabled: Boolean = true,
    height: Dp = BarHeight,
    labelSize: TextUnit = 14.sp,
) {
    val content: @Composable () -> Unit = {
        TextMMD(label, maxLines = 1, fontSize = labelSize, fontWeight = if (filled) FontWeight.Bold else null)
    }
    if (filled) {
        ButtonMMD(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier.height(height),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
        ) { content() }
    } else {
        OutlinedButtonMMD(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier.height(height),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
        ) { content() }
    }
}

/**
 * The app bar every screen shares: a big title, an optional line under it, and a thick
 * full width rule.
 *
 * MMD's own app bar divider draws **1 dp**: it hands its 3 dp to the modifier's *width*
 * and never sets the divider's thickness. The rule is drawn here instead, at the 3 dp the
 * native apps use. The height is 76 dp for the same reason: that is where the native rule
 * lands, and it leaves room for a two line subtitle.
 */
@Composable
fun TopBar(
    title: String,
    subtitle: String? = null,
    subtitleFont: FontFamily? = null,
    navigation: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    Column(Modifier.fillMaxWidth()) {
        TopAppBarMMD(
            title = {
                Column {
                    TextMMD(
                        text = title,
                        fontSize = TitleSize,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (subtitle != null) {
                        TextMMD(
                            text = subtitle,
                            fontSize = SubtitleSize,
                            maxLines = 2,
                            fontFamily = subtitleFont,
                            lineHeight = 18.sp,
                        )
                    }
                }
            },
            navigationIcon = navigation,
            actions = actions,
            expandedHeight = AppBarHeight,
            showDivider = false,
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(RuleHeight)
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}

/**
 * Mudita's settings glyph: three sliders, hollow knobs sitting left, middle and right.
 * Drawn here because MMD ships components but no icon set.
 *
 * The line stops at an end knob and runs to the edge otherwise, which is what the native
 * icon does; the white disc punches the line out from under the hollow ring.
 */
@Composable
fun SettingsIcon(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(48.dp)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(26.dp)) {
            val stroke = 2.4.dp.toPx()
            val radius = 3.6.dp.toPx()
            val knobXs = listOf(0.24f, 0.5f, 0.76f).map { it * size.width }
            val ys = listOf(0.2f, 0.5f, 0.8f).map { it * size.height }
            ys.forEachIndexed { index, y ->
                val from = if (index == 0) knobXs[index] else 0f
                val to = if (index == ys.lastIndex) knobXs[index] else size.width
                drawLine(Color.Black, Offset(from, y), Offset(to, y), strokeWidth = stroke)
                drawCircle(Color.White, radius = radius + stroke, center = Offset(knobXs[index], y))
                drawCircle(
                    color = Color.Black,
                    radius = radius,
                    center = Offset(knobXs[index], y),
                    style = Stroke(width = stroke),
                )
            }
        }
    }
}

/**
 * The "i" in a circle that sits beside the sliders: the app bar slot that explains the name.
 *
 * Drawn rather than typed so it carries the same stroke weight as the icon next to it, and so
 * nothing depends on a font being able to draw a glyph at this size.
 */
@Composable
fun InfoIcon(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(48.dp)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(26.dp)) {
            val stroke = 2.4.dp.toPx()
            val radius = size.minDimension / 2f - stroke / 2f
            drawCircle(
                color = Color.Black,
                radius = radius,
                center = center,
                style = Stroke(width = stroke),
            )
            // A lowercase i: a round dot over a stem, both inside the ring.
            drawCircle(Color.Black, radius = stroke * 0.8f, center = Offset(center.x, center.y - radius * 0.5f))
            drawLine(
                color = Color.Black,
                start = Offset(center.x, center.y - radius * 0.08f),
                end = Offset(center.x, center.y + radius * 0.52f),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
    }
}

/**
 * The ⓘ card: what a screen's one unfamiliar idea means, in the user's own words. It covers the
 * page instead of floating over it — a scrim would have to be grey on a 1-bit panel, and grey
 * dithers — so the screen behind is painted out and the card is the only thing on the display.
 *
 * [fontFamily] is for text MMD's Lato has no glyphs for: the About note is part Cyrillic.
 */
@Composable
fun InfoCard(text: String, onClose: () -> Unit, fontFamily: FontFamily? = null) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp),
        contentAlignment = Alignment.Center,
    ) {
        CardMMD(Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(Gap),
            ) {
                TextMMD(text = text, fontSize = 14.sp, fontFamily = fontFamily)
                PrimaryAction(label = "Close", onClick = onClose)
            }
        }
    }
}

/** Full width call to action. */
@Composable
fun PrimaryAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    ButtonMMD(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(48.dp),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
    ) {
        TextMMD(label, maxLines = 1, fontSize = 15.sp, fontWeight = FontWeight.Bold, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun SecondaryAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButtonMMD(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(48.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
    ) {
        TextMMD(label, maxLines = 1, fontSize = 15.sp, overflow = TextOverflow.Ellipsis)
    }
}

/** Small +/- button used next to a value.
 *
 * Note: MMD's button applies defaultMinSize(50.dp) outside whatever modifier is passed in,
 * so these can never be made narrower — budget 50 dp per step button when sizing a row.
 */
@Composable
fun StepButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButtonMMD(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(ControlHeight),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
    ) {
        TextMMD(label, maxLines = 1, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

/**
 * A read-only looking field that opens a plain list of options. This is the app's whole
 * answer to "no typing": tap the value, tap the one you want.
 */
@Composable
fun <T> ValueDropdown(
    options: List<T>,
    selected: T?,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    prefix: String? = null,
    suffix: String? = null,
    bold: Boolean = false,
    menuHeight: Dp = 320.dp,
    /** What to show when nothing is selected, e.g. "no limit" for an open ended range. */
    nullLabel: String = "—",
    /** Longer label used only inside the menu, e.g. "60 kg" for a button that shows "60". */
    menuOptionLabel: ((T) -> String)? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButtonMMD(
            onClick = { expanded = true },
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(ControlHeight),
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
        ) {
            if (prefix != null) {
                TextMMD(prefix, maxLines = 1, fontSize = 11.sp)
                Spacer(Modifier.width(4.dp))
            }
            TextMMD(
                text = selected?.let(optionLabel) ?: nullLabel,
                maxLines = 1,
                fontSize = 15.sp,
                fontWeight = if (bold) FontWeight.Bold else null,
                overflow = TextOverflow.Ellipsis,
            )
            if (suffix != null) {
                Spacer(Modifier.width(4.dp))
                TextMMD(suffix, maxLines = 1, fontSize = 11.sp)
            }
            Spacer(Modifier.width(4.dp))
            TextMMD("▾", fontSize = 11.sp)
        }
        DropdownMenuMMD(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = menuHeight),
        ) {
            options.forEach { option ->
                val isSelected = option == selected
                val label = menuOptionLabel?.invoke(option) ?: optionLabel(option)
                DropdownMenuItemMMD(
                    text = {
                        TextMMD(
                            text = if (isSelected) "• $label" else label,
                            fontSize = 15.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else null,
                        )
                    },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    },
                )
            }
        }
    }
}

/**
 * The weight control: a coarse dropdown (10 lb jumps by default, 5 kg in kilos) with a fine
 * adjuster either side, so a plate change never costs more than a few taps.
 *
 * The control works in the user's unit — [onWeightChange] hands back a number in that unit and
 * the caller stores it in kilos — while [weightKg] stays canonical. The button shows the bare
 * number and the menu carries the unit: this control lives in a 480 px wide row, and "kg" was
 * already enough to push the value into an ellipsis.
 */
@Composable
fun WeightPicker(
    weightKg: Double,
    unit: WeightUnit,
    options: List<Double>,
    fineStep: Double,
    onWeightChange: (Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shown = unit.display(weightKg)
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        StepButton(
            label = "−${trimStep(fineStep)}",
            onClick = { onWeightChange((shown - fineStep).coerceAtLeast(0.0)) },
        )
        ValueDropdown(
            options = options,
            selected = shown,
            optionLabel = { fmtNumber(it) },
            menuOptionLabel = { "${fmtNumber(it)} ${unit.suffix}" },
            onSelect = { onWeightChange(it.coerceAtLeast(0.0)) },
            modifier = Modifier.weight(1f),
            bold = true,
            menuHeight = 300.dp,
        )
        StepButton(
            label = "+${trimStep(fineStep)}",
            onClick = { onWeightChange(shown + fineStep) },
        )
    }
}

/** 0.5 -> ".5", 1.0 -> "1", 0.25 -> ".25": a +/- label has to fit in ~34 dp. */
private fun trimStep(step: Double): String {
    val n = fmtNumber(step)
    return if (n.startsWith("0.")) n.removePrefix("0") else n
}

/** Static black bar used for the progression rows; no animation, no gradients. */
@Composable
fun StaticBar(fraction: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(10.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(2.dp))
            .background(MaterialTheme.colorScheme.surface),
    ) {
        if (fraction > 0f) {
            Box(
                Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .height(10.dp)
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    TextMMD(
        text = text.uppercase(),
        modifier = modifier,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
    )
}

/** Small print: on a 1-bit panel "muted" has to mean smaller, not grey. */
@Composable
fun Note(text: String, modifier: Modifier = Modifier, fontFamily: FontFamily? = null) {
    // The theme font is Lato, which has no Cyrillic: pass a fontFamily for non-Latin text.
    TextMMD(text = text, modifier = modifier, fontSize = 12.sp, fontFamily = fontFamily)
}

@Composable
fun KeyValueRow(key: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextMMD(key, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        TextMMD(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

/** Typing is allowed, never required. Only used for custom routine names. */
@Composable
fun NameField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Name",
) {
    TextFieldMMD(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        label = { TextMMD(label, fontSize = 13.sp) },
    )
}

/**
 * Confirmation without a dialog: dialogs animate, and an animation on E Ink is a visible
 * flicker. This swaps in place instead.
 */
@Composable
fun InlineConfirm(
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Gap),
    ) {
        TextMMD(message, fontSize = 13.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(Gap)) {
            BarButton(confirmLabel, onClick = onConfirm, filled = true)
            BarButton("Cancel", onClick = onCancel)
        }
    }
}

/** Tappable row used by lists; no ripple because ThemeMMD disables it globally. */
@Composable
fun TapRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) { content() }
}

/** Vertical separator used between major blocks. */
@Composable
fun ThinDivider(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outline),
    )
}

/** Right aligned spacer helper so rows can end flush. */
@Composable
fun RowScope.RowSpacer() {
    Box(Modifier.weight(1f))
}

@Composable
fun CenterNote(text: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
        TextMMD(text, fontSize = 13.sp, textAlign = TextAlign.Center)
    }
}

@Composable
fun FilledTag(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(3.dp))
            .background(Color.Transparent)
            .padding(horizontal = 5.dp, vertical = 1.dp),
    ) {
        TextMMD(text, fontSize = 11.sp, maxLines = 1)
    }
}
