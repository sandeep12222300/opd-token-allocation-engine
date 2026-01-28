package com.opd.opd_token_engine.dto;

import com.opd.opd_token_engine.model.TokenSource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class TokenRequestDTO {
    @NotBlank(message = "Doctor ID is required")
    public String doctorId;
    
    @NotBlank(message = "Slot ID is required")
    public String slotId;
    
    @NotBlank(message = "Patient ID is required")
    public String patientId;
    
    @NotNull(message = "Token source is required")
    public TokenSource source;
}
