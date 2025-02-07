@file:OptIn(ExperimentalSerializationApi::class)

package io.jeremymorren.opentelemetry

import fleet.util.lowercaseLocaleAgnostic
import io.jeremymorren.opentelemetry.utils.TimeSpan
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonIgnoreUnknownKeys
import kotlinx.serialization.json.JsonObject
import java.time.Duration
import java.time.Instant
import java.util.*

data class Telemetry(
    val json: String,
    val telemetry: TelemetryInfo
)
{
    val timestamp: Instant? = telemetry.activity.getStartTime()

    val type: TelemetryType? = telemetry.activity.getType()

    val duration: TimeSpan? = telemetry.activity.getElapsed()

    val lowerCaseJson: String = json.lowercase(Locale.getDefault())

    val sql: String? = telemetry.activity.getDbQuery()
}

@Serializable
data class TelemetryInfo(
    val activity: Activity
);

@Serializable
@JsonIgnoreUnknownKeys
data class Activity(
    val rootId: String? = null,
    val traceId: String? = null,
    val spanId: String? = null,
    val parentSpanId: String? = null,
    val activityTraceFlags: String? = null,
    val traceStateString: String? = null,
    val activitySourceName: String? = null,
    val activitySourceVersion: String? = null,
    val displayName: String? = null,
    val kind: ActivityKind? = null,
    val startTime: String? = null,
    val duration: String? = null,
    val tags: Map<String, String>? = null,
    val operationName: String? = null,
    val statusCode: ActivityStatusCode? = null,
    val statusDescription: String? = null,
    val events: List<ActivityEvent>? = null,
) {
    fun getStartTime(): Instant? {
        if (startTime == null) {
            return null
        }
        return Instant.parse(startTime)
    }

    fun getElapsed() : TimeSpan? {
        if (duration == null) {
            return null
        }
        return TimeSpan(duration);
    }

    private fun sourceLower(): String = activitySourceName?.lowercaseLocaleAgnostic() ?: "";

    fun getType(): TelemetryType? {
        if (activitySourceName == null) {
            return null
        }
        if (kind == ActivityKind.Server && getRequestPath() != null) {
            return TelemetryType.Request;
        }
        if (kind == ActivityKind.Client) {
            return TelemetryType.Dependency;
        }
        if (kind == ActivityKind.Internal) {
            return TelemetryType.Activity;
        }
        return null;
    }

    fun getTypeDisplay(): String {
        val sb = StringBuilder();
        sb.append(getType()?.name ?: "Unknown")
        if (getSubType() != null) {
            sb.append(" - ")
            sb.append(getSubType())
        }
        return sb.toString();
    }

    private fun getRequestPath(): String? {
        if (tags == null) return null;
        val sb = StringBuilder();
        sb.append(tags.getOrDefault("url.path", ""));
        sb.append(tags.getOrDefault("url.query", ""));
        if (sb.isEmpty()) return null;
        return sb.toString();
    }

    private fun getFullUrl(): String? = tags?.getOrDefault("url.full", null);

    fun getDbQuery(): String? = (tags?.get("db.query.text") ?: tags?.get("db.statement"))?.replace("\r", "")

    private fun getDbName(): String? = tags?.get("db.name")

    private fun getResponseStatusCode(): String? = tags?.get("http.response.status_code")

    fun getStatus(): String? = statusDescription ?: tags?.get("error.type")

    // Get the subtype of the activity (e.g. HTTP, SQL)
    private fun getSubType(): String? {
        if (activitySourceName == null) {
            return null
        }
        if (activitySourceName == "System.Net.Http") {
            return "HTTP"
        }
        if (sourceLower().contains("sql")) {
            return "SQL"
        }
        return null
    }

    // Get the activity source (name and version)
    fun getActivitySource(): String? {
        if (activitySourceVersion.isNullOrEmpty()) {
            return activitySourceName
        }
        return "$activitySourceName ($activitySourceVersion)"
    }

    // Get the trace IDs in a map
    fun getTraceIds(): Map<String, String> {
        val traceIds = mutableMapOf<String, String>()
        if (rootId != null)
            traceIds["Root ID"] = rootId
        if (traceId != null)
            traceIds["Trace ID"] = traceId
        if (spanId != null)
            traceIds["Span ID"] = spanId
        if (parentSpanId != null)
            traceIds["Parent Span ID"] = parentSpanId
        if (activityTraceFlags != null)
            traceIds["Flags"] = activityTraceFlags
        return traceIds
    }

    // Gets time spent in the database (i.e. until first result is returned)
    fun getDbQueryTime(): TimeSpan? {
        if (events == null || getStartTime() == null) {
            return null
        }
        for (event in events) {
            if (event.name == "received-first-response"
                && event.getTimestamp() != null
                && event.getTimestamp()!!.isAfter(getStartTime())) {
                val duration = Duration.between(getStartTime(), event.getTimestamp())
                return TimeSpan(duration)
            }
        }
        return null
    }

    fun getDetail(): String? {
        val parts = mutableListOf<String>()
        if (getSubType() != null) {
            parts.add(getSubType()!!);
        }
        // SqlClient sends the database name as the display name (which is not useful)
        if (displayName != null && displayName != getDbName()) {
            parts.add(displayName)
        }
        if (getResponseStatusCode() != null) {
            parts.add(getResponseStatusCode()!!)
        }
        if (statusCode == ActivityStatusCode.Error && getStatus() != null) {
            parts.add(getStatus()!!)
        }
        if (getDbQuery() != null) {
            parts.add(getDbQuery()!!)
        }
        if (getFullUrl() != null) {
            parts.add(getFullUrl()!!)
        }
        if (parts.size == 0) {
            return null
        }
        val str = parts.joinToString(" - ")
            .replace("\r", "")
            .replace("\n", " ");
        if (str.length > 100) {
            return str.substring(0, 100) + "..."
        }
        return str
    }
}

@Serializable
data class ActivityEvent(
    val name: String?,
    val timestamp: String?,
    val attributes: Map<String, String>?,
)
{
    fun getTimestamp() : Instant?
    {
        if (timestamp == null) {
            return null
        }
        return Instant.parse(timestamp)
    }
}

/**
 * Activity status code.
 * See [ActivityStatusCode Enum](https://learn.microsoft.com/en-us/dotnet/api/system.diagnostics.activitystatuscode)
 */
@Serializable
enum class ActivityStatusCode {
    Unset,
    Ok,
    Error,
}

/**
 * Activity kind.
 * See [ActivityKind Enum](https://learn.microsoft.com/en-us/dotnet/api/system.diagnostics.activitykind)
 */
@Serializable
enum class ActivityKind {
    Internal,
    Server,
    Client,
    Producer,
    Consumer,
}

/**
 * Telemetry type (determined from properties of the activity).
 * @property Activity The activity is a generic activity.
 * @property Request The activity is a server request (i.e. ASP.NET Core).
 * @property Dependency The activity is a dependency (e.g. HTTP, SQL).
 */
@Serializable
enum class TelemetryType {
    Activity,
    Request,
    Dependency
}