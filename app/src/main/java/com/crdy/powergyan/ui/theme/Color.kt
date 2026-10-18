package com.crdy.powergyan.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.crdy.powergyan.domain.model.AccentColor

// Cool slate neutrals; the accent is the only saturated color on screen.
val SlateLight = Color(0xFFF2F4F7)
val SlateLightSurface = Color(0xFFFFFFFF)
val SlateLightVariant = Color(0xFFE6E9EF)
val InkLight = Color(0xFF161A22)

val SlateDark = Color(0xFF0F1217)
val SlateDarkSurface = Color(0xFF171B22)
val SlateDarkVariant = Color(0xFF222833)
val InkDark = Color(0xFFE6E9EF)

val Cobalt = Color(0xFF2F5BD3)
val Lithium = Color(0xFFD23C3C)

fun AccentColor.color() = when (this) {
    AccentColor.MINT -> Color(0xFF00A889)
    AccentColor.BLUE -> Cobalt
    AccentColor.VIOLET -> Color(0xFF7655D6)
    AccentColor.ORANGE -> Color(0xFFE88A28)
    AccentColor.ROSE -> Color(0xFFD94C72)
}

/** Black or white, whichever reads better on [background]. */
fun contentOn(background: Color) = if (background.luminance() > 0.4f) Color.Black else Color.White
