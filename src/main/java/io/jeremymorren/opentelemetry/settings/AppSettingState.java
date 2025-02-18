package io.jeremymorren.opentelemetry.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponentWithModificationTracker;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.util.SimpleModificationTracker;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.intellij.util.xmlb.annotations.OptionTag;
import com.jetbrains.rd.util.lifetime.LifetimeDefinition;
import com.jetbrains.rd.util.reactive.Property;
import io.jeremymorren.opentelemetry.settings.converters.BooleanPropertyConverter;
import io.jeremymorren.opentelemetry.settings.converters.FilterTelemetryModePropertyConverter;
import kotlin.Unit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@State(
        name = "io.jeremymorren.opentelemetry.settings.AppSettingsState",
        storages = @Storage("opentelemetry-debug-log-viewer.xml")
)
public class AppSettingState implements PersistentStateComponentWithModificationTracker<AppSettingState> {
    private final SimpleModificationTracker tracker = new SimpleModificationTracker();

    @OptionTag(converter = FilterTelemetryModePropertyConverter.class)
    public final Property<FilterTelemetryMode> filterTelemetryMode = new Property<>(FilterTelemetryMode.Default);
    @OptionTag(converter = BooleanPropertyConverter.class)
    public final Property<Boolean> caseInsensitiveSearch = new Property<>(false);

    public AppSettingState() {
        registerAllPropertyToIncrementTrackerOnChanges(this);
    }

    public static AppSettingState getInstance() {
        return ApplicationManager.getApplication().getService(AppSettingState.class);
    }

    @Override
    public @Nullable AppSettingState getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull AppSettingState state) {
        XmlSerializerUtil.copyBean(state, this);
        registerAllPropertyToIncrementTrackerOnChanges(state);
    }

    @Override
    public long getStateModificationCount() {
        return this.tracker.getModificationCount();
    }

    private void registerAllPropertyToIncrementTrackerOnChanges(@NotNull AppSettingState state) {
        incrementTrackerWhenPropertyChanges(caseInsensitiveSearch);
        incrementTrackerWhenPropertyChanges(filterTelemetryMode);
    }

    private <T> void incrementTrackerWhenPropertyChanges(Property<T> property) {
        property.advise(new LifetimeDefinition(), v -> {
            this.tracker.incModificationCount();
            return Unit.INSTANCE;
        });
    }
}
