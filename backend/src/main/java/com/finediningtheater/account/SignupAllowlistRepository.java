package com.finediningtheater.account;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SignupAllowlistRepository extends JpaRepository<SignupAllowlistEntry, Long> {

    boolean existsByProviderAndProviderUserId(String provider, String providerUserId);

    boolean existsByEmail(String email);
}
