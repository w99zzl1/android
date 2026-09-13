package com.example.calculator.api;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class GeminiResponse {
    @SerializedName("candidates")
    public List<Candidate> candidates;
    @SerializedName("promptFeedback")
    public PromptFeedback promptFeedback;

    public static class Candidate {
        @SerializedName("content")
        public Content content;

        public Candidate(Content content) {
            this.content = content;
        }
    }

    public static class Content {
        @SerializedName("role")
        public String role;
        @SerializedName("parts")
        public List<Part> parts;

        public Content(String role, List<Part> parts) {
            this.role = role;
            this.parts = parts;
        }
    }

    public static class Part {
        @SerializedName("text")
        public String text;
        @SerializedName("functionCall")
        public FunctionCall functionCall;

        public Part(String text) {
            this.text = text;
        }
    }

    public static class FunctionCall {
        @SerializedName("name")
        public String name;
        @SerializedName("args")
        public java.util.Map<String, Object> args;

        public FunctionCall(String name, java.util.Map<String, Object> args) {
            this.name = name;
            this.args = args;
        }
    }

    public static class PromptFeedback {
        @SerializedName("blockReason")
        public String blockReason;
    }
}

class FunctionResponse {
    @SerializedName("name")
    public String name;
    @SerializedName("response")
    public ResponseData response;

    public static class ResponseData {
        @SerializedName("content")
        public String content;
    }
}
