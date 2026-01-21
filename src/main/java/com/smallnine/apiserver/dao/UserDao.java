package com.smallnine.apiserver.dao;

import com.smallnine.apiserver.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface UserDao {
    
    /**
     * 根據ID查詢用戶
     */
    Optional<User> findById(@Param("id") Long id);
    
    /**
     * 根據用戶名查詢用戶
     */
    Optional<User> findByUsername(@Param("username") String username);
    
    /**
     * 根據信箱查詢用戶
     */
    Optional<User> findByEmail(@Param("email") String email);
    
    /**
     * 根據用戶名或信箱查詢用戶
     */
    Optional<User> findByUsernameOrEmail(@Param("usernameOrEmail") String usernameOrEmail);
    
    /**
     * 檢查用戶名是否存在
     */
    boolean existsByUsername(@Param("username") String username);
    
    /**
     * 檢查信箱是否存在
     */
    boolean existsByEmail(@Param("email") String email);
    
    /**
     * 創建用戶
     */
    int insert(User user);
    
    /**
     * 更新用戶訊息
     */
    int update(User user);
    
    /**
     * 根據ID刪除用戶
     */
    int deleteById(@Param("id") Long id);
    
    /**
     * 查詢所有用戶（分頁）
     */
    List<User> findAll(@Param("offset") int offset, @Param("limit") int limit);
    
    /**
     * 統計用戶總數
     */
    long count();
    
    /**
     * 更新用戶頭像
     */
    int updateImageUrl(@Param("id") Long id, @Param("imageUrl") String imageUrl);
    
    /**
     * 驗證用戶信箱
     */
    int validateEmail(@Param("id") Long id);
}