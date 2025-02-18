package io.jeremymorren.opentelemetry.ui;

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
import io.jeremymorren.opentelemetry.*;
import io.jeremymorren.opentelemetry.models.*;
import io.jeremymorren.opentelemetry.ui.components.*;
import io.jeremymorren.opentelemetry.ui.renderers.InstantRenderer;
import io.jeremymorren.opentelemetry.ui.renderers.TelemetryRenderer;
import io.jeremymorren.opentelemetry.ui.renderers.TelemetryTypeRenderer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.text.DecimalFormat;
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
    private ColorBox metricColorBox;
    private JCheckBox metricCheckBox;
    private JLabel metricCounter;
    private ColorBox exceptionColorBox;
    private JCheckBox exceptionCheckBox;
    private JLabel exceptionCounter;
    private ColorBox messageColorBox;
    private JCheckBox messageCheckBox;
    private JLabel messageCounter;
    private JComponent exceptionPanel;

    @NotNull
    private final Project project;
    @NotNull
    private final OpenTelemetrySession openTelemetrySession;

    @NotNull
    private Editor jsonEditor;
    @NotNull
    private Editor sqlEditor;
    @NotNull
    private Editor exceptionEditor;

    @NotNull
    private Document jsonPreviewDocument;

    @NotNull
    private Document sqlPreviewDocument;

    @NotNull
    private Document exceptionViewDocument;

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

        logsTable.setDefaultRenderer(Telemetry.class, new TelemetryRenderer());
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
    }

    private void selectTelemetry(@Nullable TelemetryItem telemetry) {
        if (telemetry == null)
        {
            return;
        }
        updateJsonPreview(telemetry.getJson());
        updateSqlPreview(telemetry.getTelemetry().getSql());
        updateExceptionView(telemetry.getTelemetry().getLog());
        updateFormattedDisplay(telemetry.getTelemetry());
    }

    @NotNull
    public JPanel getContent() {
        return mainPanel;
    }

    public void setTelemetries(
            @NotNull List<TelemetryItem> telemetries,
            @NotNull List<TelemetryItem> visibleTelemetries
    ) {
        telemetryTableModel.setRows(visibleTelemetries);
    }

    public void addTelemetry(
            int index,
            @NotNull TelemetryItem telemetry,
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
        toolbar = createToolbar();
        toolbar.setTargetComponent(mainPanel);
        toolbar.setVisible(false);

        jsonPreviewDocument = new LanguageTextField.SimpleDocumentCreator().createDocument("", JsonLanguage.INSTANCE, project);
        sqlPreviewDocument = new LanguageTextField.SimpleDocumentCreator().createDocument("", Language.findLanguageByID("SQL"), project);
        exceptionViewDocument = new LanguageTextField.SimpleDocumentCreator().createDocument("", Language.ANY, project);

        jsonEditor = EditorFactory.getInstance().createViewer(jsonPreviewDocument, project, EditorKind.MAIN_EDITOR);
        sqlEditor = EditorFactory.getInstance().createViewer(sqlPreviewDocument, project, EditorKind.MAIN_EDITOR);
        exceptionEditor = EditorFactory.getInstance().createViewer(exceptionViewDocument, project, EditorKind.MAIN_EDITOR);

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
        exceptionPanel = exceptionEditor.getComponent();

        metricColorBox = new ColorBox(JBColor.namedColor("OpenTelemetry.TelemetryColor.Metric", JBColor.gray));
        exceptionColorBox = new ColorBox(JBColor.namedColor("OpenTelemetry.TelemetryColor.Exception", JBColor.red));
        messageColorBox = new ColorBox(JBColor.namedColor("OpenTelemetry.TelemetryColor.Message", JBColor.orange));
        dependencyColorBox = new ColorBox(JBColor.namedColor("OpenTelemetry.TelemetryColor.Request", JBColor.blue));
        requestColorBox = new ColorBox(JBColor.namedColor("OpenTelemetry.TelemetryColor.Dependency", JBColor.green));
        activityColorBox = new ColorBox(JBColor.namedColor("OpenTelemetry.TelemetryColor.Activity", JBColor.cyan));
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

    private void updateTelemetryTypeCounter(@Nullable TelemetryItem telemetry)
    {
        if (telemetry == null) return;
        var type = telemetry.getTelemetry().getType();
        if (type == null) return;
        var count = telemetryCountPerType.getOrDefault(type, 0);
        count++;
        telemetryCountPerType.put(type, count);


        for (JLabel counter: telemetryTypesCounter)
        {
            TelemetryType telemetryType = (TelemetryType) counter.getClientProperty("TelemetryType");
            if (telemetryType == type) {
                counter.setText(format(count));
                break;
            }
        }
    }

    private void initTelemetryTypeFilters() {
        metricCounter.putClientProperty("TelemetryType", TelemetryType.Metric);
        exceptionCounter.putClientProperty("TelemetryType", TelemetryType.Exception);
        messageCounter.putClientProperty("TelemetryType", TelemetryType.Message);
        dependencyCounter.putClientProperty("TelemetryType", TelemetryType.Dependency);
        requestCounter.putClientProperty("TelemetryType", TelemetryType.Request);
        activityCounter.putClientProperty("TelemetryType", TelemetryType.Activity);

        telemetryTypesCounter.addAll(Arrays.asList(metricCounter, exceptionCounter, messageCounter, dependencyCounter, requestCounter, activityCounter));

        metricCheckBox.putClientProperty("TelemetryType", TelemetryType.Metric);
        exceptionCheckBox.putClientProperty("TelemetryType", TelemetryType.Exception);
        messageCheckBox.putClientProperty("TelemetryType", TelemetryType.Message);
        dependencyCheckBox.putClientProperty("TelemetryType", TelemetryType.Dependency);
        requestCheckBox.putClientProperty("TelemetryType", TelemetryType.Request);
        activityCheckBox.putClientProperty("TelemetryType", TelemetryType.Activity);

        for (JCheckBox checkBox: new JCheckBox[]{metricCheckBox, exceptionCheckBox, messageCheckBox, dependencyCheckBox, requestCheckBox, activityCheckBox})
        {
            var type = (TelemetryType) checkBox.getClientProperty("TelemetryType");
            checkBox.setSelected(openTelemetrySession.isTelemetryVisible(type));
            checkBox.addItemListener(e ->
                    openTelemetrySession.setTelemetryVisible(type, e.getStateChange() == ItemEvent.SELECTED));
        }
    }

    private void updateJsonPreview(String json) {
        ApplicationManager.getApplication().runWriteAction(() -> jsonPreviewDocument.setText(json));
        CodeFoldingManager.getInstance(ProjectManager.getInstance().getDefaultProject()).updateFoldRegions(jsonEditor);
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
        CodeFoldingManager.getInstance(ProjectManager.getInstance().getDefaultProject()).updateFoldRegions(sqlEditor);
    }

    private void updateExceptionView(@Nullable LogMessage message) {
        if (message == null || message.getExceptionDisplay() == null) {
            //No exception for this telemetry. Disable the exception tab and select the first tab
            tabbedPane.setEnabledAt(3, false);
            if (tabbedPane.getSelectedIndex() == 3)
                tabbedPane.setSelectedIndex(0);
            return;
        }
        tabbedPane.setEnabledAt(3, true);
        ApplicationManager.getApplication().runWriteAction(() -> exceptionViewDocument.setText(message.getExceptionDisplay()));
        CodeFoldingManager.getInstance(ProjectManager.getInstance().getDefaultProject()).updateFoldRegions(exceptionEditor);
    }

    private void updateFormattedDisplay(@NotNull Telemetry telemetry) {
        // Show information about the telemetry
        formattedInfo.removeAll();

        int indent = 30; //Indentation for subfields

        int row = 1;

        if (telemetry.getActivity() != null) {
            var activity = telemetry.getActivity();

            //Show activity information
            formattedInfo.add(createTitleLabel(activity.getTypeDisplay()), createConstraint(row++, 0));
            if (activity.getSource() != null && activity.getType() == TelemetryType.Activity) {
                formattedInfo.add(createFilterLabel("Source", activity.getSource().getName()), createConstraint(row++, indent));
            }
            if (activity.getDuration() != null) {
                formattedInfo.add(new JLabel("Duration: " + activity.getDuration().toString()), createConstraint(row++, indent));
            }
            if (activity.getDisplayName() != null) {
                formattedInfo.add(createFilterLabel("Display name", activity.getDisplayName()), createConstraint(row++, indent));
            }
            if (activity.getOperationName() != null) {
                formattedInfo.add(createFilterLabel("Operation", activity.getOperationName()), createConstraint(row++, indent));
            }
            if (activity.getErrorDisplay() != null) {
                formattedInfo.add(createFilterLabel("Status", activity.getErrorDisplay()), createConstraint(row++, indent));
            }
            if (activity.getDbQueryTime() != null) {
                var label = new JLabel("DB Time: " + activity.getDbQueryTime());
                label.setToolTipText("Time spent before first response received");
                formattedInfo.add(label, createConstraint(row++, indent));
            }
            if (activity.getDbReadTime() != null) {
                var label = new JLabel("Read Time: " + activity.getDbReadTime());
                label.setToolTipText("Time spent reading data from the database");
                formattedInfo.add(label, createConstraint(row++, indent));
            }

            if (activity.getRequestPath() != null) {
                formattedInfo.add(createFilterLabel("Path", activity.getRequestPath()), createConstraint(row++, indent));
            }
            if (activity.getTags() != null) {
                formattedInfo.add(createTitleLabel("Tags"), createConstraint(row++, 0));
                for (Map.Entry<String, String> entry : activity.getTags().getPrimitiveValues().entrySet()) {
                    var label = createFilterLabel(entry.getKey(), entry.getValue());
                    formattedInfo.add(label, createConstraint(row++, indent));
                }
            }
        }
        if (telemetry.getMetric() != null) {
            var metric = telemetry.getMetric();
            if (metric.getName() != null) {
                formattedInfo.add(createTitleLabel(metric.getName() + " (" + metric.getTemporality() + ")"), createConstraint(row++, 0));
                formattedInfo.add(createFilterLabel("Name", metric.getName()), createConstraint(row++, indent));
            }
            if (metric.getDescription() != null) {
                formattedInfo.add(createFilterLabel("Description", metric.getDescription()), createConstraint(row++, indent));
            }
            if (metric.getTemporality() != null) {
                formattedInfo.add(createFilterLabel("Temporality", metric.getTemporality()), createConstraint(row++, indent));
            }
            if (metric.getMeterName() != null) {
                formattedInfo.add(createFilterLabel("Meter", metric.getMeterName()), createConstraint(row++, indent));
            }
            if (metric.getUnit() != null) {
                formattedInfo.add(createFilterLabel("Unit", metric.getUnit()), createConstraint(row++, indent));
            }
            if (metric.getDuration() != null) {
                formattedInfo.add(new JLabel("Duration: " + metric.getDuration().toString()), createConstraint(row++, indent));
            }
            if (metric.getTaggedPoints() != null) {
                var taggedPoints = metric.getTaggedPoints();
                formattedInfo.add(createTitleLabel("Points"), createConstraint(row++, 0));
                for (var i = 0; i < taggedPoints.size(); i++) {
                    var point = taggedPoints.get(i);
                    if (point.getLongSum() != null) {
                        var sum = format(point.getLongSum());
                        formattedInfo.add(new JLabel("Sum: " + sum), createConstraint(row++, indent));
                    }
                    if (point.getDoubleSum() != null) {
                        var sum = format(point.getDoubleSum());
                        formattedInfo.add(new JLabel("Sum: " + sum), createConstraint(row++, indent));
                    }
                    if (point.getLongGauge() != null) {
                        var gauge = format(point.getLongGauge());
                        formattedInfo.add(new JLabel("Gauge: " + gauge), createConstraint(row++, indent));
                    }
                    if (point.getDoubleGauge() != null) {
                        var gauge = format(point.getDoubleGauge());
                        formattedInfo.add(new JLabel("Gauge: " + gauge), createConstraint(row++, indent));
                    }
                    if (point.getHistogramCount() != null) {
                        var count = format(point.getHistogramCount());
                        formattedInfo.add(new JLabel("Histogram Count: " + count), createConstraint(row++, indent));
                    }
                    if (point.getHistogramSum() != null) {
                        var sum = format(point.getHistogramSum());
                        formattedInfo.add(new JLabel("Histogram Sum: " + sum), createConstraint(row++, indent));
                    }
                    if (point.getTags() != null) {
                        for (Map.Entry<String, String> entry : point.getTags().getPrimitiveValues().entrySet()) {
                            var label = createFilterLabel(entry.getKey(), entry.getValue());
                            formattedInfo.add(label, createConstraint(row++, indent));
                        }
                    }
                    if (i < taggedPoints.size() - 1) {
                        //Add blank line between points
                        formattedInfo.add(new JPanel(), createConstraint(row++, 0));
                    }
                }
            }
        }
        if (telemetry.getLog() != null) {
            var log = telemetry.getLog();
            formattedInfo.add(createTitleLabel(log.getType().toString()), createConstraint(row++, 0));
            if (log.getFormattedMessage() != null)
            {
                formattedInfo.add(createFilterLabel("Message", log.getFormattedMessage()), createConstraint(row++, indent));
            }
            if (log.getLogLevel() != null)
            {
                formattedInfo.add(createFilterLabel("Level", log.getLogLevel().toString()), createConstraint(row++, indent));
            }
            if (log.getCategoryName() != null)
            {
                formattedInfo.add(createFilterLabel("Category", log.getCategoryName()), createConstraint(row++, indent));
            }
            if (log.getEventId() != null)
            {
                formattedInfo.add(createFilterLabel("EventId.Id", Integer.toString(log.getEventId().getId())), createConstraint(row++, indent));
                if (log.getEventId().getName() != null)
                {
                    formattedInfo.add(createFilterLabel("EventId.Name", log.getEventId().getName()), createConstraint(row++, indent));
                }
            }
            if (log.getException() != null) {
                if (log.getException().getType() != null)
                {
                    formattedInfo.add(createFilterLabel("Exception Type", log.getException().getType()), createConstraint(row++, indent));
                }
                if (log.getException().getMessage() != null)
                {
                    formattedInfo.add(createFilterLabel("Exception Message", log.getException().getMessage()), createConstraint(row++, indent));
                }
            }
            if (log.getAttributes() != null)
            {
                formattedInfo.add(createTitleLabel("Attributes"), createConstraint(row++, 0));
                for (Map.Entry<String, String> entry : log.getAttributes().getPrimitiveValues().entrySet()) {
                    var value = entry.getValue();
                    if (value == null) {
                        value = "";
                    }
                    var label = createFilterLabel(entry.getKey(), value);
                    formattedInfo.add(label, createConstraint(row++, indent));
                }
            }
        }

        //Add trace information
        if (telemetry.getTraceIds() != null) {
            formattedInfo.add(createTitleLabel("Trace"), createConstraint(row++, 0));
            for (Map.Entry<String, String> entry : telemetry.getTraceIds().entrySet()) {
                var label = createFilterLabel(entry.getKey(), entry.getValue());
                formattedInfo.add(label, createConstraint(row++, indent));
            }
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

    @NotNull
    private JLabel createFilterLabel(@Nullable String label, @Nullable String value) {
        if (label == null) {
            label = "";
        }
        if (value == null) {
            value = "";
        }
        return createFilterLabelFinal(label, value);
    }

    @NotNull
    private JLabel createFilterLabelFinal(@Nullable String label, @Nullable String value) {
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
    private JLabel createTitleLabel(@Nullable String label) {
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

    /**
     * Escapes a string for HTML display
     */
    @NotNull
    private static String escapeHtml(@Nullable String s) {
        if (s == null) {
            return "";
        }
        return org.apache.commons.text.StringEscapeUtils.escapeHtml4(s);
    }

    /**
     * Formats a long value to a human-readable string (with K and M suffixes)
     */
    @NotNull
    private static String format(Integer value) {
        return format(value.longValue());
    }

    /**
     * Formats a long value to a human-readable string (with K and M suffixes)
     */
    @NotNull
    private static String format(Long value) {
        if (value < 1_000) {
            return value.toString();
        }
        var formatter = new DecimalFormat("#,###.0");
        if (value < 1_000_000) {
            return formatter.format(value / 1_000.0) + " K";
        }
        return formatter.format(value / 1_000_000.0) + " M";
    }

    /**
     * Formats a long value to a human-readable string (with K and M suffixes)
     */
    @NotNull
    private static String format(Double value) {
        if (value < 1_000.0) {
            return value.toString();
        }
        var formatter = new DecimalFormat("#,###.0");
        if (value < 1_000_000.0) {
            return formatter.format(value / 1_000.0) + " K";
        }
        return formatter.format(value / 1_000_000.0) + " M";
    }
}

