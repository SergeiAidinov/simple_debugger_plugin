package com.gmail.aydinov.sergey.simple_debugger_plugin.ui;

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.CollectionEntryDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.tab.CollectionInspectorTab;

public class CollectionInspectorWindow {

    private final Shell shell;
    private final CollectionInspectorTab inspectorTab;

    public CollectionInspectorWindow(Display display, List<CollectionEntryDTO> elements) {
        shell = new Shell(display, SWT.SHELL_TRIM | SWT.APPLICATION_MODAL);
        shell.setText("Collection Inspector");
        shell.setSize(600, 400);
        shell.setLayout(new GridLayout(1, false));

        inspectorTab = new CollectionInspectorTab(shell);
        inspectorTab.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        inspectorTab.showCollection(elements);

        shell.open();
    }

    public void open() {
        shell.open();
    }

    public boolean isOpen() {
        return !shell.isDisposed();
    }

    public void close() {
        if (!shell.isDisposed()) shell.close();
    }
}