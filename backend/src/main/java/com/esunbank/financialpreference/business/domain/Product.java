package com.esunbank.financialpreference.business.domain;

import java.math.BigDecimal;

public record Product(
        long no,
        String productName,
        BigDecimal price,
        BigDecimal feeRate
) {}
