package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.AiProvidersConfig
import com.example.domain.model.MeasurementUnit
import com.example.domain.model.ModelVariant
import com.example.domain.model.PipelineConfig
import com.example.domain.model.SubscriptionTier
import com.example.domain.model.UserFitnessProfile
import com.example.ml.CachedModelInfo
import com.example.ml.ModelDownloadState
import com.example.ui.theme.DarkInk
import com.example.ui.theme.GoldBone
import com.example.ui.theme.LightCanvasBg
import com.example.ui.theme.LightCardBg
import com.example.ui.theme.LightCardBorder
import com.example.ui.theme.MintSuccess
import com.example.ui.theme.MutedInk
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ProViolet
import com.example.ui.theme.StopCoral
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.TealSoft
import com.example.ui.theme.TextPrimary
import java.io.File

@Composable
fun ProfileSettingsScreen(
    profile: UserFitnessProfile,
    config: PipelineConfig,
    aiConfig: AiProvidersConfig,
    isVariantCached: Boolean,
    cachedModels: List<CachedModelInfo>,
    downloadState: ModelDownloadState,
    snapshotCount: Int,
    webServerRunning: Boolean,
    webServerUrls: List<String>,
    openEngineInitial: Boolean = false,
    onOpenAiSettings: () -> Unit,
    onToggleWebServer: () -> Unit,
    onExportWebHtml: (Uri) -> Unit,
    onDownloadProjectZip: () -> Unit,
    onUpdatePreferences: (
        weeklyGoalDays: Int?,
        remindersEnabled: Boolean?,
        unitSystem: MeasurementUnit?,
        defaultFrontCamera: Boolean?
    ) -> Unit,
    onOpenPlans: () -> Unit,
    onExportSnapshotsJson: (Uri) -> Unit,
    onRequirePro: (String) -> Unit,
    onClearAllHistory: () -> Unit,
    onSelectVariant: (ModelVariant) -> Unit,
    onSelectFitLevel: (Int) -> Unit,
    onDownloadVariant: (ModelVariant, Int) -> Unit,
    onCancelDownload: () -> Unit,
    onImportCustomOnnx: (Uri) -> Unit,
    onDeleteCachedFile: (File) -> Unit,
    onUpdateConfig: ((PipelineConfig) -> PipelineConfig) -> Unit,
    activeProviderLabel: String? = null,
    providerFallbackReason: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current

    var showEngineSubpage by remember(openEngineInitial) { mutableStateOf(openEngineInitial) }
    var freqMenuOpen by remember { mutableStateOf(false) }
    var unitMenuOpen by remember { mutableStateOf(false) }
    var camMenuOpen by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }

    val snapJsonLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let(onExportSnapshotsJson)
    }
    val webHtmlLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/html")) { uri ->
        uri?.let(onExportWebHtml)
    }

    if (showEngineSubpage) {
        BackHandler { showEngineSubpage = false }
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(ObsidianBg)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { showEngineSubpage = false },
                    modifier = Modifier.testTag("engine_subpage_back")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Voltar para Perfil",
                        tint = TextPrimary
                    )
                }
                Text(
                    text = "Motor InstantHMR & Parâmetros",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
            }
            SystemHubScreen(
                config = config,
                cachedModels = cachedModels,
                downloadState = downloadState,
                onSelectVariant = onSelectVariant,
                onSelectFitLevel = onSelectFitLevel,
                onDownloadVariant = onDownloadVariant,
                onCancelDownload = onCancelDownload,
                onImportCustomOnnx = onImportCustomOnnx,
                onDeleteCachedFile = onDeleteCachedFile,
                onUpdateConfig = onUpdateConfig,
                activeProviderLabel = activeProviderLabel,
                providerFallbackReason = providerFallbackReason,
                modifier = Modifier.weight(1f)
            )
        }
        return
    }

    val isPro = profile.tier == SubscriptionTier.PREMIUM

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(LightCanvasBg),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Athlete Header Card
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(LightCardBg)
                    .border(1.dp, LightCardBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(TealSoft)
                ) {
                    Icon(Icons.Rounded.Person, null, tint = TealPrimary, modifier = Modifier.size(30.dp))
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = profile.userName,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = DarkInk
                    )
                    Text(
                        text = "${profile.tier.displayName} · ${profile.totalLifetimeReps} reps registradas",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedInk
                    )
                }
                Surface(
                    onClick = onOpenPlans,
                    color = if (isPro) ProViolet.copy(alpha = 0.14f) else TealSoft,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Rounded.WorkspacePremium,
                            null,
                            tint = if (isPro) ProViolet else TealPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isPro) "PRO" else "Upgrade",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isPro) ProViolet else TealPrimary
                        )
                    }
                }
            }
        }

        // 2. Multi-Provider AI & BYOK Keys Card
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(LightCardBg)
                    .border(1.dp, TealPrimary.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .clickable { onOpenAiSettings() }
                    .padding(16.dp)
                    .testTag("profile_open_ai_settings"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(TealSoft)
                ) {
                    Icon(Icons.Rounded.AutoAwesome, null, tint = TealPrimary, modifier = Modifier.size(22.dp))
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Modelos de IA & Chaves de API",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = DarkInk
                    )
                    Text(
                        text = "Texto: ${aiConfig.textProvider.displayName} · Voz: ${aiConfig.voiceProvider.displayName} · Img: ${aiConfig.imageProvider.displayName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedInk
                    )
                }
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = TealPrimary)
            }
        }

        // 3. Web Mode (Mobile Browser & PC Browser)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(LightCardBg)
                    .border(1.dp, LightCardBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp)
                    .testTag("web_mode_card"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.Language, null, tint = TealPrimary, modifier = Modifier.size(22.dp))
                    Column {
                        Text(
                            text = "Modo Web (Navegador Celular & PC)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = DarkInk
                        )
                        Text(
                            text = "Rode o FitVerso no Chrome/Edge/Safari no celular ou computador com webcam.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedInk
                        )
                    }
                }

                Button(
                    onClick = onToggleWebServer,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (webServerRunning) StopCoral else TealPrimary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("toggle_web_server_button")
                ) {
                    Text(if (webServerRunning) "Parar Servidor Web Local" else "Iniciar Servidor Web Local (Porta 8765)")
                }

                if (webServerRunning && webServerUrls.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        webServerUrls.forEach { url ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(TealSoft.copy(alpha = 0.65f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = url,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = TealPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { clipboard.setText(AnnotatedString(url)) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Rounded.ContentCopy, contentDescription = "Copiar URL", tint = TealPrimary, modifier = Modifier.size(16.dp))
                                }
                                IconButton(
                                    onClick = {
                                        runCatching {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                        }
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Rounded.OpenInBrowser, contentDescription = "Abrir no navegador", tint = TealPrimary, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                        Text(
                            text = "Dica de câmera: no navegador deste celular use http://127.0.0.1:8765. No PC, exporte o HTML abaixo ou abra via localhost.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedInk
                        )
                    }
                }

                OutlinedButton(
                    onClick = { webHtmlLauncher.launch("fitverso_web.html") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("export_web_html_button")
                ) {
                    Icon(Icons.Rounded.Download, null, tint = TealPrimary, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Baixar fitverso_web.html (Abrir no PC / Offline)", color = DarkInk)
                }

                Button(
                    onClick = onDownloadProjectZip,
                    colors = ButtonDefaults.buttonColors(containerColor = TealSoft, contentColor = TealPrimary),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, TealPrimary.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                        .testTag("profile_download_project_zip_button")
                ) {
                    Icon(Icons.Rounded.FolderZip, null, tint = TealPrimary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Baixar Projeto Inteiro (.ZIP)", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // 4. Preferences Card (Objetivo semanal, Lembretes, Unidades)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(LightCardBg)
                    .border(1.dp, LightCardBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Weekly Goal
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Objetivo de frequência semanal", style = MaterialTheme.typography.bodyMedium, color = DarkInk)
                    Box {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, LightCardBorder, RoundedCornerShape(10.dp))
                                .clickable { freqMenuOpen = true }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("${profile.weeklyGoalDays} vezes/semana", style = MaterialTheme.typography.bodySmall, color = DarkInk)
                            Icon(Icons.Rounded.ExpandMore, null, tint = MutedInk, modifier = Modifier.size(16.dp))
                        }
                        DropdownMenu(expanded = freqMenuOpen, onDismissRequest = { freqMenuOpen = false }) {
                            listOf(2, 3, 4, 5, 6).forEach { days ->
                                DropdownMenuItem(
                                    text = { Text("$days vezes/semana") },
                                    onClick = {
                                        freqMenuOpen = false
                                        onUpdatePreferences(days, null, null, null)
                                    }
                                )
                            }
                        }
                    }
                }

                // Reminders Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Preferências de lembrete", style = MaterialTheme.typography.bodyMedium, color = DarkInk)
                        Text(
                            text = if (profile.remindersEnabled) "Notificações e coach por voz ativos" else "Lembretes silenciados",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedInk
                        )
                    }
                    Switch(
                        checked = profile.remindersEnabled,
                        onCheckedChange = { checked -> onUpdatePreferences(null, checked, null, null) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = TealPrimary)
                    )
                }

                // Measurement Units
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Unidades de medida", style = MaterialTheme.typography.bodyMedium, color = DarkInk)
                    Box {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, LightCardBorder, RoundedCornerShape(10.dp))
                                .clickable { unitMenuOpen = true }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(profile.unitSystem.label, style = MaterialTheme.typography.bodySmall, color = DarkInk)
                            Icon(Icons.Rounded.ExpandMore, null, tint = MutedInk, modifier = Modifier.size(16.dp))
                        }
                        DropdownMenu(expanded = unitMenuOpen, onDismissRequest = { unitMenuOpen = false }) {
                            MeasurementUnit.entries.forEach { u ->
                                DropdownMenuItem(
                                    text = { Text(u.label) },
                                    onClick = {
                                        unitMenuOpen = false
                                        onUpdatePreferences(null, null, u, null)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. Camera / Webcam Settings Card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(LightCardBg)
                    .border(1.dp, LightCardBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Configurações da Câmera",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = DarkInk
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, LightCardBorder, RoundedCornerShape(10.dp))
                                .clickable { camMenuOpen = true }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Rounded.Videocam, null, tint = TealPrimary, modifier = Modifier.size(18.dp))
                            Text(
                                text = if (profile.defaultFrontCamera) "Câmera Frontal (Selfie)" else "Câmera Padrão (Traseira)",
                                style = MaterialTheme.typography.bodySmall,
                                color = DarkInk
                            )
                            Icon(Icons.Rounded.ExpandMore, null, tint = MutedInk, modifier = Modifier.size(16.dp))
                        }
                        DropdownMenu(expanded = camMenuOpen, onDismissRequest = { camMenuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("Câmera Padrão (Traseira)") },
                                onClick = {
                                    camMenuOpen = false
                                    onUpdatePreferences(null, null, null, false)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Câmera Frontal (Selfie)") },
                                onClick = {
                                    camMenuOpen = false
                                    onUpdatePreferences(null, null, null, true)
                                }
                            )
                        }
                    }
                }
            }
        }

        // 6. Privacy, Data & InstantHMR Engine Subpage
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(LightCardBg)
                    .border(1.dp, LightCardBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Privacidade e dados",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = DarkInk
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, LightCardBorder, RoundedCornerShape(12.dp))
                        .clickable {
                            if (!isPro) {
                                onRequirePro("Exportação de Snapshots JSON")
                            } else if (snapshotCount > 0) {
                                snapJsonLauncher.launch("fitverso_snapshots.json")
                            }
                        }
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isPro) "Exportar dados ($snapshotCount snapshots)" else "Exportar dados · PRO",
                            style = MaterialTheme.typography.bodyMedium,
                            color = DarkInk
                        )
                        Text(
                            text = "Keypoints MHR70 e histórico em JSON",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedInk
                        )
                    }
                    Icon(Icons.Rounded.Download, null, tint = if (isPro) TealPrimary else GoldBone)
                }

                // Engine Settings Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(TealSoft.copy(alpha = 0.55f))
                        .border(1.dp, TealPrimary.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .clickable { showEngineSubpage = true }
                        .padding(14.dp)
                        .testTag("open_engine_settings_row"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Rounded.Hub, null, tint = TealPrimary, modifier = Modifier.size(22.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Motor Biométrico InstantHMR",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = DarkInk
                        )
                        Text(
                            text = if (isVariantCached) {
                                "${config.modelVariant.displayName} ativo · Pesos ONNX & filtros"
                            } else {
                                "Preview On-Device · Baixar ONNX & modo diagnóstico"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isVariantCached) MintSuccess else MutedInk
                        )
                    }
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = TealPrimary)
                }

                Button(
                    onClick = { showClearConfirm = true },
                    colors = ButtonDefaults.buttonColors(containerColor = StopCoral, contentColor = Color.White),
                    shape = RoundedCornerShape(24.dp),
                    contentPadding = PaddingValues(vertical = 13.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_clear_history_button")
                ) {
                    Text("Apagar histórico e estatísticas", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            containerColor = LightCardBg,
            title = { Text("Apagar histórico?", color = DarkInk) },
            text = { Text("Isso limpará todas as séries salvas e repetições registradas localmente neste dispositivo.", color = MutedInk) },
            confirmButton = {
                Button(
                    onClick = {
                        showClearConfirm = false
                        onClearAllHistory()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StopCoral, contentColor = Color.White)
                ) {
                    Text("Apagar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Cancelar", color = MutedInk)
                }
            }
        )
    }
}
