package com.example.readerhub.api.models;

public class SyncDataRequest {
    private int userId;
    private String bookmarks;
    private String readingProgress;
    private String settings;

    public SyncDataRequest(int userId, String bookmarks, String readingProgress, String settings) {
        this.userId = userId;
        this.bookmarks = bookmarks;
        this.readingProgress = readingProgress;
        this.settings = settings;
    }

    public int getUserId() { return userId; }
    public String getBookmarks() { return bookmarks; }
    public String getReadingProgress() { return readingProgress; }
    public String getSettings() { return settings; }
}