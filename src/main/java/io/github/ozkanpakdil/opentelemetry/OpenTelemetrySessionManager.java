package io.github.ozkanpakdil.opentelemetry;

import com.intellij.xdebugger.XDebugProcess;
import com.jetbrains.rider.debugger.DotNetDebugProcess;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OpenTelemetrySessionManager {
    @Nullable
    private static OpenTelemetrySessionManager instance;
    @NotNull
    private final TelemetryFactory telemetryFactory = new TelemetryFactory();

    @NotNull
    public static OpenTelemetrySessionManager getInstance() {
        if (instance == null)
            instance = new OpenTelemetrySessionManager();
        return instance;
    }

    private OpenTelemetrySessionManager() {
    }

    @Nullable
    public void startSession(XDebugProcess debugProcess) {
        if (!(debugProcess instanceof DotNetDebugProcess))
            return;

        OpentelemetrySession opentelemetrySession = new OpentelemetrySession(
                telemetryFactory,
                (DotNetDebugProcess) debugProcess
        );
        opentelemetrySession.startListeningToOutputDebugMessage();
    }
}
