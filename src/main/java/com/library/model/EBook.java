package com.library.model;

/**
 * A digital copy of a book. Instead of physical copies, an EBook has a
 * number of concurrent licenses the library has purchased (how many
 * members can have it "checked out" for reading at the same time).
 */
public class EBook extends Book {

    private String downloadUrl;
    private int maxConcurrentLicenses;
    private int activeLoans;

    public EBook() {
        super();
    }

    public EBook(int id, String title, String author, String isbn, String genre,
                 String downloadUrl, int maxConcurrentLicenses, int activeLoans) {
        super(id, title, author, isbn, genre);
        this.downloadUrl = downloadUrl;
        this.maxConcurrentLicenses = maxConcurrentLicenses;
        this.activeLoans = activeLoans;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public int getMaxConcurrentLicenses() {
        return maxConcurrentLicenses;
    }

    public void setMaxConcurrentLicenses(int maxConcurrentLicenses) {
        this.maxConcurrentLicenses = maxConcurrentLicenses;
        if (this.activeLoans > maxConcurrentLicenses) {
            this.activeLoans = maxConcurrentLicenses;
        }
    }

    public int getActiveLoans() {
        return activeLoans;
    }

    public void setActiveLoans(int activeLoans) {
        this.activeLoans = activeLoans;
    }

    /**
     * Checks out one license. Returns false if all concurrent licenses
     * are already in use.
     */
    public boolean checkOut() {
        if (activeLoans >= maxConcurrentLicenses) {
            return false;
        }
        activeLoans++;
        return true;
    }

    /**
     * Checks in one license. Returns false if there were no active loans
     * to release.
     */
    public boolean checkIn() {
        if (activeLoans <= 0) {
            return false;
        }
        activeLoans--;
        return true;
    }

    @Override
    public boolean isAvailable() {
        return activeLoans < maxConcurrentLicenses;
    }

    @Override
    public String getType() {
        return "EBOOK";
    }
}
