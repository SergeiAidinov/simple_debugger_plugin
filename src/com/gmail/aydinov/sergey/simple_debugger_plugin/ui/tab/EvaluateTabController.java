package com.gmail.aydinov.sergey.simple_debugger_plugin.ui.tab;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.TargetApplicationClassOrInterfaceRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.TargetApplicationElementRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.TargetApplicationMethodDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebugEventDTO;

public class EvaluateTabController {

    private final Composite root;
    private final Combo classCombo;
    private final Combo methodCombo;
    private final Button invokeBtn;
    private final Text resultField;

    public EvaluateTabController(Composite parent) {
        root = new Composite(parent, SWT.NONE);
        root.setLayout(new GridLayout(2, false));

        // ====== Type label with tooltip ======
        Label typeLabel = new Label(root, SWT.NONE);
        typeLabel.setText("Type:");
        typeLabel.setToolTipText("Class or Interface");

        classCombo = new Combo(root, SWT.DROP_DOWN | SWT.READ_ONLY);
        classCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        classCombo.setToolTipText("Select a Class or Interface to invoke methods on");

        Label methodLabel = new Label(root, SWT.NONE);
        methodLabel.setText("Method:");
        methodLabel.setToolTipText("Select a method to invoke");

        methodCombo = new Combo(root, SWT.DROP_DOWN | SWT.READ_ONLY);
        methodCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        methodCombo.setToolTipText("Select a method to invoke");

        invokeBtn = new Button(root, SWT.PUSH);
        invokeBtn.setText("Invoke");
        GridData btnGD = new GridData();
        btnGD.horizontalSpan = 2;
        btnGD.horizontalAlignment = SWT.CENTER;
        invokeBtn.setLayoutData(btnGD);

        resultField = new Text(root, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.WRAP | SWT.READ_ONLY);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.horizontalSpan = 2;
        resultField.setLayoutData(gd);
        resultField.setToolTipText("Result of method invocation");

        classCombo.addListener(SWT.Selection, e -> updateMethods());
    }

    public Composite getControl() {
        return root;
    }

    public void updateFromEvent(SimpleDebugEventDTO dto) {
        classCombo.removeAll();

        for (TargetApplicationElementRepresentation el : dto.getTargetApplicationElementRepresentationList()) {
            if (el instanceof TargetApplicationClassOrInterfaceRepresentation clazz) {
                String name = clazz.getTargetApplicationElementName();
                classCombo.add(name);
                classCombo.setData(name, clazz);
            }
        }

        if (classCombo.getItemCount() > 0) {
            classCombo.select(0);
            updateMethods();
        }
    }

    private void updateMethods() {
        methodCombo.removeAll();

        String className = classCombo.getText();
        if (className == null)
            return;

        TargetApplicationClassOrInterfaceRepresentation clazz =
                (TargetApplicationClassOrInterfaceRepresentation) classCombo.getData(className);
        if (clazz == null)
            return;

        for (TargetApplicationMethodDTO m : clazz.getMethods()) {
            String display = m.getMethodName() + " : " + m.getReturnType();
            methodCombo.add(display);
            methodCombo.setData(display, m);
        }

        if (methodCombo.getItemCount() > 0)
            methodCombo.select(0);
    }

    public TargetApplicationClassOrInterfaceRepresentation getSelectedClass() {
        return (TargetApplicationClassOrInterfaceRepresentation) classCombo.getData(classCombo.getText());
    }

    public TargetApplicationMethodDTO getSelectedMethod() {
        return (TargetApplicationMethodDTO) methodCombo.getData(methodCombo.getText());
    }

    public Button getInvokeButton() {
        return invokeBtn;
    }

    public Text getResultField() {
        return resultField;
    }
}
