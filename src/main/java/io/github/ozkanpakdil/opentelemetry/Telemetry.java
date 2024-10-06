package io.github.ozkanpakdil.opentelemetry;

import io.github.ozkanpakdil.opentelemetry.utils.TimeSpan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.Locale;

public class Telemetry {
    @NotNull
    private final String json;
    @Nullable
    private String lowerCaseJson;
    @NotNull
    private final Date timestamp;
    private final TelemetryFactory.ActivityInfo data;
    @Nullable
    private String filteredBy;

    public Telemetry(
            @NotNull String json,
            TelemetryFactory.ActivityInfo data
    ) {
        this.json = json;
        this.data = data;
        TemporalAccessor ta = DateTimeFormatter.ISO_INSTANT.parse(data.getStartTime());
        Instant i = Instant.from(ta);
        timestamp = Date.from(i);
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
    }

    public TimeSpan getDuration() {
        return new TimeSpan(data.getDuration());
    }

    @Override
    public String toString() {
        return data.getDisplayName() + data.getTags().getOrDefault("url.path", "");
    }
}
