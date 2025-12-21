package com.example.readerhub.activities;

import android.os.Bundle;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.readerhub.R;
import com.example.readerhub.models.Book;
import com.example.readerhub.repository.BookRepository;

import java.io.File;

import com.example.readerhub.parsers.EpubParser;

public class EpubReaderActivity extends AppCompatActivity {

    private WebView webView;
    private BookRepository repository;
    private int bookId;
    private Book currentBook;
    private EpubParser.EpubBook epubBook;
    private int currentChapterIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fb2_reader); // Используем тот же layout

        bookId = getIntent().getIntExtra("bookId", -1);
        if (bookId == -1) {
            Toast.makeText(this, "Error loading book", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        repository = new BookRepository(this);
        webView = findViewById(R.id.webView);

        // Настройка WebView
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setAllowContentAccess(true);

        loadBookAndOpen();
    }

    private void loadBookAndOpen() {
        repository.getBookById(bookId, book -> {
            if (book != null) {
                currentBook = book;
                runOnUiThread(this::parseEpubAndDisplay);
            } else {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Book not found", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }

    private void parseEpubAndDisplay() {
        new Thread(() -> {
            try {
                File epubFile = new File(currentBook.getFilePath());
                if (!epubFile.exists()) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "File not found: " + currentBook.getFilePath(), Toast.LENGTH_SHORT).show();
                        finish();
                    });
                    return;
                }

                epubBook = EpubParser.parse(currentBook.getFilePath());
                
                if (epubBook == null || epubBook.chapters.isEmpty()) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "No content found in EPUB. Chapters: " + 
                            (epubBook != null ? epubBook.chapters.size() : 0), Toast.LENGTH_LONG).show();
                        finish();
                    });
                    return;
                }

                // Показываем все главы вместе
                String fullHtml = EpubParser.convertToHtml(epubBook);
                
                runOnUiThread(() -> {
                    webView.loadDataWithBaseURL(null, fullHtml, "text/html", "UTF-8", null);
                    saveReadingProgress();
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error parsing EPUB file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        }).start();
    }

    private void loadChapter(int chapterIndex) {
        if (chapterIndex < 0 || chapterIndex >= epubBook.chapters.size()) {
            return;
        }

        try {
            String htmlContent = epubBook.chapters.get(chapterIndex);
            
            // Создаем полный HTML документ с CSS для текущей главы
            String fullHtml = createHtmlDocument(htmlContent);

            runOnUiThread(() -> {
                webView.loadDataWithBaseURL(null, fullHtml, "text/html", "UTF-8", null);
                
                // Сохраняем прогресс
                currentChapterIndex = chapterIndex;
                saveReadingProgress();
            });

        } catch (Exception e) {
            e.printStackTrace();
            runOnUiThread(() -> {
                Toast.makeText(this, "Error loading chapter", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private String createHtmlDocument(String content) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        html.append("<style>");
        html.append("body { font-family: 'Georgia', serif; line-height: 1.8; padding: 20px; max-width: 800px; margin: 0 auto; background: #f5f5f5; }");
        html.append("h1, h2, h3 { color: #2c3e50; margin-top: 30px; }");
        html.append("p { text-indent: 2em; margin: 10px 0; text-align: justify; }");
        html.append("img { max-width: 100%; height: auto; }");
        html.append("</style>");
        html.append("</head><body>");
        html.append(content);
        html.append("</body></html>");
        return html.toString();
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
            currentBook.setCurrentPage(currentChapterIndex);
            currentBook.setLastOpened(System.currentTimeMillis());
            repository.updateBook(currentBook);
            repository.updateReadingProgress(currentBook.getId(), currentChapterIndex);
        }
    }
}
