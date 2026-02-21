package com.gmail.aydinov.sergey.simple_debugger_plugin.ui;

import java.util.ArrayDeque;
import java.util.Deque;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.FieldOrVariableDTO;

/**
 * Inspect window showing a single table with two columns (type/key + value)
 * and a top panel for object name + breadcrumb history.
 */
public class InspectWindow {

    private Shell shell;
    private Composite topPanel;
    private Label objectLabel;
    private Composite breadcrumbComposite;
    private Table table;
    private Deque<FieldOrVariableDTO> history = new ArrayDeque<>();

    public InspectWindow() {
        // Shell
        shell = new Shell(Display.getDefault());
        shell.setText("Inspect Object");
        shell.setSize(1400, 800); // альбомный формат
        shell.setLayout(new GridLayout(1, false));

        // ----------------- Top panel -----------------
        topPanel = new Composite(shell, SWT.NONE);
        topPanel.setLayout(new GridLayout(2, false));
        topPanel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        objectLabel = new Label(topPanel, SWT.NONE);
        objectLabel.setText("Object: ");
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

    /** Opens the shell */
    public void open() {
        shell.open();
    }

    /** Returns true if shell is open */
    public boolean isOpen() {
        return !shell.isDisposed();
    }

    /** Closes the window and clears history */
    public void close() {
        if (isOpen()) {
            // Выполнить закрытие синхронно в UI-потоке
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

        objectLabel.setText("Object: " + dto.getName());

        // Добавляем в историю
        history.push(dto);

        renderBreadcrumb();
        refreshContent(dto);
    }

    /** Renders breadcrumb buttons */
    private void renderBreadcrumb() {
        for (Control child : breadcrumbComposite.getChildren()) {
            child.dispose();
        }

        FieldOrVariableDTO[] items = history.toArray(new FieldOrVariableDTO[0]);
        for (int i = items.length - 1; i >= 0; i--) {
            FieldOrVariableDTO dto = items[i];
            Button btn = new Button(breadcrumbComposite, SWT.PUSH);
            btn.setText(dto.getName());
            final int index = i;
            btn.addListener(SWT.Selection, e -> navigateTo(index));
        }

        breadcrumbComposite.layout();
    }

    /** Navigate to a previous object in the breadcrumb */
    private void navigateTo(int indexFromTop) {
        if (indexFromTop < 0 || indexFromTop >= history.size()) return;

        while (history.size() > indexFromTop + 1) {
            history.pop();
        }

        FieldOrVariableDTO dto = history.peek();
        if (dto != null) {
            objectLabel.setText("Object: " + dto.getName());
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