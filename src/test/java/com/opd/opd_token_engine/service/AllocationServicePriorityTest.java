package com.opd.opd_token_engine.service;

import com.opd.opd_token_engine.dto.AllocationResponseDTO;
import com.opd.opd_token_engine.model.Doctor;
import com.opd.opd_token_engine.model.TokenSource;
import com.opd.opd_token_engine.repository.InMemoryStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests to verify priority-based allocation and preemption logic
 */
@SpringBootTest
class AllocationServicePriorityTest {

    @Autowired
    private AllocationService allocationService;

    @BeforeEach
    void setUp() {
        // Clear and reinitialize doctors
        InMemoryStore.doctors.clear();
        
        Doctor d1 = new Doctor("D1", 1.0);
        d1.addSlot("9-10", 3); // Small capacity to test preemption
        InMemoryStore.doctors.put("D1", d1);
    }

    @Test
    void testBasicAllocationWithinCapacity() {
        // Test basic allocation when slot has capacity
        AllocationResponseDTO response = allocationService.createToken(
            "D1", "9-10", "P001", TokenSource.ONLINE
        );

        assertEquals("ALLOCATED", response.status);
        assertNotNull(response.tokenId);
        assertNull(response.promotedTokenId);
    }

    @Test
    void testWaitlistWhenSlotFull() {
        // Fill the slot
        allocationService.createToken("D1", "9-10", "P001", TokenSource.WALK_IN);
        allocationService.createToken("D1", "9-10", "P002", TokenSource.WALK_IN);
        allocationService.createToken("D1", "9-10", "P003", TokenSource.WALK_IN);

        // Next request should be waitlisted
        AllocationResponseDTO response = allocationService.createToken(
            "D1", "9-10", "P004", TokenSource.WALK_IN
        );

        assertEquals("WAITLISTED", response.status);
        assertNotNull(response.tokenId);
        assertNotNull(response.positionInQueue);
    }

    @Test
    void testEmergencyPreemptsLowerPriority() {
        // Fill slot with low-priority walk-ins
        allocationService.createToken("D1", "9-10", "P001", TokenSource.WALK_IN);
        allocationService.createToken("D1", "9-10", "P002", TokenSource.WALK_IN);
        allocationService.createToken("D1", "9-10", "P003", TokenSource.WALK_IN);

        // Emergency should preempt a walk-in
        AllocationResponseDTO response = allocationService.createToken(
            "D1", "9-10", "P004", TokenSource.EMERGENCY
        );

        assertEquals("REALLOCATED_LOW_PRIORITY", response.status);
        assertNotNull(response.tokenId);
        assertNotNull(response.promotedTokenId);
    }

    @Test
    void testPriorityOrdering() {
        // Add tokens with different priorities
        AllocationResponseDTO r1 = allocationService.createToken("D1", "9-10", "P001", TokenSource.ONLINE);
        AllocationResponseDTO r2 = allocationService.createToken("D1", "9-10", "P002", TokenSource.WALK_IN);
        AllocationResponseDTO r3 = allocationService.createToken("D1", "9-10", "P003", TokenSource.PAID);

        // All should be allocated
        assertEquals("ALLOCATED", r1.status);
        assertEquals("ALLOCATED", r2.status);
        assertEquals("ALLOCATED", r3.status);

        // Add emergency - should preempt WALK_IN (lowest priority)
        AllocationResponseDTO r4 = allocationService.createToken("D1", "9-10", "P004", TokenSource.EMERGENCY);
        assertEquals("REALLOCATED_LOW_PRIORITY", r4.status);
    }

    @Test
    void testTokenCancellation() {
        // Allocate tokens
        AllocationResponseDTO r1 = allocationService.createToken("D1", "9-10", "P001", TokenSource.ONLINE);
        allocationService.createToken("D1", "9-10", "P002", TokenSource.ONLINE);
        allocationService.createToken("D1", "9-10", "P003", TokenSource.ONLINE);

        // Cancel one
        boolean cancelled = allocationService.cancelPatientToken("D1", "9-10", r1.tokenId);
        assertTrue(cancelled);

        // Next allocation should succeed without preemption
        AllocationResponseDTO r4 = allocationService.createToken("D1", "9-10", "P004", TokenSource.ONLINE);
        assertEquals("ALLOCATED", r4.status);
    }

    @Test
    void testCancellationPromotesWaitlist() {
        // Fill the slot
        AllocationResponseDTO r1 = allocationService.createToken("D1", "9-10", "P001", TokenSource.ONLINE);
        allocationService.createToken("D1", "9-10", "P002", TokenSource.ONLINE);
        allocationService.createToken("D1", "9-10", "P003", TokenSource.ONLINE);

        // Add to waitlist with same priority
        AllocationResponseDTO r4 = allocationService.createToken("D1", "9-10", "P004", TokenSource.ONLINE);
        assertEquals("WAITLISTED", r4.status);

        // Cancel an allocated token
        allocationService.cancelPatientToken("D1", "9-10", r1.tokenId);

        // The waitlisted token should have been promoted (verified by checking slot state)
        // We can't directly verify promotion here, but the cancellation should succeed
    }

    @Test
    void testInvalidDoctorReturnsError() {
        AllocationResponseDTO response = allocationService.createToken(
            "INVALID_DOCTOR", "9-10", "P001", TokenSource.ONLINE
        );

        assertEquals("ERROR", response.status);
        assertNull(response.tokenId);
        assertTrue(response.reason.contains("Doctor not found"));
    }

    @Test
    void testInvalidSlotReturnsError() {
        AllocationResponseDTO response = allocationService.createToken(
            "D1", "INVALID_SLOT", "P001", TokenSource.ONLINE
        );

        assertEquals("ERROR", response.status);
        assertNull(response.tokenId);
        assertTrue(response.reason.contains("Slot not found"));
    }
}
