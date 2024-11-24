package com.example.laba5;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private static final int STORAGE_PERMISSION_REQUEST_CODE = 1;
    private Button downloadButton, viewButton, deleteButton;
    private EditText journalIdInput;  // EditText для ввода ID журнала

    private static final String PREFS_NAME = "AppPrefs";
    private static final String KEY_SHOW_POPUP = "showPopup";

    @SuppressLint("MissingSuperCall")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Инициализация элементов
        downloadButton = findViewById(R.id.download_button);
        viewButton = findViewById(R.id.view_button);
        deleteButton = findViewById(R.id.delete_button);
        journalIdInput = findViewById(R.id.journal_id_input);

        // Проверяем, нужно ли показывать всплывающее окно
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean showPopup = preferences.getBoolean(KEY_SHOW_POPUP, true);
        if (showPopup) {
            // Запускаем PopupActivity
            Intent intent = new Intent(this, PopupActivity.class);
            startActivity(intent);

            // После показа всплывающего окна обновляем настройки, чтобы не показывать его снова
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(KEY_SHOW_POPUP, false);
            editor.apply();
        }

        // Запрашиваем разрешения на доступ к хранилищу
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_REQUEST_CODE);
        }

        // Обрабатываем отступы для системных панелей
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Устанавливаем обработчики для кнопок
        downloadButton.setOnClickListener(v -> {
            String journalId = journalIdInput.getText().toString();
            if (!journalId.isEmpty()) {
                downloadFile(journalId);  // Передаем ID для скачивания
            } else {
                Toast.makeText(this, "Пожалуйста, введите ID журнала", Toast.LENGTH_SHORT).show();
            }
        });

        viewButton.setOnClickListener(v -> {
            String journalId = journalIdInput.getText().toString();
            if (!journalId.isEmpty()) {
                openPDF(journalId);  // Передаем ID для открытия PDF
            } else {
                Toast.makeText(this, "Пожалуйста, введите ID журнала", Toast.LENGTH_SHORT).show();
            }
        });

        deleteButton.setOnClickListener(v -> {
            String journalId = journalIdInput.getText().toString();
            if (!journalId.isEmpty()) {
                deleteFile(journalId);  // Передаем ID для удаления файла
            } else {
                Toast.makeText(this, "Пожалуйста, введите ID журнала", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Разрешение получено
            } else {
                Toast.makeText(this, "Разрешение отклонено", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Скачивание файла
    private void downloadFile(String journalId) {
        String url = "http://ntv.ifmo.ru/file/journal/" + journalId + ".pdf";
        OkHttpClient client = new OkHttpClient();

        // Создаем запрос с добавленным заголовком "User-Agent"
        Request request = new Request.Builder()
                .url(url) // Указываем URL
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36") // Добавляем заголовок
                .build();

        // Выполняем запрос асинхронно
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String contentType = response.header("Content-Type");
                    if (contentType != null && contentType.contains("pdf")) {
                        // Успешный ответ, скачиваем файл
                        saveFile(response.body().byteStream(), journalId);
                    } else {
                        // Ошибка: файл не PDF
                        showErrorPage("Получен неожиданный тип контента: " + contentType);
                    }
                } else {
                    // Ошибка запроса
                    showErrorPage("Ошибка при скачивании файла. Код ответа: " + response.code());
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {
                // Ошибка при выполнении запроса
                showErrorPage("Ошибка при подключении: " + e.getMessage());
            }
        });
    }


    private void showErrorPage(String message) {
        runOnUiThread(() -> Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show());
    }



    // Сохранение файла на внешнее хранилище
    private void saveFile(InputStream inputStream, String journalId) throws IOException {
        // Получаем путь к внешнему хранилищу
        File directory = new File(getExternalFilesDir(null), "JournalFiles");

        // Если папка не существует, создаем её
        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                Toast.makeText(this, "Не удалось создать папку для файлов", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Создаем файл внутри папки
        File file = new File(directory, "journal_" + journalId + ".pdf");

        // Открываем поток для записи файла
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            // Читаем данные из потока и записываем в файл
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
    }


    // Открытие PDF файла
    private void openPDF(String journalId) {
        // Путь к файлу PDF в директории
        File file = new File(getExternalFilesDir(null), "JournalFiles/journal_" + journalId + ".pdf");

        if (file.exists()) {
            // Получаем URI для файла через FileProvider
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);

            // Создаем Intent для открытия PDF
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/pdf");
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            // Разрешаем доступ другим приложениям для чтения URI
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            try {
                startActivity(intent); // Открываем файл
            } catch (ActivityNotFoundException e) {
                // Если на устройстве нет PDF-ридера
                Toast.makeText(this, "PDF-читатель не найден", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Файл не найден", Toast.LENGTH_SHORT).show();
        }
    }



    // Удаление файла
    public boolean deleteFile(String journalId) {
        File file = new File(getExternalFilesDir(null), "JournalFiles/journal_" + journalId + ".pdf");

        if (file.exists()) {
            if (file.delete()) {
                Toast.makeText(this, "Файл удален", Toast.LENGTH_SHORT).show();
                return true;
            } else {
                Toast.makeText(this, "Не удалось удалить файл", Toast.LENGTH_SHORT).show();
                return false;
            }
        } else {
            Toast.makeText(this, "Файл не найден", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    // Показ ошибки
    private void showErrorPage() {
        runOnUiThread(() -> Toast.makeText(MainActivity.this, "Ошибка при скачивании файла", Toast.LENGTH_SHORT).show());
    }
}
