package com.opd.opd_token_engine.model;

import java.util.PriorityQueue;

public class TimeSlot {

    private String slotId;


    private int baseCapacity;


    private int capacity;

    private PriorityQueue<Token> allocatedTokens;
    private PriorityQueue<Token> waitingQueue;

    public TimeSlot(String slotId, int baseCapacity) {
        this.slotId = slotId;
        this.baseCapacity = baseCapacity;
        this.capacity = baseCapacity;

        // Use snapshot priority for stable ordering
        this.allocatedTokens = new PriorityQueue<>(
                (a, b) -> Integer.compare(a.getSnapshotPriority(), b.getSnapshotPriority())
        );

        this.waitingQueue = new PriorityQueue<>(
                (a, b) -> Integer.compare(b.getSnapshotPriority(), a.getSnapshotPriority())
        );
    }


    public String getSlotId() { return slotId; }

    public int getCapacity() {
        return capacity;
    }

    public int getBaseCapacity() {
        return baseCapacity;
    }

    public void updateCapacity(int newCapacity) {
        this.capacity = Math.max(0, newCapacity);
    }


    public PriorityQueue<Token> getAllocatedTokens() {
        return allocatedTokens;
    }

    public PriorityQueue<Token> getWaitingQueue() {
        return waitingQueue;
    }
}
