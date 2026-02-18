package com.pasindu.woundcarepro.ui.review

import android.graphics.BitmapFactory
<<<<<<< HEAD
=======
import android.graphics.PointF
>>>>>>> main
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
<<<<<<< HEAD
import com.pasindu.woundcarepro.measurement.OutlinePoint
=======
>>>>>>> main

@Composable
fun ReviewScreen(
    assessmentId: String,
    viewModel: ReviewViewModel,
    onRetake: () -> Unit,
    onAccept: () -> Unit,
    modifier: Modifier = Modifier
) {
<<<<<<< HEAD
=======
    val assessment by viewModel.assessment.collectAsState()
>>>>>>> main
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(assessmentId) {
        viewModel.loadAssessment(assessmentId)
    }

    val imagePath = uiState.assessment?.imagePath
    val bitmap = imagePath?.let { path -> BitmapFactory.decodeFile(path)?.asImageBitmap() }
    val statusMessage = if (imagePath == null) {
        "No captured image found. Please retake."
    } else {
<<<<<<< HEAD
        "Tap to mark wound border points"
=======
        "Tap on wound border to add polygon points"
    }

    val bitmap = assessment?.imagePath?.let { path ->
        BitmapFactory.decodeFile(path)?.asImageBitmap()
>>>>>>> main
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Review", style = MaterialTheme.typography.headlineMedium)
        Text(text = statusMessage, style = MaterialTheme.typography.bodyMedium)

        if (bitmap != null) {
<<<<<<< HEAD
            var containerSize = IntSize.Zero
=======
            var canvasSize by remember { mutableStateOf(IntSize.Zero) }
            val imageWidth = bitmap.width.toFloat()
            val imageHeight = bitmap.height.toFloat()
>>>>>>> main

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
<<<<<<< HEAD
                    .heightIn(min = 260.dp)
                    .background(Color.Black)
                    .onSizeChanged { containerSize = it }
                    .pointerInput(bitmap, containerSize) {
                        detectTapGestures { tapOffset ->
                            val mapped = mapTapToImagePoint(
                                tap = tapOffset,
                                containerSize = containerSize,
                                imageWidth = bitmap.width,
                                imageHeight = bitmap.height
                            )
                            if (mapped != null) {
                                viewModel.addPoint(mapped)
                            }
=======
                    .background(Color.Black)
                    .onSizeChanged { canvasSize = it }
                    .pointerInput(bitmap, uiState.points) {
                        detectTapGestures { tapOffset ->
                            val mapped = mapCanvasTapToImagePoint(
                                tap = tapOffset,
                                canvasSize = canvasSize,
                                imageWidth = imageWidth,
                                imageHeight = imageHeight
                            ) ?: return@detectTapGestures
                            viewModel.addPoint(mapped)
>>>>>>> main
                        }
                    }
            ) {
                Image(
                    bitmap = bitmap,
                    contentDescription = "Captured wound photo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
<<<<<<< HEAD

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val displayPoints = uiState.points.mapNotNull { point ->
                        mapImagePointToCanvasOffset(
                            point = point,
                            canvasWidth = size.width,
                            canvasHeight = size.height,
                            imageWidth = bitmap.width.toFloat(),
                            imageHeight = bitmap.height.toFloat()
                        )
                    }

                    displayPoints.forEachIndexed { index, point ->
                        drawCircle(
                            color = Color.Yellow,
                            radius = 8f,
                            center = point
                        )
                        if (index > 0) {
                            drawLine(
                                color = Color.Red,
                                start = displayPoints[index - 1],
                                end = point,
                                strokeWidth = 4f
                            )
                        }
                    }
                    if (uiState.pixelArea != null && displayPoints.size >= 3) {
                        drawLine(
                            color = Color.Red,
                            start = displayPoints.last(),
                            end = displayPoints.first(),
                            strokeWidth = 4f
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.undoLastPoint() },
                enabled = uiState.points.isNotEmpty(),
                modifier = Modifier.weight(1f)
            ) {
                Text("Undo")
            }
            Button(
                onClick = { viewModel.clearPoints() },
                enabled = uiState.points.isNotEmpty(),
                modifier = Modifier.weight(1f)
            ) {
                Text("Clear")
            }
        }

        Button(
            onClick = {
                viewModel.saveOutline(assessmentId = assessmentId)
            },
            enabled = uiState.points.size >= 3 && uiState.assessment?.imagePath != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Outline")
        }

        uiState.pixelArea?.let { area ->
            Text(
                text = "Saved pixel area: %.2f px²".format(area),
                style = MaterialTheme.typography.bodyLarge
            )
        }


        Button(
            onClick = onAccept,
            enabled = uiState.pixelArea != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Continue")
        }

        Button(
            onClick = onRetake,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Retake")
=======

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val mappedPoints = uiState.points.mapNotNull { point ->
                        mapImagePointToCanvasOffset(
                            point = point,
                            canvasSize = size,
                            imageWidth = imageWidth,
                            imageHeight = imageHeight
                        )
                    }

                    if (mappedPoints.size >= 2) {
                        for (i in 0 until mappedPoints.lastIndex) {
                            drawLine(
                                color = Color.Red,
                                start = mappedPoints[i],
                                end = mappedPoints[i + 1],
                                strokeWidth = 4f
                            )
                        }
                    }

                    if (mappedPoints.size >= 3) {
                        drawLine(
                            color = Color.Red.copy(alpha = 0.7f),
                            start = mappedPoints.last(),
                            end = mappedPoints.first(),
                            strokeWidth = 3f
                        )
                    }

                    mappedPoints.forEach { point ->
                        drawCircle(color = Color.Yellow, radius = 8f, center = point)
                    }
                }
            }
        }

        Text(
            text = "Saved pixel area: ${uiState.pixelArea?.let { String.format("%.2f", it) } ?: "-"}",
            style = MaterialTheme.typography.bodyMedium
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = { viewModel.undoLastPoint() }, modifier = Modifier.weight(1f)) {
                Text("Undo")
            }
            Button(onClick = { viewModel.clearPoints() }, modifier = Modifier.weight(1f)) {
                Text("Clear")
            }
            Button(
                onClick = { viewModel.saveOutline(assessmentId) },
                enabled = uiState.points.size >= 3,
                modifier = Modifier.weight(1f)
            ) {
                Text("Save Outline")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onRetake,
                modifier = Modifier.weight(1f)
            ) {
                Text("Retake")
            }

            Button(
                onClick = onAccept,
                enabled = uiState.pixelArea != null,
                modifier = Modifier.weight(1f)
            ) {
                Text("Continue")
            }
>>>>>>> main
        }
    }
}

<<<<<<< HEAD
private fun mapTapToImagePoint(
    tap: Offset,
    containerSize: IntSize,
    imageWidth: Int,
    imageHeight: Int
): OutlinePoint? {
    if (containerSize.width == 0 || containerSize.height == 0) return null

    val containerWidth = containerSize.width.toFloat()
    val containerHeight = containerSize.height.toFloat()
    val imageAspect = imageWidth.toFloat() / imageHeight.toFloat()
    val containerAspect = containerWidth / containerHeight

    val displayedWidth: Float
    val displayedHeight: Float
    val offsetX: Float
    val offsetY: Float

    if (imageAspect > containerAspect) {
        displayedWidth = containerWidth
        displayedHeight = displayedWidth / imageAspect
        offsetX = 0f
        offsetY = (containerHeight - displayedHeight) / 2f
    } else {
        displayedHeight = containerHeight
        displayedWidth = displayedHeight * imageAspect
        offsetX = (containerWidth - displayedWidth) / 2f
        offsetY = 0f
    }

    if (tap.x !in offsetX..(offsetX + displayedWidth) || tap.y !in offsetY..(offsetY + displayedHeight)) {
        return null
    }

    val normalizedX = (tap.x - offsetX) / displayedWidth
    val normalizedY = (tap.y - offsetY) / displayedHeight

    return OutlinePoint(
        x = normalizedX * imageWidth,
        y = normalizedY * imageHeight
    )
}

private fun mapImagePointToCanvasOffset(
    point: OutlinePoint,
    canvasWidth: Float,
    canvasHeight: Float,
    imageWidth: Float,
    imageHeight: Float
): Offset? {
    if (canvasWidth == 0f || canvasHeight == 0f || imageWidth == 0f || imageHeight == 0f) return null

    val imageAspect = imageWidth / imageHeight
    val canvasAspect = canvasWidth / canvasHeight

    val displayedWidth: Float
    val displayedHeight: Float
    val offsetX: Float
    val offsetY: Float

    if (imageAspect > canvasAspect) {
        displayedWidth = canvasWidth
        displayedHeight = displayedWidth / imageAspect
        offsetX = 0f
        offsetY = (canvasHeight - displayedHeight) / 2f
    } else {
        displayedHeight = canvasHeight
        displayedWidth = displayedHeight * imageAspect
        offsetX = (canvasWidth - displayedWidth) / 2f
        offsetY = 0f
    }

    val x = offsetX + (point.x / imageWidth) * displayedWidth
    val y = offsetY + (point.y / imageHeight) * displayedHeight

=======
private fun mapCanvasTapToImagePoint(
    tap: Offset,
    canvasSize: IntSize,
    imageWidth: Float,
    imageHeight: Float
): PointF? {
    if (canvasSize.width == 0 || canvasSize.height == 0) return null

    val canvasW = canvasSize.width.toFloat()
    val canvasH = canvasSize.height.toFloat()
    val imageAspect = imageWidth / imageHeight
    val canvasAspect = canvasW / canvasH

    val drawnW: Float
    val drawnH: Float
    val left: Float
    val top: Float

    if (imageAspect > canvasAspect) {
        drawnW = canvasW
        drawnH = canvasW / imageAspect
        left = 0f
        top = (canvasH - drawnH) / 2f
    } else {
        drawnH = canvasH
        drawnW = canvasH * imageAspect
        top = 0f
        left = (canvasW - drawnW) / 2f
    }

    if (tap.x < left || tap.x > left + drawnW || tap.y < top || tap.y > top + drawnH) return null

    val normalizedX = (tap.x - left) / drawnW
    val normalizedY = (tap.y - top) / drawnH

    return PointF(normalizedX * imageWidth, normalizedY * imageHeight)
}

private fun mapImagePointToCanvasOffset(
    point: PointF,
    canvasSize: androidx.compose.ui.geometry.Size,
    imageWidth: Float,
    imageHeight: Float
): Offset? {
    if (canvasSize.width == 0f || canvasSize.height == 0f) return null

    val imageAspect = imageWidth / imageHeight
    val canvasAspect = canvasSize.width / canvasSize.height

    val drawnW: Float
    val drawnH: Float
    val left: Float
    val top: Float

    if (imageAspect > canvasAspect) {
        drawnW = canvasSize.width
        drawnH = canvasSize.width / imageAspect
        left = 0f
        top = (canvasSize.height - drawnH) / 2f
    } else {
        drawnH = canvasSize.height
        drawnW = canvasSize.height * imageAspect
        top = 0f
        left = (canvasSize.width - drawnW) / 2f
    }

    val x = left + (point.x / imageWidth) * drawnW
    val y = top + (point.y / imageHeight) * drawnH
>>>>>>> main
    return Offset(x, y)
}
