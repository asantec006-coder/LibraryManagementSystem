package com.library;

import com.library.model.Member;
import com.library.model.PhysicalBook;
import com.library.repository.BookRepository;
import com.library.repository.MemberRepository;
import com.library.service.BookService;
import com.library.service.MemberService;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class LibraryServiceTest {

    @Test
    void shouldAddBookAndMemberWithoutErrors() throws SQLException {
        new BookRepository().createTableIfNotExists();
        new MemberRepository().createTableIfNotExists();

        BookService bookService = new BookService(new BookRepository());
        MemberService memberService = new MemberService(new MemberRepository());

        // PhysicalBook now uses totalCopies/availableCopies instead of available+shelfLocation
        String isbn = "9780134685991-" + System.nanoTime();
        PhysicalBook book = new PhysicalBook(0, "Effective Java", "Joshua Bloch",
                isbn, "Programming", 1, 1);

        Member member = new Member();
        member.setFullName("Jane Doe");
        member.setEmail("jane+" + System.nanoTime() + "@example.com");
        member.setPhone("123456789");
        member.setJoinDate(java.time.LocalDate.now());
        member.setMembershipType(Member.TYPE_STANDARD);
        member.setStatus(Member.STATUS_ACTIVE);

        assertDoesNotThrow(() -> bookService.addBook(book));
        assertDoesNotThrow(() -> memberService.addMember(member));
    }
}
