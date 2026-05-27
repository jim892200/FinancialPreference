package com.esunbank.financialpreference.common.security;

import com.esunbank.financialpreference.data.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 開發/容器環境 seed 用：啟動時為 PASSWORD_HASH 仍為空的 USER 填上 BCrypt('Test@1234')。
 *
 * BCrypt 是 salted、每次 build hash 都不同，因此不適合寫死於 SQL seed；
 * 改由此 Runner 啟動時呼叫 SP_USER_LIST_PENDING_PASSWORDS + SP_USER_SET_PASSWORD_HASH 補上，
 * 仍走 stored procedure（符合 CLAUDE.md §10）。
 *
 * 生產環境不應啟用此 profile；應另外提供註冊 / 改密碼流程。
 */
@Component
@Profile({"dev", "docker"})
public class DevPasswordSeedRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevPasswordSeedRunner.class);
    private static final String DEFAULT_PASSWORD = "Test@1234";

    private final UserRepository userRepository;
    private final PasswordEncoder encoder;

    public DevPasswordSeedRunner(UserRepository userRepository, PasswordEncoder encoder) {
        this.userRepository = userRepository;
        this.encoder = encoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<String> pending = userRepository.listPendingPasswords();
        if (pending.isEmpty()) {
            return;
        }
        String hash = encoder.encode(DEFAULT_PASSWORD);
        pending.forEach(userId -> userRepository.setPasswordHash(userId, hash));
        log.info("[seed] populated PASSWORD_HASH for {} user(s) with default password (dev only)", pending.size());
    }
}
