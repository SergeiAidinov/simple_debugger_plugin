package com.gmail.aydinov.sergey.simple_debugger_plugin.ui.tab;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
import org.eclipse.swt.widgets.TableItem;

import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.PairDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.inspection.UserObjectInspectionDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.utils.UiUtils;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.window.SimpleDebugerWindowsManager;

/**
 * Вкладка для отображения структуры класса: поля, методы, дочерние объекты.
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

        // Создаем колонки: Name, Type / Return, Value / Info
        createColumn("Name", 200, InnerElementRepresentationDTO::getElementName, e -> null);
        createColumn("Type / Return", 150, InnerElementRepresentationDTO::getTypeOrReturnType, UiUtils::getTypeIcon);
        createColumn("Value / Info", 250, dto -> dto.getValue() != null ? dto.getValue() : "", this::getIcon);
    }

    public Composite getControl() {
        return root;
    }

    public void showUserObject(UserObjectInspectionDTO dto) {
        if (dto == null) return;

        List<InnerElementRepresentationDTO> elements = new ArrayList<>();
        if (dto.getInstanceFields() != null) elements.addAll(dto.getInstanceFields());
        if (dto.getInstanceMethods() != null) elements.addAll(dto.getInstanceMethods());

        // Можно добавить рекурсивное раскрытие дочерних объектов, если нужно
        viewer.setInput(elements);
        viewer.refresh();
    }

    private TableViewerColumn createColumn(String title, int width,
                                           java.util.function.Function<InnerElementRepresentationDTO, String> textExtractor,
                                           java.util.function.Function<InnerElementRepresentationDTO, Image> imageExtractor) {
        TableViewerColumn column = new TableViewerColumn(viewer, SWT.NONE);
        column.getColumn().setText(title);
        column.getColumn().setWidth(width);
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
                if (!(element instanceof InnerElementRepresentationDTO dto)) return null;
                TableItem item = findTableItem(dto);
                if (item == null) return imageExtractor.apply(dto);

                Image img = imageExtractor.apply(dto);
                if (img != null) {
                    // Добавляем иконку "inspect" для объектов пользователя
                    item.setData("tooltip", SimpleDebugerWindowsManager.instance().icons.get("inspectIcon").getSecond());
                }
                return img;
            }
        });
        return column;
    }

    private TableItem findTableItem(InnerElementRepresentationDTO dto) {
        for (TableItem item : viewer.getTable().getItems()) {
            if (item.getData() == dto) return item;
        }
        return null;
    }

    private Image getIcon(InnerElementRepresentationDTO dto) {
        if (dto == null) return null;

        switch (dto.getValueCategory()) {
            case COLLECTION, MAP -> {
                return SimpleDebugerWindowsManager.instance().icons.get("lens").getFirst();
            }
            case USER_OBJECT -> {
                if (dto.getValue() != null && !UiUtils.isStandartJavaType(dto.getTypeOrReturnType())) {
                    return SimpleDebugerWindowsManager.instance().icons.get("inspectIcon").getFirst();
                }
            }
            
            default -> {}
        }
        
        return null;
    }
}