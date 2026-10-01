@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.grunchy.workout.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grunchy.workout.data.ExerciseLibrary
import com.grunchy.workout.model.WeightUnit
import com.grunchy.workout.util.GraphBucket
import com.grunchy.workout.util.GraphRange
import com.grunchy.workout.util.GraphSeries
import com.grunchy.workout.util.MaxRepsForE1rm
import com.grunchy.workout.util.display
import com.grunchy.workout.util.fmtNumber
import com.grunchy.workout.util.graphPoints
import com.grunchy.workout.util.localDate
import com.mudita.mmd.components.cards.CardMMD
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.Zoom
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLabelComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLineComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisTickComponent
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.lineModel
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.data.ExtraStore
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToLong

private val shortDate = DateTimeFormatter.ofPattern("d MMM")
private val monthAndYear = DateTimeFormatter.ofPattern("MMM yy")

/**
 * One exercise's trend, drawn as a line: the estimated 1RM of each session, old to new.
 *
 * Everything interactive about the chart is switched off — no scroll, no zoom, no animation. On an
 * E Ink panel a gesture-driven chart repaints the screen for every frame of a fling, and the range
 * dropdown does the same job in one tap anyway.
 */
@Composable
fun GraphScreen(state: AppState, exerciseId: String, onBack: () -> Unit) {
    val unit = state.settings.weightUnit
    // Sampled once per visit: a chart must not shift under the reader while they look at it.
    val now = remember { System.currentTimeMillis() }
    var range by remember { mutableStateOf(GraphRange.ThreeMonths) }
    val series = remember(exerciseId, range, state.sessions, now) {
        graphPoints(exerciseId, state.sessions, range, now)
    }

    Column(Modifier.fillMaxSize()) {
        TopBar(
            title = ExerciseLibrary.byId(exerciseId)?.name ?: "Progress",
            navigation = { BarButton("Back", onClick = onBack) },
        )
        ScrollList(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(Gap),
        ) {
            item { SectionLabel("Timespan") }
            item {
                ValueDropdown(
                    options = GraphRange.entries,
                    selected = range,
                    optionLabel = { it.label },
                    onSelect = { range = it },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (series.points.size < 2) {
                item {
                    Note(
                        if (series.points.isEmpty()) {
                            "There is no chart for this timespan. Widen it or log a set of " +
                                "$MaxRepsForE1rm reps or fewer in order to see the line."
                        } else {
                            "There is one session in this timespan. Log another one in order " +
                                "to see the line."
                        },
                    )
                }
            } else {
                item {
                    CardMMD(Modifier.fillMaxWidth()) {
                        Column(Modifier.fillMaxWidth()) {
                            LineChart(
                                series = series,
                                unit = unit,
                                showMonthLabels = range == GraphRange.Year || range == GraphRange.All,
                            )
                        }
                    }
                }
                item { Note(caption(series, unit)) }
                if (series.skipped > 0) {
                    val skipped = series.skipped
                    item {
                        Note(
                            "$skipped of the ${series.sessions} sessions in this timespan had no " +
                                "set of $MaxRepsForE1rm reps or fewer, so " +
                                (if (skipped == 1) "it is" else "they are") + " not on the line.",
                        )
                    }
                }
            }
        }
    }
}

/**
 * The chart itself. Axes are black on white, the grid is gone, and the y-range hugs the data —
 * a line from 60 kg to 80 kg drawn from zero is a flat line with the whole point of the screen
 * squeezed into its top quarter.
 */
@Composable
private fun LineChart(series: GraphSeries, unit: WeightUnit, showMonthLabels: Boolean) {
    val producer = remember { CartesianChartModelProducer() }
    // The x-axis counts epoch days, so a gap in training shows as a gap in the line, and an axis
    // label is just that day written short. Values are in the unit on screen; storage stays kilos.
    val days = remember(series) { series.points.map { it.localDate().toEpochDay().toDouble() } }
    val values = remember(series, unit) { series.points.map { unit.display(it.e1rmKg ?: 0.0) } }

    LaunchedEffect(days, values) {
        producer.runTransaction { lineModel { series(x = days, y = values) } }
    }

    val labelStyle = remember { TextStyle(color = Color.Black, fontSize = 11.sp) }
    val chart = rememberCartesianChart(
        rememberLineCartesianLayer(
            lineProvider = LineCartesianLayer.LineProvider.series(
                LineCartesianLayer.rememberLine(
                    fill = LineCartesianLayer.LineFill.single(Fill(Color.Black)),
                    stroke = LineCartesianLayer.LineStroke.Continuous(thickness = 2.dp),
                    // Straight segments: a curve drawn between two sessions would invent numbers
                    // for the days in between.
                    interpolator = LineCartesianLayer.Interpolator.Sharp,
                ),
            ),
            rangeProvider = remember { PaddedYRange() },
        ),
        // rememberStart/rememberBottom are members of the axis companions, not top-level
        // functions: importing them by name does not compile.
        startAxis = VerticalAxis.rememberStart(
            line = rememberAxisLineComponent(fill = Fill(Color.Black), thickness = 1.dp),
            label = rememberAxisLabelComponent(style = labelStyle),
            tick = rememberAxisTickComponent(fill = Fill(Color.Black), thickness = 1.dp),
            // No dashed guidelines: on a 1-bit panel they read as dirt on the page.
            guideline = null,
            valueFormatter = CartesianValueFormatter { _, value, _ -> fmtNumber(value) },
        ),
        bottomAxis = HorizontalAxis.rememberBottom(
            line = rememberAxisLineComponent(fill = Fill(Color.Black), thickness = 1.dp),
            label = rememberAxisLabelComponent(style = labelStyle),
            tick = rememberAxisTickComponent(fill = Fill(Color.Black), thickness = 1.dp),
            guideline = null,
            valueFormatter = CartesianValueFormatter { _, value, _ ->
                LocalDate.ofEpochDay(value.roundToLong())
                    .format(if (showMonthLabels) monthAndYear else shortDate)
            },
        ),
        // The x values are day numbers, so Vico's default x-step is one day and one step is about
        // 16 dp wide: only the first week or two would fit, and with scrolling switched off the
        // rest of the log would be unreachable. Sizing the step to a twentieth of the window puts
        // the whole timespan on screen, and the axis placer then picks every fourth label.
        getXStep = { _, minX, maxX -> ((maxX - minX) / 20.0).coerceAtLeast(1.0) },
    )

    CartesianChartHost(
        chart = chart,
        modelProducer = producer,
        modifier = Modifier.fillMaxWidth().height(196.dp),
        scrollState = rememberVicoScrollState(scrollEnabled = false),
        // Content zoom, fixed: Vico lays an x-step out at a fixed dp, so an unsized chart shows
        // only its first handful of steps. This scales the whole window into the panel instead,
        // and with zoom disabled the user cannot upset it with a gesture.
        zoomState = rememberVicoZoomState(zoomEnabled = false, initialZoom = Zoom.Content),
        // Nothing moves, not even the first draw.
        animationSpec = null,
        animateIn = false,
    )
}

/** A y-range around the data rather than up from zero. */
private class PaddedYRange : CartesianLayerRangeProvider {
    override fun getMinY(minY: Double, maxY: Double, extraStore: ExtraStore): Double =
        (minY - padding(minY, maxY)).coerceAtLeast(0.0)

    override fun getMaxY(minY: Double, maxY: Double, extraStore: ExtraStore): Double =
        maxY + padding(minY, maxY)

    /** 15% of the spread, and never less than a kilogram: a flat series must not look erratic. */
    private fun padding(minY: Double, maxY: Double): Double =
        ((maxY - minY) * 0.15).coerceAtLeast(1.0)
}

/**
 * What the line is: one point per session when history is thin, and one per week or month when it
 * is not. Naming the unit of the points says which of those the reader is looking at.
 */
private fun caption(series: GraphSeries, unit: WeightUnit): String {
    val counted = when (series.bucket) {
        GraphBucket.Session -> "${series.points.size} sessions"
        GraphBucket.Week -> "${series.points.size} weeks"
        GraphBucket.Month -> "${series.points.size} months"
    }
    return "Estimated 1RM from your $counted, in ${unit.suffix}"
}
