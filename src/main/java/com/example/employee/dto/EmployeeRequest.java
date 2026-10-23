package com.example.employee.dto;

import com.example.employee.entity.EmployeeStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Payload used to create or update an employee")
public record EmployeeRequest(

        @Schema(example = "Asha")
        @NotBlank(message = "First name is required")
        @Size(max = 50, message = "First name must not exceed 50 characters")
        String firstName,

        @Schema(example = "Menon")
        @NotBlank(message = "Last name is required")
        @Size(max = 50, message = "Last name must not exceed 50 characters")
        String lastName,

        @Schema(example = "asha.menon@example.com")
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        @Size(max = 100, message = "Email must not exceed 100 characters")
        String email,

        @Schema(example = "+919876543210", description = "Optional. 7-15 digits, optional leading +")
        @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "Phone must contain 7 to 15 digits with an optional leading +")
        String phone,

        @Schema(example = "Engineering")
        @NotBlank(message = "Department is required")
        @Size(max = 50, message = "Department must not exceed 50 characters")
        String department,

        @Schema(example = "Software Engineer")
        @NotBlank(message = "Designation is required")
        @Size(max = 50, message = "Designation must not exceed 50 characters")
        String designation,

        @Schema(example = "85000.00")
        @NotNull(message = "Salary is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Salary must be greater than zero")
        @Digits(integer = 10, fraction = 2, message = "Salary allows up to 10 integer and 2 fraction digits")
        BigDecimal salary,

        @Schema(example = "2024-06-15", type = "string", format = "date")
        @NotNull(message = "Date of joining is required")
        @PastOrPresent(message = "Date of joining cannot be in the future")
        LocalDate dateOfJoining,

        @Schema(description = "Defaults to ACTIVE when omitted", example = "ACTIVE")
        EmployeeStatus status
) {
}
