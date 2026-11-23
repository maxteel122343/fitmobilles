package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.domain.model.AiProvidersConfig
import com.example.domain.model.ImageAiProvider
import com.example.domain.model.TextAiProvider
import com.example.domain.model.VoiceAiProvider
import com.example.ui.theme.DarkInk
import com.example.ui.theme.LightCanvasBg
import com.example.ui.theme.LightCardBg
import com.example.ui.theme.LightCardBorder
import com.example.ui.theme.MutedInk
import com.example.ui.theme.StopCoral
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.TealSoft

@Composable
fun AiProviderSettingsDialog(
    currentConfig: AiProvidersConfig,
    onSave: (AiProvidersConfig) -> Unit,
    onTestVoice: (AiProvidersConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var draft by remember(currentConfig) { mutableStateOf(currentConfig) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var showTextKey by remember { mutableStateOf(false) }
    var showVoiceKey by remember { mutableStateOf(false) }
    var showImageKey by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 680.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(LightCardBg)
                .border(1.dp, LightCardBorder, RoundedCornerShape(22.dp))
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LightCanvasBg)
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(color = TealSoft, shape = RoundedCornerShape(10.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = TealPrimary,
                            modifier = Modifier.padding(8.dp).size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Modelos de IA & APIs (BYOK)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = DarkInk
                        )
                        Text(
                            text = "Configure provedores separados para Texto, Voz e Imagem",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedInk
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("ai_settings_close")
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Fechar", tint = MutedInk)
                }
            }

            // 3 Tabs: Texto | Voz | Imagem
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = LightCardBg,
                contentColor = TealPrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = TealPrimary
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Texto") },
                    icon = { Icon(Icons.Rounded.Chat, null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("ai_tab_text")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Voz") },
                    icon = { Icon(Icons.Rounded.RecordVoiceOver, null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("ai_tab_voice")
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Imagem") },
                    icon = { Icon(Icons.Rounded.Image, null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("ai_tab_image")
                )
            }

            // Tab body
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (selectedTab) {
                    0 -> TextProviderTab(
                        draft = draft,
                        showKey = showTextKey,
                        onToggleShowKey = { showTextKey = !showTextKey },
                        onChange = { draft = it }
                    )
                    1 -> VoiceProviderTab(
                        draft = draft,
                        showKey = showVoiceKey,
                        onToggleShowKey = { showVoiceKey = !showVoiceKey },
                        onChange = { draft = it },
                        onTestVoice = { onTestVoice(draft) }
                    )
                    else -> ImageProviderTab(
                        draft = draft,
                        showKey = showImageKey,
                        onToggleShowKey = { showImageKey = !showImageKey },
                        onChange = { draft = it }
                    )
                }

                Surface(
                    color = LightCanvasBg,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().border(1.dp, LightCardBorder, RoundedCornerShape(12.dp))
                ) {
                    Text(
                        text = "Suas chaves ficam salvas apenas localmente neste dispositivo e são enviadas diretamente ao provedor selecionado.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedInk,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Footer actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LightCanvasBg)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { draft = AiProvidersConfig() },
                    modifier = Modifier.testTag("ai_settings_clear")
                ) {
                    Text("Limpar Chaves", color = StopCoral)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar", color = MutedInk)
                    }
                    Button(
                        onClick = { onSave(draft) },
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary, contentColor = Color.White),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                        modifier = Modifier.testTag("ai_settings_save")
                    ) {
                        Text("Salvar APIs", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun TextProviderTab(
    draft: AiProvidersConfig,
    showKey: Boolean,
    onToggleShowKey: () -> Unit,
    onChange: (AiProvidersConfig) -> Unit
) {
    Text(
        text = "Provedor de Texto (Coach IA & Explicação de Exercícios)",
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        color = DarkInk
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TextAiProvider.entries.forEach { provider ->
            val selected = draft.textProvider == provider
            ProviderOptionRow(
                title = provider.displayName,
                subtitle = if (provider.requiresKey) "Modelo padrão: ${provider.defaultModel}" else "Sem chave de API necessária",
                selected = selected,
                onClick = {
                    onChange(
                        draft.copy(
                            textProvider = provider,
                            textModel = provider.defaultModel,
                            textBaseUrl = provider.defaultBaseUrl.ifBlank { draft.textBaseUrl }
                        )
                    )
                },
                tag = "text_provider_${provider.id}"
            )
        }
    }

    if (draft.textProvider != TextAiProvider.OFFLINE_COACH) {
        SuggestedChipRow(
            label = "Modelos sugeridos",
            items = draft.textProvider.suggestedModels,
            activeItem = draft.textModel,
            onSelect = { model -> onChange(draft.copy(textModel = model)) }
        )

        LightField(
            value = draft.textModel,
            onValueChange = { onChange(draft.copy(textModel = it)) },
            label = "ID do Modelo de Texto",
            placeholder = draft.textProvider.defaultModel,
            tag = "text_model_input"
        )

        if (draft.textProvider == TextAiProvider.CUSTOM) {
            LightField(
                value = draft.textBaseUrl,
                onValueChange = { onChange(draft.copy(textBaseUrl = it)) },
                label = "Endpoint URL (Chat Completions)",
                placeholder = "https://openrouter.ai/api/v1/chat/completions",
                tag = "text_baseurl_input"
            )
        }

        ApiKeyField(
            value = draft.textApiKey,
            onValueChange = { onChange(draft.copy(textApiKey = it)) },
            label = "API Key de Texto (${draft.textProvider.displayName})",
            showKey = showKey,
            onToggleShowKey = onToggleShowKey,
            tag = "text_apikey_input"
        )
    }
}

@Composable
private fun VoiceProviderTab(
    draft: AiProvidersConfig,
    showKey: Boolean,
    onToggleShowKey: () -> Unit,
    onChange: (AiProvidersConfig) -> Unit,
    onTestVoice: () -> Unit
) {
    Text(
        text = "API de Voz (Narração das Dicas & Explicação por Áudio)",
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        color = DarkInk
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        VoiceAiProvider.entries.forEach { provider ->
            val selected = draft.voiceProvider == provider
            ProviderOptionRow(
                title = provider.displayName,
                subtitle = "Voz padrão: ${provider.defaultVoiceId}",
                selected = selected,
                onClick = {
                    onChange(
                        draft.copy(
                            voiceProvider = provider,
                            voiceModel = provider.defaultModel,
                            voiceId = provider.defaultVoiceId,
                            voiceBaseUrl = provider.defaultBaseUrl.ifBlank { draft.voiceBaseUrl }
                        )
                    )
                },
                tag = "voice_provider_${provider.id}"
            )
        }
    }

    if (draft.voiceProvider.requiresKey) {
        SuggestedChipRow(
            label = "Modelos de Voz",
            items = draft.voiceProvider.suggestedModels,
            activeItem = draft.voiceModel,
            onSelect = { model -> onChange(draft.copy(voiceModel = model)) }
        )

        SuggestedChipRow(
            label = "Voice IDs",
            items = draft.voiceProvider.suggestedVoices,
            activeItem = draft.voiceId,
            onSelect = { vid -> onChange(draft.copy(voiceId = vid)) }
        )

        LightField(
            value = draft.voiceModel,
            onValueChange = { onChange(draft.copy(voiceModel = it)) },
            label = "Modelo de Voz",
            placeholder = draft.voiceProvider.defaultModel,
            tag = "voice_model_input"
        )

        LightField(
            value = draft.voiceId,
            onValueChange = { onChange(draft.copy(voiceId = it)) },
            label = "Voice ID / Locutor",
            placeholder = draft.voiceProvider.defaultVoiceId,
            tag = "voice_id_input"
        )

        ApiKeyField(
            value = draft.voiceApiKey,
            onValueChange = { onChange(draft.copy(voiceApiKey = it)) },
            label = "API Key de Voz (${draft.voiceProvider.displayName})",
            showKey = showKey,
            onToggleShowKey = onToggleShowKey,
            tag = "voice_apikey_input"
        )
    }

    OutlinedButton(
        onClick = onTestVoice,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().testTag("test_voice_api_button")
    ) {
        Icon(Icons.Rounded.VolumeUp, null, tint = TealPrimary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("Testar Voz Agora", color = TealPrimary, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ImageProviderTab(
    draft: AiProvidersConfig,
    showKey: Boolean,
    onToggleShowKey: () -> Unit,
    onChange: (AiProvidersConfig) -> Unit
) {
    Text(
        text = "API de Geração de Imagem (Ilustrações Biomecânicas)",
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        color = DarkInk
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ImageAiProvider.entries.forEach { provider ->
            val selected = draft.imageProvider == provider
            ProviderOptionRow(
                title = provider.displayName,
                subtitle = if (provider.requiresKey) "Modelo padrão: ${provider.defaultModel}" else "Diagrama instantâneo no aparelho",
                selected = selected,
                onClick = {
                    onChange(
                        draft.copy(
                            imageProvider = provider,
                            imageModel = provider.defaultModel,
                            imageBaseUrl = provider.defaultBaseUrl.ifBlank { draft.imageBaseUrl }
                        )
                    )
                },
                tag = "image_provider_${provider.id}"
            )
        }
    }

    if (draft.imageProvider.requiresKey) {
        SuggestedChipRow(
            label = "Modelos de Imagem",
            items = draft.imageProvider.suggestedModels,
            activeItem = draft.imageModel,
            onSelect = { model -> onChange(draft.copy(imageModel = model)) }
        )

        LightField(
            value = draft.imageModel,
            onValueChange = { onChange(draft.copy(imageModel = it)) },
            label = "Modelo de Imagem",
            placeholder = draft.imageProvider.defaultModel,
            tag = "image_model_input"
        )

        if (draft.imageProvider == ImageAiProvider.CUSTOM_IMAGE) {
            LightField(
                value = draft.imageBaseUrl,
                onValueChange = { onChange(draft.copy(imageBaseUrl = it)) },
                label = "Endpoint URL de Imagem",
                placeholder = "https://api.together.xyz/v1/images/generations",
                tag = "image_baseurl_input"
            )
        }

        ApiKeyField(
            value = draft.imageApiKey,
            onValueChange = { onChange(draft.copy(imageApiKey = it)) },
            label = "API Key de Imagem (${draft.imageProvider.displayName})",
            showKey = showKey,
            onToggleShowKey = onToggleShowKey,
            tag = "image_apikey_input"
        )
    }
}

@Composable
private fun ProviderOptionRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) TealSoft else LightCanvasBg)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) TealPrimary else LightCardBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (selected) TealPrimary else DarkInk
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MutedInk
            )
        }
        Surface(
            color = if (selected) TealPrimary else LightCardBg,
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = if (selected) "Ativo" else "Selecionar",
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) Color.White else MutedInk,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun SuggestedChipRow(
    label: String,
    items: List<String>,
    activeItem: String,
    onSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MutedInk)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items.forEach { item ->
                FilterChip(
                    selected = item == activeItem,
                    onClick = { onSelect(item) },
                    label = { Text(item, style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = TealPrimary,
                        selectedLabelColor = Color.White,
                        containerColor = LightCanvasBg,
                        labelColor = DarkInk
                    )
                )
            }
        }
    }
}

@Composable
private fun LightField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    tag: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder, color = MutedInk) },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = TealPrimary,
            unfocusedBorderColor = LightCardBorder,
            focusedTextColor = DarkInk,
            unfocusedTextColor = DarkInk
        ),
        modifier = Modifier.fillMaxWidth().testTag(tag)
    )
}

@Composable
private fun ApiKeyField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    showKey: Boolean,
    onToggleShowKey: () -> Unit,
    tag: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text("Cole sua API Key aqui...", color = MutedInk) },
        leadingIcon = { Icon(Icons.Rounded.Key, null, tint = TealPrimary, modifier = Modifier.size(18.dp)) },
        trailingIcon = {
            IconButton(onClick = onToggleShowKey) {
                Icon(
                    imageVector = if (showKey) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                    contentDescription = if (showKey) "Ocultar chave" else "Mostrar chave",
                    tint = MutedInk
                )
            }
        },
        visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = TealPrimary,
            unfocusedBorderColor = LightCardBorder,
            focusedTextColor = DarkInk,
            unfocusedTextColor = DarkInk
        ),
        modifier = Modifier.fillMaxWidth().testTag(tag)
    )
}
