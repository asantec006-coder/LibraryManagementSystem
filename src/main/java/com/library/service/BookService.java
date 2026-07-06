package com.library.service;

import com.library.model.Book;
import com.library.model.EBook;
import com.library.model.PhysicalBook;
import com.library.repository.BookRepository;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Business logic for books: validation, uniqueness rules, and the
 * availability question the Borrow module will rely on later.
 * Controllers should only ever talk to the service, never the repository
 * or the database directly.
 */
public class BookService {

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public Book addBook(Book book) throws SQLException {
        validateBook(book);
        Optional<Book> existing = bookRepository.findByIsbn(book.getIsbn());
        if (existing.isPresent()) {
            throw new IllegalArgumentException("A book with ISBN " + book.getIsbn() + " already exists.");
        }
        return bookRepository.save(book);
    }

    public Book updateBook(Book book) throws SQLException {
        validateBook(book);
        Optional<Book> existing = bookRepository.findByIsbn(book.getIsbn());
        if (existing.isPresent() && existing.get().getId() != book.getId()) {
            throw new IllegalArgumentException("Another book already uses ISBN " + book.getIsbn() + ".");
        }
        boolean updated = bookRepository.update(book);
        if (!updated) {
            throw new IllegalArgumentException("Book with id " + book.getId() + " was not found.");
        }
        return book;
    }

    public void deleteBook(int id) throws SQLException {
        boolean deleted = bookRepository.deleteById(id);
        if (!deleted) {
            throw new IllegalArgumentException("Book with id " + id + " was not found.");
        }
    }

    public List<Book> getAllBooks() throws SQLException {
        return bookRepository.findAll();
    }

    public List<Book> searchBooks(String keyword) throws SQLException {
        if (keyword == null || keyword.isBlank()) {
            return getAllBooks();
        }
        return bookRepository.search(keyword.trim());
    }

    public Optional<Book> getBookById(int id) throws SQLException {
        return bookRepository.findById(id);
    }

    /**
     * Used later by the Borrow module to decide if a loan can be created.
     */
    public boolean isBookAvailable(int id) throws SQLException {
        return bookRepository.findById(id)
                .map(Book::isAvailable)
                .orElse(false);
    }

    private void validateBook(Book book) {
        if (book.getTitle() == null || book.getTitle().isBlank()) {
            throw new IllegalArgumentException("Title is required.");
        }
        if (book.getAuthor() == null || book.getAuthor().isBlank()) {
            throw new IllegalArgumentException("Author is required.");
        }
        if (book.getIsbn() == null || book.getIsbn().isBlank()) {
            throw new IllegalArgumentException("ISBN is required.");
        }

        if (book instanceof PhysicalBook pb) {
            if (pb.getTotalCopies() < 0) {
                throw new IllegalArgumentException("Total copies cannot be negative.");
            }
            if (pb.getAvailableCopies() < 0 || pb.getAvailableCopies() > pb.getTotalCopies()) {
                throw new IllegalArgumentException("Available copies must be between 0 and total copies.");
            }
        } else if (book instanceof EBook eb) {
            if (eb.getMaxConcurrentLicenses() < 0) {
                throw new IllegalArgumentException("Max concurrent licenses cannot be negative.");
            }
            if (eb.getActiveLoans() < 0 || eb.getActiveLoans() > eb.getMaxConcurrentLicenses()) {
                throw new IllegalArgumentException("Active loans must be between 0 and max licenses.");
            }
        }
    }
}
