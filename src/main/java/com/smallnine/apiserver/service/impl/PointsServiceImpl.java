package com.smallnine.apiserver.service.impl;

import com.smallnine.apiserver.dao.PointsDao;
import com.smallnine.apiserver.entity.Points;
import com.smallnine.apiserver.service.PointsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PointsServiceImpl implements PointsService {

    private final PointsDao pointsDao;

    @Override
    public Map<String, Object> getPointsSummary(Long memberId) {
        int total = pointsDao.sumByMemberId(memberId);
        List<Points> history = pointsDao.findByMemberId(memberId);
        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("history", history);
        return result;
    }
}
