package com.example.readerhub.repository;

import android.content.Context;

import com.example.readerhub.database.AppDatabase;
import com.example.readerhub.database.dao.BookDao;
import com.example.readerhub.database.dao.BookmarkDao;
import com.example.readerhub.database.dao.ReadingHistoryDao;
import com.example.readerhub.models.Book;
import com.example.readerhub.models.Bookmark;
import com.example.readerhub.models.ReadingHistory;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BookRepository {

    private BookDao bookDao;
    private BookmarkDao bookmarkDao;
    private ReadingHistoryDao historyDao;
    private ExecutorService executorService;

    public BookRepository(Context context) {
        AppDatabase database = AppDatabase.getInstance(context);
        bookDao = database.bookDao();
        bookmarkDao = database.bookmarkDao();
        historyDao = database.readingHistoryDao();
        executorService = Executors.newFixedThreadPool(4);
    }

    // Book operations
    public void insertBook(Book book, OnBookInsertedListener listener) {
        executorService.execute(() -> {
            long id = bookDao.insert(book);
            if (listener != null) {
                listener.onBookInserted(id);
            }
        });
    }

    public void updateBook(Book book) {
        executorService.execute(() -> bookDao.update(book));
    }

    public void deleteBook(Book book) {
        executorService.execute(() -> bookDao.delete(book));
    }

    public void getAllBooks(OnBooksLoadedListener listener) {
        executorService.execute(() -> {
            List<Book> books = bookDao.getAllBooks();
            if (listener != null) {
                listener.onBooksLoaded(books);
            }
        });
    }

    public void getBookById(int bookId, OnBookLoadedListener listener) {
        executorService.execute(() -> {
            Book book = bookDao.getBookById(bookId);
            if (listener != null) {
                listener.onBookLoaded(book);
            }
        });
    }

    public void getFavoriteBooks(OnBooksLoadedListener listener) {
        executorService.execute(() -> {
            List<Book> books = bookDao.getFavoriteBooks();
            if (listener != null) {
                listener.onBooksLoaded(books);
            }
        });
    }

    public void getRecentBooks(OnBooksLoadedListener listener) {
        executorService.execute(() -> {
            List<Book> books = bookDao.getRecentBooks();
            if (listener != null) {
                listener.onBooksLoaded(books);
            }
        });
    }

    public void searchBooks(String query, OnBooksLoadedListener listener) {
        executorService.execute(() -> {
            List<Book> books = bookDao.searchBooks(query);
            if (listener != null) {
                listener.onBooksLoaded(books);
            }
        });
    }

    public void updateReadingProgress(int bookId, int page) {
        executorService.execute(() ->
                bookDao.updateReadingProgress(bookId, page, System.currentTimeMillis())
        );
    }

    public void updateFavoriteStatus(int bookId, boolean isFavorite) {
        executorService.execute(() -> bookDao.updateFavoriteStatus(bookId, isFavorite));
    }

    // Bookmark operations
    public void insertBookmark(Bookmark bookmark, OnBookmarkInsertedListener listener) {
        executorService.execute(() -> {
            long id = bookmarkDao.insert(bookmark);
            if (listener != null) {
                listener.onBookmarkInserted(id);
            }
        });
    }

    public void deleteBookmark(Bookmark bookmark) {
        executorService.execute(() -> bookmarkDao.delete(bookmark));
    }

    public void getBookmarksForBook(int bookId, OnBookmarksLoadedListener listener) {
        executorService.execute(() -> {
            List<Bookmark> bookmarks = bookmarkDao.getBookmarksForBook(bookId);
            if (listener != null) {
                listener.onBookmarksLoaded(bookmarks);
            }
        });
    }

    // Reading history operations
    public void insertReadingHistory(ReadingHistory history) {
        executorService.execute(() -> historyDao.insert(history));
    }

    public void getHistoryForBook(int bookId, OnHistoryLoadedListener listener) {
        executorService.execute(() -> {
            List<ReadingHistory> history = historyDao.getHistoryForBook(bookId);
            if (listener != null) {
                listener.onHistoryLoaded(history);
            }
        });
    }

    // Callback interfaces
    public interface OnBookInsertedListener {
        void onBookInserted(long bookId);
    }

    public interface OnBookLoadedListener {
        void onBookLoaded(Book book);
    }

    public interface OnBooksLoadedListener {
        void onBooksLoaded(List<Book> books);
    }

    public interface OnBookmarkInsertedListener {
        void onBookmarkInserted(long bookmarkId);
    }

    public interface OnBookmarksLoadedListener {
        void onBookmarksLoaded(List<Bookmark> bookmarks);
    }

    public interface OnHistoryLoadedListener {
        void onHistoryLoaded(List<ReadingHistory> history);
    }
}
