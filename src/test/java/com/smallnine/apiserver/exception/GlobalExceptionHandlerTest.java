package com.smallnine.apiserver.exception;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.smallnine.apiserver.constants.enums.ResponseCode;
import com.smallnine.apiserver.dto.ApiResponse;
import com.smallnine.apiserver.logging.AuditLogger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * GlobalExceptionHandler 純單元測試。
 * 不起 Spring context、不碰 DB,直接 new handler 並用 Logback ListAppender 捕捉 log event。
 */
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private WebRequest request;
    private Logger handlerLogger;
    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setUp() {
        // AuditLogger 在 RuntimeException 路徑沒被呼叫,給個 mock 即可
        AuditLogger auditLogger = mock(AuditLogger.class);
        handler = new GlobalExceptionHandler(auditLogger);

        request = mock(WebRequest.class);
        when(request.getDescription(false)).thenReturn("uri=/api/users");

        // 把 ListAppender 掛到 handler 的 logger(@Slf4j 取的是 class 名稱的 logger)
        handlerLogger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        handlerLogger.addAppender(listAppender);
    }

    @AfterEach
    void tearDown() {
        handlerLogger.detachAppender(listAppender);
        listAppender.stop();
    }

    /**
     * 把所有捕捉到的 log event 的格式化訊息串起來,方便斷言。
     */
    private String capturedMessages() {
        StringBuilder sb = new StringBuilder();
        List<ILoggingEvent> events = listAppender.list;
        for (int i = 0; i < events.size(); i++) {
            sb.append(events.get(i).getFormattedMessage()).append('\n');
        }
        return sb.toString();
    }

    /**
     * 案例一:DataAccessException 子類(DuplicateKeyException)帶敏感 message。
     * log 訊息字串不可含敏感值,但要含類別名稱;throwable 仍要被傳給 logger(保留 stack trace)。
     */
    @Test
    void dataAccessException_doesNotLeakSensitiveMessage() {
        DataAccessException ex = new DuplicateKeyException(
                "ERROR: duplicate key value violates unique constraint \"users_email_key\" "
                        + "Detail: Key (email)=(foo@bar.com) already exists.");

        ResponseEntity<ApiResponse<Void>> response = handler.handleRuntimeException(ex, request);

        String logged = capturedMessages();
        // 敏感值不可出現在 log 訊息字串
        assertFalse(logged.contains("foo@bar.com"),
                "log 訊息字串不應包含被擋的 email");
        assertFalse(logged.contains("users_email_key"),
                "log 訊息字串不應包含表/約束名稱");
        assertFalse(logged.contains("already exists"),
                "log 訊息字串不應包含 DB 原始錯誤片段");
        // 類別名稱要有,方便排查
        assertTrue(logged.contains(DuplicateKeyException.class.getName()),
                "log 訊息字串應包含例外類別名稱");

        // throwable 仍要被附在 log event 上(stack trace 物件,不是訊息字串)
        ILoggingEvent event = listAppender.list.get(0);
        assertEquals(Level.ERROR, event.getLevel());
        assertNotNull(event.getThrowableProxy(), "應保留 throwable 以記錄 stack trace");

        // response 維持泛用 500 信封,不洩漏細節
        assertGeneric500(response);
    }

    /**
     * 案例二(對照組):一般 RuntimeException 維持原行為,message 照印。
     */
    @Test
    void plainRuntimeException_keepsOriginalBehavior() {
        RuntimeException ex = new RuntimeException("一般運行時錯誤");

        ResponseEntity<ApiResponse<Void>> response = handler.handleRuntimeException(ex, request);

        String logged = capturedMessages();
        assertTrue(logged.contains("一般運行時錯誤"),
                "一般 RuntimeException 應維持原本把 message 印出的行為");

        ILoggingEvent event = listAppender.list.get(0);
        assertEquals(Level.ERROR, event.getLevel());
        assertNotNull(event.getThrowableProxy());

        assertGeneric500(response);
    }

    /**
     * 案例三:即使是另一個 DataAccessException 子類(自訂帶敏感 SQL),也不洩漏。
     */
    @Test
    void otherDataAccessExceptionSubclass_doesNotLeak() {
        DataAccessException ex = new DataAccessException(
                "could not execute statement; SQL [insert into orders(...)]; value=secret123") {
        };

        ResponseEntity<ApiResponse<Void>> response = handler.handleRuntimeException(ex, request);

        String logged = capturedMessages();
        assertFalse(logged.contains("secret123"), "不應洩漏 SQL 內的值");
        assertFalse(logged.contains("insert into orders"), "不應洩漏 SQL 語句");

        assertGeneric500(response);
    }

    /**
     * 共用斷言:回應為泛用 500 信封,不含任何細節。
     */
    private void assertGeneric500(ResponseEntity<ApiResponse<Void>> response) {
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        ApiResponse<Void> body = response.getBody();
        assertNotNull(body);
        assertEquals(ResponseCode.INTERNAL_SERVER_ERROR.getCode(), body.getCode());
        assertEquals("系統處理異常,請稍後重試", body.getMessage());
        assertFalse(body.isSuccess());
        assertNull(body.getData());
    }
}