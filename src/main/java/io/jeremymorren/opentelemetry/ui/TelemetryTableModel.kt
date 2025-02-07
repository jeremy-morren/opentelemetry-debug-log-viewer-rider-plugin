package io.jeremymorren.opentelemetry.ui

import io.jeremymorren.opentelemetry.Activity
import io.jeremymorren.opentelemetry.Telemetry
import io.jeremymorren.opentelemetry.TelemetryType
import io.jeremymorren.opentelemetry.utils.TimeSpan
import java.time.Instant
import java.util.*
import javax.swing.table.AbstractTableModel
import kotlin.math.max

class TelemetryTableModel : AbstractTableModel() {
    private val columnNames = arrayOf(
        "timestamp", "duration", "type", "detail"
    )
    private val columnClass = arrayOf<Class<*>>(
        Instant::class.java, TimeSpan::class.java, TelemetryType::class.java, Activity::class.java
    )

    private val telemetries: MutableList<Telemetry> = ArrayList()

    override fun getColumnName(column: Int): String = columnNames[column]

    override fun getColumnClass(columnIndex: Int): Class<*> = columnClass[columnIndex]

    override fun getRowCount(): Int = telemetries.size

    override fun getColumnCount(): Int = columnNames.size

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any? {
        val telemetry = telemetries[rowIndex]
        return when (columnIndex) {
            0 -> telemetry.timestamp
            1 -> telemetry.duration
            2 -> telemetry.type
            3 -> telemetry.telemetry.activity
            else -> null
        }
    }

    fun addRow(telemetry: Telemetry) {
        telemetries.add(telemetry)
        this.fireTableRowsInserted(telemetries.size - 1, telemetries.size - 1)
    }

    fun addRow(i: Int, telemetry: Telemetry) {
        telemetries.add(i, telemetry)
        this.fireTableRowsInserted(telemetries.size - 1, telemetries.size - 1)
    }

    fun setRows(telemetries: List<Telemetry>) {
        val previousSize = this.telemetries.size
        this.telemetries.clear()
        this.fireTableRowsDeleted(0, max(0.0, (previousSize - 1).toDouble()).toInt())
        this.telemetries.addAll(telemetries)
        this.fireTableRowsInserted(0, max(0.0, (this.telemetries.size - 1).toDouble()).toInt())
    }

    fun getRow(selectedRow: Int): Telemetry? {
        if (selectedRow < 0) return null
        if (selectedRow >= this.rowCount) return null
        return telemetries[selectedRow]
    }
}
