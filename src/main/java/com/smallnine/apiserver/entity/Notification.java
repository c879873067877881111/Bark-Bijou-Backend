package com.smallnine.apiserver.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    private Long id;

    @JsonProperty("member_id")
    private Long memberId;

    private String title;

    @JsonProperty("content")
    private String message;

    private String type;

    @JsonProperty("is_read")
    private Boolean isRead;

    @JsonProperty("action_url")
    private String actionUrl;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;
}
