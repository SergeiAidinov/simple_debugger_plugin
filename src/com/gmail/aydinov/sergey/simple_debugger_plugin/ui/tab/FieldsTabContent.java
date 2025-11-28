package com.gmail.aydinov.sergey.simple_debugger_plugin.ui.tab;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;

import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.UserChangedFieldDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.VariableDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.processor.UiEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.processor.SimpleDebuggerEventQueue;

public class FieldsTabContent {

    private final Composite root;
    private final Table table;
    private final TableViewer viewer;
    private final List<VariableDTO> entries = new ArrayList<>();
    private final UiEventCollector uiEventCollector;

    public FieldsTabContent(Composite parent) {
        this.uiEventCollector = SimpleDebuggerEventQueue.instance();

        root = new Composite(parent, SWT.NONE);
        root.setLayout(new GridLayout(1, false));

        table = new Table(root, SWT.BORDER | SWT.FULL_SELECTION);
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        table.setLayoutData(new GridData(GridData.FILL_BOTH));

        viewer = new TableViewer(table);
        viewer.setContentProvider(ArrayContentProvider.getInstance());

        setupColumns();
        setupCellModifier();
    }

    private void setupColumns() {
        TableViewerColumn nameColumn = new TableViewerColumn(viewer, SWT.NONE);
        nameColumn.getColumn().setText("Field");
        nameColumn.getColumn().setWidth(200);
        nameColumn.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                if (element instanceof VariableDTO) {
                    return ((VariableDTO) element).getName();
                }
                return super.getText(element);
            }
        });

        TableViewerColumn typeColumn = new TableViewerColumn(viewer, SWT.NONE);
        typeColumn.getColumn().setText("Type");
        typeColumn.getColumn().setWidth(100);
        typeColumn.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                if (element instanceof VariableDTO) {
                    return ((VariableDTO) element).getType();
                }
                return super.getText(element);
            }
        });

        TableViewerColumn valueColumn = new TableViewerColumn(viewer, SWT.NONE);
        valueColumn.getColumn().setText("Value");
        valueColumn.getColumn().setWidth(200);
        valueColumn.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                if (element instanceof VariableDTO) {
                    return ((VariableDTO) element).getValue();
                }
                return super.getText(element);
            }
        });

        viewer.setColumnProperties(new String[]{"name", "type", "value"});
        viewer.setCellEditors(new CellEditor[]{null, null, new TextCellEditor(table)});
    }

    private void setupCellModifier() {
        viewer.setCellModifier(new ICellModifier() {
            @Override
            public boolean canModify(Object element, String property) {
                return "value".equals(property);
            }

            @Override
            public Object getValue(Object element, String property) {
                if (element instanceof VariableDTO) {
                    return ((VariableDTO) element).getValue();
                }
                return null;
            }

            @Override
            public void modify(Object element, String property, Object newValue) {
                if (!(element instanceof org.eclipse.swt.widgets.TableItem)) return;

                VariableDTO entry = (VariableDTO) ((org.eclipse.swt.widgets.TableItem) element).getData();
                String valueStr = newValue != null ? newValue.toString() : null;

                // Отправляем DTO в обработчик событий
                UserChangedFieldDTO dto = new UserChangedFieldDTO(entry.getName(), entry.getType(), valueStr);
                uiEventCollector.collectUiEvent(dto);

                // Обновляем таблицу
                viewer.update(entry, null);
            }
        });
    }

    public void updateFields(List<VariableDTO> vars) {
        if (table.isDisposed()) return;

        entries.clear();
        if (vars != null) {
            entries.addAll(vars);
        }

        viewer.setInput(entries);
        viewer.refresh();
    }

    public Composite getControl() {
        return root;
    }
}
