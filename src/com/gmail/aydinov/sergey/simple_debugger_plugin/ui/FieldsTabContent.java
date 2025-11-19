package com.gmail.aydinov.sergey.simple_debugger_plugin.ui;

import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import com.sun.jdi.Field;
import com.sun.jdi.Value;

public class FieldsTabContent {

    private Composite root;
    private Table table;

    public FieldsTabContent(Composite parent) {
        root = new Composite(parent, SWT.NONE);
        root.setLayout(new FillLayout());

        table = new Table(root, SWT.BORDER | SWT.FULL_SELECTION);
        table.setHeaderVisible(true);

        TableColumn nameCol = new TableColumn(table, SWT.LEFT);
        nameCol.setText("Field");
        nameCol.setWidth(200);

        TableColumn valueCol = new TableColumn(table, SWT.LEFT);
        valueCol.setText("Value");
        valueCol.setWidth(300);
    }

    public Composite getControl() {
        return root;
    }

	public void updateFields(Map<Field, Value> fields) {
		// TODO Auto-generated method stub
		
	}
}
