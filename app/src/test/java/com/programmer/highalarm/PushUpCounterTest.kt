package com.programmer.highalarm

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class PushUpCounterTest {


    private fun pt(x: Double, y: Double, l: Float = 0.95f) = PushUpCounter.Point(x.toFloat(), y.toFloat(), l)

    /** Elbow at `elbow`, upper arm pointing to shoulder, forearm rotated by `angleDeg`. */
    private fun wristFor(shoulder: PushUpCounter.Point, elbow: PushUpCounter.Point, angleDeg: Double, len: Double, flip: Boolean = false): PushUpCounter.Point {
        val ux = (shoulder.x - elbow.x).toDouble(); val uy = (shoulder.y - elbow.y).toDouble()
        val n = Math.hypot(ux, uy)
        val a = Math.toRadians(if (flip) -angleDeg else angleDeg)
        val rx = (ux * cos(a) - uy * sin(a)) / n
        val ry = (ux * sin(a) + uy * cos(a)) / n
        return pt(elbow.x + rx * len, elbow.y + ry * len)
    }

    /** Side view plank: torso horizontal, one arm visible. */
    private fun sideBody(elbowAngle: Double, noise: Random? = null): PushUpCounter.Body {
        val j = { noise?.let { (it.nextDouble() - 0.5) * 6.0 } ?: 0.0 }
        // Chest lowers as arms bend.
        val drop = (165 - elbowAngle).coerceAtLeast(0.0) * 0.6
        val shoulder = pt(300.0 + j(), 200.0 + drop + j())
        val elbow = pt(300.0 + j(), 280.0 + j())
        val wrist = wristFor(shoulder, elbow, elbowAngle, 75.0)
        return PushUpCounter.Body(
            nose = pt(260.0, 190.0 + drop),
            leftShoulder = shoulder, rightShoulder = null,
            leftElbow = elbow, rightElbow = null,
            leftWrist = wrist, rightWrist = null,
            leftHip = pt(500.0, 215.0 + drop * 0.5), rightHip = null,
            leftKnee = pt(650.0, 225.0), rightKnee = null,
        )
    }

    /** Standing person doing arm curls in front of the camera. */
    private fun standingCurl(elbowAngle: Double): PushUpCounter.Body {
        val ls = pt(260.0, 200.0); val rs = pt(340.0, 200.0)
        val le = pt(255.0, 290.0); val re = pt(345.0, 290.0)
        return PushUpCounter.Body(
            nose = pt(300.0, 140.0),
            leftShoulder = ls, rightShoulder = rs,
            leftElbow = le, rightElbow = re,
            leftWrist = wristFor(ls, le, elbowAngle, 80.0), rightWrist = wristFor(rs, re, elbowAngle, 80.0, flip = true),
            leftHip = pt(270.0, 450.0), rightHip = pt(330.0, 450.0),
            leftKnee = pt(270.0, 600.0), rightKnee = pt(330.0, 600.0),
        )
    }

    /** Phone on the floor in front of the face: hips hidden, shoulders widen when going down. */
    private fun frontBody(elbowAngle: Double): PushUpCounter.Body {
        val depth = ((165 - elbowAngle) / 80.0).coerceIn(0.0, 1.0)
        val half = 50.0 * (1 + 0.22 * depth)
        val ls = pt(300 - half, 300.0); val rs = pt(300 + half, 300.0)
        val le = pt(300 - half - 20 - 30 * depth, 380.0); val re = pt(300 + half + 20 + 30 * depth, 380.0)
        return PushUpCounter.Body(
            nose = pt(300.0, 250.0),
            leftShoulder = ls, rightShoulder = rs,
            leftElbow = le, rightElbow = re,
            leftWrist = wristFor(ls, le, elbowAngle, 80.0, flip = true), rightWrist = wristFor(rs, re, elbowAngle, 80.0),
            leftHip = null, rightHip = null, leftKnee = null, rightKnee = null,
        )
    }

    /** Runs `reps` cosine cycles 168° -> bottom -> 168° at 30 fps. */
    private fun run(
        counter: PushUpCounter,
        reps: Int,
        bottom: Double,
        periodMs: Int = 1400,
        body: (Double) -> PushUpCounter.Body,
        startMs: Long = 0,
    ): Long {
        var t = startMs
        val frames = periodMs / 33
        // hold at top first
        repeat(10) { counter.process(body(168.0), t); t += 33 }
        repeat(reps) {
            for (f in 0 until frames) {
                val phase = f.toDouble() / frames
                val angle = 168 - (168 - bottom) * (1 - cos(2 * PI * phase)) / 2
                counter.process(body(angle), t)
                t += 33
            }
        }
        repeat(10) { counter.process(body(168.0), t); t += 33 }
        return t
    }

    @Test fun countsFullSideViewPushUps() {
        val c = PushUpCounter()
        run(c, 10, bottom = 85.0, body = { sideBody(it) })
        assertEquals(10, c.reps)
    }

    @Test fun countsSlightlyShallowButRealPushUps() {
        val c = PushUpCounter()
        run(c, 8, bottom = 100.0, body = { sideBody(it) })
        assertEquals(8, c.reps)
    }

    @Test fun countsWithLandmarkNoise() {
        val c = PushUpCounter()
        val rnd = Random(42)
        run(c, 12, bottom = 88.0, body = { sideBody(it, rnd) })
        assertEquals(12, c.reps)
    }

    @Test fun countsFastPushUps() {
        val c = PushUpCounter()
        run(c, 10, bottom = 90.0, periodMs = 800, body = { sideBody(it) })
        assertEquals(10, c.reps)
    }

    @Test fun ignoresHalfReps() {
        val c = PushUpCounter()
        run(c, 10, bottom = 135.0, body = { sideBody(it) })
        assertEquals(0, c.reps)
    }

    @Test fun ignoresJitterAtTop() {
        val c = PushUpCounter()
        val rnd = Random(7)
        var t = 0L
        repeat(300) {
            c.process(sideBody(168.0 + (rnd.nextDouble() - 0.5) * 20), t); t += 33
        }
        assertEquals(0, c.reps)
    }

    @Test fun standingArmCurlsDoNotCount() {
        val c = PushUpCounter()
        run(c, 10, bottom = 60.0, body = { standingCurl(it) })
        assertEquals(0, c.reps)
    }

    @Test fun countsFrontViewPushUps() {
        val c = PushUpCounter()
        run(c, 6, bottom = 90.0, body = { frontBody(it) })
        assertEquals(6, c.reps)
    }

    @Test fun survivesShortDetectionDropout() {
        val c = PushUpCounter()
        var t = run(c, 3, bottom = 85.0, body = { sideBody(it) })
        // go down
        for (a in 168 downTo 85 step 4) { c.process(sideBody(a.toDouble()), t); t += 33 }
        // 300 ms with nothing detected (motion blur)
        val empty = PushUpCounter.Body(null, null, null, null, null, null, null, null, null, null, null)
        repeat(9) { c.process(empty, t); t += 33 }
        for (a in 85..168 step 4) { c.process(sideBody(a.toDouble()), t); t += 33 }
        assertEquals(4, c.reps)
    }

    @Test fun repMustStartFromTop() {
        val c = PushUpCounter()
        var t = 0L
        // first frames already at the bottom, then push up once
        for (a in 85..168 step 3) { c.process(sideBody(a.toDouble()), t); t += 33 }
        assertEquals(0, c.reps)
        // a proper rep afterwards counts
        run(c, 1, bottom = 85.0, body = { sideBody(it) }, startMs = t)
        assertEquals(1, c.reps)
    }

    /** Front view: bending the arms without the chest moving toward the phone must not count. */
    @Test fun frontViewArmBendWithoutChestDropDoesNotCount() {
        val c = PushUpCounter()
        val body = { angle: Double ->
            val b = frontBody(angle)
            // keep shoulders at their "top" width
            b.copy(leftShoulder = pt(250.0, 300.0), rightShoulder = pt(350.0, 300.0))
        }
        run(c, 8, bottom = 90.0, body = body)
        assertEquals(0, c.reps)
    }
}
