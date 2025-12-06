package com.example.readerhub.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.readerhub.R;
import com.example.readerhub.models.Book;
import com.example.readerhub.models.Bookmark;
import com.example.readerhub.models.ReadingHistory;
import com.example.readerhub.repository.BookRepository;
import com.example.readerhub.utils.PreferencesManager;
import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener;
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle;

import java.io.File;

public class PdfReaderActivity extends AppCompatActivity
        implements OnPageChangeListener, OnLoadCompleteListener {

    private PDFView pdfView;
    private BookRepository repository;
    private PreferencesManager prefsManager;
    private int bookId;
    private Book currentBook;
    private int currentPage = 0;
    private int totalPages = 0;
    private long readingStartTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_reader);

        bookId = getIntent().getIntExtra("bookId", -1);
        if (bookId == -1) {
            Toast.makeText(this, "Error loading book", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        pdfView = findViewById(R.id.pdfView);
        repository = new BookRepository(this);
        prefsManager = new PreferencesManager(this);

        loadBookAndOpen();
    }

    private void loadBookAndOpen() {
        repository.getBookById(bookId, book -> {
            if (book != null) {
                currentBook = book;
                runOnUiThread(this::openPdfReader);
            } else {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Book not found", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }

    private void openPdfReader() {
        File pdfFile = new File(currentBook.getFilePath());

        if (!pdfFile.exists()) {
            Toast.makeText(this, "File not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        readingStartTime = System.currentTimeMillis();

        pdfView.fromFile(pdfFile)
                .defaultPage(currentBook.getCurrentPage())
                .onPageChange(this)
                .onLoad(this)
                .enableSwipe(true)
                .swipeHorizontal(false)
                .enableDoubletap(true)
                .nightMode(prefsManager.isNightMode())
                .scrollHandle(new DefaultScrollHandle(this))
                .spacing(10)
                .load();
    }

    @Override
    public void onPageChanged(int page, int pageCount) {
        currentPage = page;
        setTitle("Page " + (page + 1) + " of " + pageCount);

        // Save progress every 5 pages
        if (page % 5 == 0) {
            repository.updateReadingProgress(bookId, page);
        }
    }

    @Override
    public void loadComplete(int nbPages) {
        totalPages = nbPages;
        currentBook.setTotalPages(nbPages);
        repository.updateBook(currentBook);

        Toast.makeText(this, "Loaded " + nbPages + " pages", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_reader, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_bookmark) {
            addBookmark();
            return true;
        } else if (id == R.id.action_night_mode) {
            toggleNightMode();
            return true;
        } else if (id == R.id.action_brightness) {
            // Show brightness dialog
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void addBookmark() {
        Bookmark bookmark = new Bookmark(bookId, currentPage, "Page " + (currentPage + 1));
        repository.insertBookmark(bookmark, bookmarkId ->
                runOnUiThread(() ->
                        Toast.makeText(this, "Bookmark added", Toast.LENGTH_SHORT).show()
                )
        );
    }

    private void toggleNightMode() {
        boolean isNightMode = !prefsManager.isNightMode();
        prefsManager.setNightMode(isNightMode);

        if (isNightMode) {
            pdfView.setBackgroundColor(Color.BLACK);
        } else {
            pdfView.setBackgroundColor(Color.WHITE);
        }
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
            repository.updateReadingProgress(bookId, currentPage);

            long readingEndTime = System.currentTimeMillis();
            int duration = (int) ((readingEndTime - readingStartTime) / 1000);

            if (duration > 0) {
                ReadingHistory history = new ReadingHistory(bookId, currentPage, duration);
                repository.insertReadingHistory(history);
            }
        }
    }
}