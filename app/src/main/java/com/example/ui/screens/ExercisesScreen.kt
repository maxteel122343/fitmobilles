package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.SportsGymnastics
import androidx.compose.material.icons.rounded.SportsMartialArts
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.domain.fitness.ExerciseCatalog
import com.example.domain.fitness.ExerciseCategory
import com.example.domain.fitness.ExerciseDefinition
import com.example.domain.model.AiProvidersConfig
import com.example.domain.model.SubscriptionTier
import com.example.ui.ExerciseTeachUiState
import com.example.ui.theme.DarkInk
import com.example.ui.theme.GoldBone
import com.example.ui.theme.LightCanvasBg
import com.example.ui.theme.LightCardBg
import com.example.ui.theme.LightCardBorder
import com.example.ui.theme.MutedInk
import com.example.ui.theme.ProViolet
import com.example.ui.theme.StopCoral
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.TealSoft

@Composable
fun ExercisesScreen(
    selectedExerciseId: String,
    userTier: SubscriptionTier,
    aiConfig: AiProvidersConfig,
    teachState: ExerciseTeachUiState,
    onOpenExerciseTeach: (ExerciseDefinition) -> Unit,
    onTeachExerciseAi: (ExerciseDefinition) -> Unit,
    onGenerateExerciseVisual: (ExerciseDefinition) -> Unit,
    onSpeakExerciseGuide: (String) -> Unit,
    onStartExercise: (String) -> Unit,
    onRequirePro: (String) -> Unit,
    onOpenPlans: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPro = userTier == SubscriptionTier.PREMIUM
    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<ExerciseCategory?>(null) }
    var selectedLevel by remember { mutableStateOf("Todos os níveis") }

    var muscleMenuOpen by remember { mutableStateOf(false) }
    var levelMenuOpen by remember { mutableStateOf(false) }
    var detailExercise by remember { mutableStateOf<ExerciseDefinition?>(null) }

    val levelOptions = listOf("Todos os níveis", "Iniciante", "Intermediário", "Avançado")

    val filtered = remember(query, selectedCategory, selectedLevel) {
        ExerciseCatalog.ALL_EXERCISES.filter { ex ->
            val catOk = selectedCategory == null || ex.category == selectedCategory
            val lvlOk = selectedLevel == "Todos os níveis" || ex.difficulty == selectedLevel
            val q = query.trim().lowercase()
            val textOk = q.isEmpty() ||
                ex.namePt.lowercase().contains(q) ||
                ex.nameEn.lowercase().contains(q) ||
                ex.targetMuscles.lowercase().contains(q) ||
                ex.equipment.lowercase().contains(q)
            catOk && lvlOk && textOk
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LightCanvasBg)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Search + Grupo muscular + Nível Filter Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Buscar entre ${ExerciseCatalog.ALL_EXERCISES.size}+ exercícios...", color = MutedInk, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = MutedInk) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = LightCardBg,
                    unfocusedContainerColor = LightCardBg,
                    focusedBorderColor = TealPrimary,
                    unfocusedBorderColor = LightCardBorder,
                    focusedTextColor = DarkInk,
                    unfocusedTextColor = DarkInk
                ),
                modifier = Modifier
                    .weight(1.3f)
                    .testTag("exercise_search_input")
            )

            Box {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(LightCardBg)
                        .border(1.dp, LightCardBorder, RoundedCornerShape(12.dp))
                        .clickable { muscleMenuOpen = true }
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = selectedCategory?.label ?: "Grupo",
                        style = MaterialTheme.typography.bodySmall,
                        color = DarkInk,
                        maxLines = 1
                    )
                    Icon(Icons.Rounded.ExpandMore, null, tint = MutedInk, modifier = Modifier.size(16.dp))
                }
                DropdownMenu(expanded = muscleMenuOpen, onDismissRequest = { muscleMenuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Todos (${ExerciseCatalog.ALL_EXERCISES.size})") },
                        onClick = {
                            selectedCategory = null
                            muscleMenuOpen = false
                        }
                    )
                    ExerciseCategory.entries.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat.label) },
                            onClick = {
                                selectedCategory = cat
                                muscleMenuOpen = false
                            }
                        )
                    }
                }
            }

            Box {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(LightCardBg)
                        .border(1.dp, LightCardBorder, RoundedCornerShape(12.dp))
                        .clickable { levelMenuOpen = true }
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (selectedLevel == "Todos os níveis") "Nível" else selectedLevel,
                        style = MaterialTheme.typography.bodySmall,
                        color = DarkInk,
                        maxLines = 1
                    )
                    Icon(Icons.Rounded.ExpandMore, null, tint = MutedInk, modifier = Modifier.size(16.dp))
                }
                DropdownMenu(expanded = levelMenuOpen, onDismissRequest = { levelMenuOpen = false }) {
                    levelOptions.forEach { lvl ->
                        DropdownMenuItem(
                            text = { Text(lvl) },
                            onClick = {
                                selectedLevel = lvl
                                levelMenuOpen = false
                            }
                        )
                    }
                }
            }
        }

        // Subtle plan & catalog count strip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${filtered.size} de ${ExerciseCatalog.ALL_EXERCISES.size}+ exercícios · ${if (isPro) "Todos liberados (Pro)" else "${ExerciseCatalog.BASIC_INCLUDED_IDS.size} grátis no Básico"}",
                style = MaterialTheme.typography.bodySmall,
                color = MutedInk
            )
            if (!isPro) {
                Text(
                    text = "Liberar ${ExerciseCatalog.ALL_EXERCISES.size}+ no Pro →",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = TealPrimary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onOpenPlans() }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        // 2. Adaptive Multi-Column Exercise Grid
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 240.dp),
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 14.dp)
        ) {
            items(filtered, key = { it.id }) { ex ->
                val isIncludedBasic = ExerciseCatalog.isIncludedInBasic(ex.id)
                val isLocked = !isPro && !isIncludedBasic
                ExerciseCatalogGridCard(
                    exercise = ex,
                    isActive = ex.id == selectedExerciseId,
                    isIncludedBasic = isIncludedBasic,
                    isLocked = isLocked,
                    onDetails = {
                        onOpenExerciseTeach(ex)
                        detailExercise = ex
                    },
                    onTrain = {
                        if (isLocked) {
                            onRequirePro(ex.namePt)
                        } else {
                            onStartExercise(ex.id)
                        }
                    }
                )
            }
        }
    }

    val inspecting = detailExercise
    if (inspecting != null) {
        val isIncluded = ExerciseCatalog.isIncludedInBasic(inspecting.id)
        val locked = !isPro && !isIncluded
        val currentTeach = if (teachState.exerciseId == inspecting.id) teachState else ExerciseTeachUiState(exerciseId = inspecting.id)

        ExerciseTeachDialog(
            exercise = inspecting,
            locked = locked,
            aiConfig = aiConfig,
            teachState = currentTeach,
            onTeachAi = { onTeachExerciseAi(inspecting) },
            onGenerateImage = { onGenerateExerciseVisual(inspecting) },
            onSpeak = {
                val speechText = currentTeach.explanation?.takeIf { it.isNotBlank() }
                    ?: "${inspecting.namePt}. ${inspecting.resolvedSteps().joinToString(" ")} Dica principal: ${inspecting.primaryCue}"
                onSpeakExerciseGuide(speechText)
            },
            onStartTrain = {
                detailExercise = null
                if (locked) onRequirePro(inspecting.namePt) else onStartExercise(inspecting.id)
            },
            onDismiss = { detailExercise = null }
        )
    }
}

@Composable
private fun ExerciseTeachDialog(
    exercise: ExerciseDefinition,
    locked: Boolean,
    aiConfig: AiProvidersConfig,
    teachState: ExerciseTeachUiState,
    onTeachAi: () -> Unit,
    onGenerateImage: () -> Unit,
    onSpeak: () -> Unit,
    onStartTrain: () -> Unit,
    onDismiss: () -> Unit
) {
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
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LightCanvasBg)
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exercise.namePt,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = DarkInk
                    )
                    Text(
                        text = "${exercise.targetMuscles} · ${exercise.equipment} · ${exercise.difficulty}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedInk,
                        maxLines = 2
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Fechar", tint = MutedInk)
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Angle target pill
                Surface(
                    color = TealSoft,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Ângulo biomecânico alvo na câmera: ${exercise.downThresholdDeg.toInt()}° ↔ ${exercise.upThresholdDeg.toInt()}°",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = TealPrimary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }

                // AI Action Buttons: Teach Text + Generate Image + Speak
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onTeachAi,
                        enabled = !teachState.isLoadingText,
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary, contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("teach_ai_explain_btn")
                    ) {
                        if (teachState.isLoadingText) {
                            CircularProgressIndicator(modifier = Modifier.size(15.dp), strokeWidth = 2.dp, color = Color.White)
                        } else {
                            Icon(Icons.Rounded.AutoAwesome, null, modifier = Modifier.size(16.dp))
                        }
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "IA Ensina (${aiConfig.textProvider.displayName.substringBefore(" ")})",
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    OutlinedButton(
                        onClick = onGenerateImage,
                        enabled = !teachState.isLoadingImage,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("teach_ai_image_btn")
                    ) {
                        if (teachState.isLoadingImage) {
                            CircularProgressIndicator(modifier = Modifier.size(15.dp), strokeWidth = 2.dp, color = TealPrimary)
                        } else {
                            Icon(Icons.Rounded.Image, null, tint = TealPrimary, modifier = Modifier.size(16.dp))
                        }
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Ilustrar (${aiConfig.imageProvider.displayName.substringBefore(" ")})",
                            style = MaterialTheme.typography.labelSmall,
                            color = DarkInk,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                OutlinedButton(
                    onClick = onSpeak,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("teach_ai_speak_btn")
                ) {
                    Icon(Icons.Rounded.VolumeUp, null, tint = TealPrimary, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Ouvir Explicação (${aiConfig.voiceProvider.displayName})",
                        style = MaterialTheme.typography.labelMedium,
                        color = TealPrimary
                    )
                }

                // Generated Image Preview
                val bmp = teachState.generatedImage
                if (bmp != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF0E1722))
                            .border(1.dp, LightCardBorder, RoundedCornerShape(14.dp))
                    ) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Ilustração de postura ${exercise.namePt}",
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                        )
                        teachState.imageSourceLabel?.let { lbl ->
                            Text(
                                text = lbl,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                teachState.errorMessage?.let { err ->
                    Text(
                        text = err,
                        style = MaterialTheme.typography.bodySmall,
                        color = StopCoral
                    )
                }

                // Explanation Text Box
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(LightCanvasBg)
                        .border(1.dp, LightCardBorder, RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = teachState.textSourceLabel ?: "Passo a passo biomecânico",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = TealPrimary
                    )
                    Text(
                        text = teachState.explanation ?: exercise.resolvedSteps().joinToString("\n"),
                        style = MaterialTheme.typography.bodySmall,
                        color = DarkInk
                    )
                }
            }

            // Bottom Footer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LightCanvasBg)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Fechar", color = MutedInk)
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = onStartTrain,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (locked) GoldBone else TealPrimary,
                        contentColor = if (locked) DarkInk else Color.White
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(if (locked) "Desbloquear no Pro" else "Treinar agora")
                }
            }
        }
    }
}

@Composable
private fun ExerciseCatalogGridCard(
    exercise: ExerciseDefinition,
    isActive: Boolean,
    isIncludedBasic: Boolean,
    isLocked: Boolean,
    onDetails: () -> Unit,
    onTrain: () -> Unit
) {
    val categoryIcon = when (exercise.category) {
        ExerciseCategory.LOWER_BODY -> Icons.Rounded.DirectionsRun
        ExerciseCategory.CORE_MOBILITY -> Icons.Rounded.SelfImprovement
        ExerciseCategory.MOBILITY_YOGA -> Icons.Rounded.AccessibilityNew
        ExerciseCategory.FUNCTIONAL_KETTLEBELL -> Icons.Rounded.SportsMartialArts
        ExerciseCategory.FULL_BODY -> Icons.Rounded.SportsGymnastics
        else -> Icons.Rounded.FitnessCenter
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(LightCardBg)
            .border(
                width = if (isActive) 1.5.dp else 1.dp,
                color = if (isActive) TealPrimary else LightCardBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Top row: Square Exercise Icon + Plan Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isLocked) ProViolet.copy(alpha = 0.09f) else TealSoft)
            ) {
                Icon(
                    imageVector = categoryIcon,
                    contentDescription = exercise.namePt,
                    tint = if (isLocked) ProViolet else TealPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Surface(
                color = if (isIncludedBasic) TealSoft else ProViolet.copy(alpha = 0.12f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (isIncludedBasic) "GRÁTIS" else "PRO",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (isIncludedBasic) TealPrimary else ProViolet,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = exercise.namePt,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = DarkInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = exercise.targetMuscles.substringBefore(",").trim(),
                style = MaterialTheme.typography.bodySmall,
                color = MutedInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${exercise.difficulty} · ${exercise.equipment}",
                style = MaterialTheme.typography.bodySmall,
                color = MutedInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Bottom CTA row: [IA Ensina] [Treinar]
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onDetails,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, DarkInk.copy(alpha = 0.55f)),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("details_exercise_${exercise.id}")
            ) {
                Text("IA Ensina", style = MaterialTheme.typography.labelSmall, color = DarkInk, maxLines = 1)
            }

            Button(
                onClick = onTrain,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isLocked) Color(0xFF475569) else TealPrimary,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag(if (isLocked) "unlock_exercise_${exercise.id}" else "start_exercise_${exercise.id}")
            ) {
                if (isLocked) {
                    Icon(Icons.Rounded.Lock, null, tint = GoldBone, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Pro", style = MaterialTheme.typography.labelSmall, color = GoldBone, maxLines = 1)
                } else {
                    Text("Treinar", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
            }
        }
    }
}
