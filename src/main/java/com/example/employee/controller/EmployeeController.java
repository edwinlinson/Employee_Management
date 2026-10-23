package com.example.employee.controller;

import com.example.employee.dto.*;
import com.example.employee.entity.EmployeeStatus;
import com.example.employee.service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/employees")
@Validated
@Tag(name = "Employees", description = "Employee management operations")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @Operation(summary = "Create an employee")
    @ApiResponse(responseCode = "201", description = "Employee created")
    @ApiResponse(responseCode = "400", description = "Validation failed",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Email already exists",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping
    public ResponseEntity<EmployeeResponse> create(@Valid @RequestBody EmployeeRequest request) {
        EmployeeResponse created = employeeService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }


    @Operation(summary = "List employees with pagination, search and status filter",
            description = "Sort with `sort=field,asc|desc`, e.g. `sort=salary,desc`.")
    @GetMapping
    public PageResponse<EmployeeResponse> getAll(
            @Parameter(description = "Case-insensitive text matched against name, email, department and designation")
            @RequestParam(required = false) String search,
            @Parameter(description = "Filter by employment status")
            @RequestParam(required = false) EmployeeStatus status,
            @ParameterObject @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return employeeService.getAll(search, status, pageable);
    }

    @Operation(summary = "Get an employee by ID")
    @ApiResponse(responseCode = "200", description = "Employee found")
    @ApiResponse(responseCode = "404", description = "Employee not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/{id}")
    public EmployeeResponse getById(@PathVariable Long id) {
        return employeeService.getById(id);
    }

    @Operation(summary = "Update an employee (full update)")
    @ApiResponse(responseCode = "200", description = "Employee updated")
    @ApiResponse(responseCode = "400", description = "Validation failed",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Employee not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Email already used by another employee",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PutMapping("/{id}")
    public EmployeeResponse update(@PathVariable Long id, @Valid @RequestBody EmployeeRequest request) {
        return employeeService.update(id, request);
    }

    @Operation(summary = "Delete an employee")
    @ApiResponse(responseCode = "204", description = "Employee deleted")
    @ApiResponse(responseCode = "404", description = "Employee not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        employeeService.delete(id);
    }
    @Operation(summary = "Create multiple employees (all-or-nothing, max 100)")
    @ApiResponse(responseCode = "201", description = "Employees created")
    @ApiResponse(responseCode = "409", description = "A duplicate email was found")
    @PostMapping("/bulk")
    public ResponseEntity<List<EmployeeResponse>> createBulk(
            @RequestBody
            @NotEmpty(message = "At least one employee is required")
            @Size(max = 100, message = "A maximum of 100 employees is allowed per request")
            List<@NotNull(message = "Employee must not be null") @Valid EmployeeRequest> requests) {
        return ResponseEntity.status(HttpStatus.CREATED).body(employeeService.createAll(requests));
    }

    @Operation(summary = "Get multiple employees by IDs, e.g. ?ids=1,2,3")
    @GetMapping("/bulk")
    public List<EmployeeResponse> getBulk(
            @RequestParam
            @NotEmpty(message = "At least one id is required")
            @Size(max = 100, message = "A maximum of 100 ids is allowed per request")
            List<Long> ids) {
        return employeeService.getByIds(ids);
    }

    @Operation(summary = "Update multiple employees (all-or-nothing, max 100)")
    @PutMapping("/bulk")
    public List<EmployeeResponse> updateBulk(
            @RequestBody
            @NotEmpty(message = "At least one employee is required")
            @Size(max = 100, message = "A maximum of 100 employees is allowed per request")
            List<@NotNull(message = "Item must not be null") @Valid BulkEmployeeUpdateRequest> requests) {
        return employeeService.updateAll(requests);
    }

    @Operation(summary = "Delete multiple employees by IDs, e.g. ?ids=1,2,3 (all-or-nothing)")
    @DeleteMapping("/bulk")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBulk(
            @RequestParam
            @NotEmpty(message = "At least one id is required")
            @Size(max = 100, message = "A maximum of 100 ids is allowed per request")
            List<Long> ids) {
        employeeService.deleteAll(ids);
    }
}
