package com.esunbank.financialpreference.business.query;

import java.util.List;

/** 通用分頁結果包。 */
public record PagedResult<T>(List<T> items, long total, int page, int pageSize) {
}
