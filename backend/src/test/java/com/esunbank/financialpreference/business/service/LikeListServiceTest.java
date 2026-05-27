package com.esunbank.financialpreference.business.service;

import com.esunbank.financialpreference.business.command.CreateLikeCommand;
import com.esunbank.financialpreference.business.command.UpdateLikeCommand;
import com.esunbank.financialpreference.business.domain.LikeItem;
import com.esunbank.financialpreference.business.domain.Product;
import com.esunbank.financialpreference.business.domain.User;
import com.esunbank.financialpreference.common.exception.BusinessException;
import com.esunbank.financialpreference.common.exception.ErrorCode;
import com.esunbank.financialpreference.data.repository.LikeListRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.UncategorizedSQLException;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LikeListServiceTest {

    @Mock
    LikeListRepository repository;

    @InjectMocks
    LikeListService service;

    private static final String USER_ID = "A1236456789";
    private static final String ACCOUNT = "1111999666";

    // ---------- create ----------

    @Test
    void create_returnsNewSnFromRepository() {
        when(repository.insert(any(), any(), any(), any(), anyInt(), any())).thenReturn(42L);

        long sn = service.create(new CreateLikeCommand(
                USER_ID, "x", new BigDecimal("100"), new BigDecimal("0.01"), 2, ACCOUNT));

        assertEquals(42L, sn);
        verify(repository).insert(USER_ID, "x", new BigDecimal("100"), new BigDecimal("0.01"), 2, ACCOUNT);
    }

    @Test
    void create_userNotFound_throwsBusinessException() {
        when(repository.insert(any(), any(), any(), any(), anyInt(), any()))
                .thenThrow(uncategorizedSql(50001, "USER_NOT_FOUND"));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                service.create(new CreateLikeCommand(
                        "ghost", "x", new BigDecimal("100"), new BigDecimal("0.01"), 2, ACCOUNT)));

        assertSame(ErrorCode.USER_NOT_FOUND, ex.errorCode());
    }

    @Test
    void create_invalidQuantity_throwsBusinessException() {
        when(repository.insert(any(), any(), any(), any(), anyInt(), any()))
                .thenThrow(uncategorizedSql(50002, "INVALID_QUANTITY"));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                service.create(new CreateLikeCommand(
                        USER_ID, "x", new BigDecimal("100"), new BigDecimal("0.01"), 0, ACCOUNT)));

        assertSame(ErrorCode.INVALID_QUANTITY, ex.errorCode());
    }

    @Test
    void create_accountMismatch_throwsBusinessException() {
        when(repository.insert(any(), any(), any(), any(), anyInt(), any()))
                .thenThrow(uncategorizedSql(50005, "ACCOUNT_MISMATCH"));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                service.create(new CreateLikeCommand(
                        USER_ID, "x", new BigDecimal("100"), new BigDecimal("0.01"), 1, "wrong-acct")));

        assertSame(ErrorCode.ACCOUNT_MISMATCH, ex.errorCode());
    }

    // ---------- list ----------

    @Test
    void listByUserId_passesThroughRepository() {
        LikeItem item = new LikeItem(
                1L,
                new User(USER_ID, "王o明", "test@email.com", null),
                new Product(10L, "p", new BigDecimal("100.00"), new BigDecimal("0.0100")),
                5,
                ACCOUNT,
                new BigDecimal("5.00"),
                new BigDecimal("505.00")
        );
        when(repository.findByUserId(USER_ID)).thenReturn(List.of(item));

        List<LikeItem> result = service.listByUserId(USER_ID);

        assertEquals(1, result.size());
        assertSame(item, result.get(0));
    }

    // ---------- update ----------

    @Test
    void update_likeNotFound_throwsBusinessException() {
        doThrow(uncategorizedSql(50003, "LIKE_NOT_FOUND"))
                .when(repository).update(anyLong(), anyString(), anyString(), any(), any(), anyInt(), anyString());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                service.update(new UpdateLikeCommand(
                        999L, USER_ID, "x", new BigDecimal("100"), new BigDecimal("0.01"), 1, ACCOUNT)));

        assertSame(ErrorCode.LIKE_NOT_FOUND, ex.errorCode());
    }

    @Test
    void update_forbidden_throwsBusinessException() {
        doThrow(uncategorizedSql(50004, "FORBIDDEN"))
                .when(repository).update(anyLong(), anyString(), anyString(), any(), any(), anyInt(), anyString());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                service.update(new UpdateLikeCommand(
                        1L, "other", "x", new BigDecimal("100"), new BigDecimal("0.01"), 1, ACCOUNT)));

        assertSame(ErrorCode.FORBIDDEN, ex.errorCode());
    }

    // ---------- delete ----------

    @Test
    void delete_likeNotFound_throwsBusinessException() {
        doThrow(uncategorizedSql(50003, "LIKE_NOT_FOUND"))
                .when(repository).delete(anyLong(), anyString());

        BusinessException ex = assertThrows(BusinessException.class, () -> service.delete(999L, USER_ID));
        assertSame(ErrorCode.LIKE_NOT_FOUND, ex.errorCode());
    }

    @Test
    void delete_forbidden_throwsBusinessException() {
        doThrow(uncategorizedSql(50004, "FORBIDDEN"))
                .when(repository).delete(anyLong(), anyString());

        BusinessException ex = assertThrows(BusinessException.class, () -> service.delete(1L, "other"));
        assertSame(ErrorCode.FORBIDDEN, ex.errorCode());
    }

    @Test
    void delete_unknownDbError_mapsToInternal() {
        doThrow(uncategorizedSql(99999, "some weird error"))
                .when(repository).delete(anyLong(), anyString());

        BusinessException ex = assertThrows(BusinessException.class, () -> service.delete(1L, USER_ID));
        assertSame(ErrorCode.INTERNAL_ERROR, ex.errorCode());
    }

    // ---------- helpers ----------

    private static UncategorizedSQLException uncategorizedSql(int code, String message) {
        SQLException sqlEx = new SQLException(message, "S0001", code);
        return new UncategorizedSQLException("SP_LIKE_*", "", sqlEx);
    }
}
