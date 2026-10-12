package com.healthcare.hms.repository;

import com.healthcare.hms.entity.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {

    Optional<Patient> findByIdAndActiveTrue(Long id);

    boolean existsByEmailIgnoreCaseAndActiveTrue(String email);

    boolean existsByEmailIgnoreCaseAndActiveTrueAndIdNot(String email, Long id);

    long countByActiveTrue();

    @Query("""
            select p from Patient p
            where p.active = true
              and (lower(p.fullName) like lower(concat('%', :search, '%'))
                   or lower(p.phone) like lower(concat('%', :search, '%'))
                   or lower(p.email) like lower(concat('%', :search, '%')))
            """)
    Page<Patient> search(@Param("search") String search, Pageable pageable);
}
