package com.programmer.highalarm

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Push-up rep counter driven by pose landmarks (ML Kit Pose Detection).
 *
 * Pure Kotlin (no Android types) so it can be unit tested on the JVM.
 *
 * How it decides:
 *  1. Elbow angle (shoulder–elbow–wrist) from whichever arm(s) the camera sees,
 *     weighted by landmark confidence. 165°+ = arms straight, ~90° = bottom.
 *  2. Front view (phone on the floor in front of the face, hips hidden): the
 *     shoulders get visibly wider as the chest drops toward the phone, so the
 *     apparent shoulder width is blended in as a second depth signal.
 *  3. The depth signal is smoothed, then a hysteresis state machine counts a
 *     rep only for a full cycle: straight arms -> deep bend -> straight arms.
 *  4. Posture gate: if hips are visible and the torso is upright and long
 *     (i.e. the person is standing), nothing counts – arm curls can't cheat.
 *  5. Timing gate: a rep must take at least [MIN_REP_MS]; a rep that bottoms
 *     out for less than [MIN_BOTTOM_MS] is ignored as jitter.
 */
class PushUpCounter(
    private val downDepth: Float = 0.60f,   // enter "bottom" above this depth
    private val upDepth: Float = 0.25f,     // back to "top" below this depth
) {

    /** One landmark in upright-image pixel coordinates. */
    data class Point(val x: Float, val y: Float, val likelihood: Float)

    /** Landmarks the counter needs. Any can be null when not detected. */
    data class Body(
        val nose: Point?,
        val leftShoulder: Point?, val rightShoulder: Point?,
        val leftElbow: Point?, val rightElbow: Point?,
        val leftWrist: Point?, val rightWrist: Point?,
        val leftHip: Point?, val rightHip: Point?,
        val leftKnee: Point?, val rightKnee: Point?,
    )

    enum class Stage {
        /** Arms / shoulders not visible. */
        NO_BODY,
        /** Visible but standing – not in a push-up position. */
        NOT_IN_POSITION,
        /** Arms straight at the top, waiting to go down. */
        TOP,
        /** Lowering. */
        LOWERING,
        /** Deep enough – now push back up. */
        BOTTOM,
    }

    data class State(
        val reps: Int,
        val stage: Stage,
        /** 0 = arms straight, 1 = full depth. Smoothed. */
        val depth: Float,
        /** Smoothed elbow angle in degrees, or null. */
        val elbowAngle: Float?,
        /** True when the last frame completed a rep. */
        val repJustCounted: Boolean,
        /** True when the front-view (shoulder width) signal was used. */
        val frontView: Boolean,
    )

    var reps = 0
        private set

    private var stage = Stage.NO_BODY
    private var inBottom = false
    private var depthEma: Float? = null
    private var angleEma: Float? = null
    private var bottomEnteredAt = 0L
    private var topLeftAt = 0L
    private var lastRepAt = 0L
    private var lastSeenAt = 0L
    private var armedAtTop = false
    private var bottomArmed = false

    // Baseline shoulder width measured while arms are straight (front view).
    private var topShoulderWidth: Float? = null

    /** Clears tracking. With [keepReps] the rep count survives (e.g. camera switch). */
    fun reset(keepReps: Boolean = false) {
        if (!keepReps) reps = 0
        stage = Stage.NO_BODY; inBottom = false
        depthEma = null; angleEma = null; topShoulderWidth = null
        bottomEnteredAt = 0L; topLeftAt = 0L; lastRepAt = 0L; lastSeenAt = 0L
        armedAtTop = false; bottomArmed = false
    }

    fun process(body: Body, timeMs: Long): State {
        val arm = elbowAngle(body)
        if (arm == null) {
            // Keep the rep in progress through short dropouts (motion blur).
            if (timeMs - lastSeenAt > LOST_MS) {
                stage = Stage.NO_BODY
                depthEma = null; angleEma = null
                inBottom = false
                armedAtTop = false
            }
            return snapshot(false, false)
        }
        lastSeenAt = timeMs

        // ---- Posture gate -------------------------------------------------
        if (isStanding(body)) {
            stage = Stage.NOT_IN_POSITION
            inBottom = false
            armedAtTop = false
            depthEma = null; angleEma = null
            return snapshot(false, false)
        }

        // ---- Depth signals ------------------------------------------------
        val smoothedAngle = angleEma?.let { it + ANGLE_ALPHA * (arm - it) } ?: arm
        angleEma = smoothedAngle
        val armDepth = ((STRAIGHT_ANGLE - smoothedAngle) / (STRAIGHT_ANGLE - BENT_ANGLE)).coerceIn(0f, 1f)

        val hipsVisible = visible(body.leftHip) || visible(body.rightHip)
        val width = shoulderWidth(body)
        var frontView = false
        var depth = armDepth
        if (!hipsVisible && width != null) {
            // Learn the "arms straight" shoulder width (slowly, only at the top).
            if (armDepth < 0.2f) {
                topShoulderWidth = topShoulderWidth?.let { it + 0.1f * (width - it) } ?: width
            }
            val base = topShoulderWidth
            if (base != null && base > 1f) {
                val widthDepth = ((width / base - 1f) / WIDTH_GROWTH_AT_BOTTOM).coerceIn(0f, 1f)
                depth = 0.45f * armDepth + 0.55f * widthDepth
                frontView = true
            }
        }

        val smoothed = depthEma?.let { it + DEPTH_ALPHA * (depth - it) } ?: depth
        depthEma = smoothed

        // Front view: arms bending alone can't pass (max 0.45), the chest must
        // really come toward the phone, so a slightly lower bar is safe.
        val enterBottom = if (frontView) min(downDepth, FRONT_DOWN_DEPTH) else downDepth

        // ---- Hysteresis state machine ------------------------------------
        // A rep = TOP (arms straight) -> BOTTOM (deep) -> TOP again.
        var counted = false
        if (!inBottom) {
            when {
                smoothed >= enterBottom -> {
                    inBottom = true
                    bottomArmed = armedAtTop      // only reps that started at the top count
                    armedAtTop = false
                    bottomEnteredAt = timeMs
                    stage = Stage.BOTTOM
                }
                smoothed > upDepth -> {
                    if (stage == Stage.TOP) topLeftAt = timeMs
                    stage = Stage.LOWERING
                }
                else -> {
                    stage = Stage.TOP
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
                stage = Stage.TOP
                armedAtTop = true
                topLeftAt = timeMs
                if (bottomArmed && held >= MIN_BOTTOM_MS && repTime >= MIN_REP_MS && gap >= MIN_GAP_MS) {
                    reps++
                    lastRepAt = timeMs
                    counted = true
                }
            } else {
                stage = Stage.BOTTOM
            }
        }
        return snapshot(counted, frontView)
    }

    private fun snapshot(counted: Boolean, frontView: Boolean) = State(
        reps = reps,
        stage = stage,
        depth = depthEma ?: 0f,
        elbowAngle = angleEma,
        repJustCounted = counted,
        frontView = frontView,
    )

    // ---------------------------------------------------------------------

    private fun visible(p: Point?) = p != null && p.likelihood >= MIN_LIKELIHOOD

    /** Confidence-weighted elbow angle over the visible arm(s). */
    private fun elbowAngle(b: Body): Float? {
        var sum = 0f
        var weight = 0f
        fun side(s: Point?, e: Point?, w: Point?) {
            if (!visible(s) || !visible(e) || !visible(w)) return
            val conf = min(s!!.likelihood, min(e!!.likelihood, w!!.likelihood))
            // Ignore degenerate (folded on itself) detections.
            val upper = hypot(s.x - e.x, s.y - e.y)
            val fore = hypot(w.x - e.x, w.y - e.y)
            if (upper < 4f || fore < 4f) return
            sum += angle(s, e, w) * conf
            weight += conf
        }
        side(b.leftShoulder, b.leftElbow, b.leftWrist)
        side(b.rightShoulder, b.rightElbow, b.rightWrist)
        return if (weight > 0f) sum / weight else null
    }

    private fun shoulderWidth(b: Body): Float? {
        val l = b.leftShoulder; val r = b.rightShoulder
        if (!visible(l) || !visible(r)) return null
        return hypot(l!!.x - r!!.x, l.y - r.y)
    }

    /**
     * Standing = hips visible, torso close to vertical AND long compared with
     * the shoulders. A plank seen from the side is horizontal; seen from the
     * front it is heavily foreshortened (short), so neither is "standing".
     */
    private fun isStanding(b: Body): Boolean {
        val pairs = listOf(b.leftShoulder to b.leftHip, b.rightShoulder to b.rightHip)
            .filter { (s, h) -> visible(s) && visible(h) }
        if (pairs.isEmpty()) return false
        val (s, h) = pairs.maxBy { (s, h) -> min(s!!.likelihood, h!!.likelihood) }
        val dx = h!!.x - s!!.x
        val dy = h.y - s.y
        val torso = hypot(dx, dy)
        val fromHorizontal = Math.toDegrees(atan2(abs(dy), abs(dx)).toDouble()).toFloat()
        val width = shoulderWidth(b) ?: (torso / 1.6f)
        return fromHorizontal > 55f && torso > width * 1.15f
    }

    companion object {
        const val MIN_LIKELIHOOD = 0.5f
        const val STRAIGHT_ANGLE = 165f
        const val BENT_ANGLE = 85f
        const val WIDTH_GROWTH_AT_BOTTOM = 0.22f
        const val FRONT_DOWN_DEPTH = 0.52f
        const val ANGLE_ALPHA = 0.5f
        const val DEPTH_ALPHA = 0.45f
        const val MIN_BOTTOM_MS = 80L
        const val MIN_REP_MS = 450L
        const val MIN_GAP_MS = 350L
        const val LOST_MS = 700L

        fun angle(a: Point, mid: Point, c: Point): Float {
            val v1x = a.x - mid.x; val v1y = a.y - mid.y
            val v2x = c.x - mid.x; val v2y = c.y - mid.y
            val dot = v1x * v2x + v1y * v2y
            val n = sqrt(v1x * v1x + v1y * v1y) * sqrt(v2x * v2x + v2y * v2y)
            if (n == 0f) return 180f
            return Math.toDegrees(acos((dot / n).coerceIn(-1f, 1f).toDouble())).toFloat()
        }
    }
}
