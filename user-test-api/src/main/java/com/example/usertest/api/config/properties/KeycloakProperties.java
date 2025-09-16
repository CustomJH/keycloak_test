package com.example.usertest.api.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "keycloak")
public class KeycloakProperties {
    
    private String serverUrl;
    private String realm;
    private String clientId;
    private String clientSecret;
    private String tokenEndpoint;
    private Admin admin = new Admin();
    
    @Data
    public static class Admin {
        private String username;
        private String password;
    }
}