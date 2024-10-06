package io.github.ozkanpakdil.opentelemetry;

import io.github.ozkanpakdil.opentelemetry.metricdata.ITelemetryData;
import io.github.ozkanpakdil.opentelemetry.utils.TimeSpan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class Telemetry {
    @NotNull
    private final TelemetryType type;
    @NotNull
    private final String json;
    @Nullable
    private String lowerCaseJson;
    @NotNull
    private final Date timestamp;
    private final TelemetryFactory.ActivityInfo data;
    private final Map<String, String> tags;
    @Nullable
    private String filteredBy;
    private boolean unconfigured;

    public Telemetry(
            @NotNull TelemetryType type,
            @NotNull String json,
            TelemetryFactory.ActivityInfo data,
            Map<String, String> tags
    ) {
        this.type = type;
        this.json = json;
        this.data = data;
        this.tags = tags;
        TemporalAccessor ta = DateTimeFormatter.ISO_INSTANT.parse(data.getStartTime());
        Instant i = Instant.from(ta);
        timestamp = Date.from(i);
    }

    @NotNull
    public TelemetryType getType() {
        return type;
    }

    @NotNull
    public Date getTimestamp() {
        return timestamp;
    }

    @NotNull
    public String getJson() {
        return json;
    }

    @NotNull
    public String getLowerCaseJson() {
        if (lowerCaseJson == null)
            lowerCaseJson = json.toLowerCase(Locale.ROOT);
        return lowerCaseJson;
    }

    public <T extends ITelemetryData> T getData(Class<T> clazz) {
        return (T) data;
    }

    public TelemetryFactory.ActivityInfo getData() {
        return data;
    }

    public void setFilteredBy(@Nullable String filteredBy) {
        this.filteredBy = filteredBy;
    }

    @Nullable
    public String getFilteredBy() {
        return filteredBy;
    }

    public void setUnConfigured() {
        unconfigured = true;
    }

    public boolean isUnconfigured() {
        return unconfigured;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    public TimeSpan getDuration() {
        return new TimeSpan(data.getDuration());
    }

    @Override
    public String toString() {
        return data.getDisplayName() + data.getTags().getOrDefault("url.path", "");
    }
}
