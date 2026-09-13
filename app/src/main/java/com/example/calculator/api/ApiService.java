package com.example.calculator.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Query;

public class ApiService {
    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";
    private static final String MODEL_NAME = "gemini-2.5-flash:generateContent";

    private final GeminiApi geminiApi;
    private static final Gson gson = new GsonBuilder().create();

    public interface GeminiApi {
        @POST(MODEL_NAME)
        retrofit2.Call<GeminiResponse> generateContent(
            @Query("key") String apiKey,
            @Body GeminiRequest request
        );

        @POST(MODEL_NAME)
        retrofit2.Call<JsonElement> generateContentRaw(
            @Query("key") String apiKey,
            @Body String request
        );
    }

    public ApiService() {
        Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build();
        this.geminiApi = retrofit.create(GeminiApi.class);
    }

    public GeminiResponse callGemini(String apiKey, GeminiRequest request) throws Exception {
        return geminiApi.generateContent(apiKey, request).execute().body();
    }

    public JsonElement callGeminiRaw(String apiKey, String request) throws Exception {
        return geminiApi.generateContentRaw(apiKey, request).execute().body();
    }
}
