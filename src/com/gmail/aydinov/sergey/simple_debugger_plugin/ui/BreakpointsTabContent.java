package com.gmail.aydinov.sergey.simple_debugger_plugin.ui;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;

import com.sun.jdi.Location;

public class BreakpointsTabContent extends Composite {

    private TableViewer viewer;

    public BreakpointsTabContent(Composite parent) {
        super(parent, SWT.NONE);
        setLayout(new FillLayout());

        viewer = new TableViewer(this, SWT.FULL_SELECTION | SWT.BORDER);
        viewer.getTable().setHeaderVisible(true);
        viewer.getTable().setLinesVisible(true);

        TableViewerColumn file = new TableViewerColumn(viewer, SWT.NONE);
        file.getColumn().setText("File");
        file.getColumn().setWidth(250);

        TableViewerColumn line = new TableViewerColumn(viewer, SWT.NONE);
        line.getColumn().setText("Line");
        line.getColumn().setWidth(70);
    }

    public void update(Location loc) {
        viewer.setInput(new Location[] { loc });
    }
}

