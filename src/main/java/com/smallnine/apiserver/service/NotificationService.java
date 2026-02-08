package com.smallnine.apiserver.service;

import com.smallnine.apiserver.entity.Notification;

import java.util.List;

public interface NotificationService {

    List<Notification> getByMemberId(Long memberId);

    void markAsRead(Long id, Long memberId);

    void markAllAsRead(Long memberId);

    void delete(Long id, Long memberId);

    void create(Notification notification);
}
