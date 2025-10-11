package com.icpizza.backend.enums;

public enum WorkLoadLevel {
    IDLE(0), BUSY(1), CROWDED(2), OVERLOADED(3);

    private final int priority;

    WorkLoadLevel(int priority) {
        this.priority = priority;
    }

    public boolean isHigherThan(WorkLoadLevel other) {
        return this.priority > other.priority;
    }
}
