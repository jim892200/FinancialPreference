package com.esunbank.financialpreference.business.command;

import java.math.BigDecimal;

public record CreateLikeCommand(
        String userId,
        String productName,
        BigDecimal price,
        BigDecimal feeRate,
        int purchaseQuantity,
        String account
) {}
