package com.smallnine.apiserver.service.impl;

import com.smallnine.apiserver.service.EcpayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@Slf4j
public class EcpayServiceImpl implements EcpayService {

    @Value("${ecpay.merchant-id:3002607}")
    private String merchantId;

    @Value("${ecpay.hash-key:pwFHCqoQZGmho4w6}")
    private String hashKey;

    @Value("${ecpay.hash-iv:EkRm7iFT261dpevs}")
    private String hashIv;

    @Value("${ecpay.payment-url:https://payment-stage.ecpay.com.tw/Cashier/AioCheckOut/V5}")
    private String paymentUrl;

    @Value("${ecpay.return-url:http://localhost:8080/api/ecpay/callback}")
    private String returnUrl;

    @Value("${ecpay.client-back-url:http://localhost:3000/member/orders}")
    private String clientBackUrl;

    @Override
    public String createPaymentForm(Long orderId, int totalAmount, String itemName) {
        String tradeNo = "BK" + orderId + "T" + System.currentTimeMillis();
        String tradeDate = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date());

        Map<String, String> params = new TreeMap<>();
        params.put("MerchantID", merchantId);
        params.put("MerchantTradeNo", tradeNo.length() > 20 ? tradeNo.substring(0, 20) : tradeNo);
        params.put("MerchantTradeDate", tradeDate);
        params.put("PaymentType", "aio");
        params.put("TotalAmount", String.valueOf(totalAmount));
        params.put("TradeDesc", "BARK & BIJOU 訂單付款");
        params.put("ItemName", itemName != null ? itemName : "商品訂單");
        params.put("ReturnURL", returnUrl);
        params.put("ClientBackURL", clientBackUrl);
        params.put("ChoosePayment", "ALL");
        params.put("EncryptType", "1");
        params.put("CustomField1", String.valueOf(orderId));

        String checkMacValue = generateCheckMac(params);
        params.put("CheckMacValue", checkMacValue);

        // Build HTML form
        StringBuilder html = new StringBuilder();
        html.append("<form id='ecpayForm' method='post' action='").append(paymentUrl).append("'>");
        for (Map.Entry<String, String> entry : params.entrySet()) {
            html.append("<input type='hidden' name='").append(entry.getKey())
                    .append("' value='").append(entry.getValue()).append("' />");
        }
        html.append("</form>");

        return html.toString();
    }

    @Override
    public boolean handleCallback(Map<String, String> params) {
        String receivedMac = params.get("CheckMacValue");
        Map<String, String> filtered = new TreeMap<>(params);
        filtered.remove("CheckMacValue");
        String calculated = generateCheckMac(filtered);

        if (!calculated.equalsIgnoreCase(receivedMac)) {
            log.warn("ECPay callback MAC mismatch");
            return false;
        }

        String rtnCode = params.get("RtnCode");
        String orderId = params.get("CustomField1");
        log.info("ECPay callback orderId={} rtnCode={}", orderId, rtnCode);

        // RtnCode=1 means payment success
        return "1".equals(rtnCode);
    }

    private String generateCheckMac(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        sb.append("HashKey=").append(hashKey).append("&");
        for (Map.Entry<String, String> entry : new TreeMap<>(params).entrySet()) {
            sb.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
        }
        sb.append("HashIV=").append(hashIv);

        String encoded = URLEncoder.encode(sb.toString(), StandardCharsets.UTF_8).toLowerCase();
        // ECPay specific URL encoding rules
        encoded = encoded.replace("%2d", "-").replace("%5f", "_")
                .replace("%2e", ".").replace("%21", "!")
                .replace("%2a", "*").replace("%28", "(")
                .replace("%29", ")").replace("%20", "+");

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(encoded.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString().toUpperCase();
        } catch (Exception e) {
            throw new RuntimeException("CheckMac generation failed", e);
        }
    }
}
