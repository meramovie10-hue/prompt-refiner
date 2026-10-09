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
import androidx.compose.material.icons.filled.Groups
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.CharactersHost
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
import com.example.ui.viewmodel.CharacterViewModel
import com.example.ui.viewmodel.PromptViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: PromptViewModel by viewModels()
    private val charVm: CharacterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PromptRefinerTheme {
                MainAppContent(viewModel = viewModel, charVm = charVm)
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: PromptViewModel, charVm: CharacterViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var charactersOpen by rememberSaveable { mutableStateOf(true) }

    // Back: secondary tab se Refiner par wapas
    if (!charactersOpen && uiState.currentTab != AppTab.REFINER) {
        BackHandler {
            viewModel.setTab(AppTab.REFINER)
        }
    }

    val navColors = NavigationBarItemDefaults.colors(
        selectedIconColor = AccentCyan,
        selectedTextColor = AccentCyan,
        indicatorColor = Color(0xFF1E283E),
        unselectedIconColor = TextTertiary,
        unselectedTextColor = TextTertiary
    )
    val navColorsAlt = NavigationBarItemDefaults.colors(
        selectedIconColor = AccentPrimary,
        selectedTextColor = AccentPrimary,
        indicatorColor = Color(0xFF1E283E),
        unselectedIconColor = TextTertiary,
        unselectedTextColor = TextTertiary
    )

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
                    selected = charactersOpen,
                    onClick = { charactersOpen = true },
                    icon = { Icon(Icons.Default.Groups, contentDescription = "Characters") },
                    label = { Text("Characters", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = navColors,
                    modifier = Modifier.testTag("nav_characters")
                )

                NavigationBarItem(
                    selected = !charactersOpen && uiState.currentTab == AppTab.REFINER,
                    onClick = { charactersOpen = false; viewModel.setTab(AppTab.REFINER) },
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "Refiner") },
                    label = { Text("Refiner", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = navColors,
                    modifier = Modifier.testTag("nav_refiner")
                )

                NavigationBarItem(
                    selected = !charactersOpen && uiState.currentTab == AppTab.PRESETS,
                    onClick = { charactersOpen = false; viewModel.setTab(AppTab.PRESETS) },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Presets") },
                    label = { Text("Presets", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = navColorsAlt,
                    modifier = Modifier.testTag("nav_presets")
                )

                NavigationBarItem(
                    selected = !charactersOpen && uiState.currentTab == AppTab.HISTORY,
                    onClick = { charactersOpen = false; viewModel.setTab(AppTab.HISTORY) },
                    icon = { Icon(Icons.Default.History, contentDescription = "History") },
                    label = { Text("History", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = navColors,
                    modifier = Modifier.testTag("nav_history")
                )

                NavigationBarItem(
                    selected = !charactersOpen && uiState.currentTab == AppTab.SETTINGS,
                    onClick = { charactersOpen = false; viewModel.setTab(AppTab.SETTINGS) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = navColorsAlt,
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
            if (charactersOpen) {
                CharactersHost(vm = charVm)
            } else {
                when (uiState.currentTab) {
                    AppTab.REFINER -> RefinerScreen(viewModel = viewModel, uiState = uiState)
                    AppTab.PRESETS -> PresetsScreen(viewModel = viewModel)
                    AppTab.HISTORY -> HistoryScreen(viewModel = viewModel)
                    AppTab.SETTINGS -> SettingsScreen(viewModel = viewModel, uiState = uiState)
                }
            }
        }
    }
}
