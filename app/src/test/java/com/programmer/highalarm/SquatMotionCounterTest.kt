package com.programmer.highalarm

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class SquatMotionCounterTest {

    /** Device tilted by pitch/roll; returns world-up accel expressed in device axes. */
    private fun toDevice(up: Double, pitch: Double, roll: Double): FloatArray {
        // world up vector in device frame after rotation
        val ux = sin(roll) * cos(pitch); val uy = cos(roll) * cos(pitch); val uz = sin(pitch)
        return floatArrayOf((up * ux).toFloat(), (up * uy).toFloat(), (up * uz).toFloat())
    }

    private fun simulate(
        c: SquatMotionCounter,
        seconds: Double,
        pitch: Double = 0.3, roll: Double = 0.2,
        noise: Double = 0.25, bias: Double = 0.08,
        rnd: Random = Random(1),
        accelUp: (Double) -> Double
    ) {
        val hz = 50.0
        val g = toDevice(9.81, pitch, roll)
        var t = 0.0
        while (t < seconds) {
            val a = accelUp(t) + bias + (rnd.nextDouble() - 0.5) * 2 * noise
            c.process(toDevice(a, pitch, roll), g, (t * 1e9).toLong() + 1_000_000L)
            t += 1 / hz
        }
    }

    /** Squats as smooth position cycles of `depthM` metres every `periodS`. */
    private fun squats(depthM: Double, periodS: Double, startS: Double = 1.0, count: Int = 1000): (Double) -> Double = { t ->
        if (t < startS || t > startS + count * periodS) 0.0 else {
            val w = 2 * PI / periodS
            // p = -A(1-cos wt)/2  ->  a = -A w² cos(wt) / 2
            -depthM * w * w * cos(w * (t - startS)) / 2
        }
    }

    @Test fun countsSquatsHeldAtAnAngle() {
        val c = SquatMotionCounter()
        simulate(c, 1.0 + 10 * 2.2 + 2.0, accelUp = squats(0.35, 2.2, count = 10))
        assertEquals(10, c.reps)
    }

    @Test fun countsFasterSquats() {
        val c = SquatMotionCounter()
        simulate(c, 1.0 + 8 * 1.4 + 2.0, pitch = 1.2, roll = -0.4, accelUp = squats(0.30, 1.4, count = 8))
        assertEquals(8, c.reps)
    }

    @Test fun ignoresTinyBobbing() {
        val c = SquatMotionCounter()
        simulate(c, 20.0, accelUp = squats(0.06, 1.5))
        assertEquals(0, c.reps)
    }

    @Test fun ignoresHandShake() {
        val c = SquatMotionCounter()
        simulate(c, 15.0, accelUp = { t -> 4.0 * sin(2 * PI * 6 * t) })
        assertEquals(0, c.reps)
    }

    @Test fun ignoresNoiseAndBias() {
        val c = SquatMotionCounter()
        simulate(c, 30.0, noise = 0.4, bias = 0.25, accelUp = { 0.0 })
        assertEquals(0, c.reps)
    }

    @Test fun holdingStillAtTheBottomDoesNotCountUntilStandingUp() {
        val c = SquatMotionCounter()
        // go down 0.35 m over 1 s, hold 4 s, then stand up over 1 s
        val w = PI // half cycle per second
        simulate(c, 9.0, accelUp = { t ->
            when {
                t < 1.0 -> 0.0
                t < 2.0 -> -0.35 * w * w * cos(w * (t - 1.0)) / 2
                t < 6.0 -> 0.0
                t < 7.0 -> 0.35 * w * w * cos(w * (t - 6.0)) / 2
                else -> 0.0
            }
        })
        assertEquals(1, c.reps)
    }

    @Test fun countsSlowSquats() {
        val c = SquatMotionCounter()
        simulate(c, 1.0 + 6 * 3.5 + 2.0, accelUp = squats(0.35, 3.5, count = 6))
        assertEquals(6, c.reps)
    }

    @Test fun countsSquatsWithPausesBetween() {
        val c = SquatMotionCounter()
        val period = 2.0; val pause = 1.5
        val w = 2 * PI / period
        simulate(c, 1.0 + 5 * (period + pause) + 1.0, accelUp = { t ->
            if (t < 1.0) 0.0 else {
                val local = (t - 1.0) % (period + pause)
                val idx = ((t - 1.0) / (period + pause)).toInt()
                if (idx >= 5 || local > period) 0.0 else -0.35 * w * w * cos(w * local) / 2
            }
        })
        assertEquals(5, c.reps)
    }
}
