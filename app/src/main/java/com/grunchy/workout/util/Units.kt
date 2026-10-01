package com.grunchy.workout.util

import com.grunchy.workout.model.Settings
import com.grunchy.workout.model.WeightUnit
import kotlin.math.abs
import kotlin.math.round

/** One international avoirdupois pound in kilos, to the limit of a double. */
const val LbPerKg = 2.2046226218487757

/**
 * The number to put on screen for a weight held in kilos.
 *
 * Kilos keep their hundredths, so a 62.5 kg set still reads 62.5. Pounds round to the whole
 * pound: that is the increment a pound-based gym has plates for, and a weight converted from an
 * old kilo log has no business claiming a tenth of a pound.
 */
fun WeightUnit.display(kg: Double): Double = when (this) {
    WeightUnit.KG -> round(kg * 100.0) / 100.0
    WeightUnit.LB -> round(kg * LbPerKg)
}

/** Turns a number the user picked back into kilos, which is what gets stored. */
fun WeightUnit.toKg(value: Double): Double = when (this) {
    WeightUnit.KG -> value
    WeightUnit.LB -> value / LbPerKg
}

/** "60 kg", "132 lbs". */
fun fmtWeight(kg: Double, unit: WeightUnit): String = "${fmtNumber(unit.display(kg))} ${unit.suffix}"

/** "60", "132" — the bare number, for a control whose menu or column header carries the unit. */
fun fmtWeightValue(kg: Double, unit: WeightUnit): String = fmtNumber(unit.display(kg))

/** Coarse dropdown jumps: kilos move in 2.5s, pounds in 5s — one plate change either way. */
fun coarseStepChoices(unit: WeightUnit): List<Double> = when (unit) {
    WeightUnit.KG -> listOf(2.5, 5.0, 10.0)
    WeightUnit.LB -> listOf(5.0, 10.0, 25.0)
}

/** What one tap of the ± buttons moves. */
fun fineStepChoices(unit: WeightUnit): List<Double> = when (unit) {
    WeightUnit.KG -> listOf(0.25, 0.5, 1.0)
    WeightUnit.LB -> listOf(1.0, 2.0, 5.0)
}

/** The heaviest weight the dropdown will offer. */
fun maxWeightChoices(unit: WeightUnit): List<Double> = when (unit) {
    WeightUnit.KG -> listOf(100.0, 150.0, 200.0, 260.0, 300.0)
    WeightUnit.LB -> listOf(200.0, 300.0, 400.0, 500.0, 600.0)
}

/**
 * Re-expresses the weight settings in another unit.
 *
 * Those three numbers are in the unit the user chose, so switching picks the *equivalent option*
 * rather than converting the number: a 5 kg coarse jump becomes 10 lbs, not 11.02 lbs.
 */
fun Settings.withWeightUnit(unit: WeightUnit): Settings {
    if (unit == weightUnit) return this
    return copy(
        weightUnit = unit,
        coarseStep = sameChoice(coarseStep, coarseStepChoices(weightUnit), coarseStepChoices(unit)),
        fineStep = sameChoice(fineStep, fineStepChoices(weightUnit), fineStepChoices(unit)),
        maxWeight = sameChoice(maxWeight, maxWeightChoices(weightUnit), maxWeightChoices(unit)),
    )
}

/** The same rank of choice in the other unit's list, or the nearest value if it no longer exists. */
private fun sameChoice(value: Double, from: List<Double>, to: List<Double>): Double {
    val rank = from.indexOf(value).takeIf { it >= 0 }
        ?: from.indices.minByOrNull { abs(from[it] - value) }
        ?: 0
    return to[rank.coerceIn(to.indices)]
}
