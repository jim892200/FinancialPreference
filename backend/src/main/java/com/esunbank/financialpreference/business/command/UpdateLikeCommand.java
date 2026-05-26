package com.esunbank.financialpreference.business.command;

import java.math.BigDecimal;

public record UpdateLikeCommand(
        long sn,
        String productName,
        BigDecimal price,
        BigDecimal feeRate,
        int purchaseQuantity,
        String account
) {}
