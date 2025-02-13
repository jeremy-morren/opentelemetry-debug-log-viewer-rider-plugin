package io.jeremymorren.opentelemetry.utils

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.text.DecimalFormat
import java.time.Duration

/**
 * C# Timespan equivalent (HH:mm:ss.ffffff)
 * @property hours Hour component
 * @property minutes Minute component
 * @property seconds Seconds component
 */
@Serializable(with = TimeSpanSerializer::class)
class TimeSpan : Comparable<TimeSpan> {
    val hours: Int
    val minutes: Int
    val seconds: Double

    val milliseconds: Double
        get() = seconds * 1_000

    val microseconds: Double
        get() = seconds * 1_000_000

    constructor(hours: Int, minutes: Int, seconds: Double) {
        this.hours = hours
        this.minutes = minutes
        this.seconds = seconds
    }

    constructor(value: String) {
        val hourMinuteSeconds = value.split(":".toRegex(), limit = 3).toTypedArray()
        hours = hourMinuteSeconds[0].toInt()
        minutes = hourMinuteSeconds[1].toInt()
        seconds = hourMinuteSeconds[2].toDouble()
    }

    constructor(duration: Duration) {
        hours = duration.toHoursPart()
        minutes = duration.toMinutesPart()
        seconds = duration.toSecondsPart() + (duration.nano / 1_000_000_000.0);
    }

    override fun toString(): String {
        // Format with 1 decimal point
        val formatter = DecimalFormat("0.0");

        if (hours != 0) {
            return "${hours}h ${minutes}m ${formatter.format(seconds)}s"
        }
        if (minutes != 0) {
            return "${minutes}m ${formatter.format(seconds)}s"
        }

        // For fractional parts: format the first significant part
        if (seconds > 1) {
            return "${formatter.format(seconds)} s"
        }
        if (milliseconds > 1) {
            return "${formatter.format(milliseconds)} ms"
        }
        if (microseconds > 1) {
            return "${formatter.format(milliseconds)}s µs"
        }
        return ""
    }

    val totalSeconds: Double
        get() = (hours * 3_600) + (minutes * 60) + seconds

    override fun compareTo(other: TimeSpan): Int = totalSeconds.compareTo(other.totalSeconds)
}

object TimeSpanSerializer : KSerializer<TimeSpan> {
    override val descriptor = PrimitiveSerialDescriptor("TimeSpan", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: TimeSpan) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): TimeSpan {
        return TimeSpan(decoder.decodeString())
    }
}
