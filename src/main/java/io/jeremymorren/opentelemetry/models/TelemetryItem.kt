package io.jeremymorren.opentelemetry.models

import kotlinx.datetime.toJavaInstant
import java.util.*

data class TelemetryItem(
    val json: String,
    val telemetry: Telemetry
)
{
    val lowerCaseJson: String = json.lowercase(Locale.getDefault())

    val timestamp: java.time.Instant? = telemetry.timestamp?.toJavaInstant()

    val duration: TimeSpan? = telemetry.activity?.duration
}