package com.deepblue.rescue.service;

import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.dto.request.ChangeRescueStatusRequest;
import com.deepblue.rescue.dto.response.RescueCaseResponse;
import com.deepblue.rescue.exception.BusinessRuleException;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.mapper.RescueCaseMapper;
import com.deepblue.rescue.repository.RescueCaseRepository;
import com.deepblue.rescue.service.impl.RescueCaseServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RescueCaseServiceImplTest {
    @Mock RescueCaseRepository repository;
    @Mock RescueCaseMapper mapper;
    @InjectMocks RescueCaseServiceImpl service;

    @Test
    void shouldFindRescueCaseByCode() {
        RescueCase entity = new RescueCase("RES-001", LocalDate.of(2026, 8, 20), "Santa Marta", RescueStatus.ADMITTED);
        RescueCaseResponse response = new RescueCaseResponse(1L, "RES-001", entity.getRescueDate(), entity.getRescueLocation(), entity.getStatus(), null, null);
        when(repository.findByCaseCode("RES-001")).thenReturn(Optional.of(entity));
        when(mapper.toResponse(entity)).thenReturn(response);
        assertThat(service.findByCode("RES-001")).isEqualTo(response);
        verify(repository).findByCaseCode("RES-001");
        verify(mapper).toResponse(entity);
    }

    @Test
    void shouldThrowWhenCaseDoesNotExist() {
        when(repository.findByCaseCode("RES-999")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findByCode("RES-999"))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(mapper, never()).toResponse(any());
    }

    @Test
    void shouldAllowValidTransition() {
        RescueCase entity = new RescueCase("RES-001", LocalDate.of(2026, 8, 20), "Santa Marta", RescueStatus.ADMITTED);
        RescueCaseResponse response = new RescueCaseResponse(1L, "RES-001", entity.getRescueDate(), entity.getRescueLocation(), RescueStatus.UNDER_EVALUATION, null, null);
        when(repository.findByCaseCode("RES-001")).thenReturn(Optional.of(entity));
        when(repository.save(entity)).thenReturn(entity);
        when(mapper.toResponse(entity)).thenReturn(response);
        RescueCaseResponse result = service.changeStatus("RES-001", new ChangeRescueStatusRequest(RescueStatus.UNDER_EVALUATION));
        assertThat(entity.getStatus()).isEqualTo(RescueStatus.UNDER_EVALUATION);
        assertThat(result).isEqualTo(response);
        verify(repository).save(entity);
    }

    @Test
    void shouldRejectInvalidTransitionAndNotSave() {
        RescueCase entity = new RescueCase("RES-001", LocalDate.of(2026, 8, 20), "Santa Marta", RescueStatus.ADMITTED);
        when(repository.findByCaseCode("RES-001")).thenReturn(Optional.of(entity));
        assertThatThrownBy(() -> service.changeStatus("RES-001", new ChangeRescueStatusRequest(RescueStatus.READY_FOR_RELEASE)))
                .isInstanceOf(BusinessRuleException.class);
        verify(repository, never()).save(any());
    }
}
