package com.finediningtheater.global.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 쓰기 엔드포인트가 호출하는 감사 로그 기록 창구. before/after는 뭐든 JSON으로 직렬화한다. */
@Component
@RequiredArgsConstructor
public class AuditLogger {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void record(
            Long accountId, String action, String targetType, Long targetId, Object before, Object after, String ip) {
        auditLogRepository.save(
                new AuditLog(accountId, action, targetType, targetId, toJson(before), toJson(after), ip));
    }

    private String toJson(Object value) {
        if (value == null) return null;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return String.valueOf(value);
        }
    }
}
