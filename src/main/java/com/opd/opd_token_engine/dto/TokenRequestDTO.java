package com.opd.opd_token_engine.dto;

import com.opd.opd_token_engine.model.TokenSource;

public class TokenRequestDTO {
    public String doctorId;
    public String slotId;
    public String patientId;
    public TokenSource source;
}
