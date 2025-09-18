package com.example.usertest.domain.dto.keycloak;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Keycloak 역할 생성 요청 DTO
 */
@Data
@Schema(description = "Keycloak 역할 생성 요청 정보")
public class RoleCreateRequest {

    @Schema(description = "역할명", example = "admin", required = true)
    private String roleName;

    @Schema(description = "역할 설명", example = "관리자 역할")
    private String description;

    /**
     * 기본 생성자
     */
    public RoleCreateRequest() {}

    /**
     * 역할명과 설명을 받는 생성자
     * 
     * @param roleName 역할명
     * @param description 역할 설명
     */
    public RoleCreateRequest(String roleName, String description) {
        this.roleName = roleName;
        this.description = description;
    }

    /**
     * ADMIN 역할 생성을 위한 팩토리 메서드
     */
    public static RoleCreateRequest admin() {
        return new RoleCreateRequest("admin", "관리자 - 시스템의 모든 기능에 접근 가능");
    }

    /**
     * USER 역할 생성을 위한 팩토리 메서드
     */
    public static RoleCreateRequest user() {
        return new RoleCreateRequest("user", "일반 사용자 - 기본적인 기능 사용 가능");
    }

    /**
     * MANAGER 역할 생성을 위한 팩토리 메서드
     */
    public static RoleCreateRequest manager() {
        return new RoleCreateRequest("manager", "매니저 - 중간 관리 권한 보유");
    }
}