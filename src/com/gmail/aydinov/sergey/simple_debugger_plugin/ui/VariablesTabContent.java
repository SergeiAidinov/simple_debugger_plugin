package com.gmail.aydinov.sergey.simple_debugger_plugin.ui;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.gmail.aydinov.sergey.simple_debugger_plugin.processor.UiEventProvider;
import com.gmail.aydinov.sergey.simple_debugger_plugin.processor.events.UIEventUpdateVariable;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.StackFrame;
import com.sun.jdi.Value;

public class VariablesTabContent {

    //private final UiEventProvider uiEventProvider;
    private final Table table;
    private final TableViewer viewer;
  //  private StackFrame currentStackFrame;

    private static class VarEntry {
        LocalVariable localVar;
        String type;
        String value;

        VarEntry(LocalVariable localVar, Value val) {
            this.localVar = localVar;
            this.type = localVar.typeName();
            this.value = valueToString(val);
        }
    }

    private final List<VarEntry> entries = new ArrayList<>();

    public VariablesTabContent(Composite parent) {
       // this.uiEventProvider = uiEventProvider;

        table = new Table(parent, SWT.BORDER | SWT.FULL_SELECTION);
        table.setHeaderVisible(true);
        table.setLinesVisible(true);

        viewer = new TableViewer(table);
        viewer.setColumnProperties(new String[]{"name", "type", "value"});

        createColumn("Name", 200, 0);
        createColumn("Type", 200, 1);
        createColumn("Value", 200, 2);

        viewer.setCellEditors(new CellEditor[]{null, null, new TextCellEditor(table)});

        viewer.setCellModifier(new ICellModifier() {
            @Override
            public boolean canModify(Object element, String property) {
                return "value".equals(property);
            }

            @Override
            public Object getValue(Object element, String property) {
                return ((VarEntry) element).value;
            }

            @Override
            public void modify(Object element, String property, Object value) {
                VarEntry var;
                if (element instanceof org.eclipse.swt.widgets.TableItem) {
                    var = (VarEntry) ((org.eclipse.swt.widgets.TableItem) element).getData();
                } else {
                    var = (VarEntry) element;
                }

                var.value = (String) value;

				/*
				 * if (currentStackFrame != null) { uiEventProvider.sendUiEvent(new
				 * UIEventUpdateVariable(currentStackFrame, var.localVar, var.value)); }
				 */

                viewer.update(var, null);
            }
        });

        // ContentProvider
        viewer.setContentProvider(new IStructuredContentProvider() {
            @Override
            public Object[] getElements(Object inputElement) {
                if (inputElement instanceof List) {
                    return ((List<?>) inputElement).toArray();
                }
                return new Object[0];
            }

            @Override
            public void dispose() {}

            @Override
            public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}
        });

        // LabelProvider
        viewer.setLabelProvider(new ITableLabelProvider() {
            @Override
            public String getColumnText(Object element, int columnIndex) {
                VarEntry var = (VarEntry) element;
                switch (columnIndex) {
                    case 0: return var.localVar.name();
                    case 1: return var.type;
                    case 2: return var.value;
                }
                return "";
            }

            @Override
            public org.eclipse.swt.graphics.Image getColumnImage(Object element, int columnIndex) {
                return null;
            }

            @Override
            public void addListener(org.eclipse.jface.viewers.ILabelProviderListener listener) {}
            @Override
            public void dispose() {}
            @Override
            public boolean isLabelProperty(Object element, String property) { return false; }
            @Override
            public void removeListener(org.eclipse.jface.viewers.ILabelProviderListener listener) {}
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

    public void updateVariables(/*StackFrame stackFrame, */ Map<LocalVariable, Value> vars) {
       // currentStackFrame = stackFrame;

        entries.clear();
        for (Map.Entry<LocalVariable, Value> entry : vars.entrySet()) {
            entries.add(new VarEntry(entry.getKey(), entry.getValue()));
        }

        viewer.setInput(entries);
    }

    private static String valueToString(Value v) {
        if (v == null) return "null";
        try {
            return v.toString();
        } catch (Exception e) {
            return "<error>";
        }
    }
}
