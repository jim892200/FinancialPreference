package com.esunbank.financialpreference;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test：純驗證 Spring context 可載入。
 * 用 test profile 跳過 DevPasswordSeedRunner（@Profile("dev","docker")），避免需要實際 DB。
 */
@SpringBootTest
@ActiveProfiles("test")
class FinancialPreferenceApplicationTests {

    @Test
    void contextLoads() {
    }

}
