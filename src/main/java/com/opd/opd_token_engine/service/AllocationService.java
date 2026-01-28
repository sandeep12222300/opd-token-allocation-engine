package com.opd.opd_token_engine.service;

import com.opd.opd_token_engine.dto.AllocationResponseDTO;
import com.opd.opd_token_engine.engine.AllocationEngine;
import com.opd.opd_token_engine.engine.AllocationResult;
import com.opd.opd_token_engine.model.Token;
import com.opd.opd_token_engine.model.TokenSource;
import com.opd.opd_token_engine.repository.InMemoryStore;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AllocationService {


    public AllocationResponseDTO createToken(
            String doctorId,
            String slotId,
            String patientId,
            TokenSource source
    ) {

        int basePriority = switch (source) {
            case EMERGENCY -> 100;
            case PAID -> 85;
            case FOLLOW_UP -> 65;
            case ONLINE -> 50;
            case WALK_IN -> 40;
        };

        Token token = new Token(patientId, source, basePriority);

        var doctor = InMemoryStore.doctors.get(doctorId);
        if (doctor == null) {
            return new AllocationResponseDTO(null, "ERROR", "Doctor not found");
        }

        var slot = doctor.getSlots().get(slotId);
        if (slot == null) {
            return new AllocationResponseDTO(null, "ERROR", "Slot not found");
        }

        AllocationResult allocResult = AllocationEngine.allocate(doctorId, slot, token);

        String reason = switch (allocResult.status) {
            case "ALLOCATED" ->
                    "Token allocated successfully within slot capacity";

            case "WAITLISTED" ->
                    "Slot is full; token added to waiting queue";

            case "REALLOCATED_LOW_PRIORITY" ->
                    "Lower-priority token was reallocated to the waiting queue based on fairness rules";

            default ->
                    "Allocation decision applied";
        };
        Integer position = null;
        if ("WAITLISTED".equals(allocResult.status)) {
            synchronized (slot) {
                // Note: PriorityQueue iterator does not guarantee priority order
                // Position calculation is approximate and for informational purposes only
                List<Token> waiting = new ArrayList<>(slot.getWaitingQueue());
                for (int i = 0; i < waiting.size(); i++) {
                    if (waiting.get(i).getTokenId().equals(token.getTokenId())) {
                        position = i + 1; // 1-based position (approximate)
                        break;
                    }
                }
            }
        }

        return new AllocationResponseDTO(
                token.getTokenId(),
                allocResult.status,
                reason,
                allocResult.evictedTokenId,
                position
        );
    }


    public AllocationResponseDTO createEmergencyToken(String doctorId, String slotId, String patientId) {
        return createToken(doctorId, slotId, patientId, TokenSource.EMERGENCY);
    }

    public boolean cancelPatientToken(
            String doctorId,
            String slotId,
            String tokenId
    ) {
        var doctor = InMemoryStore.doctors.get(doctorId);
        if (doctor == null) {
            return false;
        }

        var slot = doctor.getSlots().get(slotId);
        if (slot == null) {
            return false;
        }

        synchronized (slot) {
            boolean removed = slot.getAllocatedTokens()
                    .removeIf(token -> token.getTokenId().equals(tokenId));

            if (removed && !slot.getWaitingQueue().isEmpty()) {
                slot.getAllocatedTokens().add(slot.getWaitingQueue().poll());
            }

            return removed;
        }
    }

}
