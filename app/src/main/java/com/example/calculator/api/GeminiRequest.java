package com.example.calculator.api;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class GeminiRequest {
    @SerializedName("contents")
    public List<Content> contents;
    @SerializedName("tools")
    public List<Tool> tools;

    public GeminiRequest(List<Content> contents, List<Tool> tools) {
        this.contents = contents;
        this.tools = tools;
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

        public Part(String text) {
            this.text = text;
        }
    }

    public static class Tool {
        @SerializedName("functionDeclarations")
        public List<FunctionDeclaration> functionDeclarations;

        public Tool(List<FunctionDeclaration> functionDeclarations) {
            this.functionDeclarations = functionDeclarations;
        }
    }

    public static class FunctionDeclaration {
        @SerializedName("name")
        public String name;
        @SerializedName("description")
        public String description;
        @SerializedName("parameters")
        public ParameterDefinition parameters;

        public FunctionDeclaration(String name, String description, ParameterDefinition parameters) {
            this.name = name;
            this.description = description;
            this.parameters = parameters;
        }
    }

    public static class ParameterDefinition {
        @SerializedName("type")
        public String type;
        @SerializedName("properties")
        public java.util.Map<String, Property> properties;
        @SerializedName("required")
        public List<String> required;

        public ParameterDefinition(String type, java.util.Map<String, Property> properties, List<String> required) {
            this.type = type;
            this.properties = properties;
            this.required = required;
        }
    }

    public static class Property {
        @SerializedName("type")
        public String type;
        @SerializedName("description")
        public String description;

        public Property(String type, String description) {
            this.type = type;
            this.description = description;
        }
    }
}
