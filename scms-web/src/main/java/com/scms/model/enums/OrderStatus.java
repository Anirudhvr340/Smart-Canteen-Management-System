package com.scms.model.enums;

public enum OrderStatus {
    CREATED {
        @Override public boolean canTransitionTo(OrderStatus n) { return n==CONFIRMED||n==CANCELLED; }
    },
    CONFIRMED {
        @Override public boolean canTransitionTo(OrderStatus n) { return n==PREPARING||n==CANCELLED; }
    },
    PREPARING {
        @Override public boolean canTransitionTo(OrderStatus n) { return n==READY; }
    },
    READY {
        @Override public boolean canTransitionTo(OrderStatus n) { return n==COMPLETED; }
    },
    COMPLETED {
        @Override public boolean canTransitionTo(OrderStatus n) { return false; }
    },
    CANCELLED {
        @Override public boolean canTransitionTo(OrderStatus n) { return n==REFUNDED; }
    },
    REFUNDED {
        @Override public boolean canTransitionTo(OrderStatus n) { return false; }
    };
    public abstract boolean canTransitionTo(OrderStatus next);
}
