package com.example.calculator.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;
import retrofit2.http.Url;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ApiService {
    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta/";
    private static final String MODEL_NAME = "gemini-1.5-flash";

    private final GeminiApi geminiApi;
    private final ModelsApi modelsApi;
    private static final com.google.gson.Gson gson = new com.google.gson.Gson();

    public ApiService() {
        OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();
        Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build();
        this.geminiApi = retrofit.create(GeminiApi.class);
        this.modelsApi = retrofit.create(ModelsApi.class);
    }

    public interface GeminiApi {
        @POST("models/{model}:generateContent")
        Call<JsonElement> generateContent(
            @Header("x-goog-api-key") String apiKey,
            @retrofit2.http.Path("model") String model,
            @Body JsonObject request
        );
    }

    public interface ModelsApi {
        @GET("models")
        Call<JsonElement> listModels(
            @Header("x-goog-api-key") String apiKey
        );
    }

    public JsonElement generateContent(String apiKey, String model, JsonObject request) throws Exception {
        Response<JsonElement> resp = geminiApi.generateContent(apiKey, model, request).execute();
        if (!resp.isSuccessful()) {
            throw new Exception("Gemini API error " + resp.code() + ": " + readErrorBody(resp));
        }
        JsonElement body = resp.body();
        if (body == null) {
            throw new Exception("Gemini API returned empty body (HTTP " + resp.code() + ")");
        }
        return body;
    }

    private String readErrorBody(Response<?> resp) {
        if (resp.errorBody() == null) return "no details";
        try {
            String s = resp.errorBody().string();
            if (s.length() > 400) s = s.substring(0, 400);
            return s;
        } catch (Exception e) {
            return "could not read error body";
        }
    }

    public List<String> listModels(String apiKey) throws Exception {
        Response<JsonElement> resp = modelsApi.listModels(apiKey).execute();
        if (!resp.isSuccessful()) {
            throw new Exception("Gemini API error " + resp.code() + ": " + readErrorBody(resp));
        }
        JsonElement response = resp.body();
        if (response == null || !response.isJsonObject()) return java.util.Collections.emptyList();
        
        JsonObject obj = response.getAsJsonObject();
        if (!obj.has("models")) return java.util.Collections.emptyList();
        
        JsonArray models = obj.getAsJsonArray("models");
        java.util.List<String> modelNames = new java.util.ArrayList<>();
        for (JsonElement model : models) {
            if (model.isJsonObject()) {
                JsonObject modelObj = model.getAsJsonObject();
                if (modelObj.has("name")) {
                    String name = modelObj.get("name").getAsString();
                    // Extract model name from "models/gemini-1.5-flash"
                    if (name.startsWith("models/")) {
                        name = name.substring("models/".length());
                    }
                    modelNames.add(name);
                }
            }
        }
        return modelNames;
    }

    public static JsonObject buildRequest(String userMessage, List<Map<String, Object>> tools) {
        JsonObject request = new JsonObject();

        JsonArray contents = new JsonArray();
        JsonObject contentObj = new JsonObject();
        contentObj.addProperty("role", "user");
        JsonArray parts = new JsonArray();
        JsonObject partObj = new JsonObject();
        partObj.addProperty("text", "You are a helpful AI assistant with access to tools. " +
            "When the user asks you to do something that requires a tool, " +
            "call the appropriate tool with the correct arguments and do not guess extra fields. " +
            "After the tool result is provided, answer to the user naturally.\n\nUser: " + userMessage);
        parts.add(partObj);
        contentObj.add("parts", parts);
        contents.add(contentObj);

        JsonArray funcDecls = new JsonArray();
        if (tools != null) {
            for (Map<String, Object> tool : tools) {
                if (tool == null) continue;
                JsonObject decl = new JsonObject();
                Object name = tool.get("name");
                if (name != null) decl.addProperty("name", String.valueOf(name));
                Object description = tool.get("description");
                if (description != null) decl.addProperty("description", String.valueOf(description));
                Object params = tool.get("parameters");
                if (params instanceof Map) {
                    decl.add("parameters", gson.toJsonTree(params));
                }
                funcDecls.add(decl);
            }
        }

        JsonObject toolObj = new JsonObject();
        toolObj.add("functionDeclarations", funcDecls);
        JsonArray toolsArray = new JsonArray();
        toolsArray.add(toolObj);

        request.add("contents", contents);
        request.add("tools", toolsArray);

        return request;
    }

    public static String extractResponse(JsonElement response) {
        if (response == null || !response.isJsonObject()) return "No response from AI.";
        
        JsonObject obj = response.getAsJsonObject();

        if (obj.has("error") && obj.get("error").isJsonObject()) {
            JsonObject err = obj.get("error").getAsJsonObject();
            String msg = err.has("message") ? err.get("message").getAsString() : "unknown";
            String code = err.has("code") ? String.valueOf(err.get("code").getAsInt()) : "?";
            return "API error " + code + ": " + msg;
        }

        if (!obj.has("candidates")) return "No response from AI.";
        
        com.google.gson.JsonArray candidates = obj.getAsJsonArray("candidates");
        if (candidates.size() == 0) return "No response from AI.";
        
        JsonObject candidate = candidates.get(0).getAsJsonObject();

        if (candidate.has("finishReason")
                && !"STOP".equals(candidate.get("finishReason").getAsString())
                && !candidate.has("content")) {
            return "AI stopped early: " + candidate.get("finishReason").getAsString()
                    + ". Try rewording the request.";
        }

        if (!candidate.has("content")) return "No response from AI.";        
        JsonObject content = candidate.getAsJsonObject("content");
        if (!content.has("parts")) return "No response from AI.";
        
        com.google.gson.JsonArray parts = content.getAsJsonArray("parts");
        if (parts.size() == 0) return "No response from AI.";

        StringBuilder text = new StringBuilder();
        for (JsonElement partEl : parts) {
            if (!partEl.isJsonObject()) continue;
            JsonObject part = partEl.getAsJsonObject();

            // Check for function call
            if (part.has("functionCall")) {
                JsonObject fc = part.getAsJsonObject("functionCall");
                String name = fc.get("name").getAsString();
                JsonObject args = fc.getAsJsonObject("args");
                return "FUNCTION_CALL:" + name + "|" + args.toString();
            }

            if (part.has("text")) {
                if (text.length() > 0) text.append('\n');
                text.append(part.get("text").getAsString());
            }
        }

        return text.length() > 0 ? text.toString() : "No response from AI.";
    }
}