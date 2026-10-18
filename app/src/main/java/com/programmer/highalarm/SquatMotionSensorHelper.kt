package com.programmer.highalarm

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

object SquatMotionSensorHelper {
    fun getSquatMotionFlow(context: Context): Flow<Boolean> = callbackFlow {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)

        var isMovingDown = false
        var hasReachedBottom = false
        
        // Robust thresholds for detecting physical movement
        // Lowered slightly to be more responsive but still filtered
        val downThreshold = -1.8f 
        val upThreshold = 1.8f
        
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event?.sensor?.type == Sensor.TYPE_LINEAR_ACCELERATION) {
                    val y = event.values[1] // Y axis (vertical relative to device)
                    
                    // State machine for high-accuracy squat detection:
                    // 1. Initial State: isMovingDown=false, hasReachedBottom=false
                    // 2. Start Descent: y < downThreshold -> isMovingDown=true
                    // 3. Reach Bottom: y > upThreshold (braking force) while isMovingDown=true -> hasReachedBottom=true, trySend(true)
                    // 4. Ascent: y > upThreshold (acceleration up) while hasReachedBottom=true
                    // 5. Reach Top: y < downThreshold (braking at top) while hasReachedBottom=true -> Reset, trySend(false)
                    
                    if (!isMovingDown && !hasReachedBottom && y < downThreshold) {
                        isMovingDown = true
                    } else if (isMovingDown && !hasReachedBottom && y > upThreshold) {
                        // Detected the "brake" at the bottom of the squat
                        hasReachedBottom = true
                        isMovingDown = false
                        trySend(true) // Emit "Down" (reached bottom)
                    } else if (hasReachedBottom && !isMovingDown && y < downThreshold) {
                        // Detected the "brake" at the top of the squat
                        hasReachedBottom = false
                        trySend(false) // Emit "Up" (returned to start)
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (accelerometer != null) {
            sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME) // Fast sampling
        }

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }
}
