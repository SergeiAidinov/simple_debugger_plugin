package com.gmail.aydinov.sergey.simple_debugger_plugin.ui.tab;

import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;

import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.PairDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.inspection.UserObjectPageDTO;

/**
 * Простая вкладка: отображает содержимое объекта в виде Name | Value
 */
public class UserObjectStructureTab {

    private final Composite root;
    private final TableViewer viewer;

    public UserObjectStructureTab(Composite parent) {
        root = new Composite(parent, SWT.NONE);
        root.setLayout(new GridLayout(1, false));

        Table table = new Table(root, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        table.setLayoutData(new GridData(GridData.FILL_BOTH));

        viewer = new TableViewer(table);
        viewer.setContentProvider(ArrayContentProvider.getInstance());

        // Две простые колонки
        createColumn("Name", 200, PairDTO::getFirst);
        createColumn("Value", 400, p -> p.getSecond() != null ? String.valueOf(p.getSecond()) : "null");
    }

    public Composite getControl() {
        return root;
    }

    /**
     * Отображение объекта
     */
    public void showUserObject(UserObjectPageDTO dto) {
        if (dto == null || dto.getEntries() == null) {
            viewer.setInput(List.of());
        } else {
            viewer.setInput(dto.getEntries());
        }
        viewer.refresh();
    }

    /**
     * Универсальное создание колонки для PairDTO
     */
    private TableViewerColumn createColumn(
            String title,
            int width,
            java.util.function.Function<PairDTO<String, Object>, String> extractor
    ) {
        TableViewerColumn column = new TableViewerColumn(viewer, SWT.NONE);
        column.getColumn().setText(title);
        column.getColumn().setWidth(width);

        column.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                if (element instanceof PairDTO<?, ?> pair) {
                    return extractor.apply((PairDTO<String, Object>) pair);
                }
                return "";
            }
        });

        return column;
    }
}