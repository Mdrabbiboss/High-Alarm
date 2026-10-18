package com.programmer.highalarm

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/*
 * HighAlarm — "Obsidian & Champagne Gold" design system.
 * Every color in the app is drawn from this single palette so that
 * backgrounds, surfaces, accents and status colors always harmonise.
 */

// ── Neutrals (cool-warm obsidian, very low saturation) ──────────────────
val HaObsidian = Color(0xFF07070A)      // app background
val HaInk = Color(0xFF0E0E13)           // deep panels
val HaSurfaceLow = Color(0xFF141419)    // low surfaces
val HaSurface = Color(0xFF1A1A21)       // cards
val HaSurfaceHigh = Color(0xFF24242C)   // raised / pressed
val HaHairline = Color(0xFF2A2A33)      // thin strokes
val HaUmber = Color(0xFF241C12)         // warm deep tint for premium panels

// ── Text ────────────────────────────────────────────────────────────────
val HaIvory = Color(0xFFF2EEE6)         // primary readable text (warm white)
val HaMuted = Color(0xFF8E8B95)         // secondary text
val HaTrackOff = Color(0xFF3A3A44)      // disabled / switch track off

// ── Champagne gold accent family ────────────────────────────────────────
val HaGoldPale = Color(0xFFF8E7BE)
val HaGoldLight = Color(0xFFF0D594)
val HaGold = Color(0xFFE6BE6A)          // primary accent
val HaGoldMid = Color(0xFFD1A04B)
val HaGoldDeep = Color(0xFFA67A2E)
val HaBronze = Color(0xFF6E5021)

// ── Status (tuned to sit beside gold) ───────────────────────────────────
val HaJade = Color(0xFF5FD4A0)          // success
val HaJadeLight = Color(0xFFA9EBCB)
val HaJadeDeep = Color(0xFF1F7A5A)
val HaCoral = Color(0xFFFF6B6B)         // danger / error
val HaCoralDeep = Color(0xFFB43A3A)
val HaWine = Color(0xFF6E1F2C)
val HaSky = Color(0xFF7FC4E8)           // water / info (used sparingly)
val HaRose = Color(0xFFE8A7A0)          // rose gold
val HaPlatinum = Color(0xFFD9DCE3)

// ── Signature gradients ─────────────────────────────────────────────────
val HaGoldGradient: Brush
    get() = Brush.linearGradient(listOf(HaGoldPale, HaGold, HaGoldDeep))

val HaGoldHorizontal: Brush
    get() = Brush.horizontalGradient(listOf(HaGoldLight, HaGold, HaGoldMid))

// ── 3D surface system (light falls from straight above) ────────────────
val HaPanel = Color(0xFF1C1C22)
val HaPanelTop = Color(0xFF25242C)
val HaPanelBottom = Color(0xFF141418)

val HaPanelBrush: Brush
    get() = Brush.verticalGradient(listOf(HaPanelTop, HaPanel, HaPanelBottom))

/** Bevelled edge: lit top rim, dark bottom rim. */
val Ha3DBorder: androidx.compose.foundation.BorderStroke
    get() = androidx.compose.foundation.BorderStroke(
        1.dp,
        Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.04f), Color.Black.copy(alpha = 0.6f))
        )
    )

/** Same bevel with a champagne-gold top rim. */
val Ha3DGoldBorder: androidx.compose.foundation.BorderStroke
    get() = androidx.compose.foundation.BorderStroke(
        1.2.dp,
        Brush.verticalGradient(
            listOf(HaGoldPale.copy(alpha = 0.6f), HaGold.copy(alpha = 0.18f), Color.Black.copy(alpha = 0.65f))
        )
    )

/** Raised, top-lit panel with a real drop shadow. Background is opaque so no shadow bleeds through. */
fun Modifier.ha3DPanel(
    shape: Shape,
    gold: Boolean = false,
    elevation: Dp = 14.dp
): Modifier = this
    .shadow(elevation, shape, ambientColor = Color.Black, spotColor = Color.Black)
    .clip(shape)
    .background(HaPanelBrush)
    .border(if (gold) Ha3DGoldBorder else Ha3DBorder, shape)

/** Small raised dark orb for icons. */
fun Modifier.ha3DOrb(): Modifier = this
    .shadow(6.dp, CircleShape, ambientColor = Color.Black, spotColor = Color.Black)
    .clip(CircleShape)
    .background(Brush.verticalGradient(listOf(Color(0xFF302F38), Color(0xFF1B1B21), Color(0xFF121216))))
    .border(Ha3DBorder, CircleShape)

/** Small raised gold coin for active icons. */
fun Modifier.ha3DGoldCoin(): Modifier = this
    .shadow(6.dp, CircleShape, ambientColor = Color.Black, spotColor = Color.Black)
    .clip(CircleShape)
    .background(Brush.verticalGradient(listOf(HaGoldPale, HaGoldLight, HaGold, HaGoldMid, HaGoldDeep)))
    .border(
        androidx.compose.foundation.BorderStroke(
            1.dp,
            Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.7f), HaBronze))
        ),
        CircleShape
    )
