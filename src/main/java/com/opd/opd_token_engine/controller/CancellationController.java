package com.opd.opd_token_engine.controller;

import com.opd.opd_token_engine.service.AllocationService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tokens")
public class CancellationController {

    private final AllocationService service;

    public CancellationController(AllocationService service) {
        this.service = service;
    }

    @PostMapping("/cancel")
    public String cancelByPatient(
            @RequestParam String doctorId,
            @RequestParam String slotId,
            @RequestParam String tokenId
    ) {
        boolean cancelled =
                service.cancelPatientToken(doctorId, slotId, tokenId);

        if (cancelled) {
            return "Token cancelled successfully by patient";
        } else {
            return "Token not found or already cancelled";
        }
    }
}
