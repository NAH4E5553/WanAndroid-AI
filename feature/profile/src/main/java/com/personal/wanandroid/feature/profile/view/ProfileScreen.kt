package com.personal.wanandroid.feature.profile.view

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.personal.wanandroid.core.data.model.ThemeModePreference
import com.personal.wanandroid.core.data.model.ThemePalettePreference
import com.personal.wanandroid.core.data.model.ThemePreferences
import com.personal.wanandroid.core.designsystem.theme.WanPalette
import com.personal.wanandroid.core.designsystem.theme.WanPaletteSwatches
import com.personal.wanandroid.core.designsystem.theme.WanSpacing
import com.personal.wanandroid.core.designsystem.theme.swatches
import com.personal.wanandroid.core.ui.component.list.AppListItem
import com.personal.wanandroid.core.ui.component.list.SettingsSectionLabel
import com.personal.wanandroid.core.ui.component.scaffold.AppScaffold
import com.personal.wanandroid.feature.profile.R
import com.personal.wanandroid.feature.profile.state.ThemeSettingsUiState
import com.personal.wanandroid.feature.profile.viewmodel.ProfileViewModel
import com.personal.wanandroid.feature.profile.viewmodel.ThemeSettingsViewModel

@Composable
fun ProfileRoute(
    onLogin: () -> Unit,
    onThemeSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.themeState.collectAsStateWithLifecycle()
    ProfileScreen(
        themeSummary = state.preferences.summary(),
        currentPalette = state.preferences.palette,
        onLogin = onLogin,
        onThemeSettings = onThemeSettings,
        modifier = modifier
    )
}

@Composable
fun ProfileScreen(
    themeSummary: String,
    currentPalette: ThemePalettePreference,
    onLogin: () -> Unit,
    onThemeSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(WanSpacing.page),
        verticalArrangement = Arrangement.spacedBy(WanSpacing.page)
    ) {
        Text(
            text = stringResource(R.string.profile),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold
        )
        AccountCard(onLogin)
        SettingsSectionLabel(stringResource(R.string.my_content))
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            UnavailableRow(stringResource(R.string.collections))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            UnavailableRow(stringResource(R.string.history))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            UnavailableRow(stringResource(R.string.offline))
        }
        SettingsSectionLabel(stringResource(R.string.preferences))
        ThemeEntryRow(
            summary = themeSummary,
            palette = currentPalette,
            onClick = onThemeSettings
        )
    }
}

@Composable
private fun AccountCard(onLogin: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(WanSpacing.page),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.guest_avatar),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
            Spacer(Modifier.width(WanSpacing.medium))
            Text(
                text = stringResource(R.string.login_supporting),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(WanSpacing.small))
            Button(onClick = onLogin) { Text(stringResource(R.string.login)) }
        }
    }
}

@Composable
private fun UnavailableRow(title: String) {
    ListItem(
        headlineContent = { Text(title) },
        trailingContent = {
            Text(
                text = stringResource(R.string.pending),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
private fun ThemeEntryRow(summary: String, palette: ThemePalettePreference, onClick: () -> Unit) {
    AppListItem(
        title = stringResource(R.string.appearance_and_theme),
        description = summary,
        modifier = Modifier.testTag("theme-entry"),
        onClick = onClick,
        leadingContent = {
            PaletteDots(
                palette.toWanPalette().swatches(isCurrentThemeDark()),
                modifier = Modifier.clearAndSetSemantics {}
            )
        },
        trailingContent = {
            Text(
                "›",
                fontSize = 28.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clearAndSetSemantics {}
            )
        }
    )
}

@Composable
fun ThemeSettingsRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ThemeSettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val failureMessage = stringResource(R.string.theme_save_failed)
    LaunchedEffect(state.saveFailureEvent) {
        val event = state.saveFailureEvent
        if (event != 0L) {
            snackbarHostState.showSnackbar(failureMessage)
            viewModel.clearSaveFailure(event)
        }
    }
    ThemeSettingsScreen(
        uiState = state,
        onBack = onBack,
        onPaletteSelected = viewModel::selectPalette,
        onModeSelected = viewModel::selectMode,
        snackbarHostState = snackbarHostState,
        modifier = modifier
    )
}

@Composable
fun ThemeSettingsScreen(
    uiState: ThemeSettingsUiState,
    onBack: () -> Unit,
    onPaletteSelected: (ThemePalettePreference) -> Unit,
    onModeSelected: (ThemeModePreference) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    AppScaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = WanSpacing.page, vertical = WanSpacing.small),
            verticalArrangement = Arrangement.spacedBy(WanSpacing.page)
        ) {
            ThemeTopBar(onBack)
            if (uiState.readFailed) {
                Text(
                    text = stringResource(R.string.theme_read_failed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
            SettingsSectionLabel(stringResource(R.string.palette_style))
            PaletteGrid(
                selected = uiState.preferences.palette,
                mode = uiState.preferences.mode,
                onSelected = onPaletteSelected
            )
            SettingsSectionLabel(stringResource(R.string.display_mode))
            ModeSelector(selected = uiState.preferences.mode, onSelected = onModeSelected)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(WanSpacing.small))
                }
                Text(
                    text = if (uiState.isSaving) {
                        stringResource(R.string.theme_saving)
                    } else {
                        stringResource(R.string.theme_applies_immediately)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ThemeTopBar(onBack: () -> Unit) {
    val backLabel = stringResource(R.string.back)
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.semantics { contentDescription = backLabel }
        ) {
            Text(
                text = "‹",
                fontSize = 36.sp,
                modifier = Modifier.clearAndSetSemantics {}
            )
        }
        Text(
            text = stringResource(R.string.appearance_and_theme),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun PaletteGrid(
    selected: ThemePalettePreference,
    mode: ThemeModePreference,
    onSelected: (ThemePalettePreference) -> Unit
) {
    val palettes = ThemePalettePreference.entries
    val dark = when (mode) {
        ThemeModePreference.FOLLOW_SYSTEM -> isSystemInDarkTheme()
        ThemeModePreference.LIGHT -> false
        ThemeModePreference.DARK -> true
    }
    Column(
        modifier = Modifier.selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(WanSpacing.medium)
    ) {
        palettes.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(WanSpacing.medium)
            ) {
                row.forEach { palette ->
                    PaletteCard(
                        palette = palette,
                        selected = palette == selected,
                        dark = dark,
                        onClick = { onSelected(palette) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun PaletteCard(
    palette: ThemePalettePreference,
    selected: Boolean,
    dark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedDescription = stringResource(
        if (selected) R.string.theme_selected else R.string.theme_not_selected
    )
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        ),
        modifier = modifier
            .heightIn(min = 112.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics { stateDescription = selectedDescription }
            .testTag("palette-${palette.name.lowercase()}")
    ) {
        Column(
            modifier = Modifier.padding(WanSpacing.page),
            verticalArrangement = Arrangement.spacedBy(WanSpacing.medium)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = palette.label(),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                if (selected) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "✓",
                                color = MaterialTheme.colorScheme.onPrimary,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.clearAndSetSemantics {}
                            )
                        }
                    }
                }
            }
            PaletteDots(
                swatches = palette.toWanPalette().swatches(dark),
                modifier = Modifier.clearAndSetSemantics {}
            )
        }
    }
}

@Composable
private fun PaletteDots(swatches: WanPaletteSwatches, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(swatches.primary, swatches.secondary, swatches.container).forEach { color ->
            Surface(color = color, shape = CircleShape, modifier = Modifier.size(24.dp)) {}
        }
    }
}

@Composable
private fun ModeSelector(selected: ThemeModePreference, onSelected: (ThemeModePreference) -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(4.dp).selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ThemeModePreference.entries.forEach { mode ->
                val isSelected = selected == mode
                val selectedDescription = stringResource(
                    if (isSelected) R.string.theme_selected else R.string.theme_not_selected
                )
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        Color.Transparent
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .selectable(
                            selected = isSelected,
                            role = Role.RadioButton,
                            onClick = { onSelected(mode) }
                        )
                        .semantics { stateDescription = selectedDescription }
                        .testTag("mode-${mode.name.lowercase()}")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = mode.label(),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemePreferences.summary(): String = stringResource(
    R.string.theme_summary,
    palette.label(),
    mode.label()
)

@Composable
private fun ThemePalettePreference.label(): String = stringResource(
    when (this) {
        ThemePalettePreference.INK_TEAL -> R.string.palette_ink_teal
        ThemePalettePreference.SLATE_BLUE -> R.string.palette_slate_blue
        ThemePalettePreference.WARM_AMBER -> R.string.palette_warm_amber
        ThemePalettePreference.BERRY_ROSE -> R.string.palette_berry_rose
    }
)

@Composable
private fun ThemeModePreference.label(): String = stringResource(
    when (this) {
        ThemeModePreference.FOLLOW_SYSTEM -> R.string.mode_follow_system
        ThemeModePreference.LIGHT -> R.string.mode_light
        ThemeModePreference.DARK -> R.string.mode_dark
    }
)

private fun ThemePalettePreference.toWanPalette(): WanPalette = when (this) {
    ThemePalettePreference.INK_TEAL -> WanPalette.INK_TEAL
    ThemePalettePreference.SLATE_BLUE -> WanPalette.SLATE_BLUE
    ThemePalettePreference.WARM_AMBER -> WanPalette.WARM_AMBER
    ThemePalettePreference.BERRY_ROSE -> WanPalette.BERRY_ROSE
}

@Composable
private fun isCurrentThemeDark(): Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f
