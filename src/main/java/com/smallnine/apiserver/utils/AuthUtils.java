package com.smallnine.apiserver.utils;

import com.smallnine.apiserver.constants.enums.ResponseCode;
import com.smallnine.apiserver.entity.User;
import com.smallnine.apiserver.exception.BusinessException;
import org.springframework.security.core.userdetails.UserDetails;

public final class AuthUtils {

    private AuthUtils() {
    }

    public static User getAuthenticatedUser(UserDetails userDetails) {
        if (userDetails == null) {
            throw new BusinessException(ResponseCode.UNAUTHORIZED);
        }
        if (!(userDetails instanceof User user)) {
            throw new BusinessException(ResponseCode.UNAUTHORIZED, "無效的身份驗證");
        }
        return user;
    }
}