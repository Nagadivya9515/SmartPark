package com.parking.operator.repository;

import com.parking.operator.models.Operator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface OperatorRepository extends JpaRepository<Operator, Long> {
    Optional<Operator> findByOperatorId(String operatorId);
    boolean existsByOperatorId(String operatorId);

    @Query("SELECT COUNT(o) FROM Operator o WHERE o.active = true")
    long countActive();
}