package com.example.usertest.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("${api.prefix:/api/v1}")
@Slf4j
@Tag(name = "Test API", description = "Test endpoints for role-based access control")
@SecurityRequirement(name = "bearerAuth")
public class TestController {

    @GetMapping("/public/hello")
    @Operation(summary = "Public endpoint", description = "Accessible without authentication")
    public ResponseEntity<Map<String, Object>> publicEndpoint() {
        log.info("Public endpoint accessed");
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Hello from public endpoint!");
        response.put("status", "success");
        response.put("timestamp", System.currentTimeMillis());
        response.put("endpoint", "/public/hello");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/profile")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "User profile", description = "Accessible with USER role")
    public ResponseEntity<Map<String, Object>> userEndpoint(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        log.info("User endpoint accessed by: {}", jwt.getClaimAsString("preferred_username"));
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Hello from user endpoint!");
        response.put("user", jwt.getClaimAsString("preferred_username"));
        response.put("subject", jwt.getSubject());
        response.put("roles", authentication.getAuthorities());
        response.put("status", "success");
        response.put("endpoint", "/user/profile");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/admin/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin dashboard", description = "Accessible with ADMIN role")
    public ResponseEntity<Map<String, Object>> adminEndpoint(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        log.info("Admin endpoint accessed by: {}", jwt.getClaimAsString("preferred_username"));
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Hello from admin dashboard!");
        response.put("admin", jwt.getClaimAsString("preferred_username"));
        response.put("email", jwt.getClaimAsString("email"));
        response.put("roles", authentication.getAuthorities());
        response.put("status", "success");
        response.put("endpoint", "/admin/dashboard");
        
        Map<String, Object> permissions = new HashMap<>();
        permissions.put("users", "manage");
        permissions.put("system", "configure");
        permissions.put("reports", "view_all");
        response.put("permissions", permissions);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/manager/reports")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Manager reports", description = "Accessible with MANAGER role")
    public ResponseEntity<Map<String, Object>> managerEndpoint(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        log.info("Manager endpoint accessed by: {}", jwt.getClaimAsString("preferred_username"));
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Hello from manager reports!");
        response.put("manager", jwt.getClaimAsString("preferred_username"));
        response.put("roles", authentication.getAuthorities());
        response.put("status", "success");
        response.put("endpoint", "/manager/reports");
        
        Map<String, Object> reports = new HashMap<>();
        reports.put("monthly", "accessible");
        reports.put("team", "accessible");
        reports.put("budget", "accessible");
        response.put("reports", reports);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/protected/info")
    @Operation(summary = "Protected endpoint", description = "Accessible with any valid JWT token")
    public ResponseEntity<Map<String, Object>> protectedEndpoint(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        log.info("Protected endpoint accessed by: {}", jwt.getClaimAsString("preferred_username"));
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "This is a protected endpoint");
        response.put("user", jwt.getClaimAsString("preferred_username"));
        response.put("subject", jwt.getSubject());
        response.put("issuer", jwt.getIssuer().toString());
        response.put("audience", jwt.getAudience());
        response.put("expiresAt", jwt.getExpiresAt().toString());
        response.put("issuedAt", jwt.getIssuedAt().toString());
        response.put("roles", authentication.getAuthorities());
        response.put("status", "success");
        response.put("endpoint", "/protected/info");
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/settings")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "User settings", description = "User-specific settings endpoint")
    public ResponseEntity<Map<String, Object>> userSettings(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        log.info("User settings accessed by: {}", jwt.getClaimAsString("preferred_username"));
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "User settings endpoint");
        response.put("user", jwt.getClaimAsString("preferred_username"));
        response.put("status", "success");
        response.put("endpoint", "/user/settings");
        
        Map<String, Object> settings = new HashMap<>();
        settings.put("notifications", true);
        settings.put("theme", "light");
        settings.put("language", "ko");
        settings.put("timezone", "Asia/Seoul");
        response.put("settings", settings);
        
        return ResponseEntity.ok(response);
    }
}
