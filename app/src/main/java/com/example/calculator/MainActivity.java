package com.example.calculator;

import android.os.Bundle;
import android.util.Log;

import androidx.activity.ComponentActivity;
import androidx.activity.compose.setContent;
import androidx.compose.foundation.layout.*;
import androidx.compose.foundation.lazy.LazyColumn;
import androidx.compose.foundation.lazy.items;
import androidx.compose.foundation.shape.CircleShape;
import androidx.compose.foundation.shape.RoundedCornerShape;
import androidx.compose.material3.*;
import androidx.compose.runtime.*;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import androidx.compose.ui.draw.clip;
import androidx.compose.ui.graphics.Color;
import androidx.compose.ui.text.font.FontWeight;
import androidx.compose.ui.text.style.TextAlign;
import androidx.compose.ui.unit.dp;
import androidx.compose.ui.unit.sp;

import com.example.calculator.api.GeminiManager;
import com.example.calculator.tools.FlashlightTool;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends ComponentActivity {
    private static final String TAG = "MainActivity";
    private final GeminiManager geminiManager = new GeminiManager();
    private final FlashlightTool flashlightTool;

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

    public MainActivity() {
        flashlightTool = new FlashlightTool(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContent(() -> MaterialTheme(
                androidx.compose.material3.Typography,
                () -> ChatScreen()
        ));
    }

    @Composable
    private void ChatScreen() {
        var messages = remember(() -> new java.util.ArrayList<ChatMessage>());
        var inputText = remember(() -> mutableStateOf(""));
        var isLoading = remember(() -> mutableStateOf(false));
        var flashlightOn = remember(() -> mutableStateOf(false));
        var context = this;

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1A1A2E))
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF16213E),
                shadowElevation = 4.dp
            ) {
                Text(
                    text = "AI Assistant",
                    modifier = Modifier.padding(16.dp),
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items((List<ChatMessage>) messages, msg -> {
                    MessageBubble(msg);
                });
                if (isLoading.value) {
                    item(() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Surface(
                                modifier = Modifier.clip(RoundedCornerShape(12.dp)),
                                color = Color(0xFF2A2A4A)
                            ) {
                                Text(
                                    text = "Thinking...",
                                    modifier = Modifier.padding(12.dp),
                                    color = Color(0xFF8888AA),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    });
                }
            }

            Text(
                text = flashlightOn.value ? "🔦 Flashlight: ON" : "🔦 Flashlight: OFF",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                color = flashlightOn.value ? Color(0xFFFFEB3B) : Color.Gray,
                fontSize = 12.sp
            );

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText.value,
                    onValueChange = v -> inputText.setValue(v),
                    placeholder = () -> Text("Ask me anything...", color = Color.Gray),
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color(0xFF6200EE),
                        unfocusedBorderColor = Color(0xFF444466),
                        cursorColor = Color(0xFF6200EE)
                    ),
                    textStyle = TextStyle(color = Color.White),
                    shape = RoundedCornerShape(20.dp)
                );
                Spacer(modifier = Modifier.width(8.dp));
                Button(
                    onClick = () -> {
                        var text = inputText.value.trim();
                        if (!text.isEmpty()) {
                            sendMessage(text, messages, inputText, isLoading, flashlightOn);
                        }
                    },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE)),
                    modifier = Modifier.size(48.dp)
                ) {
                    Text("→", fontSize = 20.sp, color = Color.White);
                }
            }
        }
    }

    private void sendMessage(
        String text,
        java.util.List<ChatMessage> messages,
        MutableState<String> inputText,
        MutableState<Boolean> isLoading,
        MutableState<Boolean> flashlightOn
    ) {
        messages.add(new ChatMessage("user", text));
        inputText.setValue("");
        isLoading.setValue(true);

        CoroutineHelper.INSTANCE.runInIO(() -> {
            try {
                String response = geminiManager.processMessage(text, tools);

                if (response.startsWith("FUNCTION_CALL:")) {
                    String[] parts = response.substring("FUNCTION_CALL:".length()).split("\\|", 2);
                    String funcName = parts[0];
                    String argsJson = parts.length > 1 ? parts[1] : "{}";

                    String toolResult = executeTool(funcName, argsJson, flashlightOn);

                    String finalResponse = geminiManager.processMessage(
                        "Based on the tool result: " + toolResult + ". Now respond to the user.",
                        tools
                    );

                    var fr = finalResponse;
                    runOnUiThread(() -> {
                        messages.add(new ChatMessage("assistant", fr));
                        isLoading.setValue(false);
                    });
                } else {
                    var res = response;
                    runOnUiThread(() -> {
                        messages.add(new ChatMessage("assistant", res));
                        isLoading.setValue(false);
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error processing message", e);
                var errMsg = "Error: " + e.getMessage();
                runOnUiThread(() -> {
                    messages.add(new ChatMessage("assistant", errMsg));
                    isLoading.setValue(false);
                });
            }
            return null;
        });
    }

    private String executeTool(String funcName, String argsJson, MutableState<Boolean> flashlightOn) {
        if ("FLASHLIGHT_TOGGLE".equals(funcName)) {
            try {
                com.google.gson.JsonObject args = com.google.gson.JsonParser.parseString(argsJson).getAsJsonObject();
                String state = args.get("state").getAsString();
                boolean success = flashlightTool.toggle(state);
                if (success) {
                    flashlightOn.setValue("on".equalsIgnoreCase(state));
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

    @Composable
    private void MessageBubble(ChatMessage message) {
        Color backgroundColor;
        Alignment alignment;
        if ("user".equals(message.sender)) {
            backgroundColor = Color(0xFF6200EE);
            alignment = Alignment.End;
        } else {
            backgroundColor = Color(0xFF2A2A4A);
            alignment = Alignment.Start;
        }

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = alignment
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(16.dp)),
                color = backgroundColor
            ) {
                Text(
                    text = message.text,
                    modifier = Modifier.padding(12.dp),
                    color = Color.White,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Start
                );
            }
        }
    }

    static class ChatMessage {
        String sender;
        String text;
        ChatMessage(String sender, String text) {
            this.sender = sender;
            this.text = text;
        }
    }
}
