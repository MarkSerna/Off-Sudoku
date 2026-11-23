package com.logicminigames.core.data

import com.logicminigames.core.data.logic.BoardStateSerializer
import com.logicminigames.core.sudoku.engine.BoardSnapshot
import com.logicminigames.core.sudoku.model.NotesGrid
import com.logicminigames.core.sudoku.model.SudokuGrid
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BoardStateSerializerTest {

    @Test
    fun `round-trips snapshot stacks preserving grid values and pencil notes`() {
        val snap1 = BoardSnapshot(
            values = SudokuGrid.EMPTY.withValue(0, 4).withValue(40, 9),
            notes = NotesGrid.EMPTY.toggleNote(1, 2).toggleNote(1, 7).toggleNote(80, 5),
        )
        val snap2 = BoardSnapshot(
            values = snap1.values.withValue(1, 2),
            notes = snap1.notes.clearCell(1),
        )

        val encoded = BoardStateSerializer.encodeSnapshots(listOf(snap1, snap2))
        val decoded = BoardStateSerializer.decodeSnapshots(encoded)

        assertEquals(2, decoded.size)
        assertEquals(snap1.values, decoded[0].values)
        assertEquals(snap1.notes, decoded[0].notes)
        assertEquals(snap2.values, decoded[1].values)
        assertEquals(snap2.notes, decoded[1].notes)
    }

    @Test
    fun `handles empty and corrupted snapshot payloads gracefully`() {
        assertTrue(BoardStateSerializer.decodeSnapshots("").isEmpty())
        assertTrue(BoardStateSerializer.decodeSnapshots("[]").isEmpty())
        assertTrue(BoardStateSerializer.decodeSnapshots("corrupted-json").isEmpty())
    }
}
