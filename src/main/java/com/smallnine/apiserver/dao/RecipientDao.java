package com.smallnine.apiserver.dao;

import com.smallnine.apiserver.entity.Recipient;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface RecipientDao {

    List<Recipient> findByMemberId(@Param("memberId") Long memberId);

    Optional<Recipient> findById(@Param("id") Long id);

    int insert(Recipient recipient);

    int update(Recipient recipient);

    int deleteById(@Param("id") Long id);

    int clearDefault(@Param("memberId") Long memberId);
}
