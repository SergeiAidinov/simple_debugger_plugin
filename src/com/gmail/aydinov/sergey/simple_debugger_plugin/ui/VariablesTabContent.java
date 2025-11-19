package com.gmail.aydinov.sergey.simple_debugger_plugin.ui;

import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import com.sun.jdi.LocalVariable;
import com.sun.jdi.Value;

public class VariablesTabContent {

    private final Table table;

    public VariablesTabContent(Composite parent) {
        table = new Table(parent, SWT.BORDER | SWT.FULL_SELECTION);
        table.setHeaderVisible(true);
        table.setLinesVisible(true);

        TableColumn colName = new TableColumn(table, SWT.LEFT);
        colName.setText("Name");
        colName.setWidth(200);

        TableColumn colType = new TableColumn(table, SWT.LEFT);
        colType.setText("Type");
        colType.setWidth(200);

        TableColumn colValue = new TableColumn(table, SWT.LEFT);
        colValue.setText("Value");
        colValue.setWidth(200);
    }

    public Table getControl() {
        return table;
    }

    /** 
     * Главный метод: обновляет таблицу переменных.
     */
    public void updateVariables(Map<LocalVariable, Value> vars) {
        table.removeAll(); // очистить таблицу

        for (Map.Entry<LocalVariable, Value> entry : vars.entrySet()) {
            LocalVariable var = entry.getKey();
            Value val = entry.getValue();

            TableItem item = new TableItem(table, SWT.NONE);
            item.setText(0, var.name());
            item.setText(1, var.typeName());
            item.setText(2, valueToString(val));
        }
    }
    

    /** 
     * Отдельная функция для безопасного отображения JDI Value.
     */
    private String valueToString(Value v) {
        if (v == null)
            return "null";
        try {
            return v.toString();
        } catch (Exception e) {
            return "<error>";
        }
    }
}
