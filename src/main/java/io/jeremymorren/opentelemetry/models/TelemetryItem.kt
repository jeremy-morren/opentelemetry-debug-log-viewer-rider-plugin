package io.jeremymorren.opentelemetry.models

import kotlinx.datetime.toJavaInstant
import java.util.*
import java.time.Duration
import java.time.Instant

data class TelemetryItem(
    val json: String,
    val telemetry: Telemetry
)
{
    val lowerCaseJson: String = json.lowercase(Locale.getDefault())

    val timestamp: Instant? = telemetry.timestamp

    val duration: Duration? = telemetry.activity?.duration
}