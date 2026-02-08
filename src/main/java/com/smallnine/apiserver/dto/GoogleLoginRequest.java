package com.smallnine.apiserver.dto;

import lombok.Data;

@Data
public class GoogleLoginRequest {
    private String providerId;
    private String uid;
    private String displayName;
    private String email;
    private String photoURL;
    private String idToken;
}
