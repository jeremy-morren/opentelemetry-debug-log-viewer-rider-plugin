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
import java.time.Instant
import java.util.*

class TelemetryFactory {
    public fun tryCreateFromDebugOutputLog(output: String): Telemetry? {
        val openTelemetryLogPrefix = "{\"activity\":{\"traceId\":\""

        val json = output.trim()
        if (!json.startsWith(openTelemetryLogPrefix) || !json.endsWith("}")) {
            return null
        }

        return try {
            val telemetry = Json.decodeFromString<TelemetryInfo>(json)
            return Telemetry(formatJson(json), telemetry)
        } catch (e: SerializationException) {
            null
        }
    }

    private fun formatJson(json: String): String {
        val prettyJson = Json {
            prettyPrint = true
            prettyPrintIndent = "  "
        }
        val obj = Json.decodeFromString<JsonObject>(json)
        return prettyJson.encodeToString(JsonObject.serializer(), obj)
    }
}

data class Telemetry(
    val json: String,
    val telemetry: TelemetryInfo
)
{
    val timestamp: Date? = telemetry.activity.getStartTime()

    val type: TelemetryType? = telemetry.activity.getType()

    val duration: TimeSpan? = telemetry.activity.getDuration()

    val lowerCaseJson: String = json.lowercase(Locale.getDefault())
}

@Serializable
data class TelemetryInfo(
    val activity: Activity
);

@Serializable
@JsonIgnoreUnknownKeys
data class Activity(
    val traceId: String? = null,
    val spanId: String? = null,
    val activityTraceFlags: String? = null,
    val traceStateString: String? = null,
    val parentSpanId: String? = null,
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
) {
    private fun sourceLower(): String = activitySourceName?.lowercaseLocaleAgnostic() ?: "";

    fun getType(): TelemetryType? {
        if (activitySourceName == null) {
            return null
        }
        if (kind == ActivityKind.Server && getRequestUrl() != null) {
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

    private fun getRequestUrl(): String? {
        if (tags == null) return null;
        val sb = StringBuilder();
        sb.append(tags.getOrDefault("url.full", ""));
        sb.append(tags.getOrDefault("url.path", ""));
        sb.append(tags.getOrDefault("url.query", ""));
        if (sb.isEmpty()) return null;
        return sb.toString();
    }

    private fun getDbQuery(): String? = tags?.get("db.query.text") ?: tags?.get("db.statement")

    private fun getDbName(): String? = tags?.get("db.name")

    private fun getResponseStatusCode(): String? = tags?.get("http.response.status_code")

    private fun getStatus(): String? = statusDescription ?: tags?.get("error.type")

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

    fun getStartTime(): Date? {
        if (startTime == null) {
            return null
        }
        val instant = Instant.parse(startTime)
        return Date.from(instant)
    }

    fun getDuration() : TimeSpan? {
        if (duration == null) {
            return null
        }
        return TimeSpan(duration);
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
        if (getRequestUrl() != null) {
            parts.add(getRequestUrl()!!)
        }
        if (parts.size == 0) {
            return null
        }
        return parts.joinToString(" - ")
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