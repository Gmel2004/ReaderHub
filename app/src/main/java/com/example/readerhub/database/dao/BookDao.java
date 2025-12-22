package com.example.readerhub.database.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.readerhub.models.Book;

import java.util.List;

/**
 * Dao для модели Книга
 */

@Dao
public interface BookDao {

    @Insert
    long insert(Book book);

    @Update
    void update(Book book);

    @Delete
    void delete(Book book);

    @Query("SELECT * FROM book ORDER BY lastOpened DESC")
    List<Book> getAllBooks();

    @Query("SELECT * FROM book WHERE id = :bookId")
    Book getBookById(int bookId);

    @Query("SELECT * FROM book WHERE isFavorite = 1 ORDER BY title ASC")
    List<Book> getFavoriteBooks();

    @Query("SELECT * FROM book ORDER BY lastOpened DESC LIMIT 10")
    List<Book> getRecentBooks();

    @Query("SELECT * FROM book WHERE title LIKE '%' || :query || '%' OR author LIKE '%' || :query || '%'")
    List<Book> searchBooks(String query);

    @Query("UPDATE book SET currentPage = :page, lastOpened = :timestamp WHERE id = :bookId")
    void updateReadingProgress(int bookId, int page, long timestamp);

    @Query("UPDATE book SET isFavorite = :isFavorite WHERE id = :bookId")
    void updateFavoriteStatus(int bookId, boolean isFavorite);

    @Query("DELETE FROM book")
    void deleteAll();
}