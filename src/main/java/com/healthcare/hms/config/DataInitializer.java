package com.healthcare.hms.config;

import com.healthcare.hms.entity.AppUser;
import com.healthcare.hms.entity.Doctor;
import com.healthcare.hms.entity.Gender;
import com.healthcare.hms.entity.Patient;
import com.healthcare.hms.entity.Role;
import com.healthcare.hms.repository.DoctorRepository;
import com.healthcare.hms.repository.PatientRepository;
import com.healthcare.hms.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * On a brand-new database there is nobody who can log in, so this creates the first accounts.
 * It does nothing once any user exists. For production: set HMS_ADMIN_PASSWORD, and HMS_SEED_DEMO=false.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository users;
    private final DoctorRepository doctors;
    private final PatientRepository patients;
    private final PasswordEncoder passwordEncoder;

    @Value("${hms.seed.enabled:true}") private boolean enabled;
    @Value("${hms.seed.demo-data:true}") private boolean demoData;
    @Value("${hms.seed.admin-password:Admin@123}") private String adminPassword;
    @Value("${hms.seed.receptionist-password:Reception@123}") private String receptionistPassword;
    @Value("${hms.seed.doctor-password:Doctor@123}") private String doctorPassword;

    public DataInitializer(UserRepository users, DoctorRepository doctors,
                           PatientRepository patients, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.doctors = doctors;
        this.patients = patients;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (!enabled || users.count() > 0) {
            return;
        }

        createUser("admin", adminPassword, "System Administrator", Role.ADMIN, null);
        log.warn("Created the first administrator account 'admin'. Change its password if you did not set HMS_ADMIN_PASSWORD.");

        if (demoData) {
            createUser("reception", receptionistPassword, "Front Desk", Role.RECEPTIONIST, null);

            Doctor doctor = new Doctor();
            doctor.setFullName("Dr. Anita Rao");
            doctor.setEmail("anita.rao@hms.local");
            doctor.setPhone("9876543210");
            doctor.setSpecialization("Cardiology");
            doctor.setQualification("MD, DM Cardiology");
            doctor.setExperienceYears(12);
            doctor.setAvailableFrom(LocalTime.of(9, 0));
            doctor.setAvailableTo(LocalTime.of(17, 0));
            doctor = doctors.save(doctor);
            createUser("doctor", doctorPassword, doctor.getFullName(), Role.DOCTOR, doctor);

            Patient patient = new Patient();
            patient.setFullName("Ravi Kumar");
            patient.setDateOfBirth(LocalDate.of(1990, 5, 14));
            patient.setGender(Gender.MALE);
            patient.setPhone("9123456780");
            patient.setEmail("ravi.kumar@example.com");
            patient.setBloodGroup("O+");
            patients.save(patient);

            log.warn("Demo data created (users 'reception' and 'doctor', one doctor, one patient). "
                    + "Set HMS_SEED_DEMO=false for real deployments.");
        }
    }

    private void createUser(String username, String password, String fullName, Role role, Doctor doctor) {
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setFullName(fullName);
        user.setRole(role);
        user.setEnabled(true);
        user.setDoctor(doctor);
        users.save(user);
    }
}
