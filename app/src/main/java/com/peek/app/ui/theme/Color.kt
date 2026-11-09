package com.peek.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Colors follow Apple's semantic system palette (Human Interface Guidelines), so PEEK reads
 * like a native iOS surface: grouped backgrounds, layered labels, hairline separators and a
 * single tint color (system indigo, close to the PEEK brand violet).
 */
data class PeekColors(
    val tint: Color,
    val green: Color,
    val red: Color,
    val groupedBackground: Color,
    val cell: Color,
    val cellPressed: Color,
    val label: Color,
    val secondaryLabel: Color,
    val tertiaryLabel: Color,
    val separator: Color,
    val fill: Color,
    val incomingBubble: Color,
    val outgoingBubble: Color,
    val menu: Color,
    val scrim: Color,
    val avatarTop: Color,
    val avatarBottom: Color,
    val handle: Color,
)

val LightPeekColors = PeekColors(
    tint = Color(0xFF5856D6),
    green = Color(0xFF34C759),
    red = Color(0xFFFF3B30),
    groupedBackground = Color(0xFFF2F2F7),
    cell = Color(0xFFFFFFFF),
    cellPressed = Color(0xFFD1D1D6),
    label = Color(0xFF000000),
    secondaryLabel = Color(0x993C3C43),
    tertiaryLabel = Color(0x4D3C3C43),
    separator = Color(0x4A3C3C43),
    fill = Color(0x29787880),
    incomingBubble = Color(0xFFE9E9EB),
    outgoingBubble = Color(0xFF5856D6),
    menu = Color(0xF0F2F2F2),
    scrim = Color(0x33000000),
    avatarTop = Color(0xFFA5ABB9),
    avatarBottom = Color(0xFF858994),
    handle = Color(0x998E8E93),
)

val DarkPeekColors = PeekColors(
    tint = Color(0xFF5E5CE6),
    green = Color(0xFF30D158),
    red = Color(0xFFFF453A),
    groupedBackground = Color(0xFF000000),
    cell = Color(0xFF1C1C1E),
    cellPressed = Color(0xFF3A3A3C),
    label = Color(0xFFFFFFFF),
    secondaryLabel = Color(0x99EBEBF5),
    tertiaryLabel = Color(0x4DEBEBF5),
    separator = Color(0x99545458),
    fill = Color(0x5C787880),
    incomingBubble = Color(0xFF262629),
    outgoingBubble = Color(0xFF5E5CE6),
    menu = Color(0xF02C2C2E),
    scrim = Color(0x66000000),
    avatarTop = Color(0xFF6C6C70),
    avatarBottom = Color(0xFF48484A),
    handle = Color(0x99AEAEB2),
)
