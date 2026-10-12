package com.healthcare.hms.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Checks the URL-level role matrix from SecurityConfig against the real filter chain.
 * (Ownership rules, such as "a doctor edits only their own records", are tested in the service tests.)
 */
@SpringBootTest
class RoleAccessTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void anonymousApiCallsGet401() throws Exception {
        mvc.perform(get("/api/patients")).andExpect(status().isUnauthorized());
    }

    @Test
    void allStaffRolesCanReadPatients() throws Exception {
        for (String role : new String[]{"ADMIN", "RECEPTIONIST", "DOCTOR"}) {
            mvc.perform(get("/api/patients").with(user("u").roles(role))).andExpect(status().isOk());
        }
    }

    @Test
    void doctorsCannotCreatePatients() throws Exception {
        mvc.perform(post("/api/patients").with(user("d").roles("DOCTOR")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void receptionistsCannotDeletePatients() throws Exception {
        mvc.perform(delete("/api/patients/1").with(user("r").roles("RECEPTIONIST")).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void receptionistsNeverSeeMedicalRecords() throws Exception {
        mvc.perform(get("/api/medical-records").with(user("r").roles("RECEPTIONIST")))
                .andExpect(status().isForbidden());
    }

    @Test
    void administratorsCannotWriteMedicalRecords() throws Exception {
        mvc.perform(post("/api/medical-records").with(user("a").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void onlyAdministratorsManageUsers() throws Exception {
        mvc.perform(get("/api/users").with(user("d").roles("DOCTOR"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/users").with(user("r").roles("RECEPTIONIST"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/users").with(user("a").roles("ADMIN"))).andExpect(status().isOk());
    }

    @Test
    void writesWithoutCsrfTokenAreRejected() throws Exception {
        mvc.perform(post("/api/patients").with(user("a").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidPatientIsRejectedWith400() throws Exception {
        mvc.perform(post("/api/patients").with(user("a").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void pagesAreProtectedToo() throws Exception {
        mvc.perform(get("/patients.html")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/medical-records.html").with(user("r").roles("RECEPTIONIST")))
                .andExpect(status().is3xxRedirection());
    }
}
