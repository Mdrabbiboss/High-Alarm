package com.programmer.highalarm

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

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
