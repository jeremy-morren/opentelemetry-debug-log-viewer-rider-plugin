package io.github.ozkanpakdil.opentelemetry;

import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.content.Content;
import com.jetbrains.rd.util.lifetime.Lifetime;
import com.jetbrains.rider.debugger.DotNetDebugProcess;
import io.github.ozkanpakdil.opentelemetry.settings.AppSettingState;
import io.github.ozkanpakdil.opentelemetry.settings.FilterTelemetryMode;
import io.github.ozkanpakdil.opentelemetry.settings.ProjectSettingsState;
import io.github.ozkanpakdil.opentelemetry.ui.OpenTelemetryToolWindow;
import kotlin.Unit;
import org.eclipse.lsp4j.jsonrpc.validation.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public class OpentelemetrySession {
    @NotNull
    private static final Icon ICON = IconLoader.getIcon("/icons/pluginIcon.svg", OpentelemetrySession.class);
    @NotNull
    private final TelemetryFactory telemetryFactory;
    @NotNull
    private final DotNetDebugProcess dotNetDebugProcess;
    @NotNull
    private final List<Telemetry> telemetries = new ArrayList<>();
    @NotNull
    private final List<Telemetry> filteredTelemetries = new ArrayList<>();
    @NotNull
    private final Lifetime lifetime;
    @NotNull
    private String filter = "";
    private String filterLowerCase = "";
    @Nullable
    private OpenTelemetryToolWindow openTelemetryToolWindow;
    private boolean firstMessage = true;
    private final ProjectSettingsState projectSettingsState;

    public OpentelemetrySession(
            @NotNull TelemetryFactory telemetryFactory,
            @NotNull DotNetDebugProcess dotNetDebugProcess
    ) {
        this.telemetryFactory = telemetryFactory;
        this.dotNetDebugProcess = dotNetDebugProcess;
        this.lifetime = dotNetDebugProcess.getSessionLifetime();

        projectSettingsState = ProjectSettingsState.getInstance(dotNetDebugProcess.getProject());

        AppSettingState.getInstance().filterTelemetryMode.advise(lifetime, (v) -> {
            this.updateFilteredTelemetries();
            return Unit.INSTANCE;
        });
        AppSettingState.getInstance().caseInsensitiveSearch.advise(lifetime, (v) -> {
            this.updateFilteredTelemetries();
            return Unit.INSTANCE;
        });
        projectSettingsState.filteredLogs.advise(lifetime, (v) -> {
            this.updateFilteredTelemetries();
            return Unit.INSTANCE;
        });
        projectSettingsState.caseInsensitiveFiltering.advise(lifetime, (v) -> {
            this.updateFilteredTelemetries();
            return Unit.INSTANCE;
        });
    }

    public void startListeningToOutputDebugMessage() {
        dotNetDebugProcess.getSessionProxy().getTargetDebug().advise(lifetime, outputMessageWithSubject -> {
            Telemetry telemetry = telemetryFactory.tryCreateFromDebugOutputLog(outputMessageWithSubject.getOutput());
            if (telemetry != null) {
                addTelemetry(telemetry);
            }
            return Unit.INSTANCE;
        });
    }

    public void updateFilter(@NonNull String filter) {
        this.filter = filter;
        this.filterLowerCase = filter.toLowerCase(Locale.ROOT);
        updateFilteredTelemetries();
    }

    public void clear() {
        this.telemetries.clear();
        updateFilteredTelemetries();
    }

    private void addTelemetry(@NotNull Telemetry telemetry) {
        if (firstMessage) {
            firstMessage = false;

            openTelemetryToolWindow = new OpenTelemetryToolWindow(this, dotNetDebugProcess.getProject(), lifetime);

            Content content = dotNetDebugProcess.getSession().getUI().createContent(
                    "opentelemetry",
                    openTelemetryToolWindow.getContent(),
                    "Opentelemetry",
                    ICON,
                    null
            );
            dotNetDebugProcess.getSession().getUI().addContent(content);
        }

        int index = -1;
        boolean visible = false;
        synchronized (telemetries) {
            telemetries.add(telemetry);
            if (isTelemetryVisible(telemetry)) {
                FilterTelemetryMode value = AppSettingState.getInstance().filterTelemetryMode.getValue();
                switch (value) {
                    case TIMESTAMP:
                        index = Collections.binarySearch(filteredTelemetries, telemetry,
                                Comparator.comparing(Telemetry::getTimestamp));
                        if (index < 0)
                            index = ~index;
                        filteredTelemetries.add(index, telemetry);
                        break;
                    case DURATION:
                        index = Collections.binarySearch(filteredTelemetries, telemetry,
                                Comparator.comparing(Telemetry::getDuration));
                        if (index < 0)
                            index = ~index;
                        filteredTelemetries.add(index, telemetry);
                        break;
                    default:
                        filteredTelemetries.add(telemetry);
                        break;
                }
                visible = true;
            }
        }
        if (openTelemetryToolWindow != null)
            openTelemetryToolWindow.addTelemetry(index, telemetry, visible,
                    AppSettingState.getInstance().filterTelemetryMode.getValue() == FilterTelemetryMode.DEFAULT);
    }

    private void updateFilteredTelemetries() {
        synchronized (telemetries) {
            filteredTelemetries.clear();
            Stream<Telemetry> stream = telemetries.stream().filter(this::isTelemetryVisible);
            stream = switch (AppSettingState.getInstance().filterTelemetryMode.getValue()) {
                case DURATION -> stream.sorted(Comparator.comparing(Telemetry::getDuration));
                case TIMESTAMP -> stream.sorted(Comparator.comparing(Telemetry::getTimestamp));
                default -> stream;
            };
            filteredTelemetries.addAll(stream.toList());
        }
        if (openTelemetryToolWindow != null)
            openTelemetryToolWindow.setTelemetries(telemetries, filteredTelemetries);
    }

    private boolean isTelemetryVisible(@NotNull Telemetry telemetry) {
        for (String filteredLog : projectSettingsState.filteredLogs.getValue()) {
            if (projectSettingsState.caseInsensitiveFiltering.getValue()) {
                if (telemetry.getLowerCaseJson().contains(filteredLog.toLowerCase()))
                    return false;
            } else {
                if (telemetry.getJson().contains(filteredLog))
                    return false;
            }
        }

        if (!filter.isEmpty()) {
            if (AppSettingState.getInstance().caseInsensitiveSearch.getValue())
                return telemetry.getLowerCaseJson().toLowerCase().contains(filterLowerCase);
            else
                return telemetry.getJson().contains(filter);
        }

        return true;
    }
}
