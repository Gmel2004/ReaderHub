package com.example.readerhub.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Модель сущности Книга
 */
@Entity(tableName = "book")
public class Book {
    @PrimaryKey(autoGenerate = true)
    private int id;

    private String title;
    private String author;
    private String filePath;
    private String fileType; // "EPUB", "PDF", "FB2"
    private String coverUrl;
    private long fileSize;
    private long dateAdded;
    private long lastOpened;
    private int currentPage;
    private int totalPages;
    private boolean isFavorite;

    public Book(String title, String author, String filePath, String fileType) {
        this.title = title;
        this.author = author;
        this.filePath = filePath;
        this.fileType = fileType;
        this.dateAdded = System.currentTimeMillis();
        this.lastOpened = 0;
        this.currentPage = 0;
        this.isFavorite = false;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public long getDateAdded() {
        return dateAdded;
    }

    public void setDateAdded(long dateAdded) {
        this.dateAdded = dateAdded;
    }

    public long getLastOpened() {
        return lastOpened;
    }

    public void setLastOpened(long lastOpened) {
        this.lastOpened = lastOpened;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }
}
