package com.programmer.highalarm

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalContext
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Living sunrise scene for the Pro page.
 *
 *  - A polished golden sun with a slowly turning corona and breathing glow
 *  - Three mountain ridges with rim light, each at a different depth
 *  - Twinkling stars and golden dust drifting upward
 *  - "4D" parallax: tilt the phone and every layer shifts by its depth
 *  - Fades into the page colour at the bottom (no hard edge)
 */
@Composable
fun PremiumSunriseBackground(
    modifier: Modifier = Modifier,
    reducedMotion: Boolean = false,
    fadeTo: Color = HaObsidian
) {
    val context = LocalContext.current

    // ---- Tilt (parallax) ---------------------------------------------------
    var tiltX by remember { mutableFloatStateOf(0f) }
    var tiltY by remember { mutableFloatStateOf(0f) }
    DisposableEffect(reducedMotion) {
        if (reducedMotion) return@DisposableEffect onDispose { }
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val acc = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                // gravity components -> -1..1, heavily smoothed
                val tx = (-e.values[0] / 5f).coerceIn(-1f, 1f)
                val ty = ((e.values[1] - 6.5f) / 5f).coerceIn(-1f, 1f)
                tiltX += (tx - tiltX) * 0.08f
                tiltY += (ty - tiltY) * 0.08f
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        if (acc != null) sm.registerListener(listener, acc, SensorManager.SENSOR_DELAY_UI)
        onDispose { sm.unregisterListener(listener) }
    }

    // ---- Time ----------------------------------------------------------------
    val time = rememberInfiniteTransition(label = "sunrise")
    val drift by time.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(26000, easing = LinearEasing), RepeatMode.Restart),
        label = "drift"
    )
    val corona by time.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(60000, easing = LinearEasing), RepeatMode.Restart),
        label = "corona"
    )
    val breath by time.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(4200, easing = LinearEasing), RepeatMode.Reverse),
        label = "breath"
    )
    val d = if (reducedMotion) 0f else drift
    val c = if (reducedMotion) 0f else corona
    val b = if (reducedMotion) 0.5f else breath

    val stars = remember {
        val r = Random(7)
        List(46) { floatArrayOf(r.nextFloat(), r.nextFloat() * 0.55f, 0.6f + r.nextFloat() * 1.4f, r.nextFloat()) }
    }
    val dust = remember {
        val r = Random(11)
        List(26) { floatArrayOf(r.nextFloat(), r.nextFloat(), 0.4f + r.nextFloat() * 0.9f, 1.2f + r.nextFloat() * 2.4f, r.nextFloat()) }
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val px = tiltX * 1f
        val py = tiltY * 1f
        fun shift(depth: Float) = Offset(px * depth * density * 18f, py * depth * density * 10f)

        // 1. Night-to-dawn sky
        drawRect(
            Brush.verticalGradient(
                0f to Color(0xFF050508),
                0.45f to Color(0xFF0D0B10),
                0.78f to Color(0xFF231A10),
                1f to Color(0xFF2E2112)
            )
        )

        // 2. Stars (far: almost no parallax)
        translate(shift(0.15f).x, shift(0.15f).y) {
            stars.forEach { s ->
                val tw = 0.35f + 0.65f * ((sin((d * 6f + s[3]) * 2f * PI.toFloat()) + 1f) / 2f)
                drawCircle(
                    HaGoldPale.copy(alpha = 0.55f * tw),
                    radius = s[2] * density * 0.7f,
                    center = Offset(s[0] * w, s[1] * h)
                )
            }
        }

        // 3. Sun with corona
        val sunC = Offset(w * 0.80f, h * 0.36f) + shift(0.45f)
        val sunR = 36f * density
        drawCircle(
            Brush.radialGradient(
                listOf(HaGold.copy(alpha = 0.30f + 0.08f * b), HaGoldMid.copy(alpha = 0.10f), Color.Transparent),
                center = sunC, radius = sunR * 6.5f
            ),
            radius = sunR * 6.5f, center = sunC
        )
        rotate(c, pivot = sunC) {
            for (i in 0 until 14) {
                rotate(i * (360f / 14f), pivot = sunC) {
                    val ray = Path().apply {
                        moveTo(sunC.x - sunR * 0.10f, sunC.y)
                        lineTo(sunC.x, sunC.y - sunR * (3.4f + if (i % 2 == 0) 0.9f else 0f))
                        lineTo(sunC.x + sunR * 0.10f, sunC.y)
                        close()
                    }
                    drawPath(
                        ray,
                        Brush.verticalGradient(
                            listOf(Color.Transparent, HaGoldLight.copy(alpha = 0.10f + 0.04f * b)),
                            startY = sunC.y - sunR * 4.3f, endY = sunC.y
                        )
                    )
                }
            }
        }
        // the sphere itself: lit from the upper left
        drawCircle(Color.Black.copy(alpha = 0.35f), radius = sunR * 1.02f, center = sunC + Offset(0f, sunR * 0.10f))
        drawCircle(
            Brush.radialGradient(
                listOf(Color(0xFFFFF8E2), HaGoldLight, HaGold, Color(0xFFB3802F)),
                center = sunC + Offset(-sunR * 0.35f, -sunR * 0.4f),
                radius = sunR * 1.7f
            ),
            radius = sunR, center = sunC
        )
        drawCircle(
            Brush.radialGradient(listOf(Color.White.copy(alpha = 0.75f), Color.Transparent),
                center = sunC + Offset(-sunR * 0.38f, -sunR * 0.42f), radius = sunR * 0.45f),
            radius = sunR * 0.45f, center = sunC + Offset(-sunR * 0.38f, -sunR * 0.42f)
        )
        drawCircle(
            HaGoldPale.copy(alpha = 0.35f), radius = sunR * 1.0f, center = sunC,
            style = Stroke(width = 1.2f * density)
        )

        // 4. Horizon glow
        drawOval(
            Brush.radialGradient(
                listOf(HaGold.copy(alpha = 0.22f + 0.05f * b), Color.Transparent),
                center = Offset(w * 0.72f, h * 0.86f), radius = w * 0.7f
            ),
            topLeft = Offset(w * 0.02f, h * 0.62f), size = Size(w * 1.4f, h * 0.5f)
        )

        // 5. Mountain ridges, far -> near (each with its own parallax depth)
        ridge(w, h, base = 0.70f, amp = 0.07f, seed = 1.3f, offset = shift(0.6f),
            top = Color(0xFF3A2C18), bottom = Color(0xFF1A140D), rim = HaGoldLight.copy(alpha = 0.40f))
        ridge(w, h, base = 0.80f, amp = 0.06f, seed = 2.7f, offset = shift(1.0f),
            top = Color(0xFF1F1911), bottom = Color(0xFF0E0C0A), rim = HaGold.copy(alpha = 0.22f))
        ridge(w, h, base = 0.90f, amp = 0.045f, seed = 4.1f, offset = shift(1.5f),
            top = Color(0xFF0E0D0E), bottom = fadeTo, rim = HaGoldDeep.copy(alpha = 0.18f))

        // 6. Golden dust drifting upward (nearest layer)
        translate(shift(2.0f).x, shift(2.0f).y) {
            dust.forEach { p ->
                val y = ((p[1] - d * p[2] * 1.4f) % 1f + 1f) % 1f
                val x = p[0] + 0.02f * sin((d * 8f + p[4]) * 2f * PI.toFloat())
                val fade = (sin(y * PI.toFloat())).coerceAtLeast(0f)
                val center = Offset(x * w, y * h)
                val r = p[3] * density
                drawCircle(
                    Brush.radialGradient(listOf(HaGoldPale.copy(alpha = 0.55f * fade), Color.Transparent), center = center, radius = r * 3f),
                    radius = r * 3f, center = center, blendMode = BlendMode.Screen
                )
            }
        }

        // 7. Seamless fade into the page
        drawRect(
            Brush.verticalGradient(
                0f to Color.Transparent,
                0.82f to Color.Transparent,
                1f to fadeTo
            )
        )
    }
}

/** Smooth ridge silhouette (sum of sines) with a vertical gradient and rim light. */
private fun DrawScope.ridge(
    w: Float, h: Float,
    base: Float, amp: Float, seed: Float, offset: Offset,
    top: Color, bottom: Color, rim: Color
) {
    val steps = 48
    val pts = (0..steps).map { i ->
        val fx = i / steps.toFloat()
        val y = base +
            amp * (0.55f * sin(fx * 6.2f + seed) + 0.30f * sin(fx * 13.1f + seed * 2.3f) + 0.15f * sin(fx * 27.7f + seed * 0.7f))
        Offset(fx * (w + 80f) - 40f + offset.x, y * h + offset.y)
    }
    val fill = Path().apply {
        moveTo(pts.first().x, h + 4f)
        pts.forEach { lineTo(it.x, it.y) }
        lineTo(pts.last().x, h + 4f)
        close()
    }
    val minY = pts.minOf { it.y }
    drawPath(fill, Brush.verticalGradient(listOf(top, bottom), startY = minY, endY = h))
    val edge = Path().apply {
        moveTo(pts.first().x, pts.first().y)
        pts.drop(1).forEach { lineTo(it.x, it.y) }
    }
    drawPath(edge, rim, style = Stroke(width = 1.4f * density))
}
