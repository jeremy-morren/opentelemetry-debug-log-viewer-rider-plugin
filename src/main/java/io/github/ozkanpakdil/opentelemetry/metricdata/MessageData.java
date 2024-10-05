package io.github.ozkanpakdil.opentelemetry.metricdata;

import java.util.HashMap;

public class MessageData implements ITelemetryData {
    public String severityLevel;
    public String message;
    public HashMap<String, String> properties;

    @Override
    public HashMap<String, String> getProperties() {
        return properties;
    }
}
