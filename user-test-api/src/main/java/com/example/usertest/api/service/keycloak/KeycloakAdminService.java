package com.example.usertest.api.service.keycloak;

import com.example.usertest.api.config.properties.KeycloakProperties;
import com.example.usertest.domain.dto.keycloak.KeycloakUserCreateRequest;
import com.example.usertest.domain.dto.keycloak.KeycloakUserCreateResponse;
import com.example.usertest.domain.dto.keycloak.RoleCreateRequest;
import com.example.usertest.domain.dto.keycloak.RoleResponse;
import com.example.usertest.domain.dto.keycloak.RoleAssignRequest;
import com.example.usertest.domain.dto.keycloak.UserRoleResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Keycloak Admin API 통신 서비스
 * 사용자 생성, 역할 할당, 그룹 관리 등의 관리 기능 제공
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakAdminService {

    private final WebClient webClient;
    private final KeycloakProperties keycloakProperties;

    /**
     * Keycloak Admin API 인증 토큰 획득
     * admin 계정을 사용하여 관리 권한 토큰을 발급받음
     * 
     * @return 관리자 액세스 토큰
     */
    public Mono<String> getAdminToken() {
        log.info("🔐 Keycloak Admin 토큰 요청 시작");

        String tokenUrl = String.format("%s/realms/master/protocol/openid-connect/token",
                keycloakProperties.getServerUrl());

        Map<String, String> tokenRequest = new HashMap<>();
        tokenRequest.put("grant_type", "password");
        tokenRequest.put("client_id", "admin-cli");
        tokenRequest.put("username", keycloakProperties.getAdmin().getUsername());
        tokenRequest.put("password", keycloakProperties.getAdmin().getPassword());

        return webClient.post()
                .uri(tokenUrl)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .bodyValue(buildFormData(tokenRequest))
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> (String) response.get("access_token"))
                .doOnSuccess(token -> log.info("✅ Admin 토큰 발급 성공"))
                .doOnError(error -> log.error("❌ Admin 토큰 발급 실패: {}", error.getMessage()));
    }

    /**
     * Keycloak에 새로운 사용자 생성
     * 
     * @param createRequest 사용자 생성 요청 정보
     * @return 사용자 생성 결과
     */
    public Mono<KeycloakUserCreateResponse> createUser(KeycloakUserCreateRequest createRequest) {
        log.info("👤 Keycloak 사용자 생성 시작: {}", createRequest.getUsername());

        return getAdminToken()
                .flatMap(adminToken -> {
                    String usersUrl = String.format("%s/admin/realms/%s/users",
                            keycloakProperties.getServerUrl(),
                            keycloakProperties.getRealm());

                    Map<String, Object> userRepresentation = buildUserRepresentation(createRequest);

                    return webClient.post()
                            .uri(usersUrl)
                            .header("Authorization", "Bearer " + adminToken)
                            .header("Content-Type", "application/json")
                            .bodyValue(userRepresentation)
                            .retrieve()
                            .toBodilessEntity()
                            .then(getUserByUsername(createRequest.getUsername(), adminToken))
                            .flatMap(userId -> {
                                if (userId != null) {
                                    return assignRolesAndGroups(userId, createRequest, adminToken)
                                            .then(Mono.just(KeycloakUserCreateResponse.success(
                                                    userId,
                                                    createRequest.getUsername(),
                                                    createRequest.getEmail()
                                            )));
                                } else {
                                    return Mono.just(KeycloakUserCreateResponse.failure("사용자 ID 조회 실패"));
                                }
                            });
                })
                .doOnSuccess(response -> {
                    if (response.isSuccess()) {
                        log.info("✅ 사용자 생성 성공: {} (ID: {})", 
                                response.getUsername(), response.getKeycloakUserId());
                    } else {
                        log.error("❌ 사용자 생성 실패: {}", response.getErrorMessage());
                    }
                })
                .onErrorResume(WebClientResponseException.class, ex -> {
                    String errorMsg = String.format("Keycloak API 오류 [%d]: %s", 
                            ex.getStatusCode().value(), ex.getResponseBodyAsString());
                    log.error("❌ 사용자 생성 API 오류: {}", errorMsg);
                    return Mono.just(KeycloakUserCreateResponse.failure(errorMsg));
                })
                .onErrorResume(Exception.class, ex -> {
                    log.error("❌ 사용자 생성 중 예상치 못한 오류: {}", ex.getMessage(), ex);
                    return Mono.just(KeycloakUserCreateResponse.failure("예상치 못한 오류: " + ex.getMessage()));
                });
    }

    /**
     * pulsar_system 사용자 생성 (특별 권한 포함)
     * delete-account, manage-account 권한을 자동으로 할당
     * 
     * @param username 사용자명
     * @param email 이메일
     * @param password 비밀번호
     * @return pulsar_system 사용자 생성 결과
     */
    public Mono<KeycloakUserCreateResponse> createPulsarSystemUser(String username, String email, String password) {
        log.info("🚀 Pulsar 시스템 사용자 생성 시작: {}", username);
        
        KeycloakUserCreateRequest request = KeycloakUserCreateRequest.forPulsarSystem(username, email, password);
        
        return createUser(request)
                .map(response -> {
                    if (response.isSuccess()) {
                        return KeycloakUserCreateResponse.pulsarSystemSuccess(
                                response.getKeycloakUserId(),
                                response.getUsername(),
                                response.getEmail()
                        );
                    }
                    return response;
                });
    }

    /**
     * 사용자명으로 Keycloak 사용자 ID 조회
     * 
     * @param username 사용자명
     * @param adminToken 관리자 토큰
     * @return Keycloak 사용자 ID
     */
    private Mono<String> getUserByUsername(String username, String adminToken) {
        String searchUrl = String.format("%s/admin/realms/%s/users?username=%s",
                keycloakProperties.getServerUrl(),
                keycloakProperties.getRealm(),
                username);

        return webClient.get()
                .uri(searchUrl)
                .header("Authorization", "Bearer " + adminToken)
                .retrieve()
                .bodyToMono(List.class)
                .map(users -> {
                    if (!users.isEmpty()) {
                        Map<String, Object> user = (Map<String, Object>) users.get(0);
                        return (String) user.get("id");
                    }
                    return null;
                });
    }

    /**
     * 사용자에게 역할과 그룹 할당
     * 
     * @param userId Keycloak 사용자 ID
     * @param createRequest 생성 요청 정보
     * @param adminToken 관리자 토큰
     * @return 할당 완료 신호
     */
    private Mono<Void> assignRolesAndGroups(String userId, KeycloakUserCreateRequest createRequest, String adminToken) {
        // 역할 할당과 그룹 할당을 병렬로 처리
        Mono<Void> roleAssignment = Mono.empty();
        Mono<Void> groupAssignment = Mono.empty();

        if (createRequest.getRoles() != null && !createRequest.getRoles().isEmpty()) {
            roleAssignment = assignRolesToUser(userId, createRequest.getRoles(), adminToken);
        }

        if (createRequest.getGroups() != null && !createRequest.getGroups().isEmpty()) {
            groupAssignment = assignGroupsToUser(userId, createRequest.getGroups(), adminToken);
        }

        return Mono.when(roleAssignment, groupAssignment);
    }

    /**
     * 사용자에게 역할 할당
     * 
     * @param userId Keycloak 사용자 ID
     * @param roles 할당할 역할 목록
     * @param adminToken 관리자 토큰
     * @return 역할 할당 완료 신호
     */
    private Mono<Void> assignRolesToUser(String userId, List<String> roles, String adminToken) {
        log.info("🎭 사용자 {}에게 역할 할당: {}", userId, roles);
        
        // Keycloak에서 사용 가능한 클라이언트 역할 조회 후 할당
        String clientRolesUrl = String.format("%s/admin/realms/%s/clients",
                keycloakProperties.getServerUrl(),
                keycloakProperties.getRealm());

        return webClient.get()
                .uri(clientRolesUrl)
                .header("Authorization", "Bearer " + adminToken)
                .retrieve()
                .bodyToMono(List.class)
                .flatMap(clients -> {
                    // account 클라이언트 찾기 (manage-account, delete-account 역할을 가진 클라이언트)
                    String accountClientId = null;
                    for (Object clientObj : clients) {
                        Map<String, Object> client = (Map<String, Object>) clientObj;
                        if ("account".equals(client.get("clientId"))) {
                            accountClientId = (String) client.get("id");
                            break;
                        }
                    }
                    
                    if (accountClientId == null) {
                        log.warn("⚠️ account 클라이언트를 찾을 수 없습니다.");
                        return Mono.<Void>empty();
                    }
                    
                    return assignClientRolesToUser(userId, accountClientId, roles, adminToken);
                })
                .onErrorResume(ex -> {
                    log.error("❌ 역할 할당 중 오류: {}", ex.getMessage());
                    return Mono.<Void>empty();
                });
    }

    /**
     * 클라이언트 역할을 사용자에게 할당
     * 
     * @param userId Keycloak 사용자 ID
     * @param clientId 클라이언트 ID
     * @param roles 할당할 역할 목록
     * @param adminToken 관리자 토큰
     * @return 역할 할당 완료 신호
     */
    private Mono<Void> assignClientRolesToUser(String userId, String clientId, List<String> roles, String adminToken) {
        // 클라이언트 역할 조회
        String clientRolesUrl = String.format("%s/admin/realms/%s/clients/%s/roles",
                keycloakProperties.getServerUrl(),
                keycloakProperties.getRealm(),
                clientId);

        return webClient.get()
                .uri(clientRolesUrl)
                .header("Authorization", "Bearer " + adminToken)
                .retrieve()
                .bodyToMono(List.class)
                .flatMap(availableRoles -> {
                    // 요청된 역할 중 실제 존재하는 역할만 필터링
                    List<Map<String, Object>> rolesToAssign = roles.stream()
                            .map(roleName -> {
                                for (Object roleObj : availableRoles) {
                                    Map<String, Object> role = (Map<String, Object>) roleObj;
                                    if (roleName.equals(role.get("name"))) {
                                        return role;
                                    }
                                }
                                return null;
                            })
                            .filter(role -> role != null)
                            .toList();

                    if (rolesToAssign.isEmpty()) {
                        log.warn("⚠️ 할당할 수 있는 역할이 없습니다: {}", roles);
                        return Mono.<Void>empty();
                    }

                    // 사용자에게 클라이언트 역할 할당
                    String assignRoleUrl = String.format("%s/admin/realms/%s/users/%s/role-mappings/clients/%s",
                            keycloakProperties.getServerUrl(),
                            keycloakProperties.getRealm(),
                            userId,
                            clientId);

                    return webClient.post()
                            .uri(assignRoleUrl)
                            .header("Authorization", "Bearer " + adminToken)
                            .header("Content-Type", "application/json")
                            .bodyValue(rolesToAssign)
                            .retrieve()
                            .toBodilessEntity()
                            .then()
                            .doOnSuccess(v -> log.info("✅ 클라이언트 역할 할당 성공: {}", rolesToAssign.stream()
                                    .map(role -> role.get("name")).toList()));
                });
    }

    /**
     * 사용자를 그룹에 추가
     * 
     * @param userId Keycloak 사용자 ID
     * @param groups 추가할 그룹 목록
     * @param adminToken 관리자 토큰
     * @return 그룹 할당 완료 신호
     */
    private Mono<Void> assignGroupsToUser(String userId, List<String> groups, String adminToken) {
        log.info("👥 사용자 {}를 그룹에 추가: {}", userId, groups);
        
        // 그룹 목록 조회
        String groupsUrl = String.format("%s/admin/realms/%s/groups",
                keycloakProperties.getServerUrl(),
                keycloakProperties.getRealm());

        return webClient.get()
                .uri(groupsUrl)
                .header("Authorization", "Bearer " + adminToken)
                .retrieve()
                .bodyToMono(List.class)
                .flatMap(availableGroups -> {
                    // 요청된 그룹들 처리
                    return Flux.fromIterable(groups)
                            .flatMap(groupName -> joinUserToGroup(userId, groupName, availableGroups, adminToken))
                            .then();
                })
                .onErrorResume(ex -> {
                    log.error("❌ 그룹 할당 중 오류: {}", ex instanceof Throwable ? ((Throwable) ex).getMessage() : ex.toString());
                    return Mono.<Void>empty();
                });
    }

    /**
     * 사용자를 특정 그룹에 추가
     * 
     * @param userId Keycloak 사용자 ID
     * @param groupName 그룹명
     * @param availableGroups 사용 가능한 그룹 목록
     * @param adminToken 관리자 토큰
     * @return 그룹 가입 완료 신호
     */
    private Mono<Void> joinUserToGroup(String userId, String groupName, List<Object> availableGroups, String adminToken) {
        // 그룹 찾기 또는 생성
        String groupId = null;
        for (Object groupObj : availableGroups) {
            Map<String, Object> group = (Map<String, Object>) groupObj;
            if (groupName.equals(group.get("name"))) {
                groupId = (String) group.get("id");
                break;
            }
        }

        if (groupId == null) {
            // 그룹이 없으면 생성
            return createGroup(groupName, adminToken)
                    .flatMap(newGroupId -> addUserToGroup(userId, newGroupId, adminToken));
        } else {
            // 기존 그룹에 사용자 추가
            return addUserToGroup(userId, groupId, adminToken);
        }
    }

    /**
     * 새 그룹 생성
     * 
     * @param groupName 그룹명
     * @param adminToken 관리자 토큰
     * @return 생성된 그룹 ID
     */
    private Mono<String> createGroup(String groupName, String adminToken) {
        log.info("🆕 새 그룹 생성: {}", groupName);
        
        String createGroupUrl = String.format("%s/admin/realms/%s/groups",
                keycloakProperties.getServerUrl(),
                keycloakProperties.getRealm());

        Map<String, Object> groupData = new HashMap<>();
        groupData.put("name", groupName);

        return webClient.post()
                .uri(createGroupUrl)
                .header("Authorization", "Bearer " + adminToken)
                .header("Content-Type", "application/json")
                .bodyValue(groupData)
                .retrieve()
                .toBodilessEntity()
                .then(getGroupByName(groupName, adminToken))
                .doOnSuccess(groupId -> log.info("✅ 그룹 생성 완료: {} (ID: {})", groupName, groupId));
    }

    /**
     * 그룹명으로 그룹 ID 조회
     * 
     * @param groupName 그룹명
     * @param adminToken 관리자 토큰
     * @return 그룹 ID
     */
    private Mono<String> getGroupByName(String groupName, String adminToken) {
        String groupsUrl = String.format("%s/admin/realms/%s/groups?search=%s",
                keycloakProperties.getServerUrl(),
                keycloakProperties.getRealm(),
                groupName);

        return webClient.get()
                .uri(groupsUrl)
                .header("Authorization", "Bearer " + adminToken)
                .retrieve()
                .bodyToMono(List.class)
                .map(groups -> {
                    if (!groups.isEmpty()) {
                        Map<String, Object> group = (Map<String, Object>) groups.get(0);
                        return (String) group.get("id");
                    }
                    return null;
                });
    }

    /**
     * 사용자를 그룹에 추가
     * 
     * @param userId Keycloak 사용자 ID
     * @param groupId 그룹 ID
     * @param adminToken 관리자 토큰
     * @return 그룹 가입 완료 신호
     */
    private Mono<Void> addUserToGroup(String userId, String groupId, String adminToken) {
        String joinGroupUrl = String.format("%s/admin/realms/%s/users/%s/groups/%s",
                keycloakProperties.getServerUrl(),
                keycloakProperties.getRealm(),
                userId,
                groupId);

        return webClient.put()
                .uri(joinGroupUrl)
                .header("Authorization", "Bearer " + adminToken)
                .retrieve()
                .toBodilessEntity()
                .then()
                .doOnSuccess(v -> log.info("✅ 사용자 그룹 가입 완료: userId={}, groupId={}", userId, groupId));
    }

    /**
     * Keycloak 사용자 표현 객체 생성
     * 
     * @param createRequest 사용자 생성 요청
     * @return Keycloak UserRepresentation Map
     */
    private Map<String, Object> buildUserRepresentation(KeycloakUserCreateRequest createRequest) {
        Map<String, Object> user = new HashMap<>();
        user.put("username", createRequest.getUsername());
        user.put("email", createRequest.getEmail());
        user.put("firstName", createRequest.getFirstName());
        user.put("lastName", createRequest.getLastName());
        user.put("enabled", createRequest.getEnabled());
        user.put("emailVerified", createRequest.getEmailVerified());

        // 초기 비밀번호 설정
        Map<String, Object> credential = new HashMap<>();
        credential.put("type", "password");
        credential.put("value", createRequest.getPassword());
        credential.put("temporary", false); // 임시 비밀번호가 아님
        user.put("credentials", List.of(credential));

        return user;
    }

    /**
     * Form 데이터 문자열 생성
     * 
     * @param data Form 데이터 Map
     * @return URL-encoded Form 데이터 문자열
     */
    private String buildFormData(Map<String, String> data) {
        return data.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .reduce("", (a, b) -> a.isEmpty() ? b : a + "&" + b);
    }

    // ===== 역할 관리 관련 메서드들 =====

    /**
     * Keycloak에 새로운 역할 생성
     * 
     * @param roleRequest 역할 생성 요청 정보
     * @return 역할 생성 결과
     */
    public Mono<RoleResponse> createRole(RoleCreateRequest roleRequest) {
        log.info("🎭 Keycloak 역할 생성 시작: {}", roleRequest.getRoleName());

        return getAdminToken()
                .flatMap(adminToken -> {
                    String rolesUrl = String.format("%s/admin/realms/%s/roles",
                            keycloakProperties.getServerUrl(),
                            keycloakProperties.getRealm());

                    Map<String, Object> roleRepresentation = buildRoleRepresentation(roleRequest);

                    return webClient.post()
                            .uri(rolesUrl)
                            .header("Authorization", "Bearer " + adminToken)
                            .header("Content-Type", "application/json")
                            .bodyValue(roleRepresentation)
                            .retrieve()
                            .toBodilessEntity()
                            .then(getRoleByName(roleRequest.getRoleName(), adminToken))
                            .map(roleId -> RoleResponse.created(roleRequest.getRoleName(), roleId));
                })
                .doOnSuccess(response -> {
                    if (response.isSuccess()) {
                        log.info("✅ 역할 생성 성공: {} (ID: {})", 
                                response.getRoleName(), response.getRoleId());
                    }
                })
                .onErrorResume(WebClientResponseException.class, ex -> {
                    if (ex.getStatusCode().value() == 409) {
                        log.warn("⚠️ 역할이 이미 존재함: {}", roleRequest.getRoleName());
                        return Mono.just(RoleResponse.failure("역할이 이미 존재합니다: " + roleRequest.getRoleName()));
                    }
                    String errorMsg = String.format("Keycloak API 오류 [%d]: %s", 
                            ex.getStatusCode().value(), ex.getResponseBodyAsString());
                    log.error("❌ 역할 생성 API 오류: {}", errorMsg);
                    return Mono.just(RoleResponse.failure(errorMsg));
                })
                .onErrorResume(Exception.class, ex -> {
                    log.error("❌ 역할 생성 중 예상치 못한 오류: {}", ex.getMessage(), ex);
                    return Mono.just(RoleResponse.failure("예상치 못한 오류: " + ex.getMessage()));
                });
    }

    /**
     * 기본 역할들(admin, user, manager) 자동 생성
     * 
     * @return 기본 역할 생성 완료 신호
     */
    public Mono<Void> createDefaultRoles() {
        log.info("🎯 기본 역할들 생성 시작 (admin, user, manager)");
        
        return Mono.when(
                createRole(RoleCreateRequest.admin()).onErrorResume(ex -> Mono.empty()),
                createRole(RoleCreateRequest.user()).onErrorResume(ex -> Mono.empty()),
                createRole(RoleCreateRequest.manager()).onErrorResume(ex -> Mono.empty())
        ).then()
        .doOnSuccess(v -> log.info("✅ 기본 역할들 생성 완료"))
        .doOnError(ex -> log.error("❌ 기본 역할 생성 중 오류: {}", ex.getMessage()));
    }

    /**
     * 사용자에게 역할 할당
     * 
     * @param assignRequest 역할 할당 요청 정보
     * @return 역할 할당 결과
     */
    public Mono<RoleResponse> assignRolesToUser(RoleAssignRequest assignRequest) {
        log.info("👤 사용자 {}에게 역할 할당: {}", assignRequest.getUsername(), assignRequest.getRoles());

        return getAdminToken()
                .flatMap(adminToken -> getUserByUsername(assignRequest.getUsername(), adminToken)
                        .flatMap(userId -> {
                            if (userId == null) {
                                return Mono.just(RoleResponse.failure("사용자를 찾을 수 없습니다: " + assignRequest.getUsername()));
                            }
                            
                            // 기존 역할 제거 (요청된 경우)
                            Mono<Void> removeExisting = Mono.empty();
                            if (assignRequest.isRemoveExistingRoles()) {
                                removeExisting = removeUserRealmRoles(userId, adminToken);
                            }
                            
                            return removeExisting.then(assignRealmRolesToUser(userId, assignRequest.getRoles(), adminToken))
                                    .then(Mono.just(RoleResponse.assigned(assignRequest.getRoles().toString())));
                        }))
                .doOnSuccess(response -> {
                    if (response.isSuccess()) {
                        log.info("✅ 역할 할당 성공: {} → {}", 
                                assignRequest.getUsername(), assignRequest.getRoles());
                    }
                })
                .onErrorResume(Exception.class, ex -> {
                    log.error("❌ 역할 할당 중 오류: {}", ex.getMessage(), ex);
                    return Mono.just(RoleResponse.failure("역할 할당 중 오류 발생: " + ex.getMessage()));
                });
    }

    /**
     * 사용자의 현재 역할 조회
     * 
     * @param username 사용자명
     * @return 사용자 역할 정보
     */
    public Mono<UserRoleResponse> getUserRoles(String username) {
        log.info("🔍 사용자 역할 조회 시작: {}", username);

        return getAdminToken()
                .flatMap(adminToken -> getUserByUsername(username, adminToken)
                        .flatMap(userId -> {
                            if (userId == null) {
                                return Mono.just(UserRoleResponse.failure(username, "사용자를 찾을 수 없습니다"));
                            }
                            
                            return Mono.zip(
                                    getUserRealmRoles(userId, adminToken),
                                    getUserClientRoles(userId, adminToken),
                                    getUserGroups(userId, adminToken)
                            ).map(tuple -> {
                                List<String> realmRoles = tuple.getT1();
                                List<String> clientRoles = tuple.getT2();
                                List<String> groups = tuple.getT3();
                                
                                UserRoleResponse response = UserRoleResponse.success(username, userId);
                                response.setRealmRoles(realmRoles);
                                response.setClientRoles(clientRoles);
                                response.setGroups(groups);
                                
                                // 모든 역할 합치기
                                List<String> allRoles = new java.util.ArrayList<>(realmRoles);
                                allRoles.addAll(clientRoles);
                                response.setAllRoles(allRoles);
                                
                                return response;
                            });
                        }))
                .doOnSuccess(response -> {
                    if (response.isSuccess()) {
                        log.info("✅ 사용자 역할 조회 성공: {} - Realm: {}, Client: {}", 
                                username, response.getRealmRoles(), response.getClientRoles());
                    }
                })
                .onErrorResume(Exception.class, ex -> {
                    log.error("❌ 사용자 역할 조회 중 오류: {}", ex.getMessage(), ex);
                    return Mono.just(UserRoleResponse.failure(username, "역할 조회 중 오류 발생: " + ex.getMessage()));
                });
    }

    /**
     * 역할명으로 역할 ID 조회
     * 
     * @param roleName 역할명
     * @param adminToken 관리자 토큰
     * @return 역할 ID
     */
    private Mono<String> getRoleByName(String roleName, String adminToken) {
        String roleUrl = String.format("%s/admin/realms/%s/roles/%s",
                keycloakProperties.getServerUrl(),
                keycloakProperties.getRealm(),
                roleName);

        return webClient.get()
                .uri(roleUrl)
                .header("Authorization", "Bearer " + adminToken)
                .retrieve()
                .bodyToMono(Map.class)
                .map(role -> (String) role.get("id"));
    }

    /**
     * 사용자에게 Realm 역할 할당
     * 
     * @param userId Keycloak 사용자 ID
     * @param roleNames 할당할 역할명 목록
     * @param adminToken 관리자 토큰
     * @return 할당 완료 신호
     */
    private Mono<Void> assignRealmRolesToUser(String userId, List<String> roleNames, String adminToken) {
        // 먼저 역할들이 존재하는지 확인하고, 역할 객체들을 가져옴
        return Flux.fromIterable(roleNames)
                .flatMap(roleName -> getRealmRoleObject(roleName, adminToken))
                .collectList()
                .flatMap(roleObjects -> {
            List<Map<String, Object>> roles = new java.util.ArrayList<>();
            for (Object roleObj : roleObjects.toArray()) {
                if (roleObj != null) {
                    roles.add((Map<String, Object>) roleObj);
                }
            }
            
            if (roles.isEmpty()) {
                log.warn("⚠️ 할당할 수 있는 역할이 없습니다: {}", roleNames);
                return Mono.<Void>empty();
            }
            
            String assignUrl = String.format("%s/admin/realms/%s/users/%s/role-mappings/realm",
                    keycloakProperties.getServerUrl(),
                    keycloakProperties.getRealm(),
                    userId);

            return webClient.post()
                    .uri(assignUrl)
                    .header("Authorization", "Bearer " + adminToken)
                    .header("Content-Type", "application/json")
                    .bodyValue(roles)
                    .retrieve()
                    .toBodilessEntity()
                    .then()
                    .doOnSuccess(v -> log.info("✅ Realm 역할 할당 성공: {}", roleNames));
        });
    }

    /**
     * Realm 역할 객체 조회
     * 
     * @param roleName 역할명
     * @param adminToken 관리자 토큰
     * @return 역할 객체
     */
    private Mono<Map<String, Object>> getRealmRoleObject(String roleName, String adminToken) {
        String roleUrl = String.format("%s/admin/realms/%s/roles/%s",
                keycloakProperties.getServerUrl(),
                keycloakProperties.getRealm(),
                roleName);

        return webClient.get()
                .uri(roleUrl)
                .header("Authorization", "Bearer " + adminToken)
                .retrieve()
                .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {})
                .onErrorResume(ex -> {
                    log.warn("⚠️ 역할을 찾을 수 없음: {}", roleName);
                    return Mono.<Map<String, Object>>empty();
                });
    }

    /**
     * 사용자의 현재 Realm 역할 조회
     * 
     * @param userId Keycloak 사용자 ID
     * @param adminToken 관리자 토큰
     * @return 사용자의 Realm 역할 목록
     */
    private Mono<List<String>> getUserRealmRoles(String userId, String adminToken) {
        String rolesUrl = String.format("%s/admin/realms/%s/users/%s/role-mappings/realm",
                keycloakProperties.getServerUrl(),
                keycloakProperties.getRealm(),
                userId);

        return webClient.get()
                .uri(rolesUrl)
                .header("Authorization", "Bearer " + adminToken)
                .retrieve()
                .bodyToMono(new org.springframework.core.ParameterizedTypeReference<List<Object>>() {})
                .map(roles -> roles.stream()
                        .map(role -> ((Map<String, Object>) role).get("name").toString())
                        .toList())
                .onErrorReturn(List.<String>of());
    }

    /**
     * 사용자의 현재 Client 역할 조회
     * 
     * @param userId Keycloak 사용자 ID
     * @param adminToken 관리자 토큰
     * @return 사용자의 Client 역할 목록
     */
    private Mono<List<String>> getUserClientRoles(String userId, String adminToken) {
        // 여기서는 account 클라이언트의 역할만 조회
        String clientRolesUrl = String.format("%s/admin/realms/%s/users/%s/role-mappings/clients",
                keycloakProperties.getServerUrl(),
                keycloakProperties.getRealm(),
                userId);

        return webClient.get()
                .uri(clientRolesUrl)
                .header("Authorization", "Bearer " + adminToken)
                .retrieve()
                .bodyToMono(Map.class)
                .map(clientRoles -> {
                    List<String> allClientRoles = new java.util.ArrayList<>();
                    for (Object entry : clientRoles.entrySet()) {
                        Map.Entry<String, Object> clientEntry = (Map.Entry<String, Object>) entry;
                        List<Map<String, Object>> roles = (List<Map<String, Object>>) clientEntry.getValue();
                        for (Map<String, Object> role : roles) {
                            allClientRoles.add((String) role.get("name"));
                        }
                    }
                    return allClientRoles;
                })
                .onErrorReturn(List.<String>of());
    }

    /**
     * 사용자의 그룹 목록 조회
     * 
     * @param userId Keycloak 사용자 ID
     * @param adminToken 관리자 토큰
     * @return 사용자의 그룹 목록
     */
    private Mono<List<String>> getUserGroups(String userId, String adminToken) {
        String groupsUrl = String.format("%s/admin/realms/%s/users/%s/groups",
                keycloakProperties.getServerUrl(),
                keycloakProperties.getRealm(),
                userId);

        return webClient.get()
                .uri(groupsUrl)
                .header("Authorization", "Bearer " + adminToken)
                .retrieve()
                .bodyToMono(new org.springframework.core.ParameterizedTypeReference<List<Object>>() {})
                .map(groups -> groups.stream()
                        .map(group -> ((Map<String, Object>) group).get("name").toString())
                        .toList())
                .onErrorReturn(List.<String>of());
    }

    /**
     * 사용자의 기존 Realm 역할 제거
     * 
     * @param userId Keycloak 사용자 ID
     * @param adminToken 관리자 토큰
     * @return 제거 완료 신호
     */
    private Mono<Void> removeUserRealmRoles(String userId, String adminToken) {
        return getUserRealmRoles(userId, adminToken)
                .flatMap(roleNames -> {
                    if (roleNames.isEmpty()) {
                        return Mono.<Void>empty();
                    }
                    
                    return Flux.fromIterable(roleNames)
                            .flatMap(roleName -> getRealmRoleObject(roleName, adminToken))
                            .collectList()
                            .flatMap(roleObjects -> {
                        List<Map<String, Object>> roles = new java.util.ArrayList<>();
                        for (Object roleObj : roleObjects.toArray()) {
                            if (roleObj != null) {
                                roles.add((Map<String, Object>) roleObj);
                            }
                        }
                        
                        if (roles.isEmpty()) {
                            return Mono.<Void>empty();
                        }
                        
                        String removeUrl = String.format("%s/admin/realms/%s/users/%s/role-mappings/realm",
                                keycloakProperties.getServerUrl(),
                                keycloakProperties.getRealm(),
                                userId);

                        return webClient.method(org.springframework.http.HttpMethod.DELETE)
                                .uri(removeUrl)
                                .header("Authorization", "Bearer " + adminToken)
                                .header("Content-Type", "application/json")
                                .bodyValue(roles)
                                .retrieve()
                                .toBodilessEntity()
                                .then();
                    });
                })
                .doOnSuccess(v -> log.info("✅ 기존 Realm 역할 제거 완료"))
                .onErrorResume(ex -> {
                    log.warn("⚠️ 기존 역할 제거 중 오류 (무시): {}", ex.getMessage());
                    return Mono.<Void>empty();
                });
    }

    /**
     * Keycloak 역할 표현 객체 생성
     * 
     * @param roleRequest 역할 생성 요청
     * @return Keycloak RoleRepresentation Map
     */
    private Map<String, Object> buildRoleRepresentation(RoleCreateRequest roleRequest) {
        Map<String, Object> role = new HashMap<>();
        role.put("name", roleRequest.getRoleName());
        role.put("description", roleRequest.getDescription());
        return role;
    }
}