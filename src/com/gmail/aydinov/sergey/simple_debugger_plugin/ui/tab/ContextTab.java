package com.gmail.aydinov.sergey.simple_debugger_plugin.ui.tab;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;

import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.window.SimpleDebugerWindowsManager;

/**
 * Вкладка отображения контекста — всех элементов, не относящихся к классу на брейкпойнте.
 * Только две колонки: Name и Type.
 */
public class ContextTab {

    private final Composite root;
    private final TableViewer viewer;

    public ContextTab(Composite parent) {
        root = new Composite(parent, SWT.NONE);
        root.setLayout(new GridLayout(1, false));

        Table table = new Table(root, SWT.BORDER | SWT.FULL_SELECTION);
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        table.setLayoutData(new GridData(GridData.FILL_BOTH));

        viewer = new TableViewer(table);
        viewer.setContentProvider(ArrayContentProvider.getInstance());

        setupColumns();
    }

    private void setupColumns() {
        // 0: Name
        createColumn("Name", 300, InnerElementRepresentationDTO::getElementName, null);
        // 1: Type
        createColumn("Type", 200, InnerElementRepresentationDTO::getTypeOrReturnType, this::getTypeIcon);
    }

    private TableViewerColumn createColumn(String title, int width,
                                           Function<InnerElementRepresentationDTO, String> textExtractor,
                                           Function<InnerElementRepresentationDTO, Image> imageExtractor) {
        TableViewerColumn column = new TableViewerColumn(viewer, SWT.NONE);
        column.getColumn().setText(title);
        column.getColumn().setWidth(width);
        column.getColumn().setResizable(true);

        column.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                if (element instanceof InnerElementRepresentationDTO dto) {
                    String value = textExtractor.apply(dto);
                    return value != null ? value : "";
                }
                return "";
            }

            @Override
            public Image getImage(Object element) {
                if (!(element instanceof InnerElementRepresentationDTO dto))
                    return null;
                if (imageExtractor != null)
                    return imageExtractor.apply(dto);
                return null;
            }
        });

        return column;
    }

    private Image getTypeIcon(InnerElementRepresentationDTO dto) {
        if (dto == null)
            return null;

        switch (dto.getElementType()) {
            case METHOD -> {
                return dto.isStatic()
                        ? SimpleDebugerWindowsManager.instance().icons.get("static_method").getFirst()
                        : SimpleDebugerWindowsManager.instance().icons.get("method").getFirst();
            }
            case FIELD -> {
                return dto.isStatic()
                        ? SimpleDebugerWindowsManager.instance().icons.get("static_field").getFirst()
                        : SimpleDebugerWindowsManager.instance().icons.get("fieldIcon").getFirst();
            }
            case LOCAL_VARIABLE -> {
                return SimpleDebugerWindowsManager.instance().icons.get("variableIcon").getFirst();
            }
            default -> {
                return null;
            }
        }
    }
    
    public void showElementsFromSecondEntry(Map<InnerElementRepresentationDTO,List<InnerElementRepresentationDTO>> map) {
        if (map == null || map.size() < 2) return;

        List<InnerElementRepresentationDTO> ordered = new ArrayList<>();

        int index = 0;
        for (Map.Entry<InnerElementRepresentationDTO, List<InnerElementRepresentationDTO>> entry : map.entrySet()) {
            if (index == 1) { // вторая запись
                // добавляем ключ
                ordered.add(entry.getKey());
                // добавляем подчинённых
                ordered.addAll(entry.getValue());
                break;
            }
            index++;
        }

        root.getDisplay().asyncExec(() -> {
            if (!viewer.getTable().isDisposed()) {
                viewer.setInput(ordered);
                viewer.refresh();
            }
        });
    }

    public void showElements(List<InnerElementRepresentationDTO> elements) {
        if (elements == null) return;

        // Обновляем таблицу в UI-потоке
        root.getDisplay().asyncExec(() -> {
            if (!viewer.getTable().isDisposed()) {
                viewer.setInput(new ArrayList<>(elements)); // создаем копию
                viewer.refresh();
            }
        });
    }

    public Composite getControl() {
        return root;
    }
}