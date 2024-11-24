package com.example.laba5;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class PopupActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "AppPrefs";
    private static final String KEY_SHOW_POPUP = "showPopup";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.popup_instructions);

        // Устанавливаем обработчик для кнопки закрытия
        Button dismissButton = findViewById(R.id.okButton);  // Предполагаем, что у вас есть такая кнопка
        dismissButton.setOnClickListener(v -> {
            // Сохраняем в SharedPreferences, что всплывающее окно больше не нужно показывать
            SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(KEY_SHOW_POPUP, false);
            editor.apply();

            // Закрываем PopupActivity
            finish();
        });
    }
}
