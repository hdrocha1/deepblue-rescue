package com.deepblue.rescue.service;

import com.deepblue.rescue.domain.*;
import com.deepblue.rescue.mapper.AnimalMapper;
import com.deepblue.rescue.repository.AnimalRepository;
import com.deepblue.rescue.service.impl.AnimalServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnimalServiceImplTest {
    @Mock AnimalRepository repository;
    @Mock AnimalMapper mapper;
    @InjectMocks AnimalServiceImpl service;

    @Test
    void canReceiveTreatmentOnlyInAllowedStatuses() {
        RescueCase rc = new RescueCase("RES-001", LocalDate.of(2026,8,20), "Santa Marta", RescueStatus.IN_REHABILITATION);
        Animal animal = new Animal("AN-001", "Turtle", "Chelonia mydas", AnimalSex.FEMALE);
        rc.assignAnimal(animal);
        when(repository.findByAnimalCode("AN-001")).thenReturn(Optional.of(animal));
        assertThat(service.canReceiveTreatment("AN-001")).isTrue();
    }
}
