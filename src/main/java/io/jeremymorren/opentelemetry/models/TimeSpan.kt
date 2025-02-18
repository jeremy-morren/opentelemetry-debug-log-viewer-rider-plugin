@file:Suppress("MemberVisibilityCanBePrivate")

package io.jeremymorren.opentelemetry.models

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
@Serializable(with = TimeSpan.TimeSpanSerializer::class)
class TimeSpan(val hours: Int,
               val minutes: Int,
               val seconds: Double) : Comparable<TimeSpan> {

    val milliseconds: Double
        get() = seconds * 1_000

    val microseconds: Double
        get() = seconds * 1_000_000

    override fun toString(): String {
        // Format with 1 decimal point
        val formatter = DecimalFormat("0.0")

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

    operator fun minus(other: TimeSpan): TimeSpan {
        return fromSeconds(totalSeconds - other.totalSeconds)
    }

    companion object {
        fun fromSeconds(seconds: Double): TimeSpan {
            val duration = Duration.ofSeconds(
                seconds.toLong(),
                ((seconds % 1) * 1_000_000_000).toLong())
            return fromDuration(duration)
        }

        fun fromDuration(duration: Duration): TimeSpan {
            return TimeSpan(
                duration.toHoursPart(),
                duration.toMinutesPart(),
                duration.toSecondsPart() + (duration.nano / 1_000_000_000.0))
        }

        /**
         * Parse a TimeSpan from a string (D.HH:mm:ss.ffffff)
         */
        fun fromJsonString(value: String): TimeSpan {
            val split = value.split(':', limit = 3).toTypedArray()
            val hours = split[0].split('.', limit = 2).toTypedArray()

            return TimeSpan(
                if (hours.size > 1)
                    (hours[0].toInt() * 24) + hours[1].toInt()
                else
                    hours[0].toInt(),
                split[1].toInt(),
                split[2].toDouble())
        }
    }

    object TimeSpanSerializer : KSerializer<TimeSpan> {
        override val descriptor = PrimitiveSerialDescriptor(
            "io.jeremymorren.opentelemetry.TimeSpan",
            PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: TimeSpan) {
            encoder.encodeString(value.toString())
        }

        override fun deserialize(decoder: Decoder): TimeSpan {
            return fromJsonString(decoder.decodeString())
        }
    }
}

