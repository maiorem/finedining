package com.finediningtheater.account;

/** ACTIVE | WITHDRAWN 최소 구성이다(CLAUDE.md §3.1). 탈퇴는 물리 삭제가 아니라 상태 전환이다
 * — Review.account_id 같은 참조가 끊기지 않게 하기 위해서다(§3.2). */
public enum AccountStatus {
    ACTIVE,
    WITHDRAWN
}
