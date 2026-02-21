package com.pasindu.woundcarepro.ui.export

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pasindu.woundcarepro.data.local.dao.AssessmentDao
import com.pasindu.woundcarepro.data.local.dao.ExportAssessmentMeasurementRow
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ExportScreen(
    assessmentDao: AssessmentDao,
    onBackHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val formatter = remember { DateTimeFormatter.ISO_LOCAL_DATE }

    var startDateText by remember { mutableStateOf("") }
    var endDateText by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf("Pick a date range and export assessments + measurements") }
    var previewContent by remember { mutableStateOf("") }

    val startDate = parseDate(startDateText, formatter)
    val endDate = parseDate(endDateText, formatter)
    val isDateRangeValid = startDate != null && endDate != null && !endDate.isBefore(startDate)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Export", style = MaterialTheme.typography.headlineMedium)
        Text(text = "Date format: yyyy-MM-dd", style = MaterialTheme.typography.bodySmall)

        OutlinedTextField(
            value = startDateText,
            onValueChange = { startDateText = it },
            label = { Text("Start date") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = endDateText,
            onValueChange = { endDateText = it },
            label = { Text("End date") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                scope.launch {
                    val rows = assessmentDao.getAssessmentsWithMeasurementsForRange(
                        startEpochMillis = startDate!!.atStartOfDayEpochMillis(),
                        endEpochMillis = endDate!!.atEndOfDayEpochMillis()
                    )
                    val csv = rows.toCsv()
                    val file = writeExportFile(
                        directory = File(context.filesDir, "exports"),
                        extension = "csv",
                        content = csv
                    )
                    statusMessage = "CSV export complete: ${file.absolutePath}"
                    previewContent = csv
                }
            },
            enabled = isDateRangeValid,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Export CSV")
        }

        Button(
            onClick = {
                scope.launch {
                    val rows = assessmentDao.getAssessmentsWithMeasurementsForRange(
                        startEpochMillis = startDate!!.atStartOfDayEpochMillis(),
                        endEpochMillis = endDate!!.atEndOfDayEpochMillis()
                    )
                    val json = rows.toJson()
                    val file = writeExportFile(
                        directory = File(context.filesDir, "exports"),
                        extension = "json",
                        content = json
                    )
                    statusMessage = "JSON export complete: ${file.absolutePath}"
                    previewContent = json
                }
            },
            enabled = isDateRangeValid,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Export JSON")
        }

        Text(text = statusMessage, style = MaterialTheme.typography.bodyMedium)
        if (previewContent.isNotBlank()) {
            Text(
                text = previewContent.take(2000),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Button(onClick = onBackHome, modifier = Modifier.fillMaxWidth()) {
            Text("Back to Home")
        }
    }
}

private fun parseDate(input: String, formatter: DateTimeFormatter): LocalDate? {
    return runCatching { LocalDate.parse(input.trim(), formatter) }.getOrNull()
}

private fun LocalDate.atStartOfDayEpochMillis(): Long {
    return atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

private fun LocalDate.atEndOfDayEpochMillis(): Long {
    return plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1
}

private fun List<ExportAssessmentMeasurementRow>.toCsv(): String {
    val header = "assessmentId,assessmentCreatedAtEpochMillis,assessmentStatus,woundAreaPixels,woundAreaCm2,measuredAtEpochMillis"
    if (isEmpty()) return header

    val rows = joinToString(separator = "\n") { row ->
        listOf(
            row.assessmentId.toString(),
            row.assessmentCreatedAtEpochMillis.toString(),
            csvEscape(row.assessmentStatus),
            row.woundAreaPixels?.toString() ?: "",
            row.woundAreaCm2?.toString() ?: "",
            row.measuredAtEpochMillis?.toString() ?: ""
        ).joinToString(",")
    }
    return "$header\n$rows"
}

private fun List<ExportAssessmentMeasurementRow>.toJson(): String {
    if (isEmpty()) return "[]"

    return buildString {
        append("[\n")
        this@toJson.forEachIndexed { index, row ->
            append("  {\n")
            append("    \"assessmentId\": ${row.assessmentId},\n")
            append("    \"assessmentCreatedAtEpochMillis\": ${row.assessmentCreatedAtEpochMillis},\n")
            append("    \"assessmentStatus\": \"${jsonEscape(row.assessmentStatus)}\",\n")
            append("    \"woundAreaPixels\": ${row.woundAreaPixels?.toString() ?: "null"},\n")
            append("    \"woundAreaCm2\": ${row.woundAreaCm2?.toString() ?: "null"},\n")
            append("    \"measuredAtEpochMillis\": ${row.measuredAtEpochMillis?.toString() ?: "null"}\n")
            append("  }")
            if (index < lastIndex) append(",")
            append("\n")
        }
        append("]")
    }
}

private fun csvEscape(value: String): String {
    if (!value.contains(',') && !value.contains('"') && !value.contains('\n')) return value
    return "\"${value.replace("\"", "\"\"")}\""
}

private fun jsonEscape(value: String): String {
    return value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
}

private fun writeExportFile(directory: File, extension: String, content: String): File {
    if (!directory.exists()) {
        directory.mkdirs()
    }
    val file = File(directory, "assessment_export_${System.currentTimeMillis()}.$extension")
    file.writeText(content)
    return file
}
