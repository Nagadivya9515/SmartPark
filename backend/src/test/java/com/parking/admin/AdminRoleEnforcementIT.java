package com.parking.admin;

import com.parking.IntegrationTestBase;
import com.parking.TestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Integration tests for Admin role enforcement.
 * Validates that SUPER_ADMIN and ADMIN roles have correct permissions on admin endpoints.
 * 
 * CRITICAL SCENARIOS:
 * 1. SUPER_ADMIN can access all /api/admin/** endpoints
 * 2. ADMIN can access all /api/admin/** endpoints
 * 3. Regular USER gets 403 Forbidden on admin endpoints
 * 4. OPERATOR gets 403 Forbidden on admin endpoints
 * 5. Slot toggle is restricted to ADMIN/SUPER_ADMIN only
 */
@DisplayName("Admin Role Enforcement Integration Tests")
public class AdminRoleEnforcementIT extends IntegrationTestBase {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ParkingLotRepository parkingLotRepository;

    @Autowired
    private ParkingSlotRepository parkingSlotRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String superAdminEmail;
    private String adminEmail;
    private String regularUserEmail;
    private String operatorEmail;

    @BeforeEach
    @Transactional
    void setUp() {
        superAdminEmail = TestSupport.TestData.SUPER_ADMIN_EMAIL;
        adminEmail = TestSupport.TestData.ADMIN_EMAIL;
        regularUserEmail = TestSupport.TestData.USER_EMAIL;
        operatorEmail = TestSupport.TestData.OPERATOR_USERNAME + "@smartpark.com";

        // Create SUPER_ADMIN user
        createUser(superAdminEmail, "SuperAdmin@123", User.Role.SUPER_ADMIN);

        // Create ADMIN user
        createUser(adminEmail, "Admin@123", User.Role.ADMIN);

        // Create regular USER
        createUser(regularUserEmail, TestSupport.TestData.USER_PASSWORD, User.Role.USER);

        // Create OPERATOR user
        createUser(operatorEmail, TestSupport.TestData.OPERATOR_PASSWORD, User.Role.OPERATOR);
    }

    @Test
    @DisplayName("SUPER_ADMIN can access /api/admin/overview")
    void testSuperAdminAccessOverview() throws Exception {
        MvcResult result = mockMvc.perform(
            get("/api/admin/overview")
                .with(user(superAdminEmail).roles("SUPER_ADMIN"))
        ).andReturn();

        assertThat(result.getResponse().getStatus())
            .as("SUPER_ADMIN should have access to admin overview")
            .isEqualTo(200);
    }

    @Test
    @DisplayName("ADMIN can access /api/admin/overview")
    void testAdminAccessOverview() throws Exception {
        MvcResult result = mockMvc.perform(
            get("/api/admin/overview")
                .with(user(adminEmail).roles("ADMIN"))
        ).andReturn();

        assertThat(result.getResponse().getStatus())
            .as("ADMIN should have access to admin overview")
            .isEqualTo(200);
    }

    @Test
    @DisplayName("Regular USER cannot access /api/admin/overview (403 Forbidden)")
    void testRegularUserDeniedAdminOverview() throws Exception {
        MvcResult result = mockMvc.perform(
            get("/api/admin/overview")
                .with(user(regularUserEmail).roles("USER"))
        ).andReturn();

        assertThat(result.getResponse().getStatus())
            .as("Regular USER should get 403 Forbidden on admin endpoint")
            .isEqualTo(403);
    }

    @Test
    @DisplayName("OPERATOR cannot access /api/admin/overview (403 Forbidden)")
    void testOperatorDeniedAdminOverview() throws Exception {
        MvcResult result = mockMvc.perform(
            get("/api/admin/overview")
                .with(user(operatorEmail).roles("OPERATOR"))
        ).andReturn();

        assertThat(result.getResponse().getStatus())
            .as("OPERATOR should get 403 Forbidden on admin endpoint")
            .isEqualTo(403);
    }

    @Test
    @DisplayName("SUPER_ADMIN can access /api/admin/parking-lots")
    void testSuperAdminAccessParkingLots() throws Exception {
        MvcResult result = mockMvc.perform(
            get("/api/admin/parking-lots")
                .with(user(superAdminEmail).roles("SUPER_ADMIN"))
        ).andReturn();

        assertThat(result.getResponse().getStatus())
            .as("SUPER_ADMIN should access parking lots")
            .isEqualTo(200);
    }

    @Test
    @DisplayName("ADMIN can access /api/admin/parking-lots")
    void testAdminAccessParkingLots() throws Exception {
        MvcResult result = mockMvc.perform(
            get("/api/admin/parking-lots")
                .with(user(adminEmail).roles("ADMIN"))
        ).andReturn();

        assertThat(result.getResponse().getStatus())
            .as("ADMIN should access parking lots")
            .isEqualTo(200);
    }

    @Test
    @DisplayName("SUPER_ADMIN can toggle slot occupancy")
    void testSuperAdminCanToggleSlot() throws Exception {
        Long slotId = createTestSlot();

        MvcResult result = mockMvc.perform(
            post("/api/admin/slots/" + slotId + "/toggle")
                .contentType(MediaType.APPLICATION_JSON)
                .with(user(superAdminEmail).roles("SUPER_ADMIN"))
        ).andReturn();

        assertThat(result.getResponse().getStatus())
            .as("SUPER_ADMIN should be able to toggle slot")
            .isEqualTo(200);
    }

    @Test
    @DisplayName("ADMIN can toggle slot occupancy")
    void testAdminCanToggleSlot() throws Exception {
        Long slotId = createTestSlot();

        MvcResult result = mockMvc.perform(
            post("/api/admin/slots/" + slotId + "/toggle")
                .contentType(MediaType.APPLICATION_JSON)
                .with(user(adminEmail).roles("ADMIN"))
        ).andReturn();

        assertThat(result.getResponse().getStatus())
            .as("ADMIN should be able to toggle slot")
            .isEqualTo(200);
    }

    @Test
    @DisplayName("Regular USER cannot toggle slot (403 Forbidden)")
    void testRegularUserCannotToggleSlot() throws Exception {
        Long slotId = createTestSlot();

        MvcResult result = mockMvc.perform(
            post("/api/admin/slots/" + slotId + "/toggle")
                .contentType(MediaType.APPLICATION_JSON)
                .with(user(regularUserEmail).roles("USER"))
        ).andReturn();

        assertThat(result.getResponse().getStatus())
            .as("Regular USER should get 403 Forbidden")
            .isEqualTo(403);
    }

    @Test
    @DisplayName("OPERATOR cannot toggle slot (403 Forbidden)")
    void testOperatorCannotToggleSlot() throws Exception {
        Long slotId = createTestSlot();

        MvcResult result = mockMvc.perform(
            post("/api/admin/slots/" + slotId + "/toggle")
                .contentType(MediaType.APPLICATION_JSON)
                .with(user(operatorEmail).roles("OPERATOR"))
        ).andReturn();

        assertThat(result.getResponse().getStatus())
            .as("OPERATOR should get 403 Forbidden")
            .isEqualTo(403);
    }

    @Test
    @DisplayName("SUPER_ADMIN can access /api/admin/operators")
    void testSuperAdminAccessOperators() throws Exception {
        MvcResult result = mockMvc.perform(
            get("/api/admin/operators")
                .with(user(superAdminEmail).roles("SUPER_ADMIN"))
        ).andReturn();

        assertThat(result.getResponse().getStatus())
            .as("SUPER_ADMIN should access operators endpoint")
            .isEqualTo(200);
    }

    @Test
    @DisplayName("ADMIN can access /api/admin/operators")
    void testAdminAccessOperators() throws Exception {
        MvcResult result = mockMvc.perform(
            get("/api/admin/operators")
                .with(user(adminEmail).roles("ADMIN"))
        ).andReturn();

        assertThat(result.getResponse().getStatus())
            .as("ADMIN should access operators endpoint")
            .isEqualTo(200);
    }

    @Test
    @DisplayName("SUPER_ADMIN can access /api/admin/reports")
    void testSuperAdminAccessReports() throws Exception {
        MvcResult result = mockMvc.perform(
            get("/api/admin/reports")
                .with(user(superAdminEmail).roles("SUPER_ADMIN"))
        ).andReturn();

        assertThat(result.getResponse().getStatus())
            .as("SUPER_ADMIN should access reports endpoint")
            .isEqualTo(200);
    }

    @Test
    @DisplayName("ADMIN can access /api/admin/reports")
    void testAdminAccessReports() throws Exception {
        MvcResult result = mockMvc.perform(
            get("/api/admin/reports")
                .with(user(adminEmail).roles("ADMIN"))
        ).andReturn();

        assertThat(result.getResponse().getStatus())
            .as("ADMIN should access reports endpoint")
            .isEqualTo(200);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "/api/admin/overview",
        "/api/admin/parking-lots",
        "/api/admin/operators",
        "/api/admin/reports"
    })
    @DisplayName("All admin endpoints enforce ADMIN/SUPER_ADMIN role strictly")
    void testAllAdminEndpointsEnforceRole(String endpoint) throws Exception {
        MvcResult result = mockMvc.perform(
            get(endpoint)
                .with(user(regularUserEmail).roles("USER"))
        ).andReturn();

        assertThat(result.getResponse().getStatus())
            .as("Endpoint " + endpoint + " should deny regular USER (403)")
            .isEqualTo(403);

        MvcResult operatorResult = mockMvc.perform(
            get(endpoint)
                .with(user(operatorEmail).roles("OPERATOR"))
        ).andReturn();

        assertThat(operatorResult.getResponse().getStatus())
            .as("Endpoint " + endpoint + " should deny OPERATOR (403)")
            .isEqualTo(403);
    }

    // ─── Helper Methods ───

    @Transactional
    private void createUser(String email, String password, User.Role role) {
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setFirstName(role.toString());
        user.setLastName("User");
        user.setRole(role);
        user.setEnabled(true);
        userRepository.save(user);
    }

    @Transactional
    private Long createTestSlot() {
        ParkingLot lot = parkingLotRepository.findAll().stream()
            .findFirst()
            .orElseGet(() -> {
                ParkingLot newLot = new ParkingLot();
                newLot.setName("Admin Test Lot");
                newLot.setLocation("Admin Test Location");
                return parkingLotRepository.save(newLot);
            });

        ParkingSlot slot = new ParkingSlot();
        slot.setParkingLot(lot);
        slot.setSlotNumber("ADMIN-" + System.currentTimeMillis());
        slot.setSlotSection("B");
        slot.setFloor(2);
        slot.setIsOccupied(false);
        slot.setHourlyRate(7.5);
        return parkingSlotRepository.save(slot).getId();
    }
}
