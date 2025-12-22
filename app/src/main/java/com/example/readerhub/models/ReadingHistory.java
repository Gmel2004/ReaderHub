package com.example.readerhub.models;

/**
 * Модель сущности История чтения
 */

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(tableName = "reading_history",
        foreignKeys = @ForeignKey(entity = Book.class,
                parentColumns = "id",
                childColumns = "bookId",
                onDelete = ForeignKey.CASCADE))
public class ReadingHistory {
    @PrimaryKey(autoGenerate = true)
    private int id;

    private int bookId;
    private int pageNumber;
    private long timestamp;
    private int readingDuration; // in seconds

    public ReadingHistory(int bookId, int pageNumber, int readingDuration) {
        this.bookId = bookId;
        this.pageNumber = pageNumber;
        this.readingDuration = readingDuration;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getBookId() {
        return bookId;
    }

    public void setBookId(int bookId) {
        this.bookId = bookId;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getReadingDuration() {
        return readingDuration;
    }

    public void setReadingDuration(int readingDuration) {
        this.readingDuration = readingDuration;
    }
}
