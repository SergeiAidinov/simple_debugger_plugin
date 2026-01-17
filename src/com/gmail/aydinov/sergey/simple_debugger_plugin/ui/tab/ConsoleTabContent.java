package com.gmail.aydinov.sergey.simple_debugger_plugin.ui.tab;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

public class ConsoleTabContent {

    private final Text consoleText;

    public ConsoleTabContent(Composite parent) {
        consoleText = new Text(
                parent,
                SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.READ_ONLY
        );
        consoleText.setLayoutData(
                new GridData(SWT.FILL, SWT.FILL, true, true)
        );
    }

    public Text getControl() {
        return consoleText;
    }

    public void appendLine(String line) {
        if (consoleText.isDisposed())
            return;
        consoleText.append(line + "\n");
    }

    public void clear() {
        if (consoleText.isDisposed())
            return;
        consoleText.setText("");
    }
}
