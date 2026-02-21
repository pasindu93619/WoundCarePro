package com.pasindu.woundcarepro.nativebridge

object NativeBridge {
    init {
        System.loadLibrary("wound_native")
    }

    external fun openCvVersion(): String
}
