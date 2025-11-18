package com.gmail.aydinov.sergey.simple_debugger_plugin.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

public class StackTabContent {

    private Composite root;
    private Table table;

    public StackTabContent(Composite parent) {
        root = new Composite(parent, SWT.NONE);
        root.setLayout(new FillLayout());

        table = new Table(root, SWT.BORDER | SWT.FULL_SELECTION);
        table.setHeaderVisible(true);

        TableColumn frameCol = new TableColumn(table, SWT.LEFT);
        frameCol.setText("Stack Frame");
        frameCol.setWidth(400);
    }

    public Composite getControl() {
        return root;
    }
}
