package com.gmail.aydinov.sergey.simple_debugger_plugin.ui.tab;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;

import com.gmail.aydinov.sergey.simple_debugger_plugin.processor.SimpleDebuggerEventQueue;
import com.gmail.aydinov.sergey.simple_debugger_plugin.processor.UiEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.UserChangedField;

import com.sun.jdi.Field;
import com.sun.jdi.Value;

public class FieldsTabContent {

    private final Table table;
    private final TableViewer viewer;
    private final UiEventCollector uiEventCollector = SimpleDebuggerEventQueue.instance();
    private final List<FieldEntry> entries = new ArrayList<>();

    public FieldsTabContent(Composite parent) {

        table = new Table(parent, SWT.BORDER | SWT.FULL_SELECTION);
        table.setHeaderVisible(true);
        table.setLinesVisible(true);

        viewer = new TableViewer(table);
        viewer.setColumnProperties(new String[]{"name", "value"});

        createColumn("Field", 200, 0);
        createColumn("Value", 300, 1);

        viewer.setCellEditors(new CellEditor[]{null, new TextCellEditor(table)});

        viewer.setCellModifier(new ICellModifier() {

            @Override
            public boolean canModify(Object element, String property) {
                return "value".equals(property);
            }

            @Override
            public Object getValue(Object element, String property) {
                return ((FieldEntry) element).getValue();
            }

            @Override
            public void modify(Object element, String property, Object newValue) {
                FieldEntry fieldEntry;

                if (element instanceof org.eclipse.swt.widgets.TableItem) {
                    fieldEntry = (FieldEntry) ((org.eclipse.swt.widgets.TableItem) element).getData();
                } else {
                    fieldEntry = (FieldEntry) element;
                }

                fieldEntry.setNewValue(newValue.toString());

                System.out.println("ENTER FIELD: " + fieldEntry.getField().name() + " = " + newValue);

                uiEventCollector.collectUiEvent(new UserChangedField(fieldEntry));

                viewer.update(fieldEntry, null);
            }
        });

        // Content provider
        viewer.setContentProvider(new IStructuredContentProvider() {
            @Override
            public Object[] getElements(Object inputElement) {
                if (inputElement instanceof List<?> list) {
                    return list.toArray();
                }
                return new Object[0];
            }
            @Override public void dispose() {}
            @Override public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}
        });

        viewer.setLabelProvider(new ITableLabelProvider() {
            @Override
            public String getColumnText(Object element, int columnIndex) {
                FieldEntry entry = (FieldEntry) element;
                return switch (columnIndex) {
                    case 0 -> entry.getField().name();
                    case 1 -> entry.getValue();
                    default -> "";
                };
            }

            @Override
            public org.eclipse.swt.graphics.Image getColumnImage(Object e, int i) { return null; }
            @Override public void addListener(org.eclipse.jface.viewers.ILabelProviderListener listener) {}
            @Override public void dispose() {}
            @Override public boolean isLabelProperty(Object arg0, String arg1) { return false; }
            @Override public void removeListener(org.eclipse.jface.viewers.ILabelProviderListener listener) {}
        });
    }


    private void createColumn(String title, int width, int index) {
        TableViewerColumn col = new TableViewerColumn(viewer, SWT.LEFT);
        col.getColumn().setText(title);
        col.getColumn().setWidth(width);
        col.getColumn().setResizable(true);
        col.getColumn().setMoveable(true);
    }


    public Table getControl() {
        return table;
    }


    public void updateFields(Map<Field, Value> fields) {

        entries.clear();

        for (Map.Entry<Field, Value> e : fields.entrySet()) {
            entries.add(new FieldEntry(e.getKey(), e.getValue()));
        }

        viewer.setInput(entries);
    }
}
