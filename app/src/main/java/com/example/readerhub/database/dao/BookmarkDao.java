package com.example.readerhub.database.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.readerhub.models.Bookmark;

import java.util.List;

/**
 * Dao для модели Книжная закладка
 */
@Dao
public interface BookmarkDao {

    @Insert
    long insert(Bookmark bookmark);

    @Update
    void update(Bookmark bookmark);

    @Delete
    void delete(Bookmark bookmark);

    @Query("SELECT * FROM bookmark WHERE bookId = :bookId ORDER BY pageNumber ASC")
    List<Bookmark> getBookmarksForBook(int bookId);

    @Query("SELECT * FROM bookmark WHERE id = :bookmarkId")
    Bookmark getBookmarkById(int bookmarkId);

    @Query("DELETE FROM bookmark WHERE bookId = :bookId")
    void deleteBookmarksForBook(int bookId);

    @Query("SELECT COUNT(*) FROM bookmark WHERE bookId = :bookId AND pageNumber = :pageNumber")
    int isPageBookmarked(int bookId, int pageNumber);
}
