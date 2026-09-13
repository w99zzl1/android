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

    private final GeminiManager geminiManager;
    private final FlashlightTool flashlightTool = new FlashlightTool();
    private final SettingsManager settingsManager;

    public MainActivity() {
        settingsManager = new SettingsManager(this);
        geminiManager = new GeminiManager(this);
    }

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
        settingsManager = new SettingsManager(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Check if API key is set
        if (!settingsManager.hasApiKey()) {
            startSettingsActivity();
            return;
        }

        flashlightTool.attachContext(this);

        initViews();
        setupRecyclerView();
        setupSendButton();
        
        // Add welcome message
        addMessage(new Message("assistant", "Hello! I'm your AI assistant. I can help you with various tasks including controlling your flashlight. How can I help you today?", false));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check if API key was set in settings
        if (!settingsManager.hasApiKey()) {
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
                runOnUiThread(() -> {
                    addMessage(new Message("assistant", "Error: " + e.getMessage(), false));
                    showLoading(false);
                });
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