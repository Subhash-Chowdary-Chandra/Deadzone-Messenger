package com.deadzone.messenger.ui

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Matrix as AndroidMatrix
import android.graphics.Paint as AndroidPaint
import android.graphics.Path as AndroidPath
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusWeak
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deadzone.messenger.data.AvatarStore
import com.deadzone.messenger.ui.theme.*

/**
 * Full-screen photo editor shown after the user picks an image.
 * Features:
 *  - Pinch-to-zoom (0.5×–8×)
 *  - Drag to reposition
 *  - Two-finger twist to rotate freely
 *  - ±90° rotate buttons
 *  - Horizontal flip
 *  - Reset button
 *  - Live circular crop preview with rule-of-thirds grid
 * On "Done" it extracts the circular crop area and calls [onConfirm] with Base64 JPEG.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoEditorScreen(
    sourceBitmap: Bitmap,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val density = LocalDensity.current
    val cropDp = 280.dp
    val cropPx = with(density) { cropDp.toPx() }
    val outputPx = 400

    // Transform state (user gestures accumulate here)
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var rotation by remember { mutableStateOf(0f) }
    var flipH by remember { mutableStateOf(false) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    // Scale that makes the image's shorter side exactly fill the crop circle at scale=1
    val initialScale by remember(containerSize, sourceBitmap) {
        derivedStateOf {
            if (containerSize == IntSize.Zero) return@derivedStateOf 1f
            cropPx / minOf(sourceBitmap.width, sourceBitmap.height).toFloat()
        }
    }

    val transformableState = rememberTransformableState { zoomChange, panChange, rotationChange ->
        scale = (scale * zoomChange).coerceIn(0.5f, 8f)
        offset += panChange
        rotation += rotationChange
    }

    // Extracts the circular crop area into a Base64 JPEG string
    fun buildCroppedBase64(): String {
        val out = Bitmap.createBitmap(outputPx, outputPx, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(out)
        val paint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG)
        val half = outputPx / 2f

        // Clip canvas to circle
        val path = AndroidPath().apply { addCircle(half, half, half, AndroidPath.Direction.CW) }
        canvas.clipPath(path)
        canvas.drawColor(android.graphics.Color.BLACK)

        // Build Android Matrix that mirrors the graphicsLayer transforms
        val screenToOut = outputPx / cropPx
        val totalScale = initialScale * scale * screenToOut
        val matrix = AndroidMatrix().apply {
            postTranslate(-sourceBitmap.width / 2f, -sourceBitmap.height / 2f)
            if (flipH) postScale(-1f, 1f)
            postRotate(rotation)
            postScale(totalScale, totalScale)
            postTranslate(half + offset.x * screenToOut, half + offset.y * screenToOut)
        }
        canvas.drawBitmap(sourceBitmap, matrix, paint)
        return AvatarStore.bitmapToBase64(out)
    }

    // ── Root layout ───────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {

        // ── Transformable image ───────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { containerSize = it }
                .transformable(state = transformableState),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Image(
                bitmap = sourceBitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val s = initialScale * scale
                        scaleX = s * if (flipH) -1f else 1f
                        scaleY = s
                        translationX = offset.x
                        translationY = offset.y
                        rotationZ = rotation
                    }
            )
        }

        // ── Crop overlay (dark vignette + circular cutout + grid) ─────────────
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        ) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val r  = cropPx / 2f

            drawRect(Color.Black.copy(alpha = 0.65f))
            drawCircle(color = Color.Transparent, radius = r,
                center = Offset(cx, cy), blendMode = BlendMode.Clear)
            drawCircle(color = AccentNeon, radius = r,
                center = Offset(cx, cy), style = Stroke(width = 2.dp.toPx()))

            // Rule-of-thirds guide lines
            val lineColor = Color.White.copy(alpha = 0.2f)
            val third = r * 2 / 3f
            for (i in 1..2) {
                val y = cy - r + third * i
                drawLine(lineColor, Offset(cx - r, y), Offset(cx + r, y), strokeWidth = 0.8.dp.toPx())
                val x = cx - r + third * i
                drawLine(lineColor, Offset(x, cy - r), Offset(x, cy + r), strokeWidth = 0.8.dp.toPx())
            }
        }

        // ── Top bar: Cancel / title / Done ────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary, fontSize = 15.sp)
            }
            Text(
                "Edit Photo",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = { onConfirm(buildCroppedBase64()) }) {
                Text("Done", color = AccentNeon, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }

        // ── Zoom level badge ──────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = cropDp / 2 + 14.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 10.dp, vertical = 3.dp)
        ) {
            Text(
                text = "${"%.1f".format(initialScale * scale)}×",
                color = TextSecondary,
                fontSize = 12.sp
            )
        }

        // ── Bottom toolbar ────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.75f))
                .padding(bottom = 24.dp, top = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Pinch to zoom  •  Drag to reposition  •  Twist to rotate",
                color = TextSecondary,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                EditorToolButton(icon = Icons.Default.RotateLeft, label = "Rotate\nLeft") {
                    rotation -= 90f
                }
                EditorToolButton(icon = Icons.Default.Flip, label = "Flip") {
                    flipH = !flipH
                }
                EditorToolButton(icon = Icons.Default.CenterFocusWeak, label = "Reset") {
                    scale = 1f; offset = Offset.Zero; rotation = 0f; flipH = false
                }
                EditorToolButton(icon = Icons.Default.RotateRight, label = "Rotate\nRight") {
                    rotation += 90f
                }
            }
        }
    }
}

// ── Tool button used in the bottom toolbar ────────────────────────────────────

@Composable
private fun RowScope.EditorToolButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.weight(1f)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
                .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
        ) {
            Icon(icon, contentDescription = label, tint = TextPrimary,
                modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(label, color = TextSecondary, fontSize = 10.sp,
            textAlign = TextAlign.Center, lineHeight = 13.sp)
    }
}
