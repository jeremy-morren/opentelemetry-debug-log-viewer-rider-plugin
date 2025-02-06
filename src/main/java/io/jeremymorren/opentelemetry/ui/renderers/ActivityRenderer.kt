package io.jeremymorren.opentelemetry.ui.renderers

import com.intellij.ui.JBColor
import com.jetbrains.rd.util.lifetime.Lifetime
import io.jeremymorren.opentelemetry.Activity
import io.jeremymorren.opentelemetry.ActivityStatusCode
import io.jeremymorren.opentelemetry.Telemetry
import io.jeremymorren.opentelemetry.settings.AppSettingState
import java.awt.Component
import javax.swing.JTable

class ActivityRenderer() : TelemetryRendererBase() {
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

        if (value !is Activity) {
            return this
        }
        super.setText(value.getDetail() ?: "")
        //Highlight error activities
        if (value.statusCode == ActivityStatusCode.Error) {
            super.setForeground(JBColor.red)
        }

        return this
    }
}
