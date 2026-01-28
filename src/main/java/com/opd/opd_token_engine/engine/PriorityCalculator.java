package com.opd.opd_token_engine.engine;

import com.opd.opd_token_engine.model.Token;
import java.time.Duration;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class PriorityCalculator {

    private static final Logger log = LoggerFactory.getLogger(PriorityCalculator.class);

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
        log.info("Token {} | Waiting(min): {} | Effective Priority: {}", token.getTokenId(), waitingMinutes, effectivePriority);

        return effectivePriority;
    }
}
