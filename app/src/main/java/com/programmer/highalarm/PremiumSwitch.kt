package com.programmer.highalarm

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/**
 * HighAlarm 3D toggle.
 * OFF: recessed dark groove with a bright silver knob (always clearly visible).
 * ON : polished gold groove with a dark obsidian knob.
 * Pass onCheckedChange = null to make it display-only (parent handles clicks).
 */
@Composable
fun HaSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val trackW = 52.dp
    val trackH = 30.dp
    val knob = 24.dp
    val pad = 3.dp

    val knobOffset by animateDpAsState(
        targetValue = if (checked) trackW - knob - pad * 2 else 0.dp,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 520f),
        label = "ha_switch_knob"
    )
    val glow by animateColorAsState(
        targetValue = if (checked) HaGold.copy(alpha = 0.35f) else Color.Black,
        label = "ha_switch_glow"
    )

    val trackShape = RoundedCornerShape(50)
    val toggleModifier = if (onCheckedChange != null) {
        Modifier.toggleable(
            value = checked,
            enabled = enabled,
            role = Role.Switch,
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onValueChange = onCheckedChange
        )
    } else Modifier

    Box(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.5f)
            .then(toggleModifier)
            .size(trackW, trackH)
            .shadow(if (checked) 8.dp else 2.dp, trackShape, ambientColor = glow, spotColor = glow)
            .clip(trackShape)
            .background(
                if (checked) {
                    // polished gold, lit from above
                    Brush.verticalGradient(listOf(HaGoldPale, HaGold, HaGoldMid, HaGoldDeep))
                } else {
                    // recessed groove: darker at the top (inner shadow), lighter at the bottom
                    Brush.verticalGradient(listOf(Color(0xFF07070A), Color(0xFF15151A), Color(0xFF26262E)))
                }
            )
            .border(
                1.dp,
                if (checked) {
                    Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.55f), HaBronze))
                } else {
                    Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.8f), Color.White.copy(alpha = 0.14f)))
                },
                trackShape
            )
            .padding(pad),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = knobOffset)
                .size(knob)
                .shadow(4.dp, CircleShape, ambientColor = Color.Black, spotColor = Color.Black)
                .clip(CircleShape)
                .background(
                    if (checked) {
                        Brush.verticalGradient(listOf(Color(0xFF3A3842), Color(0xFF16151A), Color(0xFF0A0A0D)))
                    } else {
                        Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFDCDBE0), Color(0xFFA9A8B0)))
                    }
                )
                .border(
                    0.8.dp,
                    if (checked) {
                        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.25f), Color.Black))
                    } else {
                        Brush.verticalGradient(listOf(Color.White, Color(0xFF7D7C85)))
                    },
                    CircleShape
                )
        )
    }
}
