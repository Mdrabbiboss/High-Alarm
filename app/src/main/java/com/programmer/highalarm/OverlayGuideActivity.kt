package com.programmer.highalarm

import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.res.Configuration
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Rational
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.lang.ref.WeakReference
import java.util.Locale

/**
 * Floating (Picture-in-Picture) coach for the "Display over other apps"
 * permission. It opens the system screen, then floats above it as a small
 * premium window that shows — with this phone's real High Alarm icon and
 * name — and says out loud exactly what to tap on this brand. The moment the
 * permission is on, it takes the user back to High Alarm by itself.
 *
 * It runs every time the permission is requested from the app (a running
 * guide is restarted, not ignored), and closes itself if the user returns to
 * the app without granting.
 */
class OverlayGuideActivity : ComponentActivity() {

    private var tts: TextToSpeech? = null
    @Volatile private var ttsReady = false
    @Volatile private var pendingSpeech: CompletableDeferred<Unit>? = null
    private val inPip = mutableStateOf(false)
    private val stepIndex = mutableIntStateOf(0)
    private val granted = mutableStateOf(false)
    private val runId = mutableIntStateOf(0)
    private var started = false
    private var settingsLaunchedAt = 0L
    private lateinit var guide: OverlayGuide

    private val pipSupported: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        current = WeakReference(this)
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && pipSupported) {
            // pressing Home while the guide is expanded shrinks it back into the floating window
            try { setPictureInPictureParams(pipParams()) } catch (_: Exception) { }
        }

        // Back while expanded: shrink back over the settings screen instead of quitting.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && pipSupported && !inPip.value && !granted.value) {
                    val ok = try { enterPip() } catch (_: Exception) { false }
                    if (ok) return
                }
                finishAndRemoveTask()
            }
        })

        setContent {
            val pip by inPip
            val step by stepIndex
            val done by granted
            val run by runId
            val mock by produceState<MockData?>(initialValue = null) {
                value = withContext(Dispatchers.Default) { loadMockData() }
            }
            OverlayGuideScreen(
                guide = guide,
                mock = mock,
                step = step,
                granted = done,
                compact = pip,
                onOpenSettings = { openSettings() },
                onClose = { returnToApp() }
            )

            // Narration: walk through the steps, each one spoken and animated.
            LaunchedEffect(run) {
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
                        delay(900)
                    }
                    cycle++
                    if (!granted.value) delay(1300)
                }
            }

            // Watch for the permission and bring the user home.
            LaunchedEffect(run) {
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
            LaunchedEffect(run) {
                delay(5 * 60 * 1000L)
                finishAndRemoveTask()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Requested again from the app: start the guide over and reopen settings.
        tts?.stop()
        settingsLaunchedAt = SystemClock.elapsedRealtime() + 800
        granted.value = false
        stepIndex.intValue = 0
        runId.intValue = runId.intValue + 1
        started = true
        window.decorView.postDelayed({ startFlow() }, 180)
    }

    override fun onResume() {
        super.onResume()
        if (!started) {
            started = true
            window.decorView.post { startFlow() }
        }
    }

    private fun startFlow() {
        if (isFinishing || isDestroyed) return
        if (inPip.value) {
            launchSettings()
            return
        }
        settingsLaunchedAt = SystemClock.elapsedRealtime() + 500
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
    private fun pipParams(): PictureInPictureParams =
        PictureInPictureParams.Builder()
            .setAspectRatio(Rational(4, 3))
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    setSeamlessResizeEnabled(true)
                    setAutoEnterEnabled(true)
                }
            }
            .build()

    @RequiresApi(Build.VERSION_CODES.O)
    private fun enterPip(): Boolean = enterPictureInPictureMode(pipParams())

    /** Button in the expanded guide: shrink to the floating window, then open settings. */
    private fun openSettings() {
        settingsLaunchedAt = SystemClock.elapsedRealtime() + 500
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
        settingsLaunchedAt = SystemClock.elapsedRealtime()
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

    /** The app came back to the front: if the user left settings without granting, close quietly. */
    private fun hostResumed() {
        if (isFinishing || isDestroyed || granted.value) return
        val launched = settingsLaunchedAt
        if (launched == 0L || SystemClock.elapsedRealtime() - launched < 1500) return
        tts?.stop()
        finishAndRemoveTask()
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

    /** Real icon + name of High Alarm and of the apps around it in the system list. */
    private fun loadMockData(): MockData {
        val pm = packageManager
        val selfName = try { applicationInfo.loadLabel(pm).toString() } catch (_: Exception) { "High Alarm" }
        val selfIcon = try {
            pm.getApplicationIcon(packageName).toBitmap(144, 144).asImageBitmap()
        } catch (_: Exception) { null }

        val others: List<Pair<ResolveInfo, String>> = try {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
                .filter { it.activityInfo != null && it.activityInfo.packageName != packageName }
                .distinctBy { it.activityInfo.packageName }
                .map { it to it.loadLabel(pm).toString().trim() }
                .filter { it.second.isNotBlank() }
                .sortedBy { it.second.lowercase(Locale.ROOT) }
        } catch (_: Exception) { emptyList() }

        val key = selfName.lowercase(Locale.ROOT)
        fun icon(ri: ResolveInfo): ImageBitmap? =
            try { ri.loadIcon(pm).toBitmap(96, 96).asImageBitmap() } catch (_: Exception) { null }
        val before = others.filter { it.second.lowercase(Locale.ROOT) < key }.takeLast(4)
            .map { MockApp(it.second, icon(it.first)) }
            .ifEmpty { listOf("Calculator", "Calendar", "Camera", "Gallery").map { MockApp(it, null) } }
        val after = others.filter { it.second.lowercase(Locale.ROOT) >= key }.take(4)
            .map { MockApp(it.second, icon(it.first)) }
            .ifEmpty { listOf("Messages", "Music", "Notes", "Phone").map { MockApp(it, null) } }
        return MockData(selfName, selfIcon, before, after)
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        inPip.value = isInPictureInPictureMode
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // Android 8–11: shrink back into the floating window when the user leaves.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && Build.VERSION.SDK_INT < Build.VERSION_CODES.S &&
            pipSupported && !inPip.value && !granted.value
        ) {
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
        if (current?.get() === this) current = null
        pendingSpeech?.complete(Unit)
        tts?.stop()
        tts?.shutdown()
        tts = null
        super.onDestroy()
    }

    companion object {
        private var current: WeakReference<OverlayGuideActivity>? = null

        fun start(context: Context) {
            context.startActivity(
                Intent(context, OverlayGuideActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
            )
        }

        /** Called from MainActivity.onResume. */
        fun onHostResumed() {
            try { current?.get()?.hostResumed() } catch (_: Exception) { }
        }
    }
}

// ---------------------------------------------------------------------------
// Brand-specific steps
// ---------------------------------------------------------------------------

enum class GuideVisual { FindInList, PickLetter, TapRow, FlipSwitch }

/** How this brand's app list looks. */
enum class ListStyle { TrailingStatus, SubtitleStatus, InlineSwitch }

data class GuideStep(val visual: GuideVisual, val title: String, val voice: String)

data class OverlayGuide(
    val brand: String,
    val screenName: String,
    val intro: String,
    val shortHint: String,
    val style: ListStyle,
    val hasIndex: Boolean,
    val allowWord: String,
    val denyWord: String,
    val toggleLabel: String,
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
            val intro = "Let's allow High Alarm to show your alarm on top of other apps. You can drag this small window anywhere."

            fun find() = GuideStep(GuideVisual.FindInList, "Scroll to High Alarm", "Scroll down the list and find High Alarm.")
            fun letter() = GuideStep(GuideVisual.PickLetter, "Tap “H” on the right", "On the right edge, tap the letter H to jump down the list.")
            fun tap(v: String = "Now tap High Alarm.") = GuideStep(GuideVisual.TapRow, "Tap High Alarm", v)
            fun flip(t: String, v: String) = GuideStep(GuideVisual.FlipSwitch, t, "$v $BACK")
            val allowDisplay = "Allow display over other apps"

            return when {
                any("samsung") -> OverlayGuide(
                    "Samsung", "Appear on top", intro, "Find High Alarm and turn its switch ON",
                    ListStyle.InlineSwitch, false, "", "", "Allow permission",
                    if (list) listOf(
                        find(),
                        flip("Turn its switch ON", "Turn on the switch next to High Alarm.")
                    ) else listOf(flip("Turn ON “Allow permission”", "Turn on Allow permission."))
                )
                any("xiaomi", "redmi", "poco") -> OverlayGuide(
                    "Xiaomi", "Display over other apps", intro,
                    "Tap High Alarm → turn ON “Permit drawing over other apps”",
                    ListStyle.TrailingStatus, false, "Allowed", "Not allowed", "Permit drawing over other apps",
                    listOf(
                        find(),
                        tap(),
                        flip("Turn ON “Permit drawing”", "Turn on Permit drawing over other apps.")
                    )
                )
                any("oppo", "realme", "oneplus") -> OverlayGuide(
                    when { any("realme") -> "realme"; any("oneplus") -> "OnePlus"; else -> "OPPO" },
                    "Display over other apps", intro, "Tap “H” on the right → High Alarm → turn ON",
                    ListStyle.TrailingStatus, true, "Allow", "Don't allow", allowDisplay,
                    if (list) listOf(
                        letter(),
                        tap(),
                        flip("Turn ON “Allow”", "Turn on Allow display over other apps.")
                    ) else listOf(flip("Turn ON “Allow”", "Turn on Allow display over other apps."))
                )
                any("vivo", "iqoo") -> OverlayGuide(
                    if (any("iqoo")) "iQOO" else "vivo",
                    "Display over other apps", intro, "Tap “H” on the right → High Alarm → turn ON",
                    ListStyle.TrailingStatus, true, "Allow", "Don't allow", allowDisplay,
                    if (list) listOf(
                        letter(),
                        tap(),
                        flip("Turn the switch ON", "Turn on the switch for High Alarm.")
                    ) else listOf(flip("Turn the switch ON", "Turn on the switch for High Alarm."))
                )
                any("huawei", "honor") -> OverlayGuide(
                    if (any("honor")) "HONOR" else "HUAWEI", "Display over other apps", intro,
                    "Tap High Alarm → turn ON “Allow”",
                    ListStyle.SubtitleStatus, false, "Allowed", "Not allowed", allowDisplay,
                    listOf(find(), tap(), flip("Turn ON “Allow”", "Turn on Allow display over other apps."))
                )
                any("tecno", "infinix", "itel") -> OverlayGuide(
                    when { any("tecno") -> "TECNO"; any("infinix") -> "Infinix"; else -> "itel" },
                    "Display over other apps", intro, "Tap High Alarm → turn the switch ON",
                    ListStyle.SubtitleStatus, false, "Allowed", "Not allowed", allowDisplay,
                    listOf(find(), tap(), flip("Turn the switch ON", "Turn on the switch."))
                )
                else -> OverlayGuide(
                    Build.MANUFACTURER?.replaceFirstChar { it.uppercase() }?.ifBlank { null } ?: "Android",
                    "Display over other apps", intro, "Tap High Alarm → turn ON “Allow display over other apps”",
                    ListStyle.SubtitleStatus, false, "Allowed", "Not allowed", allowDisplay,
                    if (list) listOf(
                        find(),
                        tap(),
                        flip("Turn ON “Allow display”", "Turn on Allow display over other apps.")
                    ) else listOf(flip("Turn ON “Allow display”", "Turn on Allow display over other apps."))
                )
            }
        }
    }
}

data class MockApp(val name: String, val icon: ImageBitmap?)

data class MockData(
    val appName: String,
    val appIcon: ImageBitmap?,
    val before: List<MockApp>,
    val after: List<MockApp>
) {
    val self: MockApp get() = MockApp(appName, appIcon)
    val rows: List<MockApp> get() = before + self + after
    val target: Int get() = before.size

    companion object {
        val Placeholder = MockData(
            "High Alarm", null,
            listOf("Calculator", "Calendar", "Camera", "Gallery").map { MockApp(it, null) },
            listOf("Messages", "Music", "Notes", "Phone").map { MockApp(it, null) }
        )
    }
}

// ---------------------------------------------------------------------------
// UI
// ---------------------------------------------------------------------------

private val MockScreenBg = Color(0xFF0E0E12)
private val MockRowHighlight = Color(0xFF2E2615)

@Composable
private fun OverlayGuideScreen(
    guide: OverlayGuide,
    mock: MockData?,
    step: Int,
    granted: Boolean,
    compact: Boolean,
    onOpenSettings: () -> Unit,
    onClose: () -> Unit
) {
    val current = guide.steps[step.coerceIn(0, guide.steps.lastIndex)]
    val visual = if (granted) GuideVisual.FlipSwitch else current.visual
    val data = mock ?: MockData.Placeholder
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF17171C), Color(0xFF0B0B0E), Color(0xFF060608))))
    ) {
        if (compact) {
            // ---- Floating PiP window: a zoomed-in, real-looking view + one clear instruction ----
            Column(modifier = Modifier.fillMaxSize().padding(start = 5.dp, end = 5.dp, top = 5.dp, bottom = 3.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .ha3DInset(RoundedCornerShape(10.dp))
                ) {
                    key(step, granted) {
                        SettingsMock(guide, data, visual, granted, compact = true, modifier = Modifier.fillMaxSize())
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().height(30.dp).padding(horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StepCoin(step, granted, 18.dp, 10.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    AnimatedContent(
                        targetState = if (granted) "All set! Returning…" else current.title,
                        transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                        label = "pip_title",
                        modifier = Modifier.weight(1f)
                    ) { t ->
                        Text(
                            t, color = HaIvory, fontSize = 11.sp, fontWeight = FontWeight.Black, lineHeight = 12.sp,
                            maxLines = 2, overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (!granted && guide.steps.size > 1) {
                        Text("${step + 1}/${guide.steps.size}", color = HaGoldLight, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            // ---- Expanded guide (user tapped the floating window) ----
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.size(40.dp).ha3DCoin(HaTone.Gold), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Layers, null, tint = haToneInk(HaTone.Gold), modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Allow “${guide.screenName}”", color = HaIvory, fontSize = 18.sp, fontWeight = FontWeight.Black)
                        Text("Step-by-step for your ${guide.brand} phone", color = HaMuted, fontSize = 12.sp)
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .ha3DInset(RoundedCornerShape(28.dp))
                        .padding(7.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(MockScreenBg)
                ) {
                    key(step, granted) {
                        SettingsMock(guide, data, visual, granted, compact = false, modifier = Modifier.fillMaxSize())
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                guide.steps.forEachIndexed { i, s ->
                    val active = i == step && !granted
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .then(
                                if (active) Modifier.ha3DPanelSelected(RoundedCornerShape(16.dp), elevation = 6.dp)
                                else Modifier.ha3DBlack(RoundedCornerShape(16.dp))
                            )
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val ok = granted || i < step
                        Box(
                            modifier = Modifier.size(26.dp).ha3DCoin(if (ok) HaTone.Jade else HaTone.Gold, elevation = 3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (ok) Icon(Icons.Default.Check, null, tint = haToneInk(HaTone.Jade), modifier = Modifier.size(15.dp))
                            else Text("${i + 1}", color = haToneInk(HaTone.Gold), fontSize = 12.sp, fontWeight = FontWeight.Black)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(s.title, color = HaIvory, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.RecordVoiceOver, null, tint = HaGold, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Voice guide is on", color = HaMuted, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(10.dp))
                HaGoldButton(
                    text = if (granted) "Back to High Alarm" else "Open settings",
                    onClick = if (granted) onClose else onOpenSettings
                )
            }
        }
    }
}

@Composable
private fun StepCoin(step: Int, done: Boolean, size: Dp, fontSize: TextUnit) {
    Box(
        modifier = Modifier.size(size).ha3DCoin(if (done) HaTone.Jade else HaTone.Gold, elevation = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        if (done) Icon(Icons.Default.Check, null, tint = haToneInk(HaTone.Jade), modifier = Modifier.size(size * 0.66f))
        else Text("${step + 1}", color = haToneInk(HaTone.Gold), fontSize = fontSize, fontWeight = FontWeight.Black)
    }
}

// ---- timeline helpers --------------------------------------------------------

private fun seg(t: Float, a: Float, b: Float) = ((t - a) / (b - a)).coerceIn(0f, 1f)
private fun ease(x: Float) = x * x * (3f - 2f * x)
private fun lerpF(a: Float, b: Float, t: Float) = a + (b - a) * t
private fun lerpColor(a: Color, b: Color, t: Float) = androidx.compose.ui.graphics.lerp(a, b, t.coerceIn(0f, 1f))

/**
 * A faithful little copy of the system screen, built from real text and the
 * real app icon, animated to show the exact gesture for the current step.
 */
@Composable
private fun SettingsMock(
    guide: OverlayGuide,
    data: MockData,
    visual: GuideVisual,
    done: Boolean,
    compact: Boolean,
    modifier: Modifier
) {
    val loop = rememberInfiniteTransition(label = "mock")
    val tAnim by loop.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3600, easing = LinearEasing), RepeatMode.Restart),
        label = "mock_t"
    )
    val t = if (done) 1f else tAnim
    val detail = visual == GuideVisual.FlipSwitch && guide.style != ListStyle.InlineSwitch
    if (detail) DetailMock(guide, data, t, done, compact, modifier)
    else ListMock(guide, data, visual, t, done, compact, modifier)
}

@Composable
private fun MockHeader(title: String, showSearch: Boolean) {
    Column(modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("‹", color = HaIvory, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(10.dp))
            Text(title, color = HaIvory, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (showSearch) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .background(Color(0xFF1C1C22), RoundedCornerShape(15.dp))
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Search, null, tint = HaMuted, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Search apps", color = HaMuted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun AppIcon(app: MockApp, size: Dp) {
    val bmp = app.icon
    if (bmp != null) {
        Image(bitmap = bmp, contentDescription = null, modifier = Modifier.size(size))
    } else {
        val hues = listOf(Color(0xFF4F7CF7), Color(0xFF3FB98A), Color(0xFFE0784A), Color(0xFF9B6BE8), Color(0xFFD6A13C))
        val c = hues[(app.name.hashCode() and 0x7fffffff) % hues.size]
        Box(
            modifier = Modifier.size(size).background(Brush.verticalGradient(listOf(c, c.copy(alpha = 0.75f))), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(app.name.take(1).uppercase(), color = Color.White, fontSize = (size.value * 0.45f).sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MiniSwitch(on: Float, width: Dp, height: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(width, height)) {
        val h = size.height
        val w = size.width
        val r = CornerRadius(h / 2)
        drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF07070A), Color(0xFF2B2B33))), cornerRadius = r)
        drawRoundRect(Brush.verticalGradient(listOf(HaGoldPale, HaGold, HaGoldDeep)), cornerRadius = r, alpha = on.coerceIn(0f, 1f))
        drawRoundRect(Color.White.copy(alpha = 0.08f), cornerRadius = r, style = Stroke(width = 1f))
        val kr = h * 0.38f
        val c = Offset(h / 2 + on.coerceIn(0f, 1f) * (w - h), h / 2)
        drawCircle(Color.Black.copy(alpha = 0.4f), radius = kr * 1.05f, center = c + Offset(0f, h * 0.06f))
        drawCircle(
            Brush.verticalGradient(
                if (on > 0.5f) listOf(Color(0xFF3A3842), Color(0xFF0A0A0D)) else listOf(Color.White, Color(0xFFA9A8B0)),
                startY = c.y - kr, endY = c.y + kr
            ),
            radius = kr, center = c
        )
    }
}

/** A touch indicator: glossy fingertip with a ripple ring. */
private fun DrawScope.touch(at: Offset, ripple: Float, alpha: Float, r: Float) {
    if (alpha <= 0.01f) return
    if (ripple > 0f && ripple < 1f) {
        drawCircle(
            Color.White.copy(alpha = 0.55f * (1f - ripple) * alpha),
            radius = r * (1.1f + 1.9f * ripple), center = at, style = Stroke(width = r * 0.2f)
        )
    }
    drawCircle(Color.Black.copy(alpha = 0.35f * alpha), radius = r * 1.04f, center = at + Offset(0f, r * 0.2f))
    drawCircle(
        Brush.radialGradient(listOf(Color.White, HaGoldPale, HaGold), center = at + Offset(-r * 0.3f, -r * 0.3f), radius = r * 1.5f),
        radius = r, center = at, alpha = 0.95f * alpha
    )
}

@Composable
private fun ListMock(
    guide: OverlayGuide,
    data: MockData,
    visual: GuideVisual,
    t: Float,
    done: Boolean,
    compact: Boolean,
    modifier: Modifier
) {
    val rows = data.rows
    val target = data.target
    val rowH = if (compact) 27.dp else 52.dp
    val iconS = if (compact) 17.dp else 32.dp
    val nameSp = if (compact) 10.sp else 15.sp
    val subSp = if (compact) 8.sp else 12.sp
    val padH = if (compact) 3.dp else 8.dp
    val endPad = if (compact) 5.dp else 10.dp
    val switchW = if (compact) 22.dp else 38.dp
    val switchH = if (compact) 13.dp else 22.dp
    val indexW = if (guide.hasIndex) (if (compact) 15.dp else 20.dp) else 0.dp
    val letters = if (compact) "EFGHIJK" else "#ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    val hIndex = letters.indexOf('H')
    val fingerR = if (compact) 6.dp else 11.dp

    Column(modifier = modifier.clipToBounds()) {
        if (!compact) MockHeader(guide.screenName, showSearch = true)
        if (!compact) Spacer(modifier = Modifier.height(8.dp))
        BoxWithConstraints(modifier = Modifier.fillMaxWidth().weight(1f).clipToBounds()) {
            val vw = maxWidth
            val vh = maxHeight
            val listW = vw - indexW
            val maxScroll = (rowH * target + rowH / 2 - vh / 2).coerceAtLeast(0.dp)
            val letterY = vh * ((hIndex + 0.5f) / letters.length)
            val rowCenterY0 = rowH * target + rowH / 2

            // ---- timeline for this step ----
            var scrollFrac = 1f
            var highlight = 1f
            var pressed = 0f
            var switchOn = 0f
            var letterLit = 0f
            var bubble = 0f
            var fingerX = listW * 0.55f
            var fingerY = rowCenterY0 - maxScroll
            var fingerAlpha = 0f
            var ripple = 0f
            when (visual) {
                GuideVisual.FindInList -> {
                    val s = ease(seg(t, 0.12f, 0.62f))
                    scrollFrac = s
                    highlight = seg(t, 0.62f, 0.76f)
                    fingerX = listW * 0.5f
                    fingerY = vh * lerpF(0.82f, 0.25f, s)
                    fingerAlpha = seg(t, 0.03f, 0.12f) * (1f - seg(t, 0.62f, 0.70f))
                }
                GuideVisual.PickLetter -> {
                    val move = ease(seg(t, 0f, 0.22f))
                    fingerX = lerpF(listW.value * 0.6f, (vw - indexW / 2).value, move).dp
                    fingerY = lerpF(vh.value * 0.9f, letterY.value, move).dp
                    fingerAlpha = seg(t, 0f, 0.06f) * (1f - seg(t, 0.52f, 0.60f))
                    ripple = seg(t, 0.24f, 0.46f)
                    letterLit = seg(t, 0.22f, 0.27f)
                    bubble = seg(t, 0.24f, 0.30f) * (1f - seg(t, 0.56f, 0.64f))
                    scrollFrac = ease(seg(t, 0.32f, 0.55f))
                    highlight = seg(t, 0.55f, 0.68f)
                }
                GuideVisual.TapRow -> {
                    val move = ease(seg(t, 0f, 0.28f))
                    fingerX = lerpF(listW.value * 0.75f, listW.value * 0.55f, move).dp
                    fingerY = lerpF(vh.value * 1.05f, (rowCenterY0 - maxScroll).value, move).dp
                    fingerAlpha = seg(t, 0f, 0.06f) * (1f - seg(t, 0.64f, 0.74f))
                    ripple = seg(t, 0.30f, 0.54f)
                    pressed = seg(t, 0.30f, 0.36f) * (1f - seg(t, 0.50f, 0.62f))
                }
                GuideVisual.FlipSwitch -> {
                    // inline switch inside the list (Samsung style)
                    val sx = listW - padH - endPad - switchW / 2
                    val sy = rowCenterY0 - maxScroll
                    val move = ease(seg(t, 0f, 0.26f))
                    fingerX = lerpF(listW.value * 0.6f, sx.value, move).dp
                    fingerY = lerpF(vh.value * 1.05f, sy.value, move).dp
                    fingerAlpha = seg(t, 0f, 0.06f) * (1f - seg(t, 0.62f, 0.72f))
                    ripple = seg(t, 0.30f, 0.52f)
                    switchOn = ease(seg(t, 0.32f, 0.48f))
                }
            }
            if (done) {
                scrollFrac = 1f; highlight = 1f; switchOn = 1f; fingerAlpha = 0f; bubble = 0f
            }
            val scroll = maxScroll * scrollFrac

            Column(
                modifier = Modifier
                    .width(listW)
                    .wrapContentHeight(align = Alignment.Top, unbounded = true)
                    .offset(y = -scroll)
            ) {
                rows.forEachIndexed { i, app ->
                    val isTarget = i == target
                    MockRow(
                        app = app,
                        isTarget = isTarget,
                        highlight = if (isTarget) highlight else 0f,
                        pressed = if (isTarget) pressed else 0f,
                        allowed = if (isTarget) done || switchOn > 0.5f else false,
                        switchOn = if (isTarget) switchOn else if ((app.name.length % 3) == 0) 1f else 0f,
                        guide = guide,
                        rowH = rowH, iconS = iconS, nameSp = nameSp, subSp = subSp,
                        padH = padH, endPad = endPad, switchW = switchW, switchH = switchH,
                        compact = compact
                    )
                }
            }

            if (guide.hasIndex) {
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .width(indexW)
                        .fillMaxHeight()
                ) {
                    letters.forEachIndexed { i, ch ->
                        val lit = if (i == hIndex) letterLit else 0f
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            if (lit > 0f) {
                                Box(
                                    modifier = Modifier
                                        .size(if (compact) 12.dp else 15.dp)
                                        .alpha(lit)
                                        .background(HaGold, CircleShape)
                                )
                            }
                            Text(
                                ch.toString(),
                                color = if (lit > 0.5f) HaObsidian else if (i == hIndex) HaGoldLight else HaIvory.copy(alpha = 0.55f),
                                fontSize = if (compact) 7.5.sp else 9.sp,
                                fontWeight = if (i == hIndex) FontWeight.Black else FontWeight.Medium,
                                lineHeight = if (compact) 8.sp else 10.sp
                            )
                        }
                    }
                }
                if (bubble > 0f) {
                    val bs = if (compact) 22.dp else 40.dp
                    Box(
                        modifier = Modifier
                            .offset(x = vw - indexW - bs - 6.dp, y = letterY - bs / 2)
                            .size(bs)
                            .alpha(bubble)
                            .background(Brush.verticalGradient(listOf(HaGoldPale, HaGold, HaGoldDeep)), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("H", color = HaObsidian, fontSize = if (compact) 12.sp else 20.sp, fontWeight = FontWeight.Black)
                    }
                }
            }

            Canvas(modifier = Modifier.fillMaxSize()) {
                touch(Offset(fingerX.toPx(), fingerY.toPx()), ripple, fingerAlpha, fingerR.toPx())
            }
        }
    }
}

@Composable
private fun MockRow(
    app: MockApp,
    isTarget: Boolean,
    highlight: Float,
    pressed: Float,
    allowed: Boolean,
    switchOn: Float,
    guide: OverlayGuide,
    rowH: Dp,
    iconS: Dp,
    nameSp: TextUnit,
    subSp: TextUnit,
    padH: Dp,
    endPad: Dp,
    switchW: Dp,
    switchH: Dp,
    compact: Boolean
) {
    val shape = RoundedCornerShape(if (compact) 6.dp else 12.dp)
    val bg = lerpColor(lerpColor(Color.Transparent, MockRowHighlight, highlight), Color(0xFF4A3B1C), pressed)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(rowH)
            .padding(horizontal = padH, vertical = if (compact) 1.5.dp else 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(bg, shape)
                .border(if (compact) 1.dp else 1.5.dp, HaGold.copy(alpha = 0.9f * highlight), shape)
                .padding(start = if (compact) 5.dp else 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIcon(app, iconS)
            Spacer(modifier = Modifier.width(if (compact) 6.dp else 12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    app.name,
                    color = if (isTarget) HaIvory else HaIvory.copy(alpha = 0.78f),
                    fontSize = nameSp,
                    fontWeight = if (isTarget) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    lineHeight = nameSp
                )
                if (guide.style == ListStyle.SubtitleStatus) {
                    Text(
                        if (allowed) guide.allowWord else guide.denyWord,
                        color = if (allowed) HaJade else HaMuted,
                        fontSize = subSp, maxLines = 1, lineHeight = subSp
                    )
                }
            }
            when (guide.style) {
                ListStyle.TrailingStatus -> Text(
                    (if (allowed) guide.allowWord else guide.denyWord) + "  ›",
                    color = if (allowed) HaJade else HaMuted,
                    fontSize = subSp, maxLines = 1
                )
                ListStyle.InlineSwitch -> MiniSwitch(switchOn, switchW, switchH)
                ListStyle.SubtitleStatus -> Text("›", color = HaMuted, fontSize = nameSp)
            }
            Spacer(modifier = Modifier.width(endPad))
        }
    }
}

@Composable
private fun DetailMock(
    guide: OverlayGuide,
    data: MockData,
    t: Float,
    done: Boolean,
    compact: Boolean,
    modifier: Modifier
) {
    val on = if (done) 1f else ease(seg(t, 0.34f, 0.50f))
    val move = ease(seg(t, 0f, 0.26f))
    val ripple = if (done) 0f else seg(t, 0.30f, 0.54f)
    val fingerAlpha = if (done) 0f else seg(t, 0f, 0.06f) * (1f - seg(t, 0.64f, 0.74f))
    val glow = if (done) 1f else 0.55f + 0.45f * seg(t, 0.4f, 0.5f)
    val fingerR = if (compact) 6.dp else 11.dp

    Column(modifier = modifier.clipToBounds()) {
        if (!compact) MockHeader(guide.screenName, showSearch = false)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (compact) 7.dp else 16.dp, vertical = if (compact) 6.dp else 18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(data.self, if (compact) 22.dp else 48.dp)
                Spacer(modifier = Modifier.width(if (compact) 7.dp else 14.dp))
                Column {
                    Text(
                        data.appName, color = HaIvory, fontSize = if (compact) 11.sp else 18.sp,
                        fontWeight = FontWeight.Bold, maxLines = 1, lineHeight = if (compact) 12.sp else 20.sp
                    )
                    if (!compact) Text("Alarm clock", color = HaMuted, fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(if (compact) 6.dp else 18.dp))
            val shape = RoundedCornerShape(if (compact) 8.dp else 14.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF17171C), shape)
                    .border(if (compact) 1.dp else 1.5.dp, HaGold.copy(alpha = 0.85f * glow), shape)
                    .padding(horizontal = if (compact) 7.dp else 14.dp, vertical = if (compact) 6.dp else 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    guide.toggleLabel,
                    color = HaIvory, fontSize = if (compact) 9.5.sp else 15.sp, fontWeight = FontWeight.SemiBold,
                    lineHeight = if (compact) 11.sp else 18.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(if (compact) 5.dp else 10.dp))
                MiniSwitch(
                    on = on,
                    width = if (compact) 24.dp else 44.dp,
                    height = if (compact) 14.dp else 26.dp,
                    modifier = Modifier.drawWithContent {
                        drawContent()
                        val end = Offset(size.width * (0.25f + 0.5f * on), size.height / 2)
                        val start = end + Offset(18.dp.toPx(), 26.dp.toPx())
                        val at = Offset(lerpF(start.x, end.x, move), lerpF(start.y, end.y, move))
                        touch(at, ripple, fingerAlpha, fingerR.toPx())
                    }
                )
            }
            if (!compact) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Lets ${data.appName} show your alarm on top of other apps, so you never miss it.",
                    color = HaMuted, fontSize = 12.sp, lineHeight = 16.sp
                )
            }
            if (done) {
                Spacer(modifier = Modifier.height(if (compact) 5.dp else 14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Check, null, tint = HaJade, modifier = Modifier.size(if (compact) 11.dp else 16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Allowed", color = HaJade, fontSize = if (compact) 9.sp else 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Suppress("unused")
private val GuideArrangement = Arrangement.spacedBy(4.dp)
