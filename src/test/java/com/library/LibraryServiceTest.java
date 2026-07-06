package com.library;

import com.library.model.Member;
import com.library.model.PhysicalBook;
import com.library.repository.BookRepository;
import com.library.repository.MemberRepository;
import com.library.service.LibraryService;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class LibraryServiceTest {

    @Test
    void shouldAddBookAndMemberWithoutErrors() throws SQLException {
        new BookRepository().createTableIfNotExists();
        new MemberRepository().createTableIfNotExists();

        LibraryService service = new LibraryService();

        // PhysicalBook now uses totalCopies/availableCopies instead of available+shelfLocation
        PhysicalBook book = new PhysicalBook(0, "Effective Java", "Joshua Bloch",
                "9780134685991", "Programming", 1, 1);

        Member member = new Member();
        member.setFullName("Jane Doe");
        member.setEmail("jane@example.com");
        member.setPhone("123456789");
        member.setJoinDate(java.time.LocalDate.now());
        member.setMembershipType(Member.TYPE_STANDARD);
        member.setStatus(Member.STATUS_ACTIVE);

        assertDoesNotThrow(() -> service.addBook(book));
        assertDoesNotThrow(() -> service.addMember(member));
    }
}
