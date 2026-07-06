package com.library.model;

/**
 * A physical, printed copy of a book. Tracks how many copies the library
 * owns in total and how many are currently sitting on the shelf.
 */
public class PhysicalBook extends Book {

    private int totalCopies;
    private int availableCopies;

    public PhysicalBook() {
        super();
    }

    public PhysicalBook(int id, String title, String author, String isbn, String genre,
                         int totalCopies, int availableCopies) {
        super(id, title, author, isbn, genre);
        this.totalCopies = totalCopies;
        this.availableCopies = availableCopies;
    }

    public int getTotalCopies() {
        return totalCopies;
    }

    /**
     * Setting total copies down will also cap available copies so it can
     * never exceed the new total.
     */
    public void setTotalCopies(int totalCopies) {
        this.totalCopies = totalCopies;
        if (this.availableCopies > totalCopies) {
            this.availableCopies = totalCopies;
        }
    }

    public int getAvailableCopies() {
        return availableCopies;
    }

    public void setAvailableCopies(int availableCopies) {
        this.availableCopies = availableCopies;
    }

    /**
     * Marks one copy as borrowed. Returns false if none are available.
     */
    public boolean borrowCopy() {
        if (availableCopies <= 0) {
            return false;
        }
        availableCopies--;
        return true;
    }

    /**
     * Marks one copy as returned. Returns false if that would exceed the
     * total number of copies the library owns (data-integrity guard).
     */
    public boolean returnCopy() {
        if (availableCopies >= totalCopies) {
            return false;
        }
        availableCopies++;
        return true;
    }

    @Override
    public boolean isAvailable() {
        return availableCopies > 0;
    }

    @Override
    public String getType() {
        return "PHYSICAL";
    }
}
