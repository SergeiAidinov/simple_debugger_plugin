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

import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.VariableDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.UserChangedFieldEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.processor.UiEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.processor.SimpleDebuggerEventQueue;

/**
 * Manages the "Fields" tab in the debugger UI.
 * 
 * Displays all fields of the current context, allows editing their values,
 * and safely propagates changes to the debugger backend.
 * <p>
 * Author: Sergei Aidinov
 * <br>
 * Email: <a href="mailto:sergey.aydinov@gmail.com">sergey.aydinov@gmail.com</a>
 * </p>
 */
public class FieldsTabContent {

    /** Root composite for the tab */
    private final Composite root;

    /** Table widget showing the fields */
    private final Table table;

    /** TableViewer for structured display and editing */
    private final TableViewer viewer;

    /** Cached list of fields displayed in the table */
    private final List<VariableDTO> entries = new ArrayList<>();

    /** Collector for sending UI events to the debugger */
    private final UiEventCollector uiEventCollector;

    /**
     * Constructs the Fields tab content.
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

    /** Sets up table columns: Field, Type, Value */
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
                    // Updating value safely with null replacement
                    return Objects.toString(dto.getValue(), "");
                }
                return "";
            }
        });

        viewer.setColumnProperties(new String[]{"name", "type", "value"});
        viewer.setCellEditors(new CellEditor[]{null, null, new TextCellEditor(table)});
    }

    /** Sets up editing for the Value column */
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

                // Send user field change event
                UserChangedFieldEvent dto = new UserChangedFieldEvent(entry.getName(), entry.getType(), valueStr);
                uiEventCollector.collectUiEvent(dto);

                // Refresh table to reflect new value
                viewer.update(entry, null);
            }
        });
    }

    /**
     * Updates the fields displayed in the tab.
     *
     * @param vars list of variables to show; may be null
     */
    public void updateFields(List<VariableDTO> vars) {
        if (table.isDisposed()) return;

        entries.clear();
        if (Objects.nonNull(vars)) entries.addAll(vars);

        viewer.setInput(entries);
        viewer.refresh();
    }

    /**
     * Returns the root composite of this tab.
     *
     * @return the root composite
     */
    public Composite getControl() {
        return root;
    }
}
