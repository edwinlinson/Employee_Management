package com.example.employee.controller;

import com.example.employee.dto.EmployeeRequest;
import com.example.employee.dto.EmployeeResponse;
import com.example.employee.dto.PageResponse;
import com.example.employee.entity.EmployeeStatus;
import com.example.employee.exception.DuplicateResourceException;
import com.example.employee.exception.ResourceNotFoundException;
import com.example.employee.service.EmployeeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EmployeeController.class)
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EmployeeService service;

    private EmployeeRequest validRequest() {
        return new EmployeeRequest("Asha", "Menon", "asha@example.com", "+919876543210", "Engineering",
                "Software Engineer", new BigDecimal("85000.00"), LocalDate.of(2024, 6, 15), EmployeeStatus.ACTIVE);
    }

    private EmployeeResponse response(long id) {
        return new EmployeeResponse(id, "Asha", "Menon", "asha@example.com", "+919876543210", "Engineering",
                "Software Engineer", new BigDecimal("85000.00"), LocalDate.of(2024, 6, 15),
                EmployeeStatus.ACTIVE, Instant.now(), Instant.now());
    }

    @Test
    void create_returns201WithLocation() throws Exception {
        when(service.create(any(EmployeeRequest.class))).thenReturn(response(1));

        mockMvc.perform(post("/api/v1/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/v1/employees/1")))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("asha@example.com"));
    }

    @Test
    void create_returns400WithFieldErrors_whenInvalid() throws Exception {
        EmployeeRequest invalid = new EmployeeRequest("", "Menon", "not-an-email", "abc", "Engineering",
                "Engineer", new BigDecimal("-5"), LocalDate.now().plusDays(3), null);

        mockMvc.perform(post("/api/v1/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.firstName").exists())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.phone").exists())
                .andExpect(jsonPath("$.fieldErrors.salary").exists())
                .andExpect(jsonPath("$.fieldErrors.dateOfJoining").exists());
    }

    @Test
    void create_returns400_whenEnumValueInvalid() throws Exception {
        String body = """
                {"firstName":"A","lastName":"B","email":"a@b.com","department":"HR","designation":"Mgr",
                 "salary":100,"dateOfJoining":"2024-01-01","status":"RETIRED"}""";

        mockMvc.perform(post("/api/v1/employees").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_returns409_whenEmailDuplicate() throws Exception {
        when(service.create(any(EmployeeRequest.class)))
                .thenThrow(new DuplicateResourceException("Employee already exists with email: asha@example.com"));

        mockMvc.perform(post("/api/v1/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void getAll_returnsPage() throws Exception {
        when(service.getAll(eq("asha"), eq(EmployeeStatus.ACTIVE), any()))
                .thenReturn(new PageResponse<>(List.of(response(1)), 0, 10, 1, 1, true, true));

        mockMvc.perform(get("/api/v1/employees").param("search", "asha").param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getAll_returns400_whenStatusInvalid() throws Exception {
        mockMvc.perform(get("/api/v1/employees").param("status", "BOGUS"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getById_returnsEmployee() throws Exception {
        when(service.getById(1L)).thenReturn(response(1));

        mockMvc.perform(get("/api/v1/employees/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Asha"));
    }

    @Test
    void getById_returns404_whenMissing() throws Exception {
        when(service.getById(99L)).thenThrow(new ResourceNotFoundException("Employee", 99L));

        mockMvc.perform(get("/api/v1/employees/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Employee not found with id: 99"))
                .andExpect(jsonPath("$.path").value("/api/v1/employees/99"));
    }

    @Test
    void getById_returns400_whenIdNotNumeric() throws Exception {
        mockMvc.perform(get("/api/v1/employees/abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_returns200() throws Exception {
        when(service.update(eq(1L), any(EmployeeRequest.class))).thenReturn(response(1));

        mockMvc.perform(put("/api/v1/employees/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void delete_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/employees/1")).andExpect(status().isNoContent());
    }

    @Test
    void delete_returns404_whenMissing() throws Exception {
        doThrow(new ResourceNotFoundException("Employee", 7L)).when(service).delete(7L);

        mockMvc.perform(delete("/api/v1/employees/7")).andExpect(status().isNotFound());
    }
}
