package io.jeremymorren.opentelemetry;

import com.intellij.DynamicBundle;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

import java.util.function.Supplier;

public class OpenTelemetryBundle extends DynamicBundle {
    @NonNls
    private static final String BUNDLE = "messages.Bundle";
    private static final OpenTelemetryBundle INSTANCE = new OpenTelemetryBundle();

    private OpenTelemetryBundle() {
        super(BUNDLE);
    }

    @NotNull
    public static String message(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key, Object ... params) {
        return INSTANCE.getMessage(key, params);
    }
}
