@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.grunchy.workout.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mudita.mmd.R
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Scrollbar geometry, taken from MMD's own scrollbar (`LazyDefaultsMMD`) so the panel reads the
 * way the native apps do: an 8 dp track in an 8 dp inset, a 1 dp border, 4 dp corners, and 24 dp
 * end icons with 16 dp of padding either side. Only the *gestures* in [ScrollList] differ.
 */
private val BarWidth: Dp = 8.dp
private val BarInset: Dp = 8.dp
private val BarBorder: Dp = 1.dp
private val BarCorners: Dp = 4.dp
private val ArrowGlyph: Dp = 24.dp
private val ArrowPad: Dp = 16.dp

/** MMD's `VERTICAL_SLIDER_MIN_HEIGHT` — the shortest thumb it will draw, in raw pixels. */
private const val ThumbMinPx = 16f

/**
 * How many items a finished drag should move.
 *
 * One item's worth of drag moves one item, and anything left over rounds to the nearest item, so
 * a gesture always lands on an item edge. A drag too short to round to anything but long enough
 * to be deliberate still counts as one item — otherwise a flick would silently do nothing.
 *
 * The sign is the drag's own: a finger moving down is positive and walks the list *backwards*,
 * so callers subtract the result from the index they started at.
 */
internal fun dragSteps(accumulatedPx: Float, unitPx: Float, flickPx: Float): Int {
    if (unitPx <= 0f) return 0
    val rounded = (accumulatedPx / unitPx).roundToInt()
    if (rounded != 0) return rounded
    return if (abs(accumulatedPx) > flickPx) (if (accumulatedPx > 0f) 1 else -1) else 0
}

/** How tall the thumb has to stay to be grabbable on a long list. */
private val ThumbMinHeight: Dp = 22.dp

/**
 * A drag shorter than this is a flick rather than a scroll, and still moves exactly one card —
 * otherwise a quick flick that never reaches a card's height would do nothing at all.
 */
private val FlickMinDistance: Dp = 12.dp

/**
 * The app's scrolling list.
 *
 * Replaces [com.mudita.mmd.components.lazy.LazyColumnMMD] for two reasons:
 *
 *  * **Scroll distance.** MMD steps a fixed `SCROLL_STEP` of 4 items per drag, so one flick
 *    throws you past the card you were reading. Here a drag accumulates and converts to steps at
 *    60% of an item's height, so a short flick moves exactly one card and a long drag keeps
 *    travelling: distance asked for, not a constant.
 *  * **Width.** MMD's scrollbar takes 24 dp of the panel and cannot be narrowed from outside the
 *    library. This one takes 10 dp, and the rest goes back to the content.
 *
 * Deliberately kept from MMD: no fling and no animation (`userScrollEnabled = false`), so pixels
 * only move when a finger or the bar asks them to, plus the large up/down targets that make a
 * long list navigable on E Ink.
 */
@Composable
fun ScrollList(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(Gap),
    content: LazyListScope.() -> Unit,
) {
    val scope = rememberCoroutineScope()
    val total = state.layoutInfo.totalItemsCount
    val scrollable = state.canScrollBackward || state.canScrollForward

    fun stepBy(items: Int) {
        val last = (total - 1).coerceAtLeast(0)
        val target = (state.firstVisibleItemIndex + items).coerceIn(0, last)
        scope.launch { state.scrollToItem(target) }
    }

    Row(modifier) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .pointerInput(scrollable) {
                    // All three of these are captured at drag start and never re-read mid-gesture:
                    // a step unit that changes as the list moves underneath makes the gesture
                    // accelerate, and targets relative to a moving index make concurrent steps
                    // collapse into one jump.
                    var accumulated = 0f
                    var baseIndex = 0
                    var appliedSteps = 0
                    var unitPx = 0f
                    var flickPx = 0f

                    suspend fun goTo(index: Int) {
                        val last = (state.layoutInfo.totalItemsCount - 1).coerceAtLeast(0)
                        state.scrollToItem(index.coerceIn(0, last))
                    }

                    detectVerticalDragGestures(
                        onDragStart = {
                            accumulated = 0f
                            appliedSteps = 0
                            baseIndex = state.firstVisibleItemIndex
                            flickPx = FlickMinDistance.toPx()
                            val items = state.layoutInfo.visibleItemsInfo
                            unitPx = if (items.isEmpty()) 0f
                            else items.sumOf { it.size }.toFloat() / items.size
                        },
                        onDragCancel = {
                            accumulated = 0f
                            appliedSteps = 0
                        },
                        onDragEnd = {
                            // Settle on a whole number of cards: one card's worth of drag moves
                            // one card, and the remainder rounds to the nearest card edge.
                            if (scrollable && unitPx > 0f) {
                                val wanted = dragSteps(accumulated, unitPx, flickPx)
                                if (wanted != 0) scope.launch { goTo(baseIndex - wanted) }
                            }
                            accumulated = 0f
                            appliedSteps = 0
                        },
                    ) { _, dragAmount ->
                        if (!scrollable || unitPx <= 0f) return@detectVerticalDragGestures
                        accumulated += dragAmount
                        // Follow the finger while it is still down; onDragEnd rounds it off.
                        val live = (accumulated / unitPx).toInt()
                        if (live != appliedSteps) {
                            appliedSteps = live
                            scope.launch { goTo(baseIndex - live) }
                        }
                    }
                },
            state = state,
            contentPadding = contentPadding,
            verticalArrangement = verticalArrangement,
            // The drag handling above is the only thing that moves this list: nothing animates.
            userScrollEnabled = false,
        ) {
            content()
        }

        if (scrollable) {
            ScrollBar(state = state, onStep = ::stepBy)
        }
    }
}

@Composable
private fun ScrollBar(state: LazyListState, onStep: (Int) -> Unit) {
    val scope = rememberCoroutineScope()
    val info = state.layoutInfo
    val total = info.totalItemsCount
    if (total == 0) return
    val ink = MaterialTheme.colorScheme.primary
    val container = MaterialTheme.colorScheme.onPrimary

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .padding(horizontal = BarInset),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        StepArrow(
            up = true,
            dotted = !state.canScrollBackward,
            onClick = { onStep(-1) },
            onHold = { scope.launch { state.scrollToItem(0) } },
        )
        Canvas(
            modifier = Modifier
                .width(BarWidth)
                .weight(1f)
                .border(BarBorder, ink, RoundedCornerShape(BarCorners))
                .pointerInput(total) {
                    detectTapGestures { offset ->
                        // Land the thumb under the finger, using its own range of travel.
                        val trackPx = size.height.toFloat()
                        val thumbPx = thumbLength(info, total, trackPx)
                        val travel = (trackPx - thumbPx).coerceAtLeast(1f)
                        val fraction = ((offset.y - thumbPx / 2f) / travel).coerceIn(0f, 1f)
                        val span = (total - info.visibleItemsInfo.size).coerceAtLeast(1)
                        scope.launch {
                            state.scrollToItem((fraction * span).toInt().coerceIn(0, total - 1))
                        }
                    }
                },
        ) {
            drawTrack(info = info, total = total, ink = ink, container = container)
        }
        StepArrow(
            up = false,
            dotted = !state.canScrollForward,
            onClick = { onStep(1) },
            onHold = { scope.launch { state.scrollToItem(total - 1) } },
        )
    }
}

/** How many items are really on screen: MMD discounts one hanging off either edge. */
private fun visibleCount(info: LazyListLayoutInfo): Int {
    val first = info.visibleItemsInfo.firstOrNull() ?: return 0
    val last = info.visibleItemsInfo.lastOrNull() ?: return 0
    val count = last.index - first.index + 1
    val fullyVisible = first.offset >= 0 &&
        last.offset + last.size <= info.viewportEndOffset - info.viewportStartOffset
    return (if (fullyVisible) count else count - 1).coerceAtLeast(1)
}

/** The thumb's length in pixels — MMD's ratio, with MMD's 16 px floor. */
private fun thumbLength(info: LazyListLayoutInfo, total: Int, trackPx: Float): Float {
    val byRatio = trackPx * visibleCount(info) / total
    return byRatio.coerceAtLeast(max(trackPx * 0.05f, ThumbMinPx))
}

/** MMD's track: a filled container inside the outline, with the thumb drawn on top of it. */
private fun DrawScope.drawTrack(
    info: LazyListLayoutInfo,
    total: Int,
    ink: Color,
    container: Color,
) {
    val first = info.visibleItemsInfo.firstOrNull() ?: return
    val visible = visibleCount(info)
    val trackPx = size.height
    val thumbPx = thumbLength(info, total, trackPx)
    val travel = (trackPx - thumbPx).coerceAtLeast(0f)
    val span = (total - visible).coerceAtLeast(1)
    val anchored = if (first.offset >= 0) first.index else (first.index + 1).coerceAtMost(total - visible)
    val offset = (anchored.toFloat() / span * travel).coerceIn(0f, travel)
    val pill = CornerRadius(size.width / 2f, size.width / 2f)

    drawRoundRect(
        color = container,
        topLeft = Offset.Zero,
        size = Size(size.width, size.height),
        cornerRadius = pill,
    )
    drawRoundRect(
        // Everything fits: the whole track is the thumb, so no thumb is drawn at all.
        color = if (visible >= total) container else ink,
        topLeft = Offset(0f, offset),
        size = Size(size.width, thumbPx),
        cornerRadius = pill,
    )
}

/**
 * One end of the scrollbar: tap steps a single item, hold jumps to that end of the list. The
 * glyphs are MMD's own — a filled arrowhead, or a dotted one where there is nothing further.
 */
@Composable
private fun StepArrow(up: Boolean, dotted: Boolean, onClick: () -> Unit, onHold: () -> Unit) {
    Icon(
        painter = painterResource(
            when {
                up && dotted -> R.drawable.chevron_dotted_up
                up -> R.drawable.chevron_filled_up
                dotted -> R.drawable.chevron_dotted_down
                else -> R.drawable.chevron_filled_down
            },
        ),
        contentDescription = null,
        tint = Color.Black,
        modifier = Modifier
            .padding(vertical = ArrowPad)
            .size(ArrowGlyph)
            .pointerInput(up, dotted) {
                detectTapGestures(onTap = { onClick() }, onLongPress = { onHold() })
            },
    )
}
