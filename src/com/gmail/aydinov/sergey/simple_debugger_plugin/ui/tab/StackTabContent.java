package com.gmail.aydinov.sergey.simple_debugger_plugin.ui.tab;

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.MethodCallInStack;

public class StackTabContent {

    private final Composite root;
    private final Table table;

    public StackTabContent(Composite parent) {
        root = new Composite(parent, SWT.NONE);
        root.setLayout(new FillLayout());

        table = new Table(root, SWT.BORDER | SWT.FULL_SELECTION);
        table.setHeaderVisible(true);
        table.setLinesVisible(true);

        TableColumn frameCol = new TableColumn(table, SWT.LEFT);
        frameCol.setText("Stack Frame");
        frameCol.setWidth(400);

        TableColumn sourceCol = new TableColumn(table, SWT.LEFT);
        sourceCol.setText("Source");
        sourceCol.setWidth(200);
    }

    public Composite getControl() {
        return root;
    }

    /**
     * Обновляет таблицу стека. Каждый элемент списка — отдельный кадр.
     */
    public void updateStack(List<MethodCallInStack> stackFrames) {
        if (table.isDisposed()) return;
        table.removeAll();

        for (MethodCallInStack frame : stackFrames) {
            TableItem item = new TableItem(table, SWT.NONE);
            item.setText(new String[] {
                frame.getClassName() + "." + frame.getMethodName() + "()",
                frame.getSourceInfo()
            });
        }
    }
}
