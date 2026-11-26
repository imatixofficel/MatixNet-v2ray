package com.v2ray.ang.handler

import android.content.Context
import android.content.res.Configuration
import com.v2ray.ang.AppConfig
import com.v2ray.ang.enums.Language
import com.v2ray.ang.ui.compose.ThemeManager

/**
 * Matix net: the two quick switches of the top bars, day/night theme and Persian/English.
 * They write the same settings the Settings screen writes, so both always agree.
 */
object MatixAppSettings {

    /** Flips between the day and the night theme; the system setting is read once to know which one is showing. */
    fun toggleTheme(context: Context) {
        val dark = when (ThemeManager.themeMode.value) {
            "1" -> false
            "2" -> true
            else -> (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
        }
        ThemeManager.setThemeMode(if (dark) "1" else "2")
    }

    /** Flips between Persian and English; AppCompat recreates the screen in the new language. */
    fun toggleLanguage() {
        val current = Language.fromCode(
            MmkvManager.decodeSettingsString(AppConfig.PREF_LANGUAGE) ?: Language.PERSIAN.code
        )
        val next = if (current == Language.PERSIAN) Language.ENGLISH else Language.PERSIAN
        AppLocaleManager.setApplicationLanguage(next.code)
    }

    /** True once the welcome screen was shown and left, with a subscription or without one. */
    fun welcomeDone(): Boolean = MmkvManager.decodeSettingsBool(AppConfig.PREF_MATIX_WELCOME_DONE, false)

    fun markWelcomeDone() {
        MmkvManager.encodeSettings(AppConfig.PREF_MATIX_WELCOME_DONE, true)
    }
}
