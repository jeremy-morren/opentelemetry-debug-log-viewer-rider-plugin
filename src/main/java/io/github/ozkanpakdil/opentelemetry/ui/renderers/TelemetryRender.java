package io.github.ozkanpakdil.opentelemetry.ui.renderers;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.ui.JBColor;
import com.jetbrains.rd.util.lifetime.Lifetime;
import io.github.ozkanpakdil.opentelemetry.Telemetry;
import io.github.ozkanpakdil.opentelemetry.metricdata.*;
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
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        super.setForeground(JBColor.foreground());

        Telemetry telemetry = (Telemetry) value;
        String text;
        switch (telemetry.getType()) {
            case Exception: {
                ExceptionData exceptionData = telemetry.getData(ExceptionData.class);

                colorComponentDependingOnSeverityLevel(exceptionData.severityLevel, isSelected);

                String message = exceptionData.exceptions.get(0).message;
                if (exceptionData.severityLevel != null) {
                    text = "[" + exceptionData.severityLevel + "] " + message;
                } else {
                    text = message;
                }
                break;
            }
            case Request: {
                RequestData requestData = telemetry.getData(RequestData.class);

                if (requestData.responseCode == null) {
                    text = requestData.name;
                    if (requestData.success) {
                        super.setForeground(JBColor.namedColor("SeverityLevel.Default", JBColor.foreground()));
                    } else {
                        super.setForeground(JBColor.namedColor("SeverityLevel.Error", JBColor.red));
                    }
                    break;
                }

                if (requestData.responseCode.startsWith("5")) {
                    super.setForeground(JBColor.namedColor("SeverityLevel.Error", JBColor.red));
                } else if (requestData.responseCode.startsWith("4")) {
                    super.setForeground(JBColor.namedColor("SeverityLevel.Warning", JBColor.orange));
                } else {
                    super.setForeground(JBColor.namedColor("SeverityLevel.Default", JBColor.foreground()));
                }
                text = requestData.responseCode + " - " + requestData.name;
                break;
            }
            case Duration: {
                MetricData metricData = telemetry.getData(MetricData.class);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < metricData.metrics.size(); i++) {
                    MetricData.Metric metric = metricData.metrics.get(i);
                    if (i > 0)
                        sb.append(" - ");
                    sb.append(metric.name).append(':').append(metric.value);
                }
                text = sb.toString();
                break;
            }
            case Message: {
                MessageData messageData = telemetry.getData(MessageData.class);

                colorComponentDependingOnSeverityLevel(messageData.severityLevel, isSelected);
                if (messageData.severityLevel != null) {
                    text = "[" + messageData.severityLevel + "] " + messageData.message;
                } else {
                    text = messageData.message;
                }
                break;
            }
            default:
                text = telemetry.toString();
                break;
        }

        if (showFilteredIndicator && telemetry.getFilteredBy() != null) {
            super.setText("(Filtered) " + text);
        } else {
            super.setText(text);
        }

        return this;
    }
    private void colorComponentDependingOnSeverityLevel(String severityLevel, boolean isSelected) {
        if ("Error".equals(severityLevel)) {
            super.setForeground(JBColor.namedColor("SeverityLevel.Error", JBColor.red));
        } else if ("Warning".equals(severityLevel)) {
            super.setForeground(JBColor.namedColor("SeverityLevel.Warning", JBColor.orange));
        } else if ("Critical".equals(severityLevel)) {
            super.setForeground(JBColor.namedColor("SeverityLevel.Default", JBColor.foreground()));
            if (!isSelected) {
                super.setBackground(JBColor.namedColor("SeverityLevel.Critical", new JBColor(0xA21319, 0x5B0006)));
            }
        } else {
            super.setForeground(JBColor.namedColor("SeverityLevel.Default", JBColor.foreground()));
        }
    }

    public void setShowFilteredIndicator(boolean showFilteredIndicator) {
        this.showFilteredIndicator = showFilteredIndicator;
        PropertiesComponent.getInstance().setValue("io.github.ozkanpakdil.opentelemetry.showFilteredIndicator", showFilteredIndicator);
    }

    public boolean isShowFilteredIndicator() {
        return showFilteredIndicator;
    }
}
