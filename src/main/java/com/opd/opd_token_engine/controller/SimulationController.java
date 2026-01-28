package com.opd.opd_token_engine.controller;

import com.opd.opd_token_engine.dto.AllocationResponseDTO;
import com.opd.opd_token_engine.model.TokenSource;
import com.opd.opd_token_engine.service.AllocationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/simulation")
public class SimulationController {

    private final AllocationService allocationService;

    public SimulationController(AllocationService allocationService) {
        this.allocationService = allocationService;
    }

    @GetMapping("/opd-day")
    public Map<String, Object> simulateOpdDay() {
        List<Map<String, Object>> events = new ArrayList<>();
        
        // Morning session - 9-10 AM slot
        System.out.println("\n=== OPD Day Simulation Started ===");
        System.out.println("\n--- 9:00 AM - Morning Session Begins ---");
        
        // Event 1: Emergency patient arrives at D1
        AllocationResponseDTO r1 = allocationService.createToken("D1", "9-10", "P001", TokenSource.EMERGENCY);
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
        AllocationResponseDTO r2 = allocationService.createToken("D1", "9-10", "P002", TokenSource.PAID);
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
        AllocationResponseDTO r3 = allocationService.createToken("D2", "9-10", "P003", TokenSource.ONLINE);
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
        AllocationResponseDTO r4 = allocationService.createToken("D3", "9-10", "P004", TokenSource.WALK_IN);
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
        AllocationResponseDTO r5 = allocationService.createToken("D1", "9-10", "P005", TokenSource.FOLLOW_UP);
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
            AllocationResponseDTO r = allocationService.createToken("D1", "9-10", "P00" + i, TokenSource.ONLINE);
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
        AllocationResponseDTO r9 = allocationService.createToken("D1", "9-10", "P009", TokenSource.EMERGENCY);
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
        
        System.out.println("\n--- 10:00 AM - Second Session Begins ---");
        
        // Second slot - 10-11 AM
        AllocationResponseDTO r10 = allocationService.createToken("D2", "10-11", "P010", TokenSource.PAID);
        events.add(Map.of(
            "time", "10:00 AM",
            "patient", "P010",
            "doctor", "D2",
            "slot", "10-11",
            "source", "PAID",
            "result", r10.status,
            "tokenId", r10.tokenId != null ? r10.tokenId : "N/A"
        ));
        
        AllocationResponseDTO r11 = allocationService.createToken("D3", "10-11", "P011", TokenSource.WALK_IN);
        events.add(Map.of(
            "time", "10:05 AM",
            "patient", "P011",
            "doctor", "D3",
            "slot", "10-11",
            "source", "WALK_IN",
            "result", r11.status,
            "tokenId", r11.tokenId != null ? r11.tokenId : "N/A"
        ));
        
        AllocationResponseDTO r12 = allocationService.createToken("D1", "10-11", "P012", TokenSource.FOLLOW_UP);
        events.add(Map.of(
            "time", "10:10 AM",
            "patient", "P012",
            "doctor", "D1",
            "slot", "10-11",
            "source", "FOLLOW_UP",
            "result", r12.status,
            "tokenId", r12.tokenId != null ? r12.tokenId : "N/A"
        ));
        
        System.out.println("\n=== OPD Day Simulation Completed ===\n");
        
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
}
