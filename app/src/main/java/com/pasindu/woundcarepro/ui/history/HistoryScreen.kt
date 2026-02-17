package com.pasindu.woundcarepro.ui.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pasindu.woundcarepro.data.local.Assessment
import com.pasindu.woundcarepro.data.local.AssessmentDao
import com.pasindu.woundcarepro.data.local.PatientAreaTrendPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    assessmentDao: AssessmentDao,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    var assessments by remember { mutableStateOf<List<Assessment>>(emptyList()) }
    var selectedPatientId by remember { mutableStateOf<String?>(null) }
    var trendPoints by remember { mutableStateOf<List<PatientAreaTrendPoint>>(emptyList()) }

    LaunchedEffect(Unit) {
        assessments = assessmentDao.getMeasuredAssessments()
        selectedPatientId = assessments.firstOrNull()?.patientId
    }

    LaunchedEffect(selectedPatientId) {
        trendPoints = selectedPatientId?.let { assessmentDao.getTrendPointsForPatient(it) }.orEmpty()
    }

    val patients = assessments.map { it.patientId }.distinct()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("History", style = MaterialTheme.typography.headlineMedium)

        if (patients.isEmpty()) {
            Text("No measured assessments yet.")
        } else {
            Text("Patients", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                patients.forEach { patientId ->
                    val isSelected = patientId == selectedPatientId
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable { selectedPatientId = patientId }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = patientId,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Text(
                text = "Trend: Wound area (cm²) over time",
                style = MaterialTheme.typography.titleMedium
            )
            TrendChart(points = trendPoints, modifier = Modifier
                .fillMaxWidth()
                .height(220.dp))

            Text("Assessments", style = MaterialTheme.typography.titleMedium)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(assessments.filter { it.patientId == selectedPatientId }) { assessment ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Assessment #${assessment.id}", fontWeight = FontWeight.Bold)
                            Text("Date: ${assessment.createdAtEpochMillis.toReadableDate()}")
                            Text("Area: %.4f cm²".format(assessment.woundAreaCm2 ?: 0.0))
                            Text("Status: ${assessment.status}")
                        }
                    }
                }
            }
        }

        Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
            Text("Go to Export")
        }
    }
}

@Composable
private fun TrendChart(
    points: List<PatientAreaTrendPoint>,
    modifier: Modifier = Modifier
) {
    if (points.size < 2) {
        Box(modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))) {
            Text(
                text = "Need at least 2 measured assessments to draw a trend line.",
                modifier = Modifier.padding(12.dp)
            )
        }
        return
    }

    val minY = points.minOf { it.woundAreaCm2 }
    val maxY = points.maxOf { it.woundAreaCm2 }
    val yRange = (maxY - minY).takeIf { it > 0.0 } ?: 1.0

    Canvas(modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)).padding(12.dp)) {
        val stepX = if (points.size > 1) size.width / (points.size - 1) else size.width
        val mapped = points.mapIndexed { index, point ->
            val normalizedY = ((point.woundAreaCm2 - minY) / yRange).toFloat()
            Offset(
                x = stepX * index,
                y = size.height - (normalizedY * size.height)
            )
        }

        for (i in 0 until mapped.lastIndex) {
            drawLine(
                color = Color(0xFF1565C0),
                start = mapped[i],
                end = mapped[i + 1],
                strokeWidth = 5f
            )
        }

        mapped.forEach { point ->
            drawCircle(color = Color(0xFFD32F2F), radius = 8f, center = point)
        }

        drawRect(
            color = Color.Gray,
            style = Stroke(width = 2f)
        )
    }
}

private fun Long.toReadableDate(): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return formatter.format(Date(this))
}
