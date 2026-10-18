package com.programmer.highalarm

import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Rational
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale

/**
 * Floating (Picture-in-Picture) coach for the "Display over other apps"
 * permission. It opens the system screen, then floats above it as a small
 * premium window that shows — and says out loud — exactly what to tap on
 * this phone's brand. The moment the permission is on, it takes the user
 * back to High Alarm by itself.
 *
 * PiP needs no special permission, so it works before the overlay is granted.
 */
class OverlayGuideActivity : ComponentActivity() {

    private var tts: TextToSpeech? = null
    @Volatile private var ttsReady = false
    @Volatile private var pendingSpeech: CompletableDeferred<Unit>? = null
    private val inPip = mutableStateOf(false)
    private val stepIndex = mutableIntStateOf(0)
    private val granted = mutableStateOf(false)
    private var started = false
    private lateinit var guide: OverlayGuide

    private val pipSupported: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        guide = OverlayGuide.forThisDevice()

        tts = TextToSpeech(this) { status ->
            if (status != TextToSpeech.SUCCESS) return@TextToSpeech
            val engine = tts ?: return@TextToSpeech
            engine.language = Locale.US
            engine.setSpeechRate(0.95f)
            engine.setPitch(1.0f)
            engine.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) { pendingSpeech?.complete(Unit) }
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) { pendingSpeech?.complete(Unit) }
                override fun onStop(utteranceId: String?, interrupted: Boolean) { pendingSpeech?.complete(Unit) }
            })
            ttsReady = true
        }

        if (pipSupported && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // pressing Home while the guide is expanded shrinks it back into the floating window
            try { setPictureInPictureParams(pipParams(autoEnter = true)) } catch (_: Exception) { }
        }

        setContent {
            val pip by inPip
            val step by stepIndex
            val done by granted
            OverlayGuideScreen(
                guide = guide,
                step = step,
                granted = done,
                compact = pip,
                onOpenSettings = { openSettings() },
                onClose = { returnToApp() }
            )

            // Narration: walk through the steps, each one spoken and animated.
            LaunchedEffect(Unit) {
                withTimeoutOrNull(3000) { while (!ttsReady) delay(100) }
                delay(1100) // let the settings screen appear first
                var cycle = 0
                while (!granted.value) {
                    for (i in guide.steps.indices) {
                        if (granted.value) break
                        stepIndex.intValue = i
                        val line = if (cycle == 0 && i == 0) guide.intro + " " + guide.steps[i].voice else guide.steps[i].voice
                        // voice for the first two rounds, then quiet visual hints only
                        val spoke = cycle < 2 && speakAndWait(line)
                        if (!spoke) delay(3800)
                        delay(850)
                    }
                    cycle++
                    if (!granted.value) delay(1300)
                }
            }

            // Watch for the permission and bring the user home.
            LaunchedEffect(Unit) {
                while (true) {
                    delay(400)
                    if (Settings.canDrawOverlays(this@OverlayGuideActivity)) {
                        granted.value = true
                        withTimeoutOrNull(3200) {
                            speakAndWait("Perfect, it's on. Taking you back to High Alarm.")
                        }
                        delay(500)
                        returnToApp()
                        break
                    }
                }
            }

            // Never linger forever.
            LaunchedEffect(Unit) {
                delay(5 * 60 * 1000L)
                finishAndRemoveTask()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!started) {
            started = true
            window.decorView.post { enterPipThenOpenSettings() }
        }
    }

    private fun enterPipThenOpenSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && pipSupported) {
            val ok = try { enterPip() } catch (_: Exception) { false }
            if (ok) {
                window.decorView.postDelayed({ launchSettings() }, 350)
                return
            }
        }
        // No floating window on this phone (or PiP turned off for the app):
        // open the screen directly with a short written hint.
        launchSettings()
        Toast.makeText(this, guide.shortHint, Toast.LENGTH_LONG).show()
        finishAndRemoveTask()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun pipParams(autoEnter: Boolean): PictureInPictureParams =
        PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 11))
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    setSeamlessResizeEnabled(true)
                    setAutoEnterEnabled(autoEnter)
                }
            }
            .build()

    @RequiresApi(Build.VERSION_CODES.O)
    private fun enterPip(): Boolean = enterPictureInPictureMode(pipParams(autoEnter = true))

    /** Button in the expanded guide: shrink to the floating window, then open settings. */
    private fun openSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && pipSupported && !inPip.value) {
            val ok = try { enterPip() } catch (_: Exception) { false }
            if (ok) {
                window.decorView.postDelayed({ launchSettings() }, 350)
                return
            }
        }
        launchSettings()
    }

    private fun launchSettings() {
        val direct = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            startActivity(direct)
        } catch (_: Exception) {
            try {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (_: Exception) { }
        }
    }

    private fun returnToApp() {
        val launch = packageManager.getLaunchIntentForPackage(packageName)
            ?: Intent(this, MainActivity::class.java)
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        try { startActivity(launch) } catch (_: Exception) { }
        finishAndRemoveTask()
    }

    /** Speaks [text] and suspends until it has been said. False if voice isn't available. */
    private suspend fun speakAndWait(text: String): Boolean {
        val engine = tts
        if (!ttsReady || engine == null) return false
        val done = CompletableDeferred<Unit>()
        pendingSpeech = done
        val result = engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "guide_${System.nanoTime()}")
        if (result != TextToSpeech.SUCCESS) return false
        withTimeoutOrNull(15_000) { done.await() }
        return true
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        inPip.value = isInPictureInPictureMode
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // Android 8–11: shrink back into the floating window when the user leaves.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && pipSupported && Build.VERSION.SDK_INT < Build.VERSION_CODES.S && !inPip.value && !granted.value) {
            try { enterPip() } catch (_: Exception) { }
        }
    }

    override fun onStop() {
        super.onStop()
        // The floating window was swiped away / closed: stop talking and leave.
        if (started && !isChangingConfigurations && !isInPictureInPictureMode) {
            tts?.stop()
            finishAndRemoveTask()
        }
    }

    override fun onDestroy() {
        pendingSpeech?.complete(Unit)
        tts?.stop()
        tts?.shutdown()
        tts = null
        super.onDestroy()
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(
                Intent(context, OverlayGuideActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Brand-specific steps
// ---------------------------------------------------------------------------

enum class GuideVisual { FindInList, PickLetter, TapRow, FlipSwitch }

data class GuideStep(val visual: GuideVisual, val title: String, val voice: String)

data class OverlayGuide(
    val brand: String,
    val screenName: String,
    val intro: String,
    val shortHint: String,
    val steps: List<GuideStep>
) {
    companion object {
        private const val BACK = "I'll bring you back automatically."

        fun forThisDevice(): OverlayGuide {
            val m = (Build.MANUFACTURER ?: "").lowercase(Locale.ROOT)
            val b = (Build.BRAND ?: "").lowercase(Locale.ROOT)
            fun any(vararg k: String) = k.any { m.contains(it) || b.contains(it) }
            // Since Android 11 the system always opens the list of apps first.
            val list = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
            val intro = "Let's allow High Alarm to show your alarm on top of other apps."

            fun find(t: String = "Find High Alarm", v: String = "Scroll down the list and find High Alarm.") =
                GuideStep(GuideVisual.FindInList, t, v)
            fun tap(v: String = "Tap High Alarm to open it.") = GuideStep(GuideVisual.TapRow, "Tap High Alarm", v)
            fun flip(t: String, v: String) = GuideStep(GuideVisual.FlipSwitch, t, "$v $BACK")

            return when {
                any("samsung") -> OverlayGuide(
                    "Samsung", "Appear on top", intro,
                    "Find High Alarm and turn its switch ON",
                    if (list) listOf(
                        find(),
                        flip("Turn its switch ON", "Turn on the switch next to High Alarm.")
                    ) else listOf(
                        flip("Turn ON “Allow permission”", "Turn on Allow permission.")
                    )
                )
                any("xiaomi", "redmi", "poco") -> OverlayGuide(
                    "Xiaomi", "Display over other apps", intro,
                    "Tap High Alarm → turn ON “Permit drawing over other apps”",
                    listOf(
                        find("Scroll to High Alarm"),
                        tap(),
                        flip("Turn ON “Permit drawing”", "Turn on Permit drawing over other apps.")
                    )
                )
                any("oppo", "realme", "oneplus") -> OverlayGuide(
                    when { any("realme") -> "realme"; any("oneplus") -> "OnePlus"; else -> "OPPO" },
                    "Display over other apps", intro,
                    "Tap “H” on the right → High Alarm → turn ON",
                    if (list) listOf(
                        GuideStep(GuideVisual.PickLetter, "Tap “H” on the right",
                            "On the right edge, tap the letter H to jump down the list."),
                        tap("Now tap High Alarm."),
                        flip("Turn ON “Allow”", "Turn on Allow display over other apps.")
                    ) else listOf(
                        flip("Turn ON “Allow”", "Turn on Allow display over other apps.")
                    )
                )
                any("vivo", "iqoo") -> OverlayGuide(
                    "vivo", "Display over other apps", intro,
                    "Find High Alarm and turn it ON",
                    listOf(
                        find(),
                        tap("Tap High Alarm. If you already see a switch beside it, just turn it on."),
                        flip("Turn the switch ON", "Turn on the switch.")
                    )
                )
                any("huawei", "honor") -> OverlayGuide(
                    if (any("honor")) "HONOR" else "HUAWEI", "Display over other apps", intro,
                    "Tap High Alarm → turn ON “Allow”",
                    listOf(
                        find(),
                        tap(),
                        flip("Turn ON “Allow”", "Turn on Allow display over other apps.")
                    )
                )
                any("tecno", "infinix", "itel") -> OverlayGuide(
                    when { any("tecno") -> "TECNO"; any("infinix") -> "Infinix"; else -> "itel" },
                    "Display over other apps", intro,
                    "Tap High Alarm → turn the switch ON",
                    listOf(
                        find(),
                        tap(),
                        flip("Turn the switch ON", "Turn on the switch.")
                    )
                )
                else -> OverlayGuide(
                    Build.MANUFACTURER?.replaceFirstChar { it.uppercase() }?.ifBlank { null } ?: "Android",
                    "Display over other apps", intro,
                    "Tap High Alarm → turn ON “Allow display over other apps”",
                    if (list) listOf(
                        find(),
                        tap(),
                        flip("Turn ON “Allow display”", "Turn on Allow display over other apps.")
                    ) else listOf(
                        flip("Turn ON “Allow display”", "Turn on Allow display over other apps.")
                    )
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// UI
// ---------------------------------------------------------------------------

@Composable
private fun OverlayGuideScreen(
    guide: OverlayGuide,
    step: Int,
    granted: Boolean,
    compact: Boolean,
    onOpenSettings: () -> Unit,
    onClose: () -> Unit
) {
    val current = guide.steps[step.coerceIn(0, guide.steps.lastIndex)]
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF17171C), Color(0xFF0B0B0E), Color(0xFF060608))))
    ) {
        if (compact) {
            // ---- Floating PiP layout (small window) ----
            Row(
                modifier = Modifier.fillMaxSize().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(78.dp)
                        .ha3DInset(RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    GuideDemo(visual = if (granted) GuideVisual.FlipSwitch else current.visual, done = granted, modifier = Modifier.fillMaxSize().padding(4.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(18.dp).ha3DCoin(if (granted) HaTone.Jade else HaTone.Gold, elevation = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (granted) Icon(Icons.Default.Check, null, tint = haToneInk(HaTone.Jade), modifier = Modifier.size(12.dp))
                            else Text("${step + 1}", color = haToneInk(HaTone.Gold), fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            if (granted) "All set!" else "Step ${step + 1}/${guide.steps.size} · ${guide.brand}",
                            color = HaGoldLight, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    AnimatedContent(
                        targetState = if (granted) "Permission on — returning…" else current.title,
                        transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                        label = "pip_title"
                    ) { t ->
                        Text(
                            t, color = HaIvory, fontSize = 13.sp, fontWeight = FontWeight.Black, lineHeight = 15.sp,
                            maxLines = 3, overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(5.dp))
                    StepDots(guide.steps.size, step, granted)
                }
            }
        } else {
            // ---- Expanded layout (user tapped the PiP window) ----
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(40.dp).ha3DCoin(HaTone.Gold), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Layers, null, tint = haToneInk(HaTone.Gold), modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Allow “${guide.screenName}”", color = HaIvory, fontSize = 18.sp, fontWeight = FontWeight.Black)
                        Text("Guide for your ${guide.brand} phone", color = HaMuted, fontSize = 12.sp)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .ha3DInset(RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    GuideDemo(visual = if (granted) GuideVisual.FlipSwitch else current.visual, done = granted, modifier = Modifier.fillMaxSize().padding(16.dp))
                }
                Spacer(modifier = Modifier.height(20.dp))
                guide.steps.forEachIndexed { i, s ->
                    val active = i == step && !granted
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                            .then(
                                if (active) Modifier.ha3DPanelSelected(RoundedCornerShape(16.dp), elevation = 6.dp)
                                else Modifier.ha3DBlack(RoundedCornerShape(16.dp))
                            )
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(28.dp).ha3DCoin(if (granted || i < step) HaTone.Jade else HaTone.Gold, elevation = 3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (granted || i < step) Icon(Icons.Default.Check, null, tint = haToneInk(HaTone.Jade), modifier = Modifier.size(16.dp))
                            else Text("${i + 1}", color = haToneInk(HaTone.Gold), fontSize = 13.sp, fontWeight = FontWeight.Black)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(s.title, color = HaIvory, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.RecordVoiceOver, null, tint = HaGold, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Voice guide is on", color = HaMuted, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(12.dp))
                HaGoldButton(text = if (granted) "Back to High Alarm" else "Open settings", onClick = if (granted) onClose else onOpenSettings)
            }
        }
    }
}

@Composable
private fun StepDots(count: Int, current: Int, done: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(count) { i ->
            Box(
                modifier = Modifier
                    .height(4.dp)
                    .width(if (i == current && !done) 14.dp else 6.dp)
                    .background(
                        if (done) HaJade else if (i <= current) HaGold else HaIvory.copy(alpha = 0.2f),
                        CircleShape
                    )
            )
        }
    }
}

/**
 * A tiny animated mock of the settings screen showing the exact gesture:
 * scrolling to High Alarm, tapping the A–Z bar, tapping the row, or flipping the switch.
 */
@Composable
private fun GuideDemo(visual: GuideVisual, done: Boolean, modifier: Modifier) {
    val loop = rememberInfiniteTransition(label = "guide_demo")
    val p by loop.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = 2600
                0f at 0 using LinearEasing
                0f at 250 using FastOutSlowInEasing
                1f at 1500 using LinearEasing
                1f at 2600
            },
            RepeatMode.Restart
        ),
        label = "demo_p"
    )
    val pulse by loop.animateFloat(
        0f, 1f, infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart), label = "tap_pulse"
    )
    Canvas(modifier = modifier) {
        when {
            done -> drawSwitchDemo(1f, 0f, success = true)
            visual == GuideVisual.FlipSwitch -> drawSwitchDemo(p, pulse, success = false)
            visual == GuideVisual.TapRow -> drawListDemo(scroll = 1f, highlight = 1f, tap = pulse, showIndex = false, indexTap = 0f)
            visual == GuideVisual.PickLetter -> drawListDemo(scroll = p, highlight = p, tap = 0f, showIndex = true, indexTap = pulse)
            else -> drawListDemo(scroll = p, highlight = p, tap = 0f, showIndex = false, indexTap = 0f)
        }
    }
}

private fun DrawScope.drawFinger(at: Offset, pulse: Float, scale: Float) {
    // ripple
    drawCircle(HaGoldPale.copy(alpha = 0.45f * (1f - pulse)), radius = scale * (6f + 14f * pulse), center = at, style = Stroke(width = scale * 1.6f))
    // fingertip
    drawCircle(Color.Black.copy(alpha = 0.35f), radius = scale * 7f, center = at + Offset(0f, scale * 1.5f))
    drawCircle(
        Brush.radialGradient(listOf(Color(0xFFFFF4D6), HaGoldLight, HaGold), center = at + Offset(-scale * 2f, -scale * 2f), radius = scale * 10f),
        radius = scale * 6.5f, center = at
    )
}

private fun DrawScope.drawListDemo(scroll: Float, highlight: Float, tap: Float, showIndex: Boolean, indexTap: Float) {
    val s = size.minDimension / 100f
    val w = size.width; val h = size.height
    val rowH = 16f * s
    val listW = if (showIndex) w - 12f * s else w
    val target = 6 // index of "High Alarm"
    // scroll so the target row ends in the middle
    val scrollPx = scroll * (target * rowH - h / 2 + rowH / 2)
    for (i in 0 until 14) {
        val y = i * rowH - scrollPx + 4f * s
        if (y < -rowH || y > h) continue
        val isTarget = i == target
        val bg = if (isTarget) lerpColor(Color(0xFF1C1C22), HaGoldSelectedFill, highlight) else Color(0xFF16161B)
        drawRoundRect(bg, topLeft = Offset(2f * s, y), size = Size(listW - 4f * s, rowH - 3f * s), cornerRadius = CornerRadius(4f * s))
        if (isTarget) {
            drawRoundRect(
                HaGold.copy(alpha = 0.9f * highlight), topLeft = Offset(2f * s, y), size = Size(listW - 4f * s, rowH - 3f * s),
                cornerRadius = CornerRadius(4f * s), style = Stroke(width = 1.2f * s)
            )
        }
        // app icon + label bars
        drawCircle(if (isTarget) HaGold else HaIvory.copy(alpha = 0.25f), radius = 4f * s, center = Offset(10f * s, y + (rowH - 3f * s) / 2))
        drawRoundRect(
            if (isTarget) HaIvory else HaIvory.copy(alpha = 0.18f),
            topLeft = Offset(18f * s, y + (rowH - 3f * s) / 2 - 1.6f * s), size = Size(if (isTarget) 34f * s else (22f + (i * 7) % 18) * s, 3.2f * s),
            cornerRadius = CornerRadius(2f * s)
        )
    }
    if (showIndex) {
        // A–Z bar with "H" lit
        val letters = 12
        for (i in 0 until letters) {
            val y = 6f * s + i * (h - 12f * s) / letters
            val isH = i == 4
            drawCircle(
                if (isH) HaGold else HaIvory.copy(alpha = 0.25f),
                radius = if (isH) 2.6f * s else 1.5f * s, center = Offset(w - 5f * s, y)
            )
        }
        drawFinger(Offset(w - 5f * s, 6f * s + 4 * (h - 12f * s) / letters), indexTap, s)
    }
    if (tap > 0f) {
        val y = target * rowH - scrollPx + 4f * s + (rowH - 3f * s) / 2
        drawFinger(Offset(listW * 0.62f, y), tap, s)
    }
}

private fun DrawScope.drawSwitchDemo(p: Float, pulse: Float, success: Boolean) {
    val s = size.minDimension / 100f
    val w = size.width; val h = size.height
    // header bar + description lines
    drawRoundRect(HaIvory.copy(alpha = 0.18f), topLeft = Offset(8f * s, h * 0.18f), size = Size(w * 0.5f, 3.5f * s), cornerRadius = CornerRadius(2f * s))
    drawRoundRect(HaIvory.copy(alpha = 0.10f), topLeft = Offset(8f * s, h * 0.18f + 8f * s), size = Size(w * 0.35f, 3f * s), cornerRadius = CornerRadius(2f * s))
    // the switch
    val on = if (success) 1f else ((p - 0.15f) / 0.5f).coerceIn(0f, 1f)
    val trackW = 34f * s; val trackH = 18f * s
    val tl = Offset(w - trackW - 8f * s, h * 0.55f - trackH / 2)
    drawRoundRect(
        Brush.verticalGradient(
            if (on > 0.5f) listOf(HaGoldPale, HaGold, HaGoldDeep) else listOf(Color(0xFF07070A), Color(0xFF26262E)),
            startY = tl.y, endY = tl.y + trackH
        ),
        topLeft = tl, size = Size(trackW, trackH), cornerRadius = CornerRadius(trackH / 2)
    )
    val knobX = tl.x + trackH / 2 + on * (trackW - trackH)
    val knobC = Offset(knobX, tl.y + trackH / 2)
    drawCircle(Color.Black.copy(alpha = 0.4f), radius = trackH * 0.42f, center = knobC + Offset(0f, 1.2f * s))
    drawCircle(
        Brush.verticalGradient(
            if (on > 0.5f) listOf(Color(0xFF3A3842), Color(0xFF0A0A0D)) else listOf(Color.White, Color(0xFFA9A8B0)),
            startY = knobC.y - trackH * 0.4f, endY = knobC.y + trackH * 0.4f
        ),
        radius = trackH * 0.4f, center = knobC
    )
    // label
    drawRoundRect(HaIvory.copy(alpha = 0.55f), topLeft = Offset(8f * s, h * 0.55f - 2f * s), size = Size(w * 0.42f, 4f * s), cornerRadius = CornerRadius(2f * s))
    if (!success && p < 0.7f) drawFinger(knobC + Offset(2f * s, 2f * s), pulse, s)
    if (success) {
        drawCircle(HaJade.copy(alpha = 0.18f), radius = 16f * s, center = Offset(w / 2, h * 0.86f))
    }
}

private fun lerpColor(a: Color, b: Color, t: Float) = androidx.compose.ui.graphics.lerp(a, b, t.coerceIn(0f, 1f))
