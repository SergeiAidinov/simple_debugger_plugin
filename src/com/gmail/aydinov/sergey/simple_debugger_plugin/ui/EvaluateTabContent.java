package com.gmail.aydinov.sergey.simple_debugger_plugin.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

public class EvaluateTabContent {

    private Composite root;

    private Text inputField;
    private Text resultField;

    public EvaluateTabContent(Composite parent) {

        root = new Composite(parent, SWT.NONE);
        root.setLayout(new GridLayout(2, false));

        inputField = new Text(root, SWT.BORDER);
        inputField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Button evalBtn = new Button(root, SWT.PUSH);
        evalBtn.setText("Run");

        resultField = new Text(root, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.WRAP | SWT.READ_ONLY);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.horizontalSpan = 2;
        resultField.setLayoutData(gd);

        // Заглушка обработчика
        evalBtn.addListener(SWT.Selection, e -> {
            String expr = inputField.getText();
            resultField.setText("Evaluated: " + expr);
        });
    }

    public Composite getControl() {
        return root;
    }
}
