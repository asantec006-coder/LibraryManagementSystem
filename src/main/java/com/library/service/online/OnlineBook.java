package com.library.service.online;

public class OnlineBook {
    private String title;
    private String author;
    private String publicationYear;
    private String description;
    private String coverUrl;
    private String downloadUrl;
    private String downloadFormat;
    private String readOnlineUrl;
    private String source;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getPublicationYear() { return publicationYear; }
    public void setPublicationYear(String publicationYear) { this.publicationYear = publicationYear; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }

    public String getDownloadUrl() { return downloadUrl; }
    public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }

    public String getDownloadFormat() { return downloadFormat; }
    public void setDownloadFormat(String downloadFormat) { this.downloadFormat = downloadFormat; }

    public String getReadOnlineUrl() { return readOnlineUrl; }
    public void setReadOnlineUrl(String readOnlineUrl) { this.readOnlineUrl = readOnlineUrl; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}
