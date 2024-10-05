package io.github.ozkanpakdil.opentelemetry.metricdata;

import java.util.HashMap;

public class UnkData implements ITelemetryData {
    public HashMap<String, String> properties;

    @Override
    public HashMap<String, String> getProperties() {
        return properties;
    }
}
