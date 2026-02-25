package com.gmail.aydinov.sergey.simple_debugger_plugin.ui.tab;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;

import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.DebugWindowDataDTO;

/**
 * Simplified tab: displays only the name of a loaded class.
 */
public class FieldsAndVariablesTabContent {

    private final Composite root;
    private final Table table;
    private final TableViewer viewer;
    private final List<DebugWindowDataDTO> entries = new ArrayList<>();

    public FieldsAndVariablesTabContent(Composite parent) {
        root = new Composite(parent, SWT.NONE);
        root.setLayout(new GridLayout(1, false));

        table = new Table(root, SWT.BORDER | SWT.FULL_SELECTION);
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        table.setLayoutData(new GridData(GridData.FILL_BOTH));

        viewer = new TableViewer(table);
        viewer.setContentProvider(ArrayContentProvider.getInstance());

        setupColumns();
    }

    private void setupColumns() {
        // Name column
        TableViewerColumn nameColumn = new TableViewerColumn(viewer, SWT.NONE);
        nameColumn.getColumn().setText("Class Name");
        nameColumn.getColumn().setWidth(300);
        nameColumn.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                if (element instanceof DebugWindowDataDTO dto)
                    return dto.getElementName();
                return "";
            }
        });
    }

    /**
     * Updates the table to show a single class DTO.
     */
    public void updateClass(DebugWindowDataDTO classDto) {
        if (table.isDisposed() || classDto == null)
            return;

        entries.clear();
        entries.add(classDto);

        viewer.setInput(entries);
        viewer.refresh();
    }

    public Composite getControl() {
        return root;
    }
}