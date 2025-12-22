package com.example.readerhub.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.net.Uri;
import android.os.Build;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.example.readerhub.R;
import com.example.readerhub.adapters.BookAdapter;
import com.example.readerhub.models.Book;
import com.example.readerhub.repository.BookRepository;
import com.example.readerhub.utils.FileUtils;
import com.example.readerhub.utils.PreferencesManager;
import com.example.readerhub.utils.BookCoverExtractor;
import com.example.readerhub.parsers.Fb2Parser;
import com.example.readerhub.parsers.EpubParser;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;

import java.io.File;
import java.util.List;

public class MainActivity extends AppCompatActivity implements BookAdapter.OnBookClickListener {

    private static final int PERMISSION_REQUEST_CODE = 100;

    private RecyclerView recyclerView;
    private BookAdapter adapter;
    private BookRepository repository;
    private PreferencesManager prefsManager;
    private TabLayout tabLayout;
    private FloatingActionButton fabAddBook;
    private ImageView userIconImageView;
    private TextView userStatusTextView;
    private androidx.appcompat.widget.Toolbar toolbar;
    private SearchView searchView;

    private String currentFilter = "all"; // all, recent, favorites
    
    private ActivityResultLauncher<Intent> filePickerLauncher;

    @Override
    protected void onResume() {
        super.onResume();
        loadBooks();
        updateUserStatus();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefsManager = new PreferencesManager(this);
        repository = new BookRepository(this);

        initViews();
        setupFilePicker();
        checkPermissions();
        loadBooks();
    }
    
    private void setupFilePicker() {
        filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        handleSelectedFile(uri);
                    }
                }
            }
        );
    }
    
    private void handleSelectedFile(Uri uri) {
        new Thread(() -> {
            try {
                String fileName = FileUtils.getFileName(this, uri);
                String extension = FileUtils.getFileExtension(fileName);
                String fileType = FileUtils.getFileType(extension);
                
                if ("UNKNOWN".equals(fileType)) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Unsupported file format", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }
                
                // Копируем файл во внутреннее хранилище
                File destFile = FileUtils.copyFileToInternalStorage(this, uri, fileName);
                if (destFile == null) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Error copying file", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }
                
                // Определяем реальный тип файла по содержимому
                String realFileType = FileUtils.detectFileTypeByContent(destFile.getAbsolutePath());
                if (!"UNKNOWN".equals(realFileType) && !realFileType.equals(fileType)) {
                    android.util.Log.w("MainActivity", "File type mismatch! Extension says: " + fileType + ", but content is: " + realFileType);
                    fileType = realFileType; // Используем реальный тип
                    
                    // Исправляем расширение файла если нужно
                    if (!fileName.toLowerCase().endsWith("." + realFileType.toLowerCase())) {
                        String newFileName = fileName.substring(0, fileName.lastIndexOf('.')) + "." + realFileType.toLowerCase();
                        File newFile = new File(destFile.getParent(), newFileName);
                        if (destFile.renameTo(newFile)) {
                            destFile = newFile;
                            fileName = newFileName;
                        }
                    }
                }
                
                // Извлекаем метаданные и обложку
                String title = fileName.replace("." + extension, "");
                String author = "Unknown Author";
                String coverPath = null;
                
                // Пытаемся извлечь обложку
                coverPath = BookCoverExtractor.extractCover(this, destFile.getAbsolutePath(), fileType);
                
                // Для EPUB можно попробовать извлечь метаданные
                if ("EPUB".equals(fileType)) {
                    try {
                        android.util.Log.d("MainActivity", "Parsing EPUB: " + destFile.getAbsolutePath());
                        EpubParser.EpubBook epubBook = EpubParser.parse(destFile.getAbsolutePath());
                        if (epubBook != null) {
                            android.util.Log.d("MainActivity", "EPUB parsed - title: " + epubBook.title + ", author: " + epubBook.author + ", chapters: " + epubBook.chapters.size());
                            if (epubBook.title != null && !epubBook.title.isEmpty()) {
                                title = epubBook.title;
                            }
                            if (epubBook.author != null && !epubBook.author.isEmpty()) {
                                author = epubBook.author;
                            }
                            // Если нет глав, возможно файл поврежден
                            if (epubBook.chapters.isEmpty()) {
                                android.util.Log.w("MainActivity", "EPUB has no chapters!");
                            }
                        } else {
                            android.util.Log.w("MainActivity", "EPUB parsing returned null");
                        }
                    } catch (Exception e) {
                        android.util.Log.e("MainActivity", "Error parsing EPUB", e);
                        e.printStackTrace();
                    }
                }
                
                // Для FB2 можно попробовать извлечь метаданные
                if ("FB2".equals(fileType)) {
                    try {
                        android.util.Log.d("MainActivity", "Parsing FB2: " + destFile.getAbsolutePath());
                        Fb2Parser.Fb2Book fb2Book = Fb2Parser.parse(destFile.getAbsolutePath());
                        if (fb2Book != null) {
                            android.util.Log.d("MainActivity", "FB2 parsed - title: " + fb2Book.title + ", author: " + fb2Book.author + ", chapters: " + fb2Book.chapters.size());
                            if (fb2Book.title != null && !fb2Book.title.isEmpty() && !fb2Book.title.equals("null")) {
                                title = fb2Book.title;
                            }
                            if (fb2Book.author != null && !fb2Book.author.isEmpty() && !fb2Book.author.equals("null")) {
                                author = fb2Book.author;
                            }
                            if (fb2Book.chapters.isEmpty()) {
                                android.util.Log.w("MainActivity", "FB2 has no chapters!");
                            }
                        } else {
                            android.util.Log.w("MainActivity", "FB2 parsing returned null");
                        }
                    } catch (Exception e) {
                        android.util.Log.e("MainActivity", "Error parsing FB2", e);
                        e.printStackTrace();
                    }
                }
                
                // Создаем книгу
                Book book = new Book(title, author, destFile.getAbsolutePath(), fileType);
                book.setFileSize(destFile.length());
                if (coverPath != null) {
                    book.setCoverUrl("file://" + coverPath);
                }
                
                // Добавляем в БД
                repository.insertBook(book, bookId -> runOnUiThread(() -> {
                    Toast.makeText(this, "Book added successfully", Toast.LENGTH_SHORT).show();
                    loadBooks();
                }));
                
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        recyclerView = findViewById(R.id.recyclerView);
        tabLayout = findViewById(R.id.tabLayout);
        fabAddBook = findViewById(R.id.fabAddBook);
        userIconImageView = findViewById(R.id.userIconImageView);
        userStatusTextView = findViewById(R.id.userStatusTextView);
        searchView = findViewById(R.id.searchView);

        // Setup RecyclerView
        adapter = new BookAdapter(this, this);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.setAdapter(adapter);
        
        // Setup user icon click listener
        userIconImageView.setOnClickListener(v -> showUserMenu());
        
        // Setup search view
        setupSearchView();
        
        // Update user status
        updateUserStatus();

        // Setup tabs
        tabLayout.addTab(tabLayout.newTab().setText("All Books"));
        tabLayout.addTab(tabLayout.newTab().setText("Recent"));
        tabLayout.addTab(tabLayout.newTab().setText("Favorites"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0:
                        currentFilter = "all";
                        loadBooks();
                        break;
                    case 1:
                        currentFilter = "recent";
                        loadRecentBooks();
                        break;
                    case 2:
                        currentFilter = "favorites";
                        loadFavoriteBooks();
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        // FAB click listener
        fabAddBook.setOnClickListener(v -> showAddBookDialog());
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE);
        }
    }

    private void loadBooks() {
        repository.getAllBooks(books -> runOnUiThread(() -> adapter.setBooks(books)));
    }

    private void loadRecentBooks() {
        repository.getRecentBooks(books -> runOnUiThread(() -> adapter.setBooks(books)));
    }

    private void loadFavoriteBooks() {
        repository.getFavoriteBooks(books -> runOnUiThread(() -> adapter.setBooks(books)));
    }

    @Override
    public void onBookClick(Book book) {
        Intent intent;
        switch (book.getFileType()) {
            case "EPUB":
                intent = new Intent(this, EpubReaderActivity.class);
                break;
//            case "PDF":
//                intent = new Intent(this, PdfReaderActivity.class);
//                break;
            case "FB2":
                intent = new Intent(this, Fb2ReaderActivity.class);
                break;
            default:
                Toast.makeText(this, "Unsupported file format", Toast.LENGTH_SHORT).show();
                return;
        }
        intent.putExtra("bookId", book.getId());
        startActivity(intent);
    }

    @Override
    public void onBookLongClick(Book book) {
        String[] options;
        if (book.isFavorite()) {
            options = new String[]{"Delete", "Remove from Favorites"};
        } else {
            options = new String[]{"Delete", "Mark as Favorite"};
        }
        
        new AlertDialog.Builder(this)
                .setTitle(book.getTitle())
                .setItems(options,
                        (dialog, which) -> {
                            switch (which) {
                                case 0:
                                    deleteBook(book);
                                    break;
                                case 1:
                                    toggleFavorite(book);
                                    break;
                            }
                        })
                .show();
    }

    private void deleteBook(Book book) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Book")
                .setMessage("Are you sure you want to delete this book?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    repository.deleteBook(book, () -> runOnUiThread(() -> {
                        // Обновляем GUI в зависимости от текущего фильтра после успешного удаления
                        refreshCurrentView();
                        Toast.makeText(this, "Book deleted", Toast.LENGTH_SHORT).show();
                    }));
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void refreshCurrentView() {
        switch (currentFilter) {
            case "all":
                loadBooks();
                break;
            case "recent":
                loadRecentBooks();
                break;
            case "favorites":
                loadFavoriteBooks();
                break;
            default:
                loadBooks();
        }
    }

    private void toggleFavorite(Book book) {
        boolean newFavoriteStatus = !book.isFavorite();
        book.setFavorite(newFavoriteStatus);
        repository.updateFavoriteStatus(book.getId(), newFavoriteStatus, () -> runOnUiThread(() -> {
            // Обновляем GUI после успешного обновления статуса избранного
            refreshCurrentView();
        }));
    }

    private void showBookDetails(Book book) {
        Intent intent = new Intent(this, BookDetailsActivity.class);
        intent.putExtra("bookId", book.getId());
        startActivity(intent);
    }

    private void showAddBookDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Add Book")
                .setItems(new String[]{"From Device", "From Web"},
                        (dialog, which) -> {
                            if (which == 0) {
                                openFileChooser();
                            } else {
                                startActivity(new Intent(this, WebParserActivity.class));
                            }
                        })
                .show();
    }

    private void openFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        String[] mimeTypes = {"application/epub+zip", "application/pdf", "application/x-fictionbook+xml"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        filePickerLauncher.launch(intent);
    }

    private void setupSearchView() {
        // Настройка цветов для SearchView на темном фоне
        int searchIconId = getResources().getIdentifier("android:id/search_mag_icon", null, null);
        if (searchIconId != 0) {
            ImageView searchIcon = searchView.findViewById(searchIconId);
            if (searchIcon != null) {
                searchIcon.setColorFilter(android.graphics.Color.WHITE);
            }
        }
        
        int searchCloseButtonId = getResources().getIdentifier("android:id/search_close_btn", null, null);
        if (searchCloseButtonId != 0) {
            ImageView closeButton = searchView.findViewById(searchCloseButtonId);
            if (closeButton != null) {
                closeButton.setColorFilter(android.graphics.Color.WHITE);
            }
        }
        
        int searchTextId = getResources().getIdentifier("android:id/search_src_text", null, null);
        if (searchTextId != 0) {
            TextView searchText = searchView.findViewById(searchTextId);
            if (searchText != null) {
                searchText.setTextColor(android.graphics.Color.WHITE);
                searchText.setHintTextColor(android.graphics.Color.parseColor("#CCFFFFFF"));
            }
        }
        
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchBooks(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.isEmpty()) {
                    refreshCurrentView();
                } else {
                    searchBooks(newText);
                }
                return true;
            }
        });
    }

    private void searchBooks(String query) {
        repository.searchBooks(query, books -> runOnUiThread(() -> adapter.setBooks(books)));
    }

    private void logout() {
        new AlertDialog.Builder(this)
                .setTitle("Выход")
                .setMessage("Вы уверены, что хотите выйти из аккаунта?")
                .setPositiveButton("Выйти", (dialog, which) -> {
                    prefsManager.clearUserSession();
                    updateUserStatus();
                    Toast.makeText(this, "Вы вышли из аккаунта", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }
    
    private void showUserMenu() {
        String[] options;
        if (prefsManager.isLoggedIn()) {
            options = new String[]{"Выйти из аккаунта"};
        } else {
            options = new String[]{"Войти", "Регистрация"};
        }
        
        new AlertDialog.Builder(this)
                .setTitle(prefsManager.isLoggedIn() ? "Аккаунт" : "Гость")
                .setItems(options, (dialog, which) -> {
                    if (prefsManager.isLoggedIn()) {
                        // Выход
                        logout();
                    } else {
                        // Вход или регистрация
                        if (which == 0) {
                            // Войти
                            startActivity(new Intent(this, LoginActivity.class));
                        } else {
                            // Регистрация
                            startActivity(new Intent(this, RegisterActivity.class));
                        }
                    }
                })
                .show();
    }
    
    private void updateUserStatus() {
        if (prefsManager.isLoggedIn()) {
            String username = prefsManager.getUsername();
            if (username != null && !username.isEmpty()) {
                userStatusTextView.setText(username);
            } else {
                userStatusTextView.setText("Пользователь");
            }
        } else {
            userStatusTextView.setText("Гость");
        }
    }
}