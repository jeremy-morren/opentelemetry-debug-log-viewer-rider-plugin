package io.jeremymorren.opentelemetry.ui.renderers

import com.intellij.ui.JBColor
import io.jeremymorren.opentelemetry.ActivityStatusCode
import io.jeremymorren.opentelemetry.Telemetry
import java.awt.Component
import javax.swing.JTable

class TelemetryRenderer : TelemetryRendererBase() {
    override fun getTableCellRendererComponent(
        table: JTable,
        value: Any?,
        isSelected: Boolean,
        hasFocus: Boolean,
        row: Int,
        column: Int
    ): Component {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)

        super.setForeground(JBColor.foreground())

        if (value !is Telemetry) {
            return this
        }
        if (value.activity != null) {
            super.setText(value.activity.getDetail())
            //Highlight error activities
            if (value.activity.statusCode == ActivityStatusCode.Error) {
                super.setForeground(JBColor.red)
            }
        }
        if (value.metric != null) {
            super.setText(value.metric.getDetail())
        }

        return this
    }
}
