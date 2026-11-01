package com.gun.personalcolor.feature.settings

import androidx.annotation.Keep
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable

// Enum route args are looked up by class name at runtime; keep it from R8.
@Keep
enum class LegalDoc { PRIVACY, TERMS }

@Serializable data object SettingsRoute
@Serializable data object LicensesRoute
@Serializable data class LegalRoute(val doc: LegalDoc)

/** Settings + open-source licenses + legal documents (privacy policy, terms). */
fun NavGraphBuilder.settingsScreens(onBack: () -> Unit, onLicenses: () -> Unit, onLegal: (LegalDoc) -> Unit, onOnboarding: () -> Unit) {
    composable<SettingsRoute> { SettingsRoute(onBack, onLicenses, onLegal, onOnboarding) }
    composable<LicensesRoute> { LicensesScreen(onBack) }
    composable<LegalRoute> { LegalScreen(it.toRoute<LegalRoute>().doc, onBack) }
}
