package com.example.calculator;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LogManager {
    private static final int MAX_ENTRIES = 400;
    private static final List<String> entries = new ArrayList<>();
    private static final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.ROOT);

    private LogManager() {
    }

    public static void log(String tag, String message) {
        String line = timeFormat.format(new Date()) + " [" + tag + "] " + message;
        synchronized (entries) {
            entries.add(line);
            if (entries.size() > MAX_ENTRIES) {
                entries.remove(0);
            }
        }
    }

    public static List<String> getEntries() {
        synchronized (entries) {
            return new ArrayList<>(entries);
        }
    }

    public static String toText() {
        synchronized (entries) {
            return String.join("\n", entries);
        }
    }

    public static void clear() {
        synchronized (entries) {
            entries.clear();
        }
    }

    public static String clip(String text, int maxLen) {
        if (text == null) return "null";
        if (text.length() <= maxLen) return text;
        return text.substring(0, maxLen) + "...";
    }
}