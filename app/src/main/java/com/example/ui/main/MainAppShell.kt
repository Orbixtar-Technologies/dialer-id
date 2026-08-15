package com.example.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Dialpad
import coil.compose.AsyncImage
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Dialpad
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Payment
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.service.CallPhase
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.AuthViewModel
import com.example.ui.callerid.CallerIdScreen
import com.example.ui.callerid.CallerIdViewModel
import com.example.ui.deposit.DepositScreen
import com.example.ui.deposit.DepositViewModel
import com.example.ui.dialer.ActiveCallScreen
import com.example.ui.dialer.DialerScreen
import com.example.ui.dialer.DialerViewModel
import com.example.ui.history.CallHistoryScreen
import com.example.ui.history.CallHistoryViewModel
import com.example.ui.settings.SettingsScreen
import com.example.ui.settings.SettingsViewModel
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RoyalBlue50
import com.example.ui.theme.RoyalBlue600
import com.example.ui.theme.RoyalBlue700
import com.example.ui.theme.RoyalBlue800
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900

enum class AppTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    DIALER("Dialer", Icons.Filled.Dialpad, Icons.Outlined.Dialpad, "tab_dialer"),
    HISTORY("History", Icons.Filled.History, Icons.Outlined.History, "tab_history"),
    CALLER_IDS("Caller IDs", Icons.Filled.Badge, Icons.Outlined.Badge, "tab_caller_ids"),
    DEPOSIT("Deposit", Icons.Filled.Payment, Icons.Outlined.Payment, "tab_deposit"),
    SETTINGS("Settings", Icons.Filled.Settings, Icons.Outlined.Settings, "tab_settings")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppShell(
    authViewModel: AuthViewModel = viewModel(),
    dialerViewModel: DialerViewModel = viewModel(),
    callerIdViewModel: CallerIdViewModel = viewModel(),
    historyViewModel: CallHistoryViewModel = viewModel(),
    depositViewModel: DepositViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val authState by authViewModel.uiState.collectAsState()
    val activeCallState by dialerViewModel.activeCallState.collectAsState()
    val userProfile by dialerViewModel.userProfile.collectAsState()

    var currentTab by remember { mutableStateOf(AppTab.DIALER) }

    if (!authState.isAuthenticated) {
        AuthScreen(
            viewModel = authViewModel,
            onLoginSuccess = { currentTab = AppTab.DIALER }
        )
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = PureWhite,
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.statusBars)
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Brand Logo & Title with HD Badge
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(RoyalBlue600),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = PureWhite,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "DialerID",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = (-0.5).sp
                                ),
                                color = Slate900
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(RoyalBlue50)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "HD",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = RoyalBlue700,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        // Right side: Live Balance Pill
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Live Balance Pill with + Add button
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(RoyalBlue50)
                                    .border(1.dp, RoyalBlue600.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                                    .clickable { currentTab = AppTab.DEPOSIT }
                                    .padding(start = 10.dp, end = 4.dp, top = 3.dp, bottom = 3.dp)
                                    .testTag("topbar_balance_pill")
                            ) {
                                Text(
                                    text = "$${if (userProfile.creditBalance * 100 == (userProfile.creditBalance * 100).toLong().toDouble()) String.format("%.2f", userProfile.creditBalance) else String.format("%.4f", userProfile.creditBalance)}",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = RoyalBlue800
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(RoyalBlue600),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add Funds",
                                        tint = PureWhite,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            if (!userProfile.photoUrl.isNullOrEmpty()) {
                                Spacer(modifier = Modifier.width(8.dp))
                                AsyncImage(
                                    model = userProfile.photoUrl,
                                    contentDescription = "Avatar",
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .border(1.dp, Slate200, CircleShape)
                                        .clickable { currentTab = AppTab.SETTINGS }
                                )
                            }
                        }
                    }
                }
            },
            bottomBar = {
                NavigationBar(
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
                    containerColor = PureWhite,
                    tonalElevation = 6.dp
                ) {
                    AppTab.values().forEach { tab ->
                        val isSelected = currentTab == tab
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { currentTab = tab },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.title,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = tab.title,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = RoyalBlue600,
                                selectedTextColor = RoyalBlue600,
                                indicatorColor = RoyalBlue50,
                                unselectedIconColor = Slate400,
                                unselectedTextColor = Slate500
                            ),
                            modifier = Modifier.testTag(tab.testTag)
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(Slate50)
            ) {
                when (currentTab) {
                    AppTab.DIALER -> DialerScreen(
                        viewModel = dialerViewModel,
                        onNavigateToDeposit = { currentTab = AppTab.DEPOSIT }
                    )
                    AppTab.HISTORY -> CallHistoryScreen(
                        viewModel = historyViewModel,
                        onRedialNumber = { number ->
                            dialerViewModel.onNumberChanged(number)
                            currentTab = AppTab.DIALER
                        }
                    )
                    AppTab.CALLER_IDS -> CallerIdScreen(
                        viewModel = callerIdViewModel
                    )
                    AppTab.DEPOSIT -> DepositScreen(
                        viewModel = depositViewModel
                    )
                    AppTab.SETTINGS -> SettingsScreen(
                        viewModel = settingsViewModel,
                        onLogout = { authViewModel.logout() }
                    )
                }
            }
        }

        // Active Call Overlay Screen
        AnimatedVisibility(
            visible = activeCallState.phase != CallPhase.IDLE,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            ActiveCallScreen(
                callInfo = activeCallState,
                onEndCall = { dialerViewModel.endCall() },
                onToggleMute = { dialerViewModel.toggleMute() },
                onToggleSpeaker = { dialerViewModel.toggleSpeaker() },
                onSendDtmf = { digit -> dialerViewModel.sendDtmf(digit) }
            )
        }
    }
}
