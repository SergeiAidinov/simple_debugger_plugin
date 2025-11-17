package com.gmail.aydinov.sergey.simple_debugger_plugin.view;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

import com.gmail.aydinov.sergey.simple_debugger_plugin.core.TargetApplicationBreakepointRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.BreakpointWrapper;

public class BreakpointsView extends ViewPart {

    public static final String ID =
            "com.gmail.aydinov.sergey.simple_debugger_plugin.view.breakpointsView";

    private TableViewer viewer;
    private TargetApplicationBreakepointRepresentation breakpointRepresentation;

    public void setModel(TargetApplicationBreakepointRepresentation model) {
        this.breakpointRepresentation = model;
        if (viewer != null && !viewer.getControl().isDisposed()) {
            viewer.setInput(breakpointRepresentation.getBreakpoints());
        }
    }

    @Override
    public void createPartControl(Composite parent) {
        viewer = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);
        viewer.setContentProvider(ArrayContentProvider.getInstance());

        TableViewerColumn fileColumn = new TableViewerColumn(viewer, SWT.NONE);
        fileColumn.getColumn().setText("File");
        fileColumn.getColumn().setWidth(200);
        fileColumn.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return ((BreakpointWrapper) element).get().getMarker().getResource().getName();
            }
        });

        TableViewerColumn lineColumn = new TableViewerColumn(viewer, SWT.NONE);
        lineColumn.getColumn().setText("Line");
        lineColumn.getColumn().setWidth(50);
        lineColumn.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return String.valueOf(((BreakpointWrapper) element).get()
                        .getMarker().getAttribute(IMarker.LINE_NUMBER, -1));
            }
        });

        viewer.getTable().setHeaderVisible(true);
        viewer.getTable().setLinesVisible(true);

        if (breakpointRepresentation != null)
            viewer.setInput(breakpointRepresentation.getBreakpoints());
    }

    @Override
    public void setFocus() {
        viewer.getControl().setFocus();
    }
}


