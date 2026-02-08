package com.smallnine.apiserver.dao;

import com.smallnine.apiserver.entity.EmailVerification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

@Mapper
public interface EmailVerificationDao {

    int insert(EmailVerification verification);

    Optional<EmailVerification> findByEmailAndOtp(@Param("email") String email, @Param("otpToken") String otpToken);

    Optional<EmailVerification> findBySecret(@Param("secret") String secret);

    Optional<EmailVerification> findBySecretAndOtp(@Param("secret") String secret, @Param("otpToken") String otpToken);

    boolean existsByEmail(@Param("email") String email);

    int deleteByEmail(@Param("email") String email);
}
