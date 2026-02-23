package com.pasindu.woundcarepro

import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pasindu.woundcarepro.data.local.DatabaseProvider
import com.pasindu.woundcarepro.data.local.repository.AssessmentRepositoryImpl
import com.pasindu.woundcarepro.data.local.repository.AuditRepositoryImpl
import com.pasindu.woundcarepro.data.local.repository.ConsentRepositoryImpl
import com.pasindu.woundcarepro.data.local.repository.MeasurementRepositoryImpl
import com.pasindu.woundcarepro.nativebridge.NativeBridge
import com.pasindu.woundcarepro.security.AppLockManager
import com.pasindu.woundcarepro.security.LockGateState
import com.pasindu.woundcarepro.security.UnlockResult
import com.pasindu.woundcarepro.ui.camera.CameraCaptureScreen
import com.pasindu.woundcarepro.ui.camera.CameraViewModel
import com.pasindu.woundcarepro.ui.camera.ConsentViewModel
import com.pasindu.woundcarepro.ui.camera.ConsentViewModelFactory
import com.pasindu.woundcarepro.ui.export.ExportScreen
import com.pasindu.woundcarepro.ui.history.HistoryScreen
import com.pasindu.woundcarepro.ui.history.HistoryViewModel
import com.pasindu.woundcarepro.ui.history.HistoryViewModelFactory
import com.pasindu.woundcarepro.ui.review.CalibrationScreen
import com.pasindu.woundcarepro.ui.review.CalibrationViewModel
import com.pasindu.woundcarepro.ui.review.CalibrationViewModelFactory
import com.pasindu.woundcarepro.ui.review.MarkerCalibrationScreen
import com.pasindu.woundcarepro.ui.review.MarkerCalibrationViewModel
import com.pasindu.woundcarepro.ui.review.MarkerCalibrationViewModelFactory
import com.pasindu.woundcarepro.ui.review.MeasurementResultScreen
import com.pasindu.woundcarepro.ui.review.MeasurementViewModel
import com.pasindu.woundcarepro.ui.review.MeasurementViewModelFactory
import com.pasindu.woundcarepro.ui.review.ReviewScreen
import com.pasindu.woundcarepro.ui.theme.WoundCareProTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private lateinit var appLockManager: AppLockManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            Log.d("MainActivity", "OpenCV loaded from JNI: ${NativeBridge.openCvVersion()}")
        } catch (throwable: Throwable) {
            Log.e("MainActivity", "Failed to load OpenCV JNI bridge", throwable)
        }
        appLockManager = AppLockManager(applicationContext)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        enableEdgeToEdge()
        setContent { WoundCareRoot(applicationContext, appLockManager) }
    }

    override fun onStart() {
        super.onStart()
        appLockManager.onAppForegrounded()
    }

    override fun onStop() {
        appLockManager.onAppBackgrounded()
        super.onStop()
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        appLockManager.markInteraction()
    }
}

@Composable
private fun WoundCareRoot(applicationContext: android.content.Context, appLockManager: AppLockManager) {
    WoundCareProTheme {
        when (val state = appLockManager.gateState.collectAsState().value) {
            LockGateState.NEEDS_PIN_SETUP -> PinSetupScreen { appLockManager.setupPin(it) }
            LockGateState.LOCKED -> LockScreen { appLockManager.unlock(it) }
            LockGateState.UNLOCKED -> AppContent(applicationContext)
        }
    }
}

@Composable
private fun AppContent(applicationContext: android.content.Context) {
    val navController = rememberNavController()
    val database = remember { DatabaseProvider.getDatabase(applicationContext) }
    val auditRepository = remember { AuditRepositoryImpl(database.auditLogDao()) }
    val consentRepository = remember { ConsentRepositoryImpl(database.consentDao()) }
    val assessmentRepository = remember {
        AssessmentRepositoryImpl(
            database = database,
            assessmentDao = database.assessmentDao(),
            patientDao = database.patientDao(),
            measurementDao = database.measurementDao(),
            auditRepository = auditRepository
        )
    }
    val measurementRepository = remember { MeasurementRepositoryImpl(database.measurementDao()) }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        WoundCareNavGraph(
            navController,
            assessmentRepository,
            measurementRepository,
            consentRepository,
            auditRepository,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

private object Destinations {
    const val Home = "home"
    const val NewAssessment = "new_assessment"
    const val ConsentRoute = "consent/{assessmentId}/{patientId}"
    const val CameraCaptureRoute = "camera_capture/{assessmentId}"
    const val ReviewRoute = "review/{assessmentId}"
    const val MarkerCalibrationRoute = "marker_calibration/{assessmentId}"
    const val CalibrationRoute = "calibration/{assessmentId}"
    const val MeasurementResult = "measurement_result/{assessmentId}"
    const val History = "history"
    const val Export = "export"
}

@Composable
private fun WoundCareNavGraph(
    navController: NavHostController,
    assessmentRepository: AssessmentRepositoryImpl,
    measurementRepository: MeasurementRepositoryImpl,
    consentRepository: ConsentRepositoryImpl,
    auditRepository: AuditRepositoryImpl,
    modifier: Modifier = Modifier
) {
    val calibrationViewModel: CalibrationViewModel = viewModel(factory = CalibrationViewModelFactory(assessmentRepository, auditRepository))
    val measurementViewModel: MeasurementViewModel = viewModel(factory = MeasurementViewModelFactory(assessmentRepository, measurementRepository))
    val markerCalibrationViewModel: MarkerCalibrationViewModel = viewModel(factory = MarkerCalibrationViewModelFactory(assessmentRepository))
    val historyViewModel: HistoryViewModel = viewModel(factory = HistoryViewModelFactory(assessmentRepository, measurementRepository))
    val consentViewModel: ConsentViewModel = viewModel(factory = ConsentViewModelFactory(assessmentRepository, consentRepository, auditRepository))
    val scope = rememberCoroutineScope()

    NavHost(navController = navController, startDestination = Destinations.Home, modifier = modifier) {
        composable(Destinations.Home) {
            PlaceholderScreen(
                title = "Home",
                next = "New Assessment",
                onNext = { navController.navigate(Destinations.NewAssessment) }
            )
        }
        composable(Destinations.NewAssessment) {
            NewAssessmentScreen(
                onCreateAssessment = { assessmentId, patientId ->
                scope.launch {
                    if (consentRepository.hasGrantedConsent(patientId, "PHOTO")) {
                        navController.navigate("camera_capture/$assessmentId")
                    } else {
                        navController.navigate("consent/$assessmentId/$patientId")
                    }
                }
                }
            )
        }
        composable(Destinations.ConsentRoute, arguments = listOf(
            navArgument("assessmentId") { type = NavType.StringType },
            navArgument("patientId") { type = NavType.StringType }
        )) { entry ->
            val assessmentId = entry.arguments?.getString("assessmentId") ?: return@composable
            val patientId = entry.arguments?.getString("patientId") ?: return@composable
            LaunchedEffect(assessmentId) { consentViewModel.load(assessmentId) }
            ConsentScreen(
                assessmentId = assessmentId,
                patientId = patientId,
                viewModel = consentViewModel,
                onAllowed = { navController.navigate("camera_capture/$assessmentId") }
            )
        }
        composable(Destinations.CameraCaptureRoute, arguments = listOf(navArgument("assessmentId") { type = NavType.StringType })) { entry ->
            val assessmentId = entry.arguments?.getString("assessmentId") ?: return@composable
            CameraCaptureScreen(
                assessmentId = assessmentId,
                viewModel = hiltViewModel<CameraViewModel>(),
                onPhotoCaptured = { navController.navigate("review/$assessmentId") { launchSingleTop = true } }
            )
        }
        composable(Destinations.ReviewRoute, arguments = listOf(navArgument("assessmentId") { type = NavType.StringType })) { entry ->
            val assessmentId = entry.arguments?.getString("assessmentId") ?: return@composable
            ReviewScreen(
                assessmentId = assessmentId,
                viewModel = hiltViewModel(),
                onRetake = { navController.popBackStack() },
                onNextAfterSave = { needsCalibration -> navController.navigate(if (needsCalibration) "calibration/$assessmentId" else "measurement_result/$assessmentId") },
                onMarkerCalibration = { navController.navigate("marker_calibration/$assessmentId") }
            )
        }
        composable(Destinations.MarkerCalibrationRoute, arguments = listOf(navArgument("assessmentId") { type = NavType.StringType })) { entry ->
            val assessmentId = entry.arguments?.getString("assessmentId") ?: return@composable
            MarkerCalibrationScreen(
                assessmentId = assessmentId,
                viewModel = markerCalibrationViewModel,
                onRectificationSaved = { navController.navigate("review/$assessmentId") }
            )
        }
        composable(Destinations.CalibrationRoute, arguments = listOf(navArgument("assessmentId") { type = NavType.StringType })) { entry ->
            val assessmentId = entry.arguments?.getString("assessmentId") ?: return@composable
            CalibrationScreen(
                assessmentId = assessmentId,
                viewModel = calibrationViewModel,
                onCalibrationSaved = { navController.navigate("measurement_result/$assessmentId") }
            )
        }
        composable(Destinations.MeasurementResult, arguments = listOf(navArgument("assessmentId") { type = NavType.StringType })) { entry ->
            val assessmentId = entry.arguments?.getString("assessmentId") ?: return@composable
            MeasurementResultScreen(
                assessmentId = assessmentId,
                viewModel = measurementViewModel,
                onCalibrate = { navController.navigate("calibration/$assessmentId") },
                onMarkerCalibration = { navController.navigate("marker_calibration/$assessmentId") },
                onNext = { navController.navigate(Destinations.History) }
            )
        }
        composable(Destinations.History) {
            HistoryScreen(
                viewModel = historyViewModel,
                onNext = { navController.navigate(Destinations.Export) }
            )
        }
        composable(Destinations.Export) {
            ExportScreen(assessmentDao = DatabaseProvider.getDatabase(navController.context).assessmentDao(), onBackHome = {
                navController.navigate(Destinations.Home) { popUpTo(Destinations.Home) { inclusive = true } }
            })
        }
    }
}

@Composable
private fun NewAssessmentScreen(onCreateAssessment: (String, String) -> Unit, modifier: Modifier = Modifier, viewModel: CameraViewModel = hiltViewModel()) {
    var createError by remember { mutableStateOf<String?>(null) }

    Column(modifier = modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("New Assessment", style = MaterialTheme.typography.headlineMedium)
        Button(
            onClick = {
                createError = null
                viewModel.createAssessment(
                    onCreated = onCreateAssessment,
                    onError = { createError = it }
                )
            },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Create Assessment & Open Camera")
        }
        createError?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun PinSetupScreen(onSetPin: (String) -> Boolean) {
    var pin by remember { mutableStateOf(TextFieldValue("")) }
    var confirm by remember { mutableStateOf(TextFieldValue("")) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Set App PIN (4-6 digits)")
        OutlinedTextField(pin, { pin = it.copy(text = it.text.filter(Char::isDigit).take(6)) }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), label = { Text("PIN") })
        OutlinedTextField(confirm, { confirm = it.copy(text = it.text.filter(Char::isDigit).take(6)) }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), label = { Text("Confirm PIN") }, modifier = Modifier.padding(top = 8.dp))
        Button(onClick = {
            when {
                pin.text.length !in 4..6 -> error = "PIN must be 4-6 digits"
                pin.text != confirm.text -> error = "PINs do not match"
                !onSetPin(pin.text) -> error = "Invalid PIN"
                else -> error = null
            }
        }, modifier = Modifier.padding(top = 12.dp)) { Text("Save PIN") }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
    }
}

@Composable
private fun LockScreen(onUnlock: (String) -> UnlockResult) {
    var pin by remember { mutableStateOf(TextFieldValue("")) }
    var message by remember { mutableStateOf("Enter PIN") }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("App Locked")
        OutlinedTextField(pin, { pin = it.copy(text = it.text.filter(Char::isDigit).take(6)) }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), label = { Text("PIN") })
        Button(onClick = {
            when (val result = onUnlock(pin.text)) {
                UnlockResult.Success -> message = "Unlocked"
                is UnlockResult.Invalid -> message = "Invalid PIN. Attempts left: ${result.attemptsRemaining}"
                is UnlockResult.Cooldown -> message = "Too many attempts. Wait ${result.secondsRemaining}s."
            }
            pin = TextFieldValue("")
        }, modifier = Modifier.padding(top = 12.dp)) { Text("Unlock") }
        Text(message, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun ConsentScreen(assessmentId: String, patientId: String, viewModel: ConsentViewModel, onAllowed: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Consent for wound photography and measurement")
        Text("Patient: $patientId", modifier = Modifier.padding(top = 8.dp))
        OutlinedTextField(value = uiState.note, onValueChange = viewModel::updateNote, label = { Text("Optional note") }, modifier = Modifier.padding(top = 12.dp))
        Button(onClick = { viewModel.submit(assessmentId, true, onAllowed) }, modifier = Modifier.padding(top = 12.dp)) { Text("Yes") }
        Button(onClick = { viewModel.submit(assessmentId, false, onAllowed) }, modifier = Modifier.padding(top = 8.dp)) { Text("No") }
        uiState.blockedMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
    }
}

@Composable
private fun PlaceholderScreen(title: String, next: String, onNext: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Button(onClick = onNext, modifier = Modifier.padding(top = 16.dp)) { Text(next) }
    }
}

@Preview(showBackground = true)
@Composable
private fun PlaceholderScreenPreview() {
    WoundCareProTheme {
        PlaceholderScreen(
            title = "Home",
            next = "New Assessment",
            onNext = {}
        )
    }
}
