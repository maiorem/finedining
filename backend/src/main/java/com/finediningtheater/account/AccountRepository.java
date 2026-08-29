package com.finediningtheater.account;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {

    // 식별은 email이 아니라 provider+providerUserId다(§7.4) — 카카오 이메일은 null일 수 있다.
    Optional<Account> findByProviderAndProviderUserId(String provider, String providerUserId);
}
