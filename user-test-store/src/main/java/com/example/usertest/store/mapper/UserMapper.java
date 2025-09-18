package com.example.usertest.store.mapper;

import com.example.usertest.domain.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 사용자 데이터 접근을 위한 MyBatis 매퍼 인터페이스
 * 
 * XML 매퍼 파일을 통해 SQL 쿼리가 정의됩니다.
 * 매퍼 XML 파일: resources/mapper/UserMapper.xml
 * 
 * @author YourName
 * @version 1.0
 * @since 2024-09
 */
@Mapper
public interface UserMapper {
    
    /**
     * ID로 사용자 조회
     * @param id 사용자 ID
     * @return 사용자 정보 (Optional)
     */
    Optional<User> findById(@Param("id") Long id);
    
    /**
     * 사용자명으로 사용자 조회
     * @param username 사용자명
     * @return 사용자 정보 (Optional)
     */
    Optional<User> findByUsername(@Param("username") String username);
    
    /**
     * 이메일로 사용자 조회
     * @param email 이메일 주소
     * @return 사용자 정보 (Optional)
     */
    Optional<User> findByEmail(@Param("email") String email);
    
    /**
     * 모든 사용자 조회 (생성일 기준 내림차순)
     * @return 사용자 목록
     */
    List<User> findAll();
    
    /**
     * 사용자 신규 등록
     * @param user 등록할 사용자 정보
     */
    void insert(User user);
    
    /**
     * 사용자 정보 수정
     * @param user 수정할 사용자 정보
     */
    void update(User user);
    
    /**
     * ID로 사용자 삭제
     * @param id 삭제할 사용자 ID
     */
    void deleteById(@Param("id") Long id);
    
    /**
     * 사용자명 중복 확인
     * @param username 확인할 사용자명
     * @return 중복 여부 (true: 존재, false: 사용가능)
     */
    boolean existsByUsername(@Param("username") String username);
    
    /**
     * 이메일 중복 확인
     * @param email 확인할 이메일 주소
     * @return 중복 여부 (true: 존재, false: 사용가능)
     */
    boolean existsByEmail(@Param("email") String email);
}