package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.PresetsScreen
import com.example.ui.screens.RefinerScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentPrimary
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.PromptRefinerTheme
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextTertiary
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.PromptViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: PromptViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PromptRefinerTheme {
                MainAppContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: PromptViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    // Handle back button: return to Refiner tab if on a secondary tab
    if (uiState.currentTab != AppTab.REFINER) {
        BackHandler {
            viewModel.setTab(AppTab.REFINER)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg),
        bottomBar = {
            NavigationBar(
                containerColor = DarkSurface,
                modifier = Modifier
                    .border(
                        width = 1.dp,
                        color = DarkBorder,
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                    )
                    .testTag("bottom_nav_bar")
            ) {
                NavigationBarItem(
                    selected = uiState.currentTab == AppTab.REFINER,
                    onClick = { viewModel.setTab(AppTab.REFINER) },
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "Refiner") },
                    label = { Text("Refiner", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AccentCyan,
                        selectedTextColor = AccentCyan,
                        indicatorColor = Color(0xFF1E283E),
                        unselectedIconColor = TextTertiary,
                        unselectedTextColor = TextTertiary
                    ),
                    modifier = Modifier.testTag("nav_refiner")
                )

                NavigationBarItem(
                    selected = uiState.currentTab == AppTab.PRESETS,
                    onClick = { viewModel.setTab(AppTab.PRESETS) },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Presets") },
                    label = { Text("Presets", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AccentPrimary,
                        selectedTextColor = AccentPrimary,
                        indicatorColor = Color(0xFF1E283E),
                        unselectedIconColor = TextTertiary,
                        unselectedTextColor = TextTertiary
                    ),
                    modifier = Modifier.testTag("nav_presets")
                )

                NavigationBarItem(
                    selected = uiState.currentTab == AppTab.HISTORY,
                    onClick = { viewModel.setTab(AppTab.HISTORY) },
                    icon = { Icon(Icons.Default.History, contentDescription = "History") },
                    label = { Text("History", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AccentCyan,
                        selectedTextColor = AccentCyan,
                        indicatorColor = Color(0xFF1E283E),
                        unselectedIconColor = TextTertiary,
                        unselectedTextColor = TextTertiary
                    ),
                    modifier = Modifier.testTag("nav_history")
                )

                NavigationBarItem(
                    selected = uiState.currentTab == AppTab.SETTINGS,
                    onClick = { viewModel.setTab(AppTab.SETTINGS) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AccentPrimary,
                        selectedTextColor = AccentPrimary,
                        indicatorColor = Color(0xFF1E283E),
                        unselectedIconColor = TextTertiary,
                        unselectedTextColor = TextTertiary
                    ),
                    modifier = Modifier.testTag("nav_settings")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState.currentTab) {
                AppTab.REFINER -> RefinerScreen(viewModel = viewModel, uiState = uiState)
                AppTab.PRESETS -> PresetsScreen(viewModel = viewModel)
                AppTab.HISTORY -> HistoryScreen(viewModel = viewModel)
                AppTab.SETTINGS -> SettingsScreen(viewModel = viewModel, uiState = uiState)
            }
        }
    }
}
