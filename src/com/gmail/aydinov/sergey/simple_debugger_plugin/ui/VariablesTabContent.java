package com.gmail.aydinov.sergey.simple_debugger_plugin.ui;

import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import com.sun.jdi.BooleanValue;
import com.sun.jdi.ByteValue;
import com.sun.jdi.CharValue;
import com.sun.jdi.DoubleValue;
import com.sun.jdi.FloatValue;
import com.sun.jdi.IntegerValue;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.LongValue;
import com.sun.jdi.ShortValue;
import com.sun.jdi.StackFrame;
import com.sun.jdi.StringReference;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;

public class VariablesTabContent {

	//private final UiEventProvider uiEventProvider;
    private final Table table;
    private StackFrame currentStackFrame;

	public VariablesTabContent(Composite parent) {
        //this.uiEventProvider = uiEventProvider;
		table = new Table(parent, SWT.BORDER | SWT.FULL_SELECTION);
        table.setHeaderVisible(true);
        table.setLinesVisible(true);

        TableColumn colName = new TableColumn(table, SWT.LEFT);
        colName.setText("Name");
        colName.setWidth(200);

        TableColumn colType = new TableColumn(table, SWT.LEFT);
        colType.setText("Type");
        colType.setWidth(200);

        TableColumn colValue = new TableColumn(table, SWT.LEFT);
        colValue.setText("Value");
        colValue.setWidth(200);
    }

    public Table getControl() {
        return table;
    }

    /**
     * Обновляет таблицу переменных и сохраняет текущий стек-фрейм
     * @param stackFrame 
     */
    public void updateVariables(StackFrame stackFrame, Map<LocalVariable, Value> vars) {
        table.removeAll();
        //this.currentStackFrame = frame;
        
       // LocalVariable localVariable;
        //String newValue;

        for (Map.Entry<LocalVariable, Value> entry : vars.entrySet()) {
            LocalVariable localVar = entry.getKey();
            Value val = entry.getValue();

            TableItem item = new TableItem(table, SWT.NONE);
            item.setText(0, localVar.name());
            item.setText(1, localVar.typeName());
            item.setText(2, valueToString(val));

            TableEditor editor = new TableEditor(table);
            editor.grabHorizontal = true;
            Text valueText = new Text(table, SWT.NONE);
            valueText.setText(valueToString(val));
            editor.setEditor(valueText, item, 2);

            valueText.addListener(SWT.FocusOut, e -> {
                String newValue = valueText.getText();
                try {
                    setVariableValue(stackFrame, localVar, newValue); // обновляем через JDI
                    item.setText(2, newValue);

                    // 🔹 создаём событие только после изменения
                    //UIEventUpdateVariable event = new UIEventUpdateVariable(localVar, newValue);
                   // sendUiEvent(event);

                } catch (Exception ex) {
                    ex.printStackTrace();
                    valueText.setText(valueToString(val)); // откат при ошибке
                }
            });
        }
    }

    private String valueToString(Value v) {
        if (v == null) return "null";
        try {
            return v.toString();
        } catch (Exception e) {
            return "<error>";
        }
    }

    /**
     * Изменяет значение локальной переменной через JDI
     */
    private void setVariableValue(StackFrame stackFrame, LocalVariable var, String newValue) throws Exception {
        if (currentStackFrame == null) return;
        VirtualMachine vm = currentStackFrame.virtualMachine();
        Value v = createJdiValueFromString(vm, var, newValue);
        currentStackFrame.setValue(var, v);
        //uiEventProvider.sendUiEvent(new UIEventUpdateVariable(stackFrame, var, newValue));
        
    }
    
    private Value createJdiValueFromString(VirtualMachine vm, LocalVariable var, String str) {
		String type = var.typeName();
		switch (type) {
		case "int":
			return vm.mirrorOf(Integer.parseInt(str));
		case "long":
			return vm.mirrorOf(Long.parseLong(str));
		case "short":
			return vm.mirrorOf(Short.parseShort(str));
		case "byte":
			return vm.mirrorOf(Byte.parseByte(str));
		case "char":
			return vm.mirrorOf(str.charAt(0));
		case "boolean":
			return vm.mirrorOf(Boolean.parseBoolean(str));
		case "float":
			return vm.mirrorOf(Float.parseFloat(str));
		case "double":
			return vm.mirrorOf(Double.parseDouble(str));
		case "java.lang.String":
			return vm.mirrorOf(str);
		default:
			throw new IllegalArgumentException("Unsupported type: " + type);
		}
	}

    
}