package com.deepblue.rescue.service;

import com.deepblue.rescue.domain.*;
import com.deepblue.rescue.dto.request.CreateTreatmentRequest;
import com.deepblue.rescue.dto.response.TreatmentResponse;
import com.deepblue.rescue.exception.BusinessRuleException;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.mapper.TreatmentMapper;
import com.deepblue.rescue.repository.AnimalRepository;
import com.deepblue.rescue.repository.SpecialistRepository;
import com.deepblue.rescue.repository.TreatmentRepository;
import com.deepblue.rescue.service.impl.TreatmentServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TreatmentServiceImplTest {
    @Mock AnimalRepository animalRepository;
    @Mock SpecialistRepository specialistRepository;
    @Mock TreatmentRepository treatmentRepository;
    @Mock TreatmentMapper mapper;
    @InjectMocks TreatmentServiceImpl service;

    private Animal animal(RescueStatus status) {
        RescueCase rc = new RescueCase("RES-001", LocalDate.of(2026, 8, 20), "Santa Marta", status);
        Animal a = new Animal("AN-001", "Green Sea Turtle", "Chelonia mydas", AnimalSex.FEMALE);
        rc.assignAnimal(a);
        return a;
    }

    @Test
    void shouldRegisterTreatment() {
        Animal a = animal(RescueStatus.IN_REHABILITATION);
        Specialist s = new Specialist("SPEC-001", "Elena", "Vargas", "e@x.com", true);
        Treatment saved = new Treatment(LocalDateTime.of(2026,8,21,9,0), TreatmentType.WOUND_CARE, "Cleaning");
        TreatmentResponse response = new TreatmentResponse(1L, "AN-001", "SPEC-001", saved.getPerformedAt(), saved.getType(), saved.getDescription());
        when(animalRepository.findByAnimalCode("AN-001")).thenReturn(Optional.of(a));
        when(specialistRepository.findByProfessionalCode("SPEC-001")).thenReturn(Optional.of(s));
        when(treatmentRepository.save(any(Treatment.class))).thenReturn(saved);
        when(mapper.toResponse(saved)).thenReturn(response);
        service.register(new CreateTreatmentRequest("AN-001", "SPEC-001", saved.getPerformedAt(), saved.getType(), saved.getDescription()));
        verify(treatmentRepository).save(any(Treatment.class));
    }

    @Test
    void shouldRejectInactiveSpecialist() {
        Animal a = animal(RescueStatus.IN_REHABILITATION);
        Specialist s = new Specialist("SPEC-001", "Elena", "Vargas", "e@x.com", false);
        when(animalRepository.findByAnimalCode("AN-001")).thenReturn(Optional.of(a));
        when(specialistRepository.findByProfessionalCode("SPEC-001")).thenReturn(Optional.of(s));
        assertThatThrownBy(() -> service.register(new CreateTreatmentRequest("AN-001", "SPEC-001", LocalDateTime.of(2026,8,21,9,0), TreatmentType.WOUND_CARE, "Cleaning")))
                .isInstanceOf(BusinessRuleException.class);
        verify(treatmentRepository, never()).save(any());
    }

    @Test
    void shouldRejectReleasedCase() {
        Animal a = animal(RescueStatus.RELEASED);
        Specialist s = new Specialist("SPEC-001", "Elena", "Vargas", "e@x.com", true);
        when(animalRepository.findByAnimalCode("AN-001")).thenReturn(Optional.of(a));
        when(specialistRepository.findByProfessionalCode("SPEC-001")).thenReturn(Optional.of(s));
        assertThatThrownBy(() -> service.register(new CreateTreatmentRequest("AN-001", "SPEC-001", LocalDateTime.of(2026,8,21,9,0), TreatmentType.WOUND_CARE, "Cleaning")))
                .isInstanceOf(BusinessRuleException.class);
        verify(treatmentRepository, never()).save(any());
    }

    @Test
    void shouldRejectTreatmentBeforeRescueDate() {
        Animal a = animal(RescueStatus.IN_REHABILITATION);
        Specialist s = new Specialist("SPEC-001", "Elena", "Vargas", "e@x.com", true);
        when(animalRepository.findByAnimalCode("AN-001")).thenReturn(Optional.of(a));
        when(specialistRepository.findByProfessionalCode("SPEC-001")).thenReturn(Optional.of(s));
        assertThatThrownBy(() -> service.register(new CreateTreatmentRequest("AN-001", "SPEC-001", LocalDateTime.of(2026,8,15,9,0), TreatmentType.WOUND_CARE, "Cleaning")))
                .isInstanceOf(BusinessRuleException.class);
        verify(treatmentRepository, never()).save(any());
    }
}
