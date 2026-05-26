package com.esunbank.financialpreference.presentation.dto.response;

import com.esunbank.financialpreference.business.domain.LikeItem;

import java.math.BigDecimal;

public record LikeItemResponse(
        long sn,
        String userId,
        String userName,
        String email,
        long productNo,
        String productName,
        BigDecimal price,
        BigDecimal feeRate,
        int purchaseQuantity,
        String account,
        BigDecimal totalFee,
        BigDecimal totalAmount
) {
    public static LikeItemResponse from(LikeItem item) {
        return new LikeItemResponse(
                item.sn(),
                item.user().userId(),
                item.user().userName(),
                item.user().email(),
                item.product().no(),
                item.product().productName(),
                item.product().price(),
                item.product().feeRate(),
                item.purchaseQuantity(),
                item.account(),
                item.totalFee(),
                item.totalAmount()
        );
    }
}
