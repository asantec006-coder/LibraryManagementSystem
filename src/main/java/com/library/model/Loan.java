package com.library.model;

import java.time.LocalDate;

/**
 * Represents a loan transaction between a member and a book.
 * Tracks issue date, due date, return date, and status.
 */
public class Loan {
    private int id;
    private int bookId;
    private int memberId;
    private LocalDate loanDate;
    private LocalDate dueDate;       // when the book must be returned
    private LocalDate returnDate;    // set when actually returned
    private boolean returned;

    public Loan() {
    }

    public Loan(int id, int bookId, int memberId,
                LocalDate loanDate, LocalDate dueDate,
                LocalDate returnDate, boolean returned) {
        this.id = id;
        this.bookId = bookId;
        this.memberId = memberId;
        this.loanDate = loanDate;
        this.dueDate = dueDate;
        this.returnDate = returnDate;
        this.returned = returned;
    }

    // ── getters / setters ────────────────────────────────────────────────

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getBookId() { return bookId; }
    public void setBookId(int bookId) { this.bookId = bookId; }

    public int getMemberId() { return memberId; }
    public void setMemberId(int memberId) { this.memberId = memberId; }

    public LocalDate getLoanDate() { return loanDate; }
    public void setLoanDate(LocalDate loanDate) { this.loanDate = loanDate; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public LocalDate getReturnDate() { return returnDate; }
    public void setReturnDate(LocalDate returnDate) { this.returnDate = returnDate; }

    public boolean isReturned() { return returned; }
    public void setReturned(boolean returned) { this.returned = returned; }

    /** True if today is past the due date and the book hasn't been returned. */
    public boolean isOverdue() {
        return !returned && dueDate != null && LocalDate.now().isAfter(dueDate);
    }
}
