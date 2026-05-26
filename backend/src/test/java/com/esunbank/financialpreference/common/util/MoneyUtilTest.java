package com.esunbank.financialpreference.common.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MoneyUtilTest {

    @Test
    void scale_roundsToTwoDecimalsHalfUp() {
        assertEquals(new BigDecimal("1.23"), MoneyUtil.scale(new BigDecimal("1.234")));
        assertEquals(new BigDecimal("1.24"), MoneyUtil.scale(new BigDecimal("1.235")));
        assertEquals(new BigDecimal("0.00"), MoneyUtil.scale(BigDecimal.ZERO));
    }

    @Test
    void scale_handlesNull() {
        assertNull(MoneyUtil.scale(null));
    }

    @Test
    void multiply_returnsScaledProduct() {
        // 1000.00 * 0.0100 * 5 = 50.00
        BigDecimal price = new BigDecimal("1000.00");
        BigDecimal rate  = new BigDecimal("0.0100");
        BigDecimal qty   = new BigDecimal("5");
        assertEquals(
            new BigDecimal("50.00"),
            MoneyUtil.multiply(MoneyUtil.multiply(price, rate), qty)
        );
    }

    @Test
    void add_returnsScaledSum() {
        assertEquals(
            new BigDecimal("3.33"),
            MoneyUtil.add(new BigDecimal("1.11"), new BigDecimal("2.22"))
        );
    }

    @Test
    void compound_matchesSeedRow1() {
        // Seed #1: price=1000, fee=0.01, qty=5
        //   TOTAL_FEE    = 1000 * 0.01 * 5      = 50.00
        //   TOTAL_AMOUNT = 1000 * 5 + 50.00     = 5050.00
        BigDecimal price = new BigDecimal("1000.00");
        BigDecimal rate  = new BigDecimal("0.0100");
        BigDecimal qty   = new BigDecimal("5");

        BigDecimal totalFee    = MoneyUtil.multiply(MoneyUtil.multiply(price, rate), qty);
        BigDecimal totalAmount = MoneyUtil.add(MoneyUtil.multiply(price, qty), totalFee);

        assertEquals(new BigDecimal("50.00"),   totalFee);
        assertEquals(new BigDecimal("5050.00"), totalAmount);
    }
}
