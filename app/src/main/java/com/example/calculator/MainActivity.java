package com.example.calculator;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
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
import java.util.Locale;
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
    private WebSearchTool webSearchTool;
    private YouTubeSearchTool youtubeSearchTool;
    private boolean isReady = false;

    private static final int REQ_RECORD_AUDIO = 1001;
    private static final String WAKE_WORD = "вега";
    private ImageButton micButton;
    private SpeechRecognizer speechRecognizer;
    private Intent speechRecognizerIntent;

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
        tool("WEB_SEARCH", "Searches the internet for up-to-date information. Returns the top results with titles, summaries and URLs. Use it when the user asks about current events, facts you are not sure about, or wants to find a specific web page or video.",
            "query", stringProp("The search query", true)),
        tool("PLAY_YOUTUBE", "Searches YouTube and returns a list of ACTUAL videos: titles, durations and video IDs. Use this whenever the user wants to watch, play, or listen to something on YouTube (cartoons, music, movies, tutorials...). After getting the list, pick the most suitable video and call OPEN_YOUTUBE_VIDEO with its exact videoId. NEVER open a YouTube search page instead.",
            "query", stringProp("What to search on YouTube", true)),
        tool("OPEN_YOUTUBE_VIDEO", "Plays a specific YouTube video on the device. The videoId MUST be taken from the PLAY_YOUTUBE results, never invented.",
            "videoId", stringProp("The 11-character YouTube video ID", true))
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
            webSearchTool = new WebSearchTool();
            youtubeSearchTool = new YouTubeSearchTool();
            initViews();
            setupRecyclerView();
            setupSendButton();
            setupMicButton();
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
        micButton = findViewById(R.id.micButton);

        flashlightTool.attachContext(this);
    }

    private void setupMicButton() {
        micButton.setOnClickListener(v -> startVoiceInput());
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
        sendMessage(text);
    }

    private void sendMessage(String text) {
        if (text == null || text.trim().isEmpty()) return;
        String finalText = text.trim();

        // Add user message
        addMessage(new Message("user", finalText, false));
        inputField.setText("");

        // Show loading
        showLoading(true);

        new Thread(() -> {
            try {
                int MAX_STEPS = 6;
                StringBuilder context = new StringBuilder(
                    "User request: " + finalText + "\n" +
                    "RULES: " +
                    "- If the user wants to WATCH or PLAY a video/music/cartoon on YouTube, you MUST first call PLAY_YOUTUBE to get real videos, " +
                    "choose the best one, then play it with OPEN_YOUTUBE_VIDEO using its videoId. Opening a YouTube search page is FORBIDDEN and counts as failure. " +
                    "- If you need up-to-date facts from the web, call WEB_SEARCH. " +
                    "- Loop tools until the task is fully done, then answer the user in their language.\n");

                String finalResponse = null;
                for (int step = 0; step < MAX_STEPS; step++) {
                    String response = geminiManager.processMessage(context.toString(), tools);

                    if (response != null && response.startsWith("FUNCTION_CALL:")) {
                        String[] parts = response.substring("FUNCTION_CALL:".length()).split("\\|", 2);
                        String funcName = parts[0];
                        String argsJson = parts.length > 1 ? parts[1] : "{}";

                        String toolResult = executeTool(funcName, argsJson);

                        context.append("Tool called: ").append(funcName)
                               .append(argsJson.isEmpty() || "{}".equals(argsJson) ? "" : " args=" + argsJson)
                               .append("\nTool result: ").append(toolResult)
                               .append("\nContinue: if another tool is needed, call it; otherwise answer the user now.\n");
                    } else {
                        finalResponse = response;
                        break;
                    }
                }

                String finalMsg = finalResponse != null ? finalResponse : "Done.";
                runOnUiThread(() -> {
                    addMessage(new Message("assistant", finalMsg, false));
                    showLoading(false);
                });
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
                case "WEB_SEARCH": {
                    String query = args.has("query") ? args.get("query").getAsString() : "";
                    return webSearchTool.search(query);
                }
                case "PLAY_YOUTUBE": {
                    String query = args.has("query") ? args.get("query").getAsString() : "";
                    return youtubeSearchTool.searchText(query);
                }
                case "OPEN_YOUTUBE_VIDEO":
                    return deviceTools.openYouTubeVideo(args);
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
            if (micButton != null) micButton.setEnabled(!show);
        });
    }

    private void startVoiceInput() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            UiUtils.showSnackbar(findViewById(android.R.id.content), "Speech recognition is not available on this device");
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQ_RECORD_AUDIO);
            return;
        }

        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            speechRecognizer.setRecognitionListener(recognitionListener);
            speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
            speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        }

        try {
            UiUtils.showSnackbar(findViewById(android.R.id.content), "Listening... say 'Вега, ...' for hands-free");
            speechRecognizer.startListening(speechRecognizerIntent);
        } catch (Exception e) {
            Log.e(TAG, "Voice input error", e);
            UiUtils.handleError(findViewById(android.R.id.content), this, "Voice input error", e);
        }
    }

    private final RecognitionListener recognitionListener = new RecognitionListener() {
        @Override
        public void onReadyForSpeech(Bundle params) {
        }

        @Override
        public void onBeginningOfSpeech() {
        }

        @Override
        public void onRmsChanged(float rms) {
        }

        @Override
        public void onBufferReceived(byte[] buffer) {
        }

        @Override
        public void onEndOfSpeech() {
        }

        @Override
        public void onError(int error) {
            if (error != SpeechRecognizer.ERROR_NO_MATCH && error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                UiUtils.showSnackbar(findViewById(android.R.id.content), "Voice input error (code " + error + ")");
            }
        }

        @Override
        public void onResults(Bundle results) {
            handleSpeechResult(results);
        }

        @Override
        public void onPartialResults(Bundle partialResults) {
            handleSpeechResult(partialResults);
        }

        @Override
        public void onEvent(int eventType, Bundle params) {
        }
    };

    private void handleSpeechResult(Bundle results) {
        List<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches == null || matches.isEmpty()) return;
        processVoicePhrase(matches.get(0));
    }

    private void processVoicePhrase(String phrase) {
        if (phrase == null) return;
        phrase = phrase.trim();
        if (phrase.isEmpty()) return;

        boolean wake = phrase.toLowerCase(Locale.ROOT).startsWith(WAKE_WORD);
        String remainder = phrase;
        if (wake) {
            remainder = phrase.substring(WAKE_WORD.length()).replaceFirst("^[\\s\\p{Punct}]+", "").trim();
        }

        final String cleanPhrase = wake ? remainder : phrase;

        runOnUiThread(() -> {
            if (cleanPhrase.isEmpty()) {
                UiUtils.showSnackbar(findViewById(android.R.id.content),
                    "Say a command after 'Вега', e.g. 'Вега, включи мультики'");
                return;
            }
            if (wake) {
                sendMessage(cleanPhrase);
            } else {
                inputField.setText(cleanPhrase);
                inputField.setSelection(cleanPhrase.length());
                UiUtils.showSnackbar(findViewById(android.R.id.content), "Recognized - tap send to use");
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_RECORD_AUDIO
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startVoiceInput();
        } else if (requestCode == REQ_RECORD_AUDIO) {
            UiUtils.showSnackbar(findViewById(android.R.id.content), "Microphone permission required for voice input");
        }
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
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
    }
}