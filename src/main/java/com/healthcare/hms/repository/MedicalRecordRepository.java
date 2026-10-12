package com.healthcare.hms.repository;

import com.healthcare.hms.entity.MedicalRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Long> {

    @EntityGraph(attributePaths = {"patient", "doctor"})
    Optional<MedicalRecord> findWithDetailsByIdAndActiveTrue(Long id);

    long countByActiveTrue();

    @EntityGraph(attributePaths = {"patient", "doctor"})
    @Query("""
            select r from MedicalRecord r
            where r.active = true
              and (:patientId is null or r.patient.id = :patientId)
              and (lower(r.diagnosis) like lower(concat('%', :search, '%'))
                   or lower(r.patient.fullName) like lower(concat('%', :search, '%')))
            """)
    Page<MedicalRecord> search(@Param("patientId") Long patientId,
                               @Param("search") String search,
                               Pageable pageable);
}
