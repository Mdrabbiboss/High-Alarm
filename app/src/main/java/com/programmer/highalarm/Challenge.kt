package com.programmer.highalarm

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.*

sealed class Challenge(
    val id: String,
    val name: String,
    val description: String,
    val icon: ImageVector,
    val isPremium: Boolean = true,
) {
    object None : Challenge("none", "None", "Dismiss normally", Icons.Default.Bedtime, false)
    object Maths : Challenge("maths", "Maths", "Solve sums", Icons.Default.Functions, false)
    object AdvancedMaths : Challenge("adv_maths", "Advanced Maths", "For Maths Lovers", Icons.Default.Calculate, false)
    object QRCode : Challenge("qr_code", "QR Code", "Scan code", Icons.Default.QrCode, false)
    object Rewrite : Challenge("rewrite", "Rewrite", "Type phrase", Icons.Default.Edit, false)
    object Tap : Challenge("tap", "Tap", "Tap target", Icons.Default.TouchApp, false)
    object Charger : Challenge("charger", "Charger", "Plug in", Icons.Default.Bolt, false)
    object ClockReading : Challenge("clock", "Clock Reading", "Read time", Icons.Default.Schedule, false)
    object LightsOn : Challenge("lights", "Lights on", "Turn on Lights", Icons.Default.WbSunny, false)
    object Steps : Challenge("steps", "Steps", "Walk steps", Icons.AutoMirrored.Filled.DirectionsWalk)
    object Maze : Challenge("maze", "Maze", "Find exit", Icons.Default.Grid4x4)
    object Memory : Challenge("memory", "Memory", "Repeat tiles", Icons.Default.Place)
    object Shake : Challenge("shake", "Shake", "Shake phone", Icons.Default.Vibration)
    object SpellBee : Challenge("spell", "Spell Bee", "Spell words", Icons.Default.TextFields)
    object Puzzle : Challenge("puzzle", "Puzzle", "Slide tiles", Icons.Default.Extension)
    object ScanSink : Challenge("sink", "Scan Sink", "Use camera", Icons.Default.CameraAlt)
    object PushUp : Challenge("pushup", "Push Up", "Do push-ups", Icons.Default.FitnessCenter)
    object Squat : Challenge("squat", "Squat", "Do squats", Icons.Default.AccessibilityNew)
    object ScanObject : Challenge("object", "Scan Object", "Find object", Icons.Default.ImageSearch)
    object ReverseTyping : Challenge("rev_typing", "Reverse Typing", "Reverse text", Icons.Default.Keyboard)
    object AudioMemory : Challenge("audio", "Audio Memory", "Replay sounds", Icons.Default.Headset)
    object BalanceTest : Challenge("balance_test", "Balance Test", "Tilt phone to balance", Icons.Default.Explore)
    object SafeCode : Challenge("safe_code", "Safe Code", "Crack digital lock", Icons.Default.Lock)
    object ControlPanel : Challenge("control_panel", "Control Panel", "Configure system controls", Icons.Default.Tune)
}

fun getAllChallenges(): List<Challenge> = listOf(
    Challenge.None, Challenge.Maths, Challenge.AdvancedMaths, Challenge.QRCode,
    Challenge.Rewrite, Challenge.Tap, Challenge.Charger, Challenge.ClockReading,
    Challenge.LightsOn, Challenge.Steps, Challenge.Maze, Challenge.Memory,
    Challenge.Shake, Challenge.SpellBee, Challenge.Puzzle, Challenge.ScanSink,
    Challenge.PushUp, Challenge.Squat, Challenge.ScanObject, Challenge.ReverseTyping,
    Challenge.AudioMemory, Challenge.BalanceTest, Challenge.SafeCode, Challenge.ControlPanel,
)

object ObjectCategories {
    val list = listOf(
        "Sink", "Coffee Cup", "Shoe", "Plant", "Laptop", "Fruit", "Watch", 
        "Chair", "Television", "Phone", "Bicycle", "Clock", "Backpack", 
        "Sunglasses", "Bed", "Couch", "Flowerpot", "Bottle", "Lipstick", 
        "Newspaper", "Cap", "Fire", "Dog", "Cat", "Tie", "Sky", "Badminton", "Toy"
    ).sorted()
}
