package com.finediningtheater.account;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminAccountRepository extends JpaRepository<AdminAccount, Long> {

    Optional<AdminAccount> findByUsernameAndStatus(String username, AdminAccountStatus status);
}
