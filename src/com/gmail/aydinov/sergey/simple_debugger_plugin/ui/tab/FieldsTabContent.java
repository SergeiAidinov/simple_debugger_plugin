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
import org.eclipse.swt.widgets.Text;

import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.UserChangedFieldDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.VariableDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.processor.UiEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.processor.SimpleDebuggerEventQueue;

/**
 * Manages the "Fields" tab in the debugger UI, showing all fields of the current context.
 * Allows editing field values and sending changes to the debugger backend.
 */
public class FieldsTabContent {

    /** Root composite for this tab */
    private final Composite root;

    /** Table widget displaying the fields */
    private final Table table;

    /** TableViewer for structured display and editing */
    private final TableViewer viewer;

    /** Cached list of VariableDTO entries displayed in the table */
    private final List<VariableDTO> entries = new ArrayList<>();

    /** Collector to send user UI events to the debugger */
    private final UiEventCollector uiEventCollector;

    /**
     * Constructs a FieldsTabContent attached to a parent composite.
     *
     * @param parent the parent composite
     */
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

    /** Sets up the table columns: Field, Type, Value */
    private void setupColumns() {
        TableViewerColumn nameColumn = new TableViewerColumn(viewer, SWT.NONE);
        nameColumn.getColumn().setText("Field");
        nameColumn.getColumn().setWidth(200);
        nameColumn.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                if (element instanceof VariableDTO dto) {
                    return Objects.toString(dto.getName(), "");
                }
                return "";
            }
        });

        TableViewerColumn typeColumn = new TableViewerColumn(viewer, SWT.NONE);
        typeColumn.getColumn().setText("Type");
        typeColumn.getColumn().setWidth(100);
        typeColumn.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                if (element instanceof VariableDTO dto) {
                    return Objects.toString(dto.getType(), "");
                }
                return "";
            }
        });

        TableViewerColumn valueColumn = new TableViewerColumn(viewer, SWT.NONE);
        valueColumn.getColumn().setText("Value");
        valueColumn.getColumn().setWidth(200);
        valueColumn.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                if (element instanceof VariableDTO dto) {
                    return Objects.toString(dto.getValue(), "");
                }
                return "";
            }
        });

        viewer.setColumnProperties(new String[]{"name", "type", "value"});
        viewer.setCellEditors(new CellEditor[]{null, null, new TextCellEditor(table)});
    }

    /** Sets up cell editing for the "Value" column */
    private void setupCellModifier() {
        viewer.setCellModifier(new ICellModifier() {
            @Override
            public boolean canModify(Object element, String property) {
                return "value".equals(property);
            }

            @Override
            public Object getValue(Object element, String property) {
                if (element instanceof VariableDTO dto) {
                    return dto.getValue();
                }
                return null;
            }

            @Override
            public void modify(Object element, String property, Object newValue) {
                if (!(element instanceof TableItem item)) return;

                VariableDTO entry = (VariableDTO) item.getData();
                String valueStr = Objects.toString(newValue, null);

                // Send user change event
                UserChangedFieldDTO dto = new UserChangedFieldDTO(entry.getName(), entry.getType(), valueStr);
                uiEventCollector.collectUiEvent(dto);

                // Update the table
                viewer.update(entry, null);
            }
        });
    }

    /**
     * Updates the fields displayed in the tab.
     *
     * @param vars list of VariableDTOs representing fields; can be null
     */
    public void updateFields(List<VariableDTO> vars) {
        if (table.isDisposed()) return;

        entries.clear();
        if (vars != null) {
            entries.addAll(vars);
        }

        viewer.setInput(entries);
        viewer.refresh();
    }

    /**
     * Returns the root composite containing this tab's controls.
     *
     * @return the composite
     */
    public Composite getControl() {
        return root;
    }
}
