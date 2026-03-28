package org.ciyex.ehr.careplan.repository;

import org.ciyex.ehr.careplan.entity.CarePlanGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CarePlanGoalRepository extends JpaRepository<CarePlanGoal, Long> {

    List<CarePlanGoal> findByCarePlanIdOrderByCreatedAtDesc(Long carePlanId);
}
