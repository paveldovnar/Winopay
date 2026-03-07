package com.winopay.ui.theme

import androidx.compose.ui.graphics.Color

// Primitive Brand Colors
object WinoBrand {
    val Brand100 = Color(0xFFF1ECFF)
    val Brand200 = Color(0xFFD8CCFF)
    val Brand300 = Color(0xFFBFA8FF)
    val Brand400 = Color(0xFFA486FF)
    val Brand500 = Color(0xFF7C5CFF)
    val Brand600 = Color(0xFF6346E0)
    val Brand700 = Color(0xFF4C34B5)
    val Brand800 = Color(0xFF372587)
    val Brand900 = Color(0xFF23175A)
}

// Primitive Success Colors
object WinoSuccess {
    val Success100 = Color(0xFFE7FBF4)
    val Success200 = Color(0xFFB9F3DB)
    val Success300 = Color(0xFF87E9C1)
    val Success400 = Color(0xFF4AD9A0)
    val Success500 = Color(0xFF16C784)
    val Success600 = Color(0xFF0FA66C)
    val Success700 = Color(0xFF0A8154)
    val Success800 = Color(0xFF075C3C)
    val Success900 = Color(0xFF043625)
}

// Primitive Warning Colors
object WinoWarning {
    val Warning100 = Color(0xFFFFF9E6)
    val Warning200 = Color(0xFFFFEAB5)
    val Warning300 = Color(0xFFFFD77D)
    val Warning400 = Color(0xFFFFC14A)
    val Warning500 = Color(0xFFF2A013)
    val Warning600 = Color(0xFFD17F0E)
    val Warning700 = Color(0xFFAA600A)
    val Warning800 = Color(0xFF7E4307)
    val Warning900 = Color(0xFF4E2904)
}

// Primitive Error Colors
object WinoError {
    val Error100 = Color(0xFFFFE8EC)
    val Error200 = Color(0xFFFFB9C6)
    val Error300 = Color(0xFFFF859C)
    val Error400 = Color(0xFFFF526F)
    val Error500 = Color(0xFFF2304E)
    val Error600 = Color(0xFFD1183A)
    val Error700 = Color(0xFFAA0D2C)
    val Error800 = Color(0xFF7D081F)
    val Error900 = Color(0xFF4D0413)
}

// Primitive Info Colors
object WinoInfo {
    val Info100 = Color(0xFFE6F3FF)
    val Info200 = Color(0xFFB7DCFF)
    val Info300 = Color(0xFF86C2FF)
    val Info400 = Color(0xFF55A7FF)
    val Info500 = Color(0xFF2A8BFF)
    val Info600 = Color(0xFF186ED4)
    val Info700 = Color(0xFF1053A6)
    val Info800 = Color(0xFF0A3876)
    val Info900 = Color(0xFF052047)
}

// Primitive Neutral Colors
object WinoNeutral {
    val Neutral0 = Color(0xFFFFFFFF)
    val Neutral50 = Color(0xFFF7F7FB)
    val Neutral100 = Color(0xFFE5E6F0)
    val Neutral200 = Color(0xFFD1D3E3)
    val Neutral300 = Color(0xFFB9BDD4)
    val Neutral400 = Color(0xFF9FA4C0)
    val Neutral500 = Color(0xFF858AAC)
    val Neutral600 = Color(0xFF6B708F)
    val Neutral700 = Color(0xFF4F546E)
    val Neutral800 = Color(0xFF353950)
    val Neutral900 = Color(0xFF1D2032)
    val Neutral1000 = Color(0xFF080817)
}

// Dark Theme Semantic Colors
data class WinoSemanticColors(
    // Text
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textMuted: Color,
    val textDisabled: Color,
    val textOnBrand: Color,
    val textOnCritical: Color,
    val textInverse: Color,

    // Background
    val bgCanvas: Color,
    val bgSurface: Color,
    val bgSurfaceAlt: Color,
    val bgElevated: Color,
    val bgAccentSoft: Color,
    val bgDestructiveSoft: Color,

    // Border
    val borderSubtle: Color,
    val borderDefault: Color,
    val borderStrong: Color,
    val borderBrand: Color,
    val borderCritical: Color,

    // Brand
    val brandPrimary: Color,
    val brandSoft: Color,
    val brandStrong: Color,
    val brandText: Color,

    // State
    val stateSuccess: Color,
    val stateSuccessSoft: Color,
    val stateWarning: Color,
    val stateWarningSoft: Color,
    val stateError: Color,
    val stateErrorSoft: Color,
    val stateInfo: Color,
    val stateInfoSoft: Color,

    // Financial
    val financialPositive: Color,
    val financialNegative: Color,
    val financialNeutral: Color,
    val financialPending: Color,
    val financialFee: Color
)

val WinoDarkColors = WinoSemanticColors(
    // Text
    textPrimary = WinoNeutral.Neutral0,
    textSecondary = WinoNeutral.Neutral200,
    textTertiary = WinoNeutral.Neutral400,
    textMuted = WinoNeutral.Neutral500,
    textDisabled = WinoNeutral.Neutral600,
    textOnBrand = WinoNeutral.Neutral0,
    textOnCritical = WinoNeutral.Neutral0,
    textInverse = WinoNeutral.Neutral900,

    // Background
    bgCanvas = WinoNeutral.Neutral1000,
    bgSurface = WinoNeutral.Neutral900,
    bgSurfaceAlt = WinoNeutral.Neutral800,
    bgElevated = WinoNeutral.Neutral800,
    bgAccentSoft = WinoBrand.Brand900,
    bgDestructiveSoft = WinoError.Error900,

    // Border
    borderSubtle = WinoNeutral.Neutral900,
    borderDefault = WinoNeutral.Neutral800,
    borderStrong = WinoNeutral.Neutral700,
    borderBrand = WinoBrand.Brand400,
    borderCritical = WinoError.Error400,

    // Brand
    brandPrimary = WinoBrand.Brand500,
    brandSoft = WinoBrand.Brand800,
    brandStrong = WinoBrand.Brand600,
    brandText = WinoNeutral.Neutral0,

    // State
    stateSuccess = WinoSuccess.Success400,
    stateSuccessSoft = WinoSuccess.Success800,
    stateWarning = WinoWarning.Warning400,
    stateWarningSoft = WinoWarning.Warning800,
    stateError = WinoError.Error400,
    stateErrorSoft = WinoError.Error800,
    stateInfo = WinoInfo.Info400,
    stateInfoSoft = WinoInfo.Info800,

    // Financial
    financialPositive = WinoSuccess.Success400,
    financialNegative = WinoError.Error400,
    financialNeutral = WinoNeutral.Neutral200,
    financialPending = WinoWarning.Warning400,
    financialFee = WinoNeutral.Neutral500
)

val WinoLightColors = WinoSemanticColors(
    // Text
    textPrimary = WinoNeutral.Neutral900,
    textSecondary = WinoNeutral.Neutral700,
    textTertiary = WinoNeutral.Neutral500,
    textMuted = WinoNeutral.Neutral400,
    textDisabled = WinoNeutral.Neutral300,
    textOnBrand = WinoNeutral.Neutral0,
    textOnCritical = WinoNeutral.Neutral0,
    textInverse = WinoNeutral.Neutral0,

    // Background
    bgCanvas = WinoNeutral.Neutral50,
    bgSurface = WinoNeutral.Neutral0,
    bgSurfaceAlt = WinoNeutral.Neutral100,
    bgElevated = WinoNeutral.Neutral0,
    bgAccentSoft = WinoBrand.Brand100,
    bgDestructiveSoft = WinoError.Error100,

    // Border
    borderSubtle = WinoNeutral.Neutral100,
    borderDefault = WinoNeutral.Neutral200,
    borderStrong = WinoNeutral.Neutral300,
    borderBrand = WinoBrand.Brand500,
    borderCritical = WinoError.Error400,

    // Brand
    brandPrimary = WinoBrand.Brand500,
    brandSoft = WinoBrand.Brand100,
    brandStrong = WinoBrand.Brand600,
    brandText = WinoNeutral.Neutral0,

    // State
    stateSuccess = WinoSuccess.Success500,
    stateSuccessSoft = WinoSuccess.Success100,
    stateWarning = WinoWarning.Warning500,
    stateWarningSoft = WinoWarning.Warning100,
    stateError = WinoError.Error500,
    stateErrorSoft = WinoError.Error100,
    stateInfo = WinoInfo.Info500,
    stateInfoSoft = WinoInfo.Info100,

    // Financial
    financialPositive = WinoSuccess.Success500,
    financialNegative = WinoError.Error500,
    financialNeutral = WinoNeutral.Neutral700,
    financialPending = WinoWarning.Warning500,
    financialFee = WinoNeutral.Neutral500
)
