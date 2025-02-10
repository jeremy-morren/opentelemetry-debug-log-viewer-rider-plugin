@file:OptIn(ExperimentalSerializationApi::class)

package io.jeremymorren.opentelemetry

import io.jeremymorren.opentelemetry.utils.TimeSpan
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonIgnoreUnknownKeys
import java.util.*
import kotlin.time.toJavaDuration

data class TelemetryItem(
    val json: String,
    val telemetry: Telemetry
)
{
    val lowerCaseJson: String = json.lowercase(Locale.getDefault())

    val type: TelemetryType? = telemetry.getType()

    val timestamp: java.time.Instant? = telemetry.getTimestamp()?.toJavaInstant()

    val duration: TimeSpan? = telemetry.activity?.getElapsed()

    val sql: String? = telemetry.activity?.getDbQuery()
}

@Serializable
@JsonIgnoreUnknownKeys
data class Telemetry(
    val activity: Activity? = null,
    val metric: Metric? = null
)
{
    fun getType(): TelemetryType? {
        if (activity != null) {
            return activity.getType()
        }
        if (metric != null) {
            return TelemetryType.Metric
        }
        return null
    }

    fun getTimestamp(): Instant? {
        val ts = activity?.startTime ?: metric?.lastPoint?.startTime
        if (ts != null) {
            return Instant.parse(ts)
        }
        return null
    }
}

@Serializable
@JsonIgnoreUnknownKeys
data class Activity(
    val rootId: String? = null,
    val traceId: String? = null,
    val spanId: String? = null,
    val parentSpanId: String? = null,
    val activityTraceFlags: String? = null,
    val traceStateString: String? = null,
    val source: ActivitySource? = null,
    val displayName: String? = null,
    val kind: ActivityKind? = null,
    val startTime: String? = null,
    val duration: String? = null,
    val tags: Map<String, String?>? = null,
    val operationName: String? = null,
    val statusCode: ActivityStatusCode? = null,
    val statusDescription: String? = null,
    val events: List<ActivityEvent>? = null,
) {
    fun getElapsed() : TimeSpan? {
        if (duration == null) {
            return null
        }
        return TimeSpan(duration);
    }

    fun getType(): TelemetryType {
        if (kind == ActivityKind.Server && getRequestPath() != null) {
            return TelemetryType.Request
        }
        if (kind == ActivityKind.Client) {
            return TelemetryType.Dependency
        }
        return TelemetryType.Activity
    }

    fun getTypeDisplay(): String {
        val sb = StringBuilder()
        sb.append(getType().name)
        if (getSubType() != null) {
            sb.append(" - ")
            sb.append(getSubType())
        }
        return sb.toString();
    }

    fun getRequestPath(): String? {
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
        if (source == null) {
            return null
        }
        if (source.name == "System.Net.Http") {
            return "HTTP"
        }
        if (source.nameLower.contains("sql")) {
            return "SQL"
        }
        return null
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
        if (events == null || startTime == null) {
            return null
        }
        val startTs = Instant.parse(startTime)
        // Find the event called "received-first-response"
        for (event in events) {
            if (event.name == "received-first-response" && event.getTimestamp() != null) {
                val duration = event.getTimestamp()!! - startTs
                return TimeSpan(duration.toJavaDuration())
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
@JsonIgnoreUnknownKeys
data class ActivitySource(
    val name: String,
    val version: String? = null
)
{
    fun getDisplay(): String {
        if (version == null) {
            return name
        }
        return "$name ($version)"
    }

    val nameLower: String = name.lowercase(Locale.ROOT)
}

@Serializable
@JsonIgnoreUnknownKeys
data class ActivityEvent(
    val name: String? = null,
    val timestamp: String? = null,
    val attributes: Map<String, String>? = null,
)
{
    fun getTimestamp(): Instant? {
        if (timestamp != null) {
            return Instant.parse(timestamp)
        }
        return null
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
 * @property Activity The telemetry is a generic activity.
 * @property Request The telemetry is a server request (i.e. ASP.NET Core).
 * @property Dependency The telemetry is a dependency (e.g. HTTP, SQL).
 * @property Metric The telemetry is a metric.
 */
@Serializable
enum class TelemetryType {
    Activity,
    Request,
    Dependency,
    Metric
}

/**
 * A metric.
 */
@Serializable
@JsonIgnoreUnknownKeys
data class Metric(
    val metricType: String? = null,
    val temporality: String? = null,
    val name: String? = null,
    val description: String? = null,
    val unit: String? = null,
    val meterName: String? = null,
    val meterVersion: String? = null,
    val meterTags:  Map<String,String?>? = null,
    val points: List<MetricPoint>? = null,
)
{
    val lastPoint: MetricPoint? = points?.lastOrNull()

    fun getDetail(): String? {
        val parts = mutableListOf<String>()
        if (!name.isNullOrEmpty()) {
            parts.add(name)
        }
        if (!description.isNullOrEmpty()) {
            parts.add(description)
        }
        if (!meterName.isNullOrEmpty()) {
            parts.add(meterName)
        }
        if (parts.size == 0) {
            return null
        }
        return parts.joinToString(" - ")
    }

    fun getMeterDisplay(): String? {
        if (meterName.isNullOrEmpty()) {
            return null
        }
        if (meterVersion.isNullOrEmpty()) {
            return meterName
        }
        return "$meterName ($meterVersion)"
    }
}

/**
 * A metric point.
 */
@Serializable
@JsonIgnoreUnknownKeys
data class MetricPoint(
    val startTime: String? = null,
    val endTime: String? = null,
    val longSum: Long? = null,
    val doubleSum: Double? = null,
    val longGauge: Long? = null,
    val doubleGauge: Double? = null,
    val histogramCount: Long? = null,
    val histogramSum: Double? = null,
)
