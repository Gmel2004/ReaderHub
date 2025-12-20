package com.example.readerhub.activities;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.example.readerhub.R;
import com.example.readerhub.parsers.WebBookParser;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class BookDetailsActivity extends AppCompatActivity {

    private ImageView coverImageView;
    private TextView titleTextView, authorTextView, chaptersTextView, descriptionTextView;
    private Button downloadButton;
    private String bookUrl;
    private String bookId;
    private WebBookParser parser;

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
                        .referrer("ranobe.me")
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
        // Парсим тома
        Elements volumeTexts = doc.select("b");
        Elements epubLinks = doc.select("a[href*=format=epub]");

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
                    // После выбора тома - выбираем формат
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
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setTitle("Скачивание книги");
            request.setDescription("Формат: " + format.toUpperCase());
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,
                    "ranobe_" + bookId + "." + format);

            DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            manager.enqueue(request);

            Toast.makeText(this, "Скачивание начато", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(this, "Ошибка скачивания: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}