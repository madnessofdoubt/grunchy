package com.grunchy.workout

import com.grunchy.workout.ui.counted
import org.junit.Assert.assertEquals
import org.junit.Test

/** The count line on a routine card and a history row: "1 exercises" is not a sentence. */
class CountedTest {

    @Test
    fun `a single one takes the singular`() {
        assertEquals("1 exercise", counted(1, "exercise"))
        assertEquals("1 set", counted(1, "set"))
    }

    @Test
    fun `everything else takes the plural`() {
        assertEquals("4 exercises", counted(4, "exercise"))
        assertEquals("12 sets", counted(12, "set"))
        assertEquals("0 exercises", counted(0, "exercise"))
    }
}
