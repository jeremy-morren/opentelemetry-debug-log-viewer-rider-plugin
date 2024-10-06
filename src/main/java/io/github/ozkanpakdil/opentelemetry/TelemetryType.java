package io.github.ozkanpakdil.opentelemetry;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum TelemetryType {
    Message("Message", "displayName"),
    Request("Request", "url.path"),
    Exception("Exception", "exceptions"),
    Duration("Duration", "duration"),
    Unk();

    private final String[] typeNames;

    TelemetryType(@Nullable String... typeNames) {
        this.typeNames = typeNames;
    }

    @NotNull
    public static TelemetryType fromType(@NotNull String typeName) {
        String name = typeName.substring(typeName.lastIndexOf('.') + 1);
        for (TelemetryType type : TelemetryType.values()) {
            for (String n : type.typeNames) {
                if (name.equals(n))
                    return type;
            }
        }
        return Unk;
    }
}
