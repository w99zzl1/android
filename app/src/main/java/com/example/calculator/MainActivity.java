package com.example.calculator;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private RecyclerView messagesRecyclerView;
    private MessagesAdapter messagesAdapter;
    private List<Message> messages = new ArrayList<>();
    private TextInputEditText inputField;
    private MaterialButton sendButton;
    private View loadingIndicator;

    private GeminiManager geminiManager;
    private final FlashlightTool flashlightTool = new FlashlightTool();
    private SettingsManager settingsManager;
    private DeviceTools deviceTools;
    private boolean isReady = false;

    private static Map<String, Object> tool(String name, String description, Object... props) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("type", "OBJECT");
        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> required = new HashMap<>();
        for (int i = 0; i + 1 < props.length; i += 2) {
            String key = String.valueOf(props[i]);
            Map<String, Object> prop = (Map<String, Object>) props[i + 1];
            properties.put(key, prop);
            if (Boolean.TRUE.equals(prop.get("__required"))) {
                required.put(key, null);
            }
            prop.remove("__required");
        }
        if (!required.isEmpty()) {
            parameters.put("required", required.keySet());
        }
        parameters.put("properties", properties);
        Map<String, Object> tool = new HashMap<>();
        tool.put("name", name);
        tool.put("description", description);
        tool.put("parameters", parameters);
        return tool;
    }

    private final List<Map<String, Object>> tools = Arrays.asList(
        tool("FLASHLIGHT_TOGGLE", "Turns the flashlight on or off. State must be 'on' or 'off'.",
            "state", stringProp("Must be 'on' or 'off'", true)),
        tool("OPEN_APP_URL", "Opens a website in the browser, or an installed app. For apps use 'app:<package name>' (e.g. 'app:com.whatsapp'), otherwise pass a URL or domain name.",
            "target", stringProp("URL, domain, or app:<package>", true)),
        tool("SET_ALARM_TIMER", "Sets an alarm at a given time, or a timer for a given number of seconds. type is 'alarm' (use hour/minute) or 'timer' (use seconds).",
            "type", stringProp("'alarm' or 'timer'", true),
            "hour", intProp("Hour for alarm, 0-23", false),
            "minute", intProp("Minute for alarm, 0-59", false),
            "seconds", intProp("Duration in seconds for timer, 1-86400", false),
            "label", stringProp("Optional label/message", false)),
        tool("SET_VOLUME", "Changes the volume of a stream. stream is one of: media, ring, alarm, notification, call. volume is 0-100 percent.",
            "stream", stringProp("media, ring, alarm, notification, or call", true),
            "volume", intProp("Volume 0-100 percent", true)),
        tool("SET_BRIGHTNESS", "Sets the screen brightness to a percentage (0-100).",
            "percent", intProp("Brightness 0-100 percent", true))
    );

    private static Map<String, Object> stringProp(String description, boolean required) {
        Map<String, Object> m = new HashMap<>();
        m.put("type", "STRING");
        m.put("description", description);
        if (required) m.put("__required", true);
        return m;
    }

    private static Map<String, Object> intProp(String description, boolean required) {
        Map<String, Object> m = new HashMap<>();
        m.put("type", "INTEGER");
        m.put("description", description);
        if (required) m.put("__required", true);
        return m;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        try {
            settingsManager = new SettingsManager(this);
            geminiManager = new GeminiManager(this);
            deviceTools = new DeviceTools(this);
            initViews();
            setupRecyclerView();
            setupSendButton();
            addMessage(new Message("assistant", "Hello! I'm your AI assistant. I can control your phone: flashlight, screen brightness, volume, open apps/web pages, set alarms and timers. What can I do for you?", false));
            isReady = true;
        } catch (Exception e) {
            Log.e(TAG, "Error initializing MainActivity", e);
            UiUtils.handleError(findViewById(android.R.id.content), this, "App error", e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isReady && !settingsManager.hasApiKey()) {
            startSettingsActivity();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            startSettingsActivity();
            return true;
        } else if (id == R.id.action_clear_chat) {
            clearChat();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("AI Assistant");
        }

        RecyclerView recyclerView = findViewById(R.id.messagesRecyclerView);
        messagesRecyclerView = recyclerView;
        inputField = findViewById(R.id.inputField);
        sendButton = findViewById(R.id.sendButton);
        loadingIndicator = findViewById(R.id.loadingIndicator);

        flashlightTool.attachContext(this);
    }

    private void setupRecyclerView() {
        messagesAdapter = new MessagesAdapter(messages, this);
        messagesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        messagesRecyclerView.setAdapter(messagesAdapter);
    }

    private void setupSendButton() {
        sendButton.setOnClickListener(v -> sendMessage());
    }

    private void sendMessage() {
        String text = inputField.getText().toString().trim();
        if (text.isEmpty()) return;

        // Add user message
        addMessage(new Message("user", text, false));
        inputField.setText("");

        // Show loading
        showLoading(true);

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

                    String finalMsg = finalResponse != null ? finalResponse : "Done.";
                    runOnUiThread(() -> {
                        addMessage(new Message("assistant", finalMsg, false));
                        showLoading(false);
                    });
                } else {
                    String finalMsg = response != null ? response : "No response from AI.";
                    runOnUiThread(() -> {
                        addMessage(new Message("assistant", finalMsg, false));
                        showLoading(false);
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error processing message", e);
                String errMsg = UiUtils.buildErrorMessage("Request failed", e);
                UiUtils.copyToClipboard(this, "Request error", errMsg);
                String shortMsg = e.getMessage() != null ? e.getMessage() : e.toString();
                runOnUiThread(() -> {
                    UiUtils.showSnackbar(findViewById(android.R.id.content), "Error: " + shortMsg);
                    addMessage(new Message("assistant", "Error: " + shortMsg, false));
                    showLoading(false);
                });
            }
        }).start();
    }

    private String executeTool(String funcName, String argsJson) {
        com.google.gson.JsonObject args;
        try {
            args = com.google.gson.JsonParser.parseString(argsJson).getAsJsonObject();
        } catch (Exception e) {
            return "Invalid tool arguments: " + e.getMessage();
        }

        try {
            switch (funcName) {
                case "FLASHLIGHT_TOGGLE": {
                    String state = args.has("state") ? args.get("state").getAsString() : "";
                    boolean success = flashlightTool.toggle(state);
                    return success ? "Flashlight turned " + state : "Failed to toggle flashlight";
                }
                case "OPEN_APP_URL":
                    return deviceTools.openAppUrl(args);
                case "SET_ALARM_TIMER":
                    return deviceTools.setAlarmTimer(args);
                case "SET_VOLUME":
                    return deviceTools.setVolume(args);
                case "SET_BRIGHTNESS":
                    return deviceTools.setBrightness(args);
                default:
                    return "Unknown tool: " + funcName;
            }
        } catch (Exception e) {
            return "Tool error: " + e.getMessage();
        }
    }

    private void addMessage(Message message) {
        messages.add(message);
        runOnUiThread(() -> {
            messagesAdapter.notifyItemInserted(messages.size() - 1);
            messagesRecyclerView.scrollToPosition(messages.size() - 1);
        });
    }

    private void showLoading(boolean show) {
        runOnUiThread(() -> {
            loadingIndicator.setVisibility(show ? View.VISIBLE : View.GONE);
            sendButton.setEnabled(!show);
            inputField.setEnabled(!show);
        });
    }

    private void clearChat() {
        new MaterialAlertDialogBuilder(this)
            .setTitle("Clear Chat")
            .setMessage("Are you sure you want to clear all messages?")
            .setPositiveButton("Yes", (dialog, which) -> {
                messages.clear();
                messagesAdapter.notifyDataSetChanged();
            })
            .setNegativeButton("No", null)
            .show();
    }

    private void startSettingsActivity() {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}