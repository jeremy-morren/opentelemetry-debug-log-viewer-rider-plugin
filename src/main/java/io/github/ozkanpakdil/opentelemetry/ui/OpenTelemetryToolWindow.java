package io.github.ozkanpakdil.opentelemetry.ui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intellij.codeInsight.folding.CodeFoldingManager;
import com.intellij.execution.filters.TextConsoleBuilder;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.json.JsonFileType;
import com.intellij.json.JsonLanguage;
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
import io.github.ozkanpakdil.opentelemetry.OpentelemetrySession;
import io.github.ozkanpakdil.opentelemetry.Telemetry;
import io.github.ozkanpakdil.opentelemetry.TelemetryType;
import io.github.ozkanpakdil.opentelemetry.metricdata.ExceptionData;
import io.github.ozkanpakdil.opentelemetry.metricdata.ITelemetryData;
import io.github.ozkanpakdil.opentelemetry.metricdata.MetricData;
import io.github.ozkanpakdil.opentelemetry.metricdata.RequestData;
import io.github.ozkanpakdil.opentelemetry.settings.AppSettingState;
import io.github.ozkanpakdil.opentelemetry.ui.components.AutoScrollToTheEndToolbarAction;
import io.github.ozkanpakdil.opentelemetry.ui.components.ClearApplicationInsightsLogToolbarAction;
import io.github.ozkanpakdil.opentelemetry.ui.components.ColorBox;
import io.github.ozkanpakdil.opentelemetry.ui.components.OptionsToolbarAction;
import io.github.ozkanpakdil.opentelemetry.ui.components.ToggleCaseInsensitiveSearchToolbarAction;
import io.github.ozkanpakdil.opentelemetry.ui.renderers.TelemetryDateRender;
import io.github.ozkanpakdil.opentelemetry.ui.renderers.TelemetryRender;
import io.github.ozkanpakdil.opentelemetry.ui.renderers.TelemetryTypeRender;
import io.github.ozkanpakdil.opentelemetry.utils.TimeSpan;
import kotlin.Unit;
import org.apache.velocity.runtime.directive.contrib.For;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class OpenTelemetryToolWindow {
    @NotNull
    private JPanel mainPanel;
    @NotNull
    private JBTable logsTable;
    @NotNull
    private JCheckBox metricCheckBox;
    @NotNull
    private JCheckBox exceptionCheckBox;
    @NotNull
    private JCheckBox messageCheckBox;
    @NotNull
    private JCheckBox dependencyCheckBox;
    @NotNull
    private JCheckBox requestCheckBox;
    @NotNull
    private JCheckBox eventCheckBox;
    @NotNull
    private JCheckBox pageViewCheckBox;
    @NotNull
    private JSplitPane splitPane;
    @NotNull
    private JLabel metricCounter;
    @NotNull
    private JLabel exceptionCounter;
    @NotNull
    private JLabel messageCounter;
    @NotNull
    private JLabel dependencyCounter;
    @NotNull
    private JLabel requestCounter;
    @NotNull
    private JLabel eventCounter;
    @NotNull
    private JLabel pageViewCounter;
    @NotNull
    private ColorBox metricColorBox;
    @NotNull
    private ColorBox exceptionColorBox;
    @NotNull
    private ColorBox messageColorBox;
    @NotNull
    private ColorBox dependencyColorBox;
    @NotNull
    private ColorBox requestColorBox;
    @NotNull
    private ColorBox eventColorBox;
    @NotNull
    private ColorBox pageViewColorBox;
    @NotNull
    private ExtendableTextField filter;
    @NotNull
    private JScrollPane logsScrollPane;
    @NotNull
    private ActionToolbarImpl toolbar;
    @NotNull
    private JComponent editorPanel;
    private JPanel formattedTelemetryInfo;

    @NotNull
    private final Project project;
    @NotNull
    private final TelemetryRender telemetryRender;
    @NotNull
    private final Map<TelemetryType, Integer> telemetryCountPerType = new HashMap<>();
    @NotNull
    private final OpentelemetrySession opentelemetrySession;
    private final Lifetime lifetime;

    @NotNull
    private Editor editor;
    @NotNull
    private final TelemetryTableModel telemetryTableModel;
    @NotNull
    private final ArrayList<JLabel> telemetryTypesCounter = new ArrayList<>();
    @NotNull
    private Document jsonPreviewDocument;
    private boolean autoScrollToTheEnd;
    private final TextConsoleBuilder builder;
    Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public OpenTelemetryToolWindow(
            @NotNull OpentelemetrySession opentelemetrySession,
            @NotNull Project project,
            Lifetime lifetime) {
        this.project = project;
        this.opentelemetrySession = opentelemetrySession;
        this.lifetime = lifetime;

        initTelemetryTypeFilters();

        splitPane.setDividerLocation(0.5);
        splitPane.setResizeWeight(0.5);
        try {
            ReadAction.nonBlocking(() -> {
                return CodeFoldingManager.getInstance(ProjectManager.getInstance().getDefaultProject())
                        .buildInitialFoldings(jsonPreviewDocument);
            }).submit(AppExecutorUtil.getAppExecutorService()).get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }

        this.telemetryRender = new TelemetryRender(lifetime);
        logsTable.setDefaultRenderer(Telemetry.class, telemetryRender);
        logsTable.setDefaultRenderer(TelemetryType.class, new TelemetryTypeRender());
        logsTable.setDefaultRenderer(Date.class, new TelemetryDateRender());
        logsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        telemetryTableModel = new TelemetryTableModel();
        logsTable.setModel(telemetryTableModel);
        logsTable.getColumnModel().getColumn(0).setPreferredWidth(90);
        logsTable.getColumnModel().getColumn(0).setMaxWidth(130);
        logsTable.getColumnModel().getColumn(1).setPreferredWidth(100);
        logsTable.getColumnModel().getColumn(1).setMaxWidth(100);
        logsTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        logsTable.getColumnModel().getColumn(2).setMaxWidth(100);
        logsTable.getTableHeader().setUI(null);

        filter.setExtensions(new ClearTextFieldExtension(filter));

        filter.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
            }

            @Override
            public void keyPressed(KeyEvent e) {
                OpenTelemetryToolWindow.this.opentelemetrySession.updateFilter(filter.getText());
            }

            @Override
            public void keyReleased(KeyEvent e) {
                OpenTelemetryToolWindow.this.opentelemetrySession.updateFilter(filter.getText());
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
        updateJsonPreview(gson.toJson(telemetry == null ? "" : telemetry.getData()));

        if (telemetry == null) {
            return;
        }

        formattedTelemetryInfo.setVisible(false);
//        formattedTelemetryInfo.removeAll();
        if (telemetry.getFilteredBy() != null) {
            formattedTelemetryInfo.add(new JLabel("This log was filtered by " + telemetry.getFilteredBy()),
                    createConstraint(0, 0, 0));
        }

        int column = 1;
        /*if (telemetry.getType() == TelemetryType.Message) {
            formattedTelemetryInfo.add(createTitleLabel("Message"), createConstraint(0, column++, 0));
            MessageData messageData = telemetry.getData(MessageData.class);
            formattedTelemetryInfo.add(new JLabel(messageData.message), createConstraint(0, column++, 30));
        }*/
        if (telemetry.getType() == TelemetryType.Request) {
            formattedTelemetryInfo.add(createTitleLabel("Request"), createConstraint(0, column++, 0));
            RequestData requestData = telemetry.getData(RequestData.class);
            formattedTelemetryInfo.add(new JLabel(requestData.name), createConstraint(0, column++, 30));
            formattedTelemetryInfo.add(new JLabel("Status code: " + requestData.responseCode),
                    createConstraint(0, column++, 30));
            formattedTelemetryInfo.add(new JLabel("Duration: " + new TimeSpan(requestData.duration).toString()),
                    createConstraint(0, column++, 30));
        }
        if (telemetry.getType() == TelemetryType.Duration) {
            formattedTelemetryInfo.add(createTitleLabel("Metric"), createConstraint(0, column++, 0));
            MetricData metricData = telemetry.getData(MetricData.class);
            if (metricData.metrics != null) {
                for (MetricData.Metric metric : metricData.metrics) {
                    formattedTelemetryInfo.add(new JLabel(metric.name), createConstraint(0, column++, 30));
                    formattedTelemetryInfo.add(new JLabel("Kind: " + metric.kind), createConstraint(0, column++, 60));
                    formattedTelemetryInfo.add(new JLabel("Value: " + metric.value), createConstraint(0, column++, 60));
                    formattedTelemetryInfo.add(new JLabel("Count: " + metric.count), createConstraint(0, column++, 60));
                }
            }
        }
        if (telemetry.getType() == TelemetryType.Exception) {
            formattedTelemetryInfo.add(createTitleLabel("Exception"), createConstraint(0, column++, 0));
            ExceptionData exceptionData = telemetry.getData(ExceptionData.class);
            var consoleView = builder.getConsole();
            consoleView.clear();
            consoleView.allowHeavyFilters();
            for (ExceptionData.ExceptionDetailData exception : exceptionData.exceptions) {
                consoleView.print(StackTraceFormatter.formatStackTrace(exception), ConsoleViewContentType.NORMAL_OUTPUT);
                formattedTelemetryInfo.add(consoleView.getComponent(), createConstraint(0, column++, 30));
            }
        }

        /*ITelemetryData telemetryData = telemetry.getData(ITelemetryData.class);
        if (telemetryData != null && telemetryData.getProperties() != null && !telemetryData.getProperties().isEmpty()) {
            formattedTelemetryInfo.add(createTitleLabel("Properties"), createConstraint(0, column++, 0));
            for (Map.Entry<String, String> entry : telemetryData.getProperties().entrySet()) {
                JLabel jLabel = new JLabel("<html>" + entry.getKey() + ": " + "<a href=''>" + entry.getValue() + "</a></html>");
                jLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
                jLabel.addMouseListener(new ClickListener(e -> {
                    opentelemetrySession.updateFilter(entry.getValue());
                    this.filter.setText(entry.getValue());
                }));
                formattedTelemetryInfo.add(jLabel, createConstraint(0, column++, 30));
            }
        }*/

        // Padding
        {
            GridBagConstraints c = createConstraint(0, 10_000, 0);
            c.weighty = 1;
            formattedTelemetryInfo.add(new JPanel(), c);
        }

        formattedTelemetryInfo.revalidate();
        formattedTelemetryInfo.repaint();
    }

    @NotNull
    private JLabel createTitleLabel(String label) {
        JLabel title = new JLabel("<html><b>" + label + "</b></html>");
        Font font = title.getFont();
        font.deriveFont(Font.BOLD);
        title.setFont(font);
        return title;
    }

    @NotNull
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

    private void updateJsonPreview(String text) {
        StringBuilder sb = new StringBuilder();
        // A bit hackish, replace leading space with tabs for better formatting. `gson` is not giving any option on pretty print (maybe need to try another one ?)
        for (String line : text.split("\n")) {
            int i;
            for (i = 0; i < line.length() && line.charAt(i) == ' '; i++)
                ;
            if (i > 0) {
                for (int j = 0; j < i / 2; j++)
                    sb.append('\t');
                sb.append(line.substring(i));
            } else {
                sb.append(line);
            }
            sb.append('\n');
        }
        ApplicationManager.getApplication().runWriteAction(() -> {
            jsonPreviewDocument.setText(sb.toString());
        });
        CodeFoldingManager.getInstance(ProjectManager.getInstance().getDefaultProject()).updateFoldRegions(editor);
    }

    private void initTelemetryTypeFilters() {
        metricCounter.putClientProperty("TelemetryType", TelemetryType.Duration);
        exceptionCounter.putClientProperty("TelemetryType", TelemetryType.Exception);
        messageCounter.putClientProperty("TelemetryType", TelemetryType.Message);
        requestCounter.putClientProperty("TelemetryType", TelemetryType.Request);

        telemetryTypesCounter.addAll(
                Arrays.asList(metricCounter, exceptionCounter, messageCounter, dependencyCounter, requestCounter, eventCounter,
                        pageViewCounter));

        metricCheckBox.putClientProperty("TelemetryType", TelemetryType.Duration);
        exceptionCheckBox.putClientProperty("TelemetryType", TelemetryType.Exception);
        messageCheckBox.putClientProperty("TelemetryType", TelemetryType.Message);
        requestCheckBox.putClientProperty("TelemetryType", TelemetryType.Request);

        for (JCheckBox checkBox : new JCheckBox[] { metricCheckBox, exceptionCheckBox, messageCheckBox, dependencyCheckBox,
                requestCheckBox, eventCheckBox, pageViewCheckBox }) {
            TelemetryType telemetryType = (TelemetryType) checkBox.getClientProperty("TelemetryType");
            if (telemetryType != null) {
                checkBox.setSelected(opentelemetrySession.isTelemetryVisible(telemetryType));
                checkBox.addItemListener(e -> {
                    opentelemetrySession.setTelemetryFiltered(telemetryType, e.getStateChange() != ItemEvent.SELECTED);
                });
            }
        }
    }

    @NotNull
    public JPanel getContent() {
        return mainPanel;
    }

    public void setTelemetries(
            @NotNull List<Telemetry> telemetries,
            @NotNull List<Telemetry> visibleTelemetries
    ) {
        telemetryCountPerType.clear();
        for (Telemetry telemetry : telemetries) {
            telemetryCountPerType.compute(telemetry.getType(), (telemetryType, count) -> count == null ? 1 : count + 1);
        }
        updateTelemetryTypeCounter(null);
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
        telemetryCountPerType.compute(telemetry.getType(), (telemetryType, count) -> count == null ? 1 : count + 1);
        updateTelemetryTypeCounter(telemetry.getType());
    }

    private void performAutoScrollToTheEnd() {
        logsTable.scrollRectToVisible(
                logsTable.getCellRect(telemetryTableModel.getRowCount() - 1, 0, true));
    }

    private void updateTelemetryTypeCounter(@Nullable TelemetryType type) {
        for (JLabel counter : telemetryTypesCounter) {
            TelemetryType telemetryType = (TelemetryType) counter.getClientProperty("TelemetryType");
            if (type != null && telemetryType != type)
                continue;

            int count = telemetryCountPerType.computeIfAbsent(telemetryType, (e) -> 0);
            if (count < 1000) {
                counter.setText(String.valueOf(count));
            } else if (count < 1000000) {
                counter.setText(count / 1000 + "K");
            } else {
                counter.setText(count / 1000 + "M");
            }
        }
    }

    private void createUIComponents() {
        metricColorBox = new ColorBox(JBColor.namedColor("TelemetryColor.Metric", JBColor.gray));
        exceptionColorBox = new ColorBox(JBColor.namedColor("TelemetryColor.Exception", JBColor.red));
        messageColorBox = new ColorBox(JBColor.namedColor("TelemetryColor.Message", JBColor.orange));
        dependencyColorBox = new ColorBox(
                JBColor.namedColor("TelemetryColor.RemoteDependency", JBColor.blue));
        requestColorBox = new ColorBox(JBColor.namedColor("TelemetryColor.Request", JBColor.green));
        eventColorBox = new ColorBox(JBColor.namedColor("TelemetryColor.CustomEvents", JBColor.cyan));
        pageViewColorBox = new ColorBox(JBColor.namedColor("TelemetryColor.PageView", JBColor.yellow));

        toolbar = createToolbar();
        toolbar.setTargetComponent(mainPanel);
        toolbar.setVisible(false);

        jsonPreviewDocument = new LanguageTextField.SimpleDocumentCreator().createDocument("", JsonLanguage.INSTANCE, project);
        editor = EditorFactory.getInstance().createViewer(jsonPreviewDocument, project, EditorKind.MAIN_EDITOR);
        if (editor instanceof EditorEx) {
            ((EditorEx) editor).setHighlighter(
                    EditorHighlighterFactory.getInstance().createEditorHighlighter(project, JsonFileType.INSTANCE));
            ((EditorEx) editor).getFoldingModel().setFoldingEnabled(true);
        }
        editor.getSettings().setIndentGuidesShown(true);
        editor.getSettings().setAdditionalLinesCount(3);
        editor.getSettings().setFoldingOutlineShown(true);
        editor.getSettings().setUseSoftWraps(
                PropertiesComponent.getInstance().getBoolean("io.github.ozkanpakdil.opentelemetry.useSoftWrap"));

        editorPanel = editor.getComponent();
    }

    @NotNull
    private ActionToolbarImpl createToolbar() {
        final DefaultActionGroup actionGroup = new DefaultActionGroup();

        actionGroup.add(new OptionsToolbarAction(() -> toolbar));
        actionGroup.addSeparator();

        autoScrollToTheEnd = PropertiesComponent.getInstance()
                .getBoolean("io.github.ozkanpakdil.opentelemetry.autoScrollToTheEnd");

        actionGroup.add(new AutoScrollToTheEndToolbarAction(this::acceptScrollRoEnd, autoScrollToTheEnd));

        actionGroup.add(new ToggleCaseInsensitiveSearchToolbarAction());

        actionGroup.add(new AbstractToggleUseSoftWrapsAction(SoftWrapAppliancePlaces.PREVIEW, false) {
            {
                ActionUtil.copyFrom(this, "EditorToggleUseSoftWraps");
            }

            @Override
            public void setSelected(@NotNull AnActionEvent e, boolean state) {
                super.setSelected(e, state);
                PropertiesComponent.getInstance().setValue("io.github.ozkanpakdil.opentelemetry.useSoftWrap", state);
            }

            @NotNull
            @Override
            protected Editor getEditor(@NotNull AnActionEvent e) {
                return editor;
            }
        });

        actionGroup.add(new ClearApplicationInsightsLogToolbarAction() {
            @Override
            public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
                opentelemetrySession.clear();
            }
        });

        return new ActionToolbarImpl("Opentelemetry", actionGroup, false);
    }

    private void acceptScrollRoEnd(Boolean selected) {
        autoScrollToTheEnd = selected;
        PropertiesComponent.getInstance().setValue("io.github.ozkanpakdil.opentelemetry.autoScrollToTheEnd", selected);
        if (autoScrollToTheEnd) {
            performAutoScrollToTheEnd();
        }
    }
}

