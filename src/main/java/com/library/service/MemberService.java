package com.library.service;

import com.library.model.Member;
import com.library.repository.MemberRepository;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Business logic for members: validation and uniqueness rules.
 * Controllers should only ever talk to the service, never the repository
 * or the database directly.
 */
public class MemberService {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}$");

    private final MemberRepository memberRepository;

    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    public Member addMember(Member member) throws SQLException {
        validateMember(member);
        Optional<Member> existing = memberRepository.findByEmail(member.getEmail());
        if (existing.isPresent()) {
            throw new IllegalArgumentException("A member with email " + member.getEmail() + " already exists.");
        }
        return memberRepository.save(member);
    }

    public Member updateMember(Member member) throws SQLException {
        validateMember(member);
        Optional<Member> existing = memberRepository.findByEmail(member.getEmail());
        if (existing.isPresent() && existing.get().getId() != member.getId()) {
            throw new IllegalArgumentException("Another member already uses email " + member.getEmail() + ".");
        }
        boolean updated = memberRepository.update(member);
        if (!updated) {
            throw new IllegalArgumentException("Member with id " + member.getId() + " was not found.");
        }
        return member;
    }

    public void deleteMember(int id) throws SQLException {
        boolean deleted = memberRepository.deleteById(id);
        if (!deleted) {
            throw new IllegalArgumentException("Member with id " + id + " was not found.");
        }
    }

    public List<Member> getAllMembers() throws SQLException {
        return memberRepository.findAll();
    }

    public List<Member> searchMembers(String keyword) throws SQLException {
        if (keyword == null || keyword.isBlank()) {
            return getAllMembers();
        }
        return memberRepository.search(keyword.trim());
    }

    public Optional<Member> getMemberById(int id) throws SQLException {
        return memberRepository.findById(id);
    }

    private void validateMember(Member member) {
        if (member.getName() == null || member.getName().isBlank()) {
            throw new IllegalArgumentException("Name is required.");
        }
        if (member.getEmail() == null || member.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email is required.");
        }
        if (!EMAIL_PATTERN.matcher(member.getEmail()).matches()) {
            throw new IllegalArgumentException("Email is not a valid email address.");
        }
        if (member.getJoinDate() == null) {
            throw new IllegalArgumentException("Join date is required.");
        }
        if (member.getMembershipType() == null
                || !(member.getMembershipType().equals(Member.TYPE_STANDARD)
                     || member.getMembershipType().equals(Member.TYPE_PREMIUM))) {
            throw new IllegalArgumentException("Membership type must be Standard or Premium.");
        }
        if (member.getStatus() == null
                || !(member.getStatus().equals(Member.STATUS_ACTIVE)
                     || member.getStatus().equals(Member.STATUS_INACTIVE))) {
            throw new IllegalArgumentException("Status must be Active or Inactive.");
        }
        if (member.getBooksBorrowed() < 0) {
            throw new IllegalArgumentException("Books borrowed cannot be negative.");
        }
    }
}
