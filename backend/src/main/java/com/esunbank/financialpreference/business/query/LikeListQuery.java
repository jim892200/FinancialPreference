package com.esunbank.financialpreference.business.query;

import java.math.BigDecimal;
import java.util.Set;

/**
 * 喜好清單查詢條件 — filter + sort + pagination。
 * filter 欄位為 null 代表不套用；sort/page 帶預設值。
 * sortBy 走白名單，避免從 URL 帶入奇怪的欄位被 SP 接受（雖然 SP 內部也已用 CASE 隔離）。
 */
public record LikeListQuery(
        String productName,
        String account,
        BigDecimal amountMin,
        BigDecimal amountMax,
        BigDecimal feeRateMin,
        BigDecimal feeRateMax,
        String sortBy,
        String sortDir,
        int page,
        int pageSize
) {
    public static final Set<String> SORTABLE = Set.of(
            "sn", "productName", "price", "feeRate",
            "purchaseQuantity", "totalFee", "totalAmount");
    public static final int MAX_PAGE_SIZE = 200;
    private static final String DEFAULT_SORT_BY  = "sn";
    private static final String DEFAULT_SORT_DIR = "desc";

    public LikeListQuery {
        productName = blankToNull(productName);
        account     = blankToNull(account);
        sortBy      = (sortBy != null && SORTABLE.contains(sortBy)) ? sortBy : DEFAULT_SORT_BY;
        sortDir     = "asc".equalsIgnoreCase(sortDir) ? "asc" : DEFAULT_SORT_DIR;
        if (page <= 0)                    page = 1;
        if (pageSize <= 0)                pageSize = 10;
        if (pageSize > MAX_PAGE_SIZE)     pageSize = MAX_PAGE_SIZE;
    }

    public static LikeListQuery empty() {
        return new LikeListQuery(null, null, null, null, null, null, null, null, 1, 10);
    }

    private static String blankToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
