package com.smallnine.apiserver.service;

import com.smallnine.apiserver.entity.Points;

import java.util.List;
import java.util.Map;

public interface PointsService {

    Map<String, Object> getPointsSummary(Long memberId);
}
