package com.example.calculator;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class SettingsManager {
    private static final String PREFS_NAME = "app_settings";
    private static final String PREFS_API_KEY = "gemini_api_key";
    private static final String PREFS_SELECTED_MODEL = "selected_model";
    private static final String PREFS_CACHED_MODELS = "cached_models";
    private static final String PREFS_PROVIDER = "provider";

    private final SharedPreferences prefs;
    private final Gson gson;

    public SettingsManager(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
    }

    public void saveApiKey(String apiKey) {
        prefs.edit().putString(PREFS_API_KEY, apiKey).apply();
    }

    public String getApiKey() {
        return prefs.getString(PREFS_API_KEY, "");
    }

    public boolean hasApiKey() {
        return !getApiKey().isEmpty();
    }

    public void saveSelectedModel(String model) {
        prefs.edit().putString(PREFS_SELECTED_MODEL, model).apply();
    }

    public String getSelectedModel() {
        return prefs.getString(PREFS_SELECTED_MODEL, "gemini-1.5-flash");
    }

    public void saveModels(List<String> models) {
        String json = gson.toJson(models);
        prefs.edit().putString(PREFS_CACHED_MODELS, json).apply();
    }

    public List<String> getCachedModels() {
        String json = prefs.getString(PREFS_CACHED_MODELS, null);
        if (json == null) return new ArrayList<>();
        Type type = new TypeToken<List<String>>() {}.getType();
        return gson.fromJson(json, type);
    }

    public void saveProvider(String provider) {
        prefs.edit().putString(PREFS_PROVIDER, provider).apply();
    }

    public String getProvider() {
        return prefs.getString(PREFS_PROVIDER, "google");
    }

    public void clearApiKey() {
        prefs.edit().remove(PREFS_API_KEY).apply();
    }
}