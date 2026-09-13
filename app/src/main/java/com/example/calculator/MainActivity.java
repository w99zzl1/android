package com.example.calculator;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private TextView resultText;
    private String currentExpression = "";
    private boolean isNewOp = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        resultText = findViewById(R.id.result);

        int[] buttonIds = {
            R.id.btn_c, R.id.btn_open, R.id.btn_close, R.id.btn_div,
            R.id.btn_7, R.id.btn_8, R.id.btn_9, R.id.btn_mult,
            R.id.btn_4, R.id.btn_5, R.id.btn_6, R.id.btn_sub,
            R.id.btn_1, R.id.btn_2, R.id.btn_3, R.id.btn_add,
            R.id.btn_0, R.id.btn_dot, R.id.btn_equal
        };

        for (int id : buttonIds) {
            View view = findViewById(id);
            if (view != null) {
                view.setOnClickListener(this);
            }
        }
    }

    @Override
    public void onClick(View v) {
        Button button = (Button) v;
        String buttonText = button.getText().toString();
        int id = v.getId();

        if (id == R.id.btn_c) {
            currentExpression = "";
            resultText.setText("0");
            isNewOp = true;
        } else if (id == R.id.btn_equal) {
            // Простейшая обработка результата для базовых операций
            try {
                String expr = currentExpression.replace("×", "*").replace("÷", "/");
                // Используем простую замену для демонстрации (базовое сложение/вычитание)
                if (expr.contains("+")) {
                    String[] parts = expr.split("\\+");
                    double res = Double.parseDouble(parts[0]) + Double.parseDouble(parts[1]);
                    resultText.setText(String.valueOf(res));
                } else if (expr.contains("-")) {
                    String[] parts = expr.split("-");
                    double res = Double.parseDouble(parts[0]) - Double.parseDouble(parts[1]);
                    resultText.setText(String.valueOf(res));
                } else {
                    resultText.setText(currentExpression);
                }
            } catch (Exception e) {
                resultText.setText("Ошибка");
            }
            isNewOp = true;
        } else {
            if (isNewOp) {
                if (buttonText.equals("+") || buttonText.equals("-") || buttonText.equals("×") || buttonText.equals("÷")) {
                    return;
                }
                currentExpression = "";
                isNewOp = false;
            }
            currentExpression += buttonText;
            resultText.setText(currentExpression);
        }
    }
}
