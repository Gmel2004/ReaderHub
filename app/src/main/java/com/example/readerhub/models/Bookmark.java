package com.example.readerhub.models;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

/**
 * Модель сущности Книжная закладка
 */
@Entity(tableName = "bookmark",
        foreignKeys = @ForeignKey(entity = Book.class,
                parentColumns = "id",
                childColumns = "bookId",
                onDelete = ForeignKey.CASCADE))
public class Bookmark {
    @PrimaryKey(autoGenerate = true)
    private int id;

    private int bookId;
    private int pageNumber;
    private String chapterName;
    private String note;
    private long createdAt;

    public Bookmark(int bookId, int pageNumber, String chapterName) {
        this.bookId = bookId;
        this.pageNumber = pageNumber;
        this.chapterName = chapterName;
        this.createdAt = System.currentTimeMillis();
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

    public String getChapterName() {
        return chapterName;
    }

    public void setChapterName(String chapterName) {
        this.chapterName = chapterName;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
