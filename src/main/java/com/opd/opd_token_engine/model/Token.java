package com.opd.opd_token_engine.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class Token {

    private String tokenId = UUID.randomUUID().toString();
    private String patientId;
    private TokenSource source;
    private int basePriority;
    private int reallocationCount;
    private LocalDateTime createdAt;
    private boolean allocated;
    private int snapshotPriority; // Priority at time of insertion into queue

    public Token(String patientId, TokenSource source, int basePriority) {
        this.patientId = patientId;
        this.source = source;
        this.basePriority = basePriority;
        this.createdAt = LocalDateTime.now();
        this.allocated = false;
        this.snapshotPriority = basePriority;
    }

    public String getTokenId() { return tokenId; }
    public TokenSource getSource() { return source; }
    public int getBasePriority() { return basePriority; }
    public int getReallocationCount() { return reallocationCount; }
    public void incrementPreemption() { this.reallocationCount++; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public boolean isAllocated() { return allocated; }
    public void setAllocated(boolean allocated) { this.allocated = allocated; }
    public int getSnapshotPriority() { return snapshotPriority; }
    public void setSnapshotPriority(int priority) { this.snapshotPriority = priority; }
}
