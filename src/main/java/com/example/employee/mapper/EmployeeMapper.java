package com.example.employee.mapper;

import com.example.employee.dto.EmployeeRequest;
import com.example.employee.dto.EmployeeResponse;
import com.example.employee.entity.Employee;
import com.example.employee.entity.EmployeeStatus;
import org.springframework.stereotype.Component;

/**
 * Converts between entities and DTOs so the API contract is decoupled from the persistence model.
 */
@Component
public class EmployeeMapper {

    public Employee toEntity(EmployeeRequest request) {
        Employee employee = new Employee();
        applyRequest(request, employee);
        return employee;
    }

    public void updateEntity(EmployeeRequest request, Employee employee) {
        applyRequest(request, employee);
    }

    public EmployeeResponse toResponse(Employee e) {
        return new EmployeeResponse(
                e.getId(), e.getFirstName(), e.getLastName(), e.getEmail(), e.getPhone(),
                e.getDepartment(), e.getDesignation(), e.getSalary(), e.getDateOfJoining(),
                e.getStatus(), e.getCreatedAt(), e.getUpdatedAt());
    }

    private void applyRequest(EmployeeRequest r, Employee e) {
        e.setFirstName(r.firstName().trim());
        e.setLastName(r.lastName().trim());
        e.setEmail(normalizeEmail(r.email()));
        e.setPhone(r.phone() == null ? null : r.phone().trim());
        e.setDepartment(r.department().trim());
        e.setDesignation(r.designation().trim());
        e.setSalary(r.salary());
        e.setDateOfJoining(r.dateOfJoining());
        e.setStatus(r.status() != null ? r.status() : EmployeeStatus.ACTIVE);
    }

    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
