package io.jeremymorren.opentelemetry.ui.renderers

import java.awt.Component
import java.text.SimpleDateFormat
import java.util.*
import javax.swing.JTable

class TelemetryDateRenderer : TelemetryRendererBase() {
    private val simpleDateFormat = SimpleDateFormat("HH:mm:ss.S")

    override fun getTableCellRendererComponent(
        table: JTable,
        value: Any?,
        isSelected: Boolean,
        hasFocus: Boolean,
        row: Int,
        column: Int
    ): Component {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)

        if (value is Date) {
            super.setText(simpleDateFormat.format(value))
        }

        return this
    }
}
