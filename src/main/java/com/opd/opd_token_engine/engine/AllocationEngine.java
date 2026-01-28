package com.opd.opd_token_engine.engine;

import com.opd.opd_token_engine.model.TimeSlot;
import com.opd.opd_token_engine.model.Token;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class AllocationEngine {

    private static final Logger log = LoggerFactory.getLogger(AllocationEngine.class);

    public static AllocationResult allocate(String doctorId, TimeSlot slot, Token token) {
        synchronized (slot) {
            // Calculate effective priority before adding to queue
            int tokenPriority = PriorityCalculator.calculate(token);
            token.setSnapshotPriority(tokenPriority);
            
            if (slot.getAllocatedTokens().size() < slot.getCapacity()) {
                token.setAllocated(true);
                slot.getAllocatedTokens().add(token);
                log.info("ALLOCATED → Token added [Doctor={}, Slot={}, Token={}, Priority={}]", 
                    doctorId, slot.getSlotId(), token.getTokenId(), tokenPriority);
                return new AllocationResult("ALLOCATED", null);
            }

            Token lowest = slot.getAllocatedTokens().peek();
            if (lowest == null) {
                // This should not happen if slot is at capacity, but handle defensively
                token.setAllocated(true);
                slot.getAllocatedTokens().add(token);
                log.warn("ALLOCATED (defensive) → Empty queue at capacity [Doctor={}, Slot={}, Token={}]", 
                    doctorId, slot.getSlotId(), token.getTokenId());
                return new AllocationResult("ALLOCATED", null);
            }

            if (tokenPriority > lowest.getSnapshotPriority()) {
                slot.getAllocatedTokens().poll();
                lowest.incrementPreemption();
                lowest.setAllocated(false);
                
                // Recalculate priority for evicted token before adding to waiting queue
                int evictedPriority = PriorityCalculator.calculate(lowest);
                lowest.setSnapshotPriority(evictedPriority);
                slot.getWaitingQueue().add(lowest);

                token.setAllocated(true);
                slot.getAllocatedTokens().add(token);

                log.info("REALLOCATED → Lower-priority token moved [Doctor={}, Slot={}, TokenEvicted={} (Priority={}), TokenIn={} (Priority={})]", 
                    doctorId, slot.getSlotId(), lowest.getTokenId(), evictedPriority, token.getTokenId(), tokenPriority);
                return new AllocationResult("REALLOCATED_LOW_PRIORITY", lowest.getTokenId());
            }

            slot.getWaitingQueue().add(token);
            log.info("WAITLISTED → Token added to waiting queue [Doctor={}, Slot={}, Token={}, Priority={}]", 
                doctorId, slot.getSlotId(), token.getTokenId(), tokenPriority);
            return new AllocationResult("WAITLISTED", null);
        }
    }
}
