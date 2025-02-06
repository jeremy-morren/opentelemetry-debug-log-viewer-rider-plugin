package io.jeremymorren.opentelemetry.ui.components;

import com.intellij.icons.AllIcons;
import com.intellij.idea.ActionsBundle;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class AutoScrollToTheEndToolbarAction extends ToggleAction {
    private boolean state;
    private final Consumer<Boolean> onSelect;

    public AutoScrollToTheEndToolbarAction(Consumer<Boolean> onSelect, boolean selected) {
        super();
        this.onSelect = onSelect;
        this.state = selected;

        String message = ActionsBundle.message("action.EditorConsoleScrollToTheEnd.text");
        this.getTemplatePresentation().setDescription(message);
        this.getTemplatePresentation().setText(message);
        this.getTemplatePresentation().setIcon(AllIcons.RunConfigurations.Scroll_down);
    }

    @Override
    public boolean isSelected(@NotNull AnActionEvent actionEvent) {
        return state;
    }

    public void setSelected(@NotNull AnActionEvent actionEvent, boolean state) {
        this.state = state;
        this.onSelect.accept(state);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
