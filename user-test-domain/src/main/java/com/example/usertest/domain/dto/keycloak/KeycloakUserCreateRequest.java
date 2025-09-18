package com.example.usertest.domain.dto.keycloak;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * Keycloak 사용자 생성 요청 DTO
 * Keycloak Admin API를 통해 새로운 사용자를 생성할 때 사용
 */
@Data
@Schema(description = "Keycloak 사용자 생성 요청 정보")
public class KeycloakUserCreateRequest {

    @Schema(description = "사용자명 (고유값)", example = "test_user_01", required = true)
    private String username;

    @Schema(description = "이메일 주소", example = "test@example.com", required = true)
    private String email;

    @Schema(description = "성", example = "김")
    private String firstName;

    @Schema(description = "이름", example = "철수")
    private String lastName;

    @Schema(description = "초기 비밀번호", example = "password123", required = true)
    private String password;

    @Schema(description = "계정 활성화 여부", example = "true")
    private Boolean enabled = true;

    @Schema(description = "이메일 인증 여부", example = "true")
    private Boolean emailVerified = true;

    @Schema(description = "할당할 역할 목록", example = "[\"delete-account\", \"manage-account\"]")
    private List<String> roles;

    @Schema(description = "사용자 그룹 목록", example = "[\"pulsar_system\"]")
    private List<String> groups;

    /**
     * 기본 생성자
     */
    public KeycloakUserCreateRequest() {}

    /**
     * 필수 필드 생성자
     * @param username 사용자명
     * @param email 이메일
     * @param password 비밀번호
     */
    public KeycloakUserCreateRequest(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }

    /**
     * pulsar_system 사용자를 위한 팩토리 메서드
     * delete-account, manage-account 권한을 기본으로 설정
     * 
     * @param username 사용자명
     * @param email 이메일
     * @param password 비밀번호
     * @return pulsar_system용 사용자 생성 요청 객체
     */
    public static KeycloakUserCreateRequest forPulsarSystem(String username, String email, String password) {
        KeycloakUserCreateRequest request = new KeycloakUserCreateRequest(username, email, password);
        request.setRoles(List.of("delete-account", "manage-account"));
        request.setGroups(List.of("pulsar_system"));
        request.setFirstName("Pulsar");
        request.setLastName("System");
        return request;
    }

    /**
     * 일반 사용자를 위한 팩토리 메서드
     * 기본 "user" 역할과 "manage-account" 권한 설정
     * 
     * @param username 사용자명
     * @param email 이메일
     * @param password 비밀번호
     * @return 일반 사용자 생성 요청 객체
     */
    public static KeycloakUserCreateRequest forRegularUser(String username, String email, String password) {
        KeycloakUserCreateRequest request = new KeycloakUserCreateRequest(username, email, password);
        // 기본 사용자에게는 "user" 역할과 "manage-account" 권한 할당
        request.setRoles(List.of("user", "manage-account"));
        return request;
    }
}