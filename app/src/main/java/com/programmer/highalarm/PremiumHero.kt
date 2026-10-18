package com.programmer.highalarm

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalContext
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Quiet-luxury sunrise for the Pro page.
 *
 * Deliberately minimal (no cartoon rays or bokeh): a polished half-sun rising
 * over a hairline horizon, fine concentric rings with a slow ripple, its soft
 * reflection, and a handful of faint stars. On open the sun rises into place;
 * tilting the phone shifts the layers by depth (parallax).
 */
@Composable
fun PremiumSunriseBackground(
    modifier: Modifier = Modifier,
    reducedMotion: Boolean = false,
    fadeTo: Color = HaObsidian
) {
    val context = LocalContext.current

    // ---- Tilt parallax ------------------------------------------------------
    var tiltX by remember { mutableFloatStateOf(0f) }
    var tiltY by remember { mutableFloatStateOf(0f) }
    DisposableEffect(reducedMotion) {
        if (reducedMotion) return@DisposableEffect onDispose { }
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val acc = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                val tx = (-e.values[0] / 5f).coerceIn(-1f, 1f)
                val ty = ((e.values[1] - 6.5f) / 5f).coerceIn(-1f, 1f)
                tiltX += (tx - tiltX) * 0.06f
                tiltY += (ty - tiltY) * 0.06f
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        if (acc != null) sm.registerListener(listener, acc, SensorManager.SENSOR_DELAY_UI)
        onDispose { sm.unregisterListener(listener) }
    }

    // ---- Sunrise on open ----------------------------------------------------
    val rise = remember { Animatable(if (reducedMotion) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!reducedMotion) rise.animateTo(1f, tween(2600, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)))
    }

    // ---- Ambient motion -----------------------------------------------------
    val time = rememberInfiniteTransition(label = "sunrise")
    val ripple by time.animateFloat(
        0f, 1f, infiniteRepeatable(tween(5200, easing = LinearEasing), RepeatMode.Restart), label = "ripple"
    )
    val twinkle by time.animateFloat(
        0f, 1f, infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Restart), label = "twinkle"
    )
    val breath by time.animateFloat(
        0f, 1f, infiniteRepeatable(tween(4800, easing = LinearEasing), RepeatMode.Reverse), label = "breath"
    )
    val rp = if (reducedMotion) 0.35f else ripple
    val tw = if (reducedMotion) 0f else twinkle
    val br = if (reducedMotion) 0.5f else breath

    val stars = remember {
        val r = Random(21)
        List(22) { floatArrayOf(r.nextFloat(), 0.05f + r.nextFloat() * 0.45f, 0.5f + r.nextFloat() * 0.9f, r.nextFloat()) }
    }

    Canvas(modifier = modifier.clipToBounds()) {
        val w = size.width
        val h = size.height
        fun shift(depth: Float) = Offset(tiltX * depth * density * 14f, tiltY * depth * density * 8f)

        // 1. Sky: near-black with the faintest warmth toward the horizon
        drawRect(
            Brush.verticalGradient(
                0f to Color(0xFF050507),
                0.55f to Color(0xFF0A090C),
                1f to Color(0xFF15110C)
            )
        )

        // 2. Stars — few, tiny, slow twinkle
        val so = shift(0.2f)
        stars.forEach { s ->
            val a = 0.25f + 0.55f * ((sin((tw * 2f + s[3]) * 2f * PI.toFloat()) + 1f) / 2f)
            drawCircle(
                HaIvory.copy(alpha = 0.35f * a),
                radius = s[2] * density * 0.6f,
                center = Offset(s[0] * w + so.x, s[1] * h + so.y)
            )
        }

        // Geometry
        val horizonY = h * 0.84f
        val sunR = 50f * density
        val sunX = w * 0.80f
        val sunO = shift(0.6f)
        // rises from below the horizon on open, then rests 35% submerged
        val sunY = horizonY + sunR * (0.35f + 1.1f * (1f - rise.value))
        val sunC = Offset(sunX + sunO.x, sunY + sunO.y)

        // 3. Atmospheric glow
        drawCircle(
            Brush.radialGradient(
                listOf(
                    HaGold.copy(alpha = (0.20f + 0.04f * br) * rise.value),
                    HaGoldMid.copy(alpha = 0.06f * rise.value),
                    Color.Transparent
                ),
                center = Offset(sunC.x, horizonY), radius = w * 0.85f
            ),
            radius = w * 0.85f, center = Offset(sunC.x, horizonY)
        )

        // 4. Everything above the horizon
        clipRect(bottom = horizonY + sunO.y * 0.3f) {
            // concentric hairline rings
            val rings = floatArrayOf(1.55f, 2.2f, 2.95f, 3.8f, 4.8f)
            rings.forEachIndexed { i, k ->
                drawCircle(
                    HaGoldLight.copy(alpha = (0.22f - i * 0.035f) * rise.value),
                    radius = sunR * k, center = sunC,
                    style = Stroke(width = 0.8f * density)
                )
            }
            // slow ripple travelling outward
            val rr = sunR * (1.1f + 4.4f * rp)
            drawCircle(
                HaGoldPale.copy(alpha = 0.28f * (1f - rp) * rise.value),
                radius = rr, center = sunC, style = Stroke(width = 1.1f * density)
            )
            // polished sun
            drawCircle(
                Brush.verticalGradient(
                    listOf(Color(0xFFFFF4D6), HaGoldLight, HaGold, Color(0xFFB57F2C)),
                    startY = sunC.y - sunR, endY = sunC.y + sunR * 0.4f
                ),
                radius = sunR, center = sunC
            )
            drawCircle(
                Brush.radialGradient(
                    listOf(Color.White.copy(alpha = 0.55f), Color.Transparent),
                    center = sunC + Offset(-sunR * 0.32f, -sunR * 0.45f), radius = sunR * 0.6f
                ),
                radius = sunR * 0.6f, center = sunC + Offset(-sunR * 0.32f, -sunR * 0.45f)
            )
        }

        // 5. Reflection on the "water" below the horizon
        clipRect(top = horizonY) {
            for (i in 0 until 7) {
                val y = horizonY + (i + 0.6f) * 5.5f * density
                val half = sunR * (0.95f - i * 0.11f) * (0.85f + 0.15f * sin((tw * 3f + i * 0.37f) * 2f * PI.toFloat()))
                drawLine(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, HaGoldLight.copy(alpha = (0.32f - i * 0.04f) * rise.value), Color.Transparent),
                        startX = sunC.x - half, endX = sunC.x + half
                    ),
                    start = Offset(sunC.x - half, y), end = Offset(sunC.x + half, y),
                    strokeWidth = 1.6f * density
                )
            }
        }

        // 6. Hairline horizon, brightest under the sun, fading to the left
        drawLine(
            Brush.horizontalGradient(
                0f to Color.Transparent,
                0.45f to HaGold.copy(alpha = 0.10f),
                (sunC.x / w).coerceIn(0.5f, 0.95f) to HaGoldPale.copy(alpha = 0.65f),
                1f to HaGold.copy(alpha = 0.15f)
            ),
            start = Offset(0f, horizonY), end = Offset(w, horizonY),
            strokeWidth = 1.0f * density
        )

        // 7. Seamless fade into the page
        drawRect(
            Brush.verticalGradient(0f to Color.Transparent, 0.86f to Color.Transparent, 1f to fadeTo)
        )
    }
}
