package io.jeremymorren.opentelemetry.ui.components;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import io.jeremymorren.opentelemetry.OpenTelemetryBundle;
import io.jeremymorren.opentelemetry.settings.AppSettingState;
import org.jetbrains.annotations.NotNull;

public class FilterIndicatorToolbarAction extends ToggleAction {
    public FilterIndicatorToolbarAction() {
        super();

        String message = OpenTelemetryBundle.message("ToggleFilteredIndicator.text");
        this.getTemplatePresentation().setDescription(message);
        this.getTemplatePresentation().setText(message);
        this.getTemplatePresentation().setIcon(AllIcons.General.Filter);
    }

    @Override
    public boolean isSelected(@NotNull AnActionEvent actionEvent) {
        return AppSettingState.getInstance().showFilteredIndicator.getValue();
    }

    public void setSelected(@NotNull AnActionEvent actionEvent, boolean state) {
        AppSettingState.getInstance().showFilteredIndicator.setValue(state);
    }
    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
