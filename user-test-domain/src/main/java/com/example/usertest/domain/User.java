package com.example.usertest.domain;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    private Long id;
    private String username;
    private String email;
    private String password;
    private String role;
    private boolean enabled;
    private String keycloakUserId; // Keycloak 서버에서 생성된 사용자 고유 ID
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}