package com.pasindu.woundcarepro.ui.review

import android.graphics.PointF
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntSize

fun mapCanvasTapToImagePoint(
    tap: Offset,
    canvasSize: IntSize,
    imageWidth: Float,
    imageHeight: Float
): Offset? {
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

    return Offset(normalizedX * imageWidth, normalizedY * imageHeight)
}

fun Offset.toPointF(): android.graphics.PointF = android.graphics.PointF(x, y)

fun mapImagePointToCanvasOffset(
    point: PointF,
    canvasSize: Size,
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
    return Offset(x, y)
}
