package io.github.ozkanpakdil.opentelemetry;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class TelemetryFactory {
    @NotNull
    private final Gson gson;

    public TelemetryFactory() {
        gson = new GsonBuilder()
//                .setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE)
                .create();
    }

    @NotNull
    public Telemetry fromJson(@NotNull String json) {
        ActivityOutput wrapper = gson.fromJson(json, ActivityOutput.class);
        ActivityInfo activity = wrapper.getActivity();
        TelemetryType type1 = TelemetryType.fromType(activity.getDisplayName());

        return new Telemetry(type1, json, gson.fromJson(json, JsonObject.class), activity, activity.getTags());
    }

    @Nullable
    public Telemetry tryCreateFromDebugOutputLog(@NotNull String output) {
        String appInsightsLogPrefix = "{\"activity\":{\"traceId\":\"";
        String filteredByPrefix = " (filtered by ";
        String unconfiguredPrefix = " (unconfigured) ";

        if (!output.startsWith(appInsightsLogPrefix)) {
            return null;
        }

        Telemetry telemetry = fromJson(output);
        String telemetryState = output.substring(appInsightsLogPrefix.length());
        if (telemetryState.startsWith(filteredByPrefix)) {
            telemetry.setFilteredBy(telemetryState.substring(filteredByPrefix.length(), telemetryState.indexOf(')')));
        } else if (telemetryState.startsWith(unconfiguredPrefix)) {
            telemetry.setUnConfigured();
        }

        return telemetry;
    }

    public class ActivityOutput {
        @SerializedName("activity")
        private ActivityInfo activity;

        public ActivityInfo getActivity() {
            return activity;
        }

    }

    public class ActivityInfo {
        private String traceId;
        private String spanId;
        private String activityTraceFlags;
        private String traceStateString;
        private String parentSpanId;
        private String activitySourceName;
        private String activitySourceVersion;
        private String displayName;
        private String kind;
        private String startTime;
        private String duration;
        private Map<String, String> tags;
        private String statusCode;
        private String statusDescription;
        private List<ActivityEvent> events;
        private List<ActivityLink> links;
        private Map<String, String> resource;
        private String rootId;
        private String operationName;

        public String getTraceId() {
            return traceId;
        }

        public void setTraceId(String traceId) {
            this.traceId = traceId;
        }

        public String getSpanId() {
            return spanId;
        }

        public void setSpanId(String spanId) {
            this.spanId = spanId;
        }

        public String getActivityTraceFlags() {
            return activityTraceFlags;
        }

        public void setActivityTraceFlags(String activityTraceFlags) {
            this.activityTraceFlags = activityTraceFlags;
        }

        public String getTraceStateString() {
            return traceStateString;
        }

        public void setTraceStateString(String traceStateString) {
            this.traceStateString = traceStateString;
        }

        public String getParentSpanId() {
            return parentSpanId;
        }

        public void setParentSpanId(String parentSpanId) {
            this.parentSpanId = parentSpanId;
        }

        public String getActivitySourceName() {
            return activitySourceName;
        }

        public void setActivitySourceName(String activitySourceName) {
            this.activitySourceName = activitySourceName;
        }

        public String getActivitySourceVersion() {
            return activitySourceVersion;
        }

        public void setActivitySourceVersion(String activitySourceVersion) {
            this.activitySourceVersion = activitySourceVersion;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getKind() {
            return kind;
        }

        public void setKind(String kind) {
            this.kind = kind;
        }

        public String getStartTime() {
            return startTime;
        }

        public void setStartTime(String startTime) {
            this.startTime = startTime;
        }

        public String getDuration() {
            return duration;
        }

        public void setDuration(String duration) {
            this.duration = duration;
        }

        public Map<String, String> getTags() {
            return tags;
        }

        public void setTags(Map<String, String> tags) {
            this.tags = tags;
        }

        public String getStatusCode() {
            return statusCode;
        }

        public void setStatusCode(String statusCode) {
            this.statusCode = statusCode;
        }

        public String getStatusDescription() {
            return statusDescription;
        }

        public void setStatusDescription(String statusDescription) {
            this.statusDescription = statusDescription;
        }

        public List<ActivityEvent> getEvents() {
            return events;
        }

        public void setEvents(List<ActivityEvent> events) {
            this.events = events;
        }

        public List<ActivityLink> getLinks() {
            return links;
        }

        public void setLinks(List<ActivityLink> links) {
            this.links = links;
        }

        public Map<String, String> getResource() {
            return resource;
        }

        public void setResource(Map<String, String> resource) {
            this.resource = resource;
        }

        public String getRootId() {
            return rootId;
        }

        public void setRootId(String rootId) {
            this.rootId = rootId;
        }

        public String getOperationName() {
            return operationName;
        }

        public void setOperationName(String operationName) {
            this.operationName = operationName;
        }
    }

    public class ActivityEvent {
        private String Name;
        private Instant Timestamp;
        private Map<String, String> Attributes;
    }

    public class ActivityLink {
        private String TraceId;
        private String SpanId;
    }

}


