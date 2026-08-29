package com.batteryvisibility.app.domain.model

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

enum class AlignmentOption {
    START, CENTER, END
}

enum class ColorMode {
    THEME, CUSTOM
}

enum class BackgroundStyle {
    SOLID, TRANSPARENT, SURFACE
}

data class DisplaySettings(
    val textScale: Float = 1.0f,
    val iconScale: Float = 1.0f,
    val alignment: AlignmentOption = AlignmentOption.CENTER,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val textColorMode: ColorMode = ColorMode.THEME,
    val iconColorMode: ColorMode = ColorMode.THEME,
    val backgroundStyle: BackgroundStyle = BackgroundStyle.SURFACE,
    val secondaryInfoEnabled: Boolean = true
)
