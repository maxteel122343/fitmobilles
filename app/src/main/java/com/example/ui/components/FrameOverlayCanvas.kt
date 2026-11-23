package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.fitness.ExerciseFeedback
import com.example.domain.fitness.FormStatus
import com.example.domain.model.FrameResult
import com.example.domain.skeleton.Mhr70Skeleton
import com.example.ui.theme.CoralError
import com.example.ui.theme.CyanBox
import com.example.ui.theme.EmeraldJoint
import com.example.ui.theme.GoldBone
import com.example.ui.theme.MintSuccess
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.min

/**
 * Renders the 2D workout frame (or transparent overlay over live CameraX preview),
 * clean tracking frame corners, color-coded MHR70 skeleton edges (Green/Yellow/Red form status),
 * and optional diagnostic telemetry HUD when enabled in advanced settings.
 */
@Composable
fun FrameOverlayCanvas(
    bitmap: Bitmap?,
    frameWidth: Int,
    frameHeight: Int,
    frameResult: FrameResult?,
    exerciseFeedback: ExerciseFeedback? = null,
    mirrorHorizontal: Boolean = false,
    showBitmap: Boolean = true,
    showDiagnosticHud: Boolean = false,
    modifier: Modifier = Modifier
) {
    val imageBitmap = remember(bitmap, showBitmap) {
        if (showBitmap && bitmap != null && !bitmap.isRecycled) bitmap.asImageBitmap() else null
    }

    val boneColor = when (exerciseFeedback?.formStatus) {
        FormStatus.GOOD -> EmeraldJoint
        FormStatus.CAUTION -> GoldBone
        FormStatus.FAULT -> CoralError
        null -> GoldBone
    }

    Box(
        modifier = modifier
            .background(if (showBitmap) ObsidianBg else Color.Transparent)
            .testTag("viewport_2d")
    ) {
        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap,
                contentDescription = "Frame analisado",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val result = frameResult ?: return@Canvas
            val srcW = frameWidth.coerceAtLeast(1).toFloat()
            val srcH = frameHeight.coerceAtLeast(1).toFloat()

            val scale = min(size.width / srcW, size.height / srcH)
            val drawnW = srcW * scale
            val drawnH = srcH * scale
            val offsetX = (size.width - drawnW) * 0.5f
            val offsetY = (size.height - drawnH) * 0.5f

            fun mapX(x: Float): Float {
                val nx = if (mirrorHorizontal) (srcW - x) else x
                return offsetX + nx * scale
            }

            fun mapY(y: Float): Float = offsetY + y * scale

            val strokeW = (min(size.width, size.height) / 150f).coerceIn(2.5f, 6.5f)

            for (person in result.persons) {
                if (showDiagnosticHud) {
                    val b = person.bbox
                    val left = mapX(if (mirrorHorizontal) b[2] else b[0])
                    val right = mapX(if (mirrorHorizontal) b[0] else b[2])
                    val top = mapY(b[1])
                    val bottom = mapY(b[3])

                    drawRect(
                        color = CyanBox.copy(alpha = 0.7f),
                        topLeft = Offset(left, top),
                        size = Size((right - left).coerceAtLeast(1f), (bottom - top).coerceAtLeast(1f)),
                        style = Stroke(width = strokeW * 0.75f)
                    )
                }

                val j2d = person.joints2d
                for ((a, c) in Mhr70Skeleton.SKELETON_EDGES) {
                    val ax = mapX(j2d[a * 2])
                    val ay = mapY(j2d[a * 2 + 1])
                    val cx = mapX(j2d[c * 2])
                    val cy = mapY(j2d[c * 2 + 1])
                    drawLine(
                        color = boneColor,
                        start = Offset(ax, ay),
                        end = Offset(cx, cy),
                        strokeWidth = strokeW,
                        cap = StrokeCap.Round
                    )
                }

                for (j in 0 until Mhr70Skeleton.NUM_JOINTS) {
                    val jx = mapX(j2d[j * 2])
                    val jy = mapY(j2d[j * 2 + 1])
                    val isPrimaryJoint = j in 5..14 || j == 41 || j == 62
                    drawCircle(
                        color = if (isPrimaryJoint) CyanBox else EmeraldJoint,
                        radius = if (isPrimaryJoint) strokeW * 1.15f else strokeW * 0.65f,
                        center = Offset(jx, jy)
                    )
                }
            }
        }

        if (frameResult != null) {
            if (showDiagnosticHud) {
                Surface(
                    color = ObsidianBg.copy(alpha = 0.76f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                        Text(
                            text = frameResult.engineLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = CyanBox
                        )
                        HudMetricRow(
                            "total",
                            String.format("%.1f ms (%.0f fps)", frameResult.totalMs, frameResult.fps),
                            highlight = true
                        )
                    }
                }
            } else {
                val tracked = frameResult.persons.isNotEmpty()
                Surface(
                    color = ObsidianBg.copy(alpha = 0.72f),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (tracked) MintSuccess else GoldBone)
                        )
                        Text(
                            text = if (tracked) "TRACKING · ${frameResult.fps.toInt().coerceAtLeast(1)} FPS" else "AGUARDANDO ATLETA",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HudMetricRow(label: String, value: String, highlight: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label.padEnd(7),
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            color = if (highlight) GoldBone else TextPrimary
        )
    }
}
