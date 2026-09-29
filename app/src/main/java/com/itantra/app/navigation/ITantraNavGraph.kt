package com.itantra.app.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.itantra.app.ui.communication.CommunicationScreen
import com.itantra.app.ui.home.HomeScreen
import com.itantra.app.ui.language.LanguageScreen
import com.itantra.app.ui.scan.ScanNearbyScreen
import com.itantra.app.ui.settings.AboutScreen
import com.itantra.app.ui.settings.SettingsScreen
import com.itantra.app.ui.splash.SplashScreen

/**
 * iTantra Navigation Graph.
 *
 * Flow:
 *   Splash → Home → { ScanNearby, Language, Settings, Communication }
 *   Settings → About
 *   ScanNearby → (device selected) → Home
 *   Home → (connected) → Communication
 */
@Composable
fun ITantraNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        enterTransition = {
            fadeIn(animationSpec = tween(300)) + slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(300)
            )
        },
        exitTransition = {
            fadeOut(animationSpec = tween(300)) + slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(300)
            )
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(300)) + slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(300)
            )
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(300)) + slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(300)
            )
        }
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onSplashComplete = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToScan = {
                    navController.navigate(Screen.ScanNearby.route)
                },
                onNavigateToLanguage = {
                    navController.navigate(Screen.Language.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToCommunication = {
                    navController.navigate(Screen.Communication.route)
                }
            )
        }

        composable(Screen.ScanNearby.route) {
            ScanNearbyScreen(
                onDeviceConnected = {
                    navController.popBackStack()
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Language.route) {
            LanguageScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateToAbout = {
                    navController.navigate(Screen.About.route)
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.About.route) {
            AboutScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Communication.route) {
            CommunicationScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
