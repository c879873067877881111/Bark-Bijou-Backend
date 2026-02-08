package com.smallnine.apiserver.dto;

import lombok.Data;

@Data
public class MemberUpdateRequest {
    private String realname;
    private String phone;
    private String gender;
    private String city;
    private String address;
    private String zip;
    private String imageUrl;
}
