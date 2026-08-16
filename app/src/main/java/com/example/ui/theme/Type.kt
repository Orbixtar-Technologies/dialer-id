package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val SignalSans = FontFamily.SansSerif

/**
 * Confident, product-grade type scale.
 *
 * Display / headline styles carry the phone-number and timer personality:
 * slightly tight titles, slightly open numerals. Body stays readable at 16/24.
 */
val Typography =
    Typography(
        displayLarge =
            TextStyle(
                fontFamily = SignalSans,
                fontWeight = FontWeight.Light,
                fontSize = 56.sp,
                lineHeight = 64.sp,
                letterSpacing = (-1).sp
            ),
        displayMedium =
            TextStyle(
                fontFamily = SignalSans,
                fontWeight = FontWeight.Medium,
                fontSize = 44.sp,
                lineHeight = 52.sp,
                letterSpacing = (-0.5).sp
            ),
        displaySmall =
            TextStyle(
                fontFamily = SignalSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 36.sp,
                lineHeight = 44.sp,
                letterSpacing = 0.4.sp
            ),
        headlineLarge =
            TextStyle(
                fontFamily = SignalSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 32.sp,
                lineHeight = 40.sp,
                letterSpacing = 0.2.sp
            ),
        headlineMedium =
            TextStyle(
                fontFamily = SignalSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 28.sp,
                lineHeight = 36.sp,
                letterSpacing = 0.4.sp
            ),
        headlineSmall =
            TextStyle(
                fontFamily = SignalSans,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                lineHeight = 32.sp,
                letterSpacing = (-0.2).sp
            ),
        titleLarge =
            TextStyle(
                fontFamily = SignalSans,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                lineHeight = 28.sp,
                letterSpacing = (-0.4).sp
            ),
        titleMedium =
            TextStyle(
                fontFamily = SignalSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.sp
            ),
        titleSmall =
            TextStyle(
                fontFamily = SignalSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.1.sp
            ),
        bodyLarge =
            TextStyle(
                fontFamily = SignalSans,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.15.sp
            ),
        bodyMedium =
            TextStyle(
                fontFamily = SignalSans,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.15.sp
            ),
        bodySmall =
            TextStyle(
                fontFamily = SignalSans,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.2.sp
            ),
        labelLarge =
            TextStyle(
                fontFamily = SignalSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.1.sp
            ),
        labelMedium =
            TextStyle(
                fontFamily = SignalSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.3.sp
            ),
        labelSmall =
            TextStyle(
                fontFamily = SignalSans,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.4.sp
            )
    )
