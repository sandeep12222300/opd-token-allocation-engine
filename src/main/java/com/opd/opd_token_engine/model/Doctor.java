package com.opd.opd_token_engine.model;

import java.util.HashMap;
import java.util.Map;

public class Doctor {

    private String doctorId;
    private double efficiencyScore;
    private Map<String, TimeSlot> slots = new HashMap<>();

    public Doctor(String doctorId, double efficiencyScore) {
        this.doctorId = doctorId;
        this.efficiencyScore = efficiencyScore;
    }

    public void addSlot(String slotId, int baseCapacity) {
        int effectiveCapacity = (int) (baseCapacity * efficiencyScore);
        slots.put(slotId, new TimeSlot(slotId, effectiveCapacity));
    }

    public void applyDelay(double delayFactor) {
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
