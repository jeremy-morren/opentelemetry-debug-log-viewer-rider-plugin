package io.jeremymorren.opentelemetry.ui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intellij.codeInsight.folding.CodeFoldingManager;
import com.intellij.execution.filters.TextConsoleBuilder;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.json.JsonFileType;
import com.intellij.json.JsonLanguage;
import com.intellij.lang.Language;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.EditorKind;
import com.intellij.openapi.editor.actions.AbstractToggleUseSoftWrapsAction;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory;
import com.intellij.openapi.editor.impl.softwrap.SoftWrapAppliancePlaces;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.ui.JBColor;
import com.intellij.ui.LanguageTextField;
import com.intellij.ui.components.fields.ExtendableTextField;
import com.intellij.ui.table.JBTable;
import com.intellij.unscramble.AnalyzeStacktraceUtil;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.ui.JBUI;
import com.jetbrains.rd.util.lifetime.Lifetime;
import io.jeremymorren.opentelemetry.Activity;
import io.jeremymorren.opentelemetry.TelemetryType;
import io.jeremymorren.opentelemetry.OpenTelemetrySession;
import io.jeremymorren.opentelemetry.Telemetry;
import io.jeremymorren.opentelemetry.settings.AppSettingState;
import io.jeremymorren.opentelemetry.ui.components.*;
import io.jeremymorren.opentelemetry.ui.renderers.InstantRenderer;
import io.jeremymorren.opentelemetry.ui.renderers.ActivityRenderer;
import io.jeremymorren.opentelemetry.ui.renderers.TelemetryTypeRenderer;
import kotlin.Unit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class OpenTelemetryToolWindow {
    @NotNull
    private JPanel mainPanel;
    @NotNull
    private JBTable logsTable;
    @NotNull
    private JSplitPane splitPane;
    @NotNull
    private ExtendableTextField filter;
    @NotNull
    private JScrollPane logsScrollPane;
    @NotNull
    private ActionToolbarImpl toolbar;
    @NotNull
    private JComponent jsonPanel;
    private JComponent sqlPanel;
    private JTabbedPane tabbedPane;
    private JPanel formattedInfo;
    private JScrollPane formattedInfoScrollPane;

    private JCheckBox activityCheckBox;
    private JCheckBox dependencyCheckBox;
    private JCheckBox requestCheckBox;
    private JLabel activityCounter;
    private JLabel dependencyCounter;
    private JLabel requestCounter;
    @NotNull
    private ColorBox activityColorBox;
    @NotNull
    private ColorBox dependencyColorBox;
    @NotNull
    private ColorBox requestColorBox;

    @NotNull
    private final Project project;
    @NotNull
    private final OpenTelemetrySession openTelemetrySession;

    @NotNull
    private Editor jsonEditor;
    @NotNull
    private Editor sqlEditor;

    @NotNull
    private Document jsonPreviewDocument;

    @NotNull
    private Document sqlPreviewDocument;

    @NotNull
    private final TelemetryTableModel telemetryTableModel;

    @NotNull
    private final ArrayList<JLabel> telemetryTypesCounter = new ArrayList<>();
    @NotNull
    private final Map<TelemetryType, Integer> telemetryCountPerType = new HashMap<>();

    private boolean autoScrollToTheEnd;
    private final TextConsoleBuilder builder;

    public OpenTelemetryToolWindow(
            @NotNull OpenTelemetrySession opentelemetrySession,
            @NotNull Project project,
            Lifetime lifetime) {
        this.project = project;
        this.openTelemetrySession = opentelemetrySession;

        initTelemetryTypeFilters();

        splitPane.setDividerLocation(0.5);
        splitPane.setResizeWeight(0.5);
        try {
            ReadAction.nonBlocking(() -> {
                return CodeFoldingManager.getInstance(ProjectManager.getInstance().getDefaultProject())
                        .buildInitialFoldings(jsonPreviewDocument);
            }).submit(AppExecutorUtil.getAppExecutorService()).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        //Increase scroll speed
        formattedInfoScrollPane.getVerticalScrollBar().setUnitIncrement(12);
        formattedInfoScrollPane.getHorizontalScrollBar().setUnitIncrement(12);

        logsTable.setDefaultRenderer(Activity.class, new ActivityRenderer());
        logsTable.setDefaultRenderer(Instant.class, new InstantRenderer());
        logsTable.setDefaultRenderer(TelemetryType.class, new TelemetryTypeRenderer());
        logsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        telemetryTableModel = new TelemetryTableModel();
        logsTable.setModel(telemetryTableModel);
        logsTable.getColumnModel().getColumn(0).setPreferredWidth(90);
        logsTable.getColumnModel().getColumn(0).setMaxWidth(130);
        logsTable.getColumnModel().getColumn(1).setPreferredWidth(75);
        logsTable.getColumnModel().getColumn(1).setMaxWidth(100);
        logsTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        logsTable.getColumnModel().getColumn(2).setMaxWidth(130);
        logsTable.getTableHeader().setUI(null);

        filter.setExtensions(new ClearTextFieldExtension(filter));

        filter.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
            }

            @Override
            public void keyPressed(KeyEvent e) {
                OpenTelemetryToolWindow.this.openTelemetrySession.updateFilter(filter.getText());
            }

            @Override
            public void keyReleased(KeyEvent e) {
                OpenTelemetryToolWindow.this.openTelemetrySession.updateFilter(filter.getText());
            }
        });

        logsTable.getSelectionModel().addListSelectionListener(e -> {
            selectTelemetry(telemetryTableModel.getRow(logsTable.getSelectedRow()));
        });

        builder = TextConsoleBuilderFactory.getInstance().createBuilder(project);
        builder.filters(AnalyzeStacktraceUtil.EP_NAME.getExtensions(project));

        AppSettingState.getInstance().showFilteredIndicator.advise(lifetime, (v) -> {
            this.logsTable.invalidate();
            this.logsTable.repaint();
            return Unit.INSTANCE;
        });
    }

    private void selectTelemetry(@Nullable Telemetry telemetry) {
        if (telemetry == null)
        {
            return;
        }
        updateJsonPreview(telemetry.getJson());
        updateSqlPreview(telemetry.getSql());
        updateFormattedDisplay(telemetry.getTelemetry().getActivity());
    }

    @NotNull
    public JPanel getContent() {
        return mainPanel;
    }

    public void setTelemetries(
            @NotNull List<Telemetry> telemetries,
            @NotNull List<Telemetry> visibleTelemetries
    ) {
        telemetryTableModel.setRows(visibleTelemetries);
    }

    public void addTelemetry(
            int index,
            @NotNull Telemetry telemetry,
            boolean visible,
            boolean shouldScroll
    ) {
        if (visible) {
            if (index != -1)
                telemetryTableModel.addRow(index, telemetry);
            else
                telemetryTableModel.addRow(telemetry);
            SwingUtilities.invokeLater(() -> {
                if (autoScrollToTheEnd && shouldScroll) {
                    performAutoScrollToTheEnd();
                }
            });
        }
        updateTelemetryTypeCounter(telemetry);
    }

    private void performAutoScrollToTheEnd() {
        logsTable.scrollRectToVisible(
                logsTable.getCellRect(telemetryTableModel.getRowCount() - 1, 0, true));
    }

    private void createUIComponents() {
        activityColorBox = new ColorBox(JBColor.namedColor("OpenTelemetry.TelemetryColor.Activity", JBColor.orange));
        dependencyColorBox = new ColorBox(JBColor.namedColor("OpenTelemetry.TelemetryColor.Request", JBColor.blue));
        requestColorBox = new ColorBox(JBColor.namedColor("OpenTelemetry.TelemetryColor.Dependency", JBColor.green));

        toolbar = createToolbar();
        toolbar.setTargetComponent(mainPanel);
        toolbar.setVisible(false);

        jsonPreviewDocument = new LanguageTextField.SimpleDocumentCreator().createDocument("", JsonLanguage.INSTANCE, project);
        sqlPreviewDocument = new LanguageTextField.SimpleDocumentCreator().createDocument("", Language.findLanguageByID("SQL"), project);

        jsonEditor = EditorFactory.getInstance().createViewer(jsonPreviewDocument, project, EditorKind.MAIN_EDITOR);
        sqlEditor = EditorFactory.getInstance().createViewer(sqlPreviewDocument, project, EditorKind.MAIN_EDITOR);

        Editor[] editors = {jsonEditor, sqlEditor};
        for (Editor editor : editors) {
            if (editor instanceof EditorEx) {
                ((EditorEx) editor).setHighlighter(
                        EditorHighlighterFactory.getInstance().createEditorHighlighter(project, JsonFileType.INSTANCE));
                ((EditorEx) editor).getFoldingModel().setFoldingEnabled(true);
            }
            editor.getSettings().setIndentGuidesShown(true);
            editor.getSettings().setAdditionalLinesCount(3);
            editor.getSettings().setFoldingOutlineShown(true);
            editor.getSettings().setUseSoftWraps(
                    PropertiesComponent.getInstance().getBoolean("io.jeremymorren.opentelemetry.useSoftWrap"));
        }

        jsonPanel = jsonEditor.getComponent();
        sqlPanel = sqlEditor.getComponent();
    }

    @NotNull
    private ActionToolbarImpl createToolbar() {
        final DefaultActionGroup actionGroup = new DefaultActionGroup();

        actionGroup.add(new OptionsToolbarAction(() -> toolbar));
        actionGroup.addSeparator();

        autoScrollToTheEnd = PropertiesComponent.getInstance()
                .getBoolean("io.jeremymorren.opentelemetry.autoScrollToTheEnd");

        actionGroup.add(new AutoScrollToTheEndToolbarAction(this::acceptScrollToEnd, autoScrollToTheEnd));

        actionGroup.add(new ToggleCaseInsensitiveSearchToolbarAction());

        actionGroup.add(new AbstractToggleUseSoftWrapsAction(SoftWrapAppliancePlaces.PREVIEW, false) {
            {
                ActionUtil.copyFrom(this, "EditorToggleUseSoftWraps");
            }

            @Override
            public void setSelected(@NotNull AnActionEvent e, boolean state) {
                super.setSelected(e, state);
                PropertiesComponent.getInstance().setValue("io.jeremymorren.opentelemetry.useSoftWrap", state);
            }

            @NotNull
            @Override
            protected Editor getEditor(@NotNull AnActionEvent e) {
                return jsonEditor;
            }
        });

        actionGroup.add(new ClearApplicationInsightsLogToolbarAction() {
            @Override
            public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
                openTelemetrySession.clear();
            }
        });

        return new ActionToolbarImpl("OpenTelemetry", actionGroup, false);
    }

    private void acceptScrollToEnd(Boolean selected) {
        autoScrollToTheEnd = selected;
        PropertiesComponent.getInstance().setValue("io.jeremymorren.opentelemetry.autoScrollToTheEnd", selected);
        if (autoScrollToTheEnd) {
            performAutoScrollToTheEnd();
        }
    }

    private void updateTelemetryTypeCounter(@Nullable Telemetry telemetry)
    {
        if (telemetry == null || telemetry.getType() == null)
            return;
        var count = telemetryCountPerType.getOrDefault(telemetry.getType(), 0);
        count++;
        telemetryCountPerType.put(telemetry.getType(), count);

        var text = count.toString();
        if (count > 1_000_000)
        {
            text = (count / 1_000_000) + "M";
        }
        else if (count > 1_000)
        {
            text = (count / 1_000) + "K";
        }

        for (JLabel counter: telemetryTypesCounter)
        {
            TelemetryType telemetryType = (TelemetryType) counter.getClientProperty("TelemetryType");
            if (telemetryType != telemetry.getType())
                continue;
            counter.setText(text);
        }
    }

    private void initTelemetryTypeFilters() {
        activityCounter.putClientProperty("TelemetryType", TelemetryType.Activity);
        dependencyCounter.putClientProperty("TelemetryType", TelemetryType.Dependency);
        requestCounter.putClientProperty("TelemetryType", TelemetryType.Request);

        telemetryTypesCounter.addAll(Arrays.asList(activityCounter, dependencyCounter, requestCounter));

        activityCheckBox.putClientProperty("TelemetryType", TelemetryType.Activity);
        dependencyCheckBox.putClientProperty("TelemetryType", TelemetryType.Dependency);
        requestCheckBox.putClientProperty("TelemetryType", TelemetryType.Request);

        for (JCheckBox checkBox: new JCheckBox[]{activityCheckBox, dependencyCheckBox, requestCheckBox})
        {
            var type = (TelemetryType) checkBox.getClientProperty("TelemetryType");
            checkBox.setSelected(openTelemetrySession.isTelemetryVisible(type));
            checkBox.addItemListener(e ->
                    openTelemetrySession.setTelemetryVisible(type, e.getStateChange() == ItemEvent.SELECTED));
        }
    }

    private void updateJsonPreview(String json) {
        ApplicationManager.getApplication().runWriteAction(() -> jsonPreviewDocument.setText(json));

        CodeFoldingManager.getInstance(ProjectManager.getInstance().getDefaultProject())
                .updateFoldRegions(jsonEditor);
    }

    private void updateSqlPreview(@Nullable String sql) {
        if (sql == null) {
            //No SQL for this telemetry. Disable the SQL tab and select the first tab
            tabbedPane.setEnabledAt(2, false);
            if (tabbedPane.getSelectedIndex() == 2)
                tabbedPane.setSelectedIndex(0);
            return;
        }
        tabbedPane.setEnabledAt(2, true);

        ApplicationManager.getApplication().runWriteAction(() -> sqlPreviewDocument.setText(sql));

        CodeFoldingManager.getInstance(ProjectManager.getInstance().getDefaultProject())
                .updateFoldRegions(jsonEditor);
    }

    private void updateFormattedDisplay(@NotNull Activity activity) {
        // Show information about the telemetry
        formattedInfo.removeAll();

        int indent = 30; //Indentation for subfields

        int row = 1;
        if (activity.getRootId() != null) {
            //Add root id
            formattedInfo.add(createTitleLabel("RootId"), createConstraint(row++, 0));
            formattedInfo.add(createFilterLabel("RootId", activity.getRootId()), createConstraint(row++, indent));
        }

        //Show activity information
        formattedInfo.add(createTitleLabel(activity.getTypeDisplay()), createConstraint(row++, 0));
        if (activity.getActivitySource() != null)
        {
            formattedInfo.add(createFilterLabel("Source", activity.getActivitySource()), createConstraint(row++, indent));
        }
        if (activity.getElapsed() != null) {
            formattedInfo.add(new JLabel("Duration: " + activity.getElapsed().toString()), createConstraint(row++, indent));
        }
        if (activity.getDisplayName() != null) {
            formattedInfo.add(createFilterLabel("Display name", activity.getDisplayName()), createConstraint(row++, indent));
        }
        if (activity.getOperationName() != null) {
            formattedInfo.add(createFilterLabel("Operation", activity.getOperationName()), createConstraint(row++, indent));
        }
        if (activity.getStatus() != null) {
            formattedInfo.add(new JLabel("Status: " + activity.getStatus()), createConstraint(row++, indent));
        }
        if (activity.getDbQueryTime() != null) {
            var label = new JLabel("DB Time: " + activity.getDbQueryTime());
            label.setToolTipText("Time spent before first response received");
            formattedInfo.add(label, createConstraint(row++, indent));
        }
        if (activity.getTags() != null) {
            formattedInfo.add(createTitleLabel("Tags"), createConstraint(row++, 0));
            for (Map.Entry<String, String> entry : activity.getTags().entrySet()) {
                var label = createFilterLabel(entry.getKey(), entry.getValue());
                formattedInfo.add(label, createConstraint(row++, indent));
            }
        }
        //Add trace information
        formattedInfo.add(createTitleLabel("Trace"), createConstraint(row++, 0));
        for (Map.Entry<String, String> entry : activity.getTraceIds().entrySet()) {
            var label = createFilterLabel(entry.getKey(), entry.getValue());
            formattedInfo.add(label, createConstraint(row++, indent));
        }

        // Padding
        {
            GridBagConstraints c = createConstraint(10_000, 0);
            c.weighty = 1;
            formattedInfo.add(new JPanel(), c);
        }

        formattedInfo.revalidate();
        formattedInfo.repaint();
    }

    private JLabel createFilterLabel(String label, String value) {
        var display = value.replace("\r", "").replace("\n", " ");
        if (display.length() > 100) {
            display = display.substring(0, 100) + "...";
        }
        JLabel jLabel = new JLabel("<html>" + escapeHtml(label) + ": " + "<a href=''>" + escapeHtml(display) + "</a></html>");
        jLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        jLabel.addMouseListener(new ClickListener(e -> {
            openTelemetrySession.updateFilter(value);
            this.filter.setText(value);
        }));
        return jLabel;
    }

    @NotNull
    private JLabel createTitleLabel(String label) {
        JLabel title = new JLabel("<html><b>" + escapeHtml(label) + "</b></html>");
        Font font = title.getFont();
        font.deriveFont(Font.BOLD);
        title.setFont(font);
        return title;
    }

    @NotNull
    private GridBagConstraints createConstraint(int y, int padX) {
        return createConstraint(0, y, padX);
    }
    private GridBagConstraints createConstraint(int x, int y, int padX) {
        GridBagConstraints gridConstraints = new GridBagConstraints();
        gridConstraints.gridx = x;
        gridConstraints.gridy = y;
        gridConstraints.gridheight = 1;
        gridConstraints.gridwidth = 1;
        gridConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridConstraints.weightx = 1;
        gridConstraints.weighty = 0;
        gridConstraints.anchor = GridBagConstraints.NORTHEAST;
        gridConstraints.insets = JBUI.insetsLeft(padX);
        return gridConstraints;
    }

    private static String escapeHtml(String s) {
        return org.apache.commons.text.StringEscapeUtils.escapeHtml4(s);
    }
}

