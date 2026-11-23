package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.ViewInAr
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
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
import com.example.ui.theme.LabSurfaceElevated
import com.example.ui.theme.TerracottaMesh
import com.example.ui.theme.TextSecondary
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Pure Compose 3D Spatial Avatar Viewport:
 * - Guaranteed real-time rendering on every device/emulator without SurfaceView z-order issues
 * - Auto-follows the live athlete smoothly so moving in front of the camera never leaves the viewport
 * - Renders depth-sorted lit 3D body mesh + color-coded MHR70 skeleton + studio floor grid
 */
@Composable
fun Mhr3DViewport(
    frameResult: FrameResult?,
    showMesh: Boolean,
    meshAlpha: Float,
    exerciseFeedback: ExerciseFeedback? = null,
    mirrorHorizontal: Boolean = false,
    modifier: Modifier = Modifier
) {
    var yaw by remember { mutableFloatStateOf(0.28f) }
    var pitch by remember { mutableFloatStateOf(0.10f) }
    var distance by remember { mutableFloatStateOf(2.4f) }
    var targetX by remember { mutableFloatStateOf(0f) }
    var targetY by remember { mutableFloatStateOf(0f) }
    var targetZ by remember { mutableFloatStateOf(2.5f) }

    fun snapToPerson(result: FrameResult?, resetAngles: Boolean) {
        val first = result?.persons?.firstOrNull() ?: return
        val j = first.joints3dCam
        var cx = 0f; var cy = 0f; var cz = 0f
        for (i in 0 until 70) {
            cx += j[i * 3]
            cy += j[i * 3 + 1]
            cz += j[i * 3 + 2]
        }
        cx /= 70f; cy /= 70f; cz /= 70f
        var maxExt = 0.55f
        for (i in 0 until 70) {
            maxExt = max(maxExt, abs(j[i * 3] - cx))
            maxExt = max(maxExt, abs(j[i * 3 + 1] - cy))
            maxExt = max(maxExt, abs(j[i * 3 + 2] - cz))
        }
        targetX = cx
        targetY = cy
        targetZ = cz
        if (resetAngles) {
            distance = max(2.6f * maxExt, 1.95f)
            yaw = 0.28f
            pitch = 0.10f
        }
    }

    // Smoothly track the live person in real time on every frame update
    LaunchedEffect(frameResult) {
        val first = frameResult?.persons?.firstOrNull()
        if (first != null) {
            val j = first.joints3dCam
            var cx = 0f; var cy = 0f; var cz = 0f
            for (i in 0 until 70) {
                cx += j[i * 3]
                cy += j[i * 3 + 1]
                cz += j[i * 3 + 2]
            }
            cx /= 70f; cy /= 70f; cz /= 70f
            targetX = cx
            targetY = cy
            targetZ = cz
            var maxExt = 0.55f
            for (i in 0 until 70) {
                maxExt = max(maxExt, abs(j[i * 3] - cx))
                maxExt = max(maxExt, abs(j[i * 3 + 1] - cy))
            }
            val idealDist = max(2.6f * maxExt, 1.95f)
            if (distance > idealDist * 2.2f || distance < idealDist * 0.45f) {
                distance = idealDist
            }
        }
    }

    val boneColor = when (exerciseFeedback?.formStatus) {
        FormStatus.GOOD -> EmeraldJoint
        FormStatus.CAUTION -> GoldBone
        FormStatus.FAULT -> CoralError
        null -> GoldBone
    }

    val triPath = remember { Path() }

    Box(
        modifier = modifier
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF141E30), Color(0xFF090D14))
                )
            )
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    if (zoom != 1f) {
                        distance = (distance / zoom).coerceIn(0.8f, 9.0f)
                    } else {
                        yaw -= pan.x * 0.0075f
                        pitch = (pitch - pan.y * 0.0075f).coerceIn(-1.25f, 1.25f)
                    }
                }
            }
            .testTag("viewport_3d")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            if (w <= 1f || h <= 1f) return@Canvas

            val cosP = cos(pitch)
            val sinP = sin(pitch)
            val cosY = cos(yaw)
            val sinY = sin(yaw)

            // Orbital camera in Y-down coordinates (worldUp = (0, -1, 0))
            val eyeX = targetX + distance * cosP * sinY
            val eyeY = targetY + distance * sinP
            val eyeZ = targetZ - distance * cosP * cosY

            var fx = targetX - eyeX
            var fy = targetY - eyeY
            var fz = targetZ - eyeZ
            val fLen = max(1e-5f, sqrt(fx * fx + fy * fy + fz * fz))
            fx /= fLen; fy /= fLen; fz /= fLen

            // right = forward x worldUp(0, -1, 0) = (+fz, 0, -fx)
            var rx = fz
            val ry = 0f
            var rz = -fx
            val rLen = max(1e-5f, sqrt(rx * rx + rz * rz))
            rx /= rLen; rz /= rLen

            // camUp = right x forward
            val ux = ry * fz - rz * fy
            val uy = rz * fx - rx * fz
            val uz = rx * fy - ry * fx

            val focalPx = minOf(w, h) * 1.15f
            val halfW = w * 0.5f
            val halfH = h * 0.5f
            val xSign = if (mirrorHorizontal) -1f else 1f

            fun projectX(wx: Float, wy: Float, wz: Float): Float {
                val dx = wx - eyeX; val dy = wy - eyeY; val dz = wz - eyeZ
                val cz = dx * fx + dy * fy + dz * fz
                if (cz <= 0.1f) return Float.NaN
                val cx = dx * rx + dy * ry + dz * rz
                return halfW + xSign * (cx / cz) * focalPx
            }

            fun projectY(wx: Float, wy: Float, wz: Float): Float {
                val dx = wx - eyeX; val dy = wy - eyeY; val dz = wz - eyeZ
                val cz = dx * fx + dy * fy + dz * fz
                if (cz <= 0.1f) return Float.NaN
                val cy = dx * ux + dy * uy + dz * uz
                return halfH - (cy / cz) * focalPx
            }

            // Compute floor height just beneath detected person's lowest joint
            var floorY = targetY + 0.90f
            val firstPerson = frameResult?.persons?.firstOrNull()
            if (firstPerson != null) {
                var maxY = Float.NEGATIVE_INFINITY
                val j = firstPerson.joints3dCam
                for (i in 0 until 70) maxY = max(maxY, j[i * 3 + 1])
                if (maxY.isFinite()) floorY = maxY + 0.05f
            }

            // 1. Studio Floor Grid
            val gridRange = 4
            val gridStep = 0.36f
            for (i in -gridRange..gridRange) {
                val off = i * gridStep
                val alpha = if (i == 0) 0.44f else 0.18f
                val gColor = CyanBox.copy(alpha = alpha)

                val x1 = projectX(targetX - gridRange * gridStep, floorY, targetZ + off)
                val y1 = projectY(targetX - gridRange * gridStep, floorY, targetZ + off)
                val x2 = projectX(targetX + gridRange * gridStep, floorY, targetZ + off)
                val y2 = projectY(targetX + gridRange * gridStep, floorY, targetZ + off)
                if (x1.isFinite() && y1.isFinite() && x2.isFinite() && y2.isFinite()) {
                    drawLine(gColor, Offset(x1, y1), Offset(x2, y2), strokeWidth = 1.4f)
                }

                val ax1 = projectX(targetX + off, floorY, targetZ - gridRange * gridStep)
                val ay1 = projectY(targetX + off, floorY, targetZ - gridRange * gridStep)
                val ax2 = projectX(targetX + off, floorY, targetZ + gridRange * gridStep)
                val ay2 = projectY(targetX + off, floorY, targetZ + gridRange * gridStep)
                if (ax1.isFinite() && ay1.isFinite() && ax2.isFinite() && ay2.isFinite()) {
                    drawLine(gColor, Offset(ax1, ay1), Offset(ax2, ay2), strokeWidth = 1.4f)
                }
            }

            val result = frameResult ?: return@Canvas
            val faces = result.faces

            // 2. Render each detected person (3D Mesh + 3D Skeleton)
            for (person in result.persons) {
                val verts = person.verticesCam
                val numVerts = verts.size / 3

                if (showMesh && faces.size >= 3 && numVerts > 0) {
                    val px = FloatArray(numVerts)
                    val py = FloatArray(numVerts)
                    val pz = FloatArray(numVerts)
                    for (v in 0 until numVerts) {
                        val vx = verts[v * 3]; val vy = verts[v * 3 + 1]; val vz = verts[v * 3 + 2]
                        val dx = vx - eyeX; val dy = vy - eyeY; val dz = vz - eyeZ
                        val cz = dx * fx + dy * fy + dz * fz
                        pz[v] = cz
                        if (cz > 0.1f) {
                            val cx = dx * rx + dy * ry + dz * rz
                            val cy = dx * ux + dy * uy + dz * uz
                            px[v] = halfW + xSign * (cx / cz) * focalPx
                            py[v] = halfH - (cy / cz) * focalPx
                        } else {
                            px[v] = Float.NaN
                            py[v] = Float.NaN
                        }
                    }

                    val totalTris = faces.size / 3
                    val step = when {
                        totalTris > 12000 -> 6
                        totalTris > 4000 -> 3
                        else -> 1
                    }
                    val cap = totalTris / step + 1
                    val triBases = IntArray(cap)
                    val triDepths = FloatArray(cap)
                    var validCount = 0

                    var t = 0
                    while (t < totalTris) {
                        val base = t * 3
                        val i0 = faces[base]; val i1 = faces[base + 1]; val i2 = faces[base + 2]
                        t += step
                        if (i0 < 0 || i1 < 0 || i2 < 0 || i0 >= numVerts || i1 >= numVerts || i2 >= numVerts) continue
                        if (!px[i0].isFinite() || !px[i1].isFinite() || !px[i2].isFinite()) continue
                        val d = (pz[i0] + pz[i1] + pz[i2]) * 0.3333f
                        if (d <= 0.15f) continue
                        triBases[validCount] = base
                        triDepths[validCount] = d
                        validCount++
                    }

                    // Primitive order array sorted back-to-front (painter's algorithm)
                    val order = Array(validCount) { it }
                    order.sortWith { a, b -> triDepths[b].compareTo(triDepths[a]) }

                    val baseR = TerracottaMesh.red
                    val baseG = TerracottaMesh.green
                    val baseB = TerracottaMesh.blue
                    val alphaClamped = meshAlpha.coerceIn(0.08f, 0.96f)

                    for (idx in order) {
                        val base = triBases[idx]
                        val i0 = faces[base]; val i1 = faces[base + 1]; val i2 = faces[base + 2]
                        val ax = verts[i1 * 3] - verts[i0 * 3]
                        val ay = verts[i1 * 3 + 1] - verts[i0 * 3 + 1]
                        val az = verts[i1 * 3 + 2] - verts[i0 * 3 + 2]
                        val bx = verts[i2 * 3] - verts[i0 * 3]
                        val by = verts[i2 * 3 + 1] - verts[i0 * 3 + 1]
                        val bz = verts[i2 * 3 + 2] - verts[i0 * 3 + 2]
                        val nx = ay * bz - az * by
                        val ny = az * bx - ax * bz
                        val nz = ax * by - ay * bx
                        val nLen = max(1e-6f, sqrt(nx * nx + ny * ny + nz * nz))
                        val ndotl = abs((nx * 0.4f - ny * 0.65f - nz * 0.64f) / nLen)
                        val shade = (0.36f + 0.64f * ndotl).coerceIn(0.25f, 1.0f)

                        triPath.reset()
                        triPath.moveTo(px[i0], py[i0])
                        triPath.lineTo(px[i1], py[i1])
                        triPath.lineTo(px[i2], py[i2])
                        triPath.close()

                        drawPath(
                            path = triPath,
                            color = Color(
                                red = (baseR * shade).coerceIn(0f, 1f),
                                green = (baseG * shade).coerceIn(0f, 1f),
                                blue = (baseB * shade).coerceIn(0f, 1f),
                                alpha = alphaClamped
                            )
                        )
                    }
                }

                // 3. 3D MHR70 Skeleton Overlay
                val j3 = person.joints3dCam
                val jx = FloatArray(70)
                val jy = FloatArray(70)
                for (j in 0 until 70) {
                    val wx = j3[j * 3]; val wy = j3[j * 3 + 1]; val wz = j3[j * 3 + 2]
                    jx[j] = projectX(wx, wy, wz)
                    jy[j] = projectY(wx, wy, wz)
                }

                for ((a, b) in Mhr70Skeleton.SKELETON_EDGES) {
                    if (!jx[a].isFinite() || !jx[b].isFinite()) continue
                    drawLine(
                        color = boneColor,
                        start = Offset(jx[a], jy[a]),
                        end = Offset(jx[b], jy[b]),
                        strokeWidth = 5.5f,
                        cap = StrokeCap.Round
                    )
                }

                for (j in 0 until 70) {
                    if (!jx[j].isFinite()) continue
                    val isMajor = j in 0..14 || j == 41 || j == 62 || j == 69
                    drawCircle(
                        color = if (isMajor) CyanBox else EmeraldJoint,
                        radius = if (isMajor) 5.5f else 2.5f,
                        center = Offset(jx[j], jy[j])
                    )
                }
            }
        }

        if (frameResult == null || frameResult.persons.isEmpty()) {
            Surface(
                color = LabSurfaceElevated.copy(alpha = 0.88f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.ViewInAr, null, tint = CyanBox, modifier = Modifier.size(18.dp))
                    Text(
                        text = "Posicione o corpo na câmera para gerar o Avatar 3D",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }

        FilledTonalIconButton(
            onClick = { snapToPerson(frameResult, resetAngles = true) },
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = LabSurfaceElevated.copy(alpha = 0.88f),
                contentColor = CyanBox
            ),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .size(44.dp)
                .testTag("reset_3d_camera_button")
        ) {
            Icon(
                imageVector = Icons.Rounded.CenterFocusStrong,
                contentDescription = "Centralizar avatar 3D"
            )
        }

        Surface(
            color = LabSurfaceElevated.copy(alpha = 0.78f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
        ) {
            val vertCount = frameResult?.persons?.firstOrNull()?.let { it.verticesLocal.size / 3 } ?: 0
            Text(
                text = if (vertCount > 0) "Avatar 3D · $vertCount vértices · Arraste para girar" else "Avatar 3D Interativo",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}
