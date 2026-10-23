package com.example.employee.service.impl;

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
import com.example.employee.service.EmployeeService;
import com.example.employee.specification.EmployeeSpecifications;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class EmployeeServiceImpl implements EmployeeService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeServiceImpl.class);

    /** Whitelist of sortable fields, so clients cannot sort by arbitrary/unknown properties. */
    private static final Set<String> SORTABLE_FIELDS = Set.of(
            "id", "firstName", "lastName", "email", "department", "designation",
            "salary", "dateOfJoining", "status", "createdAt");

    private final EmployeeRepository repository;
    private final EmployeeMapper mapper;

    public EmployeeServiceImpl(EmployeeRepository repository, EmployeeMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public EmployeeResponse create(EmployeeRequest request) {
        String email = EmployeeMapper.normalizeEmail(request.email());
        if (repository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("Employee already exists with email: " + email);
        }
        Employee saved = repository.save(mapper.toEntity(request));
        log.info("Created employee id={}", saved.getId());
        return mapper.toResponse(saved);
    }

    @Override
    public EmployeeResponse getById(Long id) {
        return mapper.toResponse(findOrThrow(id));
    }

    @Override
    public PageResponse<EmployeeResponse> getAll(String search, EmployeeStatus status, Pageable pageable) {
        validateSort(pageable);
        Specification<Employee> spec = Specification
                .where(EmployeeSpecifications.hasStatus(status))
                .and(EmployeeSpecifications.matchesText(search));
        Page<EmployeeResponse> page = repository.findAll(spec, pageable).map(mapper::toResponse);
        return PageResponse.from(page);
    }

    @Override
    @Transactional
    public EmployeeResponse update(Long id, EmployeeRequest request) {
        Employee employee = findOrThrow(id);
        String email = EmployeeMapper.normalizeEmail(request.email());
        if (repository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new DuplicateResourceException("Another employee already uses email: " + email);
        }
        mapper.updateEntity(request, employee);
        Employee saved = repository.save(employee);
        log.info("Updated employee id={}", id);
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Employee", id);
        }
        repository.deleteById(id);
        log.info("Deleted employee id={}", id);
    }

    private Employee findOrThrow(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Employee", id));
    }

    private void validateSort(Pageable pageable) {
        pageable.getSort().forEach(order -> {
            if (!SORTABLE_FIELDS.contains(order.getProperty())) {
                throw new BadRequestException("Invalid sort field '%s'. Allowed: %s"
                        .formatted(order.getProperty(), SORTABLE_FIELDS));
            }
        });
    }
    @Override
    @Transactional
    public List<EmployeeResponse> createAll(List<EmployeeRequest> requests) {
        List<String> emails = requests.stream()
                .map(r -> EmployeeMapper.normalizeEmail(r.email()))
                .toList();

        Set<String> seen = new HashSet<>();
        Set<String> repeated = new LinkedHashSet<>();
        emails.forEach(e -> {
            if (!seen.add(e)) {
                repeated.add(e);
            }
        });
        if (!repeated.isEmpty()) {
            throw new DuplicateResourceException("Duplicate emails within the request: " + repeated);
        }

        Set<String> existing = repository.findByEmailIn(emails).stream()
                .map(Employee::getEmail)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (!existing.isEmpty()) {
            throw new DuplicateResourceException("Employees already exist with emails: " + existing);
        }

        List<Employee> saved = repository.saveAll(requests.stream().map(mapper::toEntity).toList());
        log.info("Bulk created {} employees", saved.size());
        return saved.stream().map(mapper::toResponse).toList();
    }

    @Override
    public List<EmployeeResponse> getByIds(List<Long> ids) {
        List<Long> distinctIds = ids.stream().distinct().toList();
        Map<Long, Employee> byId = loadAllOrThrow(distinctIds);
        return distinctIds.stream().map(id -> mapper.toResponse(byId.get(id))).toList();
    }

    @Override
    @Transactional
    public List<EmployeeResponse> updateAll(List<BulkEmployeeUpdateRequest> requests) {
        List<Long> ids = requests.stream().map(BulkEmployeeUpdateRequest::id).toList();
        if (new HashSet<>(ids).size() != ids.size()) {
            throw new BadRequestException("Duplicate employee ids in the request");
        }
        Map<Long, Employee> byId = loadAllOrThrow(ids);

        Map<String, Long> emailToId = new HashMap<>();
        for (BulkEmployeeUpdateRequest item : requests) {
            String email = EmployeeMapper.normalizeEmail(item.employee().email());
            if (emailToId.put(email, item.id()) != null) {
                throw new DuplicateResourceException("Duplicate emails within the request: " + email);
            }
        }
        repository.findByEmailIn(emailToId.keySet()).forEach(found -> {
            if (!found.getId().equals(emailToId.get(found.getEmail()))) {
                throw new DuplicateResourceException("Another employee already uses email: " + found.getEmail());
            }
        });

        List<Employee> toSave = requests.stream().map(item -> {
            Employee employee = byId.get(item.id());
            mapper.updateEntity(item.employee(), employee);
            return employee;
        }).toList();

        List<Employee> saved = repository.saveAll(toSave);
        log.info("Bulk updated {} employees", saved.size());
        return saved.stream().map(mapper::toResponse).toList();
    }

    @Override
    @Transactional
    public void deleteAll(List<Long> ids) {
        List<Long> distinctIds = ids.stream().distinct().toList();
        loadAllOrThrow(distinctIds);
        repository.deleteAllByIdInBatch(distinctIds);
        log.info("Bulk deleted {} employees", distinctIds.size());
    }

    /** Loads all ids or throws 404 listing every missing id (so bulk operations are all-or-nothing). */
    private Map<Long, Employee> loadAllOrThrow(List<Long> ids) {
        Map<Long, Employee> byId = repository.findAllById(ids).stream()
                .collect(Collectors.toMap(Employee::getId, Function.identity()));
        List<Long> missing = ids.stream().filter(id -> !byId.containsKey(id)).toList();
        if (!missing.isEmpty()) {
            throw new ResourceNotFoundException("Employee", missing);
        }
        return byId;
    }
}
