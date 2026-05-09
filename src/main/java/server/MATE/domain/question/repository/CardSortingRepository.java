package server.MATE.domain.question.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.MATE.domain.question.entity.CardSorting;

import java.util.List;

public interface CardSortingRepository extends JpaRepository<CardSorting, Long> {

    List<CardSorting> findAllByIdIn(Iterable<Long> ids);
}
