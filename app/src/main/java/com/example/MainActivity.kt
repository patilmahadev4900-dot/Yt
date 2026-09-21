package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.autoshorts.engine.PipelineOrchestrator
import com.example.autoshorts.ui.screens.HistoryScreen
import com.example.autoshorts.ui.screens.SettingsScreen
import com.example.autoshorts.ui.screens.StudioScreen
import com.example.ui.theme.HighlightYellow
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.StudioBlack
import com.example.ui.theme.StudioCardBorder
import com.example.ui.theme.StudioDarkSurface
import com.example.ui.theme.StudioTextSecondary
import com.example.ui.theme.YoutubeRed

enum class AppTab(val label: String, val icon: ImageVector) {
    STUDIO("Studio", Icons.Default.MovieCreation),
    HISTORY("History", Icons.Default.VideoLibrary),
    SETTINGS("Settings", Icons.Default.Tune)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                AutoShortsApp()
            }
        }
    }
}

@Composable
fun AutoShortsApp() {
    val coroutineScope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val orchestrator = remember { PipelineOrchestrator(context, coroutineScope) }

    var currentTab by remember { mutableStateOf(AppTab.STUDIO) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioBlack),
        contentWindowInsets = WindowInsets.navigationBars,
        bottomBar = {
            NavigationBar(
                containerColor = StudioDarkSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = StudioCardBorder,
                        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                    )
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .testTag("main_navigation_bar"),
                tonalElevation = 8.dp
            ) {
                AppTab.entries.forEach { tab ->
                    val isSelected = currentTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentTab = tab },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(
                                text = tab.label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = YoutubeRed,
                            selectedTextColor = HighlightYellow,
                            indicatorColor = YoutubeRed.copy(alpha = 0.15f),
                            unselectedIconColor = StudioTextSecondary,
                            unselectedTextColor = StudioTextSecondary
                        ),
                        modifier = Modifier.testTag("nav_item_${tab.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(StudioBlack)
        ) {
            when (currentTab) {
                AppTab.STUDIO -> StudioScreen(
                    orchestrator = orchestrator,
                    modifier = Modifier.fillMaxSize()
                )
                AppTab.HISTORY -> HistoryScreen(
                    orchestrator = orchestrator,
                    onNavigateToStudio = { currentTab = AppTab.STUDIO },
                    modifier = Modifier.fillMaxSize()
                )
                AppTab.SETTINGS -> SettingsScreen(
                    orchestrator = orchestrator,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
