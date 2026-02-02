package com.gmail.aydinov.sergey.simple_debugger_plugin.ui.tab;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

/**
 * UI component for the Console tab in the debugger window.
 * Displays text output from the debugger and allows appending or clearing lines.
 */
public class ConsoleTabContent {

    private final Text consoleText;

    /**
     * Constructs the console tab content.
     * @param parent the parent composite in which the console text will be created
     */
    public ConsoleTabContent(Composite parent) {
        consoleText = new Text(parent, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.READ_ONLY);
        consoleText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    }

    /**
     * Returns the underlying SWT control for adding to the tab folder.
     * @return the Text control representing the console
     */
    public Text getControl() {
        return consoleText;
    }

    /**
     * Appends a line of text to the console. Automatically adds a newline character.
     * Safe to call even if the control has been disposed.
     * @param line the line of text to append
     */
    public void appendLine(String line) {
        if (consoleText.isDisposed()) return;
        consoleText.append(line + "\n");
    }

    /**
     * Clears all text from the console.
     * Safe to call even if the control has been disposed.
     */
    public void clear() {
        if (consoleText.isDisposed()) return;
        consoleText.setText("");
    }
}
