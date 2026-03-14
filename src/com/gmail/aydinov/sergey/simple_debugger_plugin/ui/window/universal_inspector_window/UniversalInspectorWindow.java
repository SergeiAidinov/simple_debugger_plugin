package com.gmail.aydinov.sergey.simple_debugger_plugin.ui.window.universal_inspector_window;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;

import com.gmail.aydinov.sergey.simple_debugger_plugin.event.debug_event.AbstractDebugEvent;

public class UniversalInspectorWindow {

    private static UniversalInspectorWindow INSTANCE;

    private Shell shell;

    private List navigationList;

    private CTabFolder tabFolder;

    private UniversalInspectorWindow() {}

    public static UniversalInspectorWindow getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new UniversalInspectorWindow();
        }
        return INSTANCE;
    }

    public void open() {

        Display display = Display.getDefault();

        if (shell != null && !shell.isDisposed()) {
            shell.forceActive();
            return;
        }

        shell = new Shell(display);
        shell.setText("Universal Object Inspector");
        shell.setSize(900, 600);
        shell.setLayout(new FillLayout());

        createContent(shell);

        shell.open();
    }

    private void createContent(Composite parent) {

        SashForm sash = new SashForm(parent, SWT.HORIZONTAL);

        createNavigationPanel(sash);
        createDetailPanel(sash);

        sash.setWeights(new int[] {20, 80});

        createTestTab();
    }

    private void createNavigationPanel(Composite parent) {

        Composite navigation = new Composite(parent, SWT.NONE);
        navigation.setLayout(new FillLayout());

        navigationList = new List(
                navigation,
                SWT.BORDER | SWT.V_SCROLL
        );

        navigationList.add("Navigation history will appear here");
    }

    private void createDetailPanel(Composite parent) {

        Composite details = new Composite(parent, SWT.NONE);
        details.setLayout(new FillLayout());

        tabFolder = new CTabFolder(details, SWT.BORDER);
    }

    private void createTestTab() {

        CTabItem tab = new CTabItem(tabFolder, SWT.NONE);
        tab.setText("Test");

        Composite content = new Composite(tabFolder, SWT.NONE);
        content.setLayout(new FillLayout());

        Label label = new Label(content, SWT.NONE);
        label.setText("Inspector window is working");

        tab.setControl(content);

        tabFolder.setSelection(tab);
    }

    public void close() {
        if (shell != null && !shell.isDisposed()) {
            Display.getDefault().asyncExec(() -> shell.close());
        }
        INSTANCE = null;
    }

	public void handleDebugEvent(AbstractDebugEvent event) {
		// TODO Auto-generated method stub
		
	}

}
