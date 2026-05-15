package com.smallnine.apiserver.utils;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 限流用 client IP 解析。
 *
 * X-Forwarded-For 可被 client 任意偽造，且 nginx 預設 $proxy_add_x_forwarded_for
 * 是「append」不是「覆寫」——header 會長成 {client 自己塞的..., 反代看到的真實來源}。
 * 因此：
 *  - 預設 trust=false：完全不看 header，用 remoteAddr
 *  - trust=true（確定部署在「單一」可信反代後）：取「最右側」一段，
 *    那是可信反代附加的、client 無法偽造的值。取第一段（最左）才是 #35 的舊錯誤。
 *
 * 多層反代要另算 hop 數，本專案目前單一反代，不過度設計。
 */
@Component
public class ClientIpResolver {

    @Value("${app.rate-limit.trust-forwarded-for:false}")
    private boolean trustForwardedFor;

    public String resolve(HttpServletRequest request) {
        if (trustForwardedFor) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                String[] hops = forwarded.split(",");
                for (int i = hops.length - 1; i >= 0; i--) {
                    String hop = hops[i].trim();
                    if (!hop.isEmpty()) {
                        return hop;
                    }
                }
            }
        }
        return request.getRemoteAddr();
    }
}
