package com.smallnine.apiserver.service;

import com.smallnine.apiserver.dto.CreateOrderRequest;
import com.smallnine.apiserver.service.impl.OrderCreationServiceImpl;
import com.smallnine.apiserver.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 設計守護：交易邊界必須留在獨立 bean OrderCreationServiceImpl.create()，
 * 不可搬回 OrderServiceImpl 的任何一個 createOrderFromCart overload。
 *
 * 違反原因：
 * 1. OrderServiceImpl.createOrderFromCart(memberId, request) 只是 delegate，
 *    若標 @Transactional 會多開一層無意義交易、混淆語意。
 * 2. OrderServiceImpl.createOrderFromCart(memberId, request, idempotencyKey) 是冪等性編排，
 *    若標 @Transactional 會把 Redis SETNX、Redis finalize 跟 DB commit 綁進同一交易，
 *    讓「DB commit 完才寫 Redis orderId」這個時序保證壞掉。
 * 3. 交易邊界一旦搬回 OrderServiceImpl，外層→內層的同類別呼叫就會踩到 self-invocation，
 *    @Transactional 切面不會啟動（除非重新引入 ObjectProvider self-injection 那套繞圈）。
 *
 * 任何「想把 @Transactional 加在 OrderServiceImpl 上」的 PR 應該先被這支測試擋下。
 */
class OrderTransactionBoundaryTest {

    @Test
    void transactionalLivesOnOrderCreationServiceImpl_create() throws NoSuchMethodException {
        Method create = OrderCreationServiceImpl.class
                .getDeclaredMethod("create", Long.class, CreateOrderRequest.class, String.class);

        assertTrue(create.isAnnotationPresent(Transactional.class),
                "@Transactional 必須在 OrderCreationServiceImpl.create() 上，"
                        + "外層編排才能透過 proxy bean 啟動交易切面");
    }

    @Test
    void orderServiceImpl_createOrderFromCart_delegateOverload_hasNoTransactional()
            throws NoSuchMethodException {
        Method delegate = OrderServiceImpl.class
                .getDeclaredMethod("createOrderFromCart", Long.class, CreateOrderRequest.class);

        assertFalse(delegate.isAnnotationPresent(Transactional.class),
                "OrderServiceImpl 的 createOrderFromCart(memberId, request) 只應 delegate 到 "
                        + "OrderCreationService，不可帶 @Transactional");
    }

    @Test
    void orderServiceImpl_createOrderFromCart_idempotencyOverload_hasNoTransactional()
            throws NoSuchMethodException {
        Method idempotencyOrchestrator = OrderServiceImpl.class.getDeclaredMethod(
                "createOrderFromCart", Long.class, CreateOrderRequest.class, String.class);

        assertFalse(idempotencyOrchestrator.isAnnotationPresent(Transactional.class),
                "冪等性編排不可標 @Transactional：會把 Redis SETNX / finalize 綁進 DB 交易，"
                        + "破壞『DB commit 完才寫 Redis orderId』的時序保證");
    }

    @Test
    void orderServiceImpl_classLevel_hasNoTransactional() {
        // class-level @Transactional 會繼承到所有 method，等於繞過上面三條 method-level 守護。
        // 必須一起擋下，否則設計守護有破口。
        assertFalse(OrderServiceImpl.class.isAnnotationPresent(Transactional.class),
                "OrderServiceImpl 不可在 class 上標 @Transactional："
                        + "會把交易切面套到所有 method，包含冪等性編排，破壞交易邊界設計");
    }
}