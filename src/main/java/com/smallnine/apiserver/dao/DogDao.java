package com.smallnine.apiserver.dao;

import com.smallnine.apiserver.entity.Dog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface DogDao {

    List<Dog> findByMemberId(@Param("memberId") Long memberId);

    Optional<Dog> findById(@Param("id") Long id);

    int insert(Dog dog);

    int update(Dog dog);

    int deleteById(@Param("id") Long id);
}
