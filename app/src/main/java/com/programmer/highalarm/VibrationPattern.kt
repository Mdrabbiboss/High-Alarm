package com.programmer.highalarm

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

data class VibrationPattern(
    val name: String,
    val pattern: LongArray,
    val icon: ImageVector,
    val isPremium: Boolean = true,
    val amplitudes: IntArray? = null
) {
    companion object {
        val Default = VibrationPattern("Default", longArrayOf(0, 800, 400), Icons.Default.Smartphone, false)
        val Breeze = VibrationPattern("Breeze", longArrayOf(0, 150, 800), Icons.Default.Air)
        val Pulse = VibrationPattern(
            name = "Pulse",
            pattern = longArrayOf(0, 500, 150),
            icon = Icons.Default.Waves,
            isPremium = true,
            amplitudes = intArrayOf(0, 255, 0)
        )
        val Heartbeat = VibrationPattern(
            name = "Heartbeat",
            pattern = longArrayOf(0, 250, 100, 250, 500),
            icon = Icons.Default.Favorite,
            isPremium = true,
            amplitudes = intArrayOf(0, 255, 0, 255, 0)
        )
        val DoubleTap = VibrationPattern(
            name = "Double Tap",
            pattern = longArrayOf(0, 120, 120, 120, 800),
            icon = Icons.Default.TouchApp,
            isPremium = true,
            amplitudes = intArrayOf(0, 255, 0, 255, 0)
        )
        val Knock = VibrationPattern(
            name = "Knock",
            pattern = longArrayOf(0, 120, 100, 120, 250, 350, 900),
            icon = Icons.Default.MeetingRoom,
            isPremium = true,
            amplitudes = intArrayOf(0, 255, 0, 255, 0, 255, 0)
        )
        val Wave = VibrationPattern(
            name = "Wave",
            pattern = longArrayOf(0, 80, 80, 180, 80, 320, 80, 180, 80, 80, 900),
            icon = Icons.Default.GraphicEq,
            isPremium = true,
            amplitudes = intArrayOf(0, 80, 0, 170, 0, 255, 0, 170, 0, 80, 0)
        )
        val SOS = VibrationPattern(
            name = "SOS",
            pattern = longArrayOf(0, 200, 100, 200, 100, 200, 100, 600, 100, 600, 100, 600, 100, 200, 100, 200, 100, 200, 1000),
            icon = Icons.Default.SmsFailed,
            isPremium = true,
            amplitudes = intArrayOf(0, 255, 0, 255, 0, 255, 0, 255, 0, 255, 0, 255, 0, 255, 0, 255, 0, 255, 0)
        )
        val Blast = VibrationPattern(
            name = "Blast",
            pattern = longArrayOf(0, 2500, 200),
            icon = Icons.Default.LocalFireDepartment,
            isPremium = true,
            amplitudes = intArrayOf(0, 255, 0)
        )

        val all = listOf(Default, Breeze, Pulse, Heartbeat, DoubleTap, Knock, Wave, SOS, Blast)
        
        fun getByName(name: String): VibrationPattern = all.find { it.name == name } ?: Default
    }
}
