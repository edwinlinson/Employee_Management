package com.example.employee.service;

import com.example.employee.dto.BulkEmployeeUpdateRequest;
import com.example.employee.dto.EmployeeRequest;
import com.example.employee.dto.EmployeeResponse;
import com.example.employee.dto.PageResponse;
import com.example.employee.entity.EmployeeStatus;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface EmployeeService {

    EmployeeResponse create(EmployeeRequest request);

    EmployeeResponse getById(Long id);

    PageResponse<EmployeeResponse> getAll(String search, EmployeeStatus status, Pageable pageable);

    EmployeeResponse update(Long id, EmployeeRequest request);

    void delete(Long id);

    List<EmployeeResponse> createAll(List<EmployeeRequest> requests);

    List<EmployeeResponse> getByIds(List<Long> ids);

    List<EmployeeResponse> updateAll(List<BulkEmployeeUpdateRequest> requests);

    void deleteAll(List<Long> ids);
}
