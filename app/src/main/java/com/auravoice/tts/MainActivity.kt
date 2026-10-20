package com.auravoice.tts

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.auravoice.tts.data.local.AppDatabase
import com.auravoice.tts.data.remote.FirebaseManager
import com.auravoice.tts.data.remote.HuggingFaceApiService
import com.auravoice.tts.data.repository.UserRepository
import com.auravoice.tts.data.repository.VoiceRepository
import com.auravoice.tts.engine.AudioPlayerManager
import com.auravoice.tts.ui.components.AuraBottomNavigation
import com.auravoice.tts.ui.components.AuraScreen
import com.auravoice.tts.ui.navigation.AuraVoiceNavGraph
import com.auravoice.tts.ui.screens.auth.AuthViewModel
import com.auravoice.tts.ui.screens.home.HomeViewModel
import com.auravoice.tts.ui.screens.profile.ProfileViewModel
import com.auravoice.tts.ui.screens.settings.SettingsViewModel
import com.auravoice.tts.ui.screens.voices.VoiceCatalogViewModel
import com.auravoice.tts.ui.theme.AuraVoiceTheme
import com.auravoice.tts.ui.theme.ThemeManager

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.auravoice.tts.ui.components.OfflineBlockScreen
import com.auravoice.tts.util.NetworkObserver

class MainActivity : ComponentActivity() {

    private lateinit var database: AppDatabase
    private lateinit var hfService: HuggingFaceApiService
    private lateinit var firebaseManager: FirebaseManager
    private lateinit var voiceRepository: VoiceRepository
    private lateinit var userRepository: UserRepository
    private lateinit var audioPlayerManager: AudioPlayerManager
    private lateinit var networkObserver: NetworkObserver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Theme Preference
        ThemeManager.init(this)

        networkObserver = NetworkObserver(this)

        // Initialize Core Architectural Dependencies
        database = AppDatabase.getInstance(this)
        hfService = HuggingFaceApiService()
        firebaseManager = FirebaseManager()

        voiceRepository = VoiceRepository(
            voiceModelDao = database.voiceModelDao(),
            firebaseManager = firebaseManager,
            hfService = hfService
        )

        userRepository = UserRepository(
            firebaseManager = firebaseManager,
            historyDao = database.generationHistoryDao()
        )

        audioPlayerManager = AudioPlayerManager(this)

        setContent {
            val themeMode by ThemeManager.themeMode.collectAsStateWithLifecycle()

            val isOnlineFlowState by remember {
                networkObserver.observe()
            }.collectAsState(initial = networkObserver.isCurrentlyOnline())

            var manualCheckState by remember { mutableStateOf<Boolean?>(null) }
            val isAppOnline = manualCheckState ?: isOnlineFlowState

            AuraVoiceTheme(themeMode = themeMode) {
                if (!isAppOnline) {
                    OfflineBlockScreen(
                        onRetry = {
                            val currentStatus = networkObserver.isCurrentlyOnline()
                            manualCheckState = if (currentStatus) true else null
                        }
                    )
                } else {
                    val navController = rememberNavController()
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route ?: AuraScreen.Splash.route

                    val homeViewModel: HomeViewModel = viewModel(
                        factory = HomeViewModel.Factory(this, voiceRepository, userRepository, audioPlayerManager)
                    )

                    val voiceCatalogViewModel: VoiceCatalogViewModel = viewModel(
                        factory = VoiceCatalogViewModel.Factory(voiceRepository, userRepository, audioPlayerManager)
                    )

                    val profileViewModel: ProfileViewModel = viewModel(
                        factory = ProfileViewModel.Factory(userRepository, audioPlayerManager)
                    )

                    val settingsViewModel: SettingsViewModel = viewModel(
                        factory = SettingsViewModel.Factory(voiceRepository)
                    )

                    val authViewModel: AuthViewModel = viewModel(
                        factory = AuthViewModel.Factory(userRepository)
                    )

                    val isFullScreenRoute = currentRoute == AuraScreen.Splash.route ||
                            currentRoute == AuraScreen.Login.route ||
                            currentRoute == AuraScreen.SignUp.route ||
                            currentRoute == AuraScreen.ForgotPassword.route

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        bottomBar = {
                            if (!isFullScreenRoute) {
                                AuraBottomNavigation(
                                    currentRoute = currentRoute,
                                    onNavigate = { route ->
                                        if (currentRoute != route) {
                                            navController.navigate(route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    ) { innerPadding ->
                        AuraVoiceNavGraph(
                            navController = navController,
                            homeViewModel = homeViewModel,
                            voiceCatalogViewModel = voiceCatalogViewModel,
                            profileViewModel = profileViewModel,
                            settingsViewModel = settingsViewModel,
                            authViewModel = authViewModel,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioPlayerManager.stopAudio()
    }
}
