package com.programmer.highalarm

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

class SquatMotionDetector(
    context: Context,
    private val onSquat: () -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var isDown = false
    private var lastSquatTimestamp: Long = 0
    
    // Configurable state from UI
    var isGripLocked: Boolean = false

    // Tuning for high accuracy and anti-double-counting
    private val MIN_SQUAT_DURATION = 1200L // Minimum time for one full squat cycle
    private val DESCENT_THRESHOLD = 7.5f    // Clear downward acceleration
    private val ASCENT_THRESHOLD = 11.5f    // Strong upward push

    fun start() {
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        // --- Strict Grip Gating ---
        if (!isGripLocked) {
            isDown = false 
            return
        }

        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            val gForce = sqrt(x * x + y * y + z * z)
            val currentTime = System.currentTimeMillis()

            // State Machine for Down-Up Cycle
            if (!isDown && gForce < DESCENT_THRESHOLD) {
                isDown = true
            } else if (isDown && gForce > ASCENT_THRESHOLD) {
                if (currentTime - lastSquatTimestamp > MIN_SQUAT_DURATION) {
                    lastSquatTimestamp = currentTime
                    onSquat() 
                }
                isDown = false
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
