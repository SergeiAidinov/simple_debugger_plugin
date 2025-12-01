package com.gmail.aydinov.sergey.simple_debugger_plugin.ui.tab;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.TargetApplicationClassOrInterfaceRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.TargetApplicationElementRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.TargetApplicationMethodDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.TargetApplicationMethodParameterDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.InvokeMethodEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebugEventDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.processor.UiEventCollector;
import com.sun.jdi.Type;
import java.util.List;

public class EvaluateTabController {

	private final Composite root;
	private final Combo classCombo;
	private final Combo methodCombo;
	private final Button selectBtn;
	private final Button invokeBtn;
	private final Text methodInput;
	private final Text resultField;
	private final UiEventCollector uiEventCollector;

	public EvaluateTabController(Composite parent, UiEventCollector uiEventCollector) {
		this.uiEventCollector = uiEventCollector;
		root = new Composite(parent, SWT.NONE);
		root.setLayout(new GridLayout(2, false));

		// ====== Type label ======
		Label typeLabel = new Label(root, SWT.NONE);
		typeLabel.setText("Type:");
		typeLabel.setToolTipText("Class or Interface");

		classCombo = new Combo(root, SWT.DROP_DOWN | SWT.READ_ONLY);
		classCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		classCombo.setToolTipText("Select a Class or Interface to invoke methods on");

		Label methodLabel = new Label(root, SWT.NONE);
		methodLabel.setText("Method:");
		methodLabel.setToolTipText("Select a method to move to Arguments field");

		methodCombo = new Combo(root, SWT.DROP_DOWN | SWT.READ_ONLY);
		methodCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		methodCombo.setToolTipText("Select a method");

		// ====== Button Select above Arguments ======
		selectBtn = new Button(root, SWT.PUSH);
		selectBtn.setText("Select");
		GridData selectGD = new GridData();
		selectGD.horizontalSpan = 2;
		selectGD.horizontalAlignment = SWT.CENTER;
		selectBtn.setLayoutData(selectGD);

		// ====== Arguments field ======
		methodInput = new Text(root, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
		GridData inputGD = new GridData(GridData.FILL_HORIZONTAL);
		inputGD.horizontalSpan = 2;
		inputGD.heightHint = 3 * 20; // примерно 3 строки
		methodInput.setLayoutData(inputGD);
		methodInput.setToolTipText("Edit method arguments here");

		// ====== Invoke button under Arguments ======
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

		classCombo.addListener(SWT.Selection, e -> updateMethods());
		selectBtn.addListener(SWT.Selection, e -> onSelectMethod());
		invokeBtn.addListener(SWT.Selection, e -> onInvokeMethod());
	}

	public Composite getControl() {
		return root;
	}

	public void updateFromEvent(SimpleDebugEventDTO dto) {
	    classCombo.removeAll();

	    for (TargetApplicationElementRepresentation el : dto.getTargetApplicationElementRepresentationList()) {
	        if (el instanceof TargetApplicationClassOrInterfaceRepresentation clazz) {
	            String nameAndType = clazz.getTargetApplicationElementName() + " ("
	                    + el.getTargetApplicationElementType() + ")";
	            classCombo.add(nameAndType);
	            classCombo.setData(nameAndType, clazz);
	        }
	    }

	    if (classCombo.getItemCount() > 0) {
	        classCombo.select(0);
	        updateMethods();
	    }

	    // ====== отображаем результат метода, если есть ======
	    if (dto.getResultOfMethodInvocation() != null) {
	        resultField.setText(dto.getResultOfMethodInvocation());
	    } else {
	        resultField.setText(""); // очищаем поле, если результата нет
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

	        // Формируем строку вида:
	        // methodName(type1 name1, type2 name2) : returnType

	        String params = m.getParameters().stream()
	                .map(p -> p.getType().name() + " " + p.getName())
	                .reduce((a, b) -> a + ", " + b)
	                .orElse("");

	        String display = m.getMethodName() + "(" + params + ") : " + m.getReturnType();

	        methodCombo.add(display);
	        methodCombo.setData(display, m);
	    }

	    if (methodCombo.getItemCount() > 0)
	        methodCombo.select(0);
	}


	private void onSelectMethod() {
	    TargetApplicationMethodDTO selectedMethod = getSelectedMethod();
	    if (selectedMethod == null) return;

	    StringBuilder sb = new StringBuilder();
	    sb.append(selectedMethod.getMethodName()).append("(");

	    List<TargetApplicationMethodParameterDTO> params = selectedMethod.getParameters();
	    for (int i = 0; i < params.size(); i++) {
	        TargetApplicationMethodParameterDTO p = params.get(i);
	        sb.append(p.getName()).append(": ").append(p.getType().name());
	        if (i < params.size() - 1) sb.append(", ");
	    }

	    sb.append(")");

	    methodInput.setText(sb.toString());

	    // курсор ставим в первый аргумент
	    int cursorPos = selectedMethod.getMethodName().length() + 1;
	    methodInput.setSelection(cursorPos);
	    methodInput.setFocus();
	}

	private void onInvokeMethod() {
	    TargetApplicationMethodDTO method = getSelectedMethod();
	    TargetApplicationClassOrInterfaceRepresentation clazz = getSelectedClass();
	    String argsText = methodInput.getText();

	    if (method != null && clazz != null) {
	    	InvokeMethodEvent invokeMethodEvent = new InvokeMethodEvent(clazz, method, argsText);
	        // Отправляем событие через UiEventCollector
	        uiEventCollector.collectUiEvent(invokeMethodEvent);
	    }
	}


	public TargetApplicationClassOrInterfaceRepresentation getSelectedClass() {
		return (TargetApplicationClassOrInterfaceRepresentation) classCombo.getData(classCombo.getText());
	}

	public TargetApplicationMethodDTO getSelectedMethod() {
		return (TargetApplicationMethodDTO) methodCombo.getData(methodCombo.getText());
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
