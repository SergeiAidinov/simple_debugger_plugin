package com.gmail.aydinov.sergey.simple_debugger_plugin.ui.tab;

import java.util.List;
import java.util.Objects;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.MethodCallInStack;

/**
 * Manages the "Stack" tab in the debugger UI, showing the current call stack.
 */
public class StackTabContent {

    /** Root composite for this tab */
    private final Composite root;

    /** Table displaying the call stack */
    private final Table table;

    /**
     * Constructs a StackTabContent attached to a parent composite.
     *
     * @param parent the parent composite
     */
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

    /**
     * Returns the root composite containing this tab's controls.
     *
     * @return the root composite
     */
    public Composite getControl() {
        return root;
    }

    /**
     * Updates the stack table. Each element in the list represents a stack frame.
     *
     * @param stackFrames list of MethodCallInStack objects
     */
    public void updateStack(List<MethodCallInStack> stackFrames) {
        if (table.isDisposed() || stackFrames == null) return;
        table.removeAll();

        for (MethodCallInStack frame : stackFrames) {
            TableItem item = new TableItem(table, SWT.NONE);
            item.setText(new String[]{
                Objects.toString(frame.getClassName(), "") + "." + 
                Objects.toString(frame.getMethodName(), "") + "()",
                Objects.toString(frame.getSourceInfo(), "")
            });
        }
    }
}
