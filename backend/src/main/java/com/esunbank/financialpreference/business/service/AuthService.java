package com.esunbank.financialpreference.business.service;

import com.esunbank.financialpreference.common.exception.BusinessException;
import com.esunbank.financialpreference.common.exception.ErrorCode;
import com.esunbank.financialpreference.common.security.JwtService;
import com.esunbank.financialpreference.data.repository.UserRepository;
import com.esunbank.financialpreference.data.repository.UserRepository.UserCredential;
import com.esunbank.financialpreference.presentation.dto.response.LoginResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 登入：查 USER_ID → 比對 BCrypt → 簽發 JWT。
 *
 * 認證失敗時統一拋 INVALID_CREDENTIALS（不區分「使用者不存在」與「密碼錯誤」），
 * 避免帳號列舉（user enumeration）攻擊。
 */
@Service
public class AuthService {

    private static final String DUMMY_BCRYPT =
            "$2a$10$0000000000000000000000.00000000000000000000000000000000";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(String userId, String rawPassword) {
        UserCredential cred = userRepository.findCredentialByUserId(userId).orElse(null);

        // 即使 user 不存在也跑一次 BCrypt 比對，避免 timing attack 區分帳號是否存在
        String hash = (cred == null || cred.passwordHash() == null || cred.passwordHash().isBlank())
                ? DUMMY_BCRYPT
                : cred.passwordHash();

        boolean ok = passwordEncoder.matches(rawPassword, hash);
        if (!ok || cred == null || cred.passwordHash() == null || cred.passwordHash().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        JwtService.IssuedToken issued = jwtService.issue(cred.userId());
        return new LoginResponse(
                issued.token(),
                "Bearer",
                issued.expiresInSeconds(),
                cred.userId(),
                cred.userName(),
                cred.account()
        );
    }
}
