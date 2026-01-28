package com.opd.opd_token_engine.engine;

import com.opd.opd_token_engine.model.Token;
import java.time.Duration;
import java.time.LocalDateTime;


public class PriorityCalculator {


    private static final double AGING_FACTOR = 0.3;
    private static final int REALLOCATION_PENALTY  = 10;

    public static int calculate(Token token) {
        long waitingMinutes =
                Duration.between(token.getCreatedAt(), LocalDateTime.now()).toMinutes();

        int effectivePriority = (int) (
                token.getBasePriority()
                        + waitingMinutes * AGING_FACTOR
                        - token.getReallocationCount() * REALLOCATION_PENALTY
        );

        System.out.println(
                "Token " + token.getTokenId() +
                        " | Waiting(min): " + waitingMinutes +
                        " | Effective Priority: " + effectivePriority
        );

        return effectivePriority;
    }
}
