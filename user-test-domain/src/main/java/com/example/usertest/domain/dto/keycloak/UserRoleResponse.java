package com.example.usertest.domain.dto.keycloak;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 사용자 역할 조회 응답 DTO
 */
@Data
@Schema(description = "사용자 역할 조회 응답 정보")
public class UserRoleResponse {

    @Schema(description = "작업 성공 여부", example = "true")
    private boolean success;

    @Schema(description = "사용자명", example = "test_user")
    private String username;

    @Schema(description = "Keycloak 사용자 ID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private String keycloakUserId;

    @Schema(description = "Realm 역할 목록", example = "[\"offline_access\", \"uma_authorization\"]")
    private List<String> realmRoles;

    @Schema(description = "클라이언트 역할 목록", example = "[\"admin\", \"user\"]")
    private List<String> clientRoles;

    @Schema(description = "모든 역할 목록 (Realm + Client)", example = "[\"admin\", \"user\", \"offline_access\"]")
    private List<String> allRoles;

    @Schema(description = "사용자가 속한 그룹 목록", example = "[\"admin_group\", \"user_group\"]")
    private List<String> groups;

    @Schema(description = "응답 시간")
    private LocalDateTime responseTime;

    @Schema(description = "성공 메시지", example = "사용자 역할 조회가 완료되었습니다.")
    private String message;

    @Schema(description = "오류 메시지 (실패 시)", example = "사용자를 찾을 수 없습니다.")
    private String errorMessage;

    /**
     * 기본 생성자
     */
    public UserRoleResponse() {
        this.responseTime = LocalDateTime.now();
    }

    /**
     * 성공 응답 생성자
     * 
     * @param username 사용자명
     * @param keycloakUserId Keycloak 사용자 ID
     */
    public UserRoleResponse(String username, String keycloakUserId) {
        this();
        this.success = true;
        this.username = username;
        this.keycloakUserId = keycloakUserId;
        this.message = "사용자 역할 조회가 완료되었습니다.";
    }

    /**
     * 성공 응답을 위한 팩토리 메서드
     * 
     * @param username 사용자명
     * @param keycloakUserId Keycloak 사용자 ID
     * @return 성공 응답 객체
     */
    public static UserRoleResponse success(String username, String keycloakUserId) {
        return new UserRoleResponse(username, keycloakUserId);
    }

    /**
     * 실패 응답을 위한 팩토리 메서드
     * 
     * @param username 사용자명
     * @param errorMessage 오류 메시지
     * @return 실패 응답 객체
     */
    public static UserRoleResponse failure(String username, String errorMessage) {
        UserRoleResponse response = new UserRoleResponse();
        response.success = false;
        response.username = username;
        response.errorMessage = errorMessage;
        response.message = "사용자 역할 조회에 실패했습니다.";
        return response;
    }

    /**
     * 주요 역할 확인 메서드
     * 
     * @return 사용자가 admin 역할을 가지고 있는지 여부
     */
    public boolean hasAdminRole() {
        return (clientRoles != null && clientRoles.contains("admin")) ||
               (realmRoles != null && realmRoles.contains("admin"));
    }

    /**
     * 주요 역할 확인 메서드
     * 
     * @return 사용자가 manager 역할을 가지고 있는지 여부
     */
    public boolean hasManagerRole() {
        return (clientRoles != null && clientRoles.contains("manager")) ||
               (realmRoles != null && realmRoles.contains("manager"));
    }

    /**
     * 주요 역할 확인 메서드
     * 
     * @return 사용자가 user 역할을 가지고 있는지 여부
     */
    public boolean hasUserRole() {
        return (clientRoles != null && clientRoles.contains("user")) ||
               (realmRoles != null && realmRoles.contains("user"));
    }
}