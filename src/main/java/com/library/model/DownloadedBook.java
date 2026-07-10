package com.library.model;

import java.time.LocalDateTime;

/**
 * Model representing a book downloaded from an online library.
 */
public class DownloadedBook {
    private int id;
    private String title;
    private String author;
    private String source;
    private String format;
    private String localPath;
    private String downloadUrl;
    private LocalDateTime downloadedAt;

    public DownloadedBook() {
    }

    public DownloadedBook(int id, String title, String author, String source, String format, 
                          String localPath, String downloadUrl, LocalDateTime downloadedAt) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.source = source;
        this.format = format;
        this.localPath = localPath;
        this.downloadUrl = downloadUrl;
        this.downloadedAt = downloadedAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }

    public String getLocalPath() { return localPath; }
    public void setLocalPath(String localPath) { this.localPath = localPath; }

    public String getDownloadUrl() { return downloadUrl; }
    public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }

    public LocalDateTime getDownloadedAt() { return downloadedAt; }
    public void setDownloadedAt(LocalDateTime downloadedAt) { this.downloadedAt = downloadedAt; }
}
