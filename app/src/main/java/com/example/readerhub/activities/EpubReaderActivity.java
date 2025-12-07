//package com.example.readerhub.activities;
//
//import android.os.Bundle;
//import android.widget.Toast;
//
//import androidx.appcompat.app.AppCompatActivity;
//
//import com.example.readerhub.models.Book;
//import com.example.readerhub.models.ReadingHistory;
//import com.example.readerhub.repository.BookRepository;
//import com.example.readerhub.utils.PreferencesManager;
//import com.folioreader.Config;
//import com.folioreader.FolioReader;
//import com.folioreader.model.HighLight;
//import com.folioreader.model.locators.ReadLocator;
//import com.folioreader.util.OnHighlightListener;
//import com.folioreader.util.ReadLocatorListener;
//
//public class EpubReaderActivity extends AppCompatActivity
//        implements OnHighlightListener, ReadLocatorListener {
//
//    private FolioReader folioReader;
//    private BookRepository repository;
//    private PreferencesManager prefsManager;
//    private int bookId;
//    private Book currentBook;
//    private long readingStartTime;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//
//        bookId = getIntent().getIntExtra("bookId", -1);
//        if (bookId == -1) {
//            Toast.makeText(this, "Error loading book", Toast.LENGTH_SHORT).show();
//            finish();
//            return;
//        }
//
//        repository = new BookRepository(this);
//        prefsManager = new PreferencesManager(this);
//        folioReader = FolioReader.get();
//
//        loadBookAndOpen();
//    }
//
//    private void loadBookAndOpen() {
//        repository.getBookById(bookId, book -> {
//            if (book != null) {
//                currentBook = book;
//                runOnUiThread(this::openEpubReader);
//            } else {
//                runOnUiThread(() -> {
//                    Toast.makeText(this, "Book not found", Toast.LENGTH_SHORT).show();
//                    finish();
//                });
//            }
//        });
//    }
//
//    private void openEpubReader() {
//        Config config = new Config();
//        config.setAllowedDirection(Config.AllowedDirection.VERTICAL_AND_HORIZONTAL);
//        config.setShowTts(true);
//        config.setNightMode(prefsManager.isNightMode());
//        config.setFontSize(prefsManager.getFontSize());
//
//        // Fonts: 0=sans-serif, 1=serif, 2=monospace
//        String fontFamily = prefsManager.getFontFamily();
//        if (fontFamily.equals("serif")) {
//            config.setFont(1);
//        } else if (fontFamily.equals("monospace")) {
//            config.setFont(2);
//        } else {
//            config.setFont(0); // sans-serif default
//        }
//
//        folioReader.setConfig(config, true);
//        folioReader.setOnHighlightListener(this);
//        folioReader.setReadLocatorListener(this);
//
//        readingStartTime = System.currentTimeMillis();
//
//        folioReader.openBook(currentBook.getFilePath());
//    }
//
//    @Override
//    public void onHighlight(HighLight highlight, HighLight.HighLightAction type) {
//        // Handle highlights/bookmarks
//        if (type == HighLight.HighLightAction.NEW) {
//            Toast.makeText(this, "Bookmark added", Toast.LENGTH_SHORT).show();
//        } else if (type == HighLight.HighLightAction.DELETE) {
//            Toast.makeText(this, "Bookmark removed", Toast.LENGTH_SHORT).show();
//        }
//    }
//
//    @Override
//    public void saveReadLocator(ReadLocator readLocator) {
//        // Save reading progress
//        if (currentBook != null && readLocator != null) {
//            try {
//                String[] parts = readLocator.getHref().split("/");
//                int currentPage = Integer.parseInt(parts[parts.length - 1].replaceAll("[^0-9]", ""));
//
//                repository.updateReadingProgress(bookId, currentPage);
//
//                // Save reading history
//                long readingEndTime = System.currentTimeMillis();
//                int duration = (int) ((readingEndTime - readingStartTime) / 1000);
//
//                ReadingHistory history = new ReadingHistory(bookId, currentPage, duration);
//                repository.insertReadingHistory(history);
//
//                readingStartTime = readingEndTime;
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//    }
//
//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        FolioReader.clear();
//    }
//}