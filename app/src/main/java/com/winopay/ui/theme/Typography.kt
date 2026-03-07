package com.winopay.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.winopay.R

// Albert Sans font family - weights 400, 500, 600, 700
val AlbertSansFontFamily = FontFamily(
    Font(R.font.albert_sans_regular, FontWeight.Normal),   // 400
    Font(R.font.albert_sans_medium, FontWeight.Medium),    // 500
    Font(R.font.albert_sans_semibold, FontWeight.SemiBold), // 600
    Font(R.font.albert_sans_bold, FontWeight.Bold)         // 700
)

// Typography sizes from WinoUI mobile tokens
object WinoTypography {
    // Display - 36sp / 44sp line height / -3% letter spacing
    val display = TextStyle(
        fontFamily = AlbertSansFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.03).em
    )

    val displayMedium = TextStyle(
        fontFamily = AlbertSansFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.03).em
    )

    // H1 - 28sp / 36sp line height
    val h1 = TextStyle(
        fontFamily = AlbertSansFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp
    )

    val h1SemiBold = TextStyle(
        fontFamily = AlbertSansFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp
    )

    val h1Medium = TextStyle(
        fontFamily = AlbertSansFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 28.sp,
        lineHeight = 36.sp
    )

    // H2 - 22sp / 30sp line height
    val h2 = TextStyle(
        fontFamily = AlbertSansFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 30.sp
    )

    val h2SemiBold = TextStyle(
        fontFamily = AlbertSansFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 30.sp
    )

    val h2Medium = TextStyle(
        fontFamily = AlbertSansFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 30.sp
    )

    // H3 - 18sp / 26sp line height
    val h3 = TextStyle(
        fontFamily = AlbertSansFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 26.sp
    )

    val h3SemiBold = TextStyle(
        fontFamily = AlbertSansFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 26.sp
    )

    val h3Medium = TextStyle(
        fontFamily = AlbertSansFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 26.sp
    )

    // Body - 16sp / 24sp line height / -2% letter spacing
    val body = TextStyle(
        fontFamily = AlbertSansFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.02).em
    )

    val bodyMedium = TextStyle(
        fontFamily = AlbertSansFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.02).em
    )

    val bodySemiBold = TextStyle(
        fontFamily = AlbertSansFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.02).em
    )

    val bodyBold = TextStyle(
        fontFamily = AlbertSansFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.02).em
    )

    // Small - 14sp / 20sp line height
    val small = TextStyle(
        fontFamily = AlbertSansFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    )

    val smallMedium = TextStyle(
        fontFamily = AlbertSansFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    )

    val smallSemiBold = TextStyle(
        fontFamily = AlbertSansFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    )

    // Micro - 12sp / 16sp line height
    val micro = TextStyle(
        fontFamily = AlbertSansFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )

    val microMedium = TextStyle(
        fontFamily = AlbertSansFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )

    val microSemiBold = TextStyle(
        fontFamily = AlbertSansFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )
}
