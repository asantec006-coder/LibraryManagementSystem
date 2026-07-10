package com.library.model.dto;

import com.library.model.Loan;

/**
 * Data Transfer Object containing a Loan and its associated Book and Member names.
 * This avoids needing to load full Book and Member objects for UI lists.
 */
public record LoanDetails(
    Loan loan,
    String bookTitle,
    String memberName
) {}
