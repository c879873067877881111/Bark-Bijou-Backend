package com.smallnine.apiserver.dto;

import lombok.Data;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
public class SitterListResponse {

    private int total;
    private int page;
    private int pageSize;
    private List<SitterResponse> data;
}
