package com.auravoice.tts.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

sealed class AuraScreen(val route: String, val title: String, val icon: ImageVector) {
    data object Splash : AuraScreen("splash", "Splash", Icons.Default.GraphicEq)
    data object Generate : AuraScreen("generate", "Generate", Icons.Default.GraphicEq)
    data object Voices : AuraScreen("voices", "Voices", Icons.Default.RecordVoiceOver)
    data object Profile : AuraScreen("profile", "Profile", Icons.Default.Person)
    data object Settings : AuraScreen("settings", "Settings", Icons.Default.Settings)
    data object Subscription : AuraScreen("subscription", "Subscription", Icons.Default.WorkspacePremium)
    data object History : AuraScreen("history", "History", Icons.Default.History)
    data object Help : AuraScreen("help", "Help & Support", Icons.Default.HelpOutline)
    data object Legal : AuraScreen("legal", "Privacy & Terms", Icons.Default.Info)
    data object Login : AuraScreen("login", "Sign In", Icons.Default.Lock)
    data object SignUp : AuraScreen("signup", "Sign Up", Icons.Default.Person)
    data object ForgotPassword : AuraScreen("forgot_password", "Forgot Password", Icons.Default.Lock)
}

@Composable
fun AuraBottomNavigation(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        AuraScreen.Generate,
        AuraScreen.Voices,
        AuraScreen.Profile
    )

    NavigationBar(
        modifier = modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = MaterialTheme.colorScheme.outline)
            .testTag("aura_bottom_navigation"),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        items.forEach { screen ->
            val isSelected = currentRoute == screen.route
            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigate(screen.route) },
                icon = {
                    Icon(
                        imageVector = screen.icon,
                        contentDescription = screen.title,
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = { Text(screen.title) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.secondary,
                    selectedTextColor = MaterialTheme.colorScheme.secondary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.tertiary,
                    unselectedTextColor = MaterialTheme.colorScheme.tertiary
                ),
                modifier = Modifier.testTag("nav_item_${screen.route}")
            )
        }
    }
}
