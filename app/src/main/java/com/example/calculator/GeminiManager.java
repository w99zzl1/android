package com.example.calculator;

import android.util.Log;

import com.example.calculator.api.ApiService;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.List;
import java.util.Map;

public class GeminiManager {
    private static final String TAG = "GeminiManager";
    private final ApiService apiService;
    private final SettingsManager settingsManager;

    public GeminiManager(Context context) {
        this.apiService = new ApiService();
        this.settingsManager = new SettingsManager(context);
    }

    public String processMessage(String userMessage, List<Map<String, Object>> tools) throws Exception {
        if (settingsManager.getApiKey().isEmpty()) {
            throw new Exception("API key not set");
        }

        String model = settingsManager.getSelectedModel();
        if (model == null || model.isEmpty()) {
            model = "gemini-1.5-flash";
        }

        JsonObject request = ApiService.buildRequest(userMessage, tools);
        
        JsonElement response = apiService.generateContent(settingsManager.getApiKey(), model, request);
        
        return ApiService.extractResponse(response);
    }
}