package com.opd.opd_token_engine.engine;

import com.opd.opd_token_engine.model.TimeSlot;
import com.opd.opd_token_engine.model.Token;
import com.opd.opd_token_engine.engine.PriorityCalculator;



public class AllocationEngine {


    public static String allocate(TimeSlot slot, Token token) {

        if (slot.getAllocatedTokens().size() < slot.getCapacity()) {
            token.setAllocated(true);
            slot.getAllocatedTokens().add(token);
            System.out.println("ALLOCATED → Token added");
            System.out.println("Allocated tokens: " + slot.getAllocatedTokens().size());
            System.out.println("Waiting queue: " + slot.getWaitingQueue().size());
            return "ALLOCATED";
        }

        Token lowest = slot.getAllocatedTokens().peek();

        if (PriorityCalculator.calculate(token)
                > PriorityCalculator.calculate(lowest)) {

            slot.getAllocatedTokens().poll();
            lowest.incrementPreemption();
            lowest.setAllocated(false);
            slot.getWaitingQueue().add(lowest);

            token.setAllocated(true);
            slot.getAllocatedTokens().add(token);

            System.out.println("REALLOCATED → Lower-priority token moved");
            System.out.println("Allocated tokens: " + slot.getAllocatedTokens().size());
            System.out.println("Waiting queue: " + slot.getWaitingQueue().size());


            return "REALLOCATED_LOW_PRIORITY";
        }

        slot.getWaitingQueue().add(token);

        System.out.println("WAITLISTED → Token added to waiting queue");
        System.out.println("Allocated tokens: " + slot.getAllocatedTokens().size());
        System.out.println("Waiting queue: " + slot.getWaitingQueue().size());

        return "WAITLISTED";
    }
}
