package com.smallnine.apiserver.service;

import com.smallnine.apiserver.entity.Favorite;

import java.util.List;

public interface ProductFavoriteService {

    List<Favorite> getByMemberId(Long memberId);

    Favorite add(Long memberId, Long productId);

    void remove(Long memberId, Long productId);

    boolean isFavorite(Long memberId, Long productId);
}
