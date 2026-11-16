package dev.bykowski.ssisw.simulation;

public record DemoPacing(long startupDelayMillis, long cycleDelayMillis) {

    public DemoPacing {
        if (startupDelayMillis < 0 || cycleDelayMillis < 0) {
            throw new IllegalArgumentException("Demo delays cannot be negative");
        }
    }

    public static DemoPacing forMode(boolean enabled, long startupDelayMillis, long cycleDelayMillis) {
        return enabled ? new DemoPacing(startupDelayMillis, cycleDelayMillis) : new DemoPacing(0, 0);
    }
}
