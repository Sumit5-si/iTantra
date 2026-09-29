package com.itantra.app.ui.theme

import androidx.compose.ui.unit.dp

/**
 * iTantra dimension constants for consistent spacing, sizing, and responsive layouts.
 * All touch targets meet 48dp minimum for accessibility.
 */
object Dimens {
    // ── Spacing ──
    val spacingXxs = 2.dp
    val spacingXs = 4.dp
    val spacingSm = 8.dp
    val spacingMd = 12.dp
    val spacingLg = 16.dp
    val spacingXl = 24.dp
    val spacingXxl = 32.dp
    val spacingXxxl = 48.dp

    // ── Corner Radius ──
    val radiusSm = 8.dp
    val radiusMd = 12.dp
    val radiusLg = 16.dp
    val radiusXl = 24.dp
    val radiusRound = 50.dp    // Fully rounded (pills, FABs)

    // ── Elevation ──
    val elevationNone = 0.dp
    val elevationSm = 1.dp
    val elevationMd = 2.dp
    val elevationLg = 4.dp

    // ── Touch Targets (min 48dp for accessibility) ──
    val touchTargetMin = 48.dp
    val buttonHeight = 48.dp
    val chipHeight = 36.dp
    val iconButtonSize = 48.dp

    // ── Voice Button ──
    val voiceButtonSize = 96.dp
    val voiceButtonRippleRadius = 120.dp

    // ── Cards ──
    val cardElevation = 2.dp
    val cardPadding = 16.dp

    // ── Top Bar ──
    val topBarHeight = 56.dp
    val topBarLogoHeight = 32.dp

    // ── Connection Card ──
    val connectionCardHeight = 80.dp

    // ── Mode Selector Cards ──
    val modeSelectorHeight = 56.dp

    // ── Message Bubble ──
    val messageBubbleMaxWidth = 280.dp
    val messageBubbleRadius = 16.dp

    // ── Emergency Button ──
    val emergencyButtonHeight = 56.dp

    // ── Device Card ──
    val deviceCardHeight = 72.dp

    // ── Scan Animation ──
    val scanRadarSize = 200.dp

    // ── Screen Padding ──
    val screenPaddingHorizontal = 16.dp
    val screenPaddingVertical = 8.dp
}
