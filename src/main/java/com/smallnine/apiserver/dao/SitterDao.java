package com.smallnine.apiserver.dao;

import com.smallnine.apiserver.entity.Sitter;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface SitterDao {

    Optional<Sitter> findById(@Param("id") Long id);

    Optional<Sitter> findByMemberId(@Param("memberId") Long memberId);

    List<Sitter> searchSitters(@Param("search") String search,
                               @Param("area") String area,
                               @Param("sort") String sort,
                               @Param("offset") int offset,
                               @Param("limit") int limit);

    int countSitters(@Param("search") String search,
                     @Param("area") String area);

    int insert(Sitter sitter);

    int update(Sitter sitter);

    int deleteById(@Param("id") Long id);
}
