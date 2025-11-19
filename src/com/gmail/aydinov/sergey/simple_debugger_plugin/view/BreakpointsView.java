package com.gmail.aydinov.sergey.simple_debugger_plugin.view;

import java.util.Collection;
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
import com.sun.jdi.Location;
import com.sun.jdi.ThreadReference;

public class BreakpointsView extends ViewPart {

    public static final String ID = "com.gmail.aydinov.sergey.simple_debugger_plugin.view.breakpointsView";

    private TableViewer viewer;
    private BreakepintViewController breakepintViewController;

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

                if (element instanceof Location loc) {
                    try {
                        return loc.sourceName();
                    } catch (Exception e) {
                        return "<no source>";
                    }
                }

                return "<unknown>";
            }
        });


        // Столбец строки
        TableViewerColumn lineColumn = new TableViewerColumn(viewer, SWT.NONE);
        lineColumn.getColumn().setText("Line");
        lineColumn.getColumn().setWidth(50);
        lineColumn.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {

                if (element instanceof Location loc) {
                    try {
                        return String.valueOf(loc.lineNumber());
                    } catch (Exception e) {
                        return "-";
                    }
                }

                return "-";
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
    public void setController(BreakepintViewController breakepintViewController) {
        this.breakepintViewController = breakepintViewController;

        // Подписываем view как слушателя
        // breakepintViewController.setBreakpointHitListener(this);

        // При первом подключении сразу обновляем таблицу
        bulkRefreshViewer(BreakepintViewController.instance().getAllLocations());
    }

        // Подписка на новые брейкпойнты
//        breakpointRepresentation.addListener(new BreakpointHitListener() {
//            @Override
//            public void onBreakpointHit(BreakpointWrapper wrapper) {
//                Display.getDefault().asyncExec(() -> {
//                    if (viewer != null && !viewer.getControl().isDisposed()) {
//                        refreshViewer();
//                    }
//                });
//            }
//        });

        // Обновляем таблицу сразу, если viewer уже создан
//        if (viewer != null && !viewer.getControl().isDisposed()) {
//            refreshViewer();
//        }
//    }

    private void bulkRefreshViewer(Collection<Location> locs) {
    	    if (viewer == null || viewer.getControl().isDisposed()) {
    	        return;
    	    }

    	    if (locs == null || locs.isEmpty()) {
    	        viewer.setInput(new Location[0]);
    	        viewer.refresh();
    	        return;
    	    }

    	    // Преобразуем коллекцию в массив для TableViewer
    	    Location[] locArray = locs.toArray(new Location[0]);
    	    viewer.setInput(locArray);
    	    viewer.refresh();

    	    // Отладочный вывод
    	    System.out.println("Viewer refreshed. Total locations: " + locArray.length);
    	    for (Location l : locArray) {
    	        try {
    	            System.out.println(" -> " + l.declaringType().name() + ":" + l.lineNumber());
    	        } catch (Exception e) {
    	            System.out.println(" -> <unknown location>");
    	        }
    	    }
    	}

	private void refreshViewer(Location loc) {
//        if (viewer != null && !viewer.getControl().isDisposed()) {
//
//            // Временный input — массив из одного Location
//            viewer.setInput(new Location[] { loc });
//            viewer.refresh();
//
//            System.out.println("Viewer refreshed with location: " + loc);
//        }
		bulkRefreshViewer(BreakepintViewController.instance().getAllLocations());
    }


//		@Override
//		public void onBreakpointHit(Location location) {
//			Display.getDefault().asyncExec(() -> {
//              if (viewer != null && !viewer.getControl().isDisposed()) {
//                  refreshViewer(location);
//              }
//          });
//      }
//
//		@Override
//		public void onBreakpointHit(Location location, ThreadReference thread) {
//			// TODO Auto-generated method stub
//			
//		}
			
}
