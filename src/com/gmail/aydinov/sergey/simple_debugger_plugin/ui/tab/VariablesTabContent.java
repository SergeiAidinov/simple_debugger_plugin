package com.gmail.aydinov.sergey.simple_debugger_plugin.ui.tab;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.CellEditor;

import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.VariableDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.UserChangedVariableEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.processor.UiEventCollector;

/**
 * Tab content showing all variables in the debugger.
 * Users can edit variable values directly in the table.
 */
public class VariablesTabContent {

    /** Root composite for this tab */
    private final Composite root;

    /** Table displaying variables */
    private final Table table;

    /** Table viewer managing the table content */
    private final TableViewer viewer;

    /** Local list of variable DTOs */
    private final List<VariableDTO> entries = new ArrayList<>();

    /** UI event collector for sending changes */
    private final UiEventCollector uiEventCollector;

    /**
     * Constructs the VariablesTabContent.
     *
     * @param parent parent composite
     * @param uiEventCollector collector for UI events
     */
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

    /** Sets up the table columns: Name, Type, Value */
    private void setupColumns() {
        // Name column
        TableViewerColumn nameColumn = new TableViewerColumn(viewer, SWT.NONE);
        nameColumn.getColumn().setText("Name");
        nameColumn.getColumn().setWidth(150);
        nameColumn.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                if (element instanceof VariableDTO dto) return dto.getName();
                return super.getText(element);
            }
        });

        // Type column
        TableViewerColumn typeColumn = new TableViewerColumn(viewer, SWT.NONE);
        typeColumn.getColumn().setText("Type");
        typeColumn.getColumn().setWidth(100);
        typeColumn.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                if (element instanceof VariableDTO dto) return dto.getType();
                return super.getText(element);
            }
        });

        // Value column (editable)
        TableViewerColumn valueColumn = new TableViewerColumn(viewer, SWT.NONE);
        valueColumn.getColumn().setText("Value");
        valueColumn.getColumn().setWidth(200);
        valueColumn.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                if (element instanceof VariableDTO dto) return dto.getValue();
                return super.getText(element);
            }
        });

        viewer.setColumnProperties(new String[]{"name", "type", "value"});
        viewer.setCellEditors(new CellEditor[]{null, null, new TextCellEditor(table)});
    }

    /** Sets up cell modifier for editing variable values */
    private void setupCellModifier() {
        viewer.setCellModifier(new ICellModifier() {

            @Override
            public boolean canModify(Object element, String property) {
                return "value".equals(property);
            }

            @Override
            public Object getValue(Object element, String property) {
                if (element instanceof VariableDTO dto) return dto.getValue();
                return null;
            }

            @Override
            public void modify(Object element, String property, Object newValue) {
                if (!(element instanceof TableItem item)) return;

                VariableDTO oldEntry = (VariableDTO) item.getData();
                if (Objects.isNull(oldEntry) || Objects.isNull(newValue)) return;

                String newValStr = newValue.toString();

                // Send DTO to event collector
                UserChangedVariableEvent dto = new UserChangedVariableEvent(
                        oldEntry.getName(),
                        oldEntry.getType(),
                        newValStr
                );
                uiEventCollector.collectUiEvent(dto);

                // Update local list and viewer
                int index = entries.indexOf(oldEntry);
                if (index >= 0) {
                    VariableDTO updated = new VariableDTO(oldEntry.getName(), oldEntry.getType(), newValStr);
                    entries.set(index, updated);
                    viewer.update(updated, null);
                }
            }
        });
    }

    /**
     * Updates the variable table with the given list of variables.
     *
     * @param vars list of VariableDTOs to display
     */
    public void updateVariables(List<VariableDTO> vars) {
        if (table.isDisposed() || Objects.isNull(vars)) return;

        entries.clear();
        entries.addAll(vars);

        viewer.setInput(entries);
        viewer.refresh();
    }

    /**
     * Returns the root composite for this tab.
     *
     * @return root composite
     */
    public Composite getControl() {
        return root;
    }
}
