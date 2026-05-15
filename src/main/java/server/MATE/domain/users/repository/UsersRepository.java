package server.MATE.domain.users.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.MATE.domain.users.entity.Users;

import java.util.Optional;

public interface UsersRepository extends JpaRepository<Users, Long> {

    Optional<Users> findByCi(String ci);
}
