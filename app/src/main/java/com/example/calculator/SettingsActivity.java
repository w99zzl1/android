package com.example.calculator;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends AppCompatActivity {
    private SettingsManager settingsManager;
    private FlashlightTool flashlightTool;
    
    private TextInputLayout apiKeyLayout;
    private TextInputEditText apiKeyInput;
    private Spinner modelSpinner;
    private MaterialButton saveButton;
    private MaterialButton fetchModelsButton;
    private ProgressBar loadingIndicator;
    private SwitchMaterial flashlightSwitch;
    private MaterialCardView modelsCard;
    private RecyclerView modelsRecyclerView;
    private ModelsAdapter modelsAdapter;
    private List<String> availableModels = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        try {
            settingsManager = new SettingsManager(this);
            flashlightTool = new FlashlightTool();
            flashlightTool.attachContext(this);

            Toolbar toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("Settings");
            }

            initViews();
            loadCurrentSettings();
        } catch (Exception e) {
            UiUtils.handleError(findViewById(android.R.id.content), this, "Settings error", e);
        }
    }

    private void initViews() {
        apiKeyLayout = findViewById(R.id.apiKeyLayout);
        apiKeyInput = findViewById(R.id.apiKeyInput);
        modelSpinner = findViewById(R.id.modelSpinner);
        saveButton = findViewById(R.id.saveButton);
        fetchModelsButton = findViewById(R.id.fetchModelsButton);
        loadingIndicator = findViewById(R.id.loadingIndicator);
        flashlightSwitch = findViewById(R.id.flashlightSwitch);
        modelsCard = findViewById(R.id.modelsCard);
        modelsRecyclerView = findViewById(R.id.modelsRecyclerView);

        modelsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        modelsAdapter = new ModelsAdapter();
        modelsRecyclerView.setAdapter(modelsAdapter);

        saveButton.setOnClickListener(v -> saveSettings());
        fetchModelsButton.setOnClickListener(v -> fetchModels());

        apiKeyInput.setText(settingsManager.getApiKey());
        flashlightSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!flashlightTool.toggle(isChecked ? "on" : "off")) {
                showError("Could not control the flashlight (permission or device issue)");
            }
        });
    }

    private void loadCurrentSettings() {
        apiKeyInput.setText(settingsManager.getApiKey());
        
        List<String> cachedModels = settingsManager.getCachedModels();
        if (!cachedModels.isEmpty()) {
            availableModels.clear();
            availableModels.addAll(cachedModels);
            setupModelSpinner();
            modelsCard.setVisibility(View.VISIBLE);
            modelsAdapter.setModels(availableModels);
        } else {
            modelsCard.setVisibility(View.GONE);
        }

        String currentModel = settingsManager.getSelectedModel();
        // Model will be set in spinner after it's populated
    }

    private void setupModelSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
            android.R.layout.simple_spinner_dropdown_item, availableModels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        modelSpinner.setAdapter(adapter);
        
        String currentModel = settingsManager.getSelectedModel();
        int position = availableModels.indexOf(currentModel);
        if (position >= 0) {
            modelSpinner.setSelection(position);
        }
    }

    private void saveSettings() {
        String apiKey = apiKeyInput.getText().toString().trim();
        String selectedModel = modelSpinner.getSelectedItem() != null ? 
            modelSpinner.getSelectedItem().toString() : "";

        if (apiKey.isEmpty()) {
            showError("Please enter your API key");
            return;
        }

        settingsManager.saveApiKey(apiKey);
        settingsManager.saveProvider("google");
        if (!selectedModel.isEmpty()) {
            settingsManager.saveSelectedModel(selectedModel);
        }

        Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void fetchModels() {
        String apiKey = apiKeyInput.getText().toString().trim();
        if (apiKey.isEmpty()) {
            showError("Please enter your API key first");
            return;
        }

        fetchModelsButton.setEnabled(false);
        fetchModelsButton.setText("Fetching...");
        loadingIndicator.setVisibility(View.VISIBLE);
        modelsCard.setVisibility(View.GONE);

        new Thread(() -> {
            try {
                com.example.calculator.api.ApiService apiService = new com.example.calculator.api.ApiService();
                List<String> models = apiService.listModels(apiKey);
                
                runOnUiThread(() -> {
                    if (models != null && !models.isEmpty()) {
                        availableModels.clear();
                        availableModels.addAll(models);
                        settingsManager.saveModels(models);
                        setupModelSpinner();
                        modelsCard.setVisibility(View.VISIBLE);
                        // Update adapter
                        modelsAdapter.notifyDataSetChanged();
                        Toast.makeText(this, "Found " + models.size() + " models", Toast.LENGTH_SHORT).show();
                    } else {
                        showError("No models found");
                    }
                    fetchModelsButton.setEnabled(true);
                    fetchModelsButton.setText("Fetch Models");
                    loadingIndicator.setVisibility(View.GONE);
                    modelsCard.setVisibility(View.VISIBLE);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    showError("Failed to fetch models: " + e.getMessage());
                    fetchModelsButton.setEnabled(true);
                    fetchModelsButton.setText("Fetch Models");
                    loadingIndicator.setVisibility(View.GONE);
                });
            }
        }).start();
    }

    private void showError(String message) {
        String detail = UiUtils.buildErrorMessage("Error", new RuntimeException(message));
        UiUtils.copyToClipboard(this, "App error", detail);
        UiUtils.showSnackbar(findViewById(android.R.id.content), message);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}