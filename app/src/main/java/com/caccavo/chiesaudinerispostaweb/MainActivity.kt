package com.caccavo.chiesaudinerispostaweb

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.caccavo.chiesaudinerispostaweb.notifications.DailyVerseNotificationScheduler
import com.caccavo.chiesaudinerispostaweb.ui.audio.AudioSettingsScreen
import com.caccavo.chiesaudinerispostaweb.ui.bible.BibleScreen
import com.caccavo.chiesaudinerispostaweb.ui.dailyverse.DailyVerseScreen
import com.caccavo.chiesaudinerispostaweb.ui.guide.UserGuideScreen
import com.caccavo.chiesaudinerispostaweb.ui.home.HomeScreen
import com.caccavo.chiesaudinerispostaweb.ui.theme.ChiesaTheme

private const val ROUTE_HOME = "home"
private const val ROUTE_BIBLE = "bible"
private const val ROUTE_DAILY_VERSE = "dailyVerse"
private const val ROUTE_AUDIO_SETTINGS = "audioSettings"
private const val ROUTE_USER_GUIDE = "userGuide"

class MainActivity : ComponentActivity() {

    /** Come su iOS: all'avvio si chiede il permesso per le notifiche (Android 13+) e, se
     * concesso, si programma il promemoria mattutino del versetto del giorno alle 8:00. */
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                DailyVerseNotificationScheduler.scheduleMorningReminder(this)
            }
        }

    private fun setUpDailyVerseReminder() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            DailyVerseNotificationScheduler.scheduleMorningReminder(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setUpDailyVerseReminder()
        setContent {
            ChiesaTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    // Su tablet/schermi larghi il contenuto resta leggibile in una colonna
                    // centrata invece di stirarsi da un bordo all'altro dello schermo.
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                        Box(modifier = Modifier.widthIn(max = 600.dp).fillMaxSize()) {
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
                                    DailyVerseScreen(onClose = { navController.popBackStack() })
                                }
                                composable(ROUTE_AUDIO_SETTINGS) {
                                    AudioSettingsScreen(onClose = { navController.popBackStack() })
                                }
                                composable(ROUTE_USER_GUIDE) {
                                    UserGuideScreen(onClose = { navController.popBackStack() })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
