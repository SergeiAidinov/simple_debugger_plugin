package com.gmail.aydinov.sergey.simple_debugger_plugin.ui.tab;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;

import com.gmail.aydinov.sergey.simple_debugger_plugin.event.UserChangedVariable;
import com.gmail.aydinov.sergey.simple_debugger_plugin.processor.SimpleDebuggerEventQueue;
import com.gmail.aydinov.sergey.simple_debugger_plugin.processor.UiEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.VarEntry;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.Value;

public class VariablesTabContent {

    private final Table table;
    private final TableViewer viewer;
    private final UiEventCollector uiEventCollector = SimpleDebuggerEventQueue.instance();
    private final List<VarEntry> entries = new ArrayList<>();

    public VariablesTabContent(Composite parent) {

        table = new Table(parent, SWT.BORDER | SWT.FULL_SELECTION);
        table.setHeaderVisible(true);
        table.setLinesVisible(true);

        viewer = new TableViewer(table);
        viewer.setColumnProperties(new String[]{"name", "type", "value"});

        createColumn("Name", 200);
        createColumn("Type", 200);
        createColumn("Value", 200);

        viewer.setCellEditors(new CellEditor[]{
            null,
            null,
            new TextCellEditor(table)
        });

        viewer.setCellModifier(new ICellModifier() {

            @Override
            public boolean canModify(Object element, String property) {
                return "value".equals(property);
            }

            @Override
            public Object getValue(Object element, String property) {
                return ((VarEntry) element).getValue();
            }

            @Override
            public void modify(Object element, String property, Object newValue) {

                if (!(element instanceof org.eclipse.swt.widgets.TableItem))
                    return;

                VarEntry entry = (VarEntry) ((org.eclipse.swt.widgets.TableItem) element).getData();

                entry.setNewValue(newValue);

                System.out.println("ENTER: " + entry.getNewValue());

                uiEventCollector.collectUiEvent(new UserChangedVariable(entry));

                viewer.update(entry, null);
            }
        });

        viewer.setContentProvider(new IStructuredContentProvider() {
            @Override
            public Object[] getElements(Object inputElement) {
                return entries.toArray();
            }

            @Override
            public void dispose() {}

            @Override
            public void inputChanged(Viewer viewer, Object a, Object b) {}
        });

        viewer.setLabelProvider(new ITableLabelProvider() {
            @Override
            public String getColumnText(Object element, int columnIndex) {
                VarEntry var = (VarEntry) element;
                switch (columnIndex) {
                    case 0: return var.getLocalVar().name();
                    case 1: return var.getType();
                    case 2: return var.getValue();
                }
                return "";
            }

            @Override public org.eclipse.swt.graphics.Image getColumnImage(Object e, int c) { return null; }
            @Override public void addListener(ILabelProviderListener listener) {}
            @Override public void dispose() {}
            @Override public boolean isLabelProperty(Object e, String p) { return false; }
            @Override public void removeListener(ILabelProviderListener l) {}
        });
    }

    private void createColumn(String title, int width) {
        TableViewerColumn col = new TableViewerColumn(viewer, SWT.LEFT);
        col.getColumn().setText(title);
        col.getColumn().setWidth(width);
        col.getColumn().setResizable(true);
        col.getColumn().setMoveable(true);
    }

    public Table getControl() {
        return table;
    }

    public void updateVariables(Map<LocalVariable, Value> vars) {

        if (table.isDisposed()) return;

        entries.clear();

        for (Map.Entry<LocalVariable, Value> e : vars.entrySet()) {
            entries.add(new VarEntry(e.getKey(), e.getValue()));
        }

        viewer.setInput(entries);
        viewer.refresh();
    }
}
