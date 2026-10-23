package com.example.employee.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record BulkEmployeeUpdateRequest(
        @NotNull(message = "Employee id is required")
        Long id,

        @NotNull(message = "Employee data is required")
        @Valid
        EmployeeRequest employee
) {
}