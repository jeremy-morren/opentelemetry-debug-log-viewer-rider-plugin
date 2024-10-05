package io.github.ozkanpakdil.opentelemetry.listeners;

import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebuggerManagerListener;
import io.github.ozkanpakdil.opentelemetry.OpenTelemetrySessionManager;
import org.jetbrains.annotations.NotNull;

public class DebugMessageListener implements XDebuggerManagerListener {
    @Override
    public void processStarted(@NotNull XDebugProcess debugProcess) {
        OpenTelemetrySessionManager.getInstance().startSession(debugProcess);
    }
}
