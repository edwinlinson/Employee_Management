package com.example.employee.specification;

import com.example.employee.entity.Employee;
import com.example.employee.entity.EmployeeStatus;
import org.springframework.data.jpa.domain.Specification;

/**
 * Composable query predicates used for search and filtering.
 * Each method returns {@code null} when the criterion is absent, which Spring Data treats as "no filter".
 */
public final class EmployeeSpecifications {

    private EmployeeSpecifications() {
    }

    public static Specification<Employee> hasStatus(EmployeeStatus status) {
        if (status == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    /** Case-insensitive "contains" match on name, email, department and designation. */
    public static Specification<Employee> matchesText(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String pattern = "%" + text.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("firstName")), pattern),
                cb.like(cb.lower(root.get("lastName")), pattern),
                cb.like(cb.lower(root.get("email")), pattern),
                cb.like(cb.lower(root.get("department")), pattern),
                cb.like(cb.lower(root.get("designation")), pattern));
    }
}
