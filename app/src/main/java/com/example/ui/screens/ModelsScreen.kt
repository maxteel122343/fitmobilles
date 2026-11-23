package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.ModelVariant
import com.example.domain.model.PipelineConfig
import com.example.ml.CachedModelInfo
import com.example.ml.ModelDownloadState
import com.example.ui.theme.CoralError
import com.example.ui.theme.CyanBox
import com.example.ui.theme.GoldBone
import com.example.ui.theme.LabBorder
import com.example.ui.theme.LabSurface
import com.example.ui.theme.LabSurfaceElevated
import com.example.ui.theme.MintSuccess
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.io.File

@Composable
fun ModelsScreen(
    config: PipelineConfig,
    cachedModels: List<CachedModelInfo>,
    downloadState: ModelDownloadState,
    onSelectVariant: (ModelVariant) -> Unit,
    onSelectFitLevel: (Int) -> Unit,
    onDownloadVariant: (ModelVariant, Int) -> Unit,
    onCancelDownload: () -> Unit,
    onImportCustomOnnx: (android.net.Uri) -> Unit,
    onDeleteCachedFile: (File) -> Unit,
    modifier: Modifier = Modifier
) {
    val onnxPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let(onImportCustomOnnx)
    }

    val cachedNames = cachedModels.map { it.filename }.toSet()
    val bodyReady = cachedNames.contains(ModelVariant.BODY_ONNX)

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
                    text = "Hugging Face Model Hub",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary
                )
                Text(
                    text = "momolesang/InstantHMR · ONNX Runtime graphs + shared mhr-body.onnx (18,439-vertex mesh)",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(LabSurface)
                    .border(1.dp, LabBorder, RoundedCornerShape(14.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Kinematic Fit Mode (--fit)",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary
                    )
                    Text(
                        text = "0 = Network only · 1 = 1 crop LM · 2 = 2 fused crops",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0, 1, 2).forEach { fit ->
                        FilterChip(
                            selected = config.fitLevel == fit && config.customOnnxPath == null,
                            onClick = { onSelectFitLevel(fit) },
                            label = { Text("$fit", style = MaterialTheme.typography.labelMedium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanBox.copy(alpha = 0.22f),
                                selectedLabelColor = CyanBox
                            ),
                            modifier = Modifier.testTag("models_fit_$fit")
                        )
                    }
                }
            }
        }

        if (downloadState is ModelDownloadState.Downloading) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(LabSurfaceElevated)
                        .border(1.dp, CyanBox.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Fetching ${downloadState.filename} (${downloadState.stepIndex}/${downloadState.totalSteps})",
                            style = MaterialTheme.typography.labelMedium,
                            color = CyanBox
                        )
                        OutlinedButton(onClick = onCancelDownload) {
                            Text("Cancel", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    LinearProgressIndicator(
                        progress = { downloadState.fraction },
                        modifier = Modifier.fillMaxWidth(),
                        color = CyanBox
                    )
                    val mbRead = downloadState.bytesRead / (1024f * 1024f)
                    val mbTot = if (downloadState.totalBytes > 0) downloadState.totalBytes / (1024f * 1024f) else 0f
                    Text(
                        text = if (mbTot > 0f) String.format("%.1f / %.1f MB", mbRead, mbTot) else String.format("%.1f MB", mbRead),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }
        } else if (downloadState is ModelDownloadState.Error) {
            item {
                Surface(
                    color = CoralError.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = downloadState.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = CoralError,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        items(ModelVariant.entries) { variant ->
            val targetFile = variant.onnxFilename(config.fitLevel)
            val isReady = bodyReady && cachedNames.contains(targetFile)
            val isActive = config.modelVariant == variant && config.customOnnxPath == null

            ModelVariantCard(
                variant = variant,
                fitLevel = config.fitLevel,
                isReady = isReady,
                isActive = isActive,
                isDownloading = downloadState is ModelDownloadState.Downloading,
                onActivate = { onSelectVariant(variant) },
                onDownload = { onDownloadVariant(variant, config.fitLevel) }
            )
        }

        item {
            OutlinedButton(
                onClick = { onnxPicker.launch(arrayOf("*/*")) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("import_onnx_button")
            ) {
                Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.import_onnx))
            }
        }

        if (cachedModels.isNotEmpty()) {
            item {
                Text(
                    text = "Cached ONNX Files (${cachedModels.size})",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            items(cachedModels, key = { it.filename }) { cached ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(LabSurface)
                        .border(1.dp, LabBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = cached.filename,
                            style = MaterialTheme.typography.labelMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = String.format("%.1f MB", cached.sizeBytes / (1024f * 1024f)),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                    IconButton(
                        onClick = { onDeleteCachedFile(cached.file) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Delete ${cached.filename}",
                            tint = CoralError
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModelVariantCard(
    variant: ModelVariant,
    fitLevel: Int,
    isReady: Boolean,
    isActive: Boolean,
    isDownloading: Boolean,
    onActivate: () -> Unit,
    onDownload: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isActive) LabSurfaceElevated else LabSurface)
            .border(
                width = if (isActive) 1.5.dp else 1.dp,
                color = if (isActive) CyanBox else LabBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = variant.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Text(
                    text = "${variant.paramsMillions}M params · ${variant.licenseLabel}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (variant.commercialAllowed) MintSuccess else TextSecondary
                )
            }
            if (isReady) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = "Cached locally",
                        tint = MintSuccess,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isActive) "Active" else "Cached",
                        style = MaterialTheme.typography.labelSmall,
                        color = MintSuccess
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricPill("EMDB-1", "${variant.emdb1ForFit(fitLevel)} mm", Modifier.weight(1f))
            MetricPill("3DPW", "${variant.pw3dForFit(fitLevel)} mm", Modifier.weight(1f))
            MetricPill("S23 NPU", "${variant.npuMsForFit(fitLevel)} ms", Modifier.weight(1f))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onActivate,
                modifier = Modifier
                    .weight(1f)
                    .testTag("select_model_${variant.id}")
            ) {
                Text(
                    text = if (isActive) "Selected" else "Select",
                    color = if (isActive) CyanBox else TextPrimary
                )
            }
            Button(
                onClick = onDownload,
                enabled = !isDownloading && !isReady,
                colors = ButtonDefaults.buttonColors(containerColor = CyanBox, contentColor = ObsidianBg),
                modifier = Modifier
                    .weight(1f)
                    .testTag("download_model_${variant.id}")
            ) {
                Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (isReady) "Ready" else "Download")
            }
        }
    }
}

@Composable
private fun MetricPill(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(ObsidianBg.copy(alpha = 0.7f))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Text(text = value, style = MaterialTheme.typography.labelMedium, color = GoldBone)
    }
}
