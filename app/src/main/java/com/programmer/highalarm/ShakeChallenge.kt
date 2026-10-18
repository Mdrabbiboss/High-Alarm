package com.programmer.highalarm

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.abs
import kotlin.math.sqrt

class ShakeDetector(
    context: Context,
    private val onShake: (count: Int) -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var shakeCount = 0
    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f
    
    // Sensitivity tuning - Optimized for intentional shaking
    private val SHAKE_THRESHOLD = 20.0f // m/s^2 above gravity (approx 3.0G total)
    private val MIN_INTERVAL = 300 // ms between counts (prevents accidental noise)
    private var lastShakeTimestamp: Long = 0

    fun start() {
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val curTime = System.currentTimeMillis()
            
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            // Calculate total acceleration magnitude
            val gForce = sqrt(x * x + y * y + z * z)
            val acceleration = abs(gForce - SensorManager.GRAVITY_EARTH)

            if (acceleration > SHAKE_THRESHOLD) {
                if (curTime - lastShakeTimestamp > MIN_INTERVAL) {
                    lastShakeTimestamp = curTime
                    shakeCount++
                    onShake(shakeCount)
                }
            }

            lastX = x
            lastY = y
            lastZ = z
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
