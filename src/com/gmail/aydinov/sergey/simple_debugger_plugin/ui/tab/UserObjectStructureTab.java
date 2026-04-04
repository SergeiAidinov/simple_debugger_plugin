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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;

import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.inspection.UserObjectPageDTO;

/**
 * Вкладка: отображает структуру объекта (поля/методы)
 */
public class UserObjectStructureTab {

    private final Composite root;
    private final TableViewer viewer;

    // 🔹 Header labels
    private final Label objectNameLabel;
    private final Label classTypeLabel;
    private final Label elementTypeLabel;

    public UserObjectStructureTab(Composite parent) {
        root = new Composite(parent, SWT.NONE);
        root.setLayout(new GridLayout(1, false));

        // ===== Header =====
        Composite header = new Composite(root, SWT.NONE);
        header.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        header.setLayout(new GridLayout(1, false));

        objectNameLabel = new Label(header, SWT.NONE);
        objectNameLabel.setText("Field: ");

        classTypeLabel = new Label(header, SWT.NONE);
        classTypeLabel.setText("Class: ");

        elementTypeLabel = new Label(header, SWT.NONE);
        elementTypeLabel.setText("Type: ");

        // ===== Table =====
        Table table = new Table(root, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        table.setLayoutData(new GridData(GridData.FILL_BOTH));

        viewer = new TableViewer(table);
        viewer.setContentProvider(ArrayContentProvider.getInstance());

        createColumn("Name", 200, InnerElementRepresentationDTO::getElementName);
        createColumn("Type / Return", 200, InnerElementRepresentationDTO::getTypeOrReturnType);
        createColumn("Value / Info", 400, dto -> dto.getValue() != null ? dto.getValue() : "");
    }

    public Composite getControl() {
        return root;
    }

    /**
     * Отображение объекта
     */
    public void showUserObject(UserObjectPageDTO dto) {
        if (dto == null) {
            viewer.setInput(List.of());
            return;
        }

        // 🔹 Обновляем header
        objectNameLabel.setText("Object: " + safe(dto.getElementName()));
        classTypeLabel.setText("Class: " + safe(dto.getClassType()));
        elementTypeLabel.setText("Type: " + safe(dto.getElementType()));

        // 🔹 Обновляем таблицу
        viewer.setInput(dto.getEntries() != null ? dto.getEntries() : List.of());
        viewer.refresh();

        root.layout(true, true);
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

    private String safe(String value) {
        return value == null ? "" : value;
    }
}