package com.gmail.aydinov.sergey.simple_debugger_plugin.ui.tab;

import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.TargetApplicationClassOrInterfaceRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.TargetApplicationElementRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.*;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.debug_event.DebugStoppedAtBreakpointEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.UserInvokedMethodEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.processor.UiEventCollector;

import java.util.List;
import java.util.Objects;

/**
 * Controller for the "Evaluate" tab in the debugger window.
 * Allows selecting classes and methods, entering arguments, invoking methods, and displaying results.
 */
public class EvaluateTabController {

    private final Composite root;
    private final Combo classCombo;
    private final Combo methodCombo;
    private final Button selectBtn;
    private final Button invokeBtn;
    private final Text methodInput;
    private final Text resultField;
    private final UiEventCollector uiEventCollector;
    private TableViewer stackTableViewer;

    /** Last selected method */
    private TargetApplicationMethodDTO lastMethod;

    public EvaluateTabController(Composite parent, UiEventCollector uiEventCollector) {
        this.uiEventCollector = uiEventCollector;

        root = new Composite(parent, SWT.NONE);
        root.setLayout(new GridLayout(2, false));

        // ====== Class selection ======
        Label typeLabel = new Label(root, SWT.NONE);
        typeLabel.setText("Type:");
        typeLabel.setToolTipText("Class or Interface");

        classCombo = new Combo(root, SWT.DROP_DOWN | SWT.READ_ONLY);
        classCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        classCombo.setToolTipText("Select a Class or Interface to invoke methods on");

        // ====== Method selection ======
        Label methodLabel = new Label(root, SWT.NONE);
        methodLabel.setText("Method:");
        methodLabel.setToolTipText("Select a method to move to Arguments field");

        methodCombo = new Combo(root, SWT.DROP_DOWN | SWT.READ_ONLY);
        methodCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        methodCombo.setToolTipText("Select a method");

        // ====== Select button ======
        selectBtn = new Button(root, SWT.PUSH);
        selectBtn.setText("Select");
        GridData selectGD = new GridData();
        selectGD.horizontalSpan = 2;
        selectGD.horizontalAlignment = SWT.CENTER;
        selectBtn.setLayoutData(selectGD);

        // ====== Arguments input ======
        methodInput = new Text(root, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
        GridData inputGD = new GridData(GridData.FILL_HORIZONTAL);
        inputGD.horizontalSpan = 2;
        inputGD.heightHint = 3 * 20;
        methodInput.setLayoutData(inputGD);
        methodInput.setToolTipText("Edit method arguments here");

        // ====== Invoke button ======
        invokeBtn = new Button(root, SWT.PUSH);
        invokeBtn.setText("Invoke");
        GridData invokeGD = new GridData();
        invokeGD.horizontalSpan = 2;
        invokeGD.horizontalAlignment = SWT.CENTER;
        invokeBtn.setLayoutData(invokeGD);

        // ====== Result field ======
        resultField = new Text(root, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.WRAP | SWT.READ_ONLY);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.horizontalSpan = 2;
        resultField.setLayoutData(gd);
        resultField.setToolTipText("Method invocation result");

        // ====== Listeners ======
        classCombo.addListener(SWT.Selection, e -> updateMethods());
        selectBtn.addListener(SWT.Selection, e -> onSelectMethod());
        invokeBtn.addListener(SWT.Selection, e -> onInvokeMethod());
    }

    public Composite getControl() {
        return root;
    }

    // ----------------- Stack Viewer -----------------
    public void createStackViewer(Composite parent) {
        stackTableViewer = new TableViewer(parent, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
        Table table = stackTableViewer.getTable();
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        table.setLayoutData(new GridData(GridData.FILL_BOTH));

        String[] titles = {"Class", "Method", "Source"};
        int[] bounds = {200, 150, 150};

        for (int i = 0; i < titles.length; i++) {
            TableViewerColumn col = new TableViewerColumn(stackTableViewer, SWT.NONE);
            col.getColumn().setText(titles[i]);
            col.getColumn().setWidth(bounds[i]);
            col.getColumn().setResizable(true);
        }

        stackTableViewer.setContentProvider(ArrayContentProvider.getInstance());
        stackTableViewer.setLabelProvider(new ITableLabelProvider() {
            public String getColumnText(Object element, int columnIndex) {
                if (!(element instanceof MethodCallInStackDTO call)) return "";
                return switch (columnIndex) {
                    case 0 -> call.getClassName();
                    case 1 -> call.getMethodName();
                    case 2 -> call.getSourceInfo();
                    default -> "";
                };
            }
            public Image getColumnImage(Object element, int columnIndex) { return null; }
            public void addListener(ILabelProviderListener listener) {}
            public void dispose() {}
            public boolean isLabelProperty(Object element, String property) { return false; }
            public void removeListener(ILabelProviderListener listener) {}
        });
    }

    public void updateFromEvent(DebugStoppedAtBreakpointEvent dto) {
        Display.getDefault().asyncExec(() -> {
            if (root.isDisposed()) return;

            classCombo.removeAll();

            for (TargetApplicationElementRepresentation el : dto.getTargetApplicationElements()) {
                if (el instanceof TargetApplicationClassOrInterfaceRepresentation clazz) {
                    String nameAndType = clazz.getTargetApplicationElementName() + " (" + el.getTargetApplicationElementType() + ")";
                    classCombo.add(nameAndType);
                    classCombo.setData(nameAndType, clazz);
                }
            }

            if (classCombo.getItemCount() > 0) {
                classCombo.select(0);
                updateMethods();
            }

            // Updating resultField with a safe null replacement
            resultField.setText(Objects.requireNonNullElse(dto.getResultOfMethodInvocation(), ""));
        });
    }

    public void updateStack(List<MethodCallInStackDTO> stack) {
        if (Objects.nonNull(stackTableViewer) && !stackTableViewer.getTable().isDisposed()) {
            stackTableViewer.setInput(stack);
        }
    }

    // ----------------- Method/Arguments -----------------
    private void updateMethods() {
        methodCombo.removeAll();

        String className = classCombo.getText();
        if (className.isBlank()) return;

        TargetApplicationClassOrInterfaceRepresentation clazz =
                (TargetApplicationClassOrInterfaceRepresentation) classCombo.getData(className);
        if (Objects.isNull(clazz)) return;

        TargetApplicationMethodDTO methodToSelect = null;

        for (TargetApplicationMethodDTO m : clazz.getMethods()) {
            String displayStr = buildMethodDisplay(m);
            methodCombo.add(displayStr);
            methodCombo.setData(displayStr, m);

            if (Objects.equals(lastMethod, m)) {
                methodToSelect = m;
            }
        }

        if (Objects.nonNull(methodToSelect)) {
            methodCombo.setText(buildMethodDisplay(methodToSelect));
        } else if (methodCombo.getItemCount() > 0) {
            methodCombo.select(0);
            lastMethod = getSelectedMethod();
        }
    }

    private String buildMethodDisplay(TargetApplicationMethodDTO method) {
        StringBuilder sb = new StringBuilder();
        sb.append(method.getMethodName()).append("(");
        List<TargetApplicationMethodParameterDTO> params = method.getParameters();
        for (int i = 0; i < params.size(); i++) {
            TargetApplicationMethodParameterDTO p = params.get(i);
            sb.append(p.getName()).append(": ").append(cleanTypeName(p.getTypeName()));
            if (i < params.size() - 1) sb.append(", ");
        }
        sb.append(") : ").append(cleanTypeName(method.getReturnType()));
        return sb.toString();
    }

    private String cleanTypeName(String typeName) {
        if (Objects.isNull(typeName)) return "";
        return typeName.replace(" (no class loader)", "");
    }

    private void onSelectMethod() {
        TargetApplicationMethodDTO selectedMethod = getSelectedMethod();
        if (Objects.isNull(selectedMethod)) return;

        lastMethod = selectedMethod;
        methodInput.setText(buildMethodDisplay(selectedMethod));
        methodInput.setSelection(selectedMethod.getMethodName().length() + 1);
        methodInput.setFocus();
    }

    private void onInvokeMethod() {
        clearResult();

        if (Objects.isNull(lastMethod)) {
            resultField.setText("No method selected to invoke.");
            return;
        }

        TargetApplicationClassOrInterfaceRepresentation clazz = getSelectedClass();
        String argsText = methodInput.getText();

        if (Objects.nonNull(clazz)) {
            UserInvokedMethodEvent invokeMethodEvent = new UserInvokedMethodEvent(clazz, lastMethod, argsText);
            uiEventCollector.collectUiEvent(invokeMethodEvent);
        } else {
            resultField.setText("No class selected to invoke method.");
        }
    }

    // ----------------- Public helpers -----------------
    public TargetApplicationClassOrInterfaceRepresentation getSelectedClass() {
        return (TargetApplicationClassOrInterfaceRepresentation) classCombo.getData(classCombo.getText());
    }

    public TargetApplicationMethodDTO getSelectedMethod() {
        return (TargetApplicationMethodDTO) methodCombo.getData(methodCombo.getText());
    }

    public void clearResult() {
        Display.getDefault().asyncExec(() -> {
            if (Objects.nonNull(resultField) && !resultField.isDisposed()) {
                resultField.setText("");
            }
        });
    }

    public void showResult(String text) {
        Display.getDefault().asyncExec(() -> {
            if (Objects.nonNull(resultField) && !resultField.isDisposed()) {
                resultField.setText(text);
            }
        });
    }

    public Button getSelectButton() {
        return selectBtn;
    }

    public Button getInvokeButton() {
        return invokeBtn;
    }

    public Text getMethodInput() {
        return methodInput;
    }

    public Text getResultField() {
        return resultField;
    }
}
