package com.pasindu.woundcarepro.domain.optical

data class StereoFramePair(
    val timestampNs: Long,
    val mainYuv: ByteArray,
    val ultraYuv: ByteArray,
    val width: Int,
    val height: Int
)
