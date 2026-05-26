package com.esunbank.financialpreference.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class MoneyUtil {

    public static final int           SCALE    = 2;
    public static final RoundingMode  ROUNDING = RoundingMode.HALF_UP;

    private MoneyUtil() {}

    public static BigDecimal scale(BigDecimal value) {
        return value == null ? null : value.setScale(SCALE, ROUNDING);
    }

    public static BigDecimal multiply(BigDecimal a, BigDecimal b) {
        return scale(a.multiply(b));
    }

    public static BigDecimal add(BigDecimal a, BigDecimal b) {
        return scale(a.add(b));
    }
}
