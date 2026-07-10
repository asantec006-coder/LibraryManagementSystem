package com.library.service;

import com.library.model.Book;
import com.library.model.EBook;
import com.library.model.Loan;
import com.library.model.PhysicalBook;
import com.library.repository.LoanRepository;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * Business service orchestrating loan operations.
 * Validates availability through BookService.
 */
public class LoanService {

    private final LoanRepository loanRepository;
    private final BookService bookService;
    private final MemberService memberService;

    public LoanService(LoanRepository loanRepository, BookService bookService, MemberService memberService) {
        this.loanRepository = loanRepository;
        this.bookService = bookService;
        this.memberService = memberService;
    }

    /**
     * Issues a book to a member with an explicit due date.
     * Validates member exists and book is available, then persists loan.
     */
    public void issueBook(int bookId, int memberId, LocalDate dueDate) throws SQLException {
        Book book = bookService.getBookById(bookId)
                .orElseThrow(() -> new IllegalStateException("Book not found."));
        
        if (!bookService.isBookAvailable(bookId)) {
            throw new IllegalStateException("Book is not available for loan.");
        }
        
        memberService.getMemberById(memberId)
                .orElseThrow(() -> new IllegalStateException("Member does not exist."));

        Loan loan = new Loan();
        loan.setBookId(bookId);
        loan.setMemberId(memberId);
        loan.setLoanDate(LocalDate.now());
        loan.setDueDate(dueDate);
        loan.setReturned(false);
        loanRepository.save(loan);

        switch (book) {
            case PhysicalBook pb -> pb.borrowCopy();
            case EBook eb -> eb.checkOut();
            default -> {}
        }
        bookService.updateBook(book);
    }

    public void returnBook(int loanId) throws SQLException {
        Loan loan = loanRepository.findById(loanId);
        if (loan == null || loan.isReturned()) {
            throw new IllegalStateException("Loan was not found or is already returned.");
        }

        loan.setReturned(true);
        loan.setReturnDate(LocalDate.now());
        loanRepository.update(loan);

        Book book = bookService.getBookById(loan.getBookId()).orElse(null);
        switch (book) {
            case PhysicalBook pb -> pb.returnCopy();
            case EBook eb -> eb.checkIn();
            case null, default -> {}
        }
        if (book != null) {
            bookService.updateBook(book);
        }
    }

    public List<Loan> getAllLoans() throws SQLException {
        return loanRepository.findAll();
    }
}
