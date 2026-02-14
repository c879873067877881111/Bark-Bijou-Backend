package com.smallnine.apiserver.service.impl;

import com.smallnine.apiserver.constants.enums.ResponseCode;
import com.smallnine.apiserver.dao.VipLevelDao;
import com.smallnine.apiserver.entity.VipLevel;
import com.smallnine.apiserver.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.smallnine.apiserver.service.VipLevelService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class VipLevelServiceImpl implements VipLevelService {
    
    private final VipLevelDao vipLevelDao;
    
    @Cacheable(value = "vipLevels", key = "'id:' + #id")
    public VipLevel findById(Long id) {
        return vipLevelDao.findById(id)
                .orElseThrow(() -> new BusinessException(ResponseCode.VIP_LEVEL_NOT_FOUND));
    }
    
    @Cacheable(value = "vipLevels", key = "'name:' + #name")
    public VipLevel findByName(String name) {
        return vipLevelDao.findByName(name)
                .orElseThrow(() -> new BusinessException(ResponseCode.VIP_LEVEL_NOT_FOUND));
    }
    
    @Cacheable(value = "vipLevels", key = "'allActive'")
    public List<VipLevel> findAllActive() {
        return vipLevelDao.findAllActive();
    }
    
    @Cacheable(value = "vipLevels", key = "'all'")
    public List<VipLevel> findAll() {
        return vipLevelDao.findAll();
    }
    
    public VipLevel getVipLevelBySpending(BigDecimal spending) {
        if (spending == null || spending.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ResponseCode.INVALID_SPENDING_AMOUNT);
        }

        // Use cached findAllActive() and compute in app — avoids BigDecimal cache key issues
        return findAllActive().stream()
                .filter(v -> spending.compareTo(v.getMinSpending()) >= 0)
                .reduce((a, b) -> b.getMinSpending().compareTo(a.getMinSpending()) > 0 ? b : a)
                .orElse(null);
    }
    
    @CacheEvict(value = "vipLevels", allEntries = true)
    @Transactional
    public VipLevel createVipLevel(VipLevel vipLevel) {
        validateVipLevel(vipLevel);
        
        // 檢查名稱是否已存在
        if (vipLevelDao.existsByName(vipLevel.getName())) {
            throw new BusinessException(ResponseCode.VIP_LEVEL_NAME_EXISTS);
        }
        
        // 設置默認值
        if (vipLevel.getIsActive() == null) {
            vipLevel.setIsActive(true);
        }
        
        int result = vipLevelDao.insert(vipLevel);
        if (result == 0) {
            throw new BusinessException(ResponseCode.VIP_LEVEL_CREATE_FAILED);
        }
        
        log.info("VIP等級創建成功: id={}, name={}", vipLevel.getId(), vipLevel.getName());
        return vipLevel;
    }
    
    @CacheEvict(value = "vipLevels", allEntries = true)
    @Transactional
    public VipLevel updateVipLevel(VipLevel vipLevel) {
        VipLevel existing = findById(vipLevel.getId());
        validateVipLevel(vipLevel);
        
        // 檢查名稱是否與其他記錄衝突
        VipLevel existingByName = vipLevelDao.findByName(vipLevel.getName()).orElse(null);
        if (existingByName != null && !existingByName.getId().equals(vipLevel.getId())) {
            throw new BusinessException(ResponseCode.VIP_LEVEL_NAME_EXISTS);
        }
        
        int result = vipLevelDao.update(vipLevel);
        if (result == 0) {
            throw new BusinessException(ResponseCode.VIP_LEVEL_UPDATE_FAILED);
        }
        
        log.info("VIP等級更新成功: id={}, name={}", vipLevel.getId(), vipLevel.getName());
        return findById(vipLevel.getId());
    }
    
    @CacheEvict(value = "vipLevels", allEntries = true)
    @Transactional
    public void deleteVipLevel(Long id) {
        VipLevel vipLevel = findById(id);
        
        int result = vipLevelDao.deleteById(id);
        if (result == 0) {
            throw new BusinessException(ResponseCode.VIP_LEVEL_DELETE_FAILED);
        }
        
        log.info("VIP等級刪除成功: id={}, name={}", id, vipLevel.getName());
    }
    
    /**
     * 統計VIP等級總數
     */
    public long count() {
        return vipLevelDao.count();
    }
    
    /**
     * 統計啟用的VIP等級數量
     */
    public long countActive() {
        return vipLevelDao.countActive();
    }
    
    /**
     * 驗證VIP等級數據
     */
    private void validateVipLevel(VipLevel vipLevel) {
        if (!StringUtils.hasText(vipLevel.getName())) {
            throw new BusinessException(ResponseCode.VIP_LEVEL_NAME_REQUIRED);
        }
        
        if (vipLevel.getName().length() > 50) {
            throw new BusinessException(ResponseCode.VIP_LEVEL_NAME_TOO_LONG);
        }
        
        if (vipLevel.getMinSpending() == null || vipLevel.getMinSpending().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ResponseCode.INVALID_MIN_SPENDING);
        }
        
        if (vipLevel.getDiscountRate() == null || 
            vipLevel.getDiscountRate().compareTo(BigDecimal.ZERO) < 0 ||
            vipLevel.getDiscountRate().compareTo(BigDecimal.ONE) > 0) {
            throw new BusinessException(ResponseCode.INVALID_DISCOUNT_RATE);
        }
        
        if (vipLevel.getPointsMultiplier() == null || vipLevel.getPointsMultiplier().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ResponseCode.INVALID_POINTS_MULTIPLIER);
        }
        
        if (vipLevel.getFreeShippingThreshold() != null && vipLevel.getFreeShippingThreshold().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ResponseCode.INVALID_FREE_SHIPPING_THRESHOLD);
        }
    }
}