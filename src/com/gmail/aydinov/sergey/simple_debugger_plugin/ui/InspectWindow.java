package com.gmail.aydinov.sergey.simple_debugger_plugin.ui;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.FieldOrVariableDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.tab.FieldsAndVariablesTabContent;

/**
 * Separate window for inspecting objects and collections.
 */
public class InspectWindow {

    private Shell shell;
    private Composite breadcrumbComposite;
    private Composite contentComposite;
    private Deque<FieldOrVariableDTO> history = new ArrayDeque<>();

    public InspectWindow() {
        shell = new Shell(Display.getDefault());
        shell.setText("Inspect Object");
        shell.setSize(600, 400);
        shell.setLayout(new GridLayout(1, false));

        // Breadcrumb
        breadcrumbComposite = new Composite(shell, SWT.NONE);
        breadcrumbComposite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        breadcrumbComposite.setLayout(new GridLayout(10, false)); // горизонтальные кнопки

        // Content panel
        contentComposite = new Composite(shell, SWT.NONE);
        contentComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        contentComposite.setLayout(new GridLayout(1, false));
    }

    /** Opens the shell */
    public void open() {
        shell.open();
    }

    /** Returns true if shell is open */
    public boolean isOpen() {
        return !shell.isDisposed();
    }

    /**
     * Shows an inspectable object in the window.
     * Maintains history for breadcrumb/back navigation.
     */
    public void showInspectableNode(FieldOrVariableDTO dto) {
        if (dto == null || shell.isDisposed()) return;

        // Добавляем в историю
        history.push(dto);

        // Перерисовываем breadcrumb
        renderBreadcrumb();

        // Перерисовываем содержимое
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
            refreshContent(dto);
            renderBreadcrumb();
        }
    }

    /** Updates content panel for a DTO */
    private void refreshContent(FieldOrVariableDTO dto) {
        for (Control child : contentComposite.getChildren()) {
            child.dispose();
        }

        FieldsAndVariablesTabContent tabContent = new FieldsAndVariablesTabContent(contentComposite);
        tabContent.updateVariablesAndFields(List.of(dto), null);

        contentComposite.layout();
    }
}