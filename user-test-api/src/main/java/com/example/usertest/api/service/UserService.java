package com.example.usertest.api.service;

import com.example.usertest.domain.User;
import com.example.usertest.domain.dto.UserDto;
import com.example.usertest.store.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    public List<User> getAllUsers() {
        log.debug("Retrieving all users");
        return userRepository.findAll();
    }
    
    public User getUserById(Long id) {
        log.debug("Retrieving user by id: {}", id);
        return userRepository.getById(id);
    }
    
    public User getUserByUsername(String username) {
        log.debug("Retrieving user by username: {}", username);
        return userRepository.getByUsername(username);
    }
    
    @Transactional
    public User createUser(UserDto userDto) {
        log.info("Creating new user: {}", userDto.getUsername());
        
        // Check if username or email already exists
        if (userRepository.existsByUsername(userDto.getUsername())) {
            throw new IllegalArgumentException("Username already exists: " + userDto.getUsername());
        }
        
        if (userRepository.existsByEmail(userDto.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + userDto.getEmail());
        }
        
        User user = User.builder()
                .username(userDto.getUsername())
                .email(userDto.getEmail())
                .password(passwordEncoder.encode("defaultPassword")) // In real app, get from DTO
                .role(userDto.getRole() != null ? userDto.getRole() : "USER")
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        userRepository.save(user);
        log.info("User created successfully: {}", user.getUsername());
        return user;
    }
    
    @Transactional
    public User updateUser(Long id, UserDto userDto) {
        log.info("Updating user with id: {}", id);
        
        User existingUser = userRepository.getById(id);
        
        // Check if new username/email conflicts with other users
        if (!existingUser.getUsername().equals(userDto.getUsername()) && 
            userRepository.existsByUsername(userDto.getUsername())) {
            throw new IllegalArgumentException("Username already exists: " + userDto.getUsername());
        }
        
        if (!existingUser.getEmail().equals(userDto.getEmail()) && 
            userRepository.existsByEmail(userDto.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + userDto.getEmail());
        }
        
        existingUser.setUsername(userDto.getUsername());
        existingUser.setEmail(userDto.getEmail());
        existingUser.setRole(userDto.getRole());
        existingUser.setEnabled(userDto.isEnabled());
        existingUser.setUpdatedAt(LocalDateTime.now());
        
        userRepository.save(existingUser);
        log.info("User updated successfully: {}", existingUser.getUsername());
        return existingUser;
    }
    
    @Transactional
    public void deleteUser(Long id) {
        log.info("Deleting user with id: {}", id);
        
        // Verify user exists before deletion
        userRepository.getById(id);
        userRepository.deleteById(id);
        
        log.info("User deleted successfully with id: {}", id);
    }
    
    public UserDto convertToDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .enabled(user.isEnabled())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}