package com.caccavo.chiesaudinerispostaweb

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.caccavo.chiesaudinerispostaweb.ui.bible.BibleScreen
import com.caccavo.chiesaudinerispostaweb.ui.common.ComingSoonScreen
import com.caccavo.chiesaudinerispostaweb.ui.home.HomeScreen

private const val ROUTE_HOME = "home"
private const val ROUTE_BIBLE = "bible"
private const val ROUTE_DAILY_VERSE = "dailyVerse"
private const val ROUTE_AUDIO_SETTINGS = "audioSettings"
private const val ROUTE_USER_GUIDE = "userGuide"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = ROUTE_HOME) {
                        composable(ROUTE_HOME) {
                            HomeScreen(
                                onOpenDailyVerse = { navController.navigate(ROUTE_DAILY_VERSE) },
                                onOpenBible = { navController.navigate(ROUTE_BIBLE) },
                                onOpenAudioSettings = { navController.navigate(ROUTE_AUDIO_SETTINGS) },
                                onOpenUserGuide = { navController.navigate(ROUTE_USER_GUIDE) }
                            )
                        }
                        composable(ROUTE_BIBLE) {
                            BibleScreen(onClose = { navController.popBackStack() })
                        }
                        composable(ROUTE_DAILY_VERSE) {
                            ComingSoonScreen("Versetto del giorno") { navController.popBackStack() }
                        }
                        composable(ROUTE_AUDIO_SETTINGS) {
                            ComingSoonScreen("Audio Bibbia") { navController.popBackStack() }
                        }
                        composable(ROUTE_USER_GUIDE) {
                            ComingSoonScreen("Guida all'uso dell'app") { navController.popBackStack() }
                        }
                    }
                }
            }
        }
    }
}
