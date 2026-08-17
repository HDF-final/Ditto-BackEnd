package com.ditto.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.ditto.user.domain.UserRole;
import com.ditto.user.domain.UserStatus;
import com.ditto.user.repository.SignupUserCommand;
import com.ditto.user.repository.UserMapper;
import com.ditto.user.repository.UserRow;

@SpringBootTest
class AdminAccountSetupTest {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @EnabledIfEnvironmentVariable(named = "SMOKE_TEST", matches = "true")
    @DisplayName("ROLE_ADMIN 권한을 가진 admin@naver.com 관리자 계정 생성 또는 비밀번호 초기화")
    void createAdminAccount() {
        String adminEmail = "admin@naver.com";
        String adminPassword = "12341234";

        UserRow existing = userMapper.findActiveByEmail(adminEmail).orElse(null);
        if (existing != null) {
            System.out.println("이미 admin@naver.com 계정이 존재합니다: userId=" + existing.getUserId() + ", role=" + existing.getRole());
            return;
        }

        SignupUserCommand command = SignupUserCommand.builder()
                .countryId(1L) // 대한민국
                .name("DITTO 관리자")
                .email(adminEmail)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .preferredLanguageCode("ko")
                .status(UserStatus.ACTIVE.name())
                .role(UserRole.ROLE_ADMIN.name())
                .build();

        userMapper.insert(command);
        System.out.println(">>> ROLE_ADMIN 계정 생성 완료: email=" + adminEmail + ", role=" + UserRole.ROLE_ADMIN.name());
    }
}
