package com.smallnine.apiserver.service.impl;

import com.smallnine.apiserver.constants.enums.ResponseCode;
import com.smallnine.apiserver.dao.NotificationDao;
import com.smallnine.apiserver.entity.Notification;
import com.smallnine.apiserver.exception.BusinessException;
import com.smallnine.apiserver.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationDao notificationDao;

    @Override
    public List<Notification> getByMemberId(Long memberId) {
        return notificationDao.findByMemberId(memberId);
    }

    @Override
    @Transactional
    public void markAsRead(Long id, Long memberId) {
        Notification n = notificationDao.findById(id)
                .orElseThrow(() -> new BusinessException(ResponseCode.NOTIFICATION_NOT_FOUND));
        if (!n.getMemberId().equals(memberId)) {
            throw new BusinessException(ResponseCode.FORBIDDEN);
        }
        notificationDao.markAsRead(id);
    }

    @Override
    @Transactional
    public void markAllAsRead(Long memberId) {
        notificationDao.markAllAsRead(memberId);
    }

    @Override
    @Transactional
    public void create(Notification notification) {
        notificationDao.insert(notification);
    }

    @Override
    @Transactional
    public void delete(Long id, Long memberId) {
        Notification n = notificationDao.findById(id)
                .orElseThrow(() -> new BusinessException(ResponseCode.NOTIFICATION_NOT_FOUND));
        if (!n.getMemberId().equals(memberId)) {
            throw new BusinessException(ResponseCode.FORBIDDEN);
        }
        notificationDao.deleteById(id);
    }
}
