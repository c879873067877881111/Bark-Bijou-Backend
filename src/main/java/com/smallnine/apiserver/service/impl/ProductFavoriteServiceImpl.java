package com.smallnine.apiserver.service.impl;

import com.smallnine.apiserver.constants.enums.ResponseCode;
import com.smallnine.apiserver.dao.ProductFavoriteDao;
import com.smallnine.apiserver.entity.Favorite;
import com.smallnine.apiserver.exception.BusinessException;
import com.smallnine.apiserver.service.ProductFavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductFavoriteServiceImpl implements ProductFavoriteService {

    private final ProductFavoriteDao productFavoriteDao;

    @Override
    public List<Favorite> getByMemberId(Long memberId) {
        return productFavoriteDao.findByMemberId(memberId);
    }

    @Override
    @Transactional
    public Favorite add(Long memberId, Long productId) {
        if (productFavoriteDao.existsByMemberIdAndProductId(memberId, productId)) {
            throw new BusinessException(ResponseCode.FAVORITE_ALREADY_EXISTS);
        }
        Favorite fav = new Favorite();
        fav.setMemberId(memberId);
        fav.setProductId(productId);
        productFavoriteDao.insert(fav);
        return fav;
    }

    @Override
    @Transactional
    public void remove(Long memberId, Long productId) {
        productFavoriteDao.deleteByMemberIdAndProductId(memberId, productId);
    }

    @Override
    public boolean isFavorite(Long memberId, Long productId) {
        return productFavoriteDao.existsByMemberIdAndProductId(memberId, productId);
    }
}
