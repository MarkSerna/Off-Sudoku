package com.logicminigames.feature.sudoku.game.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.logicminigames.core.ui.theme.LocalBoardColors
import com.logicminigames.core.ui.theme.LocalBoardFontScale
import com.logicminigames.core.ui.theme.LocalColorblindMode
import com.logicminigames.feature.sudoku.game.CellUiModel
import com.logicminigames.feature.sudoku.game.cellAccessibilityDescription
import kotlin.math.roundToInt

@Composable
fun SudokuBoard(
    cells: List<CellUiModel>,
    lastPlacedIndex: Int?,
    placementTick: Int,
    errorShakeTick: Int,
    onCellClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val boardColors = LocalBoardColors.current
    val shakeOffset = remember { Animatable(0f) }

    LaunchedEffect(errorShakeTick) {
        if (errorShakeTick > 0) {
            val sequence = listOf(10f, -10f, 7f, -7f, 3f, 0f)
            for (target in sequence) {
                shakeOffset.animateTo(target, tween(durationMillis = 36))
            }
        }
    }

    val shape = RoundedCornerShape(12.dp)

    BoxWithConstraints(
        modifier = modifier
            .aspectRatio(1f)
            .offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
            .clip(shape)
            .background(boardColors.boardSurface)
            .testTag("sudoku_board"),
    ) {
        val fontMultiplier = LocalBoardFontScale.current.multiplier
        val cellSizeDp = maxWidth / 9f
        val digitSizeSp = (cellSizeDp.value * 0.54f * fontMultiplier).coerceIn(14f, 36f).sp
        val noteSizeSp = (cellSizeDp.value * 0.23f * fontMultiplier).coerceIn(7f, 15f).sp

        Column(modifier = Modifier.fillMaxSize()) {
            for (row in 0 until 9) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    for (col in 0 until 9) {
                        val idx = row * 9 + col
                        val cell = cells.getOrNull(idx)
                        if (cell != null) {
                            key(idx) {
                                SudokuCell(
                                    cell = cell,
                                    digitSizeSp = digitSizeSp,
                                    noteSizeSp = noteSizeSp,
                                    isRecentlyPlaced = idx == lastPlacedIndex,
                                    placementTick = placementTick,
                                    onClick = { onCellClick(idx) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .testTag("cell_$idx"),
                                )
                            }
                        }
                    }
                }
            }
        }

        // Single-pass crisp grid & 3x3 box divider overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cellW = w / 9f
            val cellH = h / 9f
            val thinStroke = 1.dp.toPx()
            val thickStroke = 2.4.dp.toPx()

            for (i in 1 until 9) {
                val isBoxLine = i % 3 == 0
                val color = if (isBoxLine) boardColors.boxDivider else boardColors.cellDivider
                val stroke = if (isBoxLine) thickStroke else thinStroke
                val x = cellW * i
                drawLine(color = color, start = Offset(x, 0f), end = Offset(x, h), strokeWidth = stroke)
                val y = cellH * i
                drawLine(color = color, start = Offset(0f, y), end = Offset(w, y), strokeWidth = stroke)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(width = 2.dp, color = boardColors.boardOuterBorder, shape = shape),
        )
    }
}

@Composable
private fun SudokuCell(
    cell: CellUiModel,
    digitSizeSp: androidx.compose.ui.unit.TextUnit,
    noteSizeSp: androidx.compose.ui.unit.TextUnit,
    isRecentlyPlaced: Boolean,
    placementTick: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val boardColors = LocalBoardColors.current
    val colorblindMode = LocalColorblindMode.current

    val targetBg = when {
        cell.isSelected -> boardColors.cellSelected
        cell.isError -> boardColors.cellErrorBg
        cell.isHintTarget -> boardColors.cellHintTarget
        cell.isUnitPulse -> boardColors.cellUnitPulse
        cell.isSameNumberHighlighted -> boardColors.cellSameNumberHighlight
        cell.isHintRelated -> boardColors.cellHintRelated
        cell.isPeerHighlighted -> boardColors.cellPeerHighlight
        cell.isFixed -> boardColors.cellFixedBg
        else -> boardColors.cellDefault
    }
    val bgColor by animateColorAsState(
        targetValue = targetBg,
        animationSpec = tween(durationMillis = 120),
        label = "cellBg",
    )

    val popScale = remember { Animatable(1f) }
    LaunchedEffect(placementTick, isRecentlyPlaced) {
        if (isRecentlyPlaced && placementTick > 0) {
            popScale.snapTo(1.20f)
            popScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
            )
        }
    }

    val cd = cellAccessibilityDescription(cell)
    val textColor = when {
        cell.isError -> boardColors.textError
        cell.isFixed -> boardColors.textFixed
        cell.isHintTarget -> boardColors.textHint
        else -> boardColors.textUser
    }

    Box(
        modifier = modifier
            .background(bgColor)
            .then(
                if (cell.isHintTarget) {
                    Modifier.border(width = 2.dp, color = boardColors.textHint)
                } else if (cell.isSelected) {
                    Modifier.border(width = 1.5.dp, color = boardColors.textUser.copy(alpha = 0.7f))
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = cd
                selected = cell.isSelected
                role = Role.Button
            },
        contentAlignment = Alignment.Center,
    ) {
        if (cell.value != 0) {
            Text(
                text = cell.value.toString(),
                color = textColor,
                fontSize = digitSizeSp,
                fontWeight = if (cell.isFixed) FontWeight.Bold else FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer {
                    scaleX = popScale.value
                    scaleY = popScale.value
                },
            )
        } else if (cell.notes.isNotEmpty()) {
            NotesMiniGrid(
                notes = cell.notes,
                noteSizeSp = noteSizeSp,
                textColor = boardColors.textNotes,
            )
        }

        // Geometric corner indicator for error cells in colorblind mode (and subtle visual cue)
        if (cell.isError && colorblindMode) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val markerSize = 8.dp.toPx()
                val path = Path().apply {
                    moveTo(size.width - markerSize, 0f)
                    lineTo(size.width, 0f)
                    lineTo(size.width, markerSize)
                    close()
                }
                drawPath(path = path, color = boardColors.textError)
            }
        }
    }
}

@Composable
private fun NotesMiniGrid(
    notes: Set<Int>,
    noteSizeSp: androidx.compose.ui.unit.TextUnit,
    textColor: androidx.compose.ui.graphics.Color,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(1.5.dp),
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        for (r in 0 until 3) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                for (c in 0 until 3) {
                    val digit = r * 3 + c + 1
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (digit in notes) {
                            Text(
                                text = digit.toString(),
                                color = textColor,
                                fontSize = noteSizeSp,
                                lineHeight = noteSizeSp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}
