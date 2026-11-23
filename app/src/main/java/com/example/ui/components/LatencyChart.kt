package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.domain.model.TimingSample
import com.example.ui.theme.CyanBox
import com.example.ui.theme.GoldBone
import com.example.ui.theme.LabBorder
import com.example.ui.theme.LabSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.max

/**
 * Port of Rerun's TimeSeriesView(origin="timing", name="Latency (ms)"):
 * Live scrolling dual-series chart for Detector ms and InstantHMR ms + median readout.
 */
@Composable
fun LatencyChart(
    samples: List<TimingSample>,
    modifier: Modifier = Modifier
) {
    val detMedian = medianOf(samples.map { it.detectorMs })
    val hmrMedian = medianOf(samples.map { it.hmrMs })

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(LabSurface)
            .border(1.dp, LabBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "LATENCY (MS)",
                style = MaterialTheme.typography.labelMedium,
                color = TextPrimary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LegendChip("det ${String.format("%.1f", detMedian)}", CyanBox)
                LegendChip("hmr ${String.format("%.1f", hmrMedian)}", GoldBone)
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            val maxMs = max(20f, samples.maxOfOrNull { max(it.detectorMs, it.hmrMs) } ?: 50f)
            // Grid lines
            for (frac in listOf(0.25f, 0.5f, 0.75f)) {
                val y = size.height * frac
                drawLine(
                    color = LabBorder.copy(alpha = 0.6f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f
                )
            }

            if (samples.size < 2) return@Canvas

            val stepX = size.width / (samples.size - 1).coerceAtLeast(1)
            drawSeries(samples.map { it.detectorMs }, maxMs, stepX, CyanBox)
            drawSeries(samples.map { it.hmrMs }, maxMs, stepX, GoldBone)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSeries(
    values: List<Float>,
    maxVal: Float,
    stepX: Float,
    color: Color
) {
    val path = Path()
    values.forEachIndexed { idx, v ->
        val x = idx * stepX
        val y = size.height - ((v / maxVal).coerceIn(0f, 1f) * (size.height - 6f)) - 3f
        if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = 3f, cap = StrokeCap.Round)
    )
}

@Composable
private fun LegendChip(text: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
    }
}

private fun medianOf(vals: List<Float>): Float {
    if (vals.isEmpty()) return 0f
    val sorted = vals.sorted()
    val mid = sorted.size / 2
    return if (sorted.size % 2 == 0) (sorted[mid - 1] + sorted[mid]) * 0.5f else sorted[mid]
}
