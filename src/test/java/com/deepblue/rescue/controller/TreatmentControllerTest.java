package com.deepblue.rescue.controller;

import com.deepblue.rescue.domain.TreatmentType;
import com.deepblue.rescue.dto.request.CreateTreatmentRequest;
import com.deepblue.rescue.dto.response.TreatmentResponse;
import com.deepblue.rescue.exception.BusinessRuleException;
import com.deepblue.rescue.exception.GlobalExceptionHandler;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.service.TreatmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TreatmentController.class)
@Import({GlobalExceptionHandler.class, ValidationAutoConfiguration.class})
class TreatmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TreatmentService service;

    private TreatmentResponse response() {
        return new TreatmentResponse(
                100L,
                "AN-001",
                "SPEC-001",
                LocalDateTime.of(2026, 8, 21, 9, 0),
                TreatmentType.WOUND_CARE,
                "Cleaning and treatment of flipper injury."
        );
    }

    @Test
    void shouldCreateTreatment() throws Exception {
        when(service.register(any(CreateTreatmentRequest.class))).thenReturn(response());

        mockMvc.perform(post("/api/treatments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "animalCode": "AN-001",
                                  "specialistCode": "SPEC-001",
                                  "performedAt": "2026-08-21T09:00:00",
                                  "type": "WOUND_CARE",
                                  "description": "Cleaning and treatment of flipper injury."
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.animalCode").value("AN-001"))
                .andExpect(jsonPath("$.specialistCode").value("SPEC-001"))
                .andExpect(jsonPath("$.type").value("WOUND_CARE"));

        verify(service).register(any(CreateTreatmentRequest.class));
    }

    @Test
    void shouldReturn400WhenTreatmentRequestIsInvalid() throws Exception {
        mockMvc.perform(post("/api/treatments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "animalCode": "",
                                  "specialistCode": "",
                                  "type": null,
                                  "description": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.details.animalCode").value("Animal code is required"))
                .andExpect(jsonPath("$.details.specialistCode").value("Specialist code is required"))
                .andExpect(jsonPath("$.details.type").value("Treatment type is required"))
                .andExpect(jsonPath("$.details.description").value("Description is required"));

        verify(service, never()).register(any());
    }

    @Test
    void shouldReturn404WhenAnimalDoesNotExist() throws Exception {
        when(service.register(any(CreateTreatmentRequest.class)))
                .thenThrow(new ResourceNotFoundException("Animal not found: AN-999"));

        mockMvc.perform(post("/api/treatments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "animalCode": "AN-999",
                                  "specialistCode": "SPEC-001",
                                  "performedAt": "2026-08-21T09:00:00",
                                  "type": "WOUND_CARE",
                                  "description": "Cleaning and treatment of flipper injury."
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Animal not found: AN-999"));
    }

    @Test
    void shouldReturn409WhenBusinessRuleFails() throws Exception {
        when(service.register(any(CreateTreatmentRequest.class)))
                .thenThrow(new BusinessRuleException("Released animals cannot receive treatments"));

        mockMvc.perform(post("/api/treatments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "animalCode": "AN-001",
                                  "specialistCode": "SPEC-001",
                                  "performedAt": "2026-08-21T09:00:00",
                                  "type": "WOUND_CARE",
                                  "description": "Cleaning and treatment of flipper injury."
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Released animals cannot receive treatments"));
    }

    @Test
    void shouldReturn400WhenTreatmentJsonIsInvalid() throws Exception {
        mockMvc.perform(post("/api/treatments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "animalCode": "AN-001",
                                  "specialistCode": "SPEC-001",
                                  "performedAt": "2026-08-21T09:00:00",
                                  "type": "INVALID_TYPE",
                                  "description": "Cleaning and treatment of flipper injury."
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Malformed or invalid JSON request"))
                .andExpect(jsonPath("$.details.body").exists());

        verify(service, never()).register(any());
    }
}
