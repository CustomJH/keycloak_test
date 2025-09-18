package com.example.usertest.domain.dto.keycloak;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * Keycloak 사용자 역할 할당 요청 DTO
 */
@Data
@Schema(description = "사용자 역할 할당 요청 정보")
public class RoleAssignRequest {

    @Schema(description = "사용자명", example = "test_user", required = true)
    private String username;

    @Schema(description = "할당할 역할 목록", example = "[\"user\", \"manager\"]", required = true)
    private List<String> roles;

    @Schema(description = "기존 역할 제거 여부", example = "false")
    private boolean removeExistingRoles = false;

    /**
     * 기본 생성자
     */
    public RoleAssignRequest() {}

    /**
     * 사용자명과 역할을 받는 생성자
     * 
     * @param username 사용자명
     * @param roles 할당할 역할 목록
     */
    public RoleAssignRequest(String username, List<String> roles) {
        this.username = username;
        this.roles = roles;
    }

    /**
     * 사용자에게 USER 역할을 할당하는 팩토리 메서드
     * 
     * @param username 사용자명
     * @return USER 역할 할당 요청
     */
    public static RoleAssignRequest assignUser(String username) {
        return new RoleAssignRequest(username, List.of("user"));
    }

    /**
     * 사용자에게 ADMIN 역할을 할당하는 팩토리 메서드
     * 
     * @param username 사용자명
     * @return ADMIN 역할 할당 요청
     */
    public static RoleAssignRequest assignAdmin(String username) {
        return new RoleAssignRequest(username, List.of("admin"));
    }

    /**
     * 사용자에게 MANAGER 역할을 할당하는 팩토리 메서드
     * 
     * @param username 사용자명
     * @return MANAGER 역할 할당 요청
     */
    public static RoleAssignRequest assignManager(String username) {
        return new RoleAssignRequest(username, List.of("manager"));
    }

    /**
     * 기존 역할을 모두 제거하고 새 역할을 할당하는 요청 생성
     * 
     * @param username 사용자명
     * @param roles 새로 할당할 역할 목록
     * @return 역할 교체 요청
     */
    public static RoleAssignRequest replaceRoles(String username, List<String> roles) {
        RoleAssignRequest request = new RoleAssignRequest(username, roles);
        request.setRemoveExistingRoles(true);
        return request;
    }
}