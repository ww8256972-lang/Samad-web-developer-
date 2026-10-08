package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import com.example.ui.components.NeuralUltraBackground
import com.example.ui.screens.ArchivedListScreen
import com.example.ui.screens.AskAiScreen
import com.example.ui.screens.CustomerDetailScreen
import com.example.ui.screens.CustomerEditScreen
import com.example.ui.screens.CustomerListScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonPink
import com.example.ui.theme.WebRecordTheme
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.Screen

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val appSettings by viewModel.appSettings.collectAsState()
            val isDark = when (appSettings?.themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemInDarkTheme()
            }

            WebRecordTheme(darkTheme = isDark) {
                MainAppRoot(viewModel = viewModel, isDark = isDark)
            }
        }
    }
}

@Composable
fun MainAppRoot(viewModel: MainViewModel, isDark: Boolean) {
    val loginState by viewModel.loginState.collectAsState()
    val currentScreen by viewModel.currentScreen.collectAsState()
    val userMessage by viewModel.userMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(userMessage) {
        userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUserMessage()
        }
    }

    if (!loginState.isAuthenticated) {
        LoginScreen(viewModel = viewModel)
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                val showBottomNav = currentScreen is Screen.Dashboard ||
                    currentScreen is Screen.CustomerList ||
                    currentScreen is Screen.AskAi ||
                    currentScreen is Screen.Settings

                AnimatedVisibility(visible = showBottomNav) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("main_bottom_nav")
                    ) {
                        NavigationBarItem(
                            selected = currentScreen is Screen.Dashboard,
                            onClick = { viewModel.navigateTo(Screen.Dashboard) },
                            icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                            label = { Text("Dashboard") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = ElectricBlue,
                                selectedTextColor = ElectricBlue,
                                indicatorColor = ElectricBlue.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.testTag("nav_item_dashboard")
                        )

                        NavigationBarItem(
                            selected = currentScreen is Screen.CustomerList,
                            onClick = { viewModel.navigateTo(Screen.CustomerList) },
                            icon = { Icon(Icons.Default.People, contentDescription = "Customers") },
                            label = { Text("Customers") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = ElectricBlue,
                                selectedTextColor = ElectricBlue,
                                indicatorColor = ElectricBlue.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.testTag("nav_item_customers")
                        )

                        NavigationBarItem(
                            selected = currentScreen is Screen.AskAi,
                            onClick = { viewModel.navigateTo(Screen.AskAi) },
                            icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "Ask AI") },
                            label = { Text("Ask AI") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = NeonPink,
                                selectedTextColor = NeonPink,
                                indicatorColor = NeonPink.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.testTag("nav_item_ask_ai")
                        )

                        NavigationBarItem(
                            selected = currentScreen is Screen.Settings,
                            onClick = { viewModel.navigateTo(Screen.Settings) },
                            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                            label = { Text("Settings") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = ElectricBlue,
                                selectedTextColor = ElectricBlue,
                                indicatorColor = ElectricBlue.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.testTag("nav_item_settings")
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Subtle moving Neural Ultra Background behind the main views
                NeuralUltraBackground(isDark = isDark)

                when (val screen = currentScreen) {
                    is Screen.Dashboard -> DashboardScreen(viewModel = viewModel)
                    is Screen.CustomerList -> CustomerListScreen(viewModel = viewModel)
                    is Screen.CustomerDetail -> CustomerDetailScreen(customerId = screen.customerId, viewModel = viewModel)
                    is Screen.CustomerEdit -> CustomerEditScreen(customerId = screen.customerId, viewModel = viewModel)
                    is Screen.AskAi -> AskAiScreen(viewModel = viewModel)
                    is Screen.Settings -> SettingsScreen(viewModel = viewModel)
                    is Screen.ArchivedList -> ArchivedListScreen(viewModel = viewModel)
                }
            }
        }
    }
}
