package com.gmail.aydinov.sergey.simple_debugger_plugin.ui.tab;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.AbstractTargetAplicationElement.TargetApplicationElementType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.DebugWindowDataDTO;

/**
 * Tab content that displays fields or local variables
 * of a selected class or stack frame.
 */
public class FieldsAndVariablesTabContent {

    private final Composite root;
    private final TableViewer viewer;

    public FieldsAndVariablesTabContent(Composite parent) {
        root = new Composite(parent, SWT.NONE);
        root.setLayout(new GridLayout(1, false));

        Table table = new Table(root, SWT.BORDER | SWT.FULL_SELECTION);
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        table.setLayoutData(new GridData(GridData.FILL_BOTH));

        viewer = new TableViewer(table);
        viewer.setContentProvider(ArrayContentProvider.getInstance());

        setupColumns();
    }

    private void setupColumns() {
        createColumn("Name", 200, DebugWindowDataDTO::getElementName);
        createColumn("Type", 200, dto -> {
            TargetApplicationElementType type = dto.getElementType();
            return type != null ? type.name() : "";
        });
        createColumn("Value", 300,dto -> {
            String value = dto.getValue();
            return value != null ? value : "";
        });
    }

    private void createColumn(
            String title,
            int width,
            Function<DebugWindowDataDTO, String> extractor) {

        TableViewerColumn column = new TableViewerColumn(viewer, SWT.NONE);
        column.getColumn().setText(title);
        column.getColumn().setWidth(width);
        column.getColumn().setResizable(true);

        column.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                if (element instanceof DebugWindowDataDTO dto) {
                    String value = extractor.apply(dto);
                    return value != null ? value : "";
                }
                return "";
            }
        });
    }

    /**
     * Displays inner elements (fields / variables) of the given DTO.
     */
    public void showInnerElements(DebugWindowDataDTO parentDto) {
        if (parentDto == null)
            return;

        List<DebugWindowDataDTO> innerElements = parentDto.getInnerElements().stream().toList();
        if (innerElements == null)
            return;

        root.getDisplay().asyncExec(() -> {
            if (viewer.getTable().isDisposed())
                return;

            viewer.setInput(innerElements);
        });
    }

    public Composite getControl() {
        return root;
    }
}