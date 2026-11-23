package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.domain.model.InputSourceMode
import com.example.domain.model.ModelVariant
import com.example.domain.model.SubscriptionTier
import com.example.ui.InstantHmrViewModel
import com.example.ui.components.AiProviderSettingsDialog
import com.example.ui.components.FitVersoNavItem
import com.example.ui.components.FitVersoSidebar
import com.example.ui.components.LightPageTopBar
import com.example.ui.components.ProPaywallDialog
import com.example.ui.screens.CoachScreen
import com.example.ui.screens.ExercisesScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PlansScreen
import com.example.ui.screens.ProfileSettingsScreen
import com.example.ui.screens.ProgressScreen
import com.example.ui.screens.StudioScreen
import com.example.ui.theme.ArenaDarkBg
import com.example.ui.theme.InstantHmrTheme
import com.example.ui.theme.LightCanvasBg
import com.example.ui.theme.LightCardBg
import com.example.ui.theme.MutedInk
import com.example.ui.theme.SidebarNavy
import com.example.ui.theme.TealMint
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.TealSoft
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

@Serializable
data object StudioRoute

@Serializable
data object ExercisesRoute

@Serializable
data object HistoryRoute

@Serializable
data object ProgressRoute

@Serializable
data object CoachRoute

@Serializable
data object PlansRoute

@Serializable
data object ProfileRoute

private data class CompactNavDestination(
    val navItem: FitVersoNavItem,
    val shortLabel: String,
    val icon: ImageVector,
    val tag: String
)

private val COMPACT_DESTINATIONS = listOf(
    CompactNavDestination(FitVersoNavItem.HOME, "Início", Icons.Rounded.Home, "nav_home"),
    CompactNavDestination(FitVersoNavItem.LIVE, "Ao Vivo", Icons.Rounded.Sensors, "nav_studio"),
    CompactNavDestination(FitVersoNavItem.EXERCISES, "Exercícios", Icons.Rounded.FitnessCenter, "nav_exercises"),
    CompactNavDestination(FitVersoNavItem.HISTORY, "Histórico", Icons.Rounded.History, "nav_history"),
    CompactNavDestination(FitVersoNavItem.PROGRESS, "Progresso", Icons.Rounded.AutoGraph, "nav_progress")
)

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            InstantHmrTheme {
                val windowSize = calculateWindowSizeClass(this)
                val isExpanded = windowSize.widthSizeClass != WindowWidthSizeClass.Compact
                FitVersoApp(isExpandedWidth = isExpanded)
            }
        }
    }
}

@Composable
fun FitVersoApp(
    isExpandedWidth: Boolean,
    viewModel: InstantHmrViewModel = viewModel()
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = backStackEntry?.destination

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val config by viewModel.config.collectAsStateWithLifecycle()
    val aiConfig by viewModel.aiConfig.collectAsStateWithLifecycle()
    val teachState by viewModel.teachState.collectAsStateWithLifecycle()
    val webServerRunning by viewModel.webServerRunning.collectAsStateWithLifecycle()
    val webServerUrls by viewModel.webServerUrls.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val workoutHistory by viewModel.workoutHistory.collectAsStateWithLifecycle()
    val downloadState by viewModel.downloadState.collectAsStateWithLifecycle()
    val isListening by viewModel.isListening.collectAsStateWithLifecycle()
    val voiceStatus by viewModel.voiceStatus.collectAsStateWithLifecycle()

    var openEngineFromArena by remember { mutableStateOf(false) }
    var showAiSettingsDialog by remember { mutableStateOf(false) }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val projectZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        uri?.let(viewModel::exportProjectZip)
    }
    val launchProjectZipDownload: () -> Unit = {
        projectZipLauncher.launch("FitVerso-Projeto-Completo.zip")
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.statusBanner) {
        val msg = uiState.statusBanner
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearBanner()
        }
    }

    val activeNavItem = when {
        currentDest?.hasRoute(StudioRoute::class) == true -> FitVersoNavItem.LIVE
        currentDest?.hasRoute(ExercisesRoute::class) == true -> FitVersoNavItem.EXERCISES
        currentDest?.hasRoute(HistoryRoute::class) == true -> FitVersoNavItem.HISTORY
        currentDest?.hasRoute(ProgressRoute::class) == true -> FitVersoNavItem.PROGRESS
        currentDest?.hasRoute(CoachRoute::class) == true -> FitVersoNavItem.COACH
        currentDest?.hasRoute(PlansRoute::class) == true -> FitVersoNavItem.PLANS
        currentDest?.hasRoute(ProfileRoute::class) == true -> FitVersoNavItem.PROFILE
        else -> FitVersoNavItem.HOME
    }

    fun routeForNavItem(item: FitVersoNavItem): Any = when (item) {
        FitVersoNavItem.HOME -> HomeRoute
        FitVersoNavItem.LIVE -> StudioRoute
        FitVersoNavItem.EXERCISES -> ExercisesRoute
        FitVersoNavItem.HISTORY -> HistoryRoute
        FitVersoNavItem.PROGRESS -> ProgressRoute
        FitVersoNavItem.COACH -> CoachRoute
        FitVersoNavItem.PLANS -> PlansRoute
        FitVersoNavItem.PROFILE -> ProfileRoute
    }

    fun navigateToItem(item: FitVersoNavItem, openEngine: Boolean = false) {
        openEngineFromArena = openEngine && item == FitVersoNavItem.PROFILE
        val route = routeForNavItem(item)
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val isArenaActive = activeNavItem == FitVersoNavItem.LIVE
    val outerBg = if (isArenaActive) ArenaDarkBg else LightCanvasBg

    @Composable
    fun AppContent(modifier: Modifier = Modifier) {
        Row(modifier = modifier.fillMaxSize()) {
            if (isExpandedWidth) {
                FitVersoSidebar(
                    activeItem = activeNavItem,
                    userTier = userProfile.tier,
                    onSelect = { item -> navigateToItem(item) },
                    onDownloadProjectZip = launchProjectZipDownload
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(outerBg)
            ) {
                if (!isArenaActive) {
                    LightPageTopBar(
                        title = activeNavItem.label,
                        showMenuButton = !isExpandedWidth,
                        userTier = userProfile.tier,
                        onOpenMenu = { scope.launch { drawerState.open() } },
                        onDownloadProjectZip = launchProjectZipDownload,
                        onOpenPlans = { navigateToItem(FitVersoNavItem.PLANS) },
                        onOpenProfile = { navigateToItem(FitVersoNavItem.PROFILE) }
                    )
                }

                FitVersoNavHost(
                    navController = navController,
                    isExpandedWidth = isExpandedWidth,
                    openEngineInitial = openEngineFromArena,
                    onOpenMenu = { scope.launch { drawerState.open() } },
                    onNavigateToItem = { item, openEngine -> navigateToItem(item, openEngine) },
                    onOpenAiSettings = { showAiSettingsDialog = true },
                    onDownloadProjectZip = launchProjectZipDownload,
                    viewModel = viewModel,
                    uiState = uiState,
                    config = config,
                    aiConfig = aiConfig,
                    teachState = teachState,
                    webServerRunning = webServerRunning,
                    webServerUrls = webServerUrls,
                    userProfile = userProfile,
                    workoutHistory = workoutHistory,
                    downloadState = downloadState,
                    isListening = isListening,
                    voiceStatus = voiceStatus,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    @Composable
    fun AppScaffold() {
        Scaffold(
            containerColor = outerBg,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (!isExpandedWidth) {
                    val barBg = if (isArenaActive) SidebarNavy else LightCardBg
                    val activeColor = if (isArenaActive) TealMint else TealPrimary
                    val inactiveColor = if (isArenaActive) TextSecondary else MutedInk
                    val indicatorColor = if (isArenaActive) Color(0xFF233547) else TealSoft

                    NavigationBar(containerColor = barBg) {
                        COMPACT_DESTINATIONS.forEach { dest ->
                            val selected = activeNavItem == dest.navItem
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToItem(dest.navItem) },
                                icon = { Icon(dest.icon, contentDescription = dest.navItem.label) },
                                label = { Text(dest.shortLabel, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = activeColor,
                                    selectedTextColor = activeColor,
                                    indicatorColor = indicatorColor,
                                    unselectedIconColor = inactiveColor,
                                    unselectedTextColor = inactiveColor
                                ),
                                modifier = Modifier.testTag(dest.tag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            AppContent(modifier = Modifier.padding(innerPadding))
        }
    }

    if (isExpandedWidth) {
        AppScaffold()
    } else {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = SidebarNavy,
                    windowInsets = WindowInsets.safeDrawing
                ) {
                    FitVersoSidebar(
                        activeItem = activeNavItem,
                        userTier = userProfile.tier,
                        onSelect = { item ->
                            scope.launch { drawerState.close() }
                            navigateToItem(item)
                        },
                        onDownloadProjectZip = {
                            scope.launch { drawerState.close() }
                            launchProjectZipDownload()
                        }
                    )
                }
            }
        ) {
            AppScaffold()
        }
    }

    val paywallFeature = uiState.paywallFeaturePrompt
    if (paywallFeature != null) {
        ProPaywallDialog(
            featureTitle = paywallFeature,
            onUpgradePro = { viewModel.setSubscriptionTier(SubscriptionTier.PREMIUM) },
            onContinueBasic = { viewModel.dismissPaywall() },
            onViewAllPlans = {
                viewModel.dismissPaywall()
                navigateToItem(FitVersoNavItem.PLANS)
            }
        )
    }

    if (showAiSettingsDialog) {
        AiProviderSettingsDialog(
            currentConfig = aiConfig,
            onSave = { updated ->
                viewModel.saveAiProvidersConfig(updated)
                showAiSettingsDialog = false
            },
            onTestVoice = { draft -> viewModel.testVoiceWithConfig(draft) },
            onDismiss = { showAiSettingsDialog = false }
        )
    }
}

@Composable
private fun FitVersoNavHost(
    navController: NavHostController,
    isExpandedWidth: Boolean,
    openEngineInitial: Boolean,
    onOpenMenu: () -> Unit,
    onNavigateToItem: (FitVersoNavItem, Boolean) -> Unit,
    onOpenAiSettings: () -> Unit,
    onDownloadProjectZip: () -> Unit,
    viewModel: InstantHmrViewModel,
    uiState: com.example.ui.StudioUiState,
    config: com.example.domain.model.PipelineConfig,
    aiConfig: com.example.domain.model.AiProvidersConfig,
    teachState: com.example.ui.ExerciseTeachUiState,
    webServerRunning: Boolean,
    webServerUrls: List<String>,
    userProfile: com.example.domain.model.UserFitnessProfile,
    workoutHistory: List<com.example.domain.model.WorkoutHistoryEntry>,
    downloadState: com.example.ml.ModelDownloadState,
    isListening: Boolean,
    voiceStatus: String?,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        modifier = modifier
    ) {
        composable<HomeRoute> {
            HomeScreen(
                profile = userProfile,
                sessionReps = uiState.totalSessionReps,
                latestHistory = workoutHistory.firstOrNull(),
                onStartQuickWorkout = {
                    viewModel.selectExercise(uiState.selectedExerciseId, autoDetect = false)
                    viewModel.setSourceMode(InputSourceMode.CAMERA)
                    onNavigateToItem(FitVersoNavItem.LIVE, false)
                },
                onStartExercise = { exId, auto ->
                    viewModel.selectExercise(exId, autoDetect = auto)
                    viewModel.setSourceMode(InputSourceMode.CAMERA)
                    onNavigateToItem(FitVersoNavItem.LIVE, false)
                },
                onOpenProgress = { onNavigateToItem(FitVersoNavItem.PROGRESS, false) },
                onOpenCoach = { onNavigateToItem(FitVersoNavItem.COACH, false) },
                onOpenPlans = { onNavigateToItem(FitVersoNavItem.PLANS, false) }
            )
        }

        composable<StudioRoute> {
            StudioScreen(
                uiState = uiState,
                config = config,
                userTier = userProfile.tier,
                downloadState = downloadState,
                isExpandedWidth = isExpandedWidth,
                showMenuButton = !isExpandedWidth,
                onOpenMenu = onOpenMenu,
                onSelectSource = viewModel::setSourceMode,
                onSelectViewport = viewModel::setViewportMode,
                onSelectExercise = viewModel::selectExercise,
                onToggleAutoDetect = viewModel::toggleAutoDetectExercise,
                onResetReps = viewModel::resetRepCounter,
                onToggleWorkoutPause = viewModel::toggleWorkoutPause,
                onEndWorkout = viewModel::endAndSaveWorkout,
                onToggleVoiceFeedback = viewModel::toggleVoiceFeedback,
                onToggleSnapshotRecording = viewModel::toggleSnapshotRecording,
                onLoadSample = viewModel::loadSamplePhoto,
                onPickPhotoUri = viewModel::loadPhotoUri,
                onPickVideoUri = viewModel::loadVideoUri,
                shouldAcceptCameraFrame = viewModel::canAcceptCameraFrame,
                onCameraBitmap = viewModel::analyzeCameraBitmap,
                onCameraError = viewModel::reportCameraError,
                onToggleCameraLens = viewModel::toggleCameraLens,
                onToggleVideoPlayPause = viewModel::toggleVideoPlayPause,
                onSeekVideo = viewModel::seekVideoTo,
                onToggleMesh = { show -> viewModel.updateConfig { it.copy(showMesh = show) } },
                onMeshAlphaChange = { a -> viewModel.updateConfig { it.copy(meshAlpha = a) } },
                onExportObj = viewModel::exportFrameToObj,
                onExportJson = viewModel::exportFrameToJson,
                onExportSnapshotsJson = viewModel::exportSnapshotsJson,
                onExportSnapshotsCsv = viewModel::exportSnapshotsCsv,
                onRequirePro = viewModel::requestPaywall,
                onOpenExercises = { onNavigateToItem(FitVersoNavItem.EXERCISES, false) },
                onNavigateToModels = { onNavigateToItem(FitVersoNavItem.PROFILE, true) }
            )
        }

        composable<ExercisesRoute> {
            ExercisesScreen(
                selectedExerciseId = uiState.selectedExerciseId,
                userTier = userProfile.tier,
                aiConfig = aiConfig,
                teachState = teachState,
                onOpenExerciseTeach = viewModel::openExerciseTeaching,
                onTeachExerciseAi = viewModel::teachExerciseWithAi,
                onGenerateExerciseVisual = viewModel::generateExerciseImageWithAi,
                onSpeakExerciseGuide = viewModel::speakExerciseGuide,
                onStartExercise = { exId ->
                    viewModel.selectExercise(exId, autoDetect = false)
                    onNavigateToItem(FitVersoNavItem.LIVE, false)
                },
                onRequirePro = viewModel::requestPaywall,
                onOpenPlans = { onNavigateToItem(FitVersoNavItem.PLANS, false) }
            )
        }

        composable<HistoryRoute> {
            HistoryScreen(
                history = workoutHistory,
                onStartWorkout = { exId ->
                    viewModel.selectExercise(exId, autoDetect = false)
                    viewModel.setSourceMode(InputSourceMode.CAMERA)
                    onNavigateToItem(FitVersoNavItem.LIVE, false)
                }
            )
        }

        composable<ProgressRoute> {
            ProgressScreen(
                profile = userProfile,
                history = workoutHistory
            )
        }

        composable<CoachRoute> {
            CoachScreen(
                uiState = uiState,
                aiConfig = aiConfig,
                userTier = userProfile.tier,
                isListening = isListening,
                voiceStatus = voiceStatus,
                onOpenAiSettings = onOpenAiSettings,
                onSendMessage = viewModel::sendCoachMessage,
                onStartVoice = viewModel::startVoiceCoachListening,
                onStopVoice = viewModel::stopVoiceCoachListening,
                onExportSnapshotsJson = viewModel::exportSnapshotsJson,
                onExportSnapshotsCsv = viewModel::exportSnapshotsCsv,
                onClearSnapshots = viewModel::clearSnapshots,
                onRequirePro = viewModel::requestPaywall
            )
        }

        composable<PlansRoute> {
            PlansScreen(
                profile = userProfile,
                onSelectTier = viewModel::setSubscriptionTier
            )
        }

        composable<ProfileRoute> {
            ProfileSettingsScreen(
                profile = userProfile,
                config = config,
                aiConfig = aiConfig,
                isVariantCached = uiState.isSelectedVariantCached,
                cachedModels = uiState.cachedModels,
                downloadState = downloadState,
                snapshotCount = uiState.snapshotCount,
                webServerRunning = webServerRunning,
                webServerUrls = webServerUrls,
                openEngineInitial = openEngineInitial,
                onOpenAiSettings = onOpenAiSettings,
                onToggleWebServer = viewModel::toggleWebServer,
                onExportWebHtml = viewModel::exportWebAppHtml,
                onDownloadProjectZip = onDownloadProjectZip,
                onUpdatePreferences = { goal, reminders, unit, frontCam ->
                    viewModel.updateProfilePreferences(
                        weeklyGoalDays = goal,
                        remindersEnabled = reminders,
                        unitSystem = unit,
                        defaultFrontCamera = frontCam
                    )
                },
                onOpenPlans = { onNavigateToItem(FitVersoNavItem.PLANS, false) },
                onExportSnapshotsJson = viewModel::exportSnapshotsJson,
                onRequirePro = viewModel::requestPaywall,
                onClearAllHistory = viewModel::clearAllHistoryData,
                onSelectVariant = { v: ModelVariant ->
                    viewModel.updateConfig {
                        it.copy(modelVariant = v, customOnnxPath = null, customOnnxName = null)
                    }
                },
                onSelectFitLevel = { fit ->
                    viewModel.updateConfig { it.copy(fitLevel = fit) }
                },
                onDownloadVariant = viewModel::downloadModel,
                onCancelDownload = viewModel::cancelDownload,
                onImportCustomOnnx = viewModel::importCustomOnnx,
                onDeleteCachedFile = viewModel::deleteCachedModel,
                onUpdateConfig = viewModel::updateConfig
            )
        }
    }
}
