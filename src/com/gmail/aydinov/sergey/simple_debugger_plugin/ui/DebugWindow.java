package com.gmail.aydinov.sergey.simple_debugger_plugin.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.sun.jdi.Location;

public class DebugWindow {

    private Shell shell;
    private CTabFolder tabFolder;
    
    

    public DebugWindow() {
    	shell = new Shell();
        shell.setText("Simple Debugger");
        shell.setSize(800, 600);
        shell.setLayout(new FillLayout());

        tabFolder = new CTabFolder(shell, SWT.BORDER);
        tabFolder.setSimple(false);

        createTabs();
	}

	public DebugWindow(Display display) {
        shell = new Shell(display);
        shell.setText("Simple Debugger");
        shell.setSize(800, 600);
        shell.setLayout(new FillLayout());

        tabFolder = new CTabFolder(shell, SWT.BORDER);
        tabFolder.setSimple(false);

        createTabs();
    }

    private void createTabs() {

        // Variables Tab
        VariablesTabContent variables = new VariablesTabContent(tabFolder);
        CTabItem variablesTab = new CTabItem(tabFolder, SWT.NONE);
        variablesTab.setText("Variables");
        variablesTab.setControl(variables.getControl());

        // Fields Tab
        FieldsTabContent fields = new FieldsTabContent(tabFolder);
        CTabItem fieldsTab = new CTabItem(tabFolder, SWT.NONE);
        fieldsTab.setText("Fields");
        fieldsTab.setControl(fields.getControl());

        // Stack Tab
        StackTabContent stack = new StackTabContent(tabFolder);
        CTabItem stackTab = new CTabItem(tabFolder, SWT.NONE);
        stackTab.setText("Stack");
        stackTab.setControl(stack.getControl());

        // Evaluate Tab
        EvaluateTabContent eval = new EvaluateTabContent(tabFolder);
        CTabItem evalTab = new CTabItem(tabFolder, SWT.NONE);
        evalTab.setText("Evaluate");
        evalTab.setControl(eval.getControl());

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

	public void updateLocation(Location location) {
		// TODO Auto-generated method stub
		
	}
    
    

}
