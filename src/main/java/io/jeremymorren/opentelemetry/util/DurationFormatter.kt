package io.jeremymorren.opentelemetry.util

import java.text.DecimalFormat

class DurationFormatter {

    companion object {
        /**
         * Formats a duration as a string.
         */
        fun format(duration: java.time.Duration): String {
            if (duration.toNanos() <= 0) {
                return "" // Zero duration
            }
            if (duration.toHours() > 0) {
                val hours = duration.toHours()
                val minutes = duration.toMinutesPart()
                return "${hours}h ${minutes}m"
            }
            if (duration.toMinutes() > 0) {
                val minutes = duration.toMinutes()
                val seconds = duration.toSecondsPart()
                return "${minutes}m ${seconds}s"
            }

            // Format the first significant part
            val format = DecimalFormat("0.0")
            if (duration.toSeconds() > 0) {
                val seconds = duration.toSeconds() + (duration.toNanosPart() / 1_000_000_000.0)
                return "${format.format(seconds)} s"
            }
            if (duration.toMillis() > 0) {
                val milliseconds = duration.toNanos() / 1_000_000.0
                return "${format.format(milliseconds)} ms"
            }
            if (duration.toNanos() / 1_000 > 0) {
                val microseconds = duration.toNanos() / 1_000.0
                return "${format.format(microseconds)} µs"
            }

            return "${duration.toNanos()} ns"
        }
    }
}