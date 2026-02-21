package com.pasindu.woundcarepro.native

object NativeBridge {
    init {
        System.loadLibrary("wound_native")
    }

    external fun openCvVersion(): String
}
