package com.opd.opd_token_engine.engine;

import com.opd.opd_token_engine.model.TimeSlot;
import com.opd.opd_token_engine.model.Token;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class AllocationEngine {

    private static final Logger log = LoggerFactory.getLogger(AllocationEngine.class);

    public static AllocationResult allocate(String doctorId, TimeSlot slot, Token token) {
        synchronized (slot) {
            if (slot.getAllocatedTokens().size() < slot.getCapacity()) {
                token.setAllocated(true);
                slot.getAllocatedTokens().add(token);
                log.info("ALLOCATED → Token added [Doctor={}, Slot={}, Token={}]", doctorId, slot.getSlotId(), token.getTokenId());
                return new AllocationResult("ALLOCATED", null);
            }

            Token lowest = slot.getAllocatedTokens().peek();
            if (lowest == null) {
                // handle unexpected empty queue defensively
                token.setAllocated(true);
                slot.getAllocatedTokens().add(token);
                log.info("ALLOCATED (defensive) → Token added [Doctor={}, Slot={}, Token={}]", doctorId, slot.getSlotId(), token.getTokenId());
                return new AllocationResult("ALLOCATED", null);
            }

            if (PriorityCalculator.calculate(token) > PriorityCalculator.calculate(lowest)) {
                slot.getAllocatedTokens().poll();
                lowest.incrementPreemption();
                lowest.setAllocated(false);
                slot.getWaitingQueue().add(lowest);

                token.setAllocated(true);
                slot.getAllocatedTokens().add(token);

                log.info("REALLOCATED → Lower-priority token moved [Doctor={}, Slot={}, TokenEvicted={}, TokenIn={}]", doctorId, slot.getSlotId(), lowest.getTokenId(), token.getTokenId());
                return new AllocationResult("REALLOCATED_LOW_PRIORITY", lowest.getTokenId());
            }

            slot.getWaitingQueue().add(token);
            log.info("WAITLISTED → Token added to waiting queue [Doctor={}, Slot={}, Token={}]", doctorId, slot.getSlotId(), token.getTokenId());
            return new AllocationResult("WAITLISTED", null);
        }
    }
}
