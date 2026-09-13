package com.example.calculator;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private TextView resultText;
    private String currentInput = "0";

    @Override
    protected void onCreate(Bundle Bundle) {
        super.onCreate(Bundle);
        setContentView(R.layout.activity_main);

        resultText = findViewById(R.id.result);
        
        // Привязываем кнопки
        findViewById(R.id.btn_c).setOnClickListener(this);
        findViewById(R.id.btn_div).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_c) {
            currentInput = "0";
        } else if (id == R.id.btn_div) {
            currentInput += "/";
        }
        resultText.setText(currentInput);
    }
}
