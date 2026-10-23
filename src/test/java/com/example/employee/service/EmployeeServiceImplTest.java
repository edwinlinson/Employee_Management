package com.example.employee.service;

import com.example.employee.dto.BulkEmployeeUpdateRequest;
import com.example.employee.dto.EmployeeRequest;
import com.example.employee.dto.EmployeeResponse;
import com.example.employee.dto.PageResponse;
import com.example.employee.entity.Employee;
import com.example.employee.entity.EmployeeStatus;
import com.example.employee.exception.BadRequestException;
import com.example.employee.exception.DuplicateResourceException;
import com.example.employee.exception.ResourceNotFoundException;
import com.example.employee.mapper.EmployeeMapper;
import com.example.employee.repository.EmployeeRepository;
import com.example.employee.service.impl.EmployeeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceImplTest {

    @Mock
    private EmployeeRepository repository;

    private EmployeeServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new EmployeeServiceImpl(repository, new EmployeeMapper());
    }

    private EmployeeRequest request(String email) {
        return new EmployeeRequest("Asha", "Menon", email, "+919876543210", "Engineering",
                "Software Engineer", new BigDecimal("85000.00"), LocalDate.of(2024, 6, 15), null);
    }

    private Employee entity(Long id, String email) {
        Employee e = new EmployeeMapper().toEntity(request(email));
        e.setId(id);
        return e;
    }

    @Test
    void create_savesEmployee_withNormalizedEmailAndDefaultStatus() {
        when(repository.existsByEmailIgnoreCase("asha@example.com")).thenReturn(false);
        when(repository.save(any(Employee.class))).thenAnswer(inv -> {
            Employee e = inv.getArgument(0);
            e.setId(1L);
            return e;
        });

        EmployeeResponse response = service.create(request("  Asha@Example.com "));

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("asha@example.com");
        assertThat(response.status()).isEqualTo(EmployeeStatus.ACTIVE);
    }

    @Test
    void create_throwsConflict_whenEmailExists() {
        when(repository.existsByEmailIgnoreCase("asha@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request("asha@example.com")))
                .isInstanceOf(DuplicateResourceException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void getById_returnsEmployee() {
        when(repository.findById(1L)).thenReturn(Optional.of(entity(1L, "asha@example.com")));

        assertThat(service.getById(1L).firstName()).isEqualTo("Asha");
    }

    @Test
    void getById_throwsNotFound_whenMissing() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void getAll_returnsPagedResponse() {
        PageRequest pageable = PageRequest.of(0, 10, Sort.by("id"));
        when(repository.findAll(ArgumentMatchers.<Specification<Employee>>any(), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entity(1L, "asha@example.com")), pageable, 1));

        PageResponse<EmployeeResponse> result = service.getAll("asha", EmployeeStatus.ACTIVE, pageable);

        assertThat(result.content()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.last()).isTrue();
    }

    @Test
    void getAll_rejectsUnknownSortField() {
        PageRequest pageable = PageRequest.of(0, 10, Sort.by("password"));

        assertThatThrownBy(() -> service.getAll(null, null, pageable))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void update_modifiesExistingEmployee() {
        Employee existing = entity(1L, "old@example.com");
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.existsByEmailIgnoreCaseAndIdNot("new@example.com", 1L)).thenReturn(false);
        when(repository.save(existing)).thenReturn(existing);

        EmployeeResponse response = service.update(1L, request("new@example.com"));

        assertThat(response.email()).isEqualTo("new@example.com");
    }

    @Test
    void update_throwsConflict_whenEmailBelongsToAnotherEmployee() {
        when(repository.findById(1L)).thenReturn(Optional.of(entity(1L, "old@example.com")));
        when(repository.existsByEmailIgnoreCaseAndIdNot("taken@example.com", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.update(1L, request("taken@example.com")))
                .isInstanceOf(DuplicateResourceException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void update_throwsNotFound_whenMissing() {
        when(repository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(5L, request("a@example.com")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_removesEmployee() {
        when(repository.existsById(1L)).thenReturn(true);

        service.delete(1L);

        verify(repository).deleteById(1L);
    }

    @Test
    void delete_throwsNotFound_whenMissing() {
        when(repository.existsById(1L)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(ResourceNotFoundException.class);
        verify(repository, never()).deleteById(any());
    }
    @Test
    void createAll_rejectsDuplicateEmailsInsideRequest() {
        assertThatThrownBy(() -> service.createAll(List.of(request("a@example.com"), request("A@example.com"))))
                .isInstanceOf(DuplicateResourceException.class);
        verify(repository, never()).saveAll(any());
    }

    @Test
    void createAll_savesAll_whenNoConflicts() {
        when(repository.findByEmailIn(List.of("a@example.com", "b@example.com"))).thenReturn(List.of());
        when(repository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        List<EmployeeResponse> result = service.createAll(List.of(request("a@example.com"), request("b@example.com")));

        assertThat(result).hasSize(2);
    }

    @Test
    void updateAll_throwsNotFound_listingMissingIds() {
        when(repository.findAllById(List.of(1L, 2L))).thenReturn(List.of(entity(1L, "a@example.com")));

        assertThatThrownBy(() -> service.updateAll(List.of(
                new BulkEmployeeUpdateRequest(1L, request("a@example.com")),
                new BulkEmployeeUpdateRequest(2L, request("b@example.com")))))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("[2]");
    }

    @Test
    void deleteAll_throwsNotFound_andDeletesNothing_whenAnyIdMissing() {
        when(repository.findAllById(List.of(1L, 2L))).thenReturn(List.of(entity(1L, "a@example.com")));

        assertThatThrownBy(() -> service.deleteAll(List.of(1L, 2L)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(repository, never()).deleteAllByIdInBatch(any());
    }
}
