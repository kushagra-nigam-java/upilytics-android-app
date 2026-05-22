package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.BiometricGateScreen
import com.example.ui.components.SmartPopupSheet
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.InsightsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.FintechViewModel
import com.example.ui.utils.L10n

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainContainer()
            }
        }
    }
}

@Composable
fun MainContainer() {
    val viewModel: FintechViewModel = viewModel()
    
    val preferences by viewModel.preferences.collectAsState()
    val activePopupTx by viewModel.popupTransaction.collectAsState()

    val biometricEnabled = preferences["biometric_lock"]?.toBoolean() ?: false
    val langCode = preferences["language_code"] ?: "en"

    var isUnlocked by remember { mutableStateOf(false) }

    // Navigation and current tab tracker
    var currentTab by remember { mutableStateOf(0) }

    // Apply security biometric gate if enabled
    if (biometricEnabled && !isUnlocked) {
        BiometricGateScreen(
            langCode = langCode,
            onSuccess = { isUnlocked = true }
        )
    } else {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    modifier = Modifier
                        .height(72.dp)
                        .testTag("app_bottom_nav_bar")
                ) {
                    val tabs = listOf(
                        Triple(L10n.get("dashboard", langCode), Icons.Default.CreditCard, 0),
                        Triple(L10n.get("analytics", langCode), Icons.Default.Analytics, 1),
                        Triple(L10n.get("insights", langCode), Icons.Default.AutoAwesome, 2),
                        Triple(L10n.get("settings", langCode), Icons.Default.Settings, 3)
                    )

                    tabs.forEach { tab ->
                        val selected = currentTab == tab.third
                        NavigationBarItem(
                            selected = selected,
                            onClick = { currentTab = tab.third },
                            icon = {
                                Icon(
                                    imageVector = tab.second,
                                    contentDescription = tab.first,
                                    tint = if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.4f)
                                )
                            },
                            label = {
                                Text(
                                    text = tab.first,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.4f)
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
                            ),
                            modifier = Modifier.testTag("nav_item_${tab.third}")
                        )
                    }
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Display proper tab screen
                when (currentTab) {
                    0 -> DashboardScreen(viewModel = viewModel, langCode = langCode)
                    1 -> AnalyticsScreen(viewModel = viewModel, langCode = langCode)
                    2 -> InsightsScreen(viewModel = viewModel, langCode = langCode)
                    3 -> SettingsScreen(viewModel = viewModel, langCode = langCode)
                }

                // High-fidelity Floating Verified Popup Notification
                AnimatedVisibility(
                    visible = activePopupTx != null,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = spring(dampingRatio = 0.82f, stiffness = 400f)
                    ) + fadeIn(),
                    exit = slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = spring(dampingRatio = 0.85f, stiffness = 300f)
                    ) + fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp)
                ) {
                    if (activePopupTx != null) {
                        // Background blur effect under popup
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.35f))
                                .blur(8.dp)
                        )
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            SmartPopupSheet(
                                transaction = activePopupTx!!,
                                onDismiss = { viewModel.dismissPopup() },
                                onConfirmed = { correctedCat, note ->
                                    viewModel.correctionLearned(
                                        activePopupTx!!.id,
                                        activePopupTx!!.merchant,
                                        correctedCat
                                    )
                                    viewModel.dismissPopup()
                                },
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
