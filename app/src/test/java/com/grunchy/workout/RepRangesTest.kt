package com.grunchy.workout

import com.grunchy.workout.model.AmrapLabel
import com.grunchy.workout.model.AmrapPrefillReps
import com.grunchy.workout.model.DefaultRepRanges
import com.grunchy.workout.model.RepRange
import com.grunchy.workout.util.nextRepRange
import com.grunchy.workout.util.parseRangeLabel
import com.grunchy.workout.util.prefillRepsFor
import com.grunchy.workout.util.replaceRepRange
import com.grunchy.workout.util.withRepRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Rep ranges: how they read, what they prefill, and what happens when they are edited. */
class RepRangesTest {

    @Test
    fun labelsReadAsTheUserWroteThem() {
        assertEquals("1-5", RepRange(1, 5).label)
        assertEquals("5-10", RepRange(5, 10).label)
        assertEquals("10-15", RepRange(10, 15).label)
        assertEquals("15+", RepRange(15, null).label)
        assertEquals(listOf("1-5", "5-10", "10-15", "15+", "AMRAP"), DefaultRepRanges.map { it.label })
    }

    @Test
    fun aSetStartsInTheMiddleOfItsRange() {
        assertEquals(3, RepRange(1, 5).prefillReps)
        assertEquals(8, RepRange(5, 10).prefillReps)
        assertEquals(13, RepRange(10, 15).prefillReps)
        // Open ended ranges start at their floor: there is no middle to 15+.
        assertEquals(15, RepRange(15, null).prefillReps)
    }

    @Test
    fun prefillRepsFollowTheStoredLabel() {
        assertEquals(8, prefillRepsFor("5-10", DefaultRepRanges))
        assertEquals(15, prefillRepsFor("15+", DefaultRepRanges))
    }

    @Test
    fun aRangeEditedAwayStillFallsBackToItsLabel() {
        val only = listOf(RepRange(1, 5))
        // "5-10" is gone from settings, but a routine still points at it: 8 is still right.
        assertEquals(8, prefillRepsFor("5-10", only))
        assertEquals(8, prefillRepsFor("nonsense", only))
    }

    @Test
    fun labelsParseBackIntoSomethingPrefillable() {
        assertEquals(RepRange(5, 10), parseRangeLabel("5-10"))
        assertEquals(RepRange(15, null), parseRangeLabel("15+"))
        assertEquals(8, parseRangeLabel("5-10")?.prefillReps)
        assertNull(parseRangeLabel("whatever"))
    }

    @Test
    fun boundsAreKeptInOrder() {
        assertEquals(RepRange(8, 8), RepRange(8, 5).normalized())
        assertEquals(RepRange(5, 10), RepRange(5, 10).normalized())
        assertEquals(RepRange(15, null), RepRange(15, null).normalized())
    }

    @Test
    fun addingKeepsRangesUniqueAndSorted() {
        val added = withRepRange(DefaultRepRanges, RepRange(20, 25))
        assertEquals(listOf("1-5", "5-10", "10-15", "15+", "20-25", "AMRAP"), added.map { it.label })

        // Adding a range that already exists changes nothing.
        assertEquals(DefaultRepRanges, withRepRange(DefaultRepRanges, RepRange(5, 10)))

        // A new low range sorts to the front.
        assertEquals("1-3", withRepRange(DefaultRepRanges, RepRange(1, 3)).first().label)
    }

    @Test
    fun editingARangeIntoAnExistingOneDropsTheDuplicate() {
        val changed = replaceRepRange(DefaultRepRanges, index = 0, changed = RepRange(5, 10))
        assertTrue("the old 1-5 is gone", changed.none { it.label == "1-5" })
        assertEquals(1, changed.count { it.label == "5-10" })
    }

    @Test
    fun editingARangeKeepsTheListSorted() {
        val changed = replaceRepRange(DefaultRepRanges, index = 3, changed = RepRange(20, null))
        assertEquals(listOf("1-5", "5-10", "10-15", "20+", "AMRAP"), changed.map { it.label })
    }

    @Test
    fun addOffersTheNextBlockOfFive() {
        assertEquals(RepRange(16, 20), nextRepRange(DefaultRepRanges))
        assertEquals(RepRange(1, 5), nextRepRange(emptyList()))
    }

    @Test
    fun amrapIsOneOptionWithNoBounds() {
        assertEquals(AmrapLabel, RepRange(amrap = true).label)
        // Bounds left behind by an earlier edit cannot leak into it: the label is the choice.
        assertEquals(AmrapLabel, RepRange(12, 15, amrap = true).label)
        assertEquals(RepRange(12, 15, amrap = true), RepRange(12, 15, amrap = true).normalized())
        assertEquals(AmrapPrefillReps, RepRange(amrap = true).prefillReps)
    }

    @Test
    fun amrapParsesBackAndSitsLastInTheList() {
        assertEquals(RepRange(amrap = true), parseRangeLabel("AMRAP"))
        assertEquals(RepRange(amrap = true), parseRangeLabel("amrap"))
        assertEquals(AmrapPrefillReps, prefillRepsFor("AMRAP", DefaultRepRanges))

        // It has no bounds, so it sorts after every numeric range and never duplicates.
        val added = withRepRange(DefaultRepRanges, RepRange(amrap = true))
        assertEquals(AmrapLabel, added.last().label)
        assertEquals(1, added.count { it.amrap })

        // And it cannot drag the next suggested block upwards: that needs a real upper bound.
        assertEquals(RepRange(16, 20), nextRepRange(DefaultRepRanges))
        assertEquals(RepRange(1, 5), nextRepRange(listOf(RepRange(amrap = true))))
    }
}
