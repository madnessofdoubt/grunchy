package com.grunchy.workout

import com.grunchy.workout.ui.dragSteps
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * How far a drag moves the list.
 *
 * The numbers in the device-derived cases are the real ones logged from a 3-visible-item screen
 * (unit 193.3 px, flick threshold 16 px), so these tests pin the behaviour that was measured on
 * the emulator rather than an idealised version of it.
 */
class ScrollStepsTest {

    private val unit = 193.3f
    private val flick = 16f

    @Test
    fun `one item of drag moves one item`() {
        assertEquals(1, dragSteps(accumulatedPx = unit, unitPx = unit, flickPx = flick))
        assertEquals(-1, dragSteps(accumulatedPx = -unit, unitPx = unit, flickPx = flick))
    }

    @Test
    fun `a long drag moves a whole number of items`() {
        // 400 px of drag on 193 px items: measured on the device as two items.
        assertEquals(-2, dragSteps(accumulatedPx = -382.3f, unitPx = unit, flickPx = flick))
        assertEquals(3, dragSteps(accumulatedPx = 580f, unitPx = unit, flickPx = flick))
    }

    @Test
    fun `a deliberate drag shorter than an item still moves exactly one`() {
        // The two gestures logged on the device that used to do nothing at all.
        assertEquals(-1, dragSteps(accumulatedPx = -86.2f, unitPx = unit, flickPx = flick))
        assertEquals(-1, dragSteps(accumulatedPx = -29.7f, unitPx = unit, flickPx = flick))
    }

    @Test
    fun `a nudge too small to be a drag moves nothing`() {
        assertEquals(0, dragSteps(accumulatedPx = 8f, unitPx = unit, flickPx = flick))
        assertEquals(0, dragSteps(accumulatedPx = -8f, unitPx = unit, flickPx = flick))
    }

    @Test
    fun `the remainder rounds to the nearest item edge`() {
        // 1.6 items of drag is nearer two items than one.
        assertEquals(2, dragSteps(accumulatedPx = unit * 1.6f, unitPx = unit, flickPx = flick))
        // 1.4 items is nearer one.
        assertEquals(1, dragSteps(accumulatedPx = unit * 1.4f, unitPx = unit, flickPx = flick))
    }

    @Test
    fun `a short drag with a high flick threshold is ignored`() {
        assertEquals(0, dragSteps(accumulatedPx = 80f, unitPx = unit, flickPx = 200f))
    }

    @Test
    fun `direction of travel follows the finger`() {
        // Positive is a finger moving down, which walks the list backwards; the caller subtracts.
        assertEquals(1, dragSteps(accumulatedPx = 250f, unitPx = unit, flickPx = flick))
        assertEquals(-1, dragSteps(accumulatedPx = -250f, unitPx = unit, flickPx = flick))
    }

    @Test
    fun `a list with nothing on screen cannot be scrolled`() {
        assertEquals(0, dragSteps(accumulatedPx = 500f, unitPx = 0f, flickPx = flick))
    }
}
