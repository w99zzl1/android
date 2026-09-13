package com.example.calculator.tools;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.util.Log;

public class FlashlightTool {
    private static final String TAG = "FlashlightTool";
    private static final String CAMERA_ID = "0";

    private final CameraManager cameraManager;

    public FlashlightTool(Context context) {
        this.cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
    }

    public boolean toggle(String state) {
        try {
            if ("on".equalsIgnoreCase(state)) {
                cameraManager.setTorchMode(CAMERA_ID, true);
                Log.d(TAG, "Flashlight turned ON");
                return true;
            } else if ("off".equalsIgnoreCase(state)) {
                cameraManager.setTorchMode(CAMERA_ID, false);
                Log.d(TAG, "Flashlight turned OFF");
                return true;
            } else {
                Log.w(TAG, "Invalid state: " + state);
                return false;
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "Error toggling flashlight", e);
            return false;
        }
    }

    public boolean isAvailable() {
        try {
            return cameraManager.getCameraIdList().length > 0;
        } catch (CameraAccessException e) {
            return false;
        }
    }
}
