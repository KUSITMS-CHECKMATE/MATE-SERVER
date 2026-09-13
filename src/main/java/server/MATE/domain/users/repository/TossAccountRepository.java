package server.MATE.domain.users.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import server.MATE.domain.users.entity.TossAccount;
import server.MATE.domain.users.entity.Users;

public interface TossAccountRepository extends JpaRepository<TossAccount, Long> {

    Optional<TossAccount> findByUser(Users user);

    Optional<TossAccount> findByUserId(Long userId);

    Optional<TossAccount> findByTossUserKey(Long tossUserKey);

    @Query("select t.tossUserKey from TossAccount t where t.isLinked = true")
    List<Long> findTossUserKeysByIsLinkedTrue();
}
