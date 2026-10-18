package com.programmer.highalarm

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Squat counter for "hold the phone" mode, using the motion sensors.
 *
 * The old version looked at the phone's own Y axis, which only works if the
 * phone is held perfectly upright. Here every sample is projected onto the
 * real-world vertical (via the gravity vector), so the phone can be held at
 * any angle.
 *
 * A squat leaves a fixed fingerprint in vertical acceleration:
 *   down-start (−)  ->  braking at the bottom + push up (+)  ->  braking at the top (−)
 * The counter splits the signal into these "lobes" and measures the speed
 * change of each. A rep needs the full − / + / − sequence with real speed
 * AND a real drop of the phone (integrated within the rep only, so sensor
 * drift has no time to build up). Hand shakes, tiny bobbing, noise and slow
 * drift never satisfy all of that.
 */
class SquatMotionCounter {

    data class State(val reps: Int, val stage: RepStage, val depth: Float, val repJustCounted: Boolean)

    var reps = 0
        private set

    private var lastT = 0L
    private var bias = 0f
    private var af = 0f
    private var warmupMs = 0L

    // current lobe
    private var lobeSign = 0
    private var lobeDv = 0f
    private var lobeStartMs = 0L
    private var neutralMs = 0f

    // rep progress: 0 = waiting, 1 = going down seen, 2 = coming up seen
    private var phase = 0
    private var repStartMs = 0L
    private var upDv = 0f
    private var vRep = 0f
    private var pRep = 0f
    private var minP = 0f
    private var aSlow = 0f
    private var stillMs = 0f
    private var nextV = 0f
    private var nextP = 0f
    private var nextTracking = false
    private var nextPeak = 0f
    private var lastUpDv = 0f
    private var specV = 0f
    private var specP = 0f
    private var specMin = 0f
    private var lastRepMs = 0L

    fun reset(keepReps: Boolean = false) {
        if (!keepReps) reps = 0
        lastT = 0L; bias = 0f; af = 0f; warmupMs = 0L
        lobeSign = 0; lobeDv = 0f; lobeStartMs = 0L; neutralMs = 0f
        phase = 0; repStartMs = 0L; upDv = 0f; vRep = 0f; pRep = 0f; minP = 0f; lastRepMs = 0L
        aSlow = 0f; stillMs = 0f; nextV = 0f; nextP = 0f; nextTracking = false
    }

    /**
     * @param linear linear acceleration (gravity removed), device axes, m/s²
     * @param gravity gravity vector, device axes
     * @param timeNs sample timestamp in nanoseconds
     */
    fun process(linear: FloatArray, gravity: FloatArray, timeNs: Long): State {
        val gNorm = sqrt(gravity[0] * gravity[0] + gravity[1] * gravity[1] + gravity[2] * gravity[2])
        if (gNorm < 1f) return snapshot(false)
        val aUp = (linear[0] * gravity[0] + linear[1] * gravity[1] + linear[2] * gravity[2]) / gNorm

        if (lastT == 0L) { lastT = timeNs; bias = aUp; return snapshot(false) }
        val dt = ((timeNs - lastT) / 1e9f).coerceIn(0f, 0.05f)
        lastT = timeNs
        if (dt <= 0f) return snapshot(false)
        val tMs = timeNs / 1_000_000L

        // slow bias removal + light low-pass (~3 Hz) against hand tremor
        if (warmupMs < WARMUP_MS) warmupMs += (dt * 1000).toLong()
        val biasTau = if (warmupMs < WARMUP_MS) 0.3f else BIAS_TAU_S
        bias += (aUp - bias) * (dt / biasTau).coerceAtMost(1f)
        val a = aUp - bias
        af += (a - af) * (dt / (dt + LPF_TAU_S))
        aSlow += (a - aSlow) * (dt / (dt + STILL_LPF_TAU_S))
        if (abs(aSlow) < STILL_ACCEL) stillMs += dt * 1000f else stillMs = 0f

        // in-rep integration for the depth check (short window => tiny drift)
        if (phase > 0) {
            // Zero-velocity update: a long quiet stretch means the phone is
            // held still (e.g. pausing at the bottom), so speed is zero.
            if (stillMs > STILL_MS) vRep = 0f
            vRep += af * dt
            pRep += vRep * dt
            if (nextTracking) {
                nextV += af * dt; nextP += nextV * dt
                if (nextP > nextPeak) nextPeak = nextP
            }
            if (phase == 1 && pRep < minP) minP = pRep
            if (tMs - repStartMs > REP_TIMEOUT_MS) phase = 0
        }

        // ---- lobe segmentation ------------------------------------------
        var counted = false
        val sign = when {
            af > LOBE_DEADBAND -> 1
            af < -LOBE_DEADBAND -> -1
            else -> 0
        }
        if (sign != 0 && sign != lobeSign) {
            if (lobeSign != 0) counted = closeLobe(lobeSign, lobeDv, lobeStartMs, tMs)
            lobeSign = sign; lobeDv = 0f; lobeStartMs = tMs
            if (sign < 0 && phase == 0) { specV = 0f; specP = 0f; specMin = 0f }
            // Coming up and starting to brake: the phone is mid-way up, moving
            // at about half the push it just got. Track the next squat from
            // here so its depth is measured from the real top.
            if (sign < 0 && phase == 2) { nextTracking = true; nextV = lastUpDv / 2f; nextP = 0f; nextPeak = 0f }
        }
        if (phase == 0 && lobeSign < 0) {
            specV += af * dt; specP += specV * dt
            if (specP < specMin) specMin = specP
        }
        if (sign == 0) {
            neutralMs += dt * 1000f
            if (lobeSign != 0 && neutralMs > NEUTRAL_CLOSE_MS) {
                counted = closeLobe(lobeSign, lobeDv, lobeStartMs, tMs) || counted
                lobeSign = 0; lobeDv = 0f
            }
        } else {
            neutralMs = 0f
        }
        if (lobeSign != 0) lobeDv += af * dt

        return snapshot(counted)
    }

    /** Returns true when this lobe completed a rep. */
    private fun closeLobe(sign: Int, dv: Float, startMs: Long, nowMs: Long): Boolean {
        var counted = false
        if (sign < 0 && dv <= -DOWN_DV) {
            if (phase == 2) {
                val gap = if (lastRepMs == 0L) Long.MAX_VALUE else nowMs - lastRepMs
                if (upDv >= UP_DV && minP <= -MIN_DROP_M &&
                    nowMs - repStartMs >= MIN_REP_MS && gap >= MIN_GAP_MS
                ) {
                    reps++
                    lastRepMs = nowMs
                    counted = true
                }
            }
            // This braking lobe can also be the start of the next squat.
            // A lobe that merges "braking at the top" with "starting down"
            // ends mid-descent at about half its speed change.
            when {
                phase == 2 && nextTracking -> startRepFrom(startMs, nextV, nextP - nextPeak)
                phase == 0 -> startRepFrom(startMs, specV, specP)
                else -> startRep(startMs, dv)
            }
        } else if (sign > 0 && phase >= 1 && dv > 0f) {
            upDv += dv
            lastUpDv = dv
            if (upDv >= UP_DV) phase = 2
        }
        return counted
    }

    private fun startRepFrom(startMs: Long, v: Float, p: Float) {
        phase = 1
        repStartMs = startMs
        upDv = 0f
        vRep = v; pRep = p; minP = minOf(0f, p)
        nextTracking = false
    }

    private fun startRep(startMs: Long, speedNow: Float) {
        nextTracking = false
        phase = 1
        repStartMs = startMs
        upDv = 0f
        // We only learn about the lobe when it ends; seed the in-rep track
        // with the current (downward) speed and the drop it implies so far.
        vRep = speedNow.coerceAtMost(0f)
        pRep = vRep * 0.25f
        minP = pRep
    }

    private fun snapshot(counted: Boolean): State {
        val stage = when (phase) {
            0 -> RepStage.TOP
            1 -> if (-minP >= MIN_DROP_M) RepStage.BOTTOM else RepStage.LOWERING
            else -> RepStage.BOTTOM
        }
        val depth = if (phase == 0) 0f else (-pRep / FULL_DEPTH_M).coerceIn(0f, 1f)
        return State(reps, stage, depth, counted)
    }

    companion object {
        const val DOWN_DV = 0.20f        // m/s gained going down (and braking at the top)
        const val UP_DV = 0.35f          // m/s of upward push
        const val MIN_DROP_M = 0.13f     // phone must drop at least 10 cm
        const val FULL_DEPTH_M = 0.30f
        const val LOBE_DEADBAND = 0.08f
        const val NEUTRAL_CLOSE_MS = 160f
        const val LPF_TAU_S = 0.05f
        const val BIAS_TAU_S = 6f
        const val MIN_REP_MS = 500L
        const val MIN_GAP_MS = 500L
        const val REP_TIMEOUT_MS = 8000L
        const val WARMUP_MS = 800L
        const val STILL_MS = 500f
        const val STILL_ACCEL = 0.12f
        const val STILL_LPF_TAU_S = 0.15f
    }
}
