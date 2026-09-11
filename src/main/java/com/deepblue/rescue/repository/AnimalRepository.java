package com.deepblue.rescue.repository;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.RescueStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AnimalRepository extends JpaRepository<Animal, Long> {
    Optional<Animal> findByAnimalCode(String animalCode);
    List<Animal> findByCommonNameContainingIgnoreCase(String text);
    List<Animal> findByRescueCase_Status(RescueStatus status);
    List<Animal> findByRescueCase_RescueCenter_Code(String centerCode);

    @org.springframework.data.jpa.repository.Query("""
        select distinct a
        from Animal a
        join a.treatments t
        join t.specialist s
        join s.expertiseAreas e
        where a.rescueCase.status = :status
          and lower(e.name) = lower(:expertiseName)
        """)
    List<Animal> findInStatusTreatedByExpertise(
        @org.springframework.data.repository.query.Param("status") RescueStatus status,
        @org.springframework.data.repository.query.Param("expertiseName") String expertiseName);
}
