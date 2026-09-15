package com.example.calculator;

import android.provider.AlarmClock;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.google.gson.JsonObject;

import java.util.Locale;

public class DeviceTools {
    private final Context context;

    public DeviceTools(Context context) {
        this.context = context;
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

        if (url.contains("youtube.com") || url.contains("youtu.be")) {
            if (url.contains("/results") || url.contains("search_query")) {
                return "I do not open YouTube search pages. Use PLAY_YOUTUBE to find real videos, " +
                       "then play the chosen one with OPEN_YOUTUBE_VIDEO.";
            }
        }

        Intent browser = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        browser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(browser);
        return "Opened URL: " + url;
    }

    public String openYouTubeVideo(JsonObject args) {
        String videoId = args.has("videoId") ? args.get("videoId").getAsString().trim() : "";
        if (!videoId.matches("[A-Za-z0-9_-]{11}")) {
            return "Invalid videoId: '" + videoId + "'. Please use a videoId from the PLAY_YOUTUBE results, never invent one.";
        }
        context.startActivity(new Intent(Intent.ACTION_VIEW,
            Uri.parse("https://www.youtube.com/watch?v=" + videoId)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        return "Playing YouTube video: " + videoId;
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
}