package com.smallnine.apiserver.dao;

import com.smallnine.apiserver.entity.SitterGallery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SitterGalleryDao {

    List<SitterGallery> findBySitterId(@Param("sitterId") Long sitterId);

    int insert(SitterGallery gallery);

    int deleteBySitterId(@Param("sitterId") Long sitterId);
}
