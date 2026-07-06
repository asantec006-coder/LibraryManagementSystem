package com.library.service;

import com.library.model.Book;
import com.library.model.EBook;
import com.library.model.Loan;
import com.library.model.Member;
import com.library.model.PhysicalBook;
import com.library.repository.BookRepository;
import com.library.repository.LoanRepository;
import com.library.repository.MemberRepository;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * Business service orchestrating library operations.
 */
public class LibraryService {
    private final BookRepository bookRepository = new BookRepository();
    private final MemberRepository memberRepository = new MemberRepository();
    private final LoanRepository loanRepository = new LoanRepository();

    public void addBook(Book book) throws SQLException {
        bookRepository.save(book);
    }

    public void updateBook(Book book) throws SQLException {
        bookRepository.update(book);
    }

    public void deleteBook(int id) throws SQLException {
        bookRepository.deleteById(id);
    }

    public List<Book> getAllBooks() throws SQLException {
        return bookRepository.findAll();
    }

    public List<Book> searchBooks(String keyword) throws SQLException {
        return bookRepository.search(keyword);
    }

    public void addMember(Member member) throws SQLException {
        memberRepository.save(member);
    }

    public void updateMember(Member member) throws SQLException {
        memberRepository.update(member);
    }

    public void deleteMember(int id) throws SQLException {
        memberRepository.deleteById(id);
    }

    public List<Member> getAllMembers() throws SQLException {
        return memberRepository.findAll();
    }

    public List<Member> searchMembers(String keyword) throws SQLException {
        return memberRepository.search(keyword);
    }

    /**
     * Issues a book to a member with an explicit due date.
     * Updates book availability and persists the loan record.
     */
    public void issueBook(int bookId, int memberId, LocalDate dueDate) throws SQLException {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalStateException("Book not found."));
        if (!book.isAvailable()) {
            throw new IllegalStateException("Book is not available for loan.");
        }
        memberRepository.findById(memberId)
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
        bookRepository.update(book);
    }

    public void returnBook(int loanId) throws SQLException {
        Loan loan = loanRepository.findById(loanId);
        if (loan == null || loan.isReturned()) {
            throw new IllegalStateException("Loan was not found or is already returned.");
        }

        loan.setReturned(true);
        loan.setReturnDate(LocalDate.now());
        loanRepository.update(loan);

        Book book = bookRepository.findById(loan.getBookId()).orElse(null);
        switch (book) {
            case PhysicalBook pb -> pb.returnCopy();
            case EBook eb -> eb.checkIn();
            case null, default -> {}
        }
        if (book != null) {
            bookRepository.update(book);
        }
    }

    public List<Loan> getAllLoans() throws SQLException {
        return loanRepository.findAll();
    }
}
