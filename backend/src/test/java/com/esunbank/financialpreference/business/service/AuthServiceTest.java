package com.esunbank.financialpreference.business.service;

import com.esunbank.financialpreference.common.exception.BusinessException;
import com.esunbank.financialpreference.common.exception.ErrorCode;
import com.esunbank.financialpreference.common.security.JwtService;
import com.esunbank.financialpreference.data.repository.UserRepository;
import com.esunbank.financialpreference.data.repository.UserRepository.UserCredential;
import com.esunbank.financialpreference.presentation.dto.response.LoginResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder encoder;
    @Mock JwtService jwtService;

    @Test
    void login_validCredentials_returnsToken() {
        UserCredential cred = new UserCredential("A1", "王o明", "x@y", "111", "$2a$10$hash");
        when(userRepository.findCredentialByUserId("A1")).thenReturn(Optional.of(cred));
        when(encoder.matches("pwd", "$2a$10$hash")).thenReturn(true);
        when(jwtService.issue("A1")).thenReturn(new JwtService.IssuedToken("the.jwt", 3600));

        AuthService svc = new AuthService(userRepository, encoder, jwtService);
        LoginResponse resp = svc.login("A1", "pwd");

        assertEquals("the.jwt", resp.token());
        assertEquals("Bearer", resp.tokenType());
        assertEquals(3600, resp.expiresIn());
        assertEquals("A1", resp.userId());
        assertEquals("王o明", resp.userName());
        assertEquals("111", resp.account());
    }

    @Test
    void login_wrongPassword_throwsInvalidCredentials() {
        UserCredential cred = new UserCredential("A1", "王o明", "x@y", "111", "$2a$10$hash");
        when(userRepository.findCredentialByUserId("A1")).thenReturn(Optional.of(cred));
        when(encoder.matches(anyString(), eq("$2a$10$hash"))).thenReturn(false);

        AuthService svc = new AuthService(userRepository, encoder, jwtService);
        BusinessException ex = assertThrows(BusinessException.class, () -> svc.login("A1", "bad"));
        assertSame(ErrorCode.INVALID_CREDENTIALS, ex.errorCode());
    }

    @Test
    void login_unknownUser_throwsInvalidCredentials_andStillRunsBcryptToPreventTiming() {
        when(userRepository.findCredentialByUserId("ghost")).thenReturn(Optional.empty());
        // 即便 user 不存在，AuthService 也應呼叫一次 encoder.matches 以避免 timing attack
        when(encoder.matches(anyString(), anyString())).thenReturn(false);

        AuthService svc = new AuthService(userRepository, encoder, jwtService);
        BusinessException ex = assertThrows(BusinessException.class, () -> svc.login("ghost", "x"));
        assertSame(ErrorCode.INVALID_CREDENTIALS, ex.errorCode());
        verify(encoder).matches(eq("x"), anyString());
    }

    @Test
    void login_emptyHash_throwsInvalidCredentials() {
        UserCredential cred = new UserCredential("A1", "n", "x@y", "111", "");
        when(userRepository.findCredentialByUserId("A1")).thenReturn(Optional.of(cred));
        when(encoder.matches(anyString(), anyString())).thenReturn(false);

        AuthService svc = new AuthService(userRepository, encoder, jwtService);
        BusinessException ex = assertThrows(BusinessException.class, () -> svc.login("A1", "x"));
        assertSame(ErrorCode.INVALID_CREDENTIALS, ex.errorCode());
    }
}
