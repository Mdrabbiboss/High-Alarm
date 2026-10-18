package com.programmer.highalarm

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class SquatCounterTest {

    private fun pt(x: Double, y: Double, l: Float = 0.95f) = PushUpCounter.Point(x.toFloat(), y.toFloat(), l)

    /**
     * Side view. Ankle fixed, shin leans forward, thigh goes back so the knee
     * angle equals `knee`. Torso stays upright-ish. `scale` simulates distance.
     */
    private fun sideBody(knee: Double, scale: Double = 1.0, noise: Random? = null): SquatCounter.Body {
        val j = { noise?.let { (it.nextDouble() - 0.5) * 6.0 } ?: 0.0 }
        val shin = 180.0 * scale; val thigh = 180.0 * scale; val torso = 220.0 * scale
        val bend = 180.0 - knee                     // total bend at the knee
        val shinLean = Math.toRadians(bend * 0.4)   // shin tilts forward
        val ankle = pt(300.0, 900.0)
        val kneeP = pt(ankle.x + sin(shinLean) * shin + j(), ankle.y - cos(shinLean) * shin + j())
        val thighDir = shinLean - Math.toRadians(bend) // thigh rotates backward
        val hip = pt(kneeP.x + sin(thighDir) * thigh + j(), kneeP.y - cos(thighDir) * thigh + j())
        val lean = Math.toRadians(bend * 0.35)
        val sh = pt(hip.x + sin(lean) * torso, hip.y - cos(lean) * torso)
        return SquatCounter.Body(sh, null, hip, null, kneeP, null, ankle, null)
    }

    /**
     * Facing the camera: the 2D knee angle barely changes, but the hips drop.
     */
    private fun frontBody(depth: Double, scale: Double = 1.0): SquatCounter.Body {
        val ankleY = 900.0
        val leg = 360.0 * scale
        val hipY = ankleY - leg * (1 - 0.5 * depth)
        val torso = 220.0 * scale * (1 - 0.15 * depth) // slight lean toward camera
        val kneeY = ankleY - 180.0 * scale * (1 - 0.15 * depth)
        val kneeOut = 30.0 * scale * depth
        fun side(x: Double, out: Double) = Triple(pt(x, hipY), pt(x + out, kneeY), pt(x, ankleY))
        val (lh, lk, la) = side(260.0, -kneeOut)
        val (rh, rk, ra) = side(340.0, kneeOut)
        return SquatCounter.Body(pt(250.0, hipY - torso), pt(350.0, hipY - torso), lh, rh, lk, rk, la, ra)
    }

    private fun run(c: SquatCounter, reps: Int, bottom: Double, periodMs: Int = 1800, body: (Double) -> SquatCounter.Body, startMs: Long = 0): Long {
        var t = startMs
        val frames = periodMs / 33
        repeat(10) { c.process(body(172.0), t); t += 33 }
        repeat(reps) {
            for (f in 0 until frames) {
                val ph = f.toDouble() / frames
                val k = 172 - (172 - bottom) * (1 - cos(2 * PI * ph)) / 2
                c.process(body(k), t); t += 33
            }
        }
        repeat(10) { c.process(body(172.0), t); t += 33 }
        return t
    }

    @Test fun countsSideViewSquats() {
        val c = SquatCounter(); run(c, 10, 85.0, body = { sideBody(it) }); assertEquals(10, c.reps)
    }

    @Test fun countsParallelSquatsWithNoise() {
        val c = SquatCounter(); val r = Random(3)
        run(c, 12, 100.0, body = { sideBody(it, noise = r) }); assertEquals(12, c.reps)
    }

    @Test fun countsFastSquats() {
        val c = SquatCounter(); run(c, 8, 90.0, periodMs = 1000, body = { sideBody(it) }); assertEquals(8, c.reps)
    }

    @Test fun ignoresQuarterSquats() {
        val c = SquatCounter(); run(c, 10, 145.0, body = { sideBody(it) }); assertEquals(0, c.reps)
    }

    @Test fun countsFrontViewSquats() {
        val c = SquatCounter()
        // map knee angle sweep to a 0..1 depth for the front-view model
        run(c, 6, 90.0, body = { k -> frontBody(((172 - k) / 82.0).coerceIn(0.0, 1.0)) })
        assertEquals(6, c.reps)
    }

    @Test fun walkingTowardCameraDoesNotCount() {
        val c = SquatCounter()
        var t = 0L
        // standing still, but getting closer and farther (scale 0.7 <-> 1.3) several times
        repeat(6) {
            for (f in 0 until 60) {
                val s = 1.0 + 0.3 * sin(2 * PI * f / 60.0)
                c.process(frontBody(0.0, s), t); t += 33
            }
        }
        assertEquals(0, c.reps)
    }

    @Test fun ignoresStandingJitter() {
        val c = SquatCounter(); val r = Random(9); var t = 0L
        repeat(300) { c.process(sideBody(170.0 + (r.nextDouble() - 0.5) * 16), t); t += 33 }
        assertEquals(0, c.reps)
    }

    @Test fun lyingDownDoesNotCount() {
        val c = SquatCounter(); var t = 0L
        // body horizontal: shoulders level with hips
        repeat(5) {
            for (k in listOf(170.0, 120.0, 80.0, 120.0, 170.0)) {
                repeat(8) {
                    val b = sideBody(k)
                    val flat = b.copy(leftShoulder = pt(b.leftHip!!.x - 220.0, b.leftHip.y.toDouble()))
                    c.process(flat, t); t += 33
                }
            }
        }
        assertEquals(0, c.reps)
    }

    @Test fun repMustStartStanding() {
        val c = SquatCounter(); var t = 0L
        for (k in 85..172 step 3) { c.process(sideBody(k.toDouble()), t); t += 33 }
        assertEquals(0, c.reps)
        run(c, 2, 85.0, body = { sideBody(it) }, startMs = t)
        assertEquals(2, c.reps)
    }

    @Test fun survivesDropout() {
        val c = SquatCounter()
        var t = run(c, 2, 85.0, body = { sideBody(it) })
        for (k in 172 downTo 85 step 4) { c.process(sideBody(k.toDouble()), t); t += 33 }
        val empty = SquatCounter.Body(null, null, null, null, null, null, null, null)
        repeat(10) { c.process(empty, t); t += 33 }
        for (k in 85..172 step 4) { c.process(sideBody(k.toDouble()), t); t += 33 }
        assertEquals(3, c.reps)
    }

    /** Facing the camera with knees hidden behind the 2D line: only the hip drop tells the story. */
    private fun frontHipOnly(depth: Double): SquatCounter.Body {
        val b = frontBody(depth)
        // put knees exactly between hips and ankles -> 2D knee angle stays ~180
        fun midPt(a: PushUpCounter.Point, c: PushUpCounter.Point) = pt(((a.x + c.x) / 2).toDouble(), ((a.y + c.y) / 2).toDouble())
        return b.copy(leftKnee = midPt(b.leftHip!!, b.leftAnkle!!), rightKnee = midPt(b.rightHip!!, b.rightAnkle!!))
    }

    @Test fun countsFrontViewFromHipDropAlone() {
        val c = SquatCounter()
        run(c, 5, 90.0, body = { k -> frontHipOnly(((172 - k) / 82.0).coerceIn(0.0, 1.0)) })
        assertEquals(5, c.reps)
    }

    @Test fun ignoresFrontViewHalfSquats() {
        val c = SquatCounter()
        run(c, 6, 90.0, body = { k -> frontHipOnly(0.45 * ((172 - k) / 82.0).coerceIn(0.0, 1.0)) })
        assertEquals(0, c.reps)
    }
}
