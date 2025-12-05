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
                Fb2Parser.Fb2Book fb2Book = Fb2Parser.parse(currentBook.getFilePath());
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