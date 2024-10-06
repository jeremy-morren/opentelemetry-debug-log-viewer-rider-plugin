package io.github.ozkanpakdil.opentelemetry.ui;

import io.github.ozkanpakdil.opentelemetry.Telemetry;
import io.github.ozkanpakdil.opentelemetry.TelemetryType;
import io.github.ozkanpakdil.opentelemetry.utils.TimeSpan;
import org.jetbrains.annotations.Nullable;

import javax.swing.table.AbstractTableModel;
import java.util.*;

public class TelemetryTableModel extends AbstractTableModel {
    private final String[] columnNames = new String[]{
            "timestamp", "duration", "data"
    };
    private final Class<?>[] columnClass = new Class[]{
            Date.class, TimeSpan.class, Telemetry.class
    };
    private final List<Telemetry> telemetries;

    public TelemetryTableModel() {
        this.telemetries = new ArrayList<>();
    }

    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return columnClass[columnIndex];
    }

    @Override
    public int getRowCount() {
        return telemetries.size();
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Telemetry telemetry = telemetries.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> telemetry.getTimestamp();
            case 1 -> telemetry.getDuration();
            case 2 -> telemetry;
            default -> null;
        };
    }

    public void addRow(Telemetry telemetry) {
        this.telemetries.add(telemetry);
        this.fireTableRowsInserted(this.telemetries.size() - 1, this.telemetries.size() - 1);
    }

    public void addRow(int i, Telemetry telemetry) {
        this.telemetries.add(i, telemetry);
        this.fireTableRowsInserted(this.telemetries.size() - 1, this.telemetries.size() - 1);
    }

    public void setRows(List<Telemetry> telemetries) {
        int previousSize = this.telemetries.size();
        this.telemetries.clear();
        this.fireTableRowsDeleted(0, Math.max(0, previousSize - 1));
        this.telemetries.addAll(telemetries);
        this.fireTableRowsInserted(0, Math.max(0, this.telemetries.size() - 1));
    }

    @Nullable
    public Telemetry getRow(int selectedRow) {
        if (selectedRow < 0)
            return null;
        if (selectedRow >= this.getRowCount())
            return null;
        return telemetries.get(selectedRow);
    }
}
