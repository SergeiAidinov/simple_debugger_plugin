package com.gmail.aydinov.sergey.simple_debugger_plugin.ui;

import java.util.ArrayDeque;
import java.util.Deque;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.FieldOrVariableDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.UserEndedInspectionSessionForElement;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.UserStartedInspectionSessionForElement;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event_collectors.SimpleDebuggerEventQueue;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event_collectors.UiEventCollector;

/**
 * Inspect window showing a single table with two columns (type/key + value)
 * and a top panel for object name + breadcrumb history.
 * No tabs or "petals" are used.
 */
public class InspectWindow {

    private final Shell shell;
    private final Label objectLabel;
    private final Composite breadcrumbComposite;
    private final Table table;
    private Button backButton;
    private Button forwardButton;
    private final Deque<FieldOrVariableDTO> history = new ArrayDeque<>();
    private final UiEventCollector uiEventCollector = SimpleDebuggerEventQueue.instance();

    public InspectWindow() {
        shell = new Shell(Display.getDefault());
        shell.setText("Inspect Object");
        shell.setSize(1400, 800); // альбомный формат
        shell.setLayout(new GridLayout(1, false));
        shell.addListener(SWT.Close, e -> {
            System.out.println("InspectWindow: user clicked X, closing session");
            uiEventCollector.collectUiEvent(new UserEndedInspectionSessionForElement());
        });

        // ----------------- Top panel -----------------
        Composite topPanel = new Composite(shell, SWT.NONE);
        topPanel.setLayout(new GridLayout(2, false));
        topPanel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        
        backButton = new Button(topPanel, SWT.PUSH);
        backButton.setText("◀ Back");
        backButton.setEnabled(false); // изначально нельзя
        backButton.addListener(SWT.Selection, e -> navigateBack());

        forwardButton = new Button(topPanel, SWT.PUSH);
        forwardButton.setText("Forward ▶");
        forwardButton.setEnabled(false);
        forwardButton.addListener(SWT.Selection, e -> navigateForward());

        objectLabel = new Label(topPanel, SWT.NONE);
        objectLabel.setText("Inspecting instance: ... " );
        objectLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        breadcrumbComposite = new Composite(topPanel, SWT.NONE);
        breadcrumbComposite.setLayout(new GridLayout(10, false));
        breadcrumbComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        // ----------------- Table -----------------
        table = new Table(shell, SWT.BORDER | SWT.FULL_SELECTION);
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        TableColumn leftCol = new TableColumn(table, SWT.NONE);
        leftCol.setText("Type / Key");
        leftCol.setWidth(400);

        TableColumn rightCol = new TableColumn(table, SWT.NONE);
        rightCol.setText("Value");
        rightCol.setWidth(800);
    }

    private void navigateBack() {
        if (history.size() <= 1) return;

        FieldOrVariableDTO current = history.pop();
        history.push(current);

        FieldOrVariableDTO previous = history.peek();
        if (previous != null) {
            objectLabel.setText("Object: " + previous.getName());
            refreshContent(previous);
            renderBreadcrumb();
        }

        updateNavigationButtons();
    }
    
    private void updateNavigationButtons() {
        backButton.setEnabled(history.size() > 1);
        forwardButton.setEnabled(!history.isEmpty());
    }

    private void navigateForward() {
        if (history.isEmpty()) return;

        FieldOrVariableDTO next = history.pop();
        history.push(next);

        objectLabel.setText("Inspecting instance: " + next.getName() + " (" + next.getType() + ")");
        refreshContent(next);
        renderBreadcrumb();

        updateNavigationButtons();
    }

	/** Opens the shell */
    public void open() {
    	shell.setImage(DebugWindowsManager.instance().icons.get("debugger"));
        shell.open();
    }

    /** Returns true if shell is open */
    public boolean isOpen() {
        return !shell.isDisposed();
    }

    /** Closes the window */
    public void close() {
        if (isOpen()) {
            Display.getDefault().syncExec(() -> {
                if (!shell.isDisposed()) {
                    shell.close();
                }
            });
        }
    }

    /** Shows an inspectable object */
    public void showInspectableNode(FieldOrVariableDTO dto) {
        if (dto == null || shell.isDisposed()) return;

        objectLabel.setText("Inspecting instance: " + dto.getName() + " (" + dto.getType() + ")");
        history.push(dto);

        renderBreadcrumb();
        refreshContent(dto);
        uiEventCollector.collectUiEvent(new UserStartedInspectionSessionForElement(dto));
        System.out.println("===> Inspecting instance: " + dto.getName() + " (" + dto.getType() + ")");
    }

    /** Renders breadcrumb buttons */
    private void renderBreadcrumb() {
       
    }

    /** Navigate to a previous object in the breadcrumb */
    private void navigateTo(int indexFromTop) {
        if (indexFromTop < 0 || indexFromTop >= history.size()) return;

        while (history.size() > indexFromTop + 1) {
            history.pop();
        }

        FieldOrVariableDTO dto = history.peek();
        if (dto != null) {
            objectLabel.setText("Inspecting instance: " + dto.getName() + " (" + dto.getType() + ")");
            refreshContent(dto);
            renderBreadcrumb();
        }
    }

    /** Updates the table content for a DTO */
    private void refreshContent(FieldOrVariableDTO dto) {
        table.removeAll();
        if (dto == null) return;

        TableItem item = new TableItem(table, SWT.NONE);
        item.setText(new String[]{
            dto.getType() != null ? dto.getType() : "",
            dto.getValue() != null ? dto.getValue() : ""
        });

        for (TableColumn col : table.getColumns()) {
            col.pack();
        }

        table.layout();
    }
}