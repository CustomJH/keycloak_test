package com.example.usertest.api.controller.real;

import com.example.usertest.api.service.UserService;
import com.example.usertest.api.service.auth.KeycloakTokenService;
import com.example.usertest.domain.User;
import com.example.usertest.domain.dto.auth.LoginRequest;
import com.example.usertest.domain.dto.auth.TokenResponse;
import com.example.usertest.domain.exception.UserNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Step01 토큰 관리 컨트롤러 - DB 우선 인증 방식
 * 
 * 이 컨트롤러는 "데이터베이스 우선" 인증 전략을 구현합니다:
 * 1. 먼저 우리 서비스의 데이터베이스에서 사용자 존재 여부 확인
 * 2. 사용자가 존재하고 활성화되어 있으면 Keycloak을 통해 JWT 토큰 발급
 * 3. 사용자가 존재하지 않으면 회원가입이 필요하다는 응답 반환
 * 
 * 이 방식의 장점:
 * - 우리 시스템에 등록된 사용자만 로그인 가능
 * - DB와 Keycloak 간의 데이터 일관성 보장
 * - 사용자별 세부 설정 및 권한 관리 용이
 * 
 * @author YourName
 * @version 1.0
 * @since 2024-09
 */
@RestController
@RequestMapping("${api.prefix:/api/v1}/auth/real") // API 엔드포인트 기본 경로: /api/v1/auth/real
@RequiredArgsConstructor // final 필드에 대한 생성자 자동 생성 (의존성 주입용)
@Slf4j // SLF4J 로거 자동 생성 (로깅용)
@Tag(name = "Step01 Token Management", description = "DB-first authentication with Keycloak token management")
public class Step01TokenController {
    
    // 사용자 데이터베이스 조회 및 관리를 위한 서비스
    private final UserService userService;
    
    // Keycloak 서버와의 토큰 발급 통신을 담당하는 서비스
    private final KeycloakTokenService keycloakTokenService;
    
    /**
     * Step01 로그인 엔드포인트 - DB 우선 검증 후 토큰 발급
     * 
     * 로그인 프로세스:
     * 1. 요청된 사용자명으로 우리 DB에서 사용자 검색
     * 2. 사용자가 존재하지 않으면 회원가입 안내 응답
     * 3. 사용자가 비활성화 상태면 접근 거부 응답
     * 4. 모든 조건을 만족하면 Keycloak에 토큰 요청
     * 5. 성공시 토큰 정보와 사용자 정보를 함께 반환
     * 
     * @param loginRequest 사용자명과 비밀번호가 포함된 로그인 요청 DTO
     * @return ResponseEntity 로그인 결과 (토큰 정보, 사용자 정보, 또는 오류 메시지)
     */
    @PostMapping("/login")
    @Operation(
        summary = "Step01 Login with DB-first approach", 
        description = "Check user existence in DB first, then issue Keycloak token if user exists"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "User exists in DB and token issued successfully",
            content = @Content(schema = @Schema(implementation = TokenResponse.class))
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "User not found in DB - signup required",
            content = @Content(schema = @Schema(implementation = Map.class))
        ),
        @ApiResponse(
            responseCode = "401", 
            description = "User exists in DB but Keycloak authentication failed",
            content = @Content(schema = @Schema(implementation = String.class))
        ),
        @ApiResponse(
            responseCode = "500", 
            description = "Internal server error",
            content = @Content(schema = @Schema(implementation = String.class))
        )
    })
    public ResponseEntity<?> step01Login(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("🔑 Step01 로그인 시도 - 사용자: {}", loginRequest.getUsername());
        
        try {
            // 1단계: DB 우선 접근법 - 우리 데이터베이스에서 사용자 존재 확인
            log.debug("📊 데이터베이스에서 사용자 존재 확인 중: {}", loginRequest.getUsername());
            User user = userService.getUserByUsername(loginRequest.getUsername());
            
            // 사용자가 우리 DB에 존재하지 않는 경우
            if (user == null) {
                log.info("❌ 사용자 {} 데이터베이스에 미등록 - 회원가입 필요", loginRequest.getUsername());
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(
                            "error", "USER_NOT_REGISTERED",
                            "message", "사용자가 데이터베이스에 등록되지 않았습니다. 먼저 회원가입을 진행해주세요.",
                            "username", loginRequest.getUsername(),
                            "action_required", "SIGNUP",
                            "signup_endpoint", "/api/v1/auth/real/signup"
                        ));
            }
            
            // 2단계: 사용자가 DB에 존재하면 활성화 상태 확인
            if (!user.isEnabled()) {
                log.warn("⚠️ 사용자 {} 계정이 비활성화 상태입니다", loginRequest.getUsername());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of(
                            "error", "USER_DISABLED",
                            "message", "사용자 계정이 비활성화 상태입니다",
                            "username", loginRequest.getUsername()
                        ));
            }
            
            log.info("✅ 사용자 {} DB 검증 완료 및 활성화 확인. Keycloak 토큰 발급 진행", 
                    loginRequest.getUsername());
            
            // 3단계: 사용자가 존재하고 활성화되어 있으므로 Keycloak 토큰 발급 진행
            TokenResponse tokenResponse = keycloakTokenService.getToken(loginRequest);
            
            log.info("🎉 Step01 로그인 성공 - 사용자: {} (DB user_seq: {})", 
                    loginRequest.getUsername(), user.getId());
            
            // 성공 응답: 토큰 정보와 데이터베이스 사용자 정보를 함께 반환
            return ResponseEntity.ok(Map.of(
                "token_info", tokenResponse, // Keycloak에서 발급받은 JWT 토큰 정보
                "user_info", Map.of( // 우리 DB에 저장된 사용자 정보
                    "user_seq", user.getId(), // 우리 DB의 사용자 고유 번호
                    "username", user.getUsername(), // 사용자명
                    "email", user.getEmail(), // 이메일 주소
                    "role", user.getRole(), // 우리 시스템에서의 역할
                    "keycloak_user_id", user.getKeycloakUserId() != null ? user.getKeycloakUserId() : "null", // Keycloak 사용자 ID (있는 경우)
                    "last_login", user.getUpdatedAt() // 마지막 로그인 시간
                ),
                "login_type", "DB_FIRST_SUCCESS" // 로그인 방식 구분자
            ));
            
        } catch (UserNotFoundException e) {
            // UserService에서 사용자를 찾을 수 없다는 예외가 발생한 경우
            log.info("❌ UserService를 통한 사용자 {} 조회 실패 - 회원가입 필요", loginRequest.getUsername());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                        "error", "USER_NOT_REGISTERED",
                        "message", "사용자가 데이터베이스에 등록되지 않았습니다. 먼저 회원가입을 진행해주세요.",
                        "username", loginRequest.getUsername(),
                        "action_required", "SIGNUP",
                        "signup_endpoint", "/api/v1/auth/real/signup"
                    ));
            
        } catch (RuntimeException e) {
            // Keycloak 인증 실패 등의 런타임 예외 처리
            log.warn("⚠️ 사용자 {}는 DB에 존재하지만 Keycloak 인증 실패: {}", 
                    loginRequest.getUsername(), e.getMessage());
            
            // 인증 실패 유형별 세분화된 오류 응답
            if (e.getMessage().contains("Authentication failed")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of(
                            "error", "KEYCLOAK_AUTH_FAILED",
                            "message", "사용자는 데이터베이스에 존재하지만 Keycloak 인증에 실패했습니다. 자격증명을 확인해주세요.",
                            "username", loginRequest.getUsername(),
                            "details", e.getMessage()
                        ));
            } else if (e.getMessage().contains("service unavailable")) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(Map.of(
                            "error", "KEYCLOAK_UNAVAILABLE",
                            "message", "Keycloak 인증 서비스가 현재 이용할 수 없습니다",
                            "username", loginRequest.getUsername()
                        ));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of(
                            "error", "TOKEN_REQUEST_FAILED",
                            "message", "토큰 요청 실패: " + e.getMessage(),
                            "username", loginRequest.getUsername()
                        ));
            }
        } catch (Exception e) {
            // 예상하지 못한 일반적인 예외 처리
            log.error("💥 사용자 {} Step01 로그인 중 예상치 못한 오류 발생", loginRequest.getUsername(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                        "error", "INTERNAL_ERROR",
                        "message", "로그인 중 예상치 못한 오류가 발생했습니다",
                        "username", loginRequest.getUsername()
                    ));
        }
    }
    
    /**
     * 사용자 존재 여부 확인 엔드포인트
     * 
     * 로그인 전에 사용자가 우리 데이터베이스에 등록되어 있는지 확인하는 용도.
     * 프론트엔드에서 로그인 버튼 활성화/비활성화 또는 회원가입 안내에 활용 가능.
     * 
     * @param username 확인할 사용자명
     * @return ResponseEntity 사용자 존재 여부 및 관련 정보
     */
    @GetMapping("/user-check/{username}")
    @Operation(
        summary = "Check user existence in database", 
        description = "Check if user exists in our database before attempting login"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "User existence check result",
            content = @Content(schema = @Schema(implementation = Map.class))
        )
    })
    public ResponseEntity<Map<String, Object>> checkUserExistence(@PathVariable String username) {
        log.info("🔍 사용자 존재 여부 확인 중: {}", username);
        
        try {
            User user = userService.getUserByUsername(username);
            
            if (user != null) {
                // 사용자가 존재하는 경우
                log.debug("✅ 사용자 {} 데이터베이스에 존재 (user_seq: {})", username, user.getId());
                return ResponseEntity.ok(Map.of(
                    "exists", true, // 사용자 존재 여부
                    "username", username,
                    "user_seq", user.getId(), // 우리 DB의 사용자 고유 번호
                    "email", user.getEmail(), // 등록된 이메일
                    "enabled", user.isEnabled(), // 계정 활성화 상태
                    "role", user.getRole(), // 사용자 역할
                    "has_keycloak_id", user.getKeycloakUserId() != null, // Keycloak ID 연동 여부
                    "action", "LOGIN_AVAILABLE" // 권장 동작: 로그인 가능
                ));
            } else {
                // 사용자가 존재하지 않는 경우
                log.debug("❌ 사용자 {} 데이터베이스에 미존재", username);
                return ResponseEntity.ok(Map.of(
                    "exists", false, // 사용자 존재 여부
                    "username", username,
                    "action", "SIGNUP_REQUIRED", // 권장 동작: 회원가입 필요
                    "signup_endpoint", "/api/v1/auth/real/signup" // 회원가입 엔드포인트
                ));
            }
            
        } catch (UserNotFoundException e) {
            // UserService에서 사용자를 찾을 수 없다는 예외가 발생한 경우
            log.debug("❌ UserService를 통한 사용자 {} 조회 결과 없음", username);
            return ResponseEntity.ok(Map.of(
                "exists", false,
                "username", username,
                "action", "SIGNUP_REQUIRED",
                "signup_endpoint", "/api/v1/auth/real/signup"
            ));
        } catch (Exception e) {
            // 사용자 존재 확인 중 오류 발생
            log.error("💥 사용자 {} 존재 여부 확인 중 오류 발생", username, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                        "error", "CHECK_FAILED",
                        "message", "사용자 존재 여부 확인에 실패했습니다",
                        "username", username
                    ));
        }
    }
    
    /**
     * Step01 토큰 컨트롤러 헬스 체크 엔드포인트
     * 
     * 컨트롤러와 관련 서비스들이 정상적으로 작동하는지 확인하는 용도.
     * 모니터링 시스템이나 로드밸런서에서 서비스 상태 확인에 활용.
     * 
     * @return ResponseEntity 서비스 상태 정보
     */
    @GetMapping("/health")
    @Operation(
        summary = "Step01 Token Controller health check", 
        description = "Check if Step01 token controller and related services are available"
    )
    @ApiResponse(
        responseCode = "200", 
        description = "Service is healthy",
        content = @Content(schema = @Schema(implementation = Map.class))
    )
    public ResponseEntity<Map<String, Object>> healthCheck() {
        log.debug("🏥 Step01 토큰 컨트롤러 헬스 체크");
        return ResponseEntity.ok(Map.of(
            "service", "Step01TokenController", // 서비스 이름
            "status", "healthy", // 서비스 상태
            "approach", "DB_FIRST", // 사용하는 인증 방식
            "description", "데이터베이스 우선 인증 방식으로 Keycloak 토큰 관리", // 서비스 설명
            "timestamp", System.currentTimeMillis(), // 체크 시간
            "endpoints", Map.of( // 제공하는 엔드포인트 목록
                "login", "/api/v1/auth/real/login", // 로그인 엔드포인트
                "user_check", "/api/v1/auth/real/user-check/{username}", // 사용자 확인 엔드포인트
                "health", "/api/v1/auth/real/health" // 헬스 체크 엔드포인트
            )
        ));
    }
}
