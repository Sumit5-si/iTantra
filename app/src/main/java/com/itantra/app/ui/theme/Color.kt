package com.itantra.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * iTantra Color Palette — centralized color definitions.
 *
 * Primary colors derived from the hand-drawn design specification:
 *   #043168 (Dark Blue), #1F7A83 (Light Blue), #26DA18 (Green), #F09E4C (Yellow/Amber)
 *
 * Change secondary values here if the exact hex codes need adjustment —
 * no UI components reference raw hex values directly.
 */

// ══════════════════════════════════════════════
//  Brand Colors (from hand-drawn specification)
// ══════════════════════════════════════════════

val iTantraPrimary = Color(0xFF043168)       // Dark Blue — primary actions, headers
val iTantraLightBlue = Color(0xFF1F7A83)     // Light Blue — secondary elements, accents
val iTantraSuccess = Color(0xFF26DA18)       // Green — connected, success states
val iTantraWarning = Color(0xFFF09E4C)       // Yellow/Amber — warnings, emergency hold
val iTantraError = Color(0xFFDC3545)         // Red — errors, emergency active state

// ══════════════════════════════════════════════
//  Neutral Support Colors
// ══════════════════════════════════════════════

val iTantraWhite = Color(0xFFFFFFFF)
val iTantraBlack = Color(0xFF000000)
val iTantraLightBackground = Color(0xFFF8F9FA)
val iTantraDarkBackground = Color(0xFF121212)
val iTantraSurface = Color(0xFFFFFFFF)
val iTantraSurfaceDark = Color(0xFF1E1E2E)
val iTantraTextPrimary = Color(0xFF1A1A2E)
val iTantraTextPrimaryDark = Color(0xFFE8E8E8)
val iTantraTextSecondary = Color(0xFF6C757D)
val iTantraTextSecondaryDark = Color(0xFF9CA3AF)
val iTantraDivider = Color(0xFFE0E0E0)
val iTantraDividerDark = Color(0xFF2D2D3D)
val iTantraCardBackground = Color(0xFFFFFFFF)
val iTantraCardBackgroundDark = Color(0xFF252536)

// ══════════════════════════════════════════════
//  Semantic Aliases (used in components)
// ══════════════════════════════════════════════

val iTantraListening = Color(0xFF26DA18)     // Mic active/listening pulse
val iTantraProcessing = iTantraLightBlue     // Processing indicator
val iTantraSending = iTantraPrimary          // Transmitting state
val iTantraEmergencyBackground = Color(0xFFFFF3CD) // Soft yellow emergency card bg
val iTantraEmergencyBackgroundDark = Color(0xFF3D2E0F)

// ══════════════════════════════════════════════
//  Logo Identity Colors (from logo analysis)
// ══════════════════════════════════════════════

val iTantraLogoOrange = Color(0xFFE8882E)    // Voice waveform
val iTantraLogoBlue = Color(0xFF1A5BA8)      // Ashoka Chakra & signal arcs
val iTantraFlagSaffron = Color(0xFFFF9933)   // Tricolour underline
val iTantraFlagGreen = Color(0xFF138808)     // Tricolour underline
