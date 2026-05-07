package com.gmail.aydinov.sergey.simple_debugger_plugin.ui.window;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

public class LoadingWindow {

    private final Display display;
    private final Shell shell;

    private final Label messageLabel;
    private final Button cancelButton;

    private Runnable onCancel;

    public LoadingWindow() {
        this.display = Display.getDefault();

        // ✅ полноценное окно с рамкой как в Eclipse
        shell = new Shell(display.getActiveShell(), SWT.SHELL_TRIM);
        shell.setText("Loading");
        shell.setLayout(new GridLayout(1, false));
        shell.setSize(350, 140);

        GridData labelData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        labelData.widthHint = 320;

        messageLabel = new Label(shell, SWT.WRAP);
        messageLabel.setLayoutData(labelData);
        messageLabel.setText("Loading...");

        cancelButton = new Button(shell, SWT.PUSH);
        cancelButton.setText("Cancel loading");
        cancelButton.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false));

        cancelButton.addListener(SWT.Selection, e -> {
            if (onCancel != null) {
                onCancel.run();
            }
            close();
        });

        shell.addListener(SWT.Close, e -> {
            e.doit = false;
            close();
        });
    }

    public void setMessage(String message) {
        display.asyncExec(() -> {
            if (!shell.isDisposed()) {
                messageLabel.setText(message);
                shell.pack();
                shell.layout();
            }
        });
    }

    public void show() {
        display.asyncExec(() -> {
            if (!shell.isDisposed()) {
                shell.open();
                shell.layout();
                shell.setActive(); // 👈 важно для Eclipse-like поведения
            }
        });
    }

    public void setOnCancel(Runnable onCancel) {
        this.onCancel = onCancel;
    }

    public void close() {
    	System.out.println("close() close() close() close() close() close() close() ");
        display.asyncExec(() -> {
            if (!shell.isDisposed()) {
                shell.dispose();
            }
        });
    }

    public Shell getShell() {
        return shell;
    }
}