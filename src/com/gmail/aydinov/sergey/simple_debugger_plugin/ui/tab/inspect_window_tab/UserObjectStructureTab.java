package com.gmail.aydinov.sergey.simple_debugger_plugin.ui.tab.inspect_window_tab;

import java.util.List;
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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.ValueCategory;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.inspection.UserObjectPageDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.utils.UiUtils;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.window.SimpleDebugerWindowsManager;

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
        objectNameLabel.setText("Object: ");

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

        setupColumns();
    }

    public Composite getControl() {
        return root;
    }

    // =========================================================
    // Display
    // =========================================================

    public void showUserObject(UserObjectPageDTO dto) {
        if (dto == null) {
            viewer.setInput(List.of());
            return;
        }

        // 🔹 Header
        objectNameLabel.setText("Object: " + safe(dto.getElementName()));
        classTypeLabel.setText("Class: " + safe(dto.getClassType()));
        elementTypeLabel.setText("Type: " + safe(dto.getElementType()));

        // 🔹 Table
        viewer.setInput(dto.getEntries() != null ? dto.getEntries() : List.of());
        viewer.refresh();

        root.layout(true, true);
    }

    // =========================================================
    // Columns
    // =========================================================

    private void setupColumns() {

        // Name
        createColumn(
                "Name",
                200,
                InnerElementRepresentationDTO::getElementName,
                dto -> null
        );

        // Type / Return
        createColumn(
                "Type / Return",
                200,
                InnerElementRepresentationDTO::getTypeOrReturnType,
                UiUtils::getTypeIcon
        );

        // Value / Info
        createColumn(
                "Value / Info",
                400,
                dto -> dto.getValue() != null ? dto.getValue() : "",
                this::getIcon
        );
    }

    private TableViewerColumn createColumn(
            String title,
            int width,
            Function<InnerElementRepresentationDTO, String> textExtractor,
            Function<InnerElementRepresentationDTO, Image> imageExtractor
    ) {
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
                if (element instanceof InnerElementRepresentationDTO dto) {
                    return imageExtractor != null ? imageExtractor.apply(dto) : null;
                }
                return null;
            }
        });

        return column;
    }

    // =========================================================
    // Icons logic
    // =========================================================

    private Image getIcon(InnerElementRepresentationDTO dto) {
        if (dto == null) return null;

        ValueCategory category = dto.getValueCategory();
        if (category == null) return null;

        // 📦 Коллекции / Map
        if (category == ValueCategory.COLLECTION || category == ValueCategory.MAP) {
            return SimpleDebugerWindowsManager.instance().icons.get("lens").getFirst();
        }

        // 👤 Пользовательский объект
        if (category == ValueCategory.USER_OBJECT && dto.getValue() != null) {
            return SimpleDebugerWindowsManager.instance().icons.get("inspectIcon").getFirst();
        }

        return null;
    }

    // =========================================================
    // Utils
    // =========================================================

    private String safe(String value) {
        return value == null ? "" : value;
    }
}