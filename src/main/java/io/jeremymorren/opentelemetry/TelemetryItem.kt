@file:OptIn(ExperimentalSerializationApi::class)
@file:Suppress("unused")

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
    val metric: Metric? = null,
    val log: LogMessage? = null
)
{
    fun getType(): TelemetryType? {
        if (activity != null) {
            return activity.getType()
        }
        if (log != null) {
            return log.getType()
        }
        if (metric != null) {
            return TelemetryType.Metric
        }
        return null
    }

    fun getTimestamp(): Instant? {
        val ts = activity?.startTime ?: metric?.lastPoint?.startTime ?: log?.timestamp
        if (ts != null) {
            return Instant.parse(ts)
        }
        return null
    }
}

/**
 * Telemetry type (determined from properties of the activity).
 * @property Activity The telemetry is a generic activity.
 * @property Request The telemetry is a server request (i.e. ASP.NET Core).
 * @property Dependency The telemetry is a dependency (e.g. HTTP, SQL).
 * @property Metric The telemetry is a metric.
 * @property Message The telemetry is a log message.
 * @property Exception The telemetry is a log message with an exception.
 */
@Serializable
enum class TelemetryType {
    Activity,
    Request,
    Dependency,
    Metric,
    Message,
    Exception
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
    val status: ActivityStatusCode? = null,
    val statusDescription: String? = null,
    val events: List<ActivityEvent>? = null,
) {
    val detail: String? = createDetail()

    val typeDisplay: String = createTypeDisplay()

    val isError: Boolean =
        status == ActivityStatusCode.Error
                || tags?.containsKey("error.type") == true
                || tags?.get("otel.status_code") == "ERROR"

    fun getType(): TelemetryType {
        if (kind == ActivityKind.Server && tags != null && tags.containsKey("url.path")) {
            return TelemetryType.Request
        }
        if (kind == ActivityKind.Client) {
            return TelemetryType.Dependency
        }
        return TelemetryType.Activity
    }

    fun getElapsed() : TimeSpan? {
        if (duration == null) {
            return null
        }
        return TimeSpan(duration);
    }

    private fun createTypeDisplay(): String {
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

    private fun getFullUrl(): String? = tags?.get("url.full");

    fun getDbQuery(): String? = (tags?.get("db.query.text") ?: tags?.get("db.statement"))?.replace("\r", "")

    private fun getDbName(): String? = tags?.get("db.name")

    private fun getResponseStatusCode(): String? = tags?.get("http.response.status_code")

    fun getErrorDisplay(): String? = tags?.get("error.type") ?: tags?.get("otel.status_description")

    // Get the subtype of the activity (e.g. HTTP, SQL)
    private fun getSubType(): String? {
        if (source == null) {
            return null
        }
        if (source.nameLower.contains("http")) {
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
            val eventTs = event.getTimestamp()
            if (event.name == "received-first-response" && eventTs != null) {
                val duration = eventTs - startTs
                return TimeSpan(duration.toJavaDuration())
            }
        }
        return null
    }

    private fun createDetail(): String? {
        val parts = mutableListOf<String>()
        if (getSubType() != null) {
            parts.add(getSubType()!!);
        }
        // If the subtype is not known, show the source name
        if (getSubType() == null && source != null) {
            parts.add(source.name)
        }
        if (!displayName.isNullOrEmpty()) {
            // SqlClient sends the database name as the display name (which is not useful)
            if (displayName != getDbName()) {
                parts.add(displayName)
            }
            //If the request does not match a controller, display name will only be method
            //For those, add the request path to the detail
            if (getType() == TelemetryType.Request && !displayName.contains(' ')) {
                parts.add(getRequestPath()!!)
            }
        }

        if (getResponseStatusCode() != null) {
            parts.add(getResponseStatusCode()!!)
        }
        if (isError) {
            if (statusDescription != null) {
                parts.add(statusDescription)
            }
            if (getErrorDisplay() != null) {
                parts.add(getErrorDisplay()!!)
            }
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
    fun getTimestamp(): Instant? = timestamp?.let { Instant.parse(it) }
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
    val points: List<MetricPoint>? = null
)
{
    val lastPoint: MetricPoint? = points?.lastOrNull()

    val detail: String? = createDetail()

    private fun createDetail(): String? {
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

@Serializable
@JsonIgnoreUnknownKeys
data class LogMessage(
    val body: String? = null,
    val logLevel: LogLevel? = null,
    val timestamp: String? = null,
    val exception: ExceptionInfo? = null,
    val attributes: Map<String, String>? = null
)
{
    val formattedMessage: String? = createFormattedMessage()

    val exceptionDisplay: String? =
        if (exception?.display != null) {
            "${createFormattedMessage()}\n\n${exception.display}".replace("\r", "")
        } else { null }

    fun getType(): TelemetryType {
        if (exception != null) {
            return TelemetryType.Exception
        }
        return TelemetryType.Message
    }

    private fun createFormattedMessage(): String? {
        if (logLevel == null || body == null) {
            return null
        }
        val level = when(logLevel) {
            LogLevel.Trace -> "VRB"
            LogLevel.Debug -> "DBG"
            LogLevel.Information -> "INF"
            LogLevel.Warning -> "WRN"
            LogLevel.Error -> "ERR"
            LogLevel.Critical -> "FTL"
            else -> null
        }
        var msg = "[$level] $body";
        for ((key, value) in attributes ?: emptyMap()) {
            msg = msg.replace("{$key}", value, true)
        }
        return msg
    }
}

@Serializable
@JsonIgnoreUnknownKeys
data class ExceptionInfo(
    val message: String? = null,
    val display: String? = null,
    val type: String? = null,
    val innerException: ExceptionInfo? = null
)

@Serializable
enum class LogLevel {
    Trace,
    Debug,
    Information,
    Warning,
    Error,
    Critical,
    None
}