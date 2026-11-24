package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.model.ExecutionDevice
import com.example.domain.model.PipelineConfig
import com.example.domain.model.PoseFilterKind
import com.example.ui.theme.CyanBox
import com.example.ui.theme.GoldBone
import com.example.ui.theme.LabBorder
import com.example.ui.theme.LabSurface
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TerracottaMesh
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

@Composable
fun PipelineSettingsScreen(
    config: PipelineConfig,
    onUpdateConfig: ((PipelineConfig) -> PipelineConfig) -> Unit,
    activeProviderLabel: String? = null,
    providerFallbackReason: String? = null,
    modifier: Modifier = Modifier
) {
    var focalText by remember(config.customFocalPx) {
        mutableStateOf(config.customFocalPx?.toInt()?.toString() ?: "")
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Pipeline Parameters",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary
                )
                Text(
                    text = "Matches InstantHMR CLI flags (--fit, --device, --pose-filter, --smooth, --detector-stride, --focal)",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        item {
            SettingsCard(title = "Inference & Kinematic Fit") {
                Text(
                    text = "Kinematic Fit Crops (--fit)",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextPrimary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0 to "0 (Net)", 1 to "1 Crop", 2 to "2 Crops").forEach { (lvl, label) ->
                        FilterChip(
                            selected = config.fitLevel == lvl,
                            onClick = { onUpdateConfig { it.copy(fitLevel = lvl) } },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanBox.copy(alpha = 0.22f),
                                selectedLabelColor = CyanBox
                            ),
                            modifier = Modifier.testTag("settings_fit_$lvl")
                        )
                    }
                }

                Text(
                    text = "ONNX Execution Provider (--device)",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextPrimary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExecutionDevice.entries.forEach { dev ->
                        FilterChip(
                            selected = config.executionDevice == dev,
                            onClick = { onUpdateConfig { it.copy(executionDevice = dev) } },
                            label = { Text(dev.label, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanBox.copy(alpha = 0.22f),
                                selectedLabelColor = CyanBox
                            )
                        )
                    }
                }
                Text(
                    text = "Provedor ativo: ${activeProviderLabel ?: "Aguardando inferência (${config.executionDevice.label})"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (providerFallbackReason != null) GoldBone else CyanBox
                )
                if (!providerFallbackReason.isNullOrBlank()) {
                    Text(
                        text = "Fallback automático: $providerFallbackReason",
                        style = MaterialTheme.typography.labelSmall,
                        color = GoldBone
                    )
                }

                OutlinedTextField(
                    value = focalText,
                    onValueChange = { raw ->
                        focalText = raw.filter { it.isDigit() || it == '.' }
                        val parsed = focalText.toFloatOrNull()
                        onUpdateConfig { it.copy(customFocalPx = if (parsed != null && parsed > 50f) parsed else null) }
                    },
                    label = { Text("Camera Focal Length px (--focal, blank = 1.05 * diag)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        item {
            SettingsCard(title = "Detector & Keypoint Tracking") {
                SliderSettingRow(
                    label = "Max Persons (--max-persons)",
                    valueText = "${config.maxPersons}",
                    value = config.maxPersons.toFloat(),
                    range = 1f..8f,
                    steps = 6,
                    onValueChange = { v -> onUpdateConfig { it.copy(maxPersons = v.roundToInt()) } }
                )
                SliderSettingRow(
                    label = "Confidence Threshold (--det-confidence)",
                    valueText = String.format("%.2f", config.detConfidence),
                    value = config.detConfidence,
                    range = 0.20f..0.90f,
                    steps = 0,
                    onValueChange = { v -> onUpdateConfig { it.copy(detConfidence = v) } }
                )
                SliderSettingRow(
                    label = "Detector Stride (--detector-stride)",
                    valueText = "Every ${config.detectorStride} fr",
                    value = config.detectorStride.toFloat(),
                    range = 1f..10f,
                    steps = 8,
                    onValueChange = { v -> onUpdateConfig { it.copy(detectorStride = v.roundToInt()) } }
                )
            }
        }

        item {
            SettingsCard(title = "Temporal Video Filters") {
                Text(
                    text = "Pose Filter (--pose-filter, filters MHR pose before mesh build)",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PoseFilterKind.entries.forEach { kind ->
                        FilterChip(
                            selected = config.poseFilter == kind,
                            onClick = { onUpdateConfig { it.copy(poseFilter = kind) } },
                            label = { Text(kind.label, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GoldBone.copy(alpha = 0.22f),
                                selectedLabelColor = GoldBone
                            )
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Output Keypoint/Mesh Smoother (--smooth)",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "1 Euro filter on 2D/3D joints, vertices & cam_trans",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = config.smoothOutput,
                        onCheckedChange = { checked -> onUpdateConfig { it.copy(smoothOutput = checked) } },
                        colors = SwitchDefaults.colors(checkedThumbColor = ObsidianBg, checkedTrackColor = CyanBox)
                    )
                }

                if (config.smoothOutput) {
                    SliderSettingRow(
                        label = "Smoother Beta Scale",
                        valueText = String.format("%.2f", config.smoothBetaScale),
                        value = config.smoothBetaScale,
                        range = 0.2f..3.0f,
                        steps = 0,
                        onValueChange = { v -> onUpdateConfig { it.copy(smoothBetaScale = v) } }
                    )
                }
            }
        }

        item {
            SettingsCard(title = "3D Mesh Appearance") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Render MHR Body Mesh (--no-mesh off)",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextPrimary
                    )
                    Switch(
                        checked = config.showMesh,
                        onCheckedChange = { checked -> onUpdateConfig { it.copy(showMesh = checked) } },
                        colors = SwitchDefaults.colors(checkedThumbColor = ObsidianBg, checkedTrackColor = TerracottaMesh)
                    )
                }
                SliderSettingRow(
                    label = "Mesh Opacity (--mesh-alpha)",
                    valueText = String.format("%.2f", config.meshAlpha),
                    value = config.meshAlpha,
                    range = 0.05f..1.0f,
                    steps = 0,
                    onValueChange = { v -> onUpdateConfig { it.copy(meshAlpha = v) } }
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Modo Diagnóstico HUD (latência & bbox)",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Exibe ms de inferência e caixas delimitadoras na tela 2D",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = config.showDiagnosticHud,
                        onCheckedChange = { checked -> onUpdateConfig { it.copy(showDiagnosticHud = checked) } },
                        colors = SwitchDefaults.colors(checkedThumbColor = ObsidianBg, checkedTrackColor = CyanBox),
                        modifier = Modifier.testTag("toggle_diagnostic_hud_switch")
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(LabSurface)
            .border(1.dp, LabBorder, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = CyanBox
        )
        content()
    }
}

@Composable
private fun SliderSettingRow(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
            Text(text = valueText, style = MaterialTheme.typography.labelMedium, color = GoldBone)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            steps = steps,
            colors = SliderDefaults.colors(thumbColor = CyanBox, activeTrackColor = CyanBox)
        )
    }
}
