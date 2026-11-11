package com.pj95.clockwise.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pj95.clockwise.MainActivity
import com.pj95.clockwise.alarm.readAlarmReadiness
import com.pj95.clockwise.core.ui.R
import com.pj95.clockwise.model.CityDirectory
import com.pj95.clockwise.data.repository.PrefsError
import com.pj95.clockwise.model.alarm.Alarm
import com.pj95.clockwise.model.time.MinuteOfDay
import com.pj95.clockwise.screensaver.ScreenSaverActivity
import com.pj95.clockwise.ui.components.ClockwiseBottomBar
import com.pj95.clockwise.ui.components.ClockwiseTab
import com.pj95.clockwise.ui.mapping.buildPlannerUiState
import com.pj95.clockwise.ui.navigation.Routes
import com.pj95.clockwise.ui.screens.AboutScreen
import com.pj95.clockwise.ui.screens.AlarmEditorSheet
import com.pj95.clockwise.ui.screens.AlarmScreen
import com.pj95.clockwise.ui.screens.CityDetailScreen
import com.pj95.clockwise.ui.screens.CityLabelDialog
import com.pj95.clockwise.ui.screens.CitySearchSheet
import com.pj95.clockwise.ui.screens.ClocksScreen
import com.pj95.clockwise.ui.screens.HelpScreen
import com.pj95.clockwise.ui.screens.MeetingPlannerScreen
import com.pj95.clockwise.ui.screens.PrivacyScreen
import com.pj95.clockwise.ui.screens.SettingsSheet
import com.pj95.clockwise.ui.screens.StudioScreen
import com.pj95.clockwise.ui.theme.ClockwiseColors
import com.pj95.clockwise.ui.theme.FixedClockwiseTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * App shell: bottom navigation, the navigation graph, the city search sheet, the
 * alarm editor and the city label dialog.
 *
 * Alarms, Clocks and Map are destinations. Selecting a city opens its detail
 * screen; tapping an alarm card opens the editor sheet.
 */
@Composable
fun ClockwiseRoot(
    modifier: Modifier = Modifier,
    viewModel: ClockwiseViewModel = viewModel(factory = ClockwiseViewModel.Factory),
    alarmViewModel: AlarmViewModel = viewModel(factory = AlarmViewModel.Factory),
    timerViewModel: TimerViewModel = viewModel(factory = TimerViewModel.Factory),
    stopwatchViewModel: StopwatchViewModel = viewModel(factory = StopwatchViewModel.Factory),
    initialCityId: String? = null,
    onInitialCityOpened: () -> Unit = {},
    initialShortcut: String? = null,
    onInitialShortcutOpened: () -> Unit = {},
    /** Set when the status-bar alarm icon opened the app. */
    initialAlarmId: Long? = null,
    onInitialAlarmOpened: () -> Unit = {},
    /** Set when the timer notification opened the app. */
    initialTimer: Boolean = false,
    onInitialTimerOpened: () -> Unit = {},
    /** Set when a locked premium widget asked for the Studio gallery. */
    initialStudio: Boolean = false,
    onInitialStudioOpened: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val alarmState by alarmViewModel.uiState.collectAsStateWithLifecycle()
    val timerState by timerViewModel.uiState.collectAsStateWithLifecycle()
    val stopwatchState by stopwatchViewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var showSearchSheet by rememberSaveable { mutableStateOf(false) }
    var showSettingsSheet by rememberSaveable { mutableStateOf(false) }

    // True when a timer notification asked for the countdown. The Alarms tab is
    // a destination and the timer is a section inside it, so the request has to
    // survive navigation and then be cleared by the screen that honoured it.
    var timerRequested by rememberSaveable { mutableStateOf(false) }

    // Which alarm the editor is showing. `null` means closed, [Alarm.NEW] means
    // "create one"; a row id means "edit that row". An id rather than an Alarm,
    // so a rotation always re-reads the stored value.
    var editorAlarmId by rememberSaveable { mutableStateOf<Long?>(null) }

    // City whose label is being edited. The id rather than the label itself, so
    // the dialog always opens on the stored value.
    var labelDialogCityId by rememberSaveable { mutableStateOf<String?>(null) }

    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val selectedTab = when (currentRoute) {
        Routes.ALARMS -> ClockwiseTab.Alarms
        else -> ClockwiseTab.Clocks
    }

    val readErrorMessage = stringResource(R.string.error_storage_read)
    val writeErrorMessage = stringResource(R.string.error_storage_write)

    val context = LocalContext.current

    // Read whenever the shell resumes: the user may have changed a permission in
    // system settings and come straight back. Reading is cheap and never throws.
    var readiness by remember { mutableStateOf(readAlarmReadiness(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        readiness = readAlarmReadiness(context)
    }

    // Alarms are notifications first and screens second: without this permission
    // on Android 13+ the ring has nothing to appear in. Asked at the moment an
    // alarm is armed, which is when the reason for it is obvious.
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* Denial is not fatal: the alarm list still works, it just cannot ring. */ }

    fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    fun openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val intent = Intent(
            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
            Uri.parse("package:${context.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    /**
     * The one permission an alarm app cannot simply ask for on modern Android:
     * battery optimisation. The deep link takes the user to the system list where
     * the exemption is granted; the explanation lives in Help, and the readiness
     * read decides whether the row should point there at all.
     */
    fun openBatteryOptimizationSettings() {
        val intent = Intent(
            // ACTION_IGNORE_BATTERY_OPTIMIZATION is a framework constant some
            // compile SDK levels do not surface; the action string is stable.
            "android.settings.IGNORE_BATTERY_OPTIMIZATION",
            Uri.parse("package:${context.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    fun openNotificationSettings() {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    /**
     * Android 14 lets the user switch full-screen alarms off per app. The ring
     * still happens, but lands as a heads-up instead of the lock screen, which
     * is exactly the difference this row exists to explain.
     */
    fun openFullScreenSettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return
        val intent = Intent(
            Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
            Uri.parse("package:${context.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    fun openDndSettings() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    val feedbackCopiedMessage = stringResource(R.string.feedback_copied)
    fun sendFeedback() {
        if (Feedback.send(context)) return
        // No mail app: keep the user's work rather than dropping the tap.
        Feedback.copyDiagnostics(context)
        coroutineScope.launch { snackbarHostState.showSnackbar(feedbackCopiedMessage) }
    }

    val undoLabel = stringResource(R.string.action_undo)
    val alarmDeletedMessage = stringResource(R.string.alarm_deleted)
    val cityRemovedMessage = stringResource(R.string.city_removed)

    /**
     * Deletes first and offers Undo afterwards, rather than asking first.
     *
     * A swipe is already a decision; a confirmation dialog would make the fast
     * gesture slower than opening the editor. Any snackbar still on screen is
     * dismissed so the action always belongs to the deletion that just happened.
     */
    fun showUndo(message: String, undo: () -> Unit) {
        coroutineScope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = undoLabel,
                duration = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) undo()
        }
    }

    fun deleteAlarm(alarm: Alarm) {
        alarmViewModel.delete(alarm.id)
        // The whole value is kept, so Undo restores the same id, order and
        // snooze state instead of creating a lookalike at the end of the list.
        showUndo(alarmDeletedMessage) { alarmViewModel.restore(alarm) }
    }

    fun deleteCity(cityId: String) {
        val order = state.cities.map { it.id }
        val activeCityId = state.activeCityId
        viewModel.removeCity(cityId)
        showUndo(cityRemovedMessage) {
            viewModel.restoreCity(cityId, order, activeCityId)
        }
    }

    // A widget row names the city it wants. This runs once, after the graph is
    // up, and only for a city the directory still knows; the callback clears the
    // pending id so the same tap cannot be replayed on recomposition.
    LaunchedEffect(initialCityId, navController) {
        val cityId = initialCityId ?: return@LaunchedEffect
        // The NavHost installs its graph in the same composition that starts this
        // effect, so wait for the first destination before navigating.
        navController.currentBackStackEntryFlow.first()
        if (CityDirectory.find(cityId) != null) {
            viewModel.selectCity(cityId)
            navController.navigate(Routes.cityDetail(cityId)) {
                launchSingleTop = true
            }
        }
        onInitialCityOpened()
    }

    // Persistence failures are surfaced as a non-blocking message.
    LaunchedEffect(viewModel) {
        viewModel.errors.collect { error ->
            val message = when (error) {
                PrefsError.ReadFailed -> readErrorMessage
                PrefsError.WriteFailed -> writeErrorMessage
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    val navigateToTab: (ClockwiseTab) -> Unit = { tab ->
        val route = when (tab) {
            ClockwiseTab.Alarms -> Routes.ALARMS
            ClockwiseTab.Clocks -> Routes.CLOCKS
        }
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    // The status-bar alarm icon opens the app on the alarm list.
    LaunchedEffect(initialAlarmId, navController) {
        if (initialAlarmId == null) return@LaunchedEffect
        navController.currentBackStackEntryFlow.first()
        navigateToTab(ClockwiseTab.Alarms)
        onInitialAlarmOpened()
    }

    // The timer notification opens the same tab, where the countdown lives. The
    // screen is told separately: reaching the tab is not the same as landing on
    // the countdown, and a notification that arrived while the tab was already
    // open must still switch to it.
    LaunchedEffect(initialTimer, navController) {
        if (!initialTimer) return@LaunchedEffect
        navController.currentBackStackEntryFlow.first()
        timerRequested = true
        navigateToTab(ClockwiseTab.Alarms)
        onInitialTimerOpened()
    }

    // A locked widget deep-links straight to the gallery, where the style it is
    // showing can be unlocked or trialled. Consumed once, after the graph exists.
    LaunchedEffect(initialStudio, navController) {
        if (!initialStudio) return@LaunchedEffect
        navController.currentBackStackEntryFlow.first()
        navController.navigate(Routes.STUDIO) { launchSingleTop = true }
        onInitialStudioOpened()
    }

    // A launcher shortcut names a destination. Like the widget's city extra,
    // this is consumed once and only after the graph exists.
    LaunchedEffect(initialShortcut, navController) {
        val shortcut = initialShortcut ?: return@LaunchedEffect
        navController.currentBackStackEntryFlow.first()
        when (shortcut) {
            MainActivity.SHORTCUT_PLANNER -> navController.navigate(Routes.PLANNER) {
                launchSingleTop = true
            }
            MainActivity.SHORTCUT_ADD_CITY -> showSearchSheet = true
        }
        onInitialShortcutOpened()
    }

    // The shell is a stack so the label dialog can sit above the bottom bar as
    // well as the current destination.
    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = ClockwiseColors.Background,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                ClockwiseBottomBar(
                    selectedTab = selectedTab,
                    onSelectTab = navigateToTab,
                )
            },
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Routes.CLOCKS,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                composable(Routes.ALARMS) {
                    AlarmScreen(
                        state = alarmState,
                        use24Hour = state.use24Hour,
                        onAddClick = { editorAlarmId = Alarm.NEW },
                        onAlarmClick = { alarm -> editorAlarmId = alarm.id },
                        onToggleAlarm = { alarmId, enabled ->
                            alarmViewModel.setEnabled(alarmId, enabled)
                            if (enabled) requestNotificationPermissionIfNeeded()
                        },
                        onAllowExactAlarms = ::openExactAlarmSettings,
                        timerState = timerState,
                        onStartTimer = { duration ->
                            timerViewModel.start(duration)
                            requestNotificationPermissionIfNeeded()
                        },
                        onPauseTimer = timerViewModel::pause,
                        onResumeTimer = timerViewModel::resume,
                        onResetTimer = timerViewModel::reset,
                        onExtendTimer = timerViewModel::extendBy,
                        onRestartTimer = timerViewModel::restart,
                        openTimer = timerRequested,
                        onTimerOpened = { timerRequested = false },
                        onDeleteAlarm = ::deleteAlarm,
                        onReorderAlarms = alarmViewModel::reorder,
                        stopwatchState = stopwatchState,
                        onToggleStopwatch = stopwatchViewModel::toggle,
                        onResetStopwatch = stopwatchViewModel::reset,
                    )
                }

                composable(Routes.CLOCKS) {
                    ClocksScreen(
                        state = state,
                        onCityClick = { city ->
                            viewModel.selectCity(city.id)
                            navController.navigate(Routes.cityDetail(city.id))
                        },
                        onAddClick = { showSearchSheet = true },
                        onFindCityClick = { showSearchSheet = true },
                        onSettingsClick = { showSettingsSheet = true },
                        onPlanMeetingClick = { navController.navigate(Routes.PLANNER) },
                        onWidgetGalleryClick = {
                            navController.navigate(Routes.STUDIO) { launchSingleTop = true }
                        },
                        onReorderCities = viewModel::reorderCities,
                        onDeleteCity = { city -> deleteCity(city.id) },
                    )
                }

                composable(Routes.PLANNER) {
                    // The planner opens on "now" so it agrees with the Clocks
                    // screen, and is the user's from the first drag. Null means
                    // "not touched yet"; it is saved so a rotation does not hand
                    // a chosen slot back to the clock.
                    var plannerMinute by rememberSaveable { mutableStateOf<Int?>(null) }
                    // Null means the device's own zone, which is the v1.0
                    // behaviour and the default the converter starts from. A
                    // city id is saved so a rotation keeps the chosen source.
                    var sourceCityId by rememberSaveable { mutableStateOf<String?>(null) }
                    val zone = viewModel.deviceZone
                    val minuteOfDay = MinuteOfDay(
                        plannerMinute ?: MinuteOfDay
                            .ofInstant(state.now, zone)
                            .snappedToGrid()
                            .value,
                    )

                    MeetingPlannerScreen(
                        planner = buildPlannerUiState(
                            state = state,
                            minuteOfDay = minuteOfDay,
                            deviceZoneId = zone,
                            sourceCityId = sourceCityId,
                        ),
                        onMinuteChange = { plannerMinute = it.value },
                        onSourceChange = { sourceCityId = it },
                        onBack = { navController.popBackStack() },
                        onFindCityClick = { showSearchSheet = true },
                    )
                }

                composable(
                    route = Routes.CITY_DETAIL,
                    arguments = listOf(navArgument(Routes.CITY_ID_ARG) { type = NavType.StringType }),
                ) { entry ->
                    val cityId = entry.arguments?.getString(Routes.CITY_ID_ARG).orEmpty()
                    val city = CityDirectory.find(cityId)
                    if (city == null) {
                        // Unknown id (should not happen): leave the screen.
                        LaunchedEffect(cityId) { navController.popBackStack() }
                    } else {
                        CityDetailScreen(
                            city = city,
                            now = state.now,
                            use24Hour = state.use24Hour,
                            deviceLocal = state.deviceLocal,
                            label = state.cities.firstOrNull { it.id == cityId }?.label,
                            onBack = { navController.popBackStack() },
                            onToggleFormat = viewModel::setUse24Hour,
                            onEditLabel = { labelDialogCityId = cityId },
                            onRemove = {
                                viewModel.removeCity(cityId)
                                navController.popBackStack()
                            },
                            showDifference = state.compareWithLocalTime,
                        )
                    }
                }

                composable(Routes.PRIVACY) {
                    PrivacyScreen(onBack = { navController.popBackStack() })
                }

                composable(Routes.HELP) {
                    HelpScreen(onBack = { navController.popBackStack() })
                }

                composable(Routes.ABOUT) {
                    AboutScreen(onBack = { navController.popBackStack() })
                }

                composable(Routes.STUDIO) {
                    StudioScreen(onBack = { navController.popBackStack() })
                }
            }
        }

        if (showSearchSheet) {
            CitySearchSheet(
                now = state.now,
                use24Hour = state.use24Hour,
                savedCityIds = state.cities.map { it.id }.toSet(),
                onAddCity = { cityId ->
                    viewModel.addCity(cityId)
                    showSearchSheet = false
                    navigateToTab(ClockwiseTab.Clocks)
                },
                onDismiss = { showSearchSheet = false },
            )
        }

        if (showSettingsSheet) {
            SettingsSheet(
                themeMode = state.themeMode,
                onThemeModeSelected = viewModel::setThemeMode,
                use24Hour = state.use24Hour,
                onUse24HourChange = viewModel::setUse24Hour,
                compareWithLocalTime = state.compareWithLocalTime,
                onCompareWithLocalTimeChange = viewModel::setCompareWithLocalTime,
                onShowIntro = viewModel::restartOnboarding,
                onSendFeedback = ::sendFeedback,
                onDismiss = { showSettingsSheet = false },
                readiness = readiness,
                onOpenNotificationSettings = ::openNotificationSettings,
                onOpenExactAlarmSettings = ::openExactAlarmSettings,
                onOpenFullScreenSettings = ::openFullScreenSettings,
                onOpenBatterySettings = ::openBatteryOptimizationSettings,
                onOpenDndSettings = ::openDndSettings,
            )
        }

        // The editor is resolved from the live list, so deleting the alarm
        // underneath it closes the sheet instead of leaving a stale value on
        // screen. `Alarm.NEW` has no row yet and is always editable.
        val editingAlarm = editorAlarmId?.let { id ->
            if (id == Alarm.NEW) null else alarmState.alarms.firstOrNull { it.id == id }
        }
        val editorOpen = editorAlarmId?.let { id ->
            !alarmState.isLoading && (id == Alarm.NEW || editingAlarm != null)
        } == true
        if (editorOpen) {
            AlarmEditorSheet(
                alarm = editingAlarm,
                use24Hour = state.use24Hour,
                onSave = { alarm ->
                    alarmViewModel.save(alarm)
                    editorAlarmId = null
                    if (alarm.enabled) requestNotificationPermissionIfNeeded()
                },
                onDelete = { alarmId ->
                    alarmViewModel.delete(alarmId)
                    editorAlarmId = null
                },
                onDismiss = { editorAlarmId = null },
                // Material You is the theme that asks for the platform dial.
                theme = state.theme,
            )
        }

        // Resolved from the live list, so the dialog closes itself if the city is
        // removed while it is open.
        val labelDialogCity = labelDialogCityId?.let { id ->
            state.cities.firstOrNull { it.id == id }
        }
        if (labelDialogCity != null) {
            CityLabelDialog(
                cityName = labelDialogCity.name,
                currentLabel = labelDialogCity.label,
                onSave = { label ->
                    viewModel.setCityLabel(labelDialogCity.id, label)
                    labelDialogCityId = null
                },
                onDismiss = { labelDialogCityId = null },
            )
        }
    }
}

@Preview(name = "App shell 360dp", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun ClockwiseRootPreview() {
    FixedClockwiseTheme {
        ClockwiseRoot()
    }
}
