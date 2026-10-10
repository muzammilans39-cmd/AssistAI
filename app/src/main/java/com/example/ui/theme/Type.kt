package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val LexendFontFamily = FontFamily(
    Font(R.font.lexend, FontWeight.Normal),
    Font(R.font.lexend, FontWeight.Medium),
    Font(R.font.lexend, FontWeight.SemiBold),
    Font(R.font.lexend, FontWeight.Bold)
)

fun buildAccessibleTypography(scale: Float = 1.0f): Typography {
    return Typography(
        displayLarge = TextStyle(
            fontFamily = LexendFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (36 * scale).sp,
            lineHeight = (44 * scale).sp
        ),
        displayMedium = TextStyle(
            fontFamily = LexendFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (30 * scale).sp,
            lineHeight = (38 * scale).sp
        ),
        headlineLarge = TextStyle(
            fontFamily = LexendFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (28 * scale).sp,
            lineHeight = (36 * scale).sp
        ),
        headlineMedium = TextStyle(
            fontFamily = LexendFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (24 * scale).sp,
            lineHeight = (32 * scale).sp
        ),
        headlineSmall = TextStyle(
            fontFamily = LexendFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (22 * scale).sp,
            lineHeight = (28 * scale).sp
        ),
        titleLarge = TextStyle(
            fontFamily = LexendFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (21 * scale).sp,
            lineHeight = (28 * scale).sp
        ),
        titleMedium = TextStyle(
            fontFamily = LexendFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (19 * scale).sp,
            lineHeight = (26 * scale).sp
        ),
        titleSmall = TextStyle(
            fontFamily = LexendFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (17 * scale).sp,
            lineHeight = (24 * scale).sp
        ),
        bodyLarge = TextStyle(
            fontFamily = LexendFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (18 * scale).sp,
            lineHeight = (26 * scale).sp,
            letterSpacing = 0.3.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = LexendFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (16 * scale).sp,
            lineHeight = (24 * scale).sp,
            letterSpacing = 0.25.sp
        ),
        labelLarge = TextStyle(
            fontFamily = LexendFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (17 * scale).sp,
            lineHeight = (24 * scale).sp,
            letterSpacing = 0.4.sp
        ),
        labelMedium = TextStyle(
            fontFamily = LexendFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (15 * scale).sp,
            lineHeight = (20 * scale).sp
        )
    )
}

val Typography = buildAccessibleTypography(1.0f)
