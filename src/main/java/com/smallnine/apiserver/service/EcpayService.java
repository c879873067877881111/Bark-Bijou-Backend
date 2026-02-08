package com.smallnine.apiserver.service;

import java.util.Map;

public interface EcpayService {

    /**
     * 建立 ECPay 付款表單 HTML
     */
    String createPaymentForm(Long orderId, int totalAmount, String itemName);

    /**
     * 處理 ECPay 回呼
     */
    boolean handleCallback(Map<String, String> params);
}
