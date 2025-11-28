package com.gmail.aydinov.sergey.simple_debugger_plugin.ui.tab;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;

import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.UserChangedVariableDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.VariableDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.processor.UiEventCollector;

public class VariablesTabContent {

    private final Composite root;
    private final Table table;
    private final TableViewer viewer;
    private final List<VariableDTO> entries = new ArrayList<>();
    private final UiEventCollector uiEventCollector;

    public VariablesTabContent(Composite parent, UiEventCollector uiEventCollector) {
        this.uiEventCollector = uiEventCollector;

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
        nameColumn.getColumn().setText("Name");
        nameColumn.getColumn().setWidth(150);
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
                // Создаём DTO для передачи в обработчик
                UserChangedVariableDTO dto = new UserChangedVariableDTO(
                        entry.getName(),
                        entry.getType(),
                        newValue
                );

                uiEventCollector.collectUiEvent(dto);

                // Обновляем значение в списке и viewer
                int index = entries.indexOf(entry);
                if (index >= 0) {
                    entries.set(index, new VariableDTO(entry.getName(), entry.getType(), (String) newValue));
                    viewer.update(entries.get(index), null);
                }
            }
        });
    }

    public Composite getControl() {
        return root;
    }

    /**
     * Обновление списка переменных
     */
    public void updateVariables(List<VariableDTO> vars) {
        if (table.isDisposed()) return;

        entries.clear();
        entries.addAll(vars);

        viewer.setInput(entries);
        viewer.refresh();
    }
}
