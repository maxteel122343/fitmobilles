package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.WorkoutHistoryEntry
import com.example.ui.theme.DarkInk
import com.example.ui.theme.LightCanvasBg
import com.example.ui.theme.LightCardBg
import com.example.ui.theme.LightCardBorder
import com.example.ui.theme.MutedInk
import com.example.ui.theme.TealMint
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.TealSoft

@Composable
fun HistoryScreen(
    history: List<WorkoutHistoryEntry>,
    onStartWorkout: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val periods = listOf("Todos os períodos", "Últimos 7 dias", "Últimos 30 dias")
    var selectedPeriod by remember { mutableStateOf(periods.first()) }
    var periodMenuOpen by remember { mutableStateOf(false) }

    val exerciseOptions = remember(history) {
        listOf("Todos os exercícios") + history.map { it.exerciseName }.distinct()
    }
    var selectedExercise by remember { mutableStateOf("Todos os exercícios") }
    var exerciseMenuOpen by remember { mutableStateOf(false) }

    var detailEntry by remember { mutableStateOf<WorkoutHistoryEntry?>(null) }

    val filtered = remember(history, selectedPeriod, selectedExercise) {
        val now = System.currentTimeMillis()
        val cutoff = when (selectedPeriod) {
            "Últimos 7 dias" -> now - 7L * 24 * 60 * 60 * 1000
            "Últimos 30 dias" -> now - 30L * 24 * 60 * 60 * 1000
            else -> 0L
        }
        history.filter { entry ->
            entry.timestampMs >= cutoff &&
                (selectedExercise == "Todos os exercícios" || entry.exerciseName == selectedExercise)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LightCanvasBg)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Filter Bar matching image 4 (Período + Exercício dropdowns)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Período", style = MaterialTheme.typography.labelMedium, color = DarkInk)
                Box {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(LightCardBg)
                            .border(1.dp, LightCardBorder, RoundedCornerShape(10.dp))
                            .clickable { periodMenuOpen = true }
                            .padding(horizontal = 12.dp, vertical = 11.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedPeriod,
                            style = MaterialTheme.typography.bodySmall,
                            color = DarkInk,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(Icons.Rounded.ExpandMore, null, tint = MutedInk, modifier = Modifier.size(18.dp))
                    }
                    DropdownMenu(expanded = periodMenuOpen, onDismissRequest = { periodMenuOpen = false }) {
                        periods.forEach { p ->
                            DropdownMenuItem(
                                text = { Text(p) },
                                onClick = {
                                    selectedPeriod = p
                                    periodMenuOpen = false
                                }
                            )
                        }
                    }
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Exercício", style = MaterialTheme.typography.labelMedium, color = DarkInk)
                Box {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(LightCardBg)
                            .border(1.dp, LightCardBorder, RoundedCornerShape(10.dp))
                            .clickable { exerciseMenuOpen = true }
                            .padding(horizontal = 12.dp, vertical = 11.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedExercise,
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedInk,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(Icons.Rounded.ExpandMore, null, tint = MutedInk, modifier = Modifier.size(18.dp))
                    }
                    DropdownMenu(expanded = exerciseMenuOpen, onDismissRequest = { exerciseMenuOpen = false }) {
                        exerciseOptions.forEach { ex ->
                            DropdownMenuItem(
                                text = { Text(ex) },
                                onClick = {
                                    selectedExercise = ex
                                    exerciseMenuOpen = false
                                }
                            )
                        }
                    }
                }
            }
        }

        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(LightCardBg)
                        .border(1.dp, LightCardBorder, RoundedCornerShape(18.dp))
                        .padding(24.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(TealSoft)
                    ) {
                        Icon(Icons.Rounded.History, null, tint = TealPrimary, modifier = Modifier.size(28.dp))
                    }
                    Text(
                        text = "Nenhum treino registrado ainda",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = DarkInk
                    )
                    Text(
                        text = "Finalize uma sessão em Treino ao vivo clicando em Encerrar para salvar suas repetições e tempo aqui.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedInk
                    )
                    Button(
                        onClick = { onStartWorkout("squat") },
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary, contentColor = Color.White),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.testTag("history_empty_start_button")
                    ) {
                        Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Iniciar primeiro treino")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                items(filtered, key = { it.id }) { entry ->
                    HistoryRowCard(
                        entry = entry,
                        onOpenDetails = { detailEntry = entry }
                    )
                }
            }
        }
    }

    val selected = detailEntry
    if (selected != null) {
        AlertDialog(
            onDismissRequest = { detailEntry = null },
            containerColor = LightCardBg,
            title = {
                Text(selected.exerciseName, color = DarkInk, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailLine("Data", selected.dateLabel)
                    DetailLine("Repetições", "${selected.reps} reps")
                    DetailLine("Duração", formatDuration(selected.durationSec))
                    DetailLine("Séries", "${selected.setsCompleted}")
                    DetailLine("Qualidade média da forma", "${selected.avgFormScorePct}%")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        detailEntry = null
                        onStartWorkout(selected.exerciseId)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary, contentColor = Color.White)
                ) {
                    Text("Treinar Novamente")
                }
            },
            dismissButton = {
                TextButton(onClick = { detailEntry = null }) {
                    Text("Fechar", color = MutedInk)
                }
            }
        )
    }
}

@Composable
private fun HistoryRowCard(
    entry: WorkoutHistoryEntry,
    onOpenDetails: () -> Unit
) {
    val mins = maxOf(1, entry.durationSec / 60)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(LightCardBg)
            .border(1.dp, LightCardBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${entry.dateLabel} - ${entry.exerciseName} - $mins min - ${entry.reps} Reps - Completo",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = DarkInk,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(end = 10.dp)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(TealMint)
            ) {
                Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
            }

            Surface(
                onClick = onOpenDetails,
                color = TealPrimary,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.testTag("history_details_${entry.id}")
            ) {
                Text(
                    text = "Detalhes",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                )
            }
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MutedInk)
        Text(value, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = DarkInk)
    }
}

private fun formatDuration(sec: Int): String {
    val m = sec / 60
    val s = sec % 60
    return String.format("%02d:%02d", m, s)
}
