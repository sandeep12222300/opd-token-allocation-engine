package com.opd.opd_token_engine.controller;

import com.opd.opd_token_engine.dto.AllocationResponseDTO;
import com.opd.opd_token_engine.dto.TokenRequestDTO;
import com.opd.opd_token_engine.service.AllocationService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tokens")
public class TokenController {

    private final AllocationService service;

    public TokenController(AllocationService service) {
        this.service = service;
    }

    @PostMapping
    public AllocationResponseDTO createToken(
            @RequestBody TokenRequestDTO request) {

        return service.createToken(
                request.doctorId,
                request.slotId,
                request.patientId,
                request.source
        );
    }
}


