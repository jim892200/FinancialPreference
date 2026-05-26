package com.esunbank.financialpreference.business.calculator;

import com.esunbank.financialpreference.common.util.MoneyUtil;

import java.math.BigDecimal;

/**
 * 重算 TotalFee / TotalAmount 的純函式工具。
 * 公式必須與 SP_LIKE_INSERT / SP_LIKE_UPDATE 完全一致，避免雙寫不一致。
 *
 *   TOTAL_FEE    = PRICE * FEE_RATE * QTY
 *   TOTAL_AMOUNT = PRICE * QTY + TOTAL_FEE
 */
public final class FeeCalculator {

    private FeeCalculator() {}

    public static BigDecimal totalFee(BigDecimal price, BigDecimal feeRate, int qty) {
        return MoneyUtil.multiply(MoneyUtil.multiply(price, feeRate), BigDecimal.valueOf(qty));
    }

    public static BigDecimal totalAmount(BigDecimal price, int qty, BigDecimal totalFee) {
        return MoneyUtil.add(MoneyUtil.multiply(price, BigDecimal.valueOf(qty)), totalFee);
    }
}
