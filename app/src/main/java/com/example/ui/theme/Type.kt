package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val PlusJakartaSansFamily = FontFamily(
    Font(R.font.plus_jakarta_sans, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans, FontWeight.Bold)
)

val AtkinsonHyperlegibleFamily = FontFamily(
    Font(R.font.atkinson_hyperlegible, FontWeight.Normal),
    Font(R.font.atkinson_hyperlegible, FontWeight.Bold)
)

fun vaaniTypography(extraLarge: Boolean = false): Typography {
    val scale = if (extraLarge) 1.22f else 1.0f
    return Typography(
        displaySmall = TextStyle(
            fontFamily = PlusJakartaSansFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (30 * scale).sp,
            lineHeight = (38 * scale).sp,
            letterSpacing = (-0.25).sp
        ),
        headlineMedium = TextStyle(
            fontFamily = PlusJakartaSansFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (24 * scale).sp,
            lineHeight = (32 * scale).sp
        ),
        headlineSmall = TextStyle(
            fontFamily = PlusJakartaSansFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (21 * scale).sp,
            lineHeight = (28 * scale).sp
        ),
        titleLarge = TextStyle(
            fontFamily = PlusJakartaSansFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (20 * scale).sp,
            lineHeight = (28 * scale).sp
        ),
        titleMedium = TextStyle(
            fontFamily = PlusJakartaSansFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (18 * scale).sp,
            lineHeight = (25 * scale).sp
        ),
        titleSmall = TextStyle(
            fontFamily = PlusJakartaSansFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (16 * scale).sp,
            lineHeight = (22 * scale).sp
        ),
        bodyLarge = TextStyle(
            fontFamily = AtkinsonHyperlegibleFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (18 * scale).sp,
            lineHeight = (27 * scale).sp,
            letterSpacing = 0.2.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = AtkinsonHyperlegibleFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (16 * scale).sp,
            lineHeight = (24 * scale).sp,
            letterSpacing = 0.15.sp
        ),
        bodySmall = TextStyle(
            fontFamily = AtkinsonHyperlegibleFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (14 * scale).sp,
            lineHeight = (20 * scale).sp
        ),
        labelLarge = TextStyle(
            fontFamily = PlusJakartaSansFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (16 * scale).sp,
            lineHeight = (22 * scale).sp
        ),
        labelMedium = TextStyle(
            fontFamily = PlusJakartaSansFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (14 * scale).sp,
            lineHeight = (19 * scale).sp
        ),
        labelSmall = TextStyle(
            fontFamily = PlusJakartaSansFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (12 * scale).sp,
            lineHeight = (16 * scale).sp
        )
    )
}

val Typography = vaaniTypography(extraLarge = false)
