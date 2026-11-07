package dev.bykowski.ssisw.market;

import java.util.List;

public record NegotiationResult(
        boolean accepted,
        String carId,
        long catalogTotalPln,
        long minimumTotalPln,
        long agreedTotalPln,
        int rounds,
        List<NegotiationRound> history,
        String message
) {
}
