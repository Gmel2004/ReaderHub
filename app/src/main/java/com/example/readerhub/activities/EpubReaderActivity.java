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
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        
        // Добавляем WebViewClient для обработки загрузки
        webView.setWebViewClient(new android.webkit.WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // Проверяем, что страница загрузилась
                view.evaluateJavascript("document.body.innerHTML.length", value -> {
                    if (value != null && Integer.parseInt(value.replace("\"", "")) == 0) {
                        android.util.Log.w("EpubReaderActivity", "Page appears to be empty");
                        Toast.makeText(EpubReaderActivity.this, "Страница пуста. Попробуйте открыть книгу снова.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            
            @Override
            public void onReceivedError(WebView view, android.webkit.WebResourceRequest request, android.webkit.WebResourceError error) {
                super.onReceivedError(view, request, error);
                android.util.Log.e("EpubReaderActivity", "WebView error: " + error.getDescription());
                Toast.makeText(EpubReaderActivity.this, "Ошибка загрузки: " + error.getDescription(), Toast.LENGTH_SHORT).show();
            }
        });

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
        runOnUiThread(() -> {
            // Показываем индикатор загрузки
            Toast.makeText(this, "Загрузка книги...", Toast.LENGTH_SHORT).show();
        });
        
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

                // Проверяем размер файла
                long fileSize = epubFile.length();
                long maxSize = 50 * 1024 * 1024; // 50 MB
                
                if (fileSize > maxSize) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Файл слишком большой (" + (fileSize / 1024 / 1024) + " MB). Загрузка может занять время...", 
                            Toast.LENGTH_LONG).show();
                    });
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

                // Загружаем первую главу или несколько первых глав (ограничиваем размер)
                int chaptersToLoad = Math.min(epubBook.chapters.size(), 5); // Загружаем максимум 5 глав за раз
                StringBuilder htmlBuilder = new StringBuilder();
                
                htmlBuilder.append("<!DOCTYPE html><html><head>");
                htmlBuilder.append("<meta charset='UTF-8'>");
                htmlBuilder.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
                htmlBuilder.append("<style>");
                htmlBuilder.append("body { font-family: 'Georgia', serif; line-height: 1.8; padding: 20px; max-width: 800px; margin: 0 auto; background: #f5f5f5; }");
                htmlBuilder.append("h1, h2, h3 { color: #2c3e50; margin-top: 30px; }");
                htmlBuilder.append("p { text-indent: 2em; margin: 10px 0; text-align: justify; }");
                htmlBuilder.append("img { max-width: 100%; height: auto; }");
                htmlBuilder.append("</style>");
                htmlBuilder.append("</head><body>");

                if (epubBook.title != null && !epubBook.title.isEmpty()) {
                    htmlBuilder.append("<h1>").append(escapeHtml(epubBook.title)).append("</h1>");
                }
                if (epubBook.author != null && !epubBook.author.isEmpty()) {
                    htmlBuilder.append("<p style='text-align:center;'><i>").append(escapeHtml(epubBook.author)).append("</i></p>");
                }

                // Загружаем только первые главы, чтобы не перегружать память
                for (int i = 0; i < chaptersToLoad; i++) {
                    if (i < epubBook.chapters.size()) {
                        htmlBuilder.append(epubBook.chapters.get(i));
                    }
                }
                
                if (epubBook.chapters.size() > chaptersToLoad) {
                    htmlBuilder.append("<p style='text-align:center; color:#666; margin-top:40px;'>");
                    htmlBuilder.append("Загружено ").append(chaptersToLoad).append(" из ").append(epubBook.chapters.size()).append(" глав.");
                    htmlBuilder.append(" Остальные главы будут загружены по мере чтения.");
                    htmlBuilder.append("</p>");
                }

                htmlBuilder.append("</body></html>");
                
                final String htmlContent = htmlBuilder.toString();
                
                runOnUiThread(() -> {
                    try {
                        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null);
                        saveReadingProgress();
                    } catch (OutOfMemoryError e) {
                        android.util.Log.e("EpubReaderActivity", "OutOfMemoryError loading HTML", e);
                        Toast.makeText(this, "Книга слишком большая. Попробуйте открыть другую книгу.", Toast.LENGTH_LONG).show();
                        finish();
                    }
                });

            } catch (OutOfMemoryError e) {
                android.util.Log.e("EpubReaderActivity", "OutOfMemoryError parsing EPUB", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Недостаточно памяти для загрузки книги. Файл слишком большой.", Toast.LENGTH_LONG).show();
                    finish();
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
    
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
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
