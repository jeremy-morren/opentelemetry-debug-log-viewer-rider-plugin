package io.github.ozkanpakdil.opentelemetry.ui.renderers;

import javax.swing.*;
import java.awt.*;

public class TelemetryTypeRender extends TelemetryRenderBase {

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row,
            int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        return this;
    }
}
