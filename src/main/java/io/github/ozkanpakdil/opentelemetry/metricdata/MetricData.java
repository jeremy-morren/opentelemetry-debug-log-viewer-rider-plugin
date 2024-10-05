package io.github.ozkanpakdil.opentelemetry.metricdata;

import java.util.ArrayList;
import java.util.HashMap;

public class MetricData implements ITelemetryData {

    public static class Metric {
        public String name;
        public String kind;
        public int value;
        public int count;
    }

    public ArrayList<Metric> metrics;
    public HashMap<String, String> properties;

    @Override
    public HashMap<String, String> getProperties() {
        return properties;
    }
}
