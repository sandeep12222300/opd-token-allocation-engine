package com.opd.opd_token_engine.service;

import com.opd.opd_token_engine.dto.AllocationResponseDTO;
import com.opd.opd_token_engine.engine.AllocationEngine;
import com.opd.opd_token_engine.model.Token;
import com.opd.opd_token_engine.model.TokenSource;
import com.opd.opd_token_engine.repository.InMemoryStore;
import org.springframework.stereotype.Service;

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

        String result = AllocationEngine.allocate(slot, token);

        String reason = switch (result) {
            case "ALLOCATED" ->
                    "Token allocated successfully within slot capacity";

            case "WAITLISTED" ->
                    "Slot is full; token added to waiting queue";

            case "REALLOCATED_LOW_PRIORITY" ->
                    "Lower-priority token was reallocated to the waiting queue based on fairness rules";

            default ->
                    "Allocation decision applied";
        };

        return new AllocationResponseDTO(
                token.getTokenId(),
                result,
                reason
        );
    }


    public boolean cancelPatientToken(
            String doctorId,
            String slotId,
            String tokenId
    ) {
        var slot = InMemoryStore.doctors
                .get(doctorId)
                .getSlots()
                .get(slotId);

        boolean removed = slot.getAllocatedTokens()
                .removeIf(token -> token.getTokenId().equals(tokenId));

        if (removed && !slot.getWaitingQueue().isEmpty()) {
            slot.getAllocatedTokens().add(slot.getWaitingQueue().poll());
        }

        return removed;
    }

}


