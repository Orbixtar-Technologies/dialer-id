package com.example.ui.main

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Dialpad
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Payment
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.R
import com.example.service.CallPhase
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.AuthViewModel
import com.example.ui.callerid.CallerIdScreen
import com.example.ui.callerid.CallerIdViewModel
import com.example.ui.common.formatBalance
import com.example.ui.contacts.ContactsScreen
import com.example.ui.contacts.ContactsViewModel
import com.example.ui.deposit.DepositScreen
import com.example.ui.deposit.DepositViewModel
import com.example.ui.dialer.ActiveCallScreen
import com.example.ui.dialer.DialerScreen
import com.example.ui.dialer.DialerViewModel
import com.example.ui.history.CallHistoryScreen
import com.example.ui.history.CallHistoryViewModel
import com.example.ui.common.SignalBackdrop
import com.example.ui.common.SignalMark
import com.example.ui.settings.SettingsScreen
import com.example.ui.settings.SettingsViewModel
import com.example.ui.theme.glassBorder
import com.example.ui.theme.isLightScheme

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

@Composable
fun MainAppShell(
    authViewModel: AuthViewModel = viewModel(),
    dialerViewModel: DialerViewModel = viewModel(),
    callerIdViewModel: CallerIdViewModel = viewModel(),
    historyViewModel: CallHistoryViewModel = viewModel(),
    depositViewModel: DepositViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel(),
    contactsViewModel: ContactsViewModel = viewModel()
) {
    val authState by authViewModel.uiState.collectAsState()
    val activeCallState by dialerViewModel.activeCallState.collectAsState()
    val userProfile by dialerViewModel.userProfile.collectAsState()
    val registrationState by dialerViewModel.registrationState.collectAsState()

    var currentTab by remember { mutableStateOf(AppTab.DIALER) }
    // Contacts is a full-screen destination layered over the tabs rather than a
    // sixth bottom-nav item, which would crowd the bar.
    var showContacts by remember { mutableStateOf(false) }

    if (!authState.isAuthenticated) {
        AuthScreen(
            viewModel = authViewModel,
            onLoginSuccess = { currentTab = AppTab.DIALER }
        )
        return
    }

    BackHandler(enabled = showContacts) { showContacts = false }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                val colors = MaterialTheme.colorScheme
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = colors.surface.copy(alpha = if (colors.isLightScheme) 0.94f else 0.88f),
                    shadowElevation = if (colors.isLightScheme) 1.dp else 0.dp,
                    tonalElevation = 0.dp
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .windowInsetsPadding(WindowInsets.statusBars)
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                SignalMark(
                                    size = 38.dp,
                                    icon = Icons.Default.Phone
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = stringResource(R.string.app_name),
                                        style = MaterialTheme.typography.titleLarge,
                                        color = colors.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(colors.primaryContainer)
                                            .padding(horizontal = 6.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.main_hd_badge),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 9.sp,
                                                letterSpacing = 0.8.sp
                                            ),
                                            color = colors.onPrimaryContainer
                                        )
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { showContacts = true },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("topbar_contacts_button")
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(colors.surfaceContainer)
                                            .border(1.dp, colors.glassBorder, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Contacts,
                                            contentDescription = stringResource(R.string.contacts_open),
                                            tint = colors.onSurface,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .heightIn(min = 40.dp)
                                        .clip(RoundedCornerShape(22.dp))
                                        .background(colors.primaryContainer)
                                        .border(
                                            1.dp,
                                            colors.primary.copy(alpha = 0.22f),
                                            RoundedCornerShape(22.dp)
                                        )
                                        .clickable(
                                            onClickLabel = stringResource(R.string.main_open_deposit)
                                        ) { currentTab = AppTab.DEPOSIT }
                                        .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
                                        .testTag("topbar_balance_pill")
                                ) {
                                    Text(
                                        text = "$${formatBalance(userProfile.creditBalance)}",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            letterSpacing = 0.2.sp
                                        ),
                                        color = colors.onPrimaryContainer,
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(colors.primary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            tint = colors.onPrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }

                                if (!userProfile.photoUrl.isNullOrEmpty()) {
                                    IconButton(
                                        onClick = { currentTab = AppTab.SETTINGS },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .testTag("topbar_profile_button")
                                    ) {
                                        AsyncImage(
                                            model = userProfile.photoUrl,
                                            contentDescription = stringResource(R.string.main_open_settings),
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .border(1.5.dp, colors.primary.copy(alpha = 0.35f), CircleShape)
                                        )
                                    }
                                }
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(colors.glassBorder)
                        )
                    }
                }
            },
            bottomBar = {
                val colors = MaterialTheme.colorScheme
                Column(
                    modifier = Modifier
                        .background(colors.background)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(start = 16.dp, end = 16.dp, bottom = 10.dp, top = 4.dp)
                ) {
                    NavigationBar(
                        modifier = Modifier
                            .clip(RoundedCornerShape(28.dp))
                            .border(1.dp, colors.glassBorder, RoundedCornerShape(28.dp)),
                        containerColor = colors.surface,
                        tonalElevation = 0.dp,
                        windowInsets = WindowInsets(0, 0, 0, 0)
                    ) {
                        AppTab.entries.forEach { tab ->
                            val isSelected = currentTab == tab && !showContacts
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = {
                                    currentTab = tab
                                    showContacts = false
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = null,
                                        modifier = Modifier.size(22.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.title,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            letterSpacing = 0.2.sp
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = colors.onPrimaryContainer,
                                    selectedTextColor = colors.primary,
                                    indicatorColor = colors.primaryContainer,
                                    unselectedIconColor = colors.onSurfaceVariant,
                                    unselectedTextColor = colors.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            SignalBackdrop(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (showContacts) {
                    ContactsScreen(
                        viewModel = contactsViewModel,
                        isCallEnabled = registrationState.isRegistered &&
                            userProfile.creditBalance > 0.0,
                        onCallNumber = { number ->
                            if (dialerViewModel.placeCallTo(number)) {
                                showContacts = false
                                currentTab = AppTab.DIALER
                            }
                        },
                        onFillDialer = { number ->
                            dialerViewModel.onNumberChanged(number)
                            showContacts = false
                            currentTab = AppTab.DIALER
                        },
                        onBack = { showContacts = false }
                    )
                } else when (currentTab) {
                    AppTab.DIALER -> DialerScreen(
                        viewModel = dialerViewModel,
                        contactsViewModel = contactsViewModel,
                        onNavigateToDeposit = { currentTab = AppTab.DEPOSIT },
                        onNavigateToCallerIds = { currentTab = AppTab.CALLER_IDS },
                        onNavigateToHistory = { currentTab = AppTab.HISTORY },
                        onNavigateToContacts = { showContacts = true }
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
