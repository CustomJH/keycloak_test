package com.example.usertest.api.controller.real;

import com.example.usertest.api.service.UserService;
import com.example.usertest.api.service.keycloak.KeycloakAdminService;
import com.example.usertest.domain.User;
import com.example.usertest.domain.dto.UserDto;
import com.example.usertest.domain.dto.keycloak.KeycloakUserCreateRequest;
import com.example.usertest.domain.dto.keycloak.KeycloakUserCreateResponse;
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
 * 2단계: 사용자 관리 API 컨트롤러
 * Keycloak 사용자 자동 생성 및 관리 기능 제공
 * 
 * 주요 기능:
 * - Keycloak 사용자 자동 생성
 * - pulsar_system 사용자 특별 권한 설정
 * - 로컬 DB와 Keycloak 동기화
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "사용자 관리 API", description = "Keycloak 연동 사용자 생성 및 관리 API")
public class Step02UserManagementController {

    private final KeycloakAdminService keycloakAdminService;
    private final UserService userService;

    /**
     * 💫 일반 사용자 생성 API
     * Keycloak에 새로운 사용자를 생성하고 로컬 DB에도 저장
     * 
     * @param createRequest 사용자 생성 요청 정보
     * @return 사용자 생성 결과
     */
    @PostMapping("/create")
    @Operation(
        summary = "일반 사용자 생성",
        description = "Keycloak에 새로운 사용자를 생성하고 로컬 데이터베이스에도 동기화합니다. " +
                     "생성된 사용자는 기본 권한(manage-account)을 가집니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201", 
            description = "사용자 생성 성공",
            content = @Content(schema = @Schema(implementation = KeycloakUserCreateResponse.class))
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "잘못된 요청 데이터",
            content = @Content(schema = @Schema(implementation = KeycloakUserCreateResponse.class))
        ),
        @ApiResponse(
            responseCode = "409", 
            description = "이미 존재하는 사용자",
            content = @Content(schema = @Schema(implementation = KeycloakUserCreateResponse.class))
        ),
        @ApiResponse(
            responseCode = "500", 
            description = "서버 내부 오류",
            content = @Content(schema = @Schema(implementation = KeycloakUserCreateResponse.class))
        )
    })
    public Mono<ResponseEntity<KeycloakUserCreateResponse>> createUser(
            @RequestBody @Parameter(description = "사용자 생성 요청 정보") KeycloakUserCreateRequest createRequest) {
        
        log.info("👤 일반 사용자 생성 요청: {}", createRequest.getUsername());

        // 입력 데이터 검증
        if (createRequest.getUsername() == null || createRequest.getUsername().trim().isEmpty()) {
            return Mono.just(ResponseEntity.badRequest()
                    .body(KeycloakUserCreateResponse.failure("사용자명은 필수입니다.")));
        }

        if (createRequest.getEmail() == null || createRequest.getEmail().trim().isEmpty()) {
            return Mono.just(ResponseEntity.badRequest()
                    .body(KeycloakUserCreateResponse.failure("이메일은 필수입니다.")));
        }

        if (createRequest.getPassword() == null || createRequest.getPassword().trim().isEmpty()) {
            return Mono.just(ResponseEntity.badRequest()
                    .body(KeycloakUserCreateResponse.failure("비밀번호는 필수입니다.")));
        }

        // 기본 권한 설정 (일반 사용자)
        final KeycloakUserCreateRequest finalCreateRequest;
        if (createRequest.getRoles() == null || createRequest.getRoles().isEmpty()) {
            finalCreateRequest = KeycloakUserCreateRequest.forRegularUser(
                    createRequest.getUsername(),
                    createRequest.getEmail(),
                    createRequest.getPassword()
            );
            // 추가 정보가 있다면 복사
            if (createRequest.getFirstName() != null) {
                finalCreateRequest.setFirstName(createRequest.getFirstName());
            }
            if (createRequest.getLastName() != null) {
                finalCreateRequest.setLastName(createRequest.getLastName());
            }
        } else {
            finalCreateRequest = createRequest;
        }

        return keycloakAdminService.createUser(finalCreateRequest)
                .flatMap(keycloakResponse -> {
                    if (keycloakResponse.isSuccess()) {
                        // Keycloak 생성 성공 시 로컬 DB에도 저장
                        return saveToLocalDatabase(finalCreateRequest, keycloakResponse.getKeycloakUserId())
                                .map(localUser -> {
                                    log.info("✅ 로컬 DB 동기화 완료: {} → {}", 
                                            keycloakResponse.getUsername(), localUser.getId());
                                    return ResponseEntity.status(HttpStatus.CREATED).body(keycloakResponse);
                                })
                                .onErrorResume(ex -> {
                                    log.warn("⚠️ 로컬 DB 저장 실패, Keycloak 사용자는 생성됨: {}", ex.getMessage());
                                    keycloakResponse.setMessage(keycloakResponse.getMessage() + 
                                            " (주의: 로컬 DB 동기화 실패)");
                                    return Mono.just(ResponseEntity.status(HttpStatus.CREATED).body(keycloakResponse));
                                });
                    } else {
                        HttpStatus status = determineErrorStatus(keycloakResponse.getErrorMessage());
                        return Mono.just(ResponseEntity.status(status).body(keycloakResponse));
                    }
                });
    }

    /**
     * 🚀 Pulsar 시스템 사용자 생성 API
     * 특별 권한(delete-account, manage-account)을 가진 pulsar_system 사용자 생성
     * 
     * @param username 사용자명
     * @param email 이메일 주소
     * @param password 비밀번호
     * @return pulsar_system 사용자 생성 결과
     */
    @PostMapping("/create-pulsar-system")
    @Operation(
        summary = "Pulsar 시스템 사용자 생성",
        description = "특별 권한(delete-account, manage-account)을 가진 pulsar_system 사용자를 생성합니다. " +
                     "Keycloak과 로컬 DB에 모두 생성되며, pulsar_system 그룹에 자동으로 할당됩니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201", 
            description = "Pulsar 시스템 사용자 생성 성공",
            content = @Content(schema = @Schema(implementation = KeycloakUserCreateResponse.class))
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "잘못된 요청 데이터",
            content = @Content(schema = @Schema(implementation = KeycloakUserCreateResponse.class))
        ),
        @ApiResponse(
            responseCode = "409", 
            description = "이미 존재하는 사용자",
            content = @Content(schema = @Schema(implementation = KeycloakUserCreateResponse.class))
        ),
        @ApiResponse(
            responseCode = "500", 
            description = "서버 내부 오류",
            content = @Content(schema = @Schema(implementation = KeycloakUserCreateResponse.class))
        )
    })
    public Mono<ResponseEntity<KeycloakUserCreateResponse>> createPulsarSystemUser(
            @RequestParam @Parameter(description = "사용자명", example = "pulsar_admin") String username,
            @RequestParam @Parameter(description = "이메일 주소", example = "pulsar@example.com") String email,
            @RequestParam @Parameter(description = "비밀번호", example = "pulsar123!") String password) {
        
        log.info("🚀 Pulsar 시스템 사용자 생성 요청: {}", username);

        // 입력 데이터 검증
        if (username == null || username.trim().isEmpty()) {
            return Mono.just(ResponseEntity.badRequest()
                    .body(KeycloakUserCreateResponse.failure("사용자명은 필수입니다.")));
        }

        if (email == null || email.trim().isEmpty()) {
            return Mono.just(ResponseEntity.badRequest()
                    .body(KeycloakUserCreateResponse.failure("이메일은 필수입니다.")));
        }

        if (password == null || password.trim().isEmpty()) {
            return Mono.just(ResponseEntity.badRequest()
                    .body(KeycloakUserCreateResponse.failure("비밀번호는 필수입니다.")));
        }

        return keycloakAdminService.createPulsarSystemUser(username, email, password)
                .flatMap(keycloakResponse -> {
                    if (keycloakResponse.isSuccess()) {
                        // Pulsar 시스템 사용자를 로컬 DB에도 저장 (ADMIN 역할로)
                        KeycloakUserCreateRequest localRequest = KeycloakUserCreateRequest.forPulsarSystem(username, email, password);
                        
                        return saveToLocalDatabase(localRequest, keycloakResponse.getKeycloakUserId(), "ADMIN")
                                .map(localUser -> {
                                    log.info("✅ Pulsar 시스템 사용자 로컬 DB 동기화 완료: {} → {}", 
                                            keycloakResponse.getUsername(), localUser.getId());
                                    return ResponseEntity.status(HttpStatus.CREATED).body(keycloakResponse);
                                })
                                .onErrorResume(ex -> {
                                    log.warn("⚠️ Pulsar 시스템 사용자 로컬 DB 저장 실패: {}", ex.getMessage());
                                    keycloakResponse.setMessage(keycloakResponse.getMessage() + 
                                            " (주의: 로컬 DB 동기화 실패)");
                                    return Mono.just(ResponseEntity.status(HttpStatus.CREATED).body(keycloakResponse));
                                });
                    } else {
                        HttpStatus status = determineErrorStatus(keycloakResponse.getErrorMessage());
                        return Mono.just(ResponseEntity.status(status).body(keycloakResponse));
                    }
                });
    }

    /**
     * 🔍 사용자 관리 상태 확인 API
     * Keycloak Admin 서비스 연결 상태 및 토큰 발급 테스트
     * 
     * @return 상태 확인 결과
     */
    @GetMapping("/admin/health")
    @Operation(
        summary = "사용자 관리 서비스 상태 확인",
        description = "Keycloak Admin API 연결 상태와 관리자 토큰 발급이 정상적으로 작동하는지 확인합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "서비스 정상 작동"),
        @ApiResponse(responseCode = "503", description = "Keycloak Admin API 연결 실패")
    })
    public Mono<ResponseEntity<String>> checkAdminHealth() {
        log.info("🔍 사용자 관리 서비스 상태 확인");

        return keycloakAdminService.getAdminToken()
                .map(token -> {
                    log.info("✅ Keycloak Admin API 연결 정상");
                    return ResponseEntity.ok("✅ 사용자 관리 서비스 정상 작동 중");
                })
                .onErrorResume(ex -> {
                    log.error("❌ Keycloak Admin API 연결 실패: {}", ex.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                            .body("❌ Keycloak Admin API 연결 실패: " + ex.getMessage()));
                });
    }

    /**
     * 로컬 데이터베이스에 사용자 정보 저장
     * 
     * @param createRequest 사용자 생성 요청
     * @param keycloakUserId Keycloak 사용자 ID
     * @return 저장된 사용자 정보
     */
    private Mono<User> saveToLocalDatabase(KeycloakUserCreateRequest createRequest, String keycloakUserId) {
        return saveToLocalDatabase(createRequest, keycloakUserId, "USER");
    }

    /**
     * 로컬 데이터베이스에 사용자 정보 저장 (역할 지정)
     * 
     * @param createRequest 사용자 생성 요청
     * @param keycloakUserId Keycloak 사용자 ID
     * @param role 사용자 역할
     * @return 저장된 사용자 정보
     */
    private Mono<User> saveToLocalDatabase(KeycloakUserCreateRequest createRequest, String keycloakUserId, String role) {
        return Mono.fromCallable(() -> {
            UserDto userDto = UserDto.builder()
                    .username(createRequest.getUsername())
                    .email(createRequest.getEmail())
                    .role(role)
                    .enabled(createRequest.getEnabled())
                    .build();
            
            User savedUser = userService.createUser(userDto);
            
            // Keycloak 사용자 ID를 별도로 업데이트 (UserDto에 keycloakUserId 필드가 없으므로)
            // 실제로는 UserDto에 keycloakUserId 필드를 추가하거나, 별도 업데이트 로직 필요
            savedUser.setKeycloakUserId(keycloakUserId);
            
            return savedUser;
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
            if (lowerErrorMsg.contains("invalid") || lowerErrorMsg.contains("잘못된")) {
                return HttpStatus.BAD_REQUEST;
            }
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}