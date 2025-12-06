package com.example.readerhub.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.readerhub.R;
import com.example.readerhub.adapters.BookAdapter;
import com.example.readerhub.models.Book;
import com.example.readerhub.repository.BookRepository;
import com.example.readerhub.utils.PreferencesManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;

public class MainActivity extends AppCompatActivity implements BookAdapter.OnBookClickListener {

    private static final int PERMISSION_REQUEST_CODE = 100;

    private RecyclerView recyclerView;
    private BookAdapter adapter;
    private BookRepository repository;
    private PreferencesManager prefsManager;
    private TabLayout tabLayout;
    private FloatingActionButton fabAddBook;

    private String currentFilter = "all"; // all, recent, favorites

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefsManager = new PreferencesManager(this);
        repository = new BookRepository(this);

        // Check if user is logged in
        if (!prefsManager.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        initViews();
        checkPermissions();
        loadBooks();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        tabLayout = findViewById(R.id.tabLayout);
        fabAddBook = findViewById(R.id.fabAddBook);

        // Setup RecyclerView
        adapter = new BookAdapter(this, this);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.setAdapter(adapter);

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

    public boolean onQueryTextChange(String newText) {
        if (newText.isEmpty()) {
            loadBooks();
        }
        return true;
    }

    @Override
    public void onBookClick(Book book) {
        Intent intent;
        switch (book.getFileType()) {
            case "EPUB":
                intent = new Intent(this, EpubReaderActivity.class);
                break;
            case "PDF":
                intent = new Intent(this, PdfReaderActivity.class);
                break;
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
        new AlertDialog.Builder(this)
                .setTitle(book.getTitle())
                .setItems(new String[]{"Delete", "Mark as Favorite", "Book Details"},
                        (dialog, which) -> {
                            switch (which) {
                                case 0:
                                    deleteBook(book);
                                    break;
                                case 1:
                                    toggleFavorite(book);
                                    break;
                                case 2:
                                    showBookDetails(book);
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
                    repository.deleteBook(book);
                    loadBooks();
                    Toast.makeText(this, "Book deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void toggleFavorite(Book book) {
        book.setFavorite(!book.isFavorite());
        repository.updateFavoriteStatus(book.getId(), book.isFavorite());
        loadBooks();
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
        startActivityForResult(intent, 1);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchBooks(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.isEmpty()) {
                    loadBooks();
                }
                return true;
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if (id == R.id.action_logout) {
            logout();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void searchBooks(String query) {
        repository.searchBooks(query, books -> runOnUiThread(() -> adapter.setBooks(books)));
    }

    private void logout() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    prefsManager.clearUserSession();
                    startActivity(new Intent(this, LoginActivity.class));
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}