package com.auravoice.tts.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.auravoice.tts.data.model.VoiceModel
import com.auravoice.tts.ui.components.AuraScreen
import com.auravoice.tts.ui.screens.auth.AuthViewModel
import com.auravoice.tts.ui.screens.auth.ForgotPasswordScreen
import com.auravoice.tts.ui.screens.auth.LoginScreen
import com.auravoice.tts.ui.screens.auth.SignUpScreen
import com.auravoice.tts.ui.screens.help.HelpSupportScreen
import com.auravoice.tts.ui.screens.history.HistoryScreen
import com.auravoice.tts.ui.screens.home.HomeScreen
import com.auravoice.tts.ui.screens.home.HomeViewModel
import com.auravoice.tts.ui.screens.info.InfoScreen
import com.auravoice.tts.ui.screens.legal.LegalInfoScreen
import com.auravoice.tts.ui.screens.profile.ProfileScreen
import com.auravoice.tts.ui.screens.profile.ProfileViewModel
import com.auravoice.tts.ui.screens.settings.SettingsScreen
import com.auravoice.tts.ui.screens.settings.SettingsViewModel
import com.auravoice.tts.ui.screens.splash.SplashScreen
import com.auravoice.tts.ui.screens.subscription.SubscriptionScreen
import com.auravoice.tts.ui.screens.voices.VoiceCatalogScreen
import com.auravoice.tts.ui.screens.voices.VoiceCatalogViewModel

@Composable
fun AuraVoiceNavGraph(
    navController: NavHostController,
    homeViewModel: HomeViewModel,
    voiceCatalogViewModel: VoiceCatalogViewModel,
    profileViewModel: ProfileViewModel,
    settingsViewModel: SettingsViewModel,
    authViewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = AuraScreen.Splash.route,
        modifier = modifier
    ) {
        composable(AuraScreen.Splash.route) {
            val userProfile by authViewModel.userProfile.collectAsStateWithLifecycle()
            val isLoggedIn = authViewModel.isUserAuthenticatedAndVerified() &&
                    !userProfile.userId.startsWith("guest_")

            SplashScreen(
                isLoggedIn = isLoggedIn,
                onNavigateToHome = {
                    navController.navigate(AuraScreen.Generate.route) {
                        popUpTo(AuraScreen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(AuraScreen.Login.route) {
                        popUpTo(AuraScreen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(AuraScreen.Generate.route) {
            val isLoggedIn = authViewModel.isUserAuthenticatedAndVerified()
            if (!isLoggedIn) {
                androidx.compose.runtime.LaunchedEffect(Unit) {
                    navController.navigate(AuraScreen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            } else {
                HomeScreen(
                    viewModel = homeViewModel,
                    onNavigateToVoices = { navController.navigate(AuraScreen.Voices.route) },
                    onNavigateToProfile = { navController.navigate(AuraScreen.Profile.route) },
                    onNavigateToHistory = { navController.navigate(AuraScreen.History.route) },
                    onNavigateToSubscription = { navController.navigate(AuraScreen.Subscription.route) }
                )
            }
        }

        composable(AuraScreen.Voices.route) {
            val isLoggedIn = authViewModel.isUserAuthenticatedAndVerified()
            if (!isLoggedIn) {
                androidx.compose.runtime.LaunchedEffect(Unit) {
                    navController.navigate(AuraScreen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            } else {
                VoiceCatalogScreen(
                    viewModel = voiceCatalogViewModel,
                    onSelectVoiceAndNavigate = { selectedVoice ->
                        homeViewModel.onSelectVoice(selectedVoice)
                        navController.navigate(AuraScreen.Generate.route) {
                            popUpTo(AuraScreen.Generate.route) {
                                inclusive = true
                            }
                        }
                    },
                    onNavigateToSubscription = { navController.navigate(AuraScreen.Subscription.route) }
                )
            }
        }

        composable(AuraScreen.Profile.route) {
            val isLoggedIn = authViewModel.isUserAuthenticatedAndVerified()
            if (!isLoggedIn) {
                androidx.compose.runtime.LaunchedEffect(Unit) {
                    navController.navigate(AuraScreen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            } else {
                ProfileScreen(
                    viewModel = profileViewModel,
                    onNavigateToHistory = { navController.navigate(AuraScreen.History.route) },
                    onNavigateToSubscription = { navController.navigate(AuraScreen.Subscription.route) },
                    onNavigateToSettings = { navController.navigate(AuraScreen.Settings.route) },
                    onNavigateToHelp = { navController.navigate(AuraScreen.Help.route) },
                    onNavigateToLegal = { navController.navigate(AuraScreen.Legal.route) },
                    onNavigateToLogin = {
                        navController.navigate(AuraScreen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }

        composable(AuraScreen.Login.route) {
            LoginScreen(
                viewModel = authViewModel,
                onNavigateToSignUp = { navController.navigate(AuraScreen.SignUp.route) },
                onNavigateToForgotPassword = { navController.navigate(AuraScreen.ForgotPassword.route) },
                onLoginSuccess = {
                    navController.navigate(AuraScreen.Generate.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onBack = {
                    if (authViewModel.isUserAuthenticatedAndVerified()) {
                        navController.popBackStack()
                    }
                }
            )
        }

        composable(AuraScreen.SignUp.route) {
            SignUpScreen(
                viewModel = authViewModel,
                onNavigateToLogin = {
                    navController.navigate(AuraScreen.Login.route) {
                        popUpTo(AuraScreen.SignUp.route) { inclusive = true }
                    }
                },
                onSignUpSuccess = {
                    navController.navigate(AuraScreen.Generate.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onBack = {
                    navController.navigate(AuraScreen.Login.route) {
                        popUpTo(AuraScreen.SignUp.route) { inclusive = true }
                    }
                }
            )
        }

        composable(AuraScreen.ForgotPassword.route) {
            ForgotPasswordScreen(
                viewModel = authViewModel,
                onNavigateToLogin = { navController.navigate(AuraScreen.Login.route) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(AuraScreen.Settings.route) {
            SettingsScreen(
                viewModel = settingsViewModel,
                onBack = { navController.popBackStack() },
                onNavigateToLegal = { navController.navigate(AuraScreen.Legal.route) }
            )
        }

        composable(AuraScreen.Subscription.route) {
            SubscriptionScreen(
                viewModel = profileViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(AuraScreen.History.route) {
            HistoryScreen(
                viewModel = profileViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(AuraScreen.Help.route) {
            HelpSupportScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(AuraScreen.Legal.route) {
            InfoScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
