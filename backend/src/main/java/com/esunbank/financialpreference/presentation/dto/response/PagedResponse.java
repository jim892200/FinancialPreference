package com.esunbank.financialpreference.presentation.dto.response;

import com.esunbank.financialpreference.business.query.PagedResult;

import java.util.List;
import java.util.function.Function;

/** API 對外的分頁回應殼。 */
public record PagedResponse<T>(List<T> items, long total, int page, int pageSize) {

    public static <S, T> PagedResponse<T> from(PagedResult<S> src, Function<S, T> mapper) {
        return new PagedResponse<>(
                src.items().stream().map(mapper).toList(),
                src.total(),
                src.page(),
                src.pageSize()
        );
    }
}
