package com.gmail.aydinov.sergey.simple_debugger_plugin.ui.dialogs;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.layout.*;
import org.eclipse.jface.window.Window;

public class PortSelectionDialog extends TitleAreaDialog {

    private String selectedPort = "5005";
    private Combo combo;
    private Text manualInput;

    public PortSelectionDialog(Shell parentShell) {
        super(parentShell);
    }

    @Override
    public void create() {
        super.create();
        setTitle("Select Port for Simple Debugger");
        setMessage("Choose a common port or enter your own.");
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);

        Composite container = new Composite(area, SWT.NONE);
        container.setLayout(new GridLayout(2, false));
        container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        // Popular ports
        new Label(container, SWT.NONE).setText("Popular ports:");

        combo = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
        combo.setItems(new String[]{"5005", "8000", "8787", "9000"});
        combo.select(0);

        combo.addListener(SWT.Selection, e -> manualInput.setText(combo.getText()));

        // Manual input
        new Label(container, SWT.NONE).setText("Enter custom port:");
        manualInput = new Text(container, SWT.BORDER);
        manualInput.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        manualInput.setText(combo.getText());

        manualInput.addListener(SWT.Modify, e -> validatePort());

        return area;
    }

    private void validatePort() {
        String text = manualInput.getText();
        try {
            int p = Integer.parseInt(text);
            if (p > 0 && p < 65536) {
                selectedPort = text;
                getButton(OK).setEnabled(true);
                setErrorMessage(null);
                return;
            }
        } catch (Exception ignored) {}

        getButton(OK).setEnabled(false);
        setErrorMessage("Enter a valid port number (1–65535).");
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        super.createButtonsForButtonBar(parent);
        getButton(OK).setEnabled(true);
    }

    @Override
    protected void okPressed() {
        selectedPort = manualInput.getText();
        super.okPressed();
    }

    public Integer getPort() {
        return (getReturnCode() == Window.OK)
                ? Integer.parseInt(selectedPort)
                : null;
    }
}
