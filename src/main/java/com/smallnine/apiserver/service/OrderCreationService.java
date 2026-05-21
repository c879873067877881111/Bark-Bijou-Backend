package com.smallnine.apiserver.service;

import com.smallnine.apiserver.dto.CreateOrderRequest;
import com.smallnine.apiserver.entity.Order;

/**
 * 訂單建立交易單元：負責「從購物車生成訂單」這段交易邏輯本體。
 * 拆出成獨立 bean 的目的是讓 OrderService 的冪等性編排能透過外部 bean 呼叫，
 * 自然走 Spring AOP 代理啟動 @Transactional，不需要 self-injection。
 */
public interface OrderCreationService {

    /**
     * 從購物車建立訂單（單一交易單元）。
     *
     * @param idempotencyKey 冪等鍵，會寫進 order.idempotency_key 觸發 DB unique constraint；
     *                       無冪等需求時傳 null。
     */
    Order create(Long memberId, CreateOrderRequest request, String idempotencyKey);
}