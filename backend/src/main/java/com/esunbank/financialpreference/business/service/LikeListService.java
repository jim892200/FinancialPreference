package com.esunbank.financialpreference.business.service;

import com.esunbank.financialpreference.business.command.CreateLikeCommand;
import com.esunbank.financialpreference.business.command.UpdateLikeCommand;
import com.esunbank.financialpreference.business.domain.LikeItem;
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
 */
@Service
public class LikeListService {

    /** SP 內部 THROW 的錯誤編號 — 必須與 DB/02_stored_procedures.sql 對齊。 */
    private static final int SP_ERR_USER_NOT_FOUND   = 50001;
    private static final int SP_ERR_INVALID_QUANTITY = 50002;
    private static final int SP_ERR_LIKE_NOT_FOUND   = 50003;

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

    @Transactional(rollbackFor = Exception.class)
    public void update(UpdateLikeCommand cmd) {
        try {
            repository.update(
                    cmd.sn(),
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
    public void delete(long sn) {
        try {
            repository.delete(sn);
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
            default -> translateByMessage(rootMessage, ex);
        };
    }

    private BusinessException translateByMessage(String message, DataAccessException ex) {
        if (message == null) return new BusinessException(ErrorCode.INTERNAL_ERROR, ex);
        if (message.contains("USER_NOT_FOUND"))   return new BusinessException(ErrorCode.USER_NOT_FOUND, ex);
        if (message.contains("LIKE_NOT_FOUND"))   return new BusinessException(ErrorCode.LIKE_NOT_FOUND, ex);
        if (message.contains("INVALID_QUANTITY")) return new BusinessException(ErrorCode.INVALID_QUANTITY, ex);
        return new BusinessException(ErrorCode.INTERNAL_ERROR, ex);
    }
}
