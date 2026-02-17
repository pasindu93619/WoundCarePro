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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pasindu.woundcarepro.data.local.Assessment
import com.pasindu.woundcarepro.data.local.DatabaseProvider
import com.pasindu.woundcarepro.ui.camera.CameraCaptureScreen
import com.pasindu.woundcarepro.ui.theme.WoundCareProTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WoundCareProTheme {
                val navController = rememberNavController()
                val assessmentDao = remember { DatabaseProvider.getDatabase(applicationContext).assessmentDao() }
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    WoundCareNavGraph(
                        navController = navController,
                        assessmentDao = assessmentDao,
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
    const val ManualOutline = "manual_outline"
    const val MeasurementResult = "measurement_result"
    const val History = "history"
    const val Export = "export"
}

@Composable
private fun WoundCareNavGraph(
    navController: NavHostController,
    assessmentDao: com.pasindu.woundcarepro.data.local.AssessmentDao,
    modifier: Modifier = Modifier
) {
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
                onCreateAssessment = {
                    navController.navigate("${Destinations.CameraCapture}/$it")
                },
                assessmentDao = assessmentDao
            )
        }
        composable(
            route = Destinations.CameraCaptureRoute,
            arguments = listOf(navArgument("assessmentId") { type = NavType.LongType })
        ) { backStackEntry ->
            val assessmentId = backStackEntry.arguments?.getLong("assessmentId") ?: return@composable
            CameraCaptureScreen(
                assessmentId = assessmentId,
                assessmentDao = assessmentDao
            )
        }
        composable(Destinations.Review) {
            PlaceholderScreen(
                title = "Review",
                next = "Go to Manual Outline",
                onNext = { navController.navigate(Destinations.ManualOutline) }
            )
        }
        composable(Destinations.ManualOutline) {
            PlaceholderScreen(
                title = "Manual Outline",
                next = "Go to Measurement Result",
                onNext = { navController.navigate(Destinations.MeasurementResult) }
            )
        }
        composable(Destinations.MeasurementResult) {
            PlaceholderScreen(
                title = "Measurement Result",
                next = "Go to History",
                onNext = { navController.navigate(Destinations.History) }
            )
        }
        composable(Destinations.History) {
            PlaceholderScreen(
                title = "History",
                next = "Go to Export",
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
    onCreateAssessment: (Long) -> Unit,
    assessmentDao: com.pasindu.woundcarepro.data.local.AssessmentDao,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var latestAssessmentId by remember { mutableLongStateOf(0L) }

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
                scope.launch {
                    val assessmentId = assessmentDao.insertAssessment(Assessment())
                    latestAssessmentId = assessmentId
                    onCreateAssessment(assessmentId)
                }
            },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text(text = "Create Assessment & Open Camera")
        }
        if (latestAssessmentId > 0L) {
            Text(
                text = "Created assessment #$latestAssessmentId",
                modifier = Modifier.padding(top = 12.dp)
            )
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
