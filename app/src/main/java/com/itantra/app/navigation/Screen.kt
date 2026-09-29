package com.itantra.app.navigation

/**
 * iTantra navigation routes — single source of truth for all screen destinations.
 */
sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Home : Screen("home")
    data object ScanNearby : Screen("scan_nearby")
    data object Language : Screen("language")
    data object Settings : Screen("settings")
    data object About : Screen("about")
    data object Communication : Screen("communication")
}
