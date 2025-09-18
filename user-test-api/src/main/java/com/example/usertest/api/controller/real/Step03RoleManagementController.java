package com.example.usertest.api.controller.real;

import com.example.usertest.api.service.keycloak.KeycloakAdminService;
import com.example.usertest.domain.dto.keycloak.RoleAssignRequest;
import com.example.usertest.domain.dto.keycloak.RoleCreateRequest;
import com.example.usertest.domain.dto.keycloak.RoleResponse;
import com.example.usertest.domain.dto.keycloak.UserRoleResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * 3단계: 역할 관리 API 컨트롤러
 * Keycloak 역할 생성, 할당, 조회 기능 제공
 * 
 * 주요 기능:
 * - Keycloak 역할 생성 (admin, user, manager)
 * - 사용자에게 역할 할당/제거
 * - 사용자 역할 조회
 * - 기본 역할 일괄 생성
 */
@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "역할 관리 API", description = "Keycloak 연동 역할 생성 및 관리 API")
public class Step03RoleManagementController {

    private final KeycloakAdminService keycloakAdminService;

    /**
     * 🎯 개별 역할 생성 API
     * Keycloak에 새로운 역할을 생성
     * 
     * @param roleRequest 역할 생성 요청 정보
     * @return 역할 생성 결과
     */
    @PostMapping("/create")
    @Operation(
        summary = "개별 역할 생성",
        description = "Keycloak에 새로운 역할을 생성합니다. 역할명과 설명을 포함할 수 있습니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201", 
            description = "역할 생성 성공",
            content = @Content(schema = @Schema(implementation = RoleResponse.class))
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "잘못된 요청 데이터",
            content = @Content(schema = @Schema(implementation = RoleResponse.class))
        ),
        @ApiResponse(
            responseCode = "409", 
            description = "이미 존재하는 역할",
            content = @Content(schema = @Schema(implementation = RoleResponse.class))
        ),
        @ApiResponse(
            responseCode = "500", 
            description = "서버 내부 오류",
            content = @Content(schema = @Schema(implementation = RoleResponse.class))
        )
    })
    public Mono<ResponseEntity<RoleResponse>> createRole(
            @RequestBody @Parameter(description = "역할 생성 요청 정보") RoleCreateRequest roleRequest) {
        
        log.info("🎯 역할 생성 요청: {}", roleRequest.getRoleName());

        // 입력 데이터 검증
        if (roleRequest.getRoleName() == null || roleRequest.getRoleName().trim().isEmpty()) {
            return Mono.just(ResponseEntity.badRequest()
                    .body(RoleResponse.failure("역할명은 필수입니다.")));
        }

        return keycloakAdminService.createRole(roleRequest)
                .map(roleResponse -> {
                    if (roleResponse.isSuccess()) {
                        log.info("✅ 역할 생성 완료: {}", roleResponse.getRoleName());
                        return ResponseEntity.status(HttpStatus.CREATED).body(roleResponse);
                    } else {
                        log.warn("❌ 역할 생성 실패: {}", roleResponse.getErrorMessage());
                        HttpStatus status = determineErrorStatus(roleResponse.getErrorMessage());
                        return ResponseEntity.status(status).body(roleResponse);
                    }
                })
                .onErrorResume(ex -> {
                    log.error("❌ 역할 생성 중 예외 발생: {}", ex.getMessage(), ex);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(RoleResponse.failure("역할 생성 중 내부 오류가 발생했습니다: " + ex.getMessage())));
                });
    }

    /**
     * 🏗️ 기본 역할 일괄 생성 API
     * admin, user, manager 역할을 한번에 생성
     * 
     * @return 기본 역할 생성 결과
     */
    @PostMapping("/create-defaults")
    @Operation(
        summary = "기본 역할 일괄 생성",
        description = "시스템에서 사용할 기본 역할들(admin, user, manager)을 한번에 생성합니다. " +
                     "이미 존재하는 역할은 건너뛰고 없는 역할만 생성합니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "기본 역할 생성 완료",
            content = @Content(schema = @Schema(implementation = String.class))
        ),
        @ApiResponse(
            responseCode = "500", 
            description = "기본 역할 생성 중 오류",
            content = @Content(schema = @Schema(implementation = String.class))
        )
    })
    public Mono<ResponseEntity<String>> createDefaultRoles() {
        
        log.info("🏗️ 기본 역할 일괄 생성 요청 (admin, user, manager)");

        return keycloakAdminService.createDefaultRoles()
                .map(result -> {
                    log.info("✅ 기본 역할 생성 완료");
                    return ResponseEntity.ok("✅ 기본 역할(admin, user, manager) 생성이 완료되었습니다.");
                })
                .onErrorResume(ex -> {
                    log.error("❌ 기본 역할 생성 중 오류: {}", ex.getMessage(), ex);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("❌ 기본 역할 생성 중 오류가 발생했습니다: " + ex.getMessage()));
                });
    }

    /**
     * 👤 사용자 역할 할당 API
     * 특정 사용자에게 하나 이상의 역할을 할당
     * 
     * @param assignRequest 역할 할당 요청 정보
     * @return 역할 할당 결과
     */
    @PostMapping("/assign")
    @Operation(
        summary = "사용자 역할 할당",
        description = "특정 사용자에게 하나 이상의 역할을 할당합니다. " +
                     "기존 역할을 유지하거나 제거할 수 있는 옵션을 제공합니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "역할 할당 성공",
            content = @Content(schema = @Schema(implementation = RoleResponse.class))
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "잘못된 요청 데이터",
            content = @Content(schema = @Schema(implementation = RoleResponse.class))
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "사용자 또는 역할을 찾을 수 없음",
            content = @Content(schema = @Schema(implementation = RoleResponse.class))
        ),
        @ApiResponse(
            responseCode = "500", 
            description = "서버 내부 오류",
            content = @Content(schema = @Schema(implementation = RoleResponse.class))
        )
    })
    public Mono<ResponseEntity<RoleResponse>> assignRolesToUser(
            @RequestBody @Parameter(description = "역할 할당 요청 정보") RoleAssignRequest assignRequest) {
        
        log.info("👤 사용자 역할 할당 요청: {} → {}", assignRequest.getUsername(), assignRequest.getRoles());

        // 입력 데이터 검증
        if (assignRequest.getUsername() == null || assignRequest.getUsername().trim().isEmpty()) {
            return Mono.just(ResponseEntity.badRequest()
                    .body(RoleResponse.failure("사용자명은 필수입니다.")));
        }

        if (assignRequest.getRoles() == null || assignRequest.getRoles().isEmpty()) {
            return Mono.just(ResponseEntity.badRequest()
                    .body(RoleResponse.failure("할당할 역할이 최소 하나는 있어야 합니다.")));
        }

        return keycloakAdminService.assignRolesToUser(assignRequest)
                .map(roleResponse -> {
                    if (roleResponse.isSuccess()) {
                        log.info("✅ 역할 할당 완료: {} → {}", assignRequest.getUsername(), assignRequest.getRoles());
                        return ResponseEntity.ok(roleResponse);
                    } else {
                        log.warn("❌ 역할 할당 실패: {}", roleResponse.getErrorMessage());
                        HttpStatus status = determineErrorStatus(roleResponse.getErrorMessage());
                        return ResponseEntity.status(status).body(roleResponse);
                    }
                })
                .onErrorResume(ex -> {
                    log.error("❌ 역할 할당 중 예외 발생: {}", ex.getMessage(), ex);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(RoleResponse.failure("역할 할당 중 내부 오류가 발생했습니다: " + ex.getMessage())));
                });
    }

    /**
     * 🔍 사용자 역할 조회 API
     * 특정 사용자의 현재 역할 정보를 조회
     * 
     * @param username 조회할 사용자명
     * @return 사용자 역할 정보
     */
    @GetMapping("/user/{username}")
    @Operation(
        summary = "사용자 역할 조회",
        description = "특정 사용자의 현재 역할 정보(realm 역할, client 역할, 그룹)를 조회합니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "사용자 역할 조회 성공",
            content = @Content(schema = @Schema(implementation = UserRoleResponse.class))
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "사용자를 찾을 수 없음",
            content = @Content(schema = @Schema(implementation = UserRoleResponse.class))
        ),
        @ApiResponse(
            responseCode = "500", 
            description = "서버 내부 오류",
            content = @Content(schema = @Schema(implementation = UserRoleResponse.class))
        )
    })
    public Mono<ResponseEntity<UserRoleResponse>> getUserRoles(
            @PathVariable @Parameter(description = "조회할 사용자명", example = "test_user") String username) {
        
        log.info("🔍 사용자 역할 조회 요청: {}", username);

        // 사용자명 검증
        if (username == null || username.trim().isEmpty()) {
            return Mono.just(ResponseEntity.badRequest()
                    .body(UserRoleResponse.failure(username, "사용자명은 필수입니다.")));
        }

        return keycloakAdminService.getUserRoles(username)
                .map(userRoleResponse -> {
                    if (userRoleResponse.isSuccess()) {
                        log.info("✅ 사용자 역할 조회 완료: {} → {}", username, userRoleResponse.getAllRoles());
                        return ResponseEntity.ok(userRoleResponse);
                    } else {
                        log.warn("❌ 사용자 역할 조회 실패: {}", userRoleResponse.getErrorMessage());
                        HttpStatus status = determineErrorStatus(userRoleResponse.getErrorMessage());
                        return ResponseEntity.status(status).body(userRoleResponse);
                    }
                })
                .onErrorResume(ex -> {
                    log.error("❌ 사용자 역할 조회 중 예외 발생: {}", ex.getMessage(), ex);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(UserRoleResponse.failure(username, "사용자 역할 조회 중 내부 오류가 발생했습니다: " + ex.getMessage())));
                });
    }

    /**
     * 📋 역할 관리 서비스 상태 확인 API
     * Keycloak Admin 역할 관리 기능 상태 확인
     * 
     * @return 상태 확인 결과
     */
    @GetMapping("/admin/health")
    @Operation(
        summary = "역할 관리 서비스 상태 확인",
        description = "Keycloak Admin API의 역할 관리 기능이 정상적으로 작동하는지 확인합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "역할 관리 서비스 정상 작동"),
        @ApiResponse(responseCode = "503", description = "Keycloak Admin API 연결 실패")
    })
    public Mono<ResponseEntity<String>> checkRoleManagementHealth() {
        log.info("📋 역할 관리 서비스 상태 확인");

        return keycloakAdminService.getAdminToken()
                .map(token -> {
                    log.info("✅ 역할 관리 서비스 연결 정상");
                    return ResponseEntity.ok("✅ 역할 관리 서비스 정상 작동 중");
                })
                .onErrorResume(ex -> {
                    log.error("❌ 역할 관리 서비스 연결 실패: {}", ex.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                            .body("❌ Keycloak Admin API 연결 실패: " + ex.getMessage()));
                });
    }

    /**
     * 오류 메시지에 따른 HTTP 상태 코드 결정
     * 
     * @param errorMessage 오류 메시지
     * @return 적절한 HTTP 상태 코드
     */
    private HttpStatus determineErrorStatus(String errorMessage) {
        if (errorMessage != null) {
            String lowerErrorMsg = errorMessage.toLowerCase();
            if (lowerErrorMsg.contains("already exists") || lowerErrorMsg.contains("이미 존재")) {
                return HttpStatus.CONFLICT;
            }
            if (lowerErrorMsg.contains("not found") || lowerErrorMsg.contains("찾을 수 없")) {
                return HttpStatus.NOT_FOUND;
            }
            if (lowerErrorMsg.contains("invalid") || lowerErrorMsg.contains("잘못된")) {
                return HttpStatus.BAD_REQUEST;
            }
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}