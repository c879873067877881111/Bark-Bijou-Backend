package com.smallnine.apiserver.dao;

import com.smallnine.apiserver.entity.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface NotificationDao {

    List<Notification> findByMemberId(@Param("memberId") Long memberId);

    Optional<Notification> findById(@Param("id") Long id);

    int markAsRead(@Param("id") Long id);

    int markAllAsRead(@Param("memberId") Long memberId);

    int deleteById(@Param("id") Long id);

    int insert(Notification notification);
}
