package com.example.calculator.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.List;
import java.util.Map;

public class GeminiManager {
    private static final String API_KEY = "YOUR_API_KEY_HERE";
    private final ApiService apiService;

    public GeminiManager() {
        this.apiService = new ApiService();
    }

    public String processMessage(String userMessage, List<Map<String, Object>> tools) throws Exception {
        String requestJson = buildRequestJson(userMessage, tools);
        String responseJson = apiService.callGeminiRaw(API_KEY, requestJson);

        JsonObject responseObj = JsonParser.parseString(responseJson).getAsJsonObject();
        JsonArray candidates = responseObj.getAsJsonArray("candidates");
        if (candidates == null || candidates.size() == 0) {
            return "No response from Gemini.";
        }

        JsonObject candidate = candidates.get(0).getAsJsonObject();
        JsonObject content = candidate.getAsJsonObject("content");
        JsonArray parts = content.getAsJsonArray("parts");
        if (parts == null || parts.size() == 0) {
            return "Invalid response structure.";
        }

        JsonObject part = parts.get(0).getAsJsonObject();

        // Check for function call
        if (part.has("functionCall")) {
            JsonObject fc = part.getAsJsonObject("functionCall");
            String name = fc.get("name").getAsString();
            JsonObject args = fc.getAsJsonObject("args");
            return "FUNCTION_CALL:" + name + "|" + args.toString();
        }

        // Return text response
        if (part.has("text")) {
            return part.get("text").getAsString();
        }

        return "No text or function call found in response.";
    }

    private String buildRequestJson(String userMessage, List<Map<String, Object>> tools) {
        JsonObject request = new JsonObject();

        // Build contents array
        JsonArray contents = new JsonArray();
        JsonObject contentObj = new JsonObject();
        contentObj.addProperty("role", "user");
        JsonArray parts = new JsonArray();
        JsonObject partObj = new JsonObject();
        partObj.addProperty("text", userMessage);
        parts.add(partObj);
        contentObj.add("parts", parts);
        contents.add(contentObj);
        request.add("contents", contents);

        // Build tools array
        JsonArray toolsArray = new JsonArray();
        for (Map<String, Object> tool : tools) {
            JsonObject toolObj = new JsonObject();
            JsonArray funcDecls = new JsonArray();
            JsonObject funcDecl = new JsonObject();
            funcDecl.addProperty("name", tool.get("name").toString());
            funcDecl.addProperty("description", tool.get("description").toString());

            // Build parameters
            JsonObject params = new JsonObject();
            Map<String, Object> paramMap = (Map<String, Object>) tool.get("parameters");
            if (paramMap != null) {
                params.addProperty("type", paramMap.get("type").toString());
                JsonObject properties = new JsonObject();
                Map<String, Object> propDef = (Map<String, Object>) paramMap.get("properties");
                if (propDef != null) {
                    for (Map.Entry<String, Object> entry : propDef.entrySet()) {
                        Map<String, Object> propVal = (Map<String, Object>) entry.getValue();
                        JsonObject propObj = new JsonObject();
                        propObj.addProperty("type", propVal.get("type").toString());
                        propObj.addProperty("description", propVal.get("description").toString());
                        properties.add(entry.getKey(), propObj);
                    }
                }
                params.add("properties", properties);
                JsonArray required = new JsonArray();
                List<String> reqList = (List<String>) paramMap.get("required");
                if (reqList != null) {
                    for (String s : reqList) {
                        required.add(s);
                    }
                }
                params.add("required", required);
            }
            funcDecl.add("parameters", params);
            funcDecls.add(funcDecl);
            toolObj.add("functionDeclarations", funcDecls);
            toolsArray.add(toolObj);
        }
        request.add("tools", toolsArray);

        return request.toString();
    }
}
