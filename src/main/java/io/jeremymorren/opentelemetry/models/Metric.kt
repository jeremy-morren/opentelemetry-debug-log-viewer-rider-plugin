@file:OptIn(ExperimentalSerializationApi::class)
@file:Suppress("unused")

package io.jeremymorren.opentelemetry.models

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonIgnoreUnknownKeys
import java.time.Duration
import java.time.Instant


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
    val meterTags: ObjectDictionary? = null,
    val points: List<MetricPoint>? = null
)
{
    /**
     * Telemetry type (always [TelemetryType.Metric]).
     */
    val type: TelemetryType get() = TelemetryType.Metric

    /**
     * The timestamp of the metric, if available.
     */
    val timestamp: String? get() {
        if (points == null) {
            return null
        }
        for (point in points) {
            if (point.startTime != null) {
                return point.startTime
            }
        }
        return null
    }

    /**
     * The last metric point for each tag.
     */
    val taggedPoints: List<MetricPoint>? get() {
        if (points == null) {
            return null
        }
        val map = mutableMapOf<ObjectDictionary?, MetricPoint>()
        for (point in points) {
            map[point.tags] = point
        }
        return map.values.toList()
    }

    /**
     * Detail display string for the metric.
     */
    val detail: String? get() {
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

    /**
     * The meter display string.
     */
    val meter: String? get() {
        if (meterName.isNullOrEmpty()) {
            return null
        }
        if (meterVersion.isNullOrEmpty()) {
            return meterName
        }
        return "$meterName ($meterVersion)"
    }

    /**
     * The measurement duration.
     */
    val duration: TimeSpan? get() = points?.firstNotNullOf { it.duration }
}

/**
 * A metric point.
 */
@Serializable
@JsonIgnoreUnknownKeys
data class MetricPoint(
    val startTime: String? = null,
    val endTime: String? = null,
    val tags: ObjectDictionary? = null,
    val longSum: Long? = null,
    val doubleSum: Double? = null,
    val longGauge: Long? = null,
    val doubleGauge: Double? = null,
    val histogramCount: Long? = null,
    val histogramSum: Double? = null
)
{
    val duration: TimeSpan? get() {
        if (startTime == null || endTime == null) return null
        val start = Instant.parse(startTime)
        val end = Instant.parse(endTime)
        return TimeSpan.fromDuration(Duration.between(start, end))
    }
}
