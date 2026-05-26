package com.esunbank.financialpreference.data.repository;

import com.esunbank.financialpreference.business.domain.LikeItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Repository 集成測試 — 需連線到實際 MS SQL Server（已執行過 DB/ 三個腳本）。
 * 透過 MSSQL_PASSWORD 環境變數判斷是否執行；未設定則跳過，避免在沒有 DB 的環境 build 失敗。
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "MSSQL_PASSWORD", matches = ".+")
class LikeListRepositoryIntegrationTest {

    private static final String SEED_USER_ID = "A1236456789";
    private static final String SEED_ACCOUNT = "1111999666";

    @Autowired
    private LikeListRepository repo;

    @Test
    void roundTrip_insertQueryUpdateDelete_recalculatesAmounts() {
        long sn = repo.insert(
                SEED_USER_ID,
                "整合測試商品",
                new BigDecimal("100.00"),
                new BigDecimal("0.0100"),
                2,
                SEED_ACCOUNT
        );

        try {
            // QUERY：新建的紀錄必須出現在清單中
            LikeItem created = findBySn(sn).orElseThrow();
            assertEquals("整合測試商品", created.product().productName());
            // 100 * 0.01 * 2 = 2.00；100 * 2 + 2 = 202.00
            assertEquals(new BigDecimal("2.00"),   created.totalFee());
            assertEquals(new BigDecimal("202.00"), created.totalAmount());

            // UPDATE：改數量與費率，金額應重算
            repo.update(
                    sn,
                    "更新後名稱",
                    new BigDecimal("200.00"),
                    new BigDecimal("0.0200"),
                    3,
                    SEED_ACCOUNT
            );
            LikeItem updated = findBySn(sn).orElseThrow();
            assertEquals("更新後名稱", updated.product().productName());
            // 200 * 0.02 * 3 = 12.00；200 * 3 + 12 = 612.00
            assertEquals(new BigDecimal("12.00"),  updated.totalFee());
            assertEquals(new BigDecimal("612.00"), updated.totalAmount());
            assertEquals(new BigDecimal("200.00"), updated.product().price());
            assertEquals(3,                        updated.purchaseQuantity());
            assertNotEquals(0L, updated.product().no());

        } finally {
            // DELETE：必須同步刪除對應 PRODUCT，再次查詢應找不到
            repo.delete(sn);
            assertTrue(findBySn(sn).isEmpty(), "deleted SN should not appear");
        }
    }

    @Test
    void findByUserId_returnsSeedRowsForKnownUser() {
        List<LikeItem> items = repo.findByUserId(SEED_USER_ID);
        assertNotNull(items);
        // seed 已含 A1236456789 兩筆喜好
        assertTrue(items.size() >= 2, "seed should provide at least 2 rows for " + SEED_USER_ID);
    }

    @Test
    void findByUserId_unknownUserReturnsEmpty() {
        List<LikeItem> items = repo.findByUserId("DOES_NOT_EXIST_ID_12345");
        assertNotNull(items);
        assertTrue(items.isEmpty());
    }

    private Optional<LikeItem> findBySn(long sn) {
        return repo.findByUserId(SEED_USER_ID).stream()
                .filter(it -> it.sn() == sn)
                .findFirst();
    }
}
