package com.example.usertest.domain.dto.keycloak;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Keycloak 역할 관련 응답 DTO
 */
@Data
@Schema(description = "Keycloak 역할 관련 응답 정보")
public class RoleResponse {

    @Schema(description = "작업 성공 여부", example = "true")
    private boolean success;

    @Schema(description = "역할명", example = "admin")
    private String roleName;

    @Schema(description = "역할 ID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private String roleId;

    @Schema(description = "역할 설명", example = "관리자 역할")
    private String description;

    @Schema(description = "응답 시간")
    private LocalDateTime responseTime;

    @Schema(description = "성공 메시지", example = "역할이 성공적으로 생성되었습니다.")
    private String message;

    @Schema(description = "오류 메시지 (실패 시)", example = "역할이 이미 존재합니다.")
    private String errorMessage;

    /**
     * 기본 생성자
     */
    public RoleResponse() {
        this.responseTime = LocalDateTime.now();
    }

    /**
     * 성공 응답 생성자
     * 
     * @param roleName 역할명
     * @param roleId 역할 ID
     * @param message 성공 메시지
     */
    public RoleResponse(String roleName, String roleId, String message) {
        this();
        this.success = true;
        this.roleName = roleName;
        this.roleId = roleId;
        this.message = message;
    }

    /**
     * 성공 응답을 위한 팩토리 메서드
     * 
     * @param roleName 역할명
     * @param roleId 역할 ID
     * @return 성공 응답 객체
     */
    public static RoleResponse success(String roleName, String roleId) {
        return new RoleResponse(roleName, roleId, "역할 작업이 성공적으로 완료되었습니다.");
    }

    /**
     * 역할 생성 성공 응답
     * 
     * @param roleName 생성된 역할명
     * @param roleId 생성된 역할 ID
     * @return 역할 생성 성공 응답
     */
    public static RoleResponse created(String roleName, String roleId) {
        return new RoleResponse(roleName, roleId, "역할이 성공적으로 생성되었습니다.");
    }

    /**
     * 역할 할당 성공 응답
     * 
     * @param roleName 할당된 역할명
     * @return 역할 할당 성공 응답
     */
    public static RoleResponse assigned(String roleName) {
        RoleResponse response = new RoleResponse();
        response.success = true;
        response.roleName = roleName;
        response.message = "역할이 성공적으로 할당되었습니다.";
        return response;
    }

    /**
     * 실패 응답을 위한 팩토리 메서드
     * 
     * @param errorMessage 오류 메시지
     * @return 실패 응답 객체
     */
    public static RoleResponse failure(String errorMessage) {
        RoleResponse response = new RoleResponse();
        response.success = false;
        response.errorMessage = errorMessage;
        response.message = "역할 작업에 실패했습니다.";
        return response;
    }
}