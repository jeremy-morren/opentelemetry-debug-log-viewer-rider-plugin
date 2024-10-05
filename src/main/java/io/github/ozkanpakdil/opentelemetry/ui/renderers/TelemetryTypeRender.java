package io.github.ozkanpakdil.opentelemetry.ui.renderers;

import com.intellij.ui.JBColor;
import io.github.ozkanpakdil.opentelemetry.TelemetryType;

import javax.swing.*;
import java.awt.*;

public class TelemetryTypeRender extends TelemetryRenderBase {

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        TelemetryType type = (TelemetryType) value;

        super.setText(type.toString());

        switch (type) {
            case Message:
                super.setForeground(JBColor.namedColor("TelemetryColor.Message", JBColor.orange));
                break;
            case Request:
                super.setForeground(JBColor.namedColor("TelemetryColor.Request", JBColor.green));
                break;
            case Exception:
                super.setForeground(JBColor.namedColor("TelemetryColor.Exception", JBColor.red));
                break;
            case Duration:
                super.setForeground(JBColor.namedColor("TelemetryColor.Metric", JBColor.gray));
                break;
            case Unk:
                super.setForeground(JBColor.namedColor("TelemetryColor.Unk", JBColor.darkGray));
                break;
        }
        return this;
    }
}
