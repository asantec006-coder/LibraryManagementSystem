package com.library.model;

/**
 * Abstract base class for all books in the library.
 * Model layer only: fields, constructors, getters/setters, and simple
 * business methods. No database or JavaFX code belongs here.
 */
public abstract class Book {

    private int id;
    private String title;
    private String author;
    private String isbn;
    private String genre;
    /** Relative path to the cached cover image, e.g. "covers/9780439554930.jpg". Null/blank means no cover yet. */
    private String coverImage;

    public Book() {
    }

    public Book(int id, String title, String author, String isbn, String genre) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.genre = genre;
    }

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

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getCoverImage() {
        return coverImage;
    }

    public void setCoverImage(String coverImage) {
        this.coverImage = coverImage;
    }

    /**
     * Whether a copy of this book can currently be borrowed.
     * Implemented differently by PhysicalBook (copy count) and EBook (license count).
     */
    public abstract boolean isAvailable();

    /**
     * Discriminator used by the repository layer and the UI.
     * Returns "PHYSICAL" or "EBOOK".
     */
    public abstract String getType();

    @Override
    public String toString() {
        return title + " by " + author + " (" + isbn + ")";
    }
}
