package com.esunbank.financialpreference.data.repository;

import com.esunbank.financialpreference.business.domain.LikeItem;
import com.esunbank.financialpreference.business.query.LikeListQuery;
import com.esunbank.financialpreference.business.query.PagedResult;
import com.esunbank.financialpreference.data.mapper.LikeItemRowMapper;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Types;
import java.util.List;
import java.util.Map;

/**
 * 所有資料異動透過 Stored Procedure 完成（具名參數），不在 Java 端拼接 SQL。
 *
 * 對應 SP：
 *   - SP_LIKE_INSERT         (OUT @NEW_SN，含 ACCOUNT 一致性檢查)
 *   - SP_LIKE_QUERY_BY_USER  (回傳 ResultSet)
 *   - SP_LIKE_UPDATE         (含 @USER_ID ownership 檢查)
 *   - SP_LIKE_DELETE         (含 @USER_ID ownership 檢查)
 */
@Repository
public class LikeListRepository {

    private static final String P_USER_ID            = "USER_ID";
    private static final String P_PRODUCT_NAME       = "PRODUCT_NAME";
    private static final String P_PRICE              = "PRICE";
    private static final String P_FEE_RATE           = "FEE_RATE";
    private static final String P_PURCHASE_QUANTITY  = "PURCHASE_QUANTITY";
    private static final String P_ACCOUNT            = "ACCOUNT";
    private static final String P_SN                 = "SN";
    private static final String P_NEW_SN             = "NEW_SN";
    private static final String P_AMOUNT_MIN         = "AMOUNT_MIN";
    private static final String P_AMOUNT_MAX         = "AMOUNT_MAX";
    private static final String P_FEE_RATE_MIN       = "FEE_RATE_MIN";
    private static final String P_FEE_RATE_MAX       = "FEE_RATE_MAX";
    private static final String P_SORT_BY            = "SORT_BY";
    private static final String P_SORT_DIR           = "SORT_DIR";
    private static final String P_PAGE               = "PAGE";
    private static final String P_PAGE_SIZE          = "PAGE_SIZE";
    private static final String P_TOTAL              = "TOTAL";
    private static final String RS_ITEMS             = "items";

    private final SimpleJdbcCall insertCall;
    private final SimpleJdbcCall queryByUserCall;
    private final SimpleJdbcCall updateCall;
    private final SimpleJdbcCall deleteCall;

    public LikeListRepository(DataSource dataSource) {
        this.insertCall = new SimpleJdbcCall(dataSource)
                .withProcedureName("SP_LIKE_INSERT")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter(P_USER_ID,           Types.VARCHAR),
                        new SqlParameter(P_PRODUCT_NAME,      Types.NVARCHAR),
                        new SqlParameter(P_PRICE,             Types.DECIMAL),
                        new SqlParameter(P_FEE_RATE,          Types.DECIMAL),
                        new SqlParameter(P_PURCHASE_QUANTITY, Types.INTEGER),
                        new SqlParameter(P_ACCOUNT,           Types.VARCHAR),
                        new SqlOutParameter(P_NEW_SN,         Types.BIGINT)
                );

        this.queryByUserCall = new SimpleJdbcCall(dataSource)
                .withProcedureName("SP_LIKE_QUERY_BY_USER")
                .withoutProcedureColumnMetaDataAccess()
                .returningResultSet(RS_ITEMS, new LikeItemRowMapper())
                .declareParameters(
                        new SqlParameter(P_USER_ID,       Types.VARCHAR),
                        new SqlParameter(P_PRODUCT_NAME,  Types.NVARCHAR),
                        new SqlParameter(P_ACCOUNT,       Types.VARCHAR),
                        new SqlParameter(P_AMOUNT_MIN,    Types.DECIMAL),
                        new SqlParameter(P_AMOUNT_MAX,    Types.DECIMAL),
                        new SqlParameter(P_FEE_RATE_MIN,  Types.DECIMAL),
                        new SqlParameter(P_FEE_RATE_MAX,  Types.DECIMAL),
                        new SqlParameter(P_SORT_BY,       Types.VARCHAR),
                        new SqlParameter(P_SORT_DIR,      Types.VARCHAR),
                        new SqlParameter(P_PAGE,          Types.INTEGER),
                        new SqlParameter(P_PAGE_SIZE,     Types.INTEGER),
                        new SqlOutParameter(P_TOTAL,      Types.BIGINT)
                );

        this.updateCall = new SimpleJdbcCall(dataSource)
                .withProcedureName("SP_LIKE_UPDATE")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter(P_SN,                Types.BIGINT),
                        new SqlParameter(P_USER_ID,           Types.VARCHAR),
                        new SqlParameter(P_PRODUCT_NAME,      Types.NVARCHAR),
                        new SqlParameter(P_PRICE,             Types.DECIMAL),
                        new SqlParameter(P_FEE_RATE,          Types.DECIMAL),
                        new SqlParameter(P_PURCHASE_QUANTITY, Types.INTEGER),
                        new SqlParameter(P_ACCOUNT,           Types.VARCHAR)
                );

        this.deleteCall = new SimpleJdbcCall(dataSource)
                .withProcedureName("SP_LIKE_DELETE")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter(P_SN,      Types.BIGINT),
                        new SqlParameter(P_USER_ID, Types.VARCHAR)
                );
    }

    public long insert(String userId,
                       String productName,
                       BigDecimal price,
                       BigDecimal feeRate,
                       int purchaseQuantity,
                       String account) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue(P_USER_ID,           userId,            Types.VARCHAR)
                .addValue(P_PRODUCT_NAME,      productName,       Types.NVARCHAR)
                .addValue(P_PRICE,             price,             Types.DECIMAL)
                .addValue(P_FEE_RATE,          feeRate,           Types.DECIMAL)
                .addValue(P_PURCHASE_QUANTITY, purchaseQuantity,  Types.INTEGER)
                .addValue(P_ACCOUNT,           account,           Types.VARCHAR);
        Map<String, Object> out = insertCall.execute(params);
        return ((Number) out.get(P_NEW_SN)).longValue();
    }

    public List<LikeItem> findByUserId(String userId) {
        return findByUserId(userId, LikeListQuery.empty()).items();
    }

    @SuppressWarnings("unchecked")
    public PagedResult<LikeItem> findByUserId(String userId, LikeListQuery query) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue(P_USER_ID,      userId,              Types.VARCHAR)
                .addValue(P_PRODUCT_NAME, query.productName(), Types.NVARCHAR)
                .addValue(P_ACCOUNT,      query.account(),     Types.VARCHAR)
                .addValue(P_AMOUNT_MIN,   query.amountMin(),   Types.DECIMAL)
                .addValue(P_AMOUNT_MAX,   query.amountMax(),   Types.DECIMAL)
                .addValue(P_FEE_RATE_MIN, query.feeRateMin(),  Types.DECIMAL)
                .addValue(P_FEE_RATE_MAX, query.feeRateMax(),  Types.DECIMAL)
                .addValue(P_SORT_BY,      query.sortBy(),      Types.VARCHAR)
                .addValue(P_SORT_DIR,     query.sortDir(),     Types.VARCHAR)
                .addValue(P_PAGE,         query.page(),        Types.INTEGER)
                .addValue(P_PAGE_SIZE,    query.pageSize(),    Types.INTEGER);
        Map<String, Object> out = queryByUserCall.execute(params);
        List<LikeItem> items = out.get(RS_ITEMS) == null
                ? List.of()
                : (List<LikeItem>) out.get(RS_ITEMS);
        Object totalObj = out.get(P_TOTAL);
        long total = totalObj instanceof Number n ? n.longValue() : 0L;
        return new PagedResult<>(items, total, query.page(), query.pageSize());
    }

    public void update(long sn,
                       String userId,
                       String productName,
                       BigDecimal price,
                       BigDecimal feeRate,
                       int purchaseQuantity,
                       String account) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue(P_SN,                sn,                Types.BIGINT)
                .addValue(P_USER_ID,           userId,            Types.VARCHAR)
                .addValue(P_PRODUCT_NAME,      productName,       Types.NVARCHAR)
                .addValue(P_PRICE,             price,             Types.DECIMAL)
                .addValue(P_FEE_RATE,          feeRate,           Types.DECIMAL)
                .addValue(P_PURCHASE_QUANTITY, purchaseQuantity,  Types.INTEGER)
                .addValue(P_ACCOUNT,           account,           Types.VARCHAR);
        updateCall.execute(params);
    }

    public void delete(long sn, String userId) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue(P_SN,      sn,     Types.BIGINT)
                .addValue(P_USER_ID, userId, Types.VARCHAR);
        deleteCall.execute(params);
    }
}
