package com.programmer.highalarm

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

enum class ExerciseKind { PushUp, Squat }

/** Polished metal mannequin colours: light, base, deep. */
val AthleteGold = listOf(Color(0xFFFFF1C9), Color(0xFFE2B865), Color(0xFF6E4D1C))
val AthleteInk = listOf(Color(0xFF6B4E1E), Color(0xFF3A2A10), Color(0xFF140E05))

/** World units used by each pose (the canvas scales to fit). */
private const val PUSHUP_W = 34f
private const val SQUAT_W = 24f
private const val WORLD_H = 24f

private val RepEase = CubicBezierEasing(0.45f, 0f, 0.25f, 1f)

/**
 * A little 3D mannequin that does one smooth rep, rests, and repeats.
 * Limbs are tapered, shaded volumes (lit from above), joints are solved with
 * real limb lengths, and the far-side arm/leg sits behind in shadow — so it
 * reads as a body, not a stick figure.
 *
 * @param colors light → base → deep metal tones for the body
 */
@Composable
fun AnimatedExerciseIcon(
    kind: ExerciseKind,
    colors: List<Color> = AthleteGold,
    modifier: Modifier = Modifier,
    showFloor: Boolean = true
) {
    val transition = rememberInfiniteTransition(label = "exercise_icon")
    // 0 = top of the rep, 1 = bottom
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 3000
                0f at 0 using LinearEasing
                0f at 950 using RepEase       // rest at the top
                1f at 1700 using LinearEasing  // controlled descent
                1f at 1950 using RepEase       // short pause at the bottom
                0f at 2650 using LinearEasing  // drive back up
                0f at 3000
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "rep_phase"
    )

    Canvas(modifier = modifier) {
        val worldW = if (kind == ExerciseKind.PushUp) PUSHUP_W else SQUAT_W
        val u = minOf(size.width / worldW, size.height / WORLD_H)
        val ox = (size.width - worldW * u) / 2f
        val oy = (size.height - WORLD_H * u) / 2f
        val painter = AthletePainter(this, u, ox, oy, colors)
        when (kind) {
            ExerciseKind.PushUp -> painter.pushUp(t, showFloor)
            ExerciseKind.Squat -> painter.squat(t, showFloor)
        }
    }
}

private fun lerpF(a: Float, b: Float, t: Float) = a + (b - a) * t

/** Elbow / knee position from two fixed limb lengths (2-bone IK). */
private fun solveJoint(a: Offset, b: Offset, l1: Float, l2: Float, bendSign: Float): Offset {
    val dx = b.x - a.x; val dy = b.y - a.y
    val d = hypot(dx, dy).coerceIn(0.01f, l1 + l2 - 0.001f)
    val along = (l1 * l1 - l2 * l2 + d * d) / (2 * d)
    val h = sqrt((l1 * l1 - along * along).coerceAtLeast(0f))
    val mx = a.x + along * dx / d; val my = a.y + along * dy / d
    return Offset(mx + bendSign * h * (-dy / d), my + bendSign * h * (dx / d))
}

private class AthletePainter(
    val scope: DrawScope,
    val u: Float,
    val ox: Float,
    val oy: Float,
    colors: List<Color>
) {
    private val light = colors[0]
    private val base = colors.getOrElse(1) { colors[0] }
    private val deep = colors.last()
    // far-side limbs sit in shadow
    private val farLight = lerp(light, Color.Black, 0.30f)
    private val farBase = lerp(base, Color.Black, 0.38f)
    private val farDeep = lerp(deep, Color.Black, 0.40f)

    private fun p(o: Offset) = Offset(ox + o.x * u, oy + o.y * u)

    /** Tapered capsule from a (radius ra) to b (radius rb), in world units. */
    private fun capsulePath(a: Offset, b: Offset, ra: Float, rb: Float): Path {
        val pa = p(a); val pb = p(b)
        val rA = ra * u; val rB = rb * u
        val dx = pb.x - pa.x; val dy = pb.y - pa.y
        val len = hypot(dx, dy).coerceAtLeast(0.001f)
        val nx = -dy / len; val ny = dx / len
        val angN = Math.toDegrees(atan2(ny, nx).toDouble()).toFloat()
        return Path().apply {
            moveTo(pa.x + nx * rA, pa.y + ny * rA)
            lineTo(pb.x + nx * rB, pb.y + ny * rB)
            arcTo(Rect(pb.x - rB, pb.y - rB, pb.x + rB, pb.y + rB), angN, -180f, false)
            lineTo(pa.x - nx * rA, pa.y - ny * rA)
            arcTo(Rect(pa.x - rA, pa.y - rA, pa.x + rA, pa.y + rA), angN + 180f, -180f, false)
            close()
        }
    }

    /** A shaded limb: deep rim, base body, and a soft highlight on the lit (upper) side. */
    fun limb(a: Offset, b: Offset, ra: Float, rb: Float, far: Boolean = false) {
        val c0 = if (far) farDeep else deep
        val c1 = if (far) farBase else base
        val c2 = if (far) farLight else light
        scope.drawPath(capsulePath(a, b, ra, rb), c0)
        val o1 = 0.10f
        scope.drawPath(
            capsulePath(
                Offset(a.x, a.y - o1 * ra), Offset(b.x, b.y - o1 * rb),
                ra * 0.80f, rb * 0.80f
            ),
            c1
        )
        val o2 = 0.36f
        scope.drawPath(
            capsulePath(
                Offset(a.x, a.y - o2 * ra), Offset(b.x, b.y - o2 * rb),
                ra * 0.34f, rb * 0.34f
            ),
            Brush.linearGradient(
                listOf(c2, c2.copy(alpha = 0.55f)),
                start = p(a), end = p(b)
            )
        )
    }

    fun head(c: Offset, r: Float) {
        val pc = p(c); val rr = r * u
        scope.drawOval(deep, topLeft = Offset(pc.x - rr, pc.y - rr * 1.08f), size = Size(rr * 2, rr * 2.16f))
        val c1 = Offset(pc.x - 0.07f * rr, pc.y - 0.12f * rr); val r1 = rr * 0.82f
        scope.drawOval(base, topLeft = Offset(c1.x - r1, c1.y - r1 * 1.08f), size = Size(r1 * 2, r1 * 2.16f))
        val c2 = Offset(pc.x - 0.25f * rr, pc.y - 0.38f * rr); val r2 = rr * 0.36f
        scope.drawOval(
            Brush.radialGradient(listOf(light, light.copy(alpha = 0f)), center = c2, radius = r2 * 1.6f),
            topLeft = Offset(c2.x - r2 * 1.6f, c2.y - r2 * 1.6f), size = Size(r2 * 3.2f, r2 * 3.2f)
        )
    }

    fun ground(floorY: Float, shadowFrom: Float, shadowTo: Float, worldW: Float, show: Boolean) {
        if (show) {
            val top = p(Offset(0f, floorY))
            val bottom = p(Offset(worldW, WORLD_H))
            scope.drawRect(
                Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.05f), Color.Transparent), startY = top.y, endY = bottom.y),
                topLeft = top, size = Size(bottom.x - top.x, bottom.y - top.y)
            )
            scope.drawLine(
                Brush.horizontalGradient(
                    listOf(Color.Transparent, base.copy(alpha = 0.45f), Color.Transparent),
                    startX = top.x, endX = bottom.x
                ),
                start = top, end = Offset(bottom.x, top.y), strokeWidth = 0.18f * u
            )
        }
        // soft contact shadow
        val a = p(Offset(shadowFrom, floorY - 0.55f)); val b = p(Offset(shadowTo, floorY + 0.55f))
        scope.drawOval(
            Brush.radialGradient(
                listOf(Color.Black.copy(alpha = 0.75f), Color.Transparent),
                center = Offset((a.x + b.x) / 2, (a.y + b.y) / 2),
                radius = (b.x - a.x) / 2
            ),
            topLeft = a, size = Size(b.x - a.x, b.y - a.y)
        )
    }

    fun pushUp(t: Float, showFloor: Boolean) {
        val floor = 20.2f
        val toe = Offset(3.2f, 19.7f)
        val bodyLen = 15.2f
        val th = Math.toRadians(lerpF(21f, 6f, t).toDouble())
        val sh = Offset(toe.x + bodyLen * cos(th).toFloat(), toe.y - bodyLen * sin(th).toFloat())
        fun along(f: Float, lift: Float = 0f) = Offset(lerpF(toe.x, sh.x, f), lerpF(toe.y, sh.y, f) - lift)
        val ankle = along(0.08f, 0.35f)
        val knee = along(0.36f, 0.15f)
        val hip = along(0.60f, 0.25f)
        val chest = along(0.90f, 0.20f)
        val hand = Offset(sh.x + 0.4f, floor - 0.45f)
        val elbow = solveJoint(sh, hand, 3.6f, 3.4f, 1f)

        ground(floor, 2.6f, sh.x + 2.4f, PUSHUP_W, showFloor)

        // far side (behind, in shadow)
        val fsh = Offset(sh.x - 0.5f, sh.y - 0.3f)
        val fhand = Offset(hand.x - 0.7f, hand.y - 0.1f)
        val felbow = solveJoint(fsh, fhand, 3.6f, 3.4f, 1f)
        limb(fsh, felbow, 0.62f, 0.50f, far = true)
        limb(felbow, fhand, 0.50f, 0.38f, far = true)
        val fo = Offset(-0.3f, -0.2f)
        limb(hip + fo, knee + fo, 0.85f, 0.60f, far = true)
        limb(knee + fo, ankle + fo, 0.60f, 0.42f, far = true)

        // near leg + torso
        limb(knee, ankle, 0.62f, 0.42f)
        limb(ankle, toe, 0.42f, 0.32f)
        limb(hip, knee, 0.95f, 0.66f)
        limb(hip, chest, 1.15f, 1.45f)
        limb(chest, sh, 1.45f, 1.20f)

        // neck + head follow the body line
        val dx = sh.x - toe.x; val dy = sh.y - toe.y
        val l = hypot(dx, dy); val ux = dx / l; val uy = dy / l
        val neck = Offset(sh.x + ux * 1.3f + uy * 0.5f, sh.y + uy * 1.3f - ux * 0.5f)
        limb(sh, neck, 0.55f, 0.50f)
        head(Offset(sh.x + ux * 2.6f + uy * 0.9f, sh.y + uy * 2.6f - ux * 0.9f), 1.45f)

        // near arm + hand
        limb(sh, elbow, 0.70f, 0.55f)
        limb(elbow, hand, 0.55f, 0.40f)
        limb(hand, Offset(hand.x + 0.9f, hand.y + 0.05f), 0.38f, 0.30f)
    }

    fun squat(t: Float, showFloor: Boolean) {
        val floor = 22.0f
        val ankle = Offset(12.6f, 21.0f)
        val shin = 4.7f; val thigh = 4.9f; val torso = 5.2f
        val sl = Math.toRadians(lerpF(4f, 30f, t).toDouble())
        val tha = Math.toRadians(lerpF(-6f, -100f, t).toDouble())
        val tl = Math.toRadians(lerpF(3f, 38f, t).toDouble())
        val aa = Math.toRadians(lerpF(84f, 4f, t).toDouble())

        val knee = Offset(ankle.x + shin * sin(sl).toFloat(), ankle.y - shin * cos(sl).toFloat())
        val hip = Offset(knee.x + thigh * sin(tha).toFloat(), knee.y - thigh * cos(tha).toFloat())
        val sh = Offset(hip.x + torso * sin(tl).toFloat(), hip.y - torso * cos(tl).toFloat())
        val chest = Offset(lerpF(hip.x, sh.x, 0.62f), lerpF(hip.y, sh.y, 0.62f))
        val headC = Offset(sh.x + 2.3f * sin(tl).toFloat(), sh.y - 2.3f * cos(tl).toFloat())
        val elbow = Offset(sh.x + 2.9f * cos(aa).toFloat(), sh.y + 0.3f + 2.9f * sin(aa).toFloat())
        val wrist = Offset(elbow.x + 2.8f * cos(aa - 0.05).toFloat(), elbow.y + 2.8f * sin(aa - 0.05).toFloat())

        ground(floor, ankle.x - lerpF(3f, 4.2f, t), ankle.x + 3.6f, SQUAT_W, showFloor)

        // far side
        val o = Offset(-0.4f, -0.25f)
        limb(hip + o, knee + o, 0.95f, 0.62f, far = true)
        limb(knee + o, ankle + o, 0.60f, 0.42f, far = true)
        val ao = Offset(-0.3f, -0.2f)
        limb(sh + ao, elbow + ao, 0.60f, 0.50f, far = true)
        limb(elbow + ao, wrist + ao, 0.50f, 0.40f, far = true)

        // near leg, torso, head, arm
        limb(ankle, Offset(ankle.x + 2.0f, ankle.y + 0.55f), 0.45f, 0.32f)
        limb(knee, ankle, 0.65f, 0.45f)
        limb(hip, knee, 1.00f, 0.68f)
        limb(hip, chest, 1.15f, 1.40f)
        limb(chest, sh, 1.40f, 1.15f)
        val neck = Offset(lerpF(sh.x, headC.x, 0.5f), lerpF(sh.y, headC.y, 0.5f))
        limb(sh, neck, 0.55f, 0.50f)
        head(headC, 1.4f)
        limb(sh, elbow, 0.68f, 0.55f)
        limb(elbow, wrist, 0.55f, 0.42f)
    }
}
