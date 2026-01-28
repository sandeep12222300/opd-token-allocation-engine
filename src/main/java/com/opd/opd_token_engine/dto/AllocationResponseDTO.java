package com.opd.opd_token_engine.dto;

public class AllocationResponseDTO {
    public String tokenId;
    public String status;
    public String reason;

    public AllocationResponseDTO(String tokenId, String status, String reason) {
        this.tokenId = tokenId;
        this.status = status;
        this.reason = reason;
    }
}
