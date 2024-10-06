package io.github.ozkanpakdil.opentelemetry;

public enum TelemetryType {
    Message(),
    Request(),
    Exception(),
    Duration(),
    Unk();

    TelemetryType() {
    }

}
