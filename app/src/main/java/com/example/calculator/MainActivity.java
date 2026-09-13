package com.example.calculator;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.calculator.api.GeminiManager;
import com.example.calculator.tools.FlashlightTool;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private final GeminiManager geminiManager = new GeminiManager();
    private final FlashlightTool flashlightTool;
    private LinearLayout messagesContainer;
    private EditText inputField;
    private ScrollView scrollView;

    private final List<Map<String, Object>> tools = Arrays.asList(
        new HashMap<String, Object>() {{
            put("name", "FLASHLIGHT_TOGGLE");
            put("description", "Turns the flashlight on or off. State must be 'on' or 'off'.");
            Map<String, Object> params = new HashMap<>();
            params.put("type", "OBJECT");
            Map<String, Object> properties = new HashMap<>();
            Map<String, Object> stateProp = new HashMap<>();
            stateProp.put("type", "STRING");
            stateProp.put("description", "Must be 'on' or 'off'");
            properties.put("state", stateProp);
            params.put("properties", properties);
            params.put("required", Arrays.asList("state"));
            put("parameters", params);
        }}
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        flashlightTool = new FlashlightTool();
        flashlightTool.attachContext(this);

        messagesContainer = findViewById(R.id.messagesContainer);
        inputField = findViewById(R.id.inputField);
        scrollView = findViewById(R.id.scrollView);

        findViewById(R.id.sendButton).setOnClickListener(v -> sendMessage());
    }

    private void sendMessage() {
        String text = inputField.getText().toString().trim();
        if (text.isEmpty()) return;

        addMessage("user", text);
        inputField.setText("");

        new Thread(() -> {
            try {
                String response = geminiManager.processMessage(text, tools);

                if (response != null && response.startsWith("FUNCTION_CALL:")) {
                    String[] parts = response.substring("FUNCTION_CALL:".length()).split("\\|", 2);
                    String funcName = parts[0];
                    String argsJson = parts.length > 1 ? parts[1] : "{}";

                    String toolResult = executeTool(funcName, argsJson);

                    String finalResponse = geminiManager.processMessage(
                        "Based on the tool result: " + toolResult + ". Now respond to the user.",
                        tools
                    );

                    runOnUiThread(() -> addMessage("assistant", finalResponse != null ? finalResponse : "No response"));
                } else {
                    runOnUiThread(() -> addMessage("assistant", response != null ? response : "No response"));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error processing message", e);
                runOnUiThread(() -> addMessage("assistant", "Error: " + e.getMessage()));
            }
        }).start();
    }

    private String executeTool(String funcName, String argsJson) {
        if ("FLASHLIGHT_TOGGLE".equals(funcName)) {
            try {
                com.google.gson.JsonObject args = com.google.gson.JsonParser.parseString(argsJson).getAsJsonObject();
                String state = args.get("state").getAsString();
                boolean success = flashlightTool.toggle(state);
                if (success) {
                    return "Flashlight turned " + state;
                } else {
                    return "Failed to toggle flashlight";
                }
            } catch (Exception e) {
                return "Error: " + e.getMessage();
            }
        }
        return "Unknown tool: " + funcName;
    }

    private void addMessage(String sender, String text) {
        TextView messageView = new TextView(this);
        messageView.setText(text);
        messageView.setTextSize(16);
        messageView.setPadding(24, 16, 24, 16);
        messageView.setBackgroundColor(sender.equals("user") ? 0xFF6200EE : 0xFF2A2A4A);
        messageView.setTextColor(0xFFFFFFFF);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 8, 0, 8);
        messageView.setLayoutParams(params);

        messagesContainer.addView(messageView);
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
    }
}