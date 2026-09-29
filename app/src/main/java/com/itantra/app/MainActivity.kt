package com.itantra.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.itantra.app.navigation.ITantraNavGraph
import com.itantra.app.ui.theme.ITantraTheme
import com.itantra.app.ui.theme.ThemeMode

import com.itantra.app.ui.theme.ThemeManager

/**
 * Single-activity host for iTantra.
 * All UI is Jetpack Compose — no XML layouts for the main application.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager.init(this)
        enableEdgeToEdge()

        setContent {
            val themeMode by ThemeManager.themeMode.collectAsState()

            ITantraTheme(themeMode = themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    ITantraNavGraph(navController = navController)
                }
            }
        }
    }
}
