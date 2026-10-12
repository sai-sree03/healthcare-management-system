package com.healthcare.hms.repository;

import com.healthcare.hms.entity.AppUser;
import com.healthcare.hms.entity.Role;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<AppUser, Long> {

    @EntityGraph(attributePaths = "doctor")
    Optional<AppUser> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByDoctor_Id(Long doctorId);

    long countByRoleAndEnabledTrue(Role role);

    @EntityGraph(attributePaths = "doctor")
    List<AppUser> findAllByOrderByUsernameAsc();
}
