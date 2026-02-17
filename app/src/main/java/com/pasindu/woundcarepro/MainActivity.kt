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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pasindu.woundcarepro.ui.theme.WoundCareProTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WoundCareProTheme {
                val navController = rememberNavController()
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    WoundCareNavGraph(
                        navController = navController,
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
    const val Review = "review"
    const val ManualOutline = "manual_outline"
    const val MeasurementResult = "measurement_result"
    const val History = "history"
    const val Export = "export"
}

@Composable
private fun WoundCareNavGraph(
    navController: NavHostController,
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
            PlaceholderScreen(
                title = "New Assessment",
                next = "Go to Camera Capture",
                onNext = { navController.navigate(Destinations.CameraCapture) }
            )
        }
        composable(Destinations.CameraCapture) {
            PlaceholderScreen(
                title = "Camera Capture",
                next = "Go to Review",
                onNext = { navController.navigate(Destinations.Review) }
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
