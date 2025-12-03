package com.example.readerhub.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.readerhub.models.ReadingHistory;

import java.util.List;

/**
 * Dao для модели История чтения
 */
@Dao
public interface ReadingHistoryDao {

    @Insert
    long insert(ReadingHistory history);

    @Query("SELECT * FROM reading_history WHERE bookId = :bookId ORDER BY timestamp DESC")
    List<ReadingHistory> getHistoryForBook(int bookId);

    @Query("SELECT * FROM reading_history ORDER BY timestamp DESC LIMIT 50")
    List<ReadingHistory> getRecentHistory();

    @Query("SELECT SUM(readingDuration) FROM reading_history WHERE bookId = :bookId")
    int getTotalReadingTime(int bookId);

    @Query("DELETE FROM reading_history WHERE bookId = :bookId")
    void deleteHistoryForBook(int bookId);

    @Query("DELETE FROM reading_history WHERE timestamp < :timestamp")
    void deleteOldHistory(long timestamp);
}