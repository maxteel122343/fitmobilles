package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FiberManualRecord
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.FlipCameraAndroid
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VideoFile
import androidx.compose.material.icons.rounded.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.domain.fitness.ExerciseCatalog
import com.example.domain.fitness.FormStatus
import com.example.domain.model.InputSourceMode
import com.example.domain.model.PipelineConfig
import com.example.domain.model.SubscriptionTier
import com.example.domain.model.ViewportMode
import com.example.ml.ModelDownloadState
import com.example.ui.StudioUiState
import com.example.ui.components.CameraPreviewPane
import com.example.ui.components.FrameOverlayCanvas
import com.example.ui.components.LatencyChart
import com.example.ui.components.Mhr3DViewport
import com.example.ui.theme.ArenaDarkBg
import com.example.ui.theme.ArenaSurface
import com.example.ui.theme.CoralError
import com.example.ui.theme.CyanBox
import com.example.ui.theme.EmeraldJoint
import com.example.ui.theme.FitVersoDarkArenaTheme
import com.example.ui.theme.GoldBone
import com.example.ui.theme.LabBorder
import com.example.ui.theme.LabSurface
import com.example.ui.theme.LabSurfaceElevated
import com.example.ui.theme.MintSuccess
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.PauseAmber
import com.example.ui.theme.StopCoral
import com.example.ui.theme.TealMint
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.TerracottaMesh
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioScreen(
    uiState: StudioUiState,
    config: PipelineConfig,
    userTier: SubscriptionTier,
    downloadState: ModelDownloadState,
    isExpandedWidth: Boolean,
    showMenuButton: Boolean,
    onOpenMenu: () -> Unit,
    onSelectSource: (InputSourceMode) -> Unit,
    onSelectViewport: (ViewportMode) -> Unit,
    onSelectExercise: (String, Boolean) -> Unit,
    onToggleAutoDetect: () -> Unit,
    onResetReps: () -> Unit,
    onToggleWorkoutPause: () -> Unit,
    onEndWorkout: () -> Unit,
    onToggleVoiceFeedback: () -> Unit,
    onToggleSnapshotRecording: () -> Unit,
    onLoadSample: (Int) -> Unit,
    onPickPhotoUri: (android.net.Uri) -> Unit,
    onPickVideoUri: (android.net.Uri) -> Unit,
    shouldAcceptCameraFrame: () -> Boolean,
    onCameraBitmap: (android.graphics.Bitmap) -> Unit,
    onCameraError: (String) -> Unit,
    onToggleCameraLens: () -> Unit,
    onToggleVideoPlayPause: () -> Unit,
    onSeekVideo: (Long) -> Unit,
    onToggleMesh: (Boolean) -> Unit,
    onMeshAlphaChange: (Float) -> Unit,
    onExportObj: (android.net.Uri) -> Unit,
    onExportJson: (android.net.Uri) -> Unit,
    onExportSnapshotsJson: (android.net.Uri) -> Unit,
    onExportSnapshotsCsv: (android.net.Uri) -> Unit,
    onRequirePro: (String) -> Unit,
    onOpenExercises: () -> Unit,
    onNavigateToModels: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isPro = userTier == SubscriptionTier.PREMIUM

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var askedCameraOnce by remember { mutableStateOf(false) }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(uiState.sourceMode, hasCameraPermission) {
        if (uiState.sourceMode == InputSourceMode.CAMERA && !hasCameraPermission && !askedCameraOnce) {
            askedCameraOnce = true
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(onPickPhotoUri)
    }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(onPickVideoUri)
    }

    val objExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        uri?.let(onExportObj)
    }
    val jsonExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let(onExportJson)
    }
    val snapJsonLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let(onExportSnapshotsJson)
    }
    val snapCsvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let(onExportSnapshotsCsv)
    }

    var showArenaSheet by remember { mutableStateOf(false) }
    var showExercisePickerSheet by remember { mutableStateOf(false) }

    val fb = uiState.exerciseFeedback
    val activeDef = fb?.exercise ?: ExerciseCatalog.findById(uiState.selectedExerciseId)
    val mirrorCamera = uiState.sourceMode == InputSourceMode.CAMERA && uiState.useFrontCamera

    FitVersoDarkArenaTheme {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ArenaDarkBg)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Clean Dark Arena Top Bar (Status Pill "• Câmera conectada" + Viewport + Tune)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                if (showMenuButton) {
                    IconButton(
                        onClick = onOpenMenu,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("studio_menu_button")
                    ) {
                        Icon(Icons.Rounded.Menu, contentDescription = "Abrir menu", tint = TextPrimary)
                    }
                }

                // Live camera / source pill matching image 1 ("• Câmera conectada")
                val cameraConnected = uiState.sourceMode == InputSourceMode.CAMERA && hasCameraPermission
                Surface(
                    onClick = {
                        if (uiState.sourceMode == InputSourceMode.CAMERA) {
                            onSelectSource(InputSourceMode.PHOTO)
                        } else {
                            onSelectSource(InputSourceMode.CAMERA)
                            if (!hasCameraPermission) {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }
                    },
                    color = ArenaSurface,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .border(
                            1.dp,
                            if (cameraConnected) TealMint.copy(alpha = 0.5f) else LabBorder,
                            RoundedCornerShape(20.dp)
                        )
                        .testTag("quick_camera_toggle")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        cameraConnected -> TealMint
                                        uiState.sourceMode == InputSourceMode.VIDEO -> CyanBox
                                        else -> GoldBone
                                    }
                                )
                        )
                        Text(
                            text = when {
                                cameraConnected -> "Câmera conectada"
                                uiState.sourceMode == InputSourceMode.CAMERA -> "Ativar câmera"
                                uiState.sourceMode == InputSourceMode.VIDEO -> "Vídeo ativo"
                                else -> "Foto Demo"
                            },
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                            color = TextPrimary,
                            maxLines = 1
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ViewportModeToggle(selected = uiState.viewportMode, onSelect = onSelectViewport)

                IconButton(
                    onClick = onToggleVoiceFeedback,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("voice_toggle_button")
                ) {
                    Icon(
                        imageVector = if (uiState.voiceFeedbackEnabled) {
                            Icons.AutoMirrored.Rounded.VolumeUp
                        } else {
                            Icons.AutoMirrored.Rounded.VolumeOff
                        },
                        contentDescription = "Voz do Coach",
                        tint = if (uiState.voiceFeedbackEnabled) TealMint else TextSecondary
                    )
                }

                IconButton(
                    onClick = { showArenaSheet = true },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("arena_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Tune,
                        contentDescription = "Fonte, Mesh 3D e Exportação",
                        tint = TextPrimary
                    )
                }
            }
        }

        if (downloadState is ModelDownloadState.Downloading) {
            LinearProgressIndicator(
                progress = { downloadState.fraction },
                modifier = Modifier.fillMaxWidth(),
                color = CyanBox
            )
        }

        // 2. Main Arena Stage matching image 1
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (isExpandedWidth && uiState.viewportMode == ViewportMode.SPLIT) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Left: Camera / 2D Skeleton Feed
                    Column(
                        modifier = Modifier
                            .weight(0.44f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TwoDSourcePane(
                            uiState = uiState,
                            showDiagnosticHud = config.showDiagnosticHud,
                            hasCameraPermission = hasCameraPermission,
                            onRequestCamera = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                            onFallbackToPhoto = { onSelectSource(InputSourceMode.PHOTO) },
                            onToggleCameraLens = onToggleCameraLens,
                            shouldAcceptCameraFrame = shouldAcceptCameraFrame,
                            onCameraBitmap = onCameraBitmap,
                            onCameraError = onCameraError,
                            onPickVideo = {
                                videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                            },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .border(1.dp, LabBorder, RoundedCornerShape(18.dp))
                        )
                        if (config.showDiagnosticHud) {
                            LatencyChart(samples = uiState.timingHistory)
                        }
                    }

                    // Right: Exercise Title + Série/Reps/Tempo + Form Banner + 3D Avatar
                    Column(
                        modifier = Modifier
                            .weight(0.56f)
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ArenaTelemetryHeader(
                            uiState = uiState,
                            exerciseTitle = if (uiState.autoDetectExercise) "Auto · ${activeDef.namePt}" else activeDef.namePt,
                            onResetReps = onResetReps
                        )
                        Mhr3DViewport(
                            frameResult = uiState.frameResult,
                            showMesh = config.showMesh,
                            meshAlpha = config.meshAlpha,
                            exerciseFeedback = uiState.exerciseFeedback,
                            mirrorHorizontal = mirrorCamera,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .border(1.dp, LabBorder, RoundedCornerShape(18.dp))
                        )
                    }
                }
            } else {
                // Compact phone layout OR Single-view 2D/3D mode
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ArenaTelemetryHeader(
                        uiState = uiState,
                        exerciseTitle = if (uiState.autoDetectExercise) "Auto · ${activeDef.namePt}" else activeDef.namePt,
                        onResetReps = onResetReps,
                        compact = !isExpandedWidth
                    )

                    when (uiState.viewportMode) {
                        ViewportMode.SPLIT -> {
                            TwoDSourcePane(
                                uiState = uiState,
                                showDiagnosticHud = config.showDiagnosticHud,
                                hasCameraPermission = hasCameraPermission,
                                onRequestCamera = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                                onFallbackToPhoto = { onSelectSource(InputSourceMode.PHOTO) },
                                onToggleCameraLens = onToggleCameraLens,
                                shouldAcceptCameraFrame = shouldAcceptCameraFrame,
                                onCameraBitmap = onCameraBitmap,
                                onCameraError = onCameraError,
                                onPickVideo = {
                                    videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                                },
                                modifier = Modifier
                                    .weight(0.46f)
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .border(1.dp, LabBorder, RoundedCornerShape(18.dp))
                            )
                            Mhr3DViewport(
                                frameResult = uiState.frameResult,
                                showMesh = config.showMesh,
                                meshAlpha = config.meshAlpha,
                                exerciseFeedback = uiState.exerciseFeedback,
                                mirrorHorizontal = mirrorCamera,
                                modifier = Modifier
                                    .weight(0.54f)
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .border(1.dp, LabBorder, RoundedCornerShape(18.dp))
                            )
                        }
                        ViewportMode.TWO_D -> {
                            TwoDSourcePane(
                                uiState = uiState,
                                showDiagnosticHud = config.showDiagnosticHud,
                                hasCameraPermission = hasCameraPermission,
                                onRequestCamera = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                                onFallbackToPhoto = { onSelectSource(InputSourceMode.PHOTO) },
                                onToggleCameraLens = onToggleCameraLens,
                                shouldAcceptCameraFrame = shouldAcceptCameraFrame,
                                onCameraBitmap = onCameraBitmap,
                                onCameraError = onCameraError,
                                onPickVideo = {
                                    videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .border(1.dp, LabBorder, RoundedCornerShape(18.dp))
                            )
                        }
                        ViewportMode.THREE_D -> {
                            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                // Keep CameraX analyzer bound behind the 3D viewport when in 3D-only mode
                                if (uiState.sourceMode == InputSourceMode.CAMERA && hasCameraPermission) {
                                    CameraPreviewPane(
                                        useFrontCamera = uiState.useFrontCamera,
                                        shouldAnalyze = shouldAcceptCameraFrame,
                                        onFrameBitmap = onCameraBitmap,
                                        onCameraError = onCameraError,
                                        modifier = Modifier.size(1.dp)
                                    )
                                }
                                Mhr3DViewport(
                                    frameResult = uiState.frameResult,
                                    showMesh = config.showMesh,
                                    meshAlpha = config.meshAlpha,
                                    exerciseFeedback = uiState.exerciseFeedback,
                                    mirrorHorizontal = mirrorCamera,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(18.dp))
                                        .border(1.dp, LabBorder, RoundedCornerShape(18.dp))
                                )
                            }
                        }
                    }
                }
            }
        }

        // Video playback scrubber (only when playing a video)
        if (uiState.sourceMode == InputSourceMode.VIDEO && uiState.videoState.uri != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(ArenaSurface)
                    .border(1.dp, LabBorder, RoundedCornerShape(14.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onToggleVideoPlayPause,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("video_play_pause_button")
                ) {
                    Icon(
                        imageVector = if (uiState.videoState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (uiState.videoState.isPlaying) "Pausar" else "Reproduzir",
                        tint = CyanBox
                    )
                }
                val dur = uiState.videoState.durationMs.coerceAtLeast(1L).toFloat()
                Slider(
                    value = uiState.videoState.positionMs.toFloat().coerceIn(0f, dur),
                    onValueChange = { onSeekVideo(it.toLong()) },
                    valueRange = 0f..dur,
                    colors = SliderDefaults.colors(thumbColor = CyanBox, activeTrackColor = CyanBox),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = String.format("%.1fs", uiState.videoState.positionMs / 1000f),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(end = 6.dp)
                )
            }
        }

        // 3. Bottom Action Buttons matching image 1: [■ Encerrar] [❚❚ Pausar] [Trocar exercício]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onEndWorkout,
                colors = ButtonDefaults.buttonColors(containerColor = StopCoral, contentColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                modifier = Modifier.testTag("complete_set_button")
            ) {
                Icon(Icons.Rounded.Stop, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Encerrar", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold), maxLines = 1)
            }

            Button(
                onClick = onToggleWorkoutPause,
                colors = ButtonDefaults.buttonColors(containerColor = PauseAmber, contentColor = ObsidianBg),
                shape = RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                modifier = Modifier.testTag("pause_workout_button")
            ) {
                Icon(
                    imageVector = if (uiState.isWorkoutPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (uiState.isWorkoutPaused) "Retomar" else "Pausar",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1
                )
            }

            Button(
                onClick = { showExercisePickerSheet = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF475569), contentColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                modifier = Modifier.testTag("switch_exercise_button")
            ) {
                Text("Trocar exercício", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium), maxLines = 1)
            }
        }
    }

    // Quick Exercise Picker Bottom Sheet (triggered by "Trocar exercício")
    if (showExercisePickerSheet) {
        ModalBottomSheet(
            onDismissRequest = { showExercisePickerSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = LabSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Trocar exercício", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    FilterChip(
                        selected = uiState.autoDetectExercise,
                        onClick = {
                            onToggleAutoDetect()
                            showExercisePickerSheet = false
                        },
                        label = { Text("Detecção Auto") },
                        leadingIcon = { Icon(Icons.Rounded.AutoAwesome, null, modifier = Modifier.size(15.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GoldBone.copy(alpha = 0.22f),
                            selectedLabelColor = GoldBone,
                            selectedLeadingIconColor = GoldBone
                        ),
                        modifier = Modifier.testTag("exercise_auto_chip")
                    )
                }

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(ExerciseCatalog.FEATURED_MVP_IDS) { exId ->
                        val ex = ExerciseCatalog.findById(exId)
                        val selected = !uiState.autoDetectExercise && uiState.selectedExerciseId == exId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (selected) TealPrimary.copy(alpha = 0.24f) else LabSurfaceElevated)
                                .border(1.dp, if (selected) TealMint else LabBorder, RoundedCornerShape(14.dp))
                                .clickable {
                                    onSelectExercise(exId, false)
                                    showExercisePickerSheet = false
                                }
                                .padding(14.dp)
                                .testTag("exercise_chip_$exId"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(ex.namePt, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                Text(ex.targetMuscles, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                            if (selected) {
                                Icon(Icons.Rounded.CheckCircle, null, tint = TealMint)
                            }
                        }
                    }
                    item {
                        Button(
                            onClick = {
                                showExercisePickerSheet = false
                                onOpenExercises()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary, contentColor = Color.White),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.FitnessCenter, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Explorar todos os 100 exercícios")
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
            }
        }
    }

    // Arena Settings Bottom Sheet (Camera/Photo/Video/Samples, Mesh 3D, Snapshots JSON/CSV/OBJ, Engine)
    if (showArenaSheet) {
        ModalBottomSheet(
            onDismissRequest = { showArenaSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = LabSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Configurações da Arena",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )

                Text("Fonte de Vídeo / Captura", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SourceChip(
                        label = stringResource(R.string.source_camera),
                        icon = Icons.Rounded.CameraAlt,
                        selected = uiState.sourceMode == InputSourceMode.CAMERA,
                        tag = "source_camera_chip",
                        onClick = {
                            onSelectSource(InputSourceMode.CAMERA)
                            if (!hasCameraPermission) cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            showArenaSheet = false
                        },
                        modifier = Modifier.weight(1f)
                    )
                    SourceChip(
                        label = stringResource(R.string.source_photo),
                        icon = Icons.Rounded.PhotoLibrary,
                        selected = uiState.sourceMode == InputSourceMode.PHOTO,
                        tag = "source_photo_chip",
                        onClick = { onSelectSource(InputSourceMode.PHOTO) },
                        modifier = Modifier.weight(1f)
                    )
                    SourceChip(
                        label = stringResource(R.string.source_video),
                        icon = Icons.Rounded.VideoFile,
                        selected = uiState.sourceMode == InputSourceMode.VIDEO,
                        tag = "source_video_chip",
                        onClick = { onSelectSource(InputSourceMode.VIDEO) },
                        modifier = Modifier.weight(1f)
                    )
                }

                when (uiState.sourceMode) {
                    InputSourceMode.PHOTO -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    onLoadSample(0)
                                    showArenaSheet = false
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("sample_runner_button")
                            ) {
                                Text(stringResource(R.string.sample_runner), color = if (uiState.activeSampleIndex == 0) CyanBox else TextPrimary)
                            }
                            OutlinedButton(
                                onClick = {
                                    onLoadSample(1)
                                    showArenaSheet = false
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("sample_dancer_button")
                            ) {
                                Text(stringResource(R.string.sample_dancer), color = if (uiState.activeSampleIndex == 1) CyanBox else TextPrimary)
                            }
                        }
                        Button(
                            onClick = {
                                showArenaSheet = false
                                photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary, contentColor = Color.White),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("pick_photo_button")
                        ) {
                            Text(stringResource(R.string.pick_photo))
                        }
                    }
                    InputSourceMode.VIDEO -> {
                        Button(
                            onClick = {
                                showArenaSheet = false
                                videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary, contentColor = Color.White),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("pick_video_button")
                        ) {
                            Text(stringResource(R.string.pick_video))
                        }
                    }
                    InputSourceMode.CAMERA -> {
                        OutlinedButton(
                            onClick = onToggleCameraLens,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("flip_camera_button")
                        ) {
                            Icon(Icons.Rounded.FlipCameraAndroid, null, tint = CyanBox, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (uiState.useFrontCamera) "Câmera Frontal ativa (Trocar)" else "Câmera Traseira ativa (Trocar)",
                                color = TextPrimary
                            )
                        }
                    }
                }

                HorizontalDivider(color = LabBorder)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.ViewInAr, null, tint = TerracottaMesh)
                        Column {
                            Text("Malha 3D do Avatar", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                            Text("Projeção volumétrica MHR", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                    }
                    Switch(
                        checked = config.showMesh,
                        onCheckedChange = onToggleMesh,
                        colors = SwitchDefaults.colors(checkedThumbColor = ObsidianBg, checkedTrackColor = TerracottaMesh),
                        modifier = Modifier.testTag("toggle_mesh_switch")
                    )
                }

                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Opacidade do Avatar", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Text(String.format("%.0f%%", config.meshAlpha * 100f), style = MaterialTheme.typography.labelMedium, color = TerracottaMesh)
                    }
                    Slider(
                        value = config.meshAlpha,
                        onValueChange = onMeshAlphaChange,
                        valueRange = 0.08f..1.0f,
                        enabled = config.showMesh,
                        colors = SliderDefaults.colors(thumbColor = TerracottaMesh, activeTrackColor = TerracottaMesh),
                        modifier = Modifier.testTag("mesh_alpha_slider")
                    )
                }

                HorizontalDivider(color = LabBorder)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            Icons.Rounded.FiberManualRecord,
                            null,
                            tint = if (uiState.isRecordingSnapshots) CoralError else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Column {
                            Text("Gravar Snapshots (${uiState.snapshotCount})", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                            Text("Keypoints + exercício para análise", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                    }
                    Switch(
                        checked = uiState.isRecordingSnapshots,
                        onCheckedChange = { onToggleSnapshotRecording() },
                        colors = SwitchDefaults.colors(checkedThumbColor = ObsidianBg, checkedTrackColor = CoralError),
                        modifier = Modifier.testTag("snapshot_rec_chip")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (!isPro) {
                                showArenaSheet = false
                                onRequirePro("Exportação de Snapshots JSON/CSV")
                            } else {
                                snapJsonLauncher.launch("fitverso_snapshots.json")
                            }
                        },
                        enabled = uiState.snapshotCount > 0 || !isPro,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.Download, null, tint = CyanBox, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (isPro) "JSON" else "JSON PRO", style = MaterialTheme.typography.labelSmall, color = TextPrimary)
                    }
                    OutlinedButton(
                        onClick = {
                            if (!isPro) {
                                showArenaSheet = false
                                onRequirePro("Exportação de Snapshots JSON/CSV")
                            } else {
                                snapCsvLauncher.launch("fitverso_snapshots.csv")
                            }
                        },
                        enabled = uiState.snapshotCount > 0 || !isPro,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.Download, null, tint = CyanBox, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (isPro) "CSV" else "CSV PRO", style = MaterialTheme.typography.labelSmall, color = TextPrimary)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { objExportLauncher.launch("fitverso_avatar.obj") },
                        enabled = uiState.frameResult?.persons?.isNotEmpty() == true,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Malha 3D .OBJ", style = MaterialTheme.typography.labelSmall, color = TextPrimary)
                    }
                    OutlinedButton(
                        onClick = { jsonExportLauncher.launch("fitverso_frame.json") },
                        enabled = uiState.frameResult?.persons?.isNotEmpty() == true,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Frame .JSON", style = MaterialTheme.typography.labelSmall, color = TextPrimary)
                    }
                }

                Surface(
                    onClick = {
                        showArenaSheet = false
                        onNavigateToModels()
                    },
                    color = LabSurfaceElevated,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, LabBorder, RoundedCornerShape(14.dp))
                        .testTag("engine_badge_button")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Motor Biométrico InstantHMR", style = MaterialTheme.typography.labelMedium, color = TextPrimary)
                            Text(
                                text = if (uiState.isSelectedVariantCached) {
                                    "ONNX ativo · ${config.modelVariant.displayName}"
                                } else {
                                    "Preview On-Device · Baixar pesos ONNX"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (uiState.isSelectedVariantCached) MintSuccess else GoldBone
                            )
                        }
                        Icon(Icons.Rounded.Download, null, tint = CyanBox, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(Modifier.height(14.dp))
            }
        }
    }
    }
}

@Composable
private fun ArenaTelemetryHeader(
    uiState: StudioUiState,
    exerciseTitle: String,
    onResetReps: () -> Unit,
    compact: Boolean = false
) {
    val fb = uiState.exerciseFeedback
    val repCount = fb?.repCount ?: 0
    val statusColor = when (fb?.formStatus) {
        FormStatus.GOOD -> TealMint
        FormStatus.CAUTION -> GoldBone
        FormStatus.FAULT -> StopCoral
        null -> TealMint
    }
    val timeStr = String.format("%02d:%02d", uiState.workoutElapsedSec / 60, uiState.workoutElapsedSec % 60)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 10.dp)
    ) {
        Text(
            text = exerciseTitle,
            style = if (compact) {
                MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            } else {
                MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
            },
            color = Color.White,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Row matching image 1: Série 2/3  |  [ 08 ]  |  Tempo: 02:15
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Série", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Text(
                    text = "${uiState.currentSetNumber}/${uiState.totalPlannedSets}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(ArenaSurface)
                    .border(1.dp, statusColor.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                    .clickable { onResetReps() }
                    .padding(horizontal = if (compact) 18.dp else 24.dp, vertical = if (compact) 6.dp else 10.dp)
                    .testTag("rep_counter_box")
            ) {
                Text(
                    text = String.format("%02d", repCount),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = if (compact) 30.sp else 38.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = statusColor
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Tempo:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Text(
                    text = if (uiState.isWorkoutPaused) "$timeStr (Pausa)" else timeStr,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (uiState.isWorkoutPaused) PauseAmber else Color.White
                )
            }
        }

        // Form feedback banner pill matching image 1 ("Boa profundidade!")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(statusColor.copy(alpha = 0.14f))
                .border(1.dp, statusColor.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = if (compact) 7.dp else 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Rounded.CheckCircle, null, tint = statusColor, modifier = Modifier.size(18.dp))
                Text(
                    text = fb?.formCue ?: "Posicione o corpo inteiro na câmera para iniciar.",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = statusColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (fb != null) {
                Surface(
                    color = ArenaSurface,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = "${fb.primaryLabel} ${fb.primaryAngleDeg.toInt()}°",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = CyanBox,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            } else {
                IconButton(
                    onClick = onResetReps,
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("reset_reps_button")
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "Zerar reps", tint = TextSecondary, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun TwoDSourcePane(
    uiState: StudioUiState,
    showDiagnosticHud: Boolean,
    hasCameraPermission: Boolean,
    onRequestCamera: () -> Unit,
    onFallbackToPhoto: () -> Unit,
    onToggleCameraLens: () -> Unit,
    shouldAcceptCameraFrame: () -> Boolean,
    onCameraBitmap: (android.graphics.Bitmap) -> Unit,
    onCameraError: (String) -> Unit,
    onPickVideo: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.background(ObsidianBg)) {
        when (uiState.sourceMode) {
            InputSourceMode.CAMERA -> {
                if (hasCameraPermission) {
                    CameraPreviewPane(
                        useFrontCamera = uiState.useFrontCamera,
                        shouldAnalyze = shouldAcceptCameraFrame,
                        onFrameBitmap = onCameraBitmap,
                        onCameraError = onCameraError,
                        modifier = Modifier.fillMaxSize()
                    )
                    FrameOverlayCanvas(
                        bitmap = null,
                        frameWidth = uiState.frameWidth,
                        frameHeight = uiState.frameHeight,
                        frameResult = uiState.frameResult,
                        exerciseFeedback = uiState.exerciseFeedback,
                        mirrorHorizontal = uiState.useFrontCamera,
                        showBitmap = false,
                        showDiagnosticHud = showDiagnosticHud,
                        modifier = Modifier.fillMaxSize()
                    )
                    Surface(
                        onClick = onToggleCameraLens,
                        color = LabSurfaceElevated.copy(alpha = 0.85f),
                        shape = CircleShape,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .testTag("overlay_flip_camera_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FlipCameraAndroid,
                            contentDescription = "Trocar câmera frontal/traseira",
                            tint = CyanBox,
                            modifier = Modifier.padding(10.dp).size(20.dp)
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Rounded.CameraAlt, null, tint = CyanBox, modifier = Modifier.size(34.dp))
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.camera_permission_rationale),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = onRequestCamera,
                                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary, contentColor = Color.White),
                                modifier = Modifier.testTag("grant_camera_button")
                            ) {
                                Text(stringResource(R.string.grant_camera_permission))
                            }
                            OutlinedButton(onClick = onFallbackToPhoto) {
                                Text("Usar Foto Demo", color = TextPrimary)
                            }
                        }
                    }
                }
            }
            InputSourceMode.VIDEO -> {
                if (uiState.videoState.uri == null) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Rounded.VideoFile, null, tint = CyanBox, modifier = Modifier.size(34.dp))
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Selecione um vídeo para contar repetições e analisar postura quadro a quadro",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = onPickVideo,
                            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary, contentColor = Color.White)
                        ) {
                            Text(stringResource(R.string.pick_video))
                        }
                    }
                } else {
                    FrameOverlayCanvas(
                        bitmap = uiState.currentBitmap,
                        frameWidth = uiState.frameWidth,
                        frameHeight = uiState.frameHeight,
                        frameResult = uiState.frameResult,
                        exerciseFeedback = uiState.exerciseFeedback,
                        showDiagnosticHud = showDiagnosticHud,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            InputSourceMode.PHOTO -> {
                FrameOverlayCanvas(
                    bitmap = uiState.currentBitmap,
                    frameWidth = uiState.frameWidth,
                    frameHeight = uiState.frameHeight,
                    frameResult = uiState.frameResult,
                    exerciseFeedback = uiState.exerciseFeedback,
                    showDiagnosticHud = showDiagnosticHud,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun SourceChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    tag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        leadingIcon = { Icon(icon, null, modifier = Modifier.size(14.dp)) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = TealPrimary.copy(alpha = 0.25f),
            selectedLabelColor = TealMint,
            selectedLeadingIconColor = TealMint
        ),
        modifier = modifier.testTag(tag)
    )
}

@Composable
private fun ViewportModeToggle(
    selected: ViewportMode,
    onSelect: (ViewportMode) -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(ArenaSurface)
            .border(1.dp, LabBorder, RoundedCornerShape(10.dp))
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        ViewportMode.entries.forEach { mode ->
            val isSel = mode == selected
            Surface(
                onClick = { onSelect(mode) },
                color = if (isSel) TealPrimary.copy(alpha = 0.28f) else ArenaSurface,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("view_mode_${mode.name.lowercase()}")
            ) {
                Text(
                    text = mode.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSel) TealMint else TextSecondary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }
        }
    }
}
