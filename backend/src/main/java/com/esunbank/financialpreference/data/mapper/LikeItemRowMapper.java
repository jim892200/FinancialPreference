package com.esunbank.financialpreference.data.mapper;

import com.esunbank.financialpreference.business.domain.LikeItem;
import com.esunbank.financialpreference.business.domain.Product;
import com.esunbank.financialpreference.business.domain.User;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 對應 SP_LIKE_QUERY_BY_USER 回傳之欄位。
 * USER.ACCOUNT 未包含在查詢結果中（LIKE_LIST.ACCOUNT 是下單當下快照），故 User.account 為 null。
 */
public class LikeItemRowMapper implements RowMapper<LikeItem> {

    @Override
    public LikeItem mapRow(ResultSet rs, int rowNum) throws SQLException {
        User user = new User(
                rs.getString("USER_ID"),
                rs.getString("USER_NAME"),
                rs.getString("EMAIL"),
                null
        );

        Product product = new Product(
                rs.getLong("PRODUCT_NO"),
                rs.getString("PRODUCT_NAME"),
                rs.getBigDecimal("PRICE"),
                rs.getBigDecimal("FEE_RATE")
        );

        return new LikeItem(
                rs.getLong("SN"),
                user,
                product,
                rs.getInt("PURCHASE_QUANTITY"),
                rs.getString("ACCOUNT"),
                rs.getBigDecimal("TOTAL_FEE"),
                rs.getBigDecimal("TOTAL_AMOUNT")
        );
    }
}
