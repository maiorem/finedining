package com.finediningtheater.global.audit;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuditLoggerTest {

    @Mock private AuditLogRepository auditLogRepository;

    private AuditLogger logger() {
        return new AuditLogger(auditLogRepository, new ObjectMapper());
    }

    @Test
    void before_after를_JSON으로_직렬화해서_저장한다() {
        logger()
                .record(
                        1L,
                        "PRODUCTION_PUBLISH",
                        "Production",
                        10L,
                        Map.of("status", "DRAFT"),
                        Map.of("status", "PUBLISHED"),
                        "127.0.0.1");

        verify(auditLogRepository)
                .save(
                        argThat(
                                log ->
                                        log.getAccountId().equals(1L)
                                                && log.getAction().equals("PRODUCTION_PUBLISH")
                                                && log.getTargetType().equals("Production")
                                                && log.getTargetId().equals(10L)
                                                && log.getBeforeJson().contains("DRAFT")
                                                && log.getAfterJson().contains("PUBLISHED")
                                                && log.getIp().equals("127.0.0.1")));
    }

    @Test
    void before가_null이면_beforeJson도_null이다() {
        logger().record(1L, "PRODUCTION_CREATE", "Production", 10L, null, Map.of("slug", "showcase"), "127.0.0.1");

        verify(auditLogRepository)
                .save(argThat(log -> log.getBeforeJson() == null && log.getAfterJson().contains("showcase")));
    }
}
