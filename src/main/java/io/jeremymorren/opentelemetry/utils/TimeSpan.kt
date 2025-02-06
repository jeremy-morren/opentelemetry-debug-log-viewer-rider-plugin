package io.jeremymorren.opentelemetry.utils

import java.text.DecimalFormat

class TimeSpan : Comparable<TimeSpan> {
    val hours: Int
    val minutes: Int
    val seconds: Int
    val microseconds: Int

    val milliseconds: Int
        get() = microseconds / 1000

    val totalMicroseconds: Long
        get() = (((hours * 3600) + (minutes * 60) + seconds) * 1_000_000L) + microseconds

    constructor(hours: Int, minutes: Int, seconds: Int, microseconds: Int) {
        this.hours = hours
        this.minutes = minutes
        this.seconds = seconds
        this.microseconds = microseconds
    }

    constructor(value: String) {
        val split = value.split("\\.".toRegex(), limit = 2).toTypedArray()
        val hourMinuteSeconds = split[0].split(":".toRegex(), limit = 3).toTypedArray()
        hours = hourMinuteSeconds[0].toInt()
        minutes = hourMinuteSeconds[1].toInt()
        seconds = hourMinuteSeconds[2].toInt()
        microseconds = if (split.size > 1) split[1].padEnd(6, '0').substring(0, 6).toInt() else 0
    }

    override fun toString(): String {
        val msPadded = (milliseconds / 10).toString().padStart(2, '0')
        if (hours != 0) {
            return "${hours}h ${minutes}m $seconds.${msPadded}s"
        }
        if (minutes != 0) {
            return "${minutes}m $seconds.${msPadded}s"
        }
        if (seconds != 0) {
            return "$seconds.${msPadded}s"
        }
        if (milliseconds != 0) {
            // Format with decimal point
            val formatter = DecimalFormat("0.0");
            val ms = microseconds / 1000.0;
            return "${formatter.format(ms)} ms"
        }
        if (microseconds != 0) {
            return "$microseconds µs"
        }
        return ""
    }


    override fun compareTo(other: TimeSpan): Int {
        return totalMicroseconds.compareTo(other.totalMicroseconds)
    }
}
