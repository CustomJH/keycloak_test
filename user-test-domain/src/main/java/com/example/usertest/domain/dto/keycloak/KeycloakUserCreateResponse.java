package com.example.usertest.domain.dto.keycloak;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Keycloak 사용자 생성 응답 DTO
 * Keycloak Admin API를 통한 사용자 생성 결과 정보
 */
@Data
@Schema(description = "Keycloak 사용자 생성 응답 정보")
public class KeycloakUserCreateResponse {

    @Schema(description = "생성 성공 여부", example = "true")
    private boolean success;

    @Schema(description = "Keycloak에서 생성된 사용자 ID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private String keycloakUserId;

    @Schema(description = "생성된 사용자명", example = "test_user_01")
    private String username;

    @Schema(description = "생성된 이메일", example = "test@example.com")
    private String email;

    @Schema(description = "할당된 역할 목록", example = "[\"delete-account\", \"manage-account\"]")
    private List<String> assignedRoles;

    @Schema(description = "할당된 그룹 목록", example = "[\"pulsar_system\"]")
    private List<String> assignedGroups;

    @Schema(description = "생성 시간")
    private LocalDateTime createdAt;

    @Schema(description = "결과 메시지", example = "사용자가 성공적으로 생성되었습니다.")
    private String message;

    @Schema(description = "오류 메시지 (실패 시)", example = "사용자명이 이미 존재합니다.")
    private String errorMessage;

    /**
     * 기본 생성자
     */
    public KeycloakUserCreateResponse() {
        this.createdAt = LocalDateTime.now();
    }

    /**
     * 성공 응답 생성자
     * 
     * @param keycloakUserId Keycloak 사용자 ID
     * @param username 사용자명
     * @param email 이메일
     * @param message 성공 메시지
     */
    public KeycloakUserCreateResponse(String keycloakUserId, String username, String email, String message) {
        this();
        this.success = true;
        this.keycloakUserId = keycloakUserId;
        this.username = username;
        this.email = email;
        this.message = message;
    }

    /**
     * 성공 응답을 위한 팩토리 메서드
     * 
     * @param keycloakUserId Keycloak 사용자 ID
     * @param username 사용자명
     * @param email 이메일
     * @return 성공 응답 객체
     */
    public static KeycloakUserCreateResponse success(String keycloakUserId, String username, String email) {
        return new KeycloakUserCreateResponse(keycloakUserId, username, email, "사용자가 성공적으로 생성되었습니다.");
    }

    /**
     * 실패 응답을 위한 팩토리 메서드
     * 
     * @param errorMessage 오류 메시지
     * @return 실패 응답 객체
     */
    public static KeycloakUserCreateResponse failure(String errorMessage) {
        KeycloakUserCreateResponse response = new KeycloakUserCreateResponse();
        response.success = false;
        response.errorMessage = errorMessage;
        response.message = "사용자 생성에 실패했습니다.";
        return response;
    }

    /**
     * pulsar_system 사용자 생성 성공 응답
     * 
     * @param keycloakUserId Keycloak 사용자 ID
     * @param username 사용자명
     * @param email 이메일
     * @return pulsar_system용 성공 응답
     */
    public static KeycloakUserCreateResponse pulsarSystemSuccess(String keycloakUserId, String username, String email) {
        KeycloakUserCreateResponse response = success(keycloakUserId, username, email);
        response.setAssignedRoles(List.of("delete-account", "manage-account"));
        response.setAssignedGroups(List.of("pulsar_system"));
        response.setMessage("Pulsar 시스템 사용자가 성공적으로 생성되었습니다.");
        return response;
    }
}