package io.github.ozkanpakdil.opentelemetry.ui.renderers;

import com.intellij.ui.JBColor;
import com.jetbrains.rd.util.lifetime.Lifetime;
import io.github.ozkanpakdil.opentelemetry.Telemetry;
import io.github.ozkanpakdil.opentelemetry.settings.AppSettingState;
import kotlin.Unit;

import javax.swing.*;
import java.awt.*;

public class TelemetryRender extends TelemetryRenderBase {
    private boolean showFilteredIndicator;

    public TelemetryRender(Lifetime lifetime) {
        AppSettingState.getInstance().showFilteredIndicator.advise(lifetime, v -> {
            showFilteredIndicator = v;
            return Unit.INSTANCE;
        });
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row,
            int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        super.setForeground(JBColor.foreground());

        Telemetry telemetry = (Telemetry) value;
        String text = telemetry.toString();

        if (showFilteredIndicator && telemetry.getFilteredBy() != null) {
            super.setText("(Filtered) " + text);
        } else {
            super.setText(text);
        }

        return this;
    }

}
