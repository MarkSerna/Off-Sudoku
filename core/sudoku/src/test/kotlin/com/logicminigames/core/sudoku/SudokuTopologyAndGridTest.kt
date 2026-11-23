package com.logicminigames.core.sudoku

import com.logicminigames.core.sudoku.engine.SudokuTopology
import com.logicminigames.core.sudoku.model.CellCoord
import com.logicminigames.core.sudoku.model.NotesGrid
import com.logicminigames.core.sudoku.model.SudokuGrid
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SudokuTopologyAndGridTest {

    @Test
    fun `every cell has 20 distinct symmetric peers`() {
        for (i in 0 until SudokuTopology.CELL_COUNT) {
            val peers = SudokuTopology.PEERS[i]
            assertEquals(20, peers.size)
            assertFalse(i in peers)
            assertEquals(20, peers.toSet().size)
            for (peer in peers) {
                assertTrue(i in SudokuTopology.PEERS[peer])
            }
        }
    }

    @Test
    fun `CellCoord converts cleanly between row-col and index`() {
        for (r in 0..8) {
            for (c in 0..8) {
                val coord = CellCoord(r, c)
                val roundTrip = CellCoord.fromIndex(coord.index)
                assertEquals(coord, roundTrip)
                assertEquals((r / 3) * 3 + (c / 3), coord.box)
            }
        }
    }

    @Test
    fun `SudokuGrid is immutable and validates conflicts and solutions`() {
        val initial = SudokuGrid.EMPTY
        val modified = initial.withValue(0, 5)
        assertEquals(0, initial[0])
        assertEquals(5, modified[0])
        assertTrue(modified.isValidPlacement(1, 3))
        assertFalse(modified.isValidPlacement(1, 5))
        assertFalse(modified.hasConflicts())

        val conflicted = modified.withValue(1, 5)
        assertTrue(conflicted.hasConflicts())
        assertFalse(conflicted.isValidSolution())

        val validSolution = SudokuGrid.fromString(
            "534678912" +
                "672195348" +
                "198342567" +
                "859761423" +
                "426853791" +
                "713924856" +
                "961537284" +
                "287419635" +
                "345286179",
        )
        assertTrue(validSolution.isValidSolution())
        assertEquals(81, validSolution.filledCount())
        assertEquals(validSolution, SudokuGrid.fromString(validSolution.toFlatString()))
    }

    @Test
    fun `NotesGrid supports toggling, peer removal, and serialization`() {
        var notes = NotesGrid.EMPTY
        notes = notes.toggleNote(0, 1).toggleNote(0, 7).toggleNote(1, 7)
        assertEquals(setOf(1, 7), notes.notesAt(0))
        assertTrue(notes.hasNote(1, 7))

        val clearedPeer = notes.removeDigitFromCells(intArrayOf(1), 7)
        assertFalse(clearedPeer.hasNote(1, 7))
        assertTrue(clearedPeer.hasNote(0, 7))
        assertNotEquals(notes, clearedPeer)

        val serialized = clearedPeer.toSerializedString()
        val restored = NotesGrid.fromSerializedString(serialized)
        assertEquals(clearedPeer, restored)
    }
}
