package io.jeremymorren.opentelemetry.ui;

import com.intellij.icons.AllIcons;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.intellij.ui.components.fields.ExtendableTextField;

import javax.swing.*;

public class ClearTextFieldExtension implements ExtendableTextComponent.Extension {
    private final ExtendableTextField textField;

    public ClearTextFieldExtension(ExtendableTextField textField) {

        this.textField = textField;
    }

    @Override
    public Icon getIcon(boolean hovered) {
        return hovered ? AllIcons.Actions.CloseHovered : AllIcons.Actions.Close;
    }

    @Override
    public String getTooltip() {
        return "Clear";
    }

    @Override
    public Runnable getActionOnClick() {
        return textField.getText().isEmpty() ? null : () -> textField.setText(null);
    }
}
