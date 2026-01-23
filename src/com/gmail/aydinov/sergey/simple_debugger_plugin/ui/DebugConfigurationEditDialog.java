package com.gmail.aydinov.sergey.simple_debugger_plugin.ui;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import com.gmail.aydinov.sergey.simple_debugger_plugin.DebugConfiguration;

import java.nio.file.Path;
import java.util.stream.Collectors;

public class DebugConfigurationEditDialog extends TitleAreaDialog {

    private DebugConfiguration config;

    private Text mainClassText;
    private Text vmOptionsText;
    private Text portText;

    public DebugConfigurationEditDialog(Shell parentShell, DebugConfiguration config) {
        super(parentShell);
        this.config = config;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        setTitle("Edit Debug Configuration");
        setMessage("You can edit Main class, VM options and port before launching the target");

        Composite area = (Composite) super.createDialogArea(parent);
        Composite container = new Composite(area, SWT.NONE);
        container.setLayout(new GridLayout(2, false));

        // ----------------------------
        // Main class (editable)
        // ----------------------------
        createLabel(container, "Main class:");
        mainClassText = createEditableText(container, config.getMainClassName(), 60);

        // ----------------------------
        // Working directory (read-only)
        // ----------------------------
        createLabel(container, "Working directory:");
        createReadOnlyText(container, config.getWorkingDirectory().toString());

        // ----------------------------
        // Output folder (read-only)
        // ----------------------------
        createLabel(container, "Output folder:");
        createReadOnlyText(container, config.getOutputFolder().toString());

        // ----------------------------
        // Classpath (read-only)
        // ----------------------------
        createLabel(container, "Classpath:");
        createReadOnlyText(container,
                config.getAdditionalClasspath().stream()
                        .map(Path::toString)
                        .collect(Collectors.joining(System.getProperty("path.separator"))));

        // ----------------------------
        // VM Options (editable)
        // ----------------------------
        createLabel(container, "VM Options:");
        vmOptionsText = createEditableText(container, config.asVmOptionsString(), 60);

        // ----------------------------
        // Port (editable)
        // ----------------------------
        createLabel(container, "Port:");
        portText = createEditableText(container, String.valueOf(config.getPort()), 10);

        return area;
    }

    private void createLabel(Composite parent, String text) {
        new Label(parent, SWT.NONE).setText(text);
    }

    private Text createReadOnlyText(Composite parent, String text) {
        Text t = new Text(parent, SWT.BORDER | SWT.READ_ONLY | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
        t.setText(text);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.heightHint = 40;
        t.setLayoutData(gd);
        return t;
    }

    private Text createEditableText(Composite parent, String text, int widthHint) {
        Text t = new Text(parent, SWT.BORDER | SWT.SINGLE);
        t.setText(text);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.widthHint = widthHint * 7; // приближение ширины символов
        t.setLayoutData(gd);
        return t;
    }

    @Override
    protected void okPressed() {
        // ----------------------------
        // Сохраняем Main class
        // ----------------------------
        String mainClass = mainClassText.getText().trim();
        config.setMainClassName(mainClass);

        // ----------------------------
        // Сохраняем VM Options
        // ----------------------------
        String vmOptions = vmOptionsText.getText().trim();
        config.setVmOptions(vmOptions.split("\\s+"));

        // ----------------------------
        // Сохраняем порт
        // ----------------------------
        try {
            int port = Integer.parseInt(portText.getText().trim());
            if (port < 0 || port > 65535) {
                showError("Invalid Port", "Port must be between 0 and 65535.");
                return; // не закрывать диалог
            }
            config.setPort(port);
        } catch (NumberFormatException e) {
            showError("Invalid Port", "Please enter a valid number for port.");
            return; // не закрывать диалог
        }

        super.okPressed();
    }

    // ----------------------------
    // Вспомогательный метод для сообщений об ошибке
    // ----------------------------
    private void showError(String title, String message) {
        MessageBox box = new MessageBox(getShell(), SWT.ICON_ERROR | SWT.OK);
        box.setText(title);
        box.setMessage(message);
        box.open();
    }
}
