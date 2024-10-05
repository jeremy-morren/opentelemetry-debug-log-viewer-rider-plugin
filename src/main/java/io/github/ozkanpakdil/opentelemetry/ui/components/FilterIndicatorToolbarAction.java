package io.github.ozkanpakdil.opentelemetry.ui.components;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import io.github.ozkanpakdil.opentelemetry.OpentelemetryBundle;
import io.github.ozkanpakdil.opentelemetry.settings.AppSettingState;
import org.jetbrains.annotations.NotNull;

public class FilterIndicatorToolbarAction extends ToggleAction {
    public FilterIndicatorToolbarAction() {
        super();

        String message = OpentelemetryBundle.message("action.opentelemetryaction.ToggleFilteredIndicator.text");
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
}
