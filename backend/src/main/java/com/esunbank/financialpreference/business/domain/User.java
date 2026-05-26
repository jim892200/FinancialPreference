package com.esunbank.financialpreference.business.domain;

public record User(
        String userId,
        String userName,
        String email,
        String account
) {}
