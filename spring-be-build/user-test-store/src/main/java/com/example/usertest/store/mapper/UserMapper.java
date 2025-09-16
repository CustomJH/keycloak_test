package com.example.usertest.store.mapper;

import com.example.usertest.domain.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Delete;

import java.util.List;
import java.util.Optional;

@Mapper
public interface UserMapper {
    
    @Select("SELECT * FROM users WHERE id = #{id}")
    Optional<User> findById(@Param("id") Long id);
    
    @Select("SELECT * FROM users WHERE username = #{username}")
    Optional<User> findByUsername(@Param("username") String username);
    
    @Select("SELECT * FROM users WHERE email = #{email}")
    Optional<User> findByEmail(@Param("email") String email);
    
    @Select("SELECT * FROM users ORDER BY created_at DESC")
    List<User> findAll();
    
    @Insert("INSERT INTO users (username, email, password, role, enabled, created_at, updated_at) " +
            "VALUES (#{username}, #{email}, #{password}, #{role}, #{enabled}, NOW(), NOW())")
    void insert(User user);
    
    @Update("UPDATE users SET username = #{username}, email = #{email}, " +
            "password = #{password}, role = #{role}, enabled = #{enabled}, updated_at = NOW() " +
            "WHERE id = #{id}")
    void update(User user);
    
    @Delete("DELETE FROM users WHERE id = #{id}")
    void deleteById(@Param("id") Long id);
    
    @Select("SELECT COUNT(*) FROM users WHERE username = #{username}")
    boolean existsByUsername(@Param("username") String username);
    
    @Select("SELECT COUNT(*) FROM users WHERE email = #{email}")
    boolean existsByEmail(@Param("email") String email);
}