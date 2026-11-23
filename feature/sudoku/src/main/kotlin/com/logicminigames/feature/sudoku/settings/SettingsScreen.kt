package com.logicminigames.feature.sudoku.settings

import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.logicminigames.core.data.model.AppLanguage
import com.logicminigames.core.data.model.HintMode
import com.logicminigames.core.sudoku.engine.ErrorCheckMode
import com.logicminigames.core.ui.icons.AppIcons
import com.logicminigames.core.ui.theme.AppThemePalette
import com.logicminigames.core.ui.theme.BoardFontScale
import com.logicminigames.core.ui.theme.ThemeMode
import com.logicminigames.feature.sudoku.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    var showResetStatsDialog by rememberSaveable { mutableStateOf(false) }
    var showResetAllDialog by rememberSaveable { mutableStateOf(false) }

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
                        text = stringResource(R.string.settings_screen_title),
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
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                SectionHeader(stringResource(R.string.settings_section_appearance))

                SettingLabel(stringResource(R.string.settings_palette_title))
                val palettes = buildList {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        add(AppThemePalette.DYNAMIC)
                    }
                    add(AppThemePalette.CLASSIC)
                    add(AppThemePalette.PAPER)
                    add(AppThemePalette.OCEAN)
                    add(AppThemePalette.FOREST)
                }
                ChipRow(
                    items = palettes,
                    selected = prefs.themePalette,
                    labelFor = { paletteLabel(it) },
                    onSelect = viewModel::setThemePalette,
                )

                SettingLabel(stringResource(R.string.settings_theme_mode))
                ChipRow(
                    items = ThemeMode.entries,
                    selected = prefs.themeMode,
                    labelFor = { themeModeLabel(it) },
                    onSelect = viewModel::setThemeMode,
                )

                SwitchSettingRow(
                    title = stringResource(R.string.settings_colorblind),
                    subtitle = stringResource(R.string.settings_colorblind_desc),
                    checked = prefs.colorblindMode,
                    onCheckedChange = viewModel::setColorblindMode,
                )

                SettingLabel(stringResource(R.string.settings_font_scale))
                ChipRow(
                    items = BoardFontScale.entries,
                    selected = prefs.boardFontScale,
                    labelFor = { fontScaleLabel(it) },
                    onSelect = viewModel::setBoardFontScale,
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                SectionHeader(stringResource(R.string.settings_section_gameplay))

                SwitchSettingRow(
                    title = stringResource(R.string.settings_highlight_peers),
                    checked = prefs.highlightPeers,
                    onCheckedChange = viewModel::setHighlightPeers,
                )
                SwitchSettingRow(
                    title = stringResource(R.string.settings_highlight_same),
                    checked = prefs.highlightSameNumbers,
                    onCheckedChange = viewModel::setHighlightSameNumbers,
                )
                SwitchSettingRow(
                    title = stringResource(R.string.settings_auto_clear_notes),
                    checked = prefs.autoClearNotes,
                    onCheckedChange = viewModel::setAutoClearNotes,
                )
                SwitchSettingRow(
                    title = stringResource(R.string.settings_remaining_count),
                    checked = prefs.showRemainingCount,
                    onCheckedChange = viewModel::setShowRemainingCount,
                )

                SettingLabel(stringResource(R.string.settings_error_check))
                ChipRow(
                    items = ErrorCheckMode.entries,
                    selected = prefs.errorCheckMode,
                    labelFor = { errorModeLabel(it) },
                    onSelect = viewModel::setErrorCheckMode,
                )

                SettingLabel(stringResource(R.string.settings_hint_limit))
                ChipRow(
                    items = listOf(0, 3, 5, -1),
                    selected = prefs.hintLimit,
                    labelFor = { limit ->
                        when (limit) {
                            0 -> stringResource(R.string.hint_limit_none)
                            -1 -> stringResource(R.string.hint_limit_unlimited)
                            else -> limit.toString()
                        }
                    },
                    onSelect = viewModel::setHintLimit,
                )

                SettingLabel(stringResource(R.string.settings_hint_mode))
                ChipRow(
                    items = HintMode.entries,
                    selected = prefs.hintMode,
                    labelFor = {
                        when (it) {
                            HintMode.EXPLAIN_FIRST -> stringResource(R.string.hint_mode_explain)
                            HintMode.REVEAL_DIRECTLY -> stringResource(R.string.hint_mode_reveal)
                        }
                    },
                    onSelect = viewModel::setHintMode,
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                SectionHeader(stringResource(R.string.settings_section_feedback))

                SwitchSettingRow(
                    title = stringResource(R.string.settings_haptics),
                    checked = prefs.hapticsEnabled,
                    onCheckedChange = viewModel::setHapticsEnabled,
                )
                SwitchSettingRow(
                    title = stringResource(R.string.settings_sound),
                    checked = prefs.soundEnabled,
                    onCheckedChange = viewModel::setSoundEnabled,
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                SectionHeader(stringResource(R.string.settings_section_language))

                ChipRow(
                    items = AppLanguage.entries,
                    selected = prefs.language,
                    labelFor = { languageLabel(it) },
                    onSelect = { lang ->
                        viewModel.setLanguage(lang)
                        val locales = if (lang == AppLanguage.SYSTEM) {
                            LocaleListCompat.getEmptyLocaleList()
                        } else {
                            LocaleListCompat.forLanguageTags(lang.tag)
                        }
                        AppCompatDelegate.setApplicationLocales(locales)
                    },
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                SectionHeader(stringResource(R.string.settings_section_data))

                OutlinedButton(
                    onClick = { showResetStatsDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                ) {
                    Text(stringResource(R.string.settings_reset_stats))
                }
                Text(
                    text = stringResource(R.string.settings_reset_stats_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = { showResetAllDialog = true },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                ) {
                    Text(stringResource(R.string.settings_reset_all))
                }
                Text(
                    text = stringResource(R.string.settings_reset_all_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(R.string.settings_about_studio),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.settings_about_offline),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showResetStatsDialog) {
        AlertDialog(
            onDismissRequest = { showResetStatsDialog = false },
            title = { Text(stringResource(R.string.dialog_reset_stats_title)) },
            text = { Text(stringResource(R.string.dialog_reset_stats_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetStatsDialog = false
                        viewModel.resetStats()
                    },
                ) {
                    Text(stringResource(R.string.dialog_confirm_reset))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetStatsDialog = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            },
        )
    }

    if (showResetAllDialog) {
        AlertDialog(
            onDismissRequest = { showResetAllDialog = false },
            title = { Text(stringResource(R.string.dialog_reset_all_title)) },
            text = { Text(stringResource(R.string.dialog_reset_all_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetAllDialog = false
                        viewModel.resetAllData()
                        AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
                    },
                ) {
                    Text(
                        text = stringResource(R.string.dialog_confirm_reset),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetAllDialog = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            },
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .padding(top = 8.dp, bottom = 4.dp)
            .semantics { heading() },
    )
}

@Composable
private fun SettingLabel(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
    )
}

@Composable
private fun SwitchSettingRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null,
) {
    ListItem(
        modifier = Modifier.toggleable(
            value = checked,
            role = Role.Switch,
            onValueChange = onCheckedChange,
        ),
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = null,
            )
        },
    )
}

@Composable
private fun <T> ChipRow(
    items: List<T>,
    selected: T,
    labelFor: @Composable (T) -> String,
    onSelect: (T) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (item in items) {
            FilterChip(
                selected = item == selected,
                onClick = { onSelect(item) },
                label = { Text(labelFor(item)) },
            )
        }
    }
}

@Composable
private fun paletteLabel(palette: AppThemePalette): String = when (palette) {
    AppThemePalette.DYNAMIC -> stringResource(R.string.palette_dynamic)
    AppThemePalette.CLASSIC -> stringResource(R.string.palette_classic)
    AppThemePalette.PAPER -> stringResource(R.string.palette_paper)
    AppThemePalette.OCEAN -> stringResource(R.string.palette_ocean)
    AppThemePalette.FOREST -> stringResource(R.string.palette_forest)
}

@Composable
private fun themeModeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.SYSTEM -> stringResource(R.string.theme_mode_system)
    ThemeMode.LIGHT -> stringResource(R.string.theme_mode_light)
    ThemeMode.DARK -> stringResource(R.string.theme_mode_dark)
}

@Composable
private fun fontScaleLabel(scale: BoardFontScale): String = when (scale) {
    BoardFontScale.SMALL -> stringResource(R.string.font_scale_small)
    BoardFontScale.NORMAL -> stringResource(R.string.font_scale_normal)
    BoardFontScale.LARGE -> stringResource(R.string.font_scale_large)
    BoardFontScale.EXTRA_LARGE -> stringResource(R.string.font_scale_extra_large)
}

@Composable
private fun errorModeLabel(mode: ErrorCheckMode): String = when (mode) {
    ErrorCheckMode.IMMEDIATE -> stringResource(R.string.error_mode_immediate)
    ErrorCheckMode.ON_DEMAND -> stringResource(R.string.error_mode_on_demand)
    ErrorCheckMode.OFF -> stringResource(R.string.error_mode_off)
}

@Composable
private fun languageLabel(language: AppLanguage): String = when (language) {
    AppLanguage.SYSTEM -> stringResource(R.string.lang_system)
    AppLanguage.SPANISH -> stringResource(R.string.lang_es)
    AppLanguage.ENGLISH -> stringResource(R.string.lang_en)
    AppLanguage.PORTUGUESE -> stringResource(R.string.lang_pt)
}
