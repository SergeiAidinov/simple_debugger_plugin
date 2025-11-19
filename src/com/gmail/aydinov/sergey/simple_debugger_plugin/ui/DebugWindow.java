package com.gmail.aydinov.sergey.simple_debugger_plugin.ui;

import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.VariablesTabContent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.FieldsTabContent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.StackTabContent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.EvaluateTabContent;
import com.sun.jdi.Field;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.Location;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Value;

public class DebugWindow {

    private Shell shell;
    private CTabFolder tabFolder;

    // Поля вкладок
    private VariablesTabContent variablesTab;
    private FieldsTabContent fieldsTab;
    private StackTabContent stackTab;
    private EvaluateTabContent evalTab;

    public DebugWindow() {
        Display display = Display.getDefault();
        shell = new Shell(display);
        shell.setText("Simple Debugger");
        shell.setSize(800, 600);
        shell.setLayout(new FillLayout());

        tabFolder = new CTabFolder(shell, SWT.BORDER);
        tabFolder.setSimple(false);

        createTabs();
    }

    private void createTabs() {
        // --- Variables Tab ---
        variablesTab = new VariablesTabContent(tabFolder);
        CTabItem variablesItem = new CTabItem(tabFolder, SWT.NONE);
        variablesItem.setText("Variables");
        variablesItem.setControl(variablesTab.getControl());

        // --- Fields Tab ---
        fieldsTab = new FieldsTabContent(tabFolder);
        CTabItem fieldsItem = new CTabItem(tabFolder, SWT.NONE);
        fieldsItem.setText("Fields");
        fieldsItem.setControl(fieldsTab.getControl());

        // --- Stack Tab ---
        stackTab = new StackTabContent(tabFolder);
        CTabItem stackItem = new CTabItem(tabFolder, SWT.NONE);
        stackItem.setText("Stack");
        stackItem.setControl(stackTab.getControl());

        // --- Evaluate Tab ---
        evalTab = new EvaluateTabContent(tabFolder);
        CTabItem evalItem = new CTabItem(tabFolder, SWT.NONE);
        evalItem.setText("Evaluate");
        evalItem.setControl(evalTab.getControl());

        tabFolder.setSelection(0);
    }

    public void open() {
        shell.open();
    }

    public boolean isOpen() {
        return !shell.isDisposed();
    }

    public Shell getShell() {
        return shell;
    }

    /**
     * Обновление вкладок при остановке на брейкпойнте.
     * Вызывается из DebugWindowManager, безопасно через UI-поток.
     */
    public void updateLocation(Location location, ThreadReference thread) {
        Display.getDefault().asyncExec(() -> {
            try {
                if (shell.isDisposed()) return;

                // Верхняя рамка стека
                StackFrame frame = thread.frame(0);

                // Локальные переменные
                List<LocalVariable> vars = frame.visibleVariables();
                Map<LocalVariable, Value> values = frame.getValues(vars);
                variablesTab.updateVariables(values);

                // Поля объекта
                ObjectReference thisObject = frame.thisObject();
                if (thisObject != null) {
                    Map<Field, Value> fields = thisObject.getValues(thisObject.referenceType().fields());
                    fieldsTab.updateFields(fields);
                }

                // Стек вызовов
                List<StackFrame> frames = thread.frames();
                stackTab.updateStack(frames);

                // Evaluate вкладка — остаётся для пользовательского ввода

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
