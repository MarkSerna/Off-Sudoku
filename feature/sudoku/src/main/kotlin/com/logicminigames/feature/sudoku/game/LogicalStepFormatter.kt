package com.logicminigames.feature.sudoku.game

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.logicminigames.core.sudoku.model.LogicalStep
import com.logicminigames.core.sudoku.model.SolvingTechnique
import com.logicminigames.core.sudoku.model.SudokuDifficulty
import com.logicminigames.core.sudoku.model.UnitType
import com.logicminigames.feature.sudoku.R

@Composable
fun difficultyTitle(difficulty: SudokuDifficulty): String = when (difficulty) {
    SudokuDifficulty.VERY_EASY -> stringResource(R.string.difficulty_very_easy)
    SudokuDifficulty.EASY -> stringResource(R.string.difficulty_easy)
    SudokuDifficulty.MEDIUM -> stringResource(R.string.difficulty_medium)
    SudokuDifficulty.HARD -> stringResource(R.string.difficulty_hard)
    SudokuDifficulty.EXPERT -> stringResource(R.string.difficulty_expert)
}

@Composable
fun difficultyDescription(difficulty: SudokuDifficulty): String = when (difficulty) {
    SudokuDifficulty.VERY_EASY -> stringResource(R.string.difficulty_very_easy_desc)
    SudokuDifficulty.EASY -> stringResource(R.string.difficulty_easy_desc)
    SudokuDifficulty.MEDIUM -> stringResource(R.string.difficulty_medium_desc)
    SudokuDifficulty.HARD -> stringResource(R.string.difficulty_hard_desc)
    SudokuDifficulty.EXPERT -> stringResource(R.string.difficulty_expert_desc)
}

@Composable
fun techniqueTitle(technique: SolvingTechnique): String = when (technique) {
    SolvingTechnique.NAKED_SINGLE -> stringResource(R.string.tech_name_naked_single)
    SolvingTechnique.HIDDEN_SINGLE -> stringResource(R.string.tech_name_hidden_single)
    SolvingTechnique.POINTING_CANDIDATES -> stringResource(R.string.tech_name_pointing)
    SolvingTechnique.BOX_LINE_REDUCTION -> stringResource(R.string.tech_name_box_line)
    SolvingTechnique.NAKED_PAIR -> stringResource(R.string.tech_name_naked_pair)
    SolvingTechnique.HIDDEN_PAIR -> stringResource(R.string.tech_name_hidden_pair)
    SolvingTechnique.NAKED_TRIPLE -> stringResource(R.string.tech_name_naked_triple)
    SolvingTechnique.HIDDEN_TRIPLE -> stringResource(R.string.tech_name_hidden_triple)
    SolvingTechnique.X_WING -> stringResource(R.string.tech_name_x_wing)
    SolvingTechnique.XY_WING -> stringResource(R.string.tech_name_xy_wing)
    SolvingTechnique.NAKED_QUAD -> stringResource(R.string.tech_name_naked_quad)
    SolvingTechnique.SWORDFISH -> stringResource(R.string.tech_name_swordfish)
}

@Composable
private fun unitLabel(unitType: UnitType?, unitIndex: Int?): String {
    val oneBased = (unitIndex ?: 0) + 1
    return when (unitType) {
        UnitType.ROW -> stringResource(R.string.unit_row_arg, oneBased)
        UnitType.COLUMN -> stringResource(R.string.unit_col_arg, oneBased)
        UnitType.BOX, null -> stringResource(R.string.unit_box_arg, oneBased)
    }
}

@Composable
fun formatLogicalStepExplanation(step: LogicalStep): String {
    val row = (step.targetCell?.row ?: 0) + 1
    val col = (step.targetCell?.col ?: 0) + 1
    val digit = step.digit ?: 0
    val elimCount = step.eliminations.size
    val unit = unitLabel(step.unitType, step.unitIndex)

    return when (step.technique) {
        SolvingTechnique.NAKED_SINGLE ->
            stringResource(R.string.tech_explain_naked_single, row, col, digit)
        SolvingTechnique.HIDDEN_SINGLE ->
            stringResource(R.string.tech_explain_hidden_single, row, col, digit, unit)
        SolvingTechnique.POINTING_CANDIDATES ->
            stringResource(R.string.tech_explain_pointing, unit, digit, elimCount)
        SolvingTechnique.BOX_LINE_REDUCTION ->
            stringResource(R.string.tech_explain_box_line, unit, digit, elimCount)
        SolvingTechnique.NAKED_PAIR ->
            stringResource(R.string.tech_explain_naked_pair, unit, elimCount)
        SolvingTechnique.HIDDEN_PAIR ->
            stringResource(R.string.tech_explain_hidden_pair, unit, elimCount)
        SolvingTechnique.NAKED_TRIPLE ->
            stringResource(R.string.tech_explain_naked_triple, unit, elimCount)
        SolvingTechnique.HIDDEN_TRIPLE ->
            stringResource(R.string.tech_explain_hidden_triple, unit, elimCount)
        SolvingTechnique.X_WING ->
            stringResource(R.string.tech_explain_x_wing, digit, elimCount)
        SolvingTechnique.XY_WING ->
            stringResource(R.string.tech_explain_xy_wing, digit, elimCount)
        SolvingTechnique.NAKED_QUAD ->
            stringResource(R.string.tech_explain_naked_quad, unit, elimCount)
        SolvingTechnique.SWORDFISH ->
            stringResource(R.string.tech_explain_swordfish, digit, elimCount)
    }
}

@Composable
fun cellAccessibilityDescription(cell: CellUiModel): String {
    val r = cell.row + 1
    val c = cell.col + 1
    val base = when {
        cell.value == 0 -> stringResource(R.string.cd_cell_empty, r, c)
        cell.isFixed -> stringResource(R.string.cd_cell_fixed, r, c, cell.value)
        else -> stringResource(R.string.cd_cell_value, r, c, cell.value)
    }
    val errorSuffix = if (cell.isError) stringResource(R.string.cd_cell_error_suffix) else ""
    val hintSuffix = if (cell.isHintTarget) stringResource(R.string.cd_cell_hint_suffix) else ""
    val notesSuffix = if (cell.value == 0 && cell.notes.isNotEmpty()) {
        stringResource(R.string.cd_cell_notes_suffix, cell.notes.sorted().joinToString(" "))
    } else {
        ""
    }
    return base + errorSuffix + hintSuffix + notesSuffix
}
