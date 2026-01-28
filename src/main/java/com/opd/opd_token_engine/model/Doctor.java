package com.opd.opd_token_engine.model;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Doctor {

    private String doctorId;
    private volatile double efficiencyScore;
    private Map<String, TimeSlot> slots = new ConcurrentHashMap<>();

    public Doctor(String doctorId, double efficiencyScore) {
        this.doctorId = doctorId;
        this.efficiencyScore = efficiencyScore;
    }

    public synchronized void addSlot(String slotId, int baseCapacity) {
        int effectiveCapacity = (int) (baseCapacity * efficiencyScore);
        slots.put(slotId, new TimeSlot(slotId, effectiveCapacity));
    }

    public synchronized void applyDelay(double delayFactor) {
        this.efficiencyScore *= delayFactor;

        for (TimeSlot slot : slots.values()) {
            int newCapacity = (int) (slot.getBaseCapacity() * efficiencyScore);
            slot.updateCapacity(newCapacity);
        }
    }

    public Map<String, TimeSlot> getSlots() {
        return slots;
    }
}
