package com.opd.opd_token_engine.dto;

public class AllocationResponseDTO {
    public String tokenId;
    public String status;
    public String reason;
    public String promotedTokenId; // ID of token moved to waiting queue due to preemption
    public Integer positionInQueue; // if waitlisted, the position

    public AllocationResponseDTO(String tokenId, String status, String reason) {
        this.tokenId = tokenId;
        this.status = status;
        this.reason = reason;
    }

    public AllocationResponseDTO(String tokenId, String status, String reason, String promotedTokenId, Integer positionInQueue) {
        this.tokenId = tokenId;
        this.status = status;
        this.reason = reason;
        this.promotedTokenId = promotedTokenId;
        this.positionInQueue = positionInQueue;
    }
}
