package com.smallnine.apiserver.constants.enums;

import org.springframework.http.HttpStatus;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum ResponseCode {
    SUCCESS(200, "成功", HttpStatus.OK),
    CREATED(201, "創建成功", HttpStatus.CREATED),
    BAD_REQUEST(400, "請求參數錯誤", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED(401, "未授權", HttpStatus.UNAUTHORIZED),
    FORBIDDEN(403, "禁止訪問", HttpStatus.FORBIDDEN),
    NOT_FOUND(404, "資源不存在", HttpStatus.NOT_FOUND),
    CONFLICT(409, "資源衝突", HttpStatus.CONFLICT),
    INTERNAL_SERVER_ERROR(500, "內部服務器錯誤", HttpStatus.INTERNAL_SERVER_ERROR),

    // 業務異常 - 用戶相關
    USER_NOT_FOUND(1001, "用戶不存在", HttpStatus.NOT_FOUND),
    USER_ALREADY_EXISTS(1002, "用戶已存在", HttpStatus.BAD_REQUEST),
    INVALID_PASSWORD(1003, "密碼錯誤", HttpStatus.BAD_REQUEST),

    // 業務異常 - 商品相關
    PRODUCT_NOT_FOUND(2001, "商品不存在", HttpStatus.NOT_FOUND),
    INSUFFICIENT_STOCK(2002, "庫存不足", HttpStatus.BAD_REQUEST),
    PRODUCT_SKU_EXISTS(2005, "SKU已存在", HttpStatus.BAD_REQUEST),
    PRODUCT_INACTIVE(2006, "商品已下架", HttpStatus.BAD_REQUEST),
    INVALID_QUANTITY(2007, "無效的數量", HttpStatus.BAD_REQUEST),
    CATEGORY_NOT_FOUND(2003, "分類不存在", HttpStatus.NOT_FOUND),
    BRAND_NOT_FOUND(2004, "品牌不存在", HttpStatus.NOT_FOUND),

    // 業務異常 - 訂單相關
    ORDER_NOT_FOUND(3001, "訂單不存在", HttpStatus.NOT_FOUND),
    ORDER_STATUS_ERROR(3002, "訂單狀態錯誤", HttpStatus.BAD_REQUEST),
    ORDER_INVALID_TRANSITION(3005, "無效的訂單狀態轉換", HttpStatus.BAD_REQUEST),
    CART_EMPTY(3003, "購物車為空", HttpStatus.BAD_REQUEST),
    INVALID_PAGINATION(3004, "無效的分頁參數", HttpStatus.BAD_REQUEST),

    // 業務異常 - 文章相關
    ARTICLE_NOT_FOUND(4001, "文章不存在", HttpStatus.NOT_FOUND),
    ARTICLE_CREATE_FAILED(4002, "文章創建失敗", HttpStatus.BAD_REQUEST),
    ARTICLE_UPDATE_FAILED(4003, "文章更新失敗", HttpStatus.BAD_REQUEST),
    ARTICLE_DELETE_FAILED(4004, "文章刪除失敗", HttpStatus.BAD_REQUEST),

    // 業務異常 - VIP等級相關
    VIP_LEVEL_NOT_FOUND(5001, "VIP等級不存在", HttpStatus.NOT_FOUND),
    VIP_LEVEL_NAME_EXISTS(5002, "VIP等級名稱已存在", HttpStatus.BAD_REQUEST),
    VIP_LEVEL_CREATE_FAILED(5003, "VIP等級創建失敗", HttpStatus.BAD_REQUEST),
    VIP_LEVEL_UPDATE_FAILED(5004, "VIP等級更新失敗", HttpStatus.BAD_REQUEST),
    VIP_LEVEL_DELETE_FAILED(5005, "VIP等級刪除失敗", HttpStatus.BAD_REQUEST),
    VIP_LEVEL_NAME_REQUIRED(5006, "VIP等級名稱不能為空", HttpStatus.BAD_REQUEST),
    VIP_LEVEL_NAME_TOO_LONG(5007, "VIP等級名稱過長", HttpStatus.BAD_REQUEST),
    INVALID_SPENDING_AMOUNT(5008, "無效的消費金額", HttpStatus.BAD_REQUEST),
    INVALID_MIN_SPENDING(5009, "最低消費金額無效", HttpStatus.BAD_REQUEST),
    INVALID_DISCOUNT_RATE(5010, "折扣率必須在0-1之間", HttpStatus.BAD_REQUEST),
    INVALID_POINTS_MULTIPLIER(5011, "積分倍數必須大於0", HttpStatus.BAD_REQUEST),
    INVALID_FREE_SHIPPING_THRESHOLD(5012, "免運費門檻無效", HttpStatus.BAD_REQUEST),

    // 業務異常 - 保母相關
    SITTER_NOT_FOUND(6001, "保母不存在", HttpStatus.NOT_FOUND),
    SITTER_ALREADY_EXISTS(6002, "已註冊為保母", HttpStatus.CONFLICT),
    SITTER_FORBIDDEN(6003, "無權限操作此保母資料", HttpStatus.FORBIDDEN),

    // 業務異常 - 寵物相關
    DOG_NOT_FOUND(6006, "寵物不存在", HttpStatus.NOT_FOUND),

    // 業務異常 - 優惠券相關
    COUPON_NOT_FOUND(7001, "優惠券不存在", HttpStatus.NOT_FOUND),
    COUPON_ALREADY_CLAIMED(7002, "已領取過此優惠券", HttpStatus.CONFLICT),
    COUPON_EXPIRED(7003, "優惠券已過期", HttpStatus.BAD_REQUEST),

    // 業務異常 - 通知相關
    NOTIFICATION_NOT_FOUND(7501, "通知不存在", HttpStatus.NOT_FOUND),

    // 業務異常 - OTP 相關
    OTP_INVALID(7601, "驗證碼錯誤", HttpStatus.BAD_REQUEST),
    OTP_EXPIRED(7602, "驗證碼已過期", HttpStatus.BAD_REQUEST),

    // 業務異常 - 收藏/評價相關
    FAVORITE_ALREADY_EXISTS(8001, "已收藏過", HttpStatus.CONFLICT),
    REVIEW_NOT_FOUND(8003, "評價不存在", HttpStatus.NOT_FOUND),
    REVIEW_ALREADY_EXISTS(8004, "已評價過", HttpStatus.CONFLICT),
    REVIEW_NO_BOOKING(8005, "尚未預約無法評價", HttpStatus.BAD_REQUEST),

    // 業務異常 - 收件人相關
    RECIPIENT_NOT_FOUND(8101, "收件人不存在", HttpStatus.NOT_FOUND),

    // 業務異常 - 郵件相關
    MAIL_SEND_FAILED(9001, "郵件發送失敗", HttpStatus.INTERNAL_SERVER_ERROR);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    ResponseCode(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    // 預建index，時間複雜度 O(1) 查詢
    private static final Map<Integer, ResponseCode> CODE_MAP = Stream.of(values())
            .collect(Collectors.toUnmodifiableMap(ResponseCode::getCode, e -> e));

    /**
     * 根據業務碼查找對應的 ResponseCode
     * 找不到時返回 INTERNAL_SERVER_ERROR
     */
    public static ResponseCode fromCode(int code) {
        return CODE_MAP.getOrDefault(code, INTERNAL_SERVER_ERROR);
    }
}
