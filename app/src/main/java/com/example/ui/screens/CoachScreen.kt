package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.CoachMessage
import com.example.domain.fitness.ExerciseCatalog
import com.example.domain.model.AiProvidersConfig
import com.example.domain.model.SubscriptionTier
import com.example.ui.StudioUiState
import com.example.ui.theme.DarkInk
import com.example.ui.theme.LightCanvasBg
import com.example.ui.theme.LightCardBg
import com.example.ui.theme.LightCardBorder
import com.example.ui.theme.MutedInk
import com.example.ui.theme.ProViolet
import com.example.ui.theme.StopCoral
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.TealSoft

@Composable
fun CoachScreen(
    uiState: StudioUiState,
    aiConfig: AiProvidersConfig,
    userTier: SubscriptionTier,
    isListening: Boolean,
    voiceStatus: String?,
    onOpenAiSettings: () -> Unit,
    onSendMessage: (String) -> Unit,
    onStartVoice: () -> Unit,
    onStopVoice: () -> Unit,
    onExportSnapshotsJson: (android.net.Uri) -> Unit,
    onExportSnapshotsCsv: (android.net.Uri) -> Unit,
    onClearSnapshots: () -> Unit,
    onRequirePro: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isPro = userTier == SubscriptionTier.PREMIUM
    var inputText by remember { mutableStateOf("") }
    var hasAudioPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val audioPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPerm = granted
        if (granted) onStartVoice()
    }

    val snapJsonLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let(onExportSnapshotsJson)
    }
    val snapCsvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let(onExportSnapshotsCsv)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LightCanvasBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Active Multi-Provider AI Config Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(LightCardBg)
                .border(1.dp, TealPrimary.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.AutoAwesome, null, tint = TealPrimary, modifier = Modifier.size(18.dp))
                    Text(
                        text = "Motores de IA Configurados",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = DarkInk
                    )
                }
                OutlinedButton(
                    onClick = onOpenAiSettings,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("coach_open_ai_settings")
                ) {
                    Icon(Icons.Rounded.Tune, null, tint = TealPrimary, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Configurar APIs", style = MaterialTheme.typography.labelSmall, color = TealPrimary)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ProviderPill("Texto: ${aiConfig.textProvider.displayName.substringBefore(" (")}", Modifier.weight(1f))
                ProviderPill("Voz: ${aiConfig.voiceProvider.displayName.substringBefore(" (")}", Modifier.weight(1f))
                ProviderPill("Img: ${aiConfig.imageProvider.displayName.substringBefore(" (")}", Modifier.weight(1f))
            }
        }

        // Snapshot Dataset Export Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(LightCardBg)
                .border(1.dp, LightCardBorder, RoundedCornerShape(16.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Histórico Biométrico (Snapshots)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = DarkInk
                    )
                    Text(
                        text = "${uiState.snapshotCount} registros · ${uiState.totalSessionReps} reps nesta sessão",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedInk
                    )
                }
                OutlinedButton(
                    onClick = onClearSnapshots,
                    enabled = uiState.snapshotCount > 0,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Rounded.DeleteSweep, null, modifier = Modifier.size(16.dp), tint = StopCoral)
                    Spacer(Modifier.width(4.dp))
                    Text("Limpar", style = MaterialTheme.typography.labelSmall, color = StopCoral)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        if (!isPro) {
                            onRequirePro("Exportação de Snapshots JSON/CSV")
                        } else {
                            snapJsonLauncher.launch("fitverso_snapshots.json")
                        }
                    },
                    enabled = uiState.snapshotCount > 0 || !isPro,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("export_snap_json_btn")
                ) {
                    Icon(
                        imageVector = if (isPro) Icons.Rounded.Download else Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = if (isPro) TealPrimary else ProViolet,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (isPro) "Exportar JSON" else "JSON · PRO",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isPro) DarkInk else ProViolet
                    )
                }
                OutlinedButton(
                    onClick = {
                        if (!isPro) {
                            onRequirePro("Exportação de Snapshots JSON/CSV")
                        } else {
                            snapCsvLauncher.launch("fitverso_snapshots.csv")
                        }
                    },
                    enabled = uiState.snapshotCount > 0 || !isPro,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("export_snap_csv_btn")
                ) {
                    Icon(
                        imageVector = if (isPro) Icons.Rounded.Download else Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = if (isPro) TealPrimary else ProViolet,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (isPro) "Exportar CSV" else "CSV · PRO",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isPro) DarkInk else ProViolet
                    )
                }
            }
        }

        // Quick prompts
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val quickPrompts = listOf(
                "Sugerir circuito dos ${ExerciseCatalog.ALL_EXERCISES.size}+ exercícios",
                "Analisar minha postura atual",
                "Como executar o agachamento perfeito?"
            )
            items(quickPrompts) { prompt ->
                AssistChip(
                    onClick = { onSendMessage(prompt) },
                    label = { Text(prompt, style = MaterialTheme.typography.labelSmall) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = LightCardBg,
                        labelColor = DarkInk
                    )
                )
            }
        }

        // Chat messages
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.coachMessages, key = { it.id }) { msg ->
                CoachBubble(msg)
            }
        }

        if (isListening || voiceStatus != null) {
            Text(
                text = voiceStatus ?: "Ouvindo comando de voz...",
                style = MaterialTheme.typography.labelSmall,
                color = TealPrimary
            )
        }

        // Voice + Text composer
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledIconButton(
                onClick = {
                    if (!isPro) {
                        onRequirePro("Conversação por Voz com Coach IA")
                    } else if (isListening) {
                        onStopVoice()
                    } else if (hasAudioPerm) {
                        onStartVoice()
                    } else {
                        audioPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = when {
                        isListening -> StopCoral
                        !isPro -> ProViolet.copy(alpha = 0.14f)
                        else -> TealSoft
                    },
                    contentColor = when {
                        isListening -> Color.White
                        !isPro -> ProViolet
                        else -> TealPrimary
                    }
                ),
                modifier = Modifier
                    .size(48.dp)
                    .testTag("coach_mic_button")
            ) {
                Icon(
                    imageVector = when {
                        isListening -> Icons.Rounded.Stop
                        !isPro -> Icons.Rounded.Lock
                        else -> Icons.Rounded.Mic
                    },
                    contentDescription = if (isListening) "Parar gravação" else "Falar com o coach"
                )
            }

            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text(if (isPro) "Pergunte ou use o microfone..." else "Pergunte ao Coach (Voz no Pro)", color = MutedInk) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = LightCardBg,
                    unfocusedContainerColor = LightCardBg,
                    focusedBorderColor = TealPrimary,
                    unfocusedBorderColor = LightCardBorder,
                    focusedTextColor = DarkInk,
                    unfocusedTextColor = DarkInk
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("coach_text_input")
            )

            FilledIconButton(
                onClick = {
                    val txt = inputText
                    inputText = ""
                    onSendMessage(txt)
                },
                enabled = inputText.isNotBlank() && !uiState.isCoachThinking,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = TealPrimary,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .size(48.dp)
                    .testTag("coach_send_button")
            ) {
                Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Enviar")
            }
        }
    }
}

@Composable
private fun ProviderPill(label: String, modifier: Modifier = Modifier) {
    Surface(
        color = TealSoft,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TealPrimary,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun CoachBubble(msg: CoachMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (msg.fromUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (msg.fromUser) TealSoft else LightCardBg,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .border(
                    1.dp,
                    if (msg.fromUser) TealPrimary.copy(alpha = 0.45f) else LightCardBorder,
                    RoundedCornerShape(14.dp)
                )
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (!msg.fromUser && !msg.providerTag.isNullOrBlank()) {
                    Text(
                        text = msg.providerTag,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = TealPrimary
                    )
                }
                Text(
                    text = msg.text,
                    style = MaterialTheme.typography.bodySmall,
                    color = DarkInk
                )
            }
        }
    }
}
