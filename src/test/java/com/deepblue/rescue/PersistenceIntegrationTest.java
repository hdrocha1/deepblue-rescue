package com.deepblue.rescue;

import com.deepblue.rescue.domain.*;
import com.deepblue.rescue.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest
@Transactional
class PersistenceIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres =
        new PostgreSQLContainer("postgres:18-alpine")
            .withDatabaseName("deepblue_test")
            .withUsername("deepblue")
            .withPassword("deepblue");

    @Autowired
    RescueCenterRepository rescueCenterRepository;

    @Autowired
    RescueCaseRepository rescueCaseRepository;

    @Autowired
    AnimalRepository animalRepository;

    @Autowired
    MedicalRecordRepository medicalRecordRepository;

    @Autowired
    SpecialistRepository specialistRepository;

    @Autowired
    ExpertiseRepository expertiseRepository;

    @Autowired
    TreatmentRepository treatmentRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    

    @Test
    void inheritedRepositoryMethods() {
        RescueCenter center = rescueCenterRepository.save(
            new RescueCenter(
                "DB-CAR",
                "DeepBlue Caribbean Center",
                "Santa Marta"
            )
        );

        assertNotNull(center.getId());
        assertTrue(
            rescueCenterRepository.findById(center.getId()).isPresent()
        );
        assertTrue(
            rescueCenterRepository.existsById(center.getId())
        );
        assertEquals(1, rescueCenterRepository.count());
    }

    @Test
    void oneToManyCenterCases() {
        RescueCenter center = rescueCenterRepository.save(
            new RescueCenter(
                "DB-CAR",
                "DeepBlue Caribbean Center",
                "Santa Marta"
            )
        );

        RescueCase c1 = new RescueCase(
            "RES-001",
            LocalDate.of(2026, 8, 1),
            "Bahia Concha",
            RescueStatus.ADMITTED
        );

        RescueCase c2 = new RescueCase(
            "RES-002",
            LocalDate.of(2026, 8, 2),
            "Taganga",
            RescueStatus.UNDER_EVALUATION
        );

        c1.setRescueCenter(center);
        c2.setRescueCenter(center);

        rescueCaseRepository.saveAllAndFlush(
            List.of(c1, c2)
        );

        List<RescueCase> cases =
            rescueCaseRepository.findByRescueCenter_Code("DB-CAR");

        assertEquals(2, cases.size());

        assertTrue(
            cases.stream()
                .allMatch(c ->
                    c.getRescueCenter().getCode().equals("DB-CAR"))
        );
    }

    @Test
    void rescueCaseOneToOneAnimal() {
        RescueCenter center = rescueCenterRepository.save(
            new RescueCenter(
                "DB-CAR",
                "DeepBlue Caribbean Center",
                "Santa Marta"
            )
        );

        RescueCase rescueCase = new RescueCase(
            "RES-2026-001",
            LocalDate.of(2026, 8, 1),
            "Bahia Concha",
            RescueStatus.ADMITTED
        );

        rescueCase.setRescueCenter(center);
        rescueCaseRepository.saveAndFlush(rescueCase);

        Animal animal = new Animal(
            "AN-2026-001",
            "Green Sea Turtle",
            "Chelonia mydas",
            AnimalSex.UNKNOWN
        );

        rescueCase.assignAnimal(animal);

        animalRepository.saveAndFlush(animal);

        assertNotNull(rescueCase.getAnimal());
        assertNotNull(animal.getRescueCase());
        assertEquals(
            "AN-2026-001",
            rescueCase.getAnimal().getAnimalCode()
        );
    }

    @Test
    void animalOneToOneMedicalRecordWithCascade() {
        RescueCenter center = rescueCenterRepository.save(
            new RescueCenter(
                "DB-CAR-2",
                "DeepBlue Caribbean Center",
                "Santa Marta"
            )
        );

        RescueCase rescueCase = new RescueCase(
            "RES-2026-002",
            LocalDate.of(2026, 8, 2),
            "Playa Grande",
            RescueStatus.ADMITTED
        );

        rescueCase.setRescueCenter(center);
        rescueCaseRepository.saveAndFlush(rescueCase);

        Animal animal = new Animal(
            "AN-2026-002",
            "Green Sea Turtle",
            "Chelonia mydas",
            AnimalSex.UNKNOWN
        );

        rescueCase.assignAnimal(animal);
        animalRepository.saveAndFlush(animal);

        MedicalRecord record = new MedicalRecord(
            new BigDecimal("28.40"),
            "STABLE",
            "Left front flipper injury",
            null
        );

        animal.assignMedicalRecord(record);

        // Aquí se demuestra el cascade Animal -> MedicalRecord
        animalRepository.saveAndFlush(animal);

        Integer animalCount = jdbcTemplate.queryForObject(
            "select count(*) from animals where animal_code = ?",
            Integer.class,
            "AN-2026-002"
        );

        Integer medicalRecordCount = jdbcTemplate.queryForObject(
            "select count(*) from medical_records mr " +
            "join animals a on a.id = mr.animal_id " +
            "where a.animal_code = ?",
            Integer.class,
            "AN-2026-002"
        );

        assertEquals(1, animalCount);
        assertEquals(1, medicalRecordCount);
        assertEquals(animal, record.getAnimal());
    }

    @Test
    void manyToManySpecialistExpertise() {
        Expertise trauma =
            expertiseRepository.findByNameIgnoreCase("Trauma")
                .orElseThrow();

        Expertise rehab =
            expertiseRepository.findByNameIgnoreCase("Rehabilitation")
                .orElseThrow();

        Specialist elena = specialistRepository.save(
            new Specialist(
                "SPEC-001",
                "Elena",
                "Vargas",
                "elena@deepblue.org",
                true
            )
        );

        elena.addExpertise(trauma);
        elena.addExpertise(rehab);

        specialistRepository.saveAndFlush(elena);

        Specialist reloaded =
            specialistRepository.findById(elena.getId())
                .orElseThrow();

        assertEquals(2, reloaded.getExpertiseAreas().size());
    }

    @Test
    void queryMethodByStatus() {
        RescueCenter center = rescueCenterRepository.save(
            new RescueCenter(
                "DB-CAR",
                "Caribbean",
                "Santa Marta"
            )
        );

        RescueCase c1 = new RescueCase(
            "RES-001",
            LocalDate.of(2026, 8, 1),
            "A",
            RescueStatus.IN_REHABILITATION
        );

        RescueCase c2 = new RescueCase(
            "RES-002",
            LocalDate.of(2026, 8, 2),
            "B",
            RescueStatus.READY_FOR_RELEASE
        );

        RescueCase c3 = new RescueCase(
            "RES-003",
            LocalDate.of(2026, 8, 3),
            "C",
            RescueStatus.IN_REHABILITATION
        );

        c1.setRescueCenter(center);
        c2.setRescueCenter(center);
        c3.setRescueCenter(center);

        rescueCaseRepository.saveAllAndFlush(
            List.of(c1, c2, c3)
        );

        assertEquals(
            2,
            rescueCaseRepository.findByStatusOrderByRescueDateAsc(
                RescueStatus.IN_REHABILITATION
            ).size()
        );
    }

    @Test
    void queryMethodAcrossRelations() {
        RescueCenter car = rescueCenterRepository.save(
            new RescueCenter(
                "DB-CAR",
                "Caribbean",
                "Santa Marta"
            )
        );

        RescueCenter pac = rescueCenterRepository.save(
            new RescueCenter(
                "DB-PAC",
                "Pacific",
                "Buenaventura"
            )
        );

        RescueCase carCase = new RescueCase(
            "CAR-001",
            LocalDate.of(2026, 8, 1),
            "A",
            RescueStatus.IN_REHABILITATION
        );

        RescueCase pacCase = new RescueCase(
            "PAC-001",
            LocalDate.of(2026, 8, 1),
            "B",
            RescueStatus.IN_REHABILITATION
        );

        carCase.setRescueCenter(car);
        pacCase.setRescueCenter(pac);

        rescueCaseRepository.saveAllAndFlush(
            List.of(carCase, pacCase)
        );

        Animal a1 = new Animal(
            "AN-CAR",
            "Green Sea Turtle",
            "Chelonia mydas",
            AnimalSex.FEMALE
        );

        Animal a2 = new Animal(
            "AN-PAC",
            "Sea Lion",
            "Zalophus californianus",
            AnimalSex.MALE
        );

        carCase.assignAnimal(a1);
        pacCase.assignAnimal(a2);

        animalRepository.saveAndFlush(a1);
        animalRepository.saveAndFlush(a2);

        List<Animal> result =
            animalRepository.findByRescueCase_RescueCenter_Code(
                "DB-CAR"
            );

        assertEquals(1, result.size());
        assertEquals(
            "AN-CAR",
            result.get(0).getAnimalCode()
        );
    }

    @Test
    void specialistJpqlByExpertise() {
        Expertise trauma =
            expertiseRepository.findByNameIgnoreCase("Trauma")
                .orElseThrow();

        Expertise rehab =
            expertiseRepository.findByNameIgnoreCase("Rehabilitation")
                .orElseThrow();

        Expertise mammals =
            expertiseRepository.findByNameIgnoreCase("Marine Mammals")
                .orElseThrow();

        Expertise birds =
            expertiseRepository.findByNameIgnoreCase("Marine Birds")
                .orElseThrow();

        Specialist elena = new Specialist(
            "SPEC-001",
            "Elena",
            "Vargas",
            "elena@deepblue.org",
            true
        );

        elena.addExpertise(trauma);
        elena.addExpertise(rehab);

        Specialist mateo = new Specialist(
            "SPEC-002",
            "Mateo",
            "Rios",
            "mateo@deepblue.org",
            true
        );

        mateo.addExpertise(mammals);
        mateo.addExpertise(rehab);

        Specialist sofia = new Specialist(
            "SPEC-003",
            "Sofia",
            "Diaz",
            "sofia@deepblue.org",
            true
        );

        sofia.addExpertise(birds);
        sofia.addExpertise(trauma);

        specialistRepository.saveAll(
            List.of(elena, mateo, sofia)
        );

        specialistRepository.flush();

        List<Specialist> result =
            specialistRepository.findActiveByExpertise("TRAUMA");

        assertEquals(2, result.size());

        assertEquals(
            List.of("Sofia", "Elena"),
            result.stream()
                .map(Specialist::getFirstName)
                .toList()
        );
    }

    @Test
    void treatmentQueryMethodsAndJpql() {
        RescueCenter center = rescueCenterRepository.save(
            new RescueCenter(
                "DB-CAR",
                "Caribbean",
                "Santa Marta"
            )
        );

        RescueCase rc = new RescueCase(
            "RES-T-001",
            LocalDate.of(2026, 8, 1),
            "A",
            RescueStatus.IN_REHABILITATION
        );

        rc.setRescueCenter(center);
        rescueCaseRepository.saveAndFlush(rc);

        Animal animal = new Animal(
            "AN-T-001",
            "Green Sea Turtle",
            "Chelonia mydas",
            AnimalSex.FEMALE
        );

        rc.assignAnimal(animal);
        animalRepository.saveAndFlush(animal);

        Expertise trauma =
            expertiseRepository.findByNameIgnoreCase("Trauma")
                .orElseThrow();

        Expertise rehab =
            expertiseRepository.findByNameIgnoreCase("Rehabilitation")
                .orElseThrow();

        Specialist elena = new Specialist(
            "SPEC-T-001",
            "Elena",
            "Vargas",
            "elena.t@deepblue.org",
            true
        );

        elena.addExpertise(trauma);
        elena.addExpertise(rehab);

        specialistRepository.saveAndFlush(elena);

        Treatment t1 = new Treatment(
            LocalDateTime.of(2026, 8, 1, 10, 0),
            TreatmentType.WOUND_CARE,
            "Cleaning of left front flipper"
        );

        t1.setAnimal(animal);
        t1.setSpecialist(elena);

        Treatment t2 = new Treatment(
            LocalDateTime.of(2026, 8, 10, 10, 0),
            TreatmentType.HYDRATION,
            "Subcutaneous fluid therapy"
        );

        t2.setAnimal(animal);
        t2.setSpecialist(elena);

        Treatment t3 = new Treatment(
            LocalDateTime.of(2026, 8, 20, 10, 0),
            TreatmentType.OBSERVATION,
            "General observation"
        );

        t3.setAnimal(animal);
        t3.setSpecialist(elena);

        treatmentRepository.saveAllAndFlush(
            List.of(t1, t2, t3)
        );

        assertEquals(
            3,
            treatmentRepository
                .findByAnimal_IdOrderByPerformedAtAsc(
                    animal.getId()
                )
                .size()
        );

        assertEquals(
            1,
            treatmentRepository.findPerformedBetween(
                LocalDateTime.of(2026, 8, 5, 0, 0),
                LocalDateTime.of(2026, 8, 15, 23, 59)
            ).size()
        );

        assertEquals(
            3,
            treatmentRepository
                .findByAnimalRescueCenter("DB-CAR")
                .size()
        );

        assertEquals(
            3,
            treatmentRepository
                .findBySpecialistExpertise("rehabilitation")
                .size()
        );
    }

    @Test
    void uniqueConstraintThrowsDataIntegrityViolation() {
        RescueCenter center = rescueCenterRepository.save(
            new RescueCenter(
                "DB-UNIQUE",
                "DeepBlue Test",
                "Santa Marta"
            )
        );

        RescueCase rescueCase = new RescueCase(
            "RES-UNIQUE",
            LocalDate.of(2026, 8, 1),
            "Test",
            RescueStatus.ADMITTED
        );

        rescueCase.setRescueCenter(center);
        rescueCaseRepository.saveAndFlush(rescueCase);

        Animal animal1 = new Animal(
            "AN-100",
            "Turtle",
            "Chelonia mydas",
            AnimalSex.UNKNOWN
        );

        rescueCase.assignAnimal(animal1);
        animalRepository.saveAndFlush(animal1);

        RescueCase rescueCase2 = new RescueCase(
            "RES-UNIQUE-2",
            LocalDate.of(2026, 8, 2),
            "Test",
            RescueStatus.ADMITTED
        );

        rescueCase2.setRescueCenter(center);
        rescueCaseRepository.saveAndFlush(rescueCase2);

        Animal animal2 = new Animal(
            "AN-100",
            "Another Turtle",
            "Chelonia mydas",
            AnimalSex.UNKNOWN
        );

        rescueCase2.assignAnimal(animal2);

        assertThrows(
            DataIntegrityViolationException.class,
            () -> animalRepository.saveAndFlush(animal2)
        );
    }

    @Test
    void integratorScenario() {
        RescueCenter center = rescueCenterRepository.save(
            new RescueCenter(
                "DB-CAR",
                "DeepBlue Caribbean",
                "Santa Marta"
            )
        );

        RescueCase rc = new RescueCase(
            "RES-2026-100",
            LocalDate.of(2026, 8, 18),
            "Bahía Concha",
            RescueStatus.IN_REHABILITATION
        );

        rc.setRescueCenter(center);
        rescueCaseRepository.saveAndFlush(rc);

        Animal animal = new Animal(
            "AN-2026-100",
            "Green Sea Turtle",
            "Chelonia mydas",
            AnimalSex.FEMALE
        );

        rc.assignAnimal(animal);
        animal.setTrackingDeviceCode("GPS-100");

        animalRepository.saveAndFlush(animal);

        MedicalRecord record = new MedicalRecord(
            new BigDecimal("27.80"),
            "STABLE",
            "Injury caused by fishing net",
            "Possible plastic ingestion"
        );

        animal.assignMedicalRecord(record);
        animalRepository.saveAndFlush(animal);

        Expertise reptiles =
            expertiseRepository.findByNameIgnoreCase("Marine Reptiles")
                .orElseThrow();

        Expertise trauma =
            expertiseRepository.findByNameIgnoreCase("Trauma")
                .orElseThrow();

        Expertise rehab =
            expertiseRepository.findByNameIgnoreCase("Rehabilitation")
                .orElseThrow();

        Specialist elena = new Specialist(
            "SPEC-001",
            "Elena",
            "Vargas",
            "elena@deepblue.org",
            true
        );

        elena.addExpertise(reptiles);
        elena.addExpertise(trauma);
        elena.addExpertise(rehab);

        specialistRepository.saveAndFlush(elena);

        Treatment t1 = new Treatment(
            LocalDateTime.of(2026, 8, 18, 10, 0),
            TreatmentType.WOUND_CARE,
            "Cleaning of left front flipper"
        );

        t1.setAnimal(animal);
        t1.setSpecialist(elena);

        Treatment t2 = new Treatment(
            LocalDateTime.of(2026, 8, 19, 10, 0),
            TreatmentType.HYDRATION,
            "Subcutaneous fluid therapy"
        );

        t2.setAnimal(animal);
        t2.setSpecialist(elena);

        treatmentRepository.saveAllAndFlush(
            List.of(t1, t2)
        );

        assertTrue(
            rescueCaseRepository
                .findByCaseCode("RES-2026-100")
                .isPresent()
        );

        assertEquals(
            1,
            rescueCaseRepository
                .findByStatusOrderByRescueDateAsc(
                    RescueStatus.IN_REHABILITATION
                )
                .size()
        );

        assertEquals(
            1,
            animalRepository
                .findByRescueCase_RescueCenter_Code("DB-CAR")
                .size()
        );

        assertEquals(
            1,
            animalRepository
                .findByCommonNameContainingIgnoreCase("turtle")
                .size()
        );

        assertEquals(
            1,
            specialistRepository
                .findActiveByExpertise("Trauma")
                .size()
        );

        assertEquals(
            2,
            treatmentRepository
                .findByAnimal_IdOrderByPerformedAtAsc(
                    animal.getId()
                )
                .size()
        );

        assertEquals(
            2,
            treatmentRepository
                .findBySpecialistExpertise("Rehabilitation")
                .size()
        );

        List<Animal> finalChallenge =
            animalRepository.findInStatusTreatedByExpertise(
                RescueStatus.IN_REHABILITATION,
                "trauma"
            );

        assertEquals(1, finalChallenge.size());

        assertEquals(
            "AN-2026-100",
            finalChallenge.get(0).getAnimalCode()
        );
    }

    @Test
    void invalidStatusIsRejectedByDatabase() {
        jdbcTemplate.update("""
            insert into rescue_centers(code, name, city)
            values ('BAD', 'Bad Center', 'Santa Marta')
            """);

        Long centerId = jdbcTemplate.queryForObject(
            "select id from rescue_centers where code='BAD'",
            Long.class
        );

        assertNotNull(centerId);

        assertThrows(DataIntegrityViolationException.class, () ->
            jdbcTemplate.update("""
                insert into rescue_cases(
                    case_code,
                    rescue_date,
                    rescue_location,
                    status,
                    rescue_center_id
                ) values (?, ?, ?, ?, ?)
                """,
                "BAD-CASE",
                LocalDate.now(),
                "Nowhere",
                "INVALID_STATUS",
                centerId
            )
        );
    }
}