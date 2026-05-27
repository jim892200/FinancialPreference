package com.esunbank.financialpreference.business.service;

import com.esunbank.financialpreference.business.command.CreateLikeCommand;
import com.esunbank.financialpreference.business.command.UpdateLikeCommand;
import com.esunbank.financialpreference.business.domain.LikeItem;
import com.esunbank.financialpreference.business.query.LikeListQuery;
import com.esunbank.financialpreference.business.query.PagedResult;
import com.esunbank.financialpreference.common.exception.BusinessException;
import com.esunbank.financialpreference.common.exception.ErrorCode;
import com.esunbank.financialpreference.data.repository.LikeListRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.List;

/**
 * 喜好清單業務邏輯。Transaction 邊界在這層：
 *   - 異動方法：@Transactional(rollbackFor = Exception.class)
 *   - 查詢方法：@Transactional(readOnly = true)
 * 同時 SP 內部已用 BEGIN TRAN / COMMIT / ROLLBACK，雙重保障。
 *
 * 將 SP 透過 THROW 拋出的錯誤（SQLException.getErrorCode）轉換成統一的 BusinessException。
 *
 * SP 錯誤碼對照（與 DB/02_stored_procedures.sql 對齊）：
 *   50001 USER_NOT_FOUND
 *   50002 INVALID_QUANTITY
 *   50003 LIKE_NOT_FOUND
 *   50004 FORBIDDEN          (SN 不屬於請求者)
 *   50005 ACCOUNT_MISMATCH
 */
@Service
public class LikeListService {

    private static final int SP_ERR_USER_NOT_FOUND   = 50001;
    private static final int SP_ERR_INVALID_QUANTITY = 50002;
    private static final int SP_ERR_LIKE_NOT_FOUND   = 50003;
    private static final int SP_ERR_FORBIDDEN        = 50004;
    private static final int SP_ERR_ACCOUNT_MISMATCH = 50005;

    private final LikeListRepository repository;

    public LikeListService(LikeListRepository repository) {
        this.repository = repository;
    }

    @Transactional(rollbackFor = Exception.class)
    public long create(CreateLikeCommand cmd) {
        try {
            return repository.insert(
                    cmd.userId(),
                    cmd.productName(),
                    cmd.price(),
                    cmd.feeRate(),
                    cmd.purchaseQuantity(),
                    cmd.account()
            );
        } catch (DataAccessException ex) {
            throw translate(ex);
        }
    }

    @Transactional(readOnly = true)
    public List<LikeItem> listByUserId(String userId) {
        return repository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public PagedResult<LikeItem> listByUserId(String userId, LikeListQuery query) {
        return repository.findByUserId(userId, query);
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(UpdateLikeCommand cmd) {
        try {
            repository.update(
                    cmd.sn(),
                    cmd.userId(),
                    cmd.productName(),
                    cmd.price(),
                    cmd.feeRate(),
                    cmd.purchaseQuantity(),
                    cmd.account()
            );
        } catch (DataAccessException ex) {
            throw translate(ex);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(long sn, String userId) {
        try {
            repository.delete(sn, userId);
        } catch (DataAccessException ex) {
            throw translate(ex);
        }
    }

    private BusinessException translate(DataAccessException ex) {
        int sqlCode = 0;
        String rootMessage = "";
        Throwable root = ex.getRootCause();
        if (root instanceof SQLException sqlEx) {
            sqlCode = sqlEx.getErrorCode();
            rootMessage = String.valueOf(sqlEx.getMessage());
        } else if (root != null) {
            rootMessage = String.valueOf(root.getMessage());
        }

        return switch (sqlCode) {
            case SP_ERR_USER_NOT_FOUND   -> new BusinessException(ErrorCode.USER_NOT_FOUND, ex);
            case SP_ERR_INVALID_QUANTITY -> new BusinessException(ErrorCode.INVALID_QUANTITY, ex);
            case SP_ERR_LIKE_NOT_FOUND   -> new BusinessException(ErrorCode.LIKE_NOT_FOUND, ex);
            case SP_ERR_FORBIDDEN        -> new BusinessException(ErrorCode.FORBIDDEN, ex);
            case SP_ERR_ACCOUNT_MISMATCH -> new BusinessException(ErrorCode.ACCOUNT_MISMATCH, ex);
            default -> translateByMessage(rootMessage, ex);
        };
    }

    private BusinessException translateByMessage(String message, DataAccessException ex) {
        if (message == null) return new BusinessException(ErrorCode.INTERNAL_ERROR, ex);
        if (message.contains("USER_NOT_FOUND"))   return new BusinessException(ErrorCode.USER_NOT_FOUND, ex);
        if (message.contains("LIKE_NOT_FOUND"))   return new BusinessException(ErrorCode.LIKE_NOT_FOUND, ex);
        if (message.contains("INVALID_QUANTITY")) return new BusinessException(ErrorCode.INVALID_QUANTITY, ex);
        if (message.contains("FORBIDDEN"))        return new BusinessException(ErrorCode.FORBIDDEN, ex);
        if (message.contains("ACCOUNT_MISMATCH")) return new BusinessException(ErrorCode.ACCOUNT_MISMATCH, ex);
        return new BusinessException(ErrorCode.INTERNAL_ERROR, ex);
    }
}
