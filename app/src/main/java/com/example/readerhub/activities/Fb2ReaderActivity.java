package com.example.readerhub.activities;

import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.readerhub.R;
import com.example.readerhub.models.Book;
import com.example.readerhub.models.ReadingHistory;
import com.example.readerhub.repository.BookRepository;
import com.example.readerhub.parsers.Fb2Parser;
import com.example.readerhub.utils.PreferencesManager;
import com.example.readerhub.utils.FileUtils;

public class Fb2ReaderActivity extends AppCompatActivity {

    private WebView webView;
    private BookRepository repository;
    private PreferencesManager prefsManager;
    private int bookId;
    private Book currentBook;
    private long readingStartTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fb2_reader);

        bookId = getIntent().getIntExtra("bookId", -1);
        if (bookId == -1) {
            Toast.makeText(this, "Error loading book", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        webView = findViewById(R.id.webView);
        repository = new BookRepository(this);
        prefsManager = new PreferencesManager(this);

        setupWebView();
        loadBookAndOpen();
    }

    private void setupWebView() {
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false);
        webView.setWebViewClient(new WebViewClient());

        // Apply theme
        if (prefsManager.isNightMode()) {
            webView.setBackgroundColor(0xFF1E1E1E);
        }
    }

    private void loadBookAndOpen() {
        repository.getBookById(bookId, book -> {
            if (book != null) {
                currentBook = book;
                runOnUiThread(this::parseFb2AndDisplay);
            } else {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Book not found", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }

    private void parseFb2AndDisplay() {
        new Thread(() -> {
            try {
                // Проверяем реальный тип файла
                String realType = com.example.readerhub.utils.FileUtils.detectFileTypeByContent(currentBook.getFilePath());
                if (!"FB2".equals(realType)) {
                    runOnUiThread(() -> {
                        if ("EPUB".equals(realType)) {
                            Toast.makeText(this, "Это EPUB файл, а не FB2! Открываю как EPUB...", Toast.LENGTH_LONG).show();
                            // Перенаправляем на EPUB reader
                            android.content.Intent intent = new android.content.Intent(this, com.example.readerhub.activities.EpubReaderActivity.class);
                            intent.putExtra("bookId", bookId);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(this, "Неверный формат файла. Ожидается FB2, обнаружен: " + realType, Toast.LENGTH_LONG).show();
                            finish();
                        }
                    });
                    return;
                }
                
                Fb2Parser.Fb2Book fb2Book = Fb2Parser.parse(currentBook.getFilePath());
                
                // Проверяем, что парсинг прошел успешно
                if (fb2Book == null) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Ошибка: парсер вернул null", Toast.LENGTH_LONG).show();
                        finish();
                    });
                    return;
                }
                
                // Логируем результаты парсинга для отладки
                android.util.Log.d("Fb2ReaderActivity", "Parsed FB2 - title: " + fb2Book.title + 
                    ", author: " + fb2Book.author + ", chapters: " + fb2Book.chapters.size());
                
                // Проверяем наличие содержимого
                if (fb2Book.chapters.isEmpty()) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "В файле не найдено содержимое. Попробуйте другой файл.", Toast.LENGTH_LONG).show();
                        finish();
                    });
                    return;
                }
                
                String htmlContent = Fb2Parser.convertToHtml(fb2Book);

                runOnUiThread(() -> {
                    webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null);
                    readingStartTime = System.currentTimeMillis();
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error parsing FB2 file", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        }).start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveReadingProgress();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        saveReadingProgress();
    }

    private void saveReadingProgress() {
        if (currentBook != null) {
            int scrollPosition = webView.getScrollY();
            repository.updateReadingProgress(bookId, scrollPosition);

            long readingEndTime = System.currentTimeMillis();
            int duration = (int) ((readingEndTime - readingStartTime) / 1000);

            if (duration > 0) {
                ReadingHistory history = new ReadingHistory(bookId, scrollPosition, duration);
                repository.insertReadingHistory(history);
            }
        }
    }
}