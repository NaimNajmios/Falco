package com.najmi.falco.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.najmi.falco.data.remote.LlmProvider
import com.najmi.falco.ui.theme.FalcoBg
import com.najmi.falco.ui.theme.FalcoDivider
import com.najmi.falco.ui.theme.FalcoSurface
import com.najmi.falco.ui.theme.FalcoTextBody
import com.najmi.falco.ui.theme.FalcoTextGhost
import com.najmi.falco.ui.theme.FalcoTextMuted
import com.najmi.falco.ui.theme.FalcoTextPrimary
import com.najmi.falco.ui.theme.FalcoTypography
import com.najmi.falco.ui.theme.FalcoZeroShape

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val prefs = uiState.preferences

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FalcoBg)
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(48.dp))

        Text(
            "SYSTEM\nCONFIGURATION",
            style = FalcoTypography.headlineLarge,
            color = FalcoTextPrimary
        )

        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(FalcoDivider)
        )

        Spacer(Modifier.height(32.dp))

        SettingsSectionHeader("INTERFACE")

        Spacer(Modifier.height(12.dp))

        SettingsToggleRow(
            label = "DARK_MODE",
            description = "Enforce pure black display",
            isChecked = prefs.isDarkMode,
            onCheckedChange = { viewModel.setDarkMode(it) }
        )

        Spacer(Modifier.height(32.dp))

        SettingsSectionHeader("INFERENCE")

        Spacer(Modifier.height(12.dp))

        SettingsDropdownRow(
            label = "PRIMARY_PROVIDER",
            description = "Default LLM for verification pipeline",
            currentValue = prefs.preferredProvider,
            options = viewModel.providers.map { it.name },
            onOptionSelected = { name ->
                viewModel.setPreferredProvider(LlmProvider.valueOf(name))
            }
        )

        Spacer(Modifier.height(32.dp))

        SettingsSectionHeader("DIAGNOSTICS")

        Spacer(Modifier.height(12.dp))

        SettingsToggleRow(
            label = "DEBUG_MODE",
            description = "Verbose logging and error traces",
            isChecked = prefs.isDebugMode,
            onCheckedChange = { viewModel.setDebugMode(it) }
        )

        Spacer(Modifier.height(48.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(FalcoSurface)
                .border(1.dp, FalcoDivider, FalcoZeroShape)
                .padding(16.dp)
        ) {
            Column {
                Text("BUILD INFO", style = FalcoTypography.labelSmall, color = FalcoTextGhost)
                Spacer(Modifier.height(8.dp))
                SettingsInfoRow("VERSION", "1.0.0_STABLE")
                Spacer(Modifier.height(4.dp))
                SettingsInfoRow("ENV", "PRODUCTION")
                Spacer(Modifier.height(4.dp))
                SettingsInfoRow("BUILD", "20260328")
            }
        }

        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun SettingsSectionHeader(label: String) {
    Text(
        label,
        style = FalcoTypography.labelSmall.copy(letterSpacing = androidx.compose.ui.unit.TextUnit(2f, androidx.compose.ui.unit.TextUnitType.Sp)),
        color = FalcoTextGhost
    )
}

@Composable
private fun SettingsToggleRow(
    label: String,
    description: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(FalcoSurface)
            .border(1.dp, FalcoDivider, FalcoZeroShape)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = FalcoTypography.bodySmall.copy(fontWeight = FontWeight.Medium), color = FalcoTextPrimary)
            Spacer(Modifier.height(2.dp))
            Text(description, style = FalcoTypography.labelMedium, color = FalcoTextGhost)
        }
        FalcoSwitch(isChecked = isChecked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsDropdownRow(
    label: String,
    description: String,
    currentValue: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(FalcoSurface)
            .border(1.dp, FalcoDivider, FalcoZeroShape)
            .clickable { expanded = true }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = FalcoTypography.bodySmall.copy(fontWeight = FontWeight.Medium), color = FalcoTextPrimary)
            Spacer(Modifier.height(2.dp))
            Text(description, style = FalcoTypography.labelMedium, color = FalcoTextGhost)
        }
        Box {
            Text(
                "[$currentValue]",
                style = FalcoTypography.labelSmall,
                color = FalcoTextMuted
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(FalcoSurface)
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                option,
                                style = FalcoTypography.bodySmall,
                                color = if (option == currentValue) FalcoTextPrimary else FalcoTextBody
                            )
                        },
                        onClick = {
                            onOptionSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FalcoSwitch(
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .width(40.dp)
            .height(20.dp)
            .background(if (isChecked) FalcoTextPrimary else FalcoSurface, FalcoZeroShape)
            .border(1.dp, if (isChecked) FalcoTextPrimary else FalcoTextGhost, FalcoZeroShape)
            .clickable { onCheckedChange(!isChecked) },
        contentAlignment = if (isChecked) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .padding(2.dp)
                .fillMaxSize()
                .background(if (isChecked) FalcoBg else FalcoTextGhost, FalcoZeroShape)
        )
    }
}

@Composable
private fun SettingsInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = FalcoTypography.labelSmall, color = FalcoTextGhost)
        Text(value, style = FalcoTypography.labelSmall, color = FalcoTextMuted)
    }
}
