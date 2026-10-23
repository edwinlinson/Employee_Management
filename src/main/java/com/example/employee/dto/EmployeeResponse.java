package com.example.employee.dto;

import com.example.employee.entity.EmployeeStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record EmployeeResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String phone,
        String department,
        String designation,
        BigDecimal salary,
        LocalDate dateOfJoining,
        EmployeeStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
