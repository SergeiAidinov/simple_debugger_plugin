package com.gmail.aydinov.sergey.simple_debugger_plugin.view;

import java.util.Set;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.part.ViewPart;

import com.gmail.aydinov.sergey.simple_debugger_plugin.core.TargetApplicationBreakepointRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.BreakpointHitListener;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.BreakpointWrapper;

public class BreakpointsView extends ViewPart {

    public static final String ID = "com.gmail.aydinov.sergey.simple_debugger_plugin.view.breakpointsView";

    private TableViewer viewer;
    private TargetApplicationBreakepointRepresentation breakpointRepresentation;

    @Override
    public void createPartControl(Composite parent) {
        viewer = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);
        viewer.setContentProvider(ArrayContentProvider.getInstance());

        // Столбец файла
        TableViewerColumn fileColumn = new TableViewerColumn(viewer, SWT.NONE);
        fileColumn.getColumn().setText("File");
        fileColumn.getColumn().setWidth(250);
        fileColumn.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return ((BreakpointWrapper) element).get().getMarker().getResource().getName();
            }
        });

        // Столбец строки
        TableViewerColumn lineColumn = new TableViewerColumn(viewer, SWT.NONE);
        lineColumn.getColumn().setText("Line");
        lineColumn.getColumn().setWidth(50);
        lineColumn.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return String.valueOf(((BreakpointWrapper) element).get().getMarker().getAttribute(IMarker.LINE_NUMBER, -1));
            }
        });

        viewer.getTable().setHeaderVisible(true);
        viewer.getTable().setLinesVisible(true);
    }

    @Override
    public void setFocus() {
        viewer.getControl().setFocus();
    }

    /**
     * Связать view с моделью и подписаться на события добавления брейкпойнтов
     */
    public void setModel(TargetApplicationBreakepointRepresentation model) {
        this.breakpointRepresentation = model;

        // Подписка на новые брейкпойнты
        breakpointRepresentation.addListener(new BreakpointHitListener() {
            @Override
            public void onBreakpointHit(BreakpointWrapper wrapper) {
                Display.getDefault().asyncExec(() -> {
                    if (viewer != null && !viewer.getControl().isDisposed()) {
                        refreshViewer();
                    }
                });
            }
        });

        // Обновляем таблицу сразу, если viewer уже создан
        if (viewer != null && !viewer.getControl().isDisposed()) {
            refreshViewer();
        }
    }

    private void refreshViewer() {
        if (viewer != null && !viewer.getControl().isDisposed() && breakpointRepresentation != null) {
            Set<BreakpointWrapper> bps = breakpointRepresentation.getBreakpoints();

            // Отладочный вывод
            System.out.println("Refreshing viewer, breakpoints count: " + bps.size());
            for (BreakpointWrapper bp : bps) {
                System.out.println("BP: " + bp.get().getMarker().getResource().getName()
                                   + ":" + bp.get().getMarker().getAttribute(IMarker.LINE_NUMBER, -1));
            }

            viewer.setInput(bps);
            viewer.refresh();
        }
    }
}
