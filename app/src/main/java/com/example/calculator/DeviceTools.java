package com.example.calculator;

import android.app.AlarmClock;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.provider.Settings;

import com.google.gson.JsonObject;

import java.util.Locale;

public class DeviceTools {
    private final Context context;
    private final AudioManager audioManager;

    public DeviceTools(Context context) {
        this.context = context;
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    public String openAppUrl(JsonObject args) {
        String target = args.has("target") ? args.get("target").getAsString().trim() : "";
        if (target.isEmpty()) return "No target provided";

        if (target.startsWith("app:")) {
            String pkg = target.substring(4).trim();
            Intent launch = context.getPackageManager().getLaunchIntentForPackage(pkg);
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(launch);
                return "Opened app: " + pkg;
            }
            return "App not installed: " + pkg + ". Ask the user to install it.";
        }

        String url = target.contains("://") ? target : "https://" + target;
        if (!url.matches("^https?://.+")) return "Not a valid URL: " + target;
        Intent browser = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        browser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(browser);
        return "Opened URL: " + url;
    }

    public String setAlarmTimer(JsonObject args) {
        try {
            String type = args.has("type") ? args.get("type").getAsString() : "timer";

            if ("alarm".equalsIgnoreCase(type)) {
                int hour = args.has("hour") ? args.get("hour").getAsInt() : 0;
                hour = Math.max(0, Math.min(23, hour));
                int minute = args.has("minute") ? args.get("minute").getAsInt() : 0;
                minute = Math.max(0, Math.min(59, minute));
                Intent i = new Intent(AlarmClock.ACTION_SET_ALARM)
                    .putExtra(AlarmClock.EXTRA_HOUR, hour)
                    .putExtra(AlarmClock.EXTRA_MINUTES, minute);
                if (args.has("label") && !args.get("label").getAsString().isEmpty()) {
                    i.putExtra(AlarmClock.EXTRA_MESSAGE, args.get("label").getAsString());
                }
                context.startActivity(i);
                return String.format(Locale.ROOT, "Opened alarm setter for %02d:%02d", hour, minute);
            }

            int seconds = args.has("seconds") ? args.get("seconds").getAsInt() : 0;
            if (seconds <= 0 || seconds > 24 * 3600) {
                return "Timer requires seconds between 1 and 86400";
            }
            Intent t = new Intent(AlarmClock.ACTION_SET_TIMER)
                .putExtra(AlarmClock.EXTRA_LENGTH, seconds);
            if (args.has("label") && !args.get("label").getAsString().isEmpty()) {
                t.putExtra(AlarmClock.EXTRA_MESSAGE, args.get("label").getAsString());
            }
            context.startActivity(t);
            return "Opened timer for " + seconds + " seconds";
        } catch (Exception e) {
            return "Could not open alarm/timer: " + e.getMessage();
        }
    }

    public String setVolume(JsonObject args) {
        String streamName = args.has("stream") ? args.get("stream").getAsString().toLowerCase(Locale.ROOT) : "media";
        int percent = args.has("volume") ? args.get("volume").getAsInt() : 0;
        percent = Math.max(0, Math.min(100, percent));

        int stream;
        switch (streamName) {
            case "ring":
                stream = AudioManager.STREAM_RING;
                break;
            case "alarm":
                stream = AudioManager.STREAM_ALARM;
                break;
            case "notification":
                stream = AudioManager.STREAM_NOTIFICATION;
                break;
            case "call":
                stream = AudioManager.STREAM_VOICE_CALL;
                break;
            default:
                stream = AudioManager.STREAM_MUSIC;
                streamName = "media";
        }

        int max = audioManager.getStreamMaxVolume(stream);
        if (max <= 0) return "Cannot adjust volume stream: " + streamName;
        int level = Math.round(max * percent / 100f);
        audioManager.setStreamVolume(stream, level, 0);
        return "Set " + streamName + " volume to " + percent + "%";
    }

    public String setBrightness(JsonObject args) {
        int percent = args.has("percent") ? args.get("percent").getAsInt() : 50;
        percent = Math.max(0, Math.min(100, percent));

        if (!Settings.System.canWrite(context)) {
            return "Setting brightness requires the special permission. " +
                "Open Settings in the app, tap 'Allow brightness control' and enable it, then try again.";
        }

        int brightness = Math.round(255 * percent / 100f);
        Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, brightness);
        Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE,
            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
        return "Set screen brightness to " + percent + "%";
    }
}