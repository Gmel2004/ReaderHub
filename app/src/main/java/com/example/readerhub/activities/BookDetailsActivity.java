package com.example.readerhub.activities;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.readerhub.R;
import com.example.readerhub.adapters.BookmarkAdapter;
import com.example.readerhub.repository.BookRepository;

public class BookDetailsActivity extends AppCompatActivity {

    private TextView titleTextView, authorTextView, pagesTextView, progressTextView;
    private RecyclerView bookmarksRecyclerView;
    private BookRepository repository;
    private BookmarkAdapter bookmarkAdapter;
    private int bookId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_details);

        bookId = getIntent().getIntExtra("bookId", -1);
        repository = new BookRepository(this);

        titleTextView = findViewById(R.id.titleTextView);
        authorTextView = findViewById(R.id.authorTextView);
        pagesTextView = findViewById(R.id.pagesTextView);
        progressTextView = findViewById(R.id.progressTextView);
        bookmarksRecyclerView = findViewById(R.id.bookmarksRecyclerView);

        bookmarkAdapter = new BookmarkAdapter(this, bookmark -> {
            repository.deleteBookmark(bookmark);
            loadBookmarks();
        });

        bookmarksRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        bookmarksRecyclerView.setAdapter(bookmarkAdapter);

        loadBookDetails();
        loadBookmarks();
    }

    private void loadBookDetails() {
        repository.getBookById(bookId, book -> runOnUiThread(() -> {
            if (book != null) {
                titleTextView.setText(book.getTitle());
                authorTextView.setText(book.getAuthor());
                pagesTextView.setText("Pages: " + book.getTotalPages());

                int progress = book.getTotalPages() > 0 ?
                        (int)((book.getCurrentPage() / (float)book.getTotalPages()) * 100) : 0;
                progressTextView.setText("Progress: " + progress + "%");
            }
        }));
    }

    private void loadBookmarks() {
        repository.getBookmarksForBook(bookId, bookmarks ->
                runOnUiThread(() -> bookmarkAdapter.setBookmarks(bookmarks))
        );
    }
}