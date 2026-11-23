package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.UserFitnessProfile
import com.example.domain.model.WorkoutHistoryEntry
import com.example.ui.theme.DarkInk
import com.example.ui.theme.LightCanvasBg
import com.example.ui.theme.LightCardBg
import com.example.ui.theme.LightCardBorder
import com.example.ui.theme.MutedInk
import com.example.ui.theme.TealMint
import com.example.ui.theme.TealPrimary
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun ProgressScreen(
    profile: UserFitnessProfile,
    history: List<WorkoutHistoryEntry>,
    modifier: Modifier = Modifier
) {
    val ranges = listOf("Semana", "Mês", "3 Meses")
    var selectedRange by remember { mutableIntStateOf(1) }

    // Aggregate real sessions into 7 buckets for visualization
    val bucketValues = remember(history, selectedRange) {
        val buckets = IntArray(7)
        val repBuckets = IntArray(7)
        val now = System.currentTimeMillis()
        val spanDays = when (selectedRange) {
            0 -> 7
            1 -> 28
            else -> 84
        }
        val bucketMs = (spanDays.toLong() * 24 * 60 * 60 * 1000) / 7L
        for (entry in history) {
            val age = (now - entry.timestampMs).coerceAtLeast(0L)
            val idxFromEnd = (age / max(1L, bucketMs)).toInt()
            if (idxFromEnd in 0..6) {
                val bucketIdx = 6 - idxFromEnd
                buckets[bucketIdx] += 1
                repBuckets[bucketIdx] += entry.reps
            }
        }
        buckets to repBuckets
    }

    val workoutsPerBucket = bucketValues.first
    val repsPerBucket = bucketValues.second

    val cumulativeReps = remember(repsPerBucket) {
        val out = IntArray(repsPerBucket.size)
        var acc = 0
        for (i in repsPerBucket.indices) {
            acc += repsPerBucket[i]
            out[i] = acc
        }
        out
    }

    val consistencyPct = remember(profile.workoutsCompleted, profile.weeklyGoalDays) {
        val goal = profile.weeklyGoalDays.coerceAtLeast(1)
        ((profile.workoutsCompleted.toFloat() / goal) * 100f).roundToInt().coerceIn(0, 100)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(LightCanvasBg),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Timeframe selector pills matching image 5
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ranges.forEachIndexed { idx, label ->
                        FilterChip(
                            selected = selectedRange == idx,
                            onClick = { selectedRange = idx },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TealPrimary,
                                selectedLabelColor = Color.White,
                                containerColor = LightCardBg,
                                labelColor = DarkInk
                            ),
                            modifier = Modifier.testTag("progress_range_$idx")
                        )
                    }
                }
            }
        }

        // Card 1: Treinos por semana (Bar Chart)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(LightCardBg)
                    .border(1.dp, LightCardBorder, RoundedCornerShape(16.dp))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Treinos por período",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = DarkInk
                )

                val maxCount = max(4, workoutsPerBucket.maxOrNull() ?: 4)
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(148.dp)
                ) {
                    val slotW = size.width / workoutsPerBucket.size
                    val barW = (slotW * 0.56f).coerceAtMost(56.dp.toPx())
                    val topPad = 14.dp.toPx()
                    val chartH = size.height - topPad - 8.dp.toPx()

                    // Subtle horizontal grid lines
                    for (g in 0..3) {
                        val gy = topPad + chartH * (g / 3f)
                        drawLine(
                            color = LightCardBorder,
                            start = Offset(0f, gy),
                            end = Offset(size.width, gy),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    workoutsPerBucket.forEachIndexed { idx, value ->
                        val frac = value.toFloat() / maxCount.toFloat()
                        val bh = (chartH * frac).coerceAtLeast(if (value > 0) 10.dp.toPx() else 4.dp.toPx())
                        val left = idx * slotW + (slotW - barW) * 0.5f
                        val top = topPad + chartH - bh
                        val barColor = if (value == 0) {
                            LightCardBorder
                        } else if (idx >= 4) {
                            TealMint
                        } else {
                            TealPrimary
                        }
                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(left, top),
                            size = Size(barW, bh),
                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    workoutsPerBucket.forEachIndexed { idx, count ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$count",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (count > 0) TealPrimary else MutedInk
                            )
                            Text(
                                text = "P${idx + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MutedInk
                            )
                        }
                    }
                }
            }
        }

        // Card 2: Total de repetições (Area / Line Chart)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(LightCardBg)
                    .border(1.dp, LightCardBorder, RoundedCornerShape(16.dp))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total de repetições",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = DarkInk
                    )
                    Text(
                        text = "${profile.totalLifetimeReps} reps acumuladas",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = TealPrimary
                    )
                }

                val maxReps = max(20, cumulativeReps.maxOrNull() ?: 20)
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                ) {
                    val padH = 12.dp.toPx()
                    val chartH = size.height - padH * 2
                    val stepX = size.width / (cumulativeReps.size - 1).coerceAtLeast(1)

                    for (g in 0..2) {
                        val gy = padH + chartH * (g / 2f)
                        drawLine(LightCardBorder, Offset(0f, gy), Offset(size.width, gy), strokeWidth = 1.dp.toPx())
                    }

                    val linePath = Path()
                    val areaPath = Path()
                    val points = cumulativeReps.mapIndexed { i, v ->
                        val x = i * stepX
                        val y = padH + chartH * (1f - (v.toFloat() / maxReps.toFloat()))
                        Offset(x, y)
                    }

                    points.forEachIndexed { idx, pt ->
                        if (idx == 0) {
                            linePath.moveTo(pt.x, pt.y)
                            areaPath.moveTo(pt.x, size.height - padH)
                            areaPath.lineTo(pt.x, pt.y)
                        } else {
                            linePath.lineTo(pt.x, pt.y)
                            areaPath.lineTo(pt.x, pt.y)
                        }
                    }
                    areaPath.lineTo(points.last().x, size.height - padH)
                    areaPath.close()

                    drawPath(
                        path = areaPath,
                        brush = Brush.verticalGradient(
                            listOf(TealPrimary.copy(alpha = 0.25f), Color.Transparent)
                        )
                    )
                    drawPath(
                        path = linePath,
                        color = TealPrimary,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                    points.forEach { pt ->
                        drawCircle(color = TealPrimary, radius = 4.dp.toPx(), center = pt)
                    }
                }
            }
        }

        // Card 3: Consistency Score Card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(LightCardBg)
                    .border(1.dp, LightCardBorder, RoundedCornerShape(16.dp))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Índice de Consistência",
                    style = MaterialTheme.typography.titleMedium,
                    color = DarkInk
                )
                Text(
                    text = "$consistencyPct%",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (consistencyPct >= 60) TealMint else TealPrimary
                )
                Text(
                    text = if (profile.workoutsCompleted == 0) {
                        "Conclua seu primeiro treino na câmera para iniciar sua curva de evolução."
                    } else {
                        "Continue assim para manter seu hábito saudável (${profile.workoutsCompleted}/${profile.weeklyGoalDays} treinos na meta)."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedInk
                )
            }
        }
    }
}
