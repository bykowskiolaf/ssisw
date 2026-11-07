package dev.bykowski.ssisw.market;

public record ReturnResult(
        boolean accepted,
        Transaction transaction,
        long returnedAtEpochMillis,
        String message
) {
}
