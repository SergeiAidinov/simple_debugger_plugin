package com.gmail.aydinov.sergey.simple_debugger_plugin.ui.tab.inspect_window_tab;

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

import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.inspection.UserObjectPageDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;

/**
 * Простая и стабильная вкладка инспектора объекта.
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

        createColumn("Name", 250, InnerElementRepresentationDTO::getElementName);

        createColumn("Value", 500, dto ->
                dto.getValue() != null ? dto.getValue() : "null"
        );
    }

    public Composite getControl() {
        return root;
    }

    /**
     * Отображение объекта
     */
    public void showUserObject(UserObjectPageDTO dto) {
        List<InnerElementRepresentationDTO> entries =
                (dto == null || dto.getEntries() == null)
                        ? List.of()
                        : dto.getEntries();

        viewer.setInput(entries);
        viewer.refresh();
    }

    /**
     * Универсальное создание колонки
     */
    private TableViewerColumn createColumn(
            String title,
            int width,
            java.util.function.Function<InnerElementRepresentationDTO, String> extractor
    ) {
        TableViewerColumn column = new TableViewerColumn(viewer, SWT.NONE);
        column.getColumn().setText(title);
        column.getColumn().setWidth(width);
        column.getColumn().setResizable(true);

        column.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                if (element instanceof InnerElementRepresentationDTO dto) {
                    String value = extractor.apply(dto);
                    return value != null ? value : "";
                }
                return "";
            }
        });

        return column;
    }
}