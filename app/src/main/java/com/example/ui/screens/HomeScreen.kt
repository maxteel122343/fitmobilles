package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.fitness.ExerciseCatalog
import com.example.domain.model.SubscriptionTier
import com.example.domain.model.UserFitnessProfile
import com.example.domain.model.WorkoutHistoryEntry
import com.example.ui.theme.DarkInk
import com.example.ui.theme.GoldBone
import com.example.ui.theme.LightCanvasBg
import com.example.ui.theme.LightCardBg
import com.example.ui.theme.LightCardBorder
import com.example.ui.theme.MutedInk
import com.example.ui.theme.ProViolet
import com.example.ui.theme.SidebarNavy
import com.example.ui.theme.TealMint
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.TealSoft

@Composable
fun HomeScreen(
    profile: UserFitnessProfile,
    sessionReps: Int,
    latestHistory: WorkoutHistoryEntry?,
    onStartQuickWorkout: () -> Unit,
    onStartExercise: (exerciseId: String, autoDetect: Boolean) -> Unit,
    onOpenProgress: () -> Unit,
    onOpenCoach: () -> Unit,
    onOpenPlans: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPro = profile.tier == SubscriptionTier.PREMIUM
    val weeklyGoal = profile.weeklyGoalDays.coerceAtLeast(1)
    val completedThisWeek = profile.workoutsCompleted.coerceAtMost(weeklyGoal)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(LightCanvasBg),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // 1. Personalized Greeting
        item {
            Text(
                text = "Olá, ${profile.userName}!",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = DarkInk
            )
        }

        // 2. Weekly Summary White Card (matches image 2)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(LightCardBg)
                    .border(1.dp, LightCardBorder, RoundedCornerShape(16.dp))
                    .clickable { onOpenProgress() }
                    .padding(18.dp)
                    .testTag("home_weekly_card"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Resumo semanal",
                        style = MaterialTheme.typography.labelMedium,
                        color = MutedInk
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        Column {
                            Text(
                                text = "$completedThisWeek/$weeklyGoal treinos",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = DarkInk
                            )
                            Text(
                                text = "Meta semanal concluída",
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedInk
                            )
                        }
                        Column {
                            Text(
                                text = "Sequência de ${profile.streakDays} dias",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = DarkInk
                            )
                            Text(
                                text = "${profile.totalLifetimeReps + sessionReps} reps totais",
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedInk
                            )
                        }
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = "Ver progresso",
                    tint = MutedInk
                )
            }
        }

        // 3. Full-width Teal CTA: "Começar treino rápido"
        item {
            Button(
                onClick = onStartQuickWorkout,
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary, contentColor = Color.White),
                shape = RoundedCornerShape(28.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_start_live_button")
            ) {
                Text(
                    text = "Começar treino rápido",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }

        // 4. Adaptive 2-Column Section: "Sua recomendação para hoje" & "Último treino"
        item {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val twoCol = maxWidth >= 560.dp
                if (twoCol) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        RecommendationCard(
                            onStart = { onStartExercise("squat", false) },
                            modifier = Modifier.weight(1.25f)
                        )
                        LastWorkoutCard(
                            latest = latestHistory,
                            onStartAuto = { onStartExercise("squat", true) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        RecommendationCard(
                            onStart = { onStartExercise("squat", false) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        LastWorkoutCard(
                            latest = latestHistory,
                            onStartAuto = { onStartExercise("squat", true) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // 5. Subscription Tier Card (Plano Básico vs FitVerso Pro + Coach quick link)
        item {
            if (!isPro) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(SidebarNavy, Color(0xFF233547))
                            )
                        )
                        .border(1.dp, GoldBone.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
                        .clickable { onOpenPlans() }
                        .padding(18.dp)
                        .testTag("home_pro_banner")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Icon(Icons.Rounded.WorkspacePremium, null, tint = GoldBone, modifier = Modifier.size(28.dp))
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Você está no Plano Básico · Evolua para o FitVerso Pro",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                text = "Desbloqueie ${ExerciseCatalog.ALL_EXERCISES.size}+ exercícios, Coach IA (Gemini/DeepSeek/ChatGPT) por voz e exportação JSON/CSV.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFCBD5E1)
                            )
                        }
                        Surface(
                            color = GoldBone,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Ver Planos",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = SidebarNavy,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(TealSoft)
                        .border(1.dp, TealPrimary.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                        .clickable { onOpenCoach() }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Rounded.AutoAwesome, null, tint = ProViolet, modifier = Modifier.size(22.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "FitVerso Pro Ativo · Coach IA & Voz Liberados",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = DarkInk
                        )
                        Text(
                            text = "Todos os ${ExerciseCatalog.ALL_EXERCISES.size}+ exercícios com IA que ensina e exportação desbloqueados.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedInk
                        )
                    }
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = TealPrimary)
                }
            }
        }
    }
}

@Composable
private fun RecommendationCard(
    onStart: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Sua recomendação para hoje",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = DarkInk
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(LightCardBg)
                .border(1.dp, LightCardBorder, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Treino de Pernas - Iniciante",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = DarkInk
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Rounded.Schedule, null, tint = MutedInk, modifier = Modifier.size(16.dp))
                    Text("20 min", style = MaterialTheme.typography.bodySmall, color = MutedInk)
                }
                Button(
                    onClick = onStart,
                    colors = ButtonDefaults.buttonColors(containerColor = SidebarNavy, contentColor = Color.White),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Text("Começar", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun LastWorkoutCard(
    latest: WorkoutHistoryEntry?,
    onStartAuto: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Último treino",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = DarkInk
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(LightCardBg)
                .border(1.dp, LightCardBorder, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (latest != null) {
                Text(
                    text = latest.dateLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedInk
                )
                Text(
                    text = "${latest.exerciseName} - ${maxOf(1, latest.durationSec / 60)} min",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = DarkInk
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    LastStatItem(value = "${latest.reps}", label = "Reps")
                    LastStatItem(value = "${latest.setsCompleted}", label = "Séries")
                    LastStatItem(value = "${latest.avgFormScorePct}%", label = "Forma", accent = TealMint)
                }
            } else {
                Text(
                    text = "Nenhuma sessão salva ainda",
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedInk
                )
                Text(
                    text = "Detecção Automática com Câmera",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = DarkInk
                )
                Button(
                    onClick = onStartAuto,
                    colors = ButtonDefaults.buttonColors(containerColor = TealSoft, contentColor = TealPrimary),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("home_auto_detect_button")
                ) {
                    Icon(Icons.Rounded.AutoAwesome, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Modo Auto", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun LastStatItem(
    value: String,
    label: String,
    accent: Color = DarkInk
) {
    Column {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = accent
        )
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MutedInk)
    }
}
