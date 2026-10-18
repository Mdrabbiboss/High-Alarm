package com.programmer.highalarm

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.min

/**
 * Squat rep counter driven by pose landmarks (ML Kit Pose Detection).
 * Same philosophy as [PushUpCounter]; pure Kotlin so it is unit tested.
 *
 *  1. Knee angle (hip–knee–ankle), confidence-weighted over the visible legs.
 *     Best when the camera sees you side-on.
 *  2. Hip drop: how far the hips come down toward the ankles, measured in
 *     torso lengths (so walking toward/away from the phone doesn't fake it).
 *     This works when you face the camera, where the 2D knee angle barely
 *     changes. The deeper of the two signals is used.
 *  3. Hysteresis: a rep is standing -> deep -> standing again, starting from
 *     standing. Half squats never reach the bottom zone.
 *  4. Posture gate: torso must be roughly upright with feet below hips (no
 *     counting while lying down or sitting on the floor).
 */
class SquatCounter(
    private val downDepth: Float = 0.60f,
    private val upDepth: Float = 0.25f,
) {
    data class Body(
        val leftShoulder: PushUpCounter.Point?, val rightShoulder: PushUpCounter.Point?,
        val leftHip: PushUpCounter.Point?, val rightHip: PushUpCounter.Point?,
        val leftKnee: PushUpCounter.Point?, val rightKnee: PushUpCounter.Point?,
        val leftAnkle: PushUpCounter.Point?, val rightAnkle: PushUpCounter.Point?,
    )

    data class State(
        val reps: Int,
        val stage: RepStage,
        val depth: Float,
        val kneeAngle: Float?,
        val repJustCounted: Boolean,
    )

    var reps = 0
        private set

    private var stage = RepStage.NO_BODY
    private var inBottom = false
    private var armedAtTop = false
    private var bottomArmed = false
    private var depthEma: Float? = null
    private var angleEma: Float? = null
    private var standingHipRatio: Float? = null
    private var bottomEnteredAt = 0L
    private var topLeftAt = 0L
    private var lastRepAt = 0L
    private var lastSeenAt = 0L

    fun reset(keepReps: Boolean = false) {
        if (!keepReps) reps = 0
        stage = RepStage.NO_BODY; inBottom = false; armedAtTop = false; bottomArmed = false
        depthEma = null; angleEma = null; standingHipRatio = null
        bottomEnteredAt = 0L; topLeftAt = 0L; lastRepAt = 0L; lastSeenAt = 0L
    }

    fun process(body: Body, timeMs: Long): State {
        val knee = kneeAngle(body)
        if (knee == null) {
            if (timeMs - lastSeenAt > LOST_MS) {
                stage = RepStage.NO_BODY
                depthEma = null; angleEma = null
                inBottom = false; armedAtTop = false
            }
            return snapshot(false)
        }
        lastSeenAt = timeMs

        if (!isUpright(body)) {
            stage = RepStage.NOT_IN_POSITION
            inBottom = false; armedAtTop = false
            depthEma = null; angleEma = null
            return snapshot(false)
        }

        val a = angleEma?.let { it + ANGLE_ALPHA * (knee - it) } ?: knee
        angleEma = a
        val kneeDepth = ((STRAIGHT_ANGLE - a) / (STRAIGHT_ANGLE - BENT_ANGLE)).coerceIn(0f, 1f)

        var hipDepth = 0f
        val ratio = hipRatio(body)
        if (ratio != null) {
            // Standing hip height: rises quickly to the tallest stance seen,
            // decays very slowly (only while the knees are straight) so a
            // partial squat can't drag the reference down.
            val base0 = standingHipRatio
            standingHipRatio = when {
                base0 == null -> ratio
                ratio > base0 -> base0 + 0.3f * (ratio - base0)
                kneeDepth < 0.1f -> base0 + 0.01f * (ratio - base0)
                else -> base0
            }
            val base = standingHipRatio
            if (base != null && base > 0.2f) {
                hipDepth = ((1f - ratio / base) / HIP_DROP_AT_BOTTOM).coerceIn(0f, 1f)
            }
        }
        val depth = maxOf(kneeDepth, hipDepth)
        val smoothed = depthEma?.let { it + DEPTH_ALPHA * (depth - it) } ?: depth
        depthEma = smoothed

        var counted = false
        if (!inBottom) {
            when {
                smoothed >= downDepth -> {
                    inBottom = true
                    bottomArmed = armedAtTop
                    armedAtTop = false
                    bottomEnteredAt = timeMs
                    stage = RepStage.BOTTOM
                }
                smoothed > upDepth -> {
                    if (stage == RepStage.TOP) topLeftAt = timeMs
                    stage = RepStage.LOWERING
                }
                else -> {
                    stage = RepStage.TOP
                    armedAtTop = true
                    topLeftAt = timeMs
                }
            }
        } else {
            if (smoothed <= upDepth) {
                val held = timeMs - bottomEnteredAt
                val repTime = timeMs - topLeftAt
                val gap = if (lastRepAt == 0L) Long.MAX_VALUE else timeMs - lastRepAt
                inBottom = false
                stage = RepStage.TOP
                armedAtTop = true
                topLeftAt = timeMs
                if (bottomArmed && held >= MIN_BOTTOM_MS && repTime >= MIN_REP_MS && gap >= MIN_GAP_MS) {
                    reps++
                    lastRepAt = timeMs
                    counted = true
                }
            } else {
                stage = RepStage.BOTTOM
            }
        }
        return snapshot(counted)
    }

    private fun snapshot(counted: Boolean) = State(reps, stage, depthEma ?: 0f, angleEma, counted)

    private fun visible(p: PushUpCounter.Point?) = p != null && p.likelihood >= PushUpCounter.MIN_LIKELIHOOD

    private fun kneeAngle(b: Body): Float? {
        var sum = 0f; var weight = 0f
        fun side(h: PushUpCounter.Point?, k: PushUpCounter.Point?, a: PushUpCounter.Point?) {
            if (!visible(h) || !visible(k) || !visible(a)) return
            val thigh = hypot(h!!.x - k!!.x, h.y - k.y)
            val shin = hypot(a!!.x - k.x, a.y - k.y)
            if (thigh < 4f || shin < 4f) return
            val conf = min(h.likelihood, min(k.likelihood, a.likelihood))
            sum += PushUpCounter.angle(h, k, a) * conf
            weight += conf
        }
        side(b.leftHip, b.leftKnee, b.leftAnkle)
        side(b.rightHip, b.rightKnee, b.rightAnkle)
        return if (weight > 0f) sum / weight else null
    }

    private fun mid(a: PushUpCounter.Point?, b: PushUpCounter.Point?): PushUpCounter.Point? = when {
        visible(a) && visible(b) -> PushUpCounter.Point((a!!.x + b!!.x) / 2, (a.y + b.y) / 2, min(a.likelihood, b.likelihood))
        visible(a) -> a
        visible(b) -> b
        else -> null
    }

    /** Vertical hip-above-ankle height measured in torso lengths. */
    private fun hipRatio(b: Body): Float? {
        val sh = mid(b.leftShoulder, b.rightShoulder) ?: return null
        val hip = mid(b.leftHip, b.rightHip) ?: return null
        val ankle = mid(b.leftAnkle, b.rightAnkle) ?: return null
        val torso = hypot(sh.x - hip.x, sh.y - hip.y)
        if (torso < 8f) return null
        return (ankle.y - hip.y) / torso
    }

    /** Torso not lying flat and ankles below the hips. */
    private fun isUpright(b: Body): Boolean {
        val hip = mid(b.leftHip, b.rightHip) ?: return true
        val ankle = mid(b.leftAnkle, b.rightAnkle)
        if (ankle != null && ankle.y < hip.y) return false
        val sh = mid(b.leftShoulder, b.rightShoulder) ?: return true
        val dx = sh.x - hip.x
        val dy = hip.y - sh.y // shoulders should be above hips (smaller y)
        if (dy <= 0f) return false
        val fromVertical = Math.toDegrees(atan2(abs(dx), dy).toDouble()).toFloat()
        return fromVertical < 70f // squats lean forward, but not lying down
    }

    companion object {
        const val STRAIGHT_ANGLE = 165f
        const val BENT_ANGLE = 90f
        const val HIP_DROP_AT_BOTTOM = 0.50f
        const val ANGLE_ALPHA = 0.5f
        const val DEPTH_ALPHA = 0.45f
        const val MIN_BOTTOM_MS = 80L
        const val MIN_REP_MS = 500L
        const val MIN_GAP_MS = 400L
        const val LOST_MS = 800L
    }
}

/** Shared stage names for the camera rep counters (push-up, squat). */
enum class RepStage { NO_BODY, NOT_IN_POSITION, TOP, LOWERING, BOTTOM }
