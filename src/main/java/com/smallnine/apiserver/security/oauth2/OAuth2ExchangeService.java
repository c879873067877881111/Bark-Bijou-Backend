package com.smallnine.apiserver.security.oauth2;

import com.smallnine.apiserver.constants.enums.ResponseCode;
import com.smallnine.apiserver.dao.UserDao;
import com.smallnine.apiserver.dto.AuthResponse;
import com.smallnine.apiserver.entity.User;
import com.smallnine.apiserver.exception.AccountDisabledException;
import com.smallnine.apiserver.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class OAuth2ExchangeService {

    private final OAuth2CodeStore codeStore;
    private final UserDao userDao;

    public AuthResponse exchange(String code) {
        OAuth2CodeStore.Payload payload = codeStore.consume(code)
                .orElseThrow(() -> new BusinessException(ResponseCode.BAD_REQUEST, "code 無效或已過期"));

        User user = userDao.findById(payload.getUserId())
                .orElseThrow(() -> new BusinessException(ResponseCode.USER_NOT_FOUND));

        // #NEW-B：跟 #NEW-A 同理,OAuth code 兌換也要擋停用帳號。
        // code 雖一次性 60s,但 admin 停用後若 user 已持 code,仍能換到 token。
        if (!user.isEnabled()) {
            throw new AccountDisabledException();
        }

        LocalDateTime expiresAt = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(payload.getAccessExpiresAtEpochMilli()),
                ZoneId.systemDefault());

        return new AuthResponse(payload.getAccessToken(), payload.getRefreshToken(), user, expiresAt);
    }
}