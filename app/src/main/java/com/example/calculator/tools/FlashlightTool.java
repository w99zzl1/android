package com.example.calculator.tools

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager

class FlashlightTool {
    private var cameraManager: CameraManager? = null

    fun attachContext(context: Context) {
        cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    fun toggle(state: String): Boolean {
        try {
            val cm = cameraManager ?: return false
            if ("on".equalsIgnoreCase(state)) {
                cm.setTorchMode("0", true)
                return true
            } else if ("off".equalsIgnoreCase(state)) {
                cm.setTorchMode("0", false)
                return true
            }
            return false
        } catch (e: CameraAccessException) {
            return false
        }
    }
}
