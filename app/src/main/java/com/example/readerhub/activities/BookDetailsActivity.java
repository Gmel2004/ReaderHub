package com.example.readerhub.activities;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.example.readerhub.R;
import com.example.readerhub.models.Book;
import com.example.readerhub.parsers.WebBookParser;
import com.example.readerhub.repository.BookRepository;
import com.example.readerhub.utils.FileUtils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.File;
import java.util.stream.Collectors;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;

public class BookDetailsActivity extends AppCompatActivity {

    private ImageView coverImageView;
    private TextView titleTextView, authorTextView, chaptersTextView, descriptionTextView;
    private Button downloadButton;
    private String bookUrl;
    private String bookId;
    private WebBookParser parser;

    private long lastDownloadId = -1;
    private String bookTitle;
    private String bookAuthor;
    private String bookFormat;
    private String bookCoverUrl;

    private boolean receiverRegistered = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_details);

        bookUrl = getIntent().getStringExtra("bookUrl");
        parser = new WebBookParser();

        // Извлекаем ID книги из URL (например: /ranobe311 -> 311)
        if (bookUrl != null && bookUrl.contains("/ranobe")) {
            bookId = bookUrl.replaceAll(".*/ranobe(\\d+).*", "$1");
        }

        coverImageView = findViewById(R.id.coverImageView);
        titleTextView = findViewById(R.id.titleTextView);
        authorTextView = findViewById(R.id.authorTextView);
        chaptersTextView = findViewById(R.id.chaptersTextView);
        descriptionTextView = findViewById(R.id.descriptionTextView);
        downloadButton = findViewById(R.id.downloadButton);

        loadBookDetails();
    }

    private void loadBookDetails() {
        parser.parseRanobeBookDetails(bookUrl, new WebBookParser.OnBookDetailsListener() {
            @Override
            public void onDetailsLoaded(WebBookParser.ParsedBook book, java.util.List<WebBookParser.Chapter> chapters) {
                runOnUiThread(() -> {
                    bookTitle = book.title;
                    bookAuthor = book.author;
                    bookCoverUrl = book.coverUrl;

                    titleTextView.setText(book.title);
                    authorTextView.setText(book.author);
                    chaptersTextView.setText("Глав: " + book.chapters);
                    descriptionTextView.setText(book.description);

                    if (book.coverUrl != null && !book.coverUrl.isEmpty()) {
                        Glide.with(BookDetailsActivity.this)
                                .load(book.coverUrl)
                                .placeholder(R.drawable.ic_book_placeholder)
                                .into(coverImageView);
                    }

                    downloadButton.setOnClickListener(v -> checkDownloadOptions());
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(BookDetailsActivity.this,
                            "Ошибка: " + error,
                            Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }

    private void checkDownloadOptions() {
        String downloadCheckUrl = "https://ranobe.me/ranobe" + bookId + "?action=download";

        new Thread(() -> {
            try {
                Document doc = Jsoup.connect(downloadCheckUrl)
                        .userAgent("Mozilla/5.0")
                        .timeout(10000)
                        .get();

                // Проверяем, есть ли разбивка на тома
                Elements volumeLinks = doc.select("a[href*=section_fictofile_download]");

                if (volumeLinks.size() > 2) {
                    // Есть тома - парсим их
                    runOnUiThread(() -> showVolumeSelection(doc));
                } else {
                    // Обычное скачивание - epub или fb2
                    runOnUiThread(() -> showFormatSelection());
                }

            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Ошибка проверки скачивания", Toast.LENGTH_SHORT).show();
                    // Fallback на обычное скачивание
                    showFormatSelection();
                });
            }
        }).start();
    }

    private void showFormatSelection() {
        new AlertDialog.Builder(this)
                .setTitle("Выберите формат")
                .setItems(new String[]{"EPUB", "FB2"}, (dialog, which) -> {
                    String format = which == 0 ? "epub" : "fb2";
                    String downloadUrl = "https://ranobe.me/section_fictofile_download.php?id=" +
                            bookId + "&format=" + format;
                    downloadFile(downloadUrl, format);
                })
                .show();
    }

    private void showVolumeSelection(Document doc) {
        Elements volumeTexts = doc.select(".ContentTable b")
                .stream()
                .filter(e -> e.text().matches("\\d+\\s*-\\s*\\d+"))
                .collect(Collectors.toCollection(Elements::new));

        Elements epubLinks = doc.select(".ContentTable a[href*=format=epub]");

        if (volumeTexts.isEmpty() || epubLinks.isEmpty()) {
            showFormatSelection();
            return;
        }

        String[] volumes = new String[volumeTexts.size()];
        for (int i = 0; i < volumeTexts.size(); i++) {
            volumes[i] = "Главы " + volumeTexts.get(i).text();
        }

        new AlertDialog.Builder(this)
                .setTitle("Выберите том")
                .setItems(volumes, (dialog, which) -> {
                    int volumePart = which + 1;
                    showFormatSelectionForVolume(volumePart);
                })
                .show();
    }


    private void showFormatSelectionForVolume(int part) {
        new AlertDialog.Builder(this)
                .setTitle("Выберите формат")
                .setItems(new String[]{"EPUB", "FB2"}, (dialog, which) -> {
                    String format = which == 0 ? "epub" : "fb2";
                    String downloadUrl = "https://ranobe.me/section_fictofile_download.php?id=" +
                            bookId + "&format=" + format + "&part=" + part;
                    downloadFile(downloadUrl, format);
                })
                .show();
    }

    private void downloadFile(String url, String format) {
        try {
            bookFormat = format;
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.addRequestHeader("Referer", "https://ranobe.me/");
            request.setTitle("Скачивание книги");
            request.setDescription("Формат: " + format.toUpperCase());
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,
                    "ranobe_" + bookId + "." + format);

            DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);

            lastDownloadId = manager.enqueue(request);

            Toast.makeText(this, "Скачивание начато", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(this, "Ошибка скачивания: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onResume() {
        super.onResume();

        if (!receiverRegistered) {
            registerReceiver(
                    downloadReceiver,
                    new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            );
            receiverRegistered = true;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (receiverRegistered) {
            unregisterReceiver(downloadReceiver);
            receiverRegistered = false;
        }
    }


    private final BroadcastReceiver downloadReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            long id = intent.getLongExtra(
                    DownloadManager.EXTRA_DOWNLOAD_ID, -1
            );

            if (id != lastDownloadId) return;

            DownloadManager dm =
                    (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);

            DownloadManager.Query query =
                    new DownloadManager.Query().setFilterById(id);

            try (Cursor cursor = dm.query(query)) {
                if (cursor != null && cursor.moveToFirst()) {

                    int status = cursor.getInt(
                            cursor.getColumnIndexOrThrow(
                                    DownloadManager.COLUMN_STATUS
                            )
                    );

                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        String uri = cursor.getString(
                                cursor.getColumnIndexOrThrow(
                                        DownloadManager.COLUMN_LOCAL_URI
                                )
                        );

                        addBookToDatabase(uri);
                    }
                }
            }
        }
    };

    private void addBookToDatabase(String fileUri) {
        new Thread(() -> {
            try {
                String initialFilePath = Uri.parse(fileUri).getPath();
                File file = new File(initialFilePath);
                
                if (!file.exists()) {
                    final String errorPath = initialFilePath;
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Файл не найден: " + errorPath, Toast.LENGTH_SHORT).show();
                    });
                    return;
                }
                
                // Определяем реальный тип файла по содержимому
                String realFileType = FileUtils.detectFileTypeByContent(initialFilePath);
                String actualFormat = bookFormat.toUpperCase();
                String finalFilePath = initialFilePath;
                
                // Если реальный тип отличается от ожидаемого, исправляем
                if (!"UNKNOWN".equals(realFileType) && !realFileType.equals(actualFormat)) {
                    android.util.Log.w("BookDetailsActivity", 
                        "File type mismatch! Expected: " + actualFormat + ", but detected: " + realFileType);
                    
                    // Переименовываем файл с правильным расширением
                    String fileName = file.getName();
                    int lastDot = fileName.lastIndexOf('.');
                    String baseName = (lastDot > 0) ? fileName.substring(0, lastDot) : fileName;
                    String newFileName = baseName + "." + realFileType.toLowerCase();
                    File newFile = new File(file.getParent(), newFileName);
                    
                    if (file.renameTo(newFile)) {
                        finalFilePath = newFile.getAbsolutePath();
                        actualFormat = realFileType;
                        android.util.Log.d("BookDetailsActivity", 
                            "File renamed from " + fileName + " to " + newFileName);
                    } else {
                        android.util.Log.w("BookDetailsActivity", 
                            "Failed to rename file, using detected type anyway");
                        actualFormat = realFileType;
                    }
                }
                
                // Если тип все еще неизвестен, используем ожидаемый формат
                if ("UNKNOWN".equals(actualFormat)) {
                    actualFormat = bookFormat.toUpperCase();
                }
                
                final String finalFormat = actualFormat;
                final String finalFile = finalFilePath;
                
                Book book = new Book(bookTitle, bookAuthor, finalFile, finalFormat);
                
                // Сохраняем обложку
                if (bookCoverUrl != null && !bookCoverUrl.isEmpty()) {
                    book.setCoverUrl(bookCoverUrl);
                }
                
                // Устанавливаем размер файла
                File fileToCheck = new File(finalFile);
                if (fileToCheck.exists()) {
                    book.setFileSize(fileToCheck.length());
                }

                BookRepository repository = new BookRepository(this);

                repository.insertBook(book, insertedId -> runOnUiThread(() -> {
                    String message = "Книга добавлена в библиотеку";
                    if (!finalFormat.equals(bookFormat.toUpperCase())) {
                        message += " (формат: " + finalFormat + ")";
                    }
                    
                    Toast.makeText(
                            BookDetailsActivity.this,
                            message,
                            Toast.LENGTH_SHORT
                    ).show();

                    setResult(RESULT_OK);
                    finish();
                }));
                
            } catch (Exception e) {
                android.util.Log.e("BookDetailsActivity", "Error adding book to database", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Ошибка при добавлении книги: " + e.getMessage(), 
                        Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }
}