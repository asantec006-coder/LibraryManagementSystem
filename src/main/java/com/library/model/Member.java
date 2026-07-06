package com.library.model;

import java.time.LocalDate;

/**
 * A library member. Model layer only: fields, constructors, getters/setters.
 * No database or JavaFX code belongs here.
 */
public class Member {

    public static final String TYPE_STANDARD = "Standard";
    public static final String TYPE_PREMIUM = "Premium";

    public static final String STATUS_ACTIVE = "Active";
    public static final String STATUS_INACTIVE = "Inactive";

    private int id;
    private String name;
    private String email;
    private String phone;
    private LocalDate joinDate;
    private int booksBorrowed;
    private String membershipType;
    private String status;

    public Member() {
    }

    public Member(int id, String name, String email, String phone, LocalDate joinDate,
                  int booksBorrowed, String membershipType, String status) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.joinDate = joinDate;
        this.booksBorrowed = booksBorrowed;
        this.membershipType = membershipType;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public LocalDate getJoinDate() {
        return joinDate;
    }

    public void setJoinDate(LocalDate joinDate) {
        this.joinDate = joinDate;
    }

    public int getBooksBorrowed() {
        return booksBorrowed;
    }

    public void setBooksBorrowed(int booksBorrowed) {
        this.booksBorrowed = booksBorrowed;
    }

    public String getMembershipType() {
        return membershipType;
    }

    public void setMembershipType(String membershipType) {
        this.membershipType = membershipType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFullName() {
        return name;
    }

    public void setFullName(String name) {
        this.name = name;
    }

    public LocalDate getMembershipDate() {
        return joinDate;
    }

    public void setMembershipDate(LocalDate joinDate) {
        this.joinDate = joinDate;
    }

    public boolean isEligibleToBorrow() {
        return STATUS_ACTIVE.equals(this.status);
    }

    @Override
    public String toString() {
        return name + " <" + email + ">";
    }
}
