package com.gmail.aydinov.sergey.simple_debugger_plugin.ui.tab.inspect_window_tab;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

public class LoadingPopup {

    private Shell shell;
    private Label messageLabel;

    public void showPopup(String message, Runnable cancelAction) {

        Display display = Display.getDefault();

        display.asyncExec(() -> {

            if (shell != null && !shell.isDisposed()) {
                messageLabel.setText(message);
                shell.layout();
                return;
            }

            shell = new Shell(display, SWT.ON_TOP | SWT.TOOL);

            shell.setText("Loading");

            shell.setLayout(new GridLayout(1, false));

            messageLabel = new Label(shell, SWT.WRAP);
            messageLabel.setLayoutData(
                    new GridData(SWT.FILL, SWT.CENTER, true, false));

            messageLabel.setText(message);

            Button cancelButton = new Button(shell, SWT.PUSH);
            cancelButton.setText("Cancel loading");

            cancelButton.setLayoutData(
                    new GridData(SWT.CENTER, SWT.CENTER, true, false));

            cancelButton.addListener(SWT.Selection, e -> {
                if (cancelAction != null) {
                    cancelAction.run();
                }

                close();
            });

            shell.setSize(350, 120);
            shell.open();
        });
    }

    public void close() {

        Display.getDefault().asyncExec(() -> {

            if (shell != null && !shell.isDisposed()) {
                shell.close();
                shell.dispose();
            }

            shell = null;
        });
    }
}
