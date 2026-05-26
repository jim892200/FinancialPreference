package com.esunbank.financialpreference.business.calculator;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FeeCalculatorTest {

    @Test
    void seedRow1_priceTimesRateTimesQty() {
        // 1000.00 * 0.0100 * 5 = 50.00
        BigDecimal fee = FeeCalculator.totalFee(new BigDecimal("1000.00"), new BigDecimal("0.0100"), 5);
        BigDecimal amt = FeeCalculator.totalAmount(new BigDecimal("1000.00"), 5, fee);
        assertEquals(new BigDecimal("50.00"),   fee);
        assertEquals(new BigDecimal("5050.00"), amt);
    }

    @Test
    void seedRow2() {
        // 500 * 0.015 * 10 = 75.00 ; 500 * 10 + 75 = 5075.00
        BigDecimal fee = FeeCalculator.totalFee(new BigDecimal("500.00"), new BigDecimal("0.0150"), 10);
        BigDecimal amt = FeeCalculator.totalAmount(new BigDecimal("500.00"), 10, fee);
        assertEquals(new BigDecimal("75.00"),   fee);
        assertEquals(new BigDecimal("5075.00"), amt);
    }

    @Test
    void seedRow3() {
        // 2000 * 0.008 * 3 = 48.00 ; 2000 * 3 + 48 = 6048.00
        BigDecimal fee = FeeCalculator.totalFee(new BigDecimal("2000.00"), new BigDecimal("0.0080"), 3);
        BigDecimal amt = FeeCalculator.totalAmount(new BigDecimal("2000.00"), 3, fee);
        assertEquals(new BigDecimal("48.00"),   fee);
        assertEquals(new BigDecimal("6048.00"), amt);
    }

    @Test
    void roundsHalfUp() {
        // 100.005 * 0.005 * 1 = 0.500025 -> 0.50
        BigDecimal fee = FeeCalculator.totalFee(new BigDecimal("100.005"), new BigDecimal("0.0050"), 1);
        assertEquals(new BigDecimal("0.50"), fee);
    }
}
