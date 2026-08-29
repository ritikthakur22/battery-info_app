package com.crdy.batterygyan.domain.model

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

enum class AccentColor {
    MINT, BLUE, VIOLET, ORANGE, ROSE
}

data class DisplaySettings(
    val textScale: Float = 1.0f,
    val iconScale: Float = 1.0f,
    val alignment: AlignmentOption = AlignmentOption.CENTER,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val textColorMode: ColorMode = ColorMode.THEME,
    val iconColorMode: ColorMode = ColorMode.THEME,
    val backgroundStyle: BackgroundStyle = BackgroundStyle.SURFACE,
    val accentColor: AccentColor = AccentColor.MINT,
    val alertPolicy: AlertPolicy = AlertPolicy(),
    val secondaryInfoEnabled: Boolean = true
)
