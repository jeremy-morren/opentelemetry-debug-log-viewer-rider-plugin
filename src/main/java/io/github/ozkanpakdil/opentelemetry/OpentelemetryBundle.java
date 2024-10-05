package io.github.ozkanpakdil.opentelemetry;

import com.intellij.DynamicBundle;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

import java.util.function.Supplier;

public class OpentelemetryBundle extends DynamicBundle {
    @NonNls
    private static final String BUNDLE = "messages.Bundle";
    private static final OpentelemetryBundle INSTANCE = new OpentelemetryBundle();

    private OpentelemetryBundle() {
        super(BUNDLE);
    }

    @NotNull
    public static String message(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key, Object ... params) {
        return INSTANCE.getMessage(key, params);
    }


    @NotNull
    public static Supplier<String> messagePointer(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key, Object ... params) {
        return INSTANCE.getLazyMessage(key, params);
    }

}
