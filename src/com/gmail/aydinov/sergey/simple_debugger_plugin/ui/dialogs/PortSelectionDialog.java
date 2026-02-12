package com.gmail.aydinov.sergey.simple_debugger_plugin.ui.dialogs;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.layout.*;
import org.eclipse.jface.window.Window;

/**
 * Dialog to select a port for the Simple Debugger.
 * Users can choose from popular ports or enter a custom port.
 * <p>
 * Author: Sergei Aidinov
 * <br>
 * Email: <a href="mailto:sergey.aydinov@gmail.com">sergey.aydinov@gmail.com</a>
 * </p>
 */
public class PortSelectionDialog extends TitleAreaDialog {

    /** Currently selected port as string */
    private String selectedPort = "5005";

    /** Combo box for popular ports */
    private Combo combo;

    /** Text field for manual port entry */
    private Text manualInput;

    /**
     * Creates a port selection dialog.
     *
     * @param parentShell parent shell
     */
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

    /**
     * Validates the manually entered port and enables/disables the OK button.
     */
    private void validatePort() {
        String text = manualInput.getText();
        try {
            int port = Integer.parseInt(text);
            if (port > 0 && port < 65536) {
                selectedPort = text;
                getButton(OK).setEnabled(true);
                setErrorMessage(null);
                return;
            }
        } catch (Exception ignored) {
        }

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

    /**
     * Returns the selected port if OK was pressed, otherwise null.
     *
     * @return selected port number or null
     */
    public Integer getPort() {
        return (getReturnCode() == Window.OK)
                ? Integer.parseInt(selectedPort)
                : null;
    }
}
