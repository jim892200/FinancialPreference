package com.esunbank.financialpreference.data.repository;

import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Types;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 走 SP_USER_LOGIN_LOOKUP 取得登入驗證所需資料。
 * 不在 Java 端拼接 SQL。
 */
@Repository
public class UserRepository {

    private static final String P_USER_ID        = "USER_ID";
    private static final String P_PASSWORD_HASH  = "PASSWORD_HASH";
    private static final String RS_USER          = "user";
    private static final String RS_PENDING       = "pending";

    private final SimpleJdbcCall loginLookupCall;
    private final SimpleJdbcCall listPendingPasswordsCall;
    private final SimpleJdbcCall setPasswordHashCall;

    public UserRepository(DataSource dataSource) {
        this.loginLookupCall = new SimpleJdbcCall(dataSource)
                .withProcedureName("SP_USER_LOGIN_LOOKUP")
                .withoutProcedureColumnMetaDataAccess()
                .returningResultSet(RS_USER, (rs, rowNum) -> new UserCredential(
                        rs.getString("USER_ID"),
                        rs.getString("USER_NAME"),
                        rs.getString("EMAIL"),
                        rs.getString("ACCOUNT"),
                        rs.getString("PASSWORD_HASH")))
                .declareParameters(new SqlParameter(P_USER_ID, Types.VARCHAR));

        this.listPendingPasswordsCall = new SimpleJdbcCall(dataSource)
                .withProcedureName("SP_USER_LIST_PENDING_PASSWORDS")
                .withoutProcedureColumnMetaDataAccess()
                .returningResultSet(RS_PENDING, (rs, rowNum) -> rs.getString("USER_ID"));

        this.setPasswordHashCall = new SimpleJdbcCall(dataSource)
                .withProcedureName("SP_USER_SET_PASSWORD_HASH")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter(P_USER_ID,       Types.VARCHAR),
                        new SqlParameter(P_PASSWORD_HASH, Types.VARCHAR));
    }

    @SuppressWarnings("unchecked")
    public List<String> listPendingPasswords() {
        Map<String, Object> out = listPendingPasswordsCall.execute(new MapSqlParameterSource());
        return (List<String>) out.getOrDefault(RS_PENDING, List.of());
    }

    public void setPasswordHash(String userId, String passwordHash) {
        setPasswordHashCall.execute(new MapSqlParameterSource()
                .addValue(P_USER_ID,       userId,       Types.VARCHAR)
                .addValue(P_PASSWORD_HASH, passwordHash, Types.VARCHAR));
    }

    @SuppressWarnings("unchecked")
    public Optional<UserCredential> findCredentialByUserId(String userId) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue(P_USER_ID, userId, Types.VARCHAR);
        Map<String, Object> out = loginLookupCall.execute(params);
        List<UserCredential> rows = (List<UserCredential>) out.getOrDefault(RS_USER, List.of());
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    public record UserCredential(
            String userId,
            String userName,
            String email,
            String account,
            String passwordHash
    ) {}
}
