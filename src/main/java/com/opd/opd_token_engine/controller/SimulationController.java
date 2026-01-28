package com.opd.opd_token_engine.controller;

import com.opd.opd_token_engine.dto.AllocationResponseDTO;
import com.opd.opd_token_engine.model.TokenSource;
import com.opd.opd_token_engine.service.AllocationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/simulation")
public class SimulationController {

    private static final Logger log = LoggerFactory.getLogger(SimulationController.class);

    private final AllocationService allocationService;

    public SimulationController(AllocationService allocationService) {
        this.allocationService = allocationService;
    }

    @PostMapping("/opd-day")
    public Map<String, Object> simulateOpdDay() {
        List<Map<String, Object>> events = new ArrayList<>();

        // Morning session - 9-10 AM slot
        log.info("=== OPD Day Simulation Started ===");
        log.info("--- 9:00 AM - Morning Session Begins ---");

        // Event 1: Emergency patient arrives at D1
        AllocationResponseDTO r1 = safeCreateToken("9:00 AM", "P001", "D1", "9-10", TokenSource.EMERGENCY);
        events.add(Map.of(
                "time", "9:00 AM",
                "patient", "P001",
                "doctor", "D1",
                "slot", "9-10",
                "source", "EMERGENCY",
                "result", r1.status,
                "tokenId", r1.tokenId != null ? r1.tokenId : "N/A"
        ));

        // Event 2: Paid patient at D1
        AllocationResponseDTO r2 = safeCreateToken("9:05 AM", "P002", "D1", "9-10", TokenSource.PAID);
        events.add(Map.of(
                "time", "9:05 AM",
                "patient", "P002",
                "doctor", "D1",
                "slot", "9-10",
                "source", "PAID",
                "result", r2.status,
                "tokenId", r2.tokenId != null ? r2.tokenId : "N/A"
        ));

        // Event 3: Online booking at D2
        AllocationResponseDTO r3 = safeCreateToken("9:10 AM", "P003", "D2", "9-10", TokenSource.ONLINE);
        events.add(Map.of(
                "time", "9:10 AM",
                "patient", "P003",
                "doctor", "D2",
                "slot", "9-10",
                "source", "ONLINE",
                "result", r3.status,
                "tokenId", r3.tokenId != null ? r3.tokenId : "N/A"
        ));

        // Event 4: Walk-in at D3
        AllocationResponseDTO r4 = safeCreateToken("9:15 AM", "P004", "D3", "9-10", TokenSource.WALK_IN);
        events.add(Map.of(
                "time", "9:15 AM",
                "patient", "P004",
                "doctor", "D3",
                "slot", "9-10",
                "source", "WALK_IN",
                "result", r4.status,
                "tokenId", r4.tokenId != null ? r4.tokenId : "N/A"
        ));

        // Event 5: Follow-up at D1
        AllocationResponseDTO r5 = safeCreateToken("9:20 AM", "P005", "D1", "9-10", TokenSource.FOLLOW_UP);
        events.add(Map.of(
                "time", "9:20 AM",
                "patient", "P005",
                "doctor", "D1",
                "slot", "9-10",
                "source", "FOLLOW_UP",
                "result", r5.status,
                "tokenId", r5.tokenId != null ? r5.tokenId : "N/A"
        ));

        // Fill D1 to capacity (capacity = 6)
        for (int i = 6; i <= 8; i++) {
            AllocationResponseDTO r = safeCreateToken("9:" + (20 + (i - 6) * 5) + " AM", "P00" + i, "D1", "9-10", TokenSource.ONLINE);
            events.add(Map.of(
                    "time", "9:" + (20 + (i-6)*5) + " AM",
                    "patient", "P00" + i,
                    "doctor", "D1",
                    "slot", "9-10",
                    "source", "ONLINE",
                    "result", r.status,
                    "tokenId", r.tokenId != null ? r.tokenId : "N/A"
            ));
        }

        // Event: New emergency arrives when D1 is full
        AllocationResponseDTO r9 = safeCreateToken("9:40 AM", "P009", "D1", "9-10", TokenSource.EMERGENCY);
        events.add(Map.of(
                "time", "9:40 AM",
                "patient", "P009",
                "doctor", "D1",
                "slot", "9-10",
                "source", "EMERGENCY",
                "result", r9.status,
                "tokenId", r9.tokenId != null ? r9.tokenId : "N/A",
                "note", "Slot was full - check if reallocation occurred"
        ));

        log.info("--- 10:00 AM - Second Session Begins ---");

        // Second slot - 10-11 AM
        AllocationResponseDTO r10 = safeCreateToken("10:00 AM", "P010", "D2", "10-11", TokenSource.PAID);
        events.add(Map.of(
                "time", "10:00 AM",
                "patient", "P010",
                "doctor", "D2",
                "slot", "10-11",
                "source", "PAID",
                "result", r10.status,
                "tokenId", r10.tokenId != null ? r10.tokenId : "N/A"
        ));

        AllocationResponseDTO r11 = safeCreateToken("10:05 AM", "P011", "D3", "10-11", TokenSource.WALK_IN);
        events.add(Map.of(
                "time", "10:05 AM",
                "patient", "P011",
                "doctor", "D3",
                "slot", "10-11",
                "source", "WALK_IN",
                "result", r11.status,
                "tokenId", r11.tokenId != null ? r11.tokenId : "N/A"
        ));

        AllocationResponseDTO r12 = safeCreateToken("10:10 AM", "P012", "D1", "10-11", TokenSource.FOLLOW_UP);
        events.add(Map.of(
                "time", "10:10 AM",
                "patient", "P012",
                "doctor", "D1",
                "slot", "10-11",
                "source", "FOLLOW_UP",
                "result", r12.status,
                "tokenId", r12.tokenId != null ? r12.tokenId : "N/A"
        ));

        log.info("=== OPD Day Simulation Completed ===");

        // Summary statistics
        long allocated = events.stream().filter(e -> "ALLOCATED".equals(e.get("result"))).count();
        long reallocated = events.stream().filter(e -> "REALLOCATED_LOW_PRIORITY".equals(e.get("result"))).count();
        long waitlisted = events.stream().filter(e -> "WAITLISTED".equals(e.get("result"))).count();

        Map<String, Object> summary = Map.of(
                "totalRequests", events.size(),
                "allocated", allocated,
                "reallocated", reallocated,
                "waitlisted", waitlisted,
                "doctors", Map.of(
                        "D1", Map.of("efficiency", 1.2, "slots", List.of("9-10", "10-11"), "effectiveCapacity", "6 per slot"),
                        "D2", Map.of("efficiency", 1.0, "slots", List.of("9-10", "10-11"), "effectiveCapacity", "4 per slot"),
                        "D3", Map.of("efficiency", 0.8, "slots", List.of("9-10", "10-11"), "effectiveCapacity", "2-3 per slot")
                )
        );

        return Map.of(
                "simulationName", "OPD Day with 3 Doctors",
                "events", events,
                "summary", summary,
                "notes", List.of(
                        "Simulation demonstrates multi-doctor OPD day",
                        "Emergency patients may preempt lower priority tokens",
                        "Waiting queue automatically manages overflow",
                        "Each doctor has different efficiency affecting capacity"
                )
        );
    }

    // Helper to call allocationService safely and return an AllocationResponseDTO; caller records the event
    private AllocationResponseDTO safeCreateToken(String time, String patientId, String doctorId, String slotId, TokenSource source) {
        try {
            return allocationService.createToken(doctorId, slotId, patientId, source);
        } catch (Exception ex) {
            log.error("Allocation failed for patient {} at {} {}:{}", patientId, time, doctorId, slotId, ex);
            return new AllocationResponseDTO(null, "ERROR", ex.getMessage());
        }
    }

}

