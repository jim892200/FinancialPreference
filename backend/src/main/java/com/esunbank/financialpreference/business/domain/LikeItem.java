package com.esunbank.financialpreference.business.domain;

import java.math.BigDecimal;

public record LikeItem(
        long sn,
        User user,
        Product product,
        int purchaseQuantity,
        String account,
        BigDecimal totalFee,
        BigDecimal totalAmount
) {}
