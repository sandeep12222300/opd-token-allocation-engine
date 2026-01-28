package com.opd.opd_token_engine.engine;

public class AllocationResult {
    public final String status;
    public final String evictedTokenId; // token moved to waiting queue due to preemption (if any)

    public AllocationResult(String status, String evictedTokenId) {
        this.status = status;
        this.evictedTokenId = evictedTokenId;
    }
}
