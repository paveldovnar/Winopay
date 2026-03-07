package com.winopay.util

import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * Manages app locale settings using AppCompatDelegate.
 *
 * Supports:
 * - Per-app language preferences (Android 13+)
 * - Backwards compatibility via AppCompatDelegate
 * - System default option
 */
object LocaleManager {

    private const val TAG = "LocaleManager"

    // Language codes
    const val LANG_ENGLISH = "en"
    const val LANG_RUSSIAN = "ru"
    const val LANG_SPANISH = "es"
    const val LANG_THAI = "th"
    const val LANG_ARABIC = "ar"
    const val LANG_CHINESE = "zh"
    const val LANG_SYSTEM = "system"

    /**
     * Available languages for the app.
     * Note: Some languages require string translations to be added in values-XX/strings.xml
     */
    data class LanguageOption(
        val code: String,
        val displayName: String,
        val nativeName: String
    )

    val availableLanguages = listOf(
        LanguageOption(LANG_SYSTEM, "System default", "System default"),
        LanguageOption(LANG_ENGLISH, "English", "English"),
        LanguageOption(LANG_RUSSIAN, "Russian", "Русский"),
        LanguageOption(LANG_SPANISH, "Spanish", "Español"),
        LanguageOption(LANG_THAI, "Thai", "ไทย"),
        LanguageOption(LANG_ARABIC, "Arabic", "العربية"),
        LanguageOption(LANG_CHINESE, "Chinese", "中文")
    )

    /**
     * Apply language setting.
     *
     * @param languageCode Language code (en, ru, system)
     */
    fun setAppLanguage(languageCode: String) {
        Log.d(TAG, "Setting app language: $languageCode")

        val localeList = when (languageCode) {
            LANG_SYSTEM -> LocaleListCompat.getEmptyLocaleList()
            else -> LocaleListCompat.forLanguageTags(languageCode)
        }

        AppCompatDelegate.setApplicationLocales(localeList)
        Log.d(TAG, "App language set to: $languageCode")
    }

    /**
     * Get current language code.
     *
     * @return Current language code or "system" if using system default
     */
    fun getCurrentLanguage(): String {
        val locales = AppCompatDelegate.getApplicationLocales()
        return if (locales.isEmpty) {
            LANG_SYSTEM
        } else {
            locales.get(0)?.language ?: LANG_SYSTEM
        }
    }

    /**
     * Convert legacy language name to code.
     * For backwards compatibility with existing DataStore values.
     */
    fun legacyNameToCode(name: String): String {
        return when (name.lowercase()) {
            "english" -> LANG_ENGLISH
            "russian", "русский" -> LANG_RUSSIAN
            "spanish", "español" -> LANG_SPANISH
            "thai", "ไทย" -> LANG_THAI
            "arabic", "العربية" -> LANG_ARABIC
            "chinese", "中文" -> LANG_CHINESE
            "system", "system default" -> LANG_SYSTEM
            else -> LANG_ENGLISH
        }
    }

    /**
     * Convert code to display name.
     */
    fun codeToDisplayName(code: String): String {
        return availableLanguages.find { it.code == code }?.displayName ?: "English"
    }
}
