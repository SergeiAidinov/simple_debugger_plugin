package com.gmail.aydinov.sergey.simple_debugger_plugin.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

public class VariablesTabContent {

    private Composite root;
    private Table table;

    public VariablesTabContent(Composite parent) {
        root = new Composite(parent, SWT.NONE);
        root.setLayout(new FillLayout());

        table = new Table(root, SWT.BORDER | SWT.FULL_SELECTION);
        table.setHeaderVisible(true);

        TableColumn nameCol = new TableColumn(table, SWT.LEFT);
        nameCol.setText("Variable");
        nameCol.setWidth(200);

        TableColumn valueCol = new TableColumn(table, SWT.LEFT);
        valueCol.setText("Value");
        valueCol.setWidth(300);
    }

    public Composite getControl() {
        return root;
    }
}
