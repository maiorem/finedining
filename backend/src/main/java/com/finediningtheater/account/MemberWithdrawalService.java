package com.finediningtheater.account;

import com.finediningtheater.global.error.BusinessException;
import com.finediningtheater.global.error.ErrorCode;
import com.finediningtheater.review.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 탈퇴(CLAUDE.md §3.2). 계정 행은 남기고 개인정보만 지운다 — 작성한 글은 삭제하지 않고
 * "탈퇴한 회원" 글로 남는다. 글에 붙은 이름·연락처(개인정보처리방침에서 탈퇴 시 파기를 약속한 값)도
 * 같은 트랜잭션에서 지운다.
 */
@Service
@RequiredArgsConstructor
public class MemberWithdrawalService {

    private final AccountRepository accountRepository;
    private final ReviewService reviewService;

    @Transactional
    public void withdraw(Long accountId) {
        Account account =
                accountRepository.findById(accountId).orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
        if (!account.isActive()) {
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION);
        }
        account.withdraw();
        reviewService.clearAuthorInfo(accountId);
    }
}
