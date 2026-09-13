package com.example.calculator;

import android.os.Bundle;

import androidx.activity.ComponentActivity;
import androidx.activity.compose.setContent;

public class MainActivity extends ComponentActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppState state = new AppState();
        state.flashlightTool.attachContext(this);
        setContent(() -> ChatScreen(state));
    }
}
