package com.logicminigames.feature.sudoku.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.logicminigames.core.data.model.AchievementStatus
import com.logicminigames.core.data.model.DailyStatsSummary
import com.logicminigames.core.data.model.DifficultyStats
import com.logicminigames.core.data.model.SudokuAchievement
import com.logicminigames.core.sudoku.model.SudokuDifficulty
import com.logicminigames.core.ui.icons.AppIcons
import com.logicminigames.core.ui.util.formatElapsedSeconds
import com.logicminigames.feature.sudoku.R
import com.logicminigames.feature.sudoku.game.difficultyTitle
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: StatsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(AppIcons.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                title = {
                    Text(
                        text = stringResource(R.string.stats_screen_title),
                        fontWeight = FontWeight.SemiBold,
                    )
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = uiState.selectedTab == StatsTab.OVERVIEW,
                        onClick = { viewModel.selectTab(StatsTab.OVERVIEW) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    ) {
                        Text(stringResource(R.string.stats_tab_overview))
                    }
                    SegmentedButton(
                        selected = uiState.selectedTab == StatsTab.ACHIEVEMENTS,
                        onClick = { viewModel.selectTab(StatsTab.ACHIEVEMENTS) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    ) {
                        Text(stringResource(R.string.stats_tab_achievements))
                    }
                }

                when (uiState.selectedTab) {
                    StatsTab.OVERVIEW -> OverviewTabContent(
                        dailySummary = uiState.dailySummary,
                        todayEpochDay = uiState.todayEpochDay,
                        selectedDifficulty = uiState.selectedDifficulty,
                        stats = uiState.activeDifficultyStats,
                        onSelectDifficulty = viewModel::selectDifficulty,
                    )

                    StatsTab.ACHIEVEMENTS -> AchievementsTabContent(
                        achievements = uiState.achievements,
                    )
                }
            }
        }
    }
}

@Composable
private fun OverviewTabContent(
    dailySummary: DailyStatsSummary,
    todayEpochDay: Long,
    selectedDifficulty: SudokuDifficulty,
    stats: DifficultyStats,
    onSelectDifficulty: (SudokuDifficulty) -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = AppIcons.Calendar,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.stats_daily_section),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.semantics { heading() },
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                MetricTile(
                    label = stringResource(R.string.stats_daily_total),
                    value = dailySummary.totalCompleted.toString(),
                    modifier = Modifier.weight(1f),
                )
                MetricTile(
                    label = stringResource(R.string.stats_daily_current_streak),
                    value = dailySummary.currentStreak.toString(),
                    modifier = Modifier.weight(1f),
                )
                MetricTile(
                    label = stringResource(R.string.stats_daily_best_streak),
                    value = dailySummary.bestStreak.toString(),
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                text = stringResource(R.string.stats_daily_last_14),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val recentCompletedCount = (0..13).count { (todayEpochDay - it) in dailySummary.completedEpochDays }
            val recentSummaryCd = stringResource(R.string.cd_daily_last_14_summary, recentCompletedCount, 14)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) { contentDescription = recentSummaryCd },
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                for (offset in 13 downTo 0) {
                    val day = todayEpochDay - offset
                    val done = day in dailySummary.completedEpochDays
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(
                                if (done) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                            ),
                    )
                }
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (tier in SudokuDifficulty.entries) {
            FilterChip(
                selected = tier == selectedDifficulty,
                onClick = { onSelectDifficulty(tier) },
                label = { Text(difficultyTitle(tier)) },
            )
        }
    }

    val noTime = stringResource(R.string.stats_no_time)
    val bestTimeText = stats.bestTimeSeconds?.let(::formatElapsedSeconds) ?: noTime
    val avgTimeText = stats.averageTimeSeconds?.let(::formatElapsedSeconds) ?: noTime

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        MetricRow(
            leftLabel = stringResource(R.string.stats_metric_played),
            leftValue = stats.gamesPlayed.toString(),
            rightLabel = stringResource(R.string.stats_metric_won),
            rightValue = stats.gamesWon.toString(),
        )
        MetricRow(
            leftLabel = stringResource(R.string.stats_metric_win_rate),
            leftValue = stringResource(R.string.stats_percent_format, stats.winRatePercent),
            rightLabel = stringResource(R.string.stats_metric_best_time),
            rightValue = bestTimeText,
        )
        MetricRow(
            leftLabel = stringResource(R.string.stats_metric_avg_time),
            leftValue = avgTimeText,
            rightLabel = stringResource(R.string.stats_metric_streak),
            rightValue = stats.currentWinStreak.toString(),
        )
        MetricRow(
            leftLabel = stringResource(R.string.stats_metric_best_streak),
            leftValue = stats.bestWinStreak.toString(),
            rightLabel = stringResource(R.string.stats_metric_flawless),
            rightValue = stats.flawlessWins.toString(),
        )
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            MetricTile(
                label = stringResource(R.string.stats_metric_no_hints),
                value = stats.noHintWins.toString(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
            )
        }
    }
}

@Composable
private fun MetricRow(
    leftLabel: String,
    leftValue: String,
    rightLabel: String,
    rightValue: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        OutlinedCard(modifier = Modifier.weight(1f)) {
            MetricTile(
                label = leftLabel,
                value = leftValue,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
            )
        }
        OutlinedCard(modifier = Modifier.weight(1f)) {
            MetricTile(
                label = rightLabel,
                value = rightValue,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
            )
        }
    }
}

@Composable
private fun MetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun AchievementsTabContent(
    achievements: List<AchievementStatus>,
) {
    val unlockedCount = achievements.count { it.isUnlocked }
    val totalCount = achievements.size.coerceAtLeast(1)

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.achievements_progress, unlockedCount, totalCount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            LinearProgressIndicator(
                progress = { unlockedCount.toFloat() / totalCount.toFloat() },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    for (status in achievements) {
        val unlocked = status.isUnlocked
        val cardColors = if (unlocked) {
            CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        } else {
            CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = cardColors,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (unlocked) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    modifier = Modifier.size(44.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (unlocked) AppIcons.Trophy else AppIcons.Lock,
                            contentDescription = null,
                            tint = if (unlocked) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.outline
                            },
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = achievementTitle(status.achievement),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = achievementDescription(status.achievement),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val statusText = if (unlocked && status.unlockedAtEpochMillis != null) {
                        val dateText = DateFormat.getDateInstance(DateFormat.MEDIUM)
                            .format(Date(status.unlockedAtEpochMillis!!))
                        stringResource(R.string.achievement_unlocked_on, dateText)
                    } else {
                        stringResource(R.string.achievement_locked)
                    }
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
fun achievementTitle(achievement: SudokuAchievement): String = when (achievement) {
    SudokuAchievement.FIRST_WIN -> stringResource(R.string.ach_first_win_title)
    SudokuAchievement.NO_HINTS_WIN -> stringResource(R.string.ach_no_hints_title)
    SudokuAchievement.FLAWLESS_WIN -> stringResource(R.string.ach_flawless_title)
    SudokuAchievement.SPEED_SOLVER -> stringResource(R.string.ach_speed_title)
    SudokuAchievement.DAILY_STREAK_7 -> stringResource(R.string.ach_daily_7_title)
    SudokuAchievement.ALL_DIFFICULTIES -> stringResource(R.string.ach_all_diff_title)
    SudokuAchievement.EXPERT_FLAWLESS -> stringResource(R.string.ach_expert_flawless_title)
    SudokuAchievement.VETERAN_25 -> stringResource(R.string.ach_veteran_25_title)
}

@Composable
fun achievementDescription(achievement: SudokuAchievement): String = when (achievement) {
    SudokuAchievement.FIRST_WIN -> stringResource(R.string.ach_first_win_desc)
    SudokuAchievement.NO_HINTS_WIN -> stringResource(R.string.ach_no_hints_desc)
    SudokuAchievement.FLAWLESS_WIN -> stringResource(R.string.ach_flawless_desc)
    SudokuAchievement.SPEED_SOLVER -> stringResource(R.string.ach_speed_desc)
    SudokuAchievement.DAILY_STREAK_7 -> stringResource(R.string.ach_daily_7_desc)
    SudokuAchievement.ALL_DIFFICULTIES -> stringResource(R.string.ach_all_diff_desc)
    SudokuAchievement.EXPERT_FLAWLESS -> stringResource(R.string.ach_expert_flawless_desc)
    SudokuAchievement.VETERAN_25 -> stringResource(R.string.ach_veteran_25_desc)
}
