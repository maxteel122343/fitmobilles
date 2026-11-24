package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.model.ModelVariant
import com.example.domain.model.PipelineConfig
import com.example.ml.CachedModelInfo
import com.example.ml.ModelDownloadState
import com.example.ui.theme.CyanBox
import com.example.ui.theme.FitVersoDarkArenaTheme
import com.example.ui.theme.LabBorder
import com.example.ui.theme.LabSurface
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TextSecondary
import java.io.File

@Composable
fun SystemHubScreen(
    config: PipelineConfig,
    cachedModels: List<CachedModelInfo>,
    downloadState: ModelDownloadState,
    onSelectVariant: (ModelVariant) -> Unit,
    onSelectFitLevel: (Int) -> Unit,
    onDownloadVariant: (ModelVariant, Int) -> Unit,
    onCancelDownload: () -> Unit,
    onImportCustomOnnx: (android.net.Uri) -> Unit,
    onDeleteCachedFile: (File) -> Unit,
    onUpdateConfig: ((PipelineConfig) -> PipelineConfig) -> Unit,
    activeProviderLabel: String? = null,
    providerFallbackReason: String? = null,
    modifier: Modifier = Modifier
) {
    FitVersoDarkArenaTheme {
        var activeTab by remember { mutableIntStateOf(0) }

        Column(
            modifier = modifier
                .fillMaxSize()
                .background(ObsidianBg)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(LabSurface)
                    .border(1.dp, LabBorder, RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("Modelos ONNX" to 0, "Pipeline" to 1).forEach { (label, idx) ->
                    val selected = activeTab == idx
                    Surface(
                        onClick = { activeTab = idx },
                        color = if (selected) CyanBox.copy(alpha = 0.22f) else LabSurface,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("system_tab_$idx")
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) CyanBox else TextSecondary,
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp)
                        )
                    }
                }
            }

            if (activeTab == 0) {
                ModelsScreen(
                    config = config,
                    cachedModels = cachedModels,
                    downloadState = downloadState,
                    onSelectVariant = onSelectVariant,
                    onSelectFitLevel = onSelectFitLevel,
                    onDownloadVariant = onDownloadVariant,
                    onCancelDownload = onCancelDownload,
                    onImportCustomOnnx = onImportCustomOnnx,
                    onDeleteCachedFile = onDeleteCachedFile,
                    modifier = Modifier.weight(1f)
                )
            } else {
                PipelineSettingsScreen(
                    config = config,
                    onUpdateConfig = onUpdateConfig,
                    activeProviderLabel = activeProviderLabel,
                    providerFallbackReason = providerFallbackReason,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
