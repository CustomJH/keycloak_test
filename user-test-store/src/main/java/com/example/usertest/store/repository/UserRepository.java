package com.example.usertest.store.repository;

import com.example.usertest.domain.User;
import com.example.usertest.domain.exception.UserNotFoundException;
import com.example.usertest.store.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserRepository {
    
    private final UserMapper userMapper;
    
    public Optional<User> findById(Long id) {
        return userMapper.findById(id);
    }
    
    public User getById(Long id) {
        return findById(id).orElseThrow(() -> UserNotFoundException.byId(id));
    }
    
    public Optional<User> findByUsername(String username) {
        return userMapper.findByUsername(username);
    }
    
    public User getByUsername(String username) {
        return findByUsername(username).orElseThrow(() -> UserNotFoundException.byUsername(username));
    }
    
    public Optional<User> findByEmail(String email) {
        return userMapper.findByEmail(email);
    }
    
    public List<User> findAll() {
        return userMapper.findAll();
    }
    
    public void save(User user) {
        if (user.getId() == null) {
            userMapper.insert(user);
        } else {
            userMapper.update(user);
        }
    }
    
    public void deleteById(Long id) {
        userMapper.deleteById(id);
    }
    
    public boolean existsByUsername(String username) {
        return userMapper.existsByUsername(username);
    }
    
    public boolean existsByEmail(String email) {
        return userMapper.existsByEmail(email);
    }
}