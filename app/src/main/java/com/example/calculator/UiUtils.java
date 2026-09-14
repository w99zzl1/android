package com.example.calculator;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;

public final class UiUtils {

    private UiUtils() {
    }

    public static String buildErrorMessage(String label, Throwable t) {
        StringBuilder sb = new StringBuilder();
        if (label != null && !label.isEmpty()) {
            sb.append(label).append(": ");
        }
        if (t != null) {
            sb.append(t.getMessage() != null ? t.getMessage() : t.toString());
            String trace = Log.getStackTraceString(t);
            if (trace != null && !trace.isEmpty()) {
                sb.append("\n\n").append(trace);
            }
        } else {
            sb.append("Unknown error");
        }
        return sb.toString();
    }

    public static String copyToClipboard(Context context, String label, String text) {
        if (context == null || text == null) return text;
        try {
            ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText(label, text));
        } catch (Exception ignored) {
        }
        return text;
    }

    public static void showSnackbar(View anchor, String message) {
        if (anchor == null) return;
        try {
            Snackbar.make(anchor, message, Snackbar.LENGTH_LONG).show();
        } catch (Exception ignored) {
        }
    }

    public static void handleError(View anchor, Context context, String label, Throwable t) {
        String detail = buildErrorMessage(label, t);
        copyToClipboard(context, label != null ? label : "App error", detail);
        String shortMsg = t != null && t.getMessage() != null
            ? t.getMessage() : (t != null ? t.toString() : detail);
        String display = (label != null && !label.isEmpty() ? label + ": " : "") + shortMsg;
        if (anchor != null) {
            showSnackbar(anchor, display);
        } else {
            try {
                Toast.makeText(context, display, Toast.LENGTH_LONG).show();
            } catch (Exception ignored) {
            }
        }
    }

    public static void setupGlobalErrorHandler(final Context appContext) {
        final Thread.UncaughtExceptionHandler defaultHandler =
            Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            try {
                String msg = buildErrorMessage("App error", throwable);
                copyToClipboard(appContext, "App error", msg);
                String shortMsg = throwable.getMessage() != null
                    ? throwable.getMessage() : throwable.toString();
                Toast.makeText(appContext, "Error: " + shortMsg, Toast.LENGTH_LONG).show();
            } catch (Exception ignored) {
            }
            if (defaultHandler != null) {
                defaultHandler.uncaughtException(thread, throwable);
            }
        });
    }
}