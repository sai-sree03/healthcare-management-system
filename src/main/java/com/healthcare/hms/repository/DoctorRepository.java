package com.healthcare.hms.repository;

import com.healthcare.hms.entity.Doctor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    Optional<Doctor> findByIdAndActiveTrue(Long id);

    List<Doctor> findByActiveTrueOrderByFullNameAsc();

    boolean existsByEmailIgnoreCaseAndActiveTrue(String email);

    boolean existsByEmailIgnoreCaseAndActiveTrueAndIdNot(String email, Long id);

    long countByActiveTrue();

    @Query("""
            select d from Doctor d
            where d.active = true
              and (lower(d.fullName) like lower(concat('%', :search, '%'))
                   or lower(d.specialization) like lower(concat('%', :search, '%'))
                   or lower(d.email) like lower(concat('%', :search, '%')))
            """)
    Page<Doctor> search(@Param("search") String search, Pageable pageable);
}
