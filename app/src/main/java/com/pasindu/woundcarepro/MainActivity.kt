package com.pasindu.woundcarepro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.appcompat.app.AppCompatDelegate
import com.pasindu.woundcarepro.data.local.DatabaseProvider
import com.pasindu.woundcarepro.data.local.repository.AssessmentRepositoryImpl
import com.pasindu.woundcarepro.data.local.repository.MeasurementRepositoryImpl
import com.pasindu.woundcarepro.ui.camera.CameraCaptureScreen
import com.pasindu.woundcarepro.ui.camera.CameraViewModel
import com.pasindu.woundcarepro.ui.history.HistoryScreen
import com.pasindu.woundcarepro.ui.history.HistoryViewModel
import com.pasindu.woundcarepro.ui.history.HistoryViewModelFactory
import com.pasindu.woundcarepro.ui.review.CalibrationScreen
import com.pasindu.woundcarepro.ui.review.CalibrationViewModel
import com.pasindu.woundcarepro.ui.review.CalibrationViewModelFactory
import com.pasindu.woundcarepro.ui.review.MeasurementResultScreen
import com.pasindu.woundcarepro.ui.review.MeasurementViewModel
import com.pasindu.woundcarepro.ui.review.MeasurementViewModelFactory
import com.pasindu.woundcarepro.ui.review.ReviewScreen
import com.pasindu.woundcarepro.ui.review.ReviewViewModel
import com.pasindu.woundcarepro.ui.review.ReviewViewModelFactory
import com.pasindu.woundcarepro.ui.theme.WoundCareProTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        enableEdgeToEdge()
        setContent {
            WoundCareProTheme {
                val navController = rememberNavController()
                val database = remember { DatabaseProvider.getDatabase(applicationContext) }
                val assessmentRepository = remember {
                    AssessmentRepositoryImpl(
                        database = database,
                        assessmentDao = database.assessmentDao(),
                        measurementDao = database.measurementDao()
                    )
                }
                val measurementRepository = remember { MeasurementRepositoryImpl(database.measurementDao()) }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    WoundCareNavGraph(
                        navController = navController,
                        assessmentRepository = assessmentRepository,
                        measurementRepository = measurementRepository,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

private object Destinations {
    const val Home = "home"
    const val Patients = "patients"
    const val PatientDetails = "patient_details"
    const val NewAssessment = "new_assessment"
    const val CameraCapture = "camera_capture"
    const val CameraCaptureRoute = "camera_capture/{assessmentId}"
    const val Review = "review"
    const val ReviewRoute = "review/{assessmentId}"
    const val Calibration = "calibration"
    const val CalibrationRoute = "calibration/{assessmentId}"
    const val ManualOutline = "manual_outline"
    const val MeasurementResult = "measurement_result"
    const val MeasurementResultRoute = "measurement_result/{assessmentId}"
    const val History = "history"
    const val Export = "export"
}

@Composable
private fun WoundCareNavGraph(
    navController: NavHostController,
    assessmentRepository: AssessmentRepositoryImpl,
    measurementRepository: MeasurementRepositoryImpl,
    modifier: Modifier = Modifier
) {
    val reviewViewModel: ReviewViewModel = viewModel(factory = ReviewViewModelFactory(assessmentRepository))
    val calibrationViewModel: CalibrationViewModel = viewModel(factory = CalibrationViewModelFactory(assessmentRepository))
    val measurementViewModel: MeasurementViewModel = viewModel(
        factory = MeasurementViewModelFactory(assessmentRepository, measurementRepository)
    )
    val historyViewModel: HistoryViewModel = viewModel(
        factory = HistoryViewModelFactory(assessmentRepository, measurementRepository)
    )

    NavHost(
        navController = navController,
        startDestination = Destinations.Home,
        modifier = modifier
    ) {
        composable(Destinations.Home) {
            PlaceholderScreen(
                title = "Home",
                next = "Go to Patients",
                onNext = { navController.navigate(Destinations.Patients) }
            )
        }
        composable(Destinations.Patients) {
            PlaceholderScreen(
                title = "Patients",
                next = "Go to Patient Details",
                onNext = { navController.navigate(Destinations.PatientDetails) }
            )
        }
        composable(Destinations.PatientDetails) {
            PlaceholderScreen(
                title = "Patient Details",
                next = "Go to New Assessment",
                onNext = { navController.navigate(Destinations.NewAssessment) }
            )
        }
        composable(Destinations.NewAssessment) {
            NewAssessmentScreen(
                onCreateAssessment = { navController.navigate("${Destinations.CameraCapture}/$it") }
            )
        }
        composable(
            route = Destinations.CameraCaptureRoute,
            arguments = listOf(navArgument("assessmentId") { type = NavType.StringType })
        ) { backStackEntry ->
            val assessmentId = backStackEntry.arguments?.getString("assessmentId") ?: return@composable
            CameraCaptureScreen(
                assessmentId = assessmentId,
                viewModel = hiltViewModel<CameraViewModel>(),
                onPhotoCaptured = { navController.navigate("${Destinations.Review}/$assessmentId") }
            )
        }
        composable(
            route = Destinations.ReviewRoute,
            arguments = listOf(navArgument("assessmentId") { type = NavType.StringType })
        ) { backStackEntry ->
            val assessmentId = backStackEntry.arguments?.getString("assessmentId") ?: return@composable
            ReviewScreen(
                assessmentId = assessmentId,
                viewModel = reviewViewModel,
                onRetake = { navController.popBackStack() },
                onAccept = { navController.navigate("${Destinations.MeasurementResult}/$assessmentId") }
            )
        }
        composable(
            route = Destinations.CalibrationRoute,
            arguments = listOf(navArgument("assessmentId") { type = NavType.StringType })
        ) { backStackEntry ->
            val assessmentId = backStackEntry.arguments?.getString("assessmentId") ?: return@composable
            CalibrationScreen(
                assessmentId = assessmentId,
                viewModel = calibrationViewModel,
                onCalibrationSaved = {
                    navController.navigate("${Destinations.MeasurementResult}/$assessmentId")
                }
            )
        }
        composable(Destinations.ManualOutline) {
            PlaceholderScreen(
                title = "Manual Outline",
                next = "Go to History",
                onNext = { navController.navigate(Destinations.History) }
            )
        }
        composable(
            route = Destinations.MeasurementResultRoute,
            arguments = listOf(navArgument("assessmentId") { type = NavType.StringType })
        ) { backStackEntry ->
            val assessmentId = backStackEntry.arguments?.getString("assessmentId") ?: return@composable
            MeasurementResultScreen(
                assessmentId = assessmentId,
                viewModel = measurementViewModel,
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
            PlaceholderScreen(
                title = "Export",
                next = "Back to Home",
                onNext = { navController.navigate(Destinations.Home) }
            )
        }
    }
}

@Composable
private fun NewAssessmentScreen(
    onCreateAssessment: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CameraViewModel = hiltViewModel()
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "New Assessment", style = MaterialTheme.typography.headlineMedium)
        Button(
            onClick = {
                viewModel.createAssessment(onCreated = onCreateAssessment)
            },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text(text = "Create Assessment & Open Camera")
        }
    }
}

@Composable
private fun PlaceholderScreen(
    title: String,
    next: String,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = title, style = MaterialTheme.typography.headlineMedium)
        Button(
            onClick = onNext,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text(text = next)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PlaceholderScreenPreview() {
    WoundCareProTheme {
        PlaceholderScreen(
            title = "Home",
            next = "Go to Patients",
            onNext = {}
        )
    }
}
