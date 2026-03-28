package org.ciyex.ehr.careplan.repository;

import org.ciyex.ehr.careplan.entity.CarePlanIntervention;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CarePlanInterventionRepository extends JpaRepository<CarePlanIntervention, Long> {

    List<CarePlanIntervention> findByCarePlanIdOrderByCreatedAtDesc(Long carePlanId);

    List<CarePlanIntervention> findByGoalIdOrderByCreatedAtDesc(Long goalId);
}
