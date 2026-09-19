package com.finediningtheater.inquiry;

/**
 * 협업 제안의 분류. 처음 세 개는 2026-09-04(홈페이지 구성 요청사항 §6), 나머지 세 개는 2026-09-14
 * 요청서에서 "추가"됐다 — 운영자가 기존 분류를 그대로 두고 총 6개로 하기로 했다(2026-09-19).
 * DB에는 이름 문자열로 저장되고 컬럼이 VARCHAR(30)이라 이름은 30자를 넘기지 않는다.
 */
public enum ProposalCategory {
    CORPORATE_EVENT,
    LOCAL_CULTURE,
    CUSTOM_CONSULTING,
    PERFORMANCE_INQUIRY,
    CONTENT_FOOD_COLLAB,
    OTHER
}
