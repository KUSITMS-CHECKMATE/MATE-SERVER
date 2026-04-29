package server.MATE.domain.cardsorting.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.MATE.domain.cardsorting.entity.CardSorting;

public interface CardSortingRepository extends JpaRepository<CardSorting, Long> {

}
