package com.programmer.highalarm

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * Shared 3D building blocks. One rule everywhere: light falls from straight
 * above, so raised things are lighter on top with a bright top rim and a dark
 * bottom rim; recessed things are the reverse. All fills are opaque so drop
 * shadows never bleed through as dark smudges.
 */

/** Meaning of a coin/badge, so colour follows logic (gold = info/action, jade = success, coral = warning). */
enum class HaTone { Gold, Jade, Coral, Steel }

private fun toneStops(tone: HaTone): List<Color> = when (tone) {
    HaTone.Gold -> listOf(HaGoldPale, HaGoldLight, HaGold, HaGoldMid, HaGoldDeep)
    HaTone.Jade -> listOf(Color(0xFFD5F7E7), HaJadeLight, HaJade, Color(0xFF34A97A), HaJadeDeep)
    HaTone.Coral -> listOf(Color(0xFFFFD9D6), Color(0xFFFF9B95), HaCoral, Color(0xFFD94A4A), HaCoralDeep)
    HaTone.Steel -> listOf(Color(0xFFF6F7FA), Color(0xFFDCDDE3), Color(0xFFB5B7C0), Color(0xFF8D8F99), Color(0xFF5E606A))
}

private fun toneRimBottom(tone: HaTone): Color = when (tone) {
    HaTone.Gold -> HaBronze
    HaTone.Jade -> Color(0xFF14523C)
    HaTone.Coral -> Color(0xFF6E1F24)
    HaTone.Steel -> Color(0xFF3E4048)
}

/** Ink colour that reads on top of a toned coin. */
fun haToneInk(tone: HaTone): Color = when (tone) {
    HaTone.Gold -> Color(0xFF2A1F0E)
    HaTone.Jade -> Color(0xFF0B2A1E)
    HaTone.Coral -> Color(0xFF3A0D10)
    HaTone.Steel -> Color(0xFF1C1D22)
}

/** Raised metal coin in the given tone. */
fun Modifier.ha3DCoin(tone: HaTone, elevation: Dp = 8.dp): Modifier = this
    .shadow(elevation, CircleShape, ambientColor = Color.Black, spotColor = Color.Black)
    .clip(CircleShape)
    .background(Brush.verticalGradient(toneStops(tone)))
    .border(
        BorderStroke(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.75f), toneRimBottom(tone)))),
        CircleShape
    )

/** Raised gold face for any shape (selected chips, primary buttons). */
fun Modifier.ha3DGoldFace(shape: Shape, elevation: Dp = 8.dp): Modifier = this
    .shadow(elevation, shape, ambientColor = Color.Black, spotColor = Color.Black)
    .clip(shape)
    .background(Brush.verticalGradient(listOf(HaGoldPale, HaGoldLight, HaGold, HaGoldMid)))
    .border(
        BorderStroke(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.7f), HaBronze))),
        shape
    )

/** Recessed groove (input fields, tracks, stages). */
fun Modifier.ha3DInset(shape: Shape): Modifier = this
    .clip(shape)
    .background(Brush.verticalGradient(listOf(Color(0xFF060609), Color(0xFF0E0E12), Color(0xFF16161B))))
    .border(
        BorderStroke(1.dp, Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.85f), Color.White.copy(alpha = 0.10f)))),
        shape
    )

/** Selected-state dark panel warmed with gold (for selected tiles). */
fun Modifier.ha3DPanelSelected(shape: Shape, elevation: Dp = 12.dp): Modifier = this
    .shadow(elevation, shape, ambientColor = HaGold.copy(alpha = 0.6f), spotColor = Color.Black)
    .clip(shape)
    .background(Brush.verticalGradient(listOf(Color(0xFF3A3020), Color(0xFF262016), Color(0xFF17140F))))
    .border(
        BorderStroke(1.5.dp, Brush.verticalGradient(listOf(HaGoldPale, HaGold.copy(alpha = 0.6f), HaGoldDeep.copy(alpha = 0.8f)))),
        shape
    )

/** Primary 3D gold button that physically presses down when touched. */
@Composable
fun HaGoldButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 56.dp,
    enabled: Boolean = true
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed) 1f else 0f, label = "ha_btn_press")
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .graphicsLayer {
                val s = 1f - 0.03f * press
                scaleX = s; scaleY = s
                translationY = 2.dp.toPx() * press
                alpha = if (enabled) 1f else 0.5f
            }
            .ha3DGoldFace(shape, elevation = if (pressed) 3.dp else 12.dp)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color(0xFF1A140A), fontWeight = FontWeight.Black, fontSize = 16.sp)
    }
}
