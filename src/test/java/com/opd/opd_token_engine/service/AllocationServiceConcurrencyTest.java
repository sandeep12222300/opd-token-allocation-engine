package com.opd.opd_token_engine.service;

import com.opd.opd_token_engine.dto.AllocationResponseDTO;
import com.opd.opd_token_engine.model.Doctor;
import com.opd.opd_token_engine.model.TokenSource;
import com.opd.opd_token_engine.repository.InMemoryStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests to verify thread-safety and concurrent access handling in AllocationService
 */
@SpringBootTest
class AllocationServiceConcurrencyTest {

    @Autowired
    private AllocationService allocationService;

    @BeforeEach
    void setUp() {
        // Clear and reinitialize doctors
        InMemoryStore.doctors.clear();
        
        Doctor d1 = new Doctor("D1", 1.0);
        d1.addSlot("9-10", 5);
        InMemoryStore.doctors.put("D1", d1);
    }

    @Test
    void testConcurrentTokenAllocation() throws InterruptedException, ExecutionException {
        int numThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        List<Future<AllocationResponseDTO>> futures = new ArrayList<>();

        // Submit 10 concurrent token allocation requests
        for (int i = 0; i < numThreads; i++) {
            final int patientNum = i;
            Future<AllocationResponseDTO> future = executor.submit(() -> 
                allocationService.createToken(
                    "D1", 
                    "9-10", 
                    "P" + patientNum, 
                    patientNum % 2 == 0 ? TokenSource.EMERGENCY : TokenSource.WALK_IN
                )
            );
            futures.add(future);
        }

        // Wait for all to complete
        List<AllocationResponseDTO> responses = new ArrayList<>();
        for (Future<AllocationResponseDTO> future : futures) {
            responses.add(future.get());
        }

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        // Verify all requests were processed (no exceptions)
        assertEquals(numThreads, responses.size());
        
        // Verify none returned ERROR status
        for (AllocationResponseDTO response : responses) {
            assertNotEquals("ERROR", response.status);
            assertNotNull(response.tokenId);
        }

        // Count allocations - should be 5 allocated, 5 waitlisted or reallocated
        long allocatedCount = responses.stream()
            .filter(r -> "ALLOCATED".equals(r.status))
            .count();
        
        long waitlistedOrReallocated = responses.stream()
            .filter(r -> "WAITLISTED".equals(r.status) || "REALLOCATED_LOW_PRIORITY".equals(r.status))
            .count();

        assertEquals(10, allocatedCount + waitlistedOrReallocated);
    }

    @Test
    void testConcurrentDoctorModification() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(3);
        CountDownLatch latch = new CountDownLatch(3);
        ConcurrentLinkedQueue<Exception> exceptions = new ConcurrentLinkedQueue<>();

        // Thread 1: Add slots
        executor.submit(() -> {
            try {
                for (int i = 0; i < 5; i++) {
                    Doctor d = InMemoryStore.doctors.get("D1");
                    if (d != null) {
                        d.addSlot("10-" + (11 + i), 3);
                    }
                    Thread.sleep(10);
                }
            } catch (Exception e) {
                exceptions.add(e);
            } finally {
                latch.countDown();
            }
        });

        // Thread 2: Apply delays
        executor.submit(() -> {
            try {
                for (int i = 0; i < 5; i++) {
                    Doctor d = InMemoryStore.doctors.get("D1");
                    if (d != null) {
                        d.applyDelay(0.9);
                    }
                    Thread.sleep(10);
                }
            } catch (Exception e) {
                exceptions.add(e);
            } finally {
                latch.countDown();
            }
        });

        // Thread 3: Read slots
        executor.submit(() -> {
            try {
                for (int i = 0; i < 5; i++) {
                    Doctor d = InMemoryStore.doctors.get("D1");
                    if (d != null) {
                        int slotCount = d.getSlots().size();
                        assertTrue(slotCount > 0);
                    }
                    Thread.sleep(10);
                }
            } catch (Exception e) {
                exceptions.add(e);
            } finally {
                latch.countDown();
            }
        });

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        // Verify no ConcurrentModificationException or other exceptions occurred
        if (!exceptions.isEmpty()) {
            Exception firstException = exceptions.peek();
            fail("Concurrent modification exception occurred: " + firstException.getMessage());
        }
    }

    @Test
    void testConcurrentTokenCancellation() throws InterruptedException, ExecutionException {
        // Allocate some tokens first
        List<String> tokenIds = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            AllocationResponseDTO response = allocationService.createToken(
                "D1", "9-10", "P" + i, TokenSource.ONLINE
            );
            tokenIds.add(response.tokenId);
        }

        ExecutorService executor = Executors.newFixedThreadPool(5);
        List<Future<Boolean>> futures = new ArrayList<>();

        // Cancel tokens concurrently
        for (String tokenId : tokenIds) {
            Future<Boolean> future = executor.submit(() -> 
                allocationService.cancelPatientToken("D1", "9-10", tokenId)
            );
            futures.add(future);
        }

        // Wait for all cancellations
        List<Boolean> results = new ArrayList<>();
        for (Future<Boolean> future : futures) {
            results.add(future.get());
        }

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        // Verify all cancellations succeeded
        long successfulCancellations = results.stream().filter(r -> r).count();
        assertEquals(5, successfulCancellations);
    }
}
