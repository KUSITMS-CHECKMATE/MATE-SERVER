package server.MATE.domain.users.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import server.MATE.domain.users.entity.TossAccount;
import server.MATE.domain.users.entity.Users;

public interface TossAccountRepository extends JpaRepository<TossAccount, Long> {

    Optional<TossAccount> findByUser(Users user);
}
