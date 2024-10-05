package io.github.ozkanpakdil.opentelemetry.ui.components;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import io.github.ozkanpakdil.opentelemetry.OpentelemetryBundle;
import io.github.ozkanpakdil.opentelemetry.settings.FilterTelemetryMode;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.function.Supplier;

public class OptionsToolbarAction extends AnAction {
    private Supplier<Component> toolbarComponent;

    public OptionsToolbarAction(Supplier<Component> toolbarComponent) {
        super();
        this.toolbarComponent = toolbarComponent;

        String message = OpentelemetryBundle.message("action.opentelemetryaction.ShowOptionsMenu");
        this.getTemplatePresentation().setDescription(message);
        this.getTemplatePresentation().setText(message);
        this.getTemplatePresentation().setIcon(AllIcons.General.Gear);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        DefaultActionGroup actionGroup = new DefaultActionGroup();
        actionGroup.add(new FilterIndicatorToolbarAction());
        actionGroup.add(new Separator());
        actionGroup.add(new ChangeFilterModeToolbarAction(FilterTelemetryMode.DEFAULT));
        actionGroup.add(new ChangeFilterModeToolbarAction(FilterTelemetryMode.DURATION));
        actionGroup.add(new ChangeFilterModeToolbarAction(FilterTelemetryMode.TIMESTAMP));
        actionGroup.add(new Separator());
        actionGroup.add(new OpenSettingsToolbarAction());

        ActionManager.getInstance().createActionPopupMenu("AIDLV_OpenSettingsMenu", actionGroup)
                .getComponent()
                .show(toolbarComponent.get(), 15, 15);
    }
}
