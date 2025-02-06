package io.jeremymorren.opentelemetry.listeners;

import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebuggerManagerListener;
import io.jeremymorren.opentelemetry.OpenTelemetrySessionManager;
import org.jetbrains.annotations.NotNull;

public class DebugMessageListener implements XDebuggerManagerListener {
    @Override
    public void processStarted(@NotNull XDebugProcess debugProcess) {
        OpenTelemetrySessionManager.getInstance().startSession(debugProcess);
    }
}
