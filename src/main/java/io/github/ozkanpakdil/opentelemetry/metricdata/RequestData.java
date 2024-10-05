package io.github.ozkanpakdil.opentelemetry.metricdata;

import java.util.HashMap;

public class RequestData implements ITelemetryData {
    public String id;
    public String name;
    public String duration;
    public boolean success;
    public String responseCode;
    public String url;
    public HashMap<String, String> properties;

    @Override
    public HashMap<String, String> getProperties() {
        return properties;
    }
}
