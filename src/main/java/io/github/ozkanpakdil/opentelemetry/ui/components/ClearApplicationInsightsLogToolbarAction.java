package io.github.ozkanpakdil.opentelemetry.ui.components;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import io.github.ozkanpakdil.opentelemetry.OpentelemetryBundle;

public abstract class ClearApplicationInsightsLogToolbarAction extends AnAction {
    public ClearApplicationInsightsLogToolbarAction() {
        String message = OpentelemetryBundle.message("ClearLogs.text");
        this.getTemplatePresentation().setDescription(message);
        this.getTemplatePresentation().setText(message);
        this.getTemplatePresentation().setIcon(AllIcons.Actions.GC);
    }

    @Override
    public boolean isDumbAware() {
        return false;
    }
}
