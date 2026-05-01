package server.MATE.domain.question.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.MATE.domain.question.entity.CardSorting;

public interface CardSortingRepository extends JpaRepository<CardSorting, Long> {

}
