package com.grunchy.workout

import com.grunchy.workout.data.Store
import com.grunchy.workout.model.Settings
import com.grunchy.workout.model.WeightUnit
import com.grunchy.workout.util.LbPerKg
import com.grunchy.workout.util.coarseStepChoices
import com.grunchy.workout.util.display
import com.grunchy.workout.util.fineStepChoices
import com.grunchy.workout.util.fmtWeight
import com.grunchy.workout.util.fmtWeightValue
import com.grunchy.workout.util.maxWeightChoices
import com.grunchy.workout.util.toKg
import com.grunchy.workout.util.weightOptions
import com.grunchy.workout.util.withWeightUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Kilos and pounds.
 *
 * Sets are stored in kilos however the user logs them, so the interesting cases are the two
 * boundaries: what a stored weight reads as in the other unit, and what a picked weight becomes
 * when it is stored.
 */
class UnitsTest {

    @Test
    fun `a weight logged in pounds is stored in kilos and reads back the same`() {
        val stored = WeightUnit.LB.toKg(135.0)

        assertEquals(61.235, stored, 0.001)
        assertEquals(135.0, WeightUnit.LB.display(stored), 0.0)
    }

    @Test
    fun `pounds display as whole pounds and kilos keep their halves`() {
        // A kilo log from before the unit existed has no hundredths of a pound to offer.
        assertEquals(132.0, WeightUnit.LB.display(60.0), 0.0)
        assertEquals(62.5, WeightUnit.KG.display(62.5), 0.0)
        assertEquals(25.33, WeightUnit.KG.display(25.3333), 0.0)
    }

    @Test
    fun `weights are labelled with the chosen unit`() {
        assertEquals("60 kg", fmtWeight(60.0, WeightUnit.KG))
        assertEquals("132 lbs", fmtWeight(60.0, WeightUnit.LB))
        assertEquals("132", fmtWeightValue(60.0, WeightUnit.LB))
        assertEquals(2.2046226218487757, LbPerKg, 0.0)
    }

    @Test
    fun `switching unit picks the equivalent choice, not a converted number`() {
        val kilos = Settings(coarseStep = 5.0, fineStep = 0.5, maxWeight = 260.0)

        val pounds = kilos.withWeightUnit(WeightUnit.LB)

        assertEquals(WeightUnit.LB, pounds.weightUnit)
        // 5 kg of plates is a pair of 5s; nobody jumps 11.02 lbs.
        assertEquals(10.0, pounds.coarseStep, 0.0)
        assertEquals(2.0, pounds.fineStep, 0.0)
        assertEquals(500.0, pounds.maxWeight, 0.0)
    }

    @Test
    fun `switching unit and back returns the original settings`() {
        val kilos = Settings()
        assertEquals(kilos, kilos.withWeightUnit(WeightUnit.LB).withWeightUnit(WeightUnit.KG))
    }

    @Test
    fun `a value that is not a choice snaps to the nearest one`() {
        // A file edited by hand, or written by an older build with different options.
        val odd = Settings(coarseStep = 7.0, fineStep = 0.4, maxWeight = 250.0)

        val pounds = odd.withWeightUnit(WeightUnit.LB)

        assertEquals(10.0, pounds.coarseStep, 0.0)
        assertEquals(2.0, pounds.fineStep, 0.0)
        assertEquals(500.0, pounds.maxWeight, 0.0)
    }

    @Test
    fun `the dropdown runs from zero to the maximum in steps of the coarse jump`() {
        val kilos = weightOptions(Settings().coarseStep, Settings().maxWeight)
        assertEquals(0.0, kilos.first(), 0.0)
        assertEquals(260.0, kilos.last(), 0.0)
        assertEquals(53, kilos.size)

        val pounds = weightOptions(Settings().withWeightUnit(WeightUnit.LB).let { it.coarseStep },
            Settings().withWeightUnit(WeightUnit.LB).let { it.maxWeight })
        assertEquals(0.0, pounds.first(), 0.0)
        assertEquals(500.0, pounds.last(), 0.0)
        assertEquals(51, pounds.size)
    }

    @Test
    fun `both units offer three jumps, a fine step and a top weight`() {
        for (unit in WeightUnit.entries) {
            assertEquals(3, coarseStepChoices(unit).size)
            assertEquals(3, fineStepChoices(unit).size)
            assertEquals(5, maxWeightChoices(unit).size)
            assertTrue(maxWeightChoices(unit).zipWithNext().all { (a, b) -> b > a })
        }
    }

    @Test
    fun `a settings file with no unit is read as kilos`() {
        val decoded = Store.decode("""{ "settings": { "coarseStepKg": 5.0 } }""")

        assertEquals(WeightUnit.KG, decoded.settings.weightUnit)
        assertEquals(5.0, decoded.settings.coarseStep, 0.0)
    }

    @Test
    fun `a pound setting survives a save and load`() {
        val settings = Settings().withWeightUnit(WeightUnit.LB)

        val reloaded = Store.decode(Store.encode(com.grunchy.workout.model.AppData(settings = settings)))

        assertEquals(settings, reloaded.settings)
    }
}
