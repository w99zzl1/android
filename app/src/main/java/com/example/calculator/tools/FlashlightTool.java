package com.example.calculator.tools;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;

public class FlashlightTool {
    private CameraManager cameraManager;

    public void attachContext(Context context) {
        cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
    }

    public boolean toggle(String state) {
        try {
            if (cameraManager == null) return false;
            if ("on".equalsIgnoreCase(state)) {
                cameraManager.setTorchMode("0", true);
                return true;
            } else if ("off".equalsIgnoreCase(state)) {
                cameraManager.setTorchMode("0", false);
                return true;
            }
            return false;
        } catch (CameraAccessException e) {
            return false;
        }
    }
}
