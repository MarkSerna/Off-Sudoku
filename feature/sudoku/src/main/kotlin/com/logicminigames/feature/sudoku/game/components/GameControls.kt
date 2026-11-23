package com.logicminigames.feature.sudoku.game.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.logicminigames.core.data.model.SudokuAchievement
import com.logicminigames.core.sudoku.engine.ErrorCheckMode
import com.logicminigames.core.sudoku.model.SudokuDifficulty
import com.logicminigames.core.ui.components.ConfettiOverlay
import com.logicminigames.core.ui.icons.AppIcons
import com.logicminigames.core.ui.util.formatElapsedSeconds
import com.logicminigames.feature.sudoku.R
import com.logicminigames.feature.sudoku.game.HintBannerUiModel
import com.logicminigames.feature.sudoku.game.difficultyTitle
import com.logicminigames.feature.sudoku.game.formatLogicalStepExplanation
import com.logicminigames.feature.sudoku.game.techniqueTitle
import com.logicminigames.feature.sudoku.stats.achievementTitle

@Composable
fun GameActionBar(
    canUndo: Boolean,
    canRedo: Boolean,
    isNotesMode: Boolean,
    hintsRemaining: Int?,
    showCheckErrorsButton: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onErase: () -> Unit,
    onToggleNotes: () -> Unit,
    onHint: () -> Unit,
    onCheckErrors: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ActionToolButton(
            icon = AppIcons.Undo,
            label = stringResource(R.string.action_undo),
            enabled = canUndo,
            testTag = "action_undo",
            onClick = onUndo,
        )
        ActionToolButton(
            icon = AppIcons.Redo,
            label = stringResource(R.string.action_redo),
            enabled = canRedo,
            testTag = "action_redo",
            onClick = onRedo,
        )
        ActionToolButton(
            icon = AppIcons.Backspace,
            label = stringResource(R.string.action_erase),
            enabled = true,
            testTag = "action_erase",
            onClick = onErase,
        )
        ActionToolButton(
            icon = AppIcons.Edit,
            label = if (isNotesMode) stringResource(R.string.action_notes_on) else stringResource(R.string.action_notes),
            enabled = true,
            highlighted = isNotesMode,
            testTag = "action_notes",
            onClick = onToggleNotes,
        )
        ActionToolButton(
            icon = AppIcons.Lightbulb,
            label = stringResource(R.string.action_hint),
            enabled = hintsRemaining == null || hintsRemaining > 0,
            badgeText = hintsRemaining?.toString() ?: stringResource(R.string.game_hints_unlimited),
            testTag = "action_hint",
            onClick = onHint,
        )
        if (showCheckErrorsButton) {
            ActionToolButton(
                icon = AppIcons.CheckCircle,
                label = stringResource(R.string.action_check),
                enabled = true,
                testTag = "action_check",
                onClick = onCheckErrors,
            )
        }
    }
}

@Composable
private fun ActionToolButton(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    highlighted: Boolean = false,
    badgeText: String? = null,
    testTag: String? = null,
    onClick: () -> Unit,
) {
    val containerColor = if (highlighted) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
    }
    val contentColor = if (highlighted) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 2.dp),
    ) {
        BadgedBox(
            badge = {
                if (badgeText != null) {
                    Badge {
                        Text(text = badgeText, fontSize = 10.sp)
                    }
                }
            },
        ) {
            FilledTonalIconButton(
                onClick = onClick,
                enabled = enabled,
                modifier = Modifier
                    .size(48.dp)
                    .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = containerColor,
                    contentColor = contentColor,
                ),
            ) {
                Icon(imageVector = icon, contentDescription = label, modifier = Modifier.size(22.dp))
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outline,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun NumberPad(
    remainingDigitCounts: Map<Int, Int>,
    showRemainingCount: Boolean,
    isNotesMode: Boolean,
    isCompactRows: Boolean,
    onDigitClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rows = if (isCompactRows) {
        listOf(listOf(1, 2, 3, 4, 5), listOf(6, 7, 8, 9))
    } else {
        listOf(listOf(1, 2, 3), listOf(4, 5, 6), listOf(7, 8, 9))
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (rowDigits in rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            ) {
                for (digit in rowDigits) {
                    val remaining = remainingDigitCounts[digit] ?: 0
                    val enabled = remaining > 0 || isNotesMode
                    NumberKey(
                        digit = digit,
                        remaining = remaining,
                        showRemainingCount = showRemainingCount,
                        enabled = enabled,
                        onClick = { onDigitClick(digit) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun NumberKey(
    digit: Int,
    remaining: Int,
    showRemainingCount: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cd = stringResource(R.string.cd_number_key, digit, remaining)
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(vertical = 6.dp, horizontal = 4.dp),
        modifier = modifier
            .heightIn(min = 54.dp)
            .testTag("num_key_$digit")
            .semantics { contentDescription = cd },
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = digit.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            if (showRemainingCount) {
                Text(
                    text = remaining.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.78f),
                )
            }
        }
    }
}

@Composable
fun HintBanner(
    hint: HintBannerUiModel,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title: String
    val message: String
    val canApply: Boolean

    when (hint) {
        is HintBannerUiModel.Logical -> {
            title = techniqueTitle(hint.step.technique)
            message = formatLogicalStepExplanation(hint.step)
            canApply = true
        }
        is HintBannerUiModel.WrongCells -> {
            title = stringResource(R.string.hint_wrong_cells_title)
            message = stringResource(R.string.hint_wrong_cells_desc, hint.count)
            canApply = false
        }
        is HintBannerUiModel.DirectReveal -> {
            title = stringResource(R.string.hint_fallback_title)
            message = stringResource(
                R.string.hint_fallback_desc,
                hint.cell.row + 1,
                hint.cell.col + 1,
                hint.digit,
            )
            canApply = true
        }
    }

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(R.string.hint_dismiss))
                }
                if (canApply) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = onApply) {
                        Text(text = stringResource(R.string.hint_apply))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HintChoiceSheet(
    onExplain: () -> Unit,
    onReveal: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                text = stringResource(R.string.hint_sheet_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            )
            Surface(
                onClick = onExplain,
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.hint_explain_title)) },
                    supportingContent = { Text(stringResource(R.string.hint_explain_desc)) },
                    leadingContent = { Icon(AppIcons.Lightbulb, contentDescription = null) },
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                onClick = onReveal,
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.hint_reveal_title)) },
                    supportingContent = { Text(stringResource(R.string.hint_reveal_desc)) },
                    leadingContent = { Icon(AppIcons.CheckCircle, contentDescription = null) },
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun PauseOverlay(
    difficulty: SudokuDifficulty,
    elapsedSeconds: Long,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .testTag("pause_overlay")
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = {},
            ),
        contentAlignment = Alignment.Center,
    ) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = AppIcons.VisibilityOff,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.pause_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.pause_board_hidden),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(
                        R.string.pause_meta,
                        difficultyTitle(difficulty),
                        formatElapsedSeconds(elapsedSeconds),
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onResume,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                ) {
                    Icon(AppIcons.Play, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.pause_resume))
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onRestart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                ) {
                    Icon(AppIcons.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.pause_restart))
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = onHome,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                ) {
                    Text(stringResource(R.string.pause_home))
                }
            }
        }
    }
}

@Composable
fun VictoryOverlay(
    difficulty: SudokuDifficulty,
    isDaily: Boolean,
    elapsedSeconds: Long,
    mistakes: Int,
    hintsUsed: Int,
    isNewBestTime: Boolean,
    onNewGame: () -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
    unlockedAchievements: List<SudokuAchievement> = emptyList(),
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.55f))
            .testTag("victory_overlay")
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = {},
            ),
        contentAlignment = Alignment.Center,
    ) {
        ConfettiOverlay()
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = AppIcons.Trophy,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp),
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.victory_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isDaily) {
                        stringResource(R.string.victory_daily_subtitle)
                    } else {
                        difficultyTitle(difficulty)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (isNewBestTime) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(999.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.victory_new_best),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    VictoryStatColumn(
                        label = stringResource(R.string.victory_stat_time),
                        value = formatElapsedSeconds(elapsedSeconds),
                    )
                    VictoryStatColumn(
                        label = stringResource(R.string.victory_stat_mistakes),
                        value = mistakes.toString(),
                    )
                    VictoryStatColumn(
                        label = stringResource(R.string.victory_stat_hints),
                        value = hintsUsed.toString(),
                    )
                }
                if (unlockedAchievements.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = stringResource(R.string.victory_achievements_unlocked),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    for (ach in unlockedAchievements.take(3)) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(999.dp),
                            modifier = Modifier.padding(vertical = 2.dp),
                        ) {
                            Text(
                                text = achievementTitle(ach),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onNewGame,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                ) {
                    Text(stringResource(R.string.victory_new_game))
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onHome,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                ) {
                    Text(stringResource(R.string.victory_home))
                }
            }
        }
    }
}

@Composable
private fun VictoryStatColumn(
    label: String,
    value: String,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
