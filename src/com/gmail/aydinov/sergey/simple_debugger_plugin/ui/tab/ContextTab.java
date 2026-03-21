package com.gmail.aydinov.sergey.simple_debugger_plugin.ui.tab;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ILazyContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.PairDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.DebugWindowDataDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.tooltip_manager.TooltipManager;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.utils.UiUtils;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.window.SimpleDebugerWindowsManager;

public class ContextTab {

    private final Composite root;
    private final TableViewer viewer;
    private final Table table;

    private TooltipManager tooltipManager;

    private List<InnerElementRepresentationDTO> flatList;

    public ContextTab(Composite parent) {
        root = new Composite(parent, SWT.NONE);
        root.setLayout(new GridLayout(1, false));

        table = new Table(root, SWT.BORDER | SWT.FULL_SELECTION | SWT.VIRTUAL);
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        table.setLayoutData(new GridData(GridData.FILL_BOTH));

        viewer = new TableViewer(table);

        setupColumns();
        setupLazyContentProvider();
        setupTooltipManager(); // 🔥 ключевая часть
    }

    // =========================================================
    // TooltipManager
    // =========================================================

    private void setupTooltipManager() {
        tooltipManager = new TooltipManager(table, root);

        tooltipManager.setTooltipProvider(item -> {
            if (item == null) return null;

            Point cursor = table.toControl(Display.getCurrent().getCursorLocation());
            int columnIndex = TooltipManager.getColumnIndexAtPoint(table, cursor.x);

            Object data = item.getData("tooltip_col_" + columnIndex);
            return data instanceof String ? (String) data : null;
        });
    }

    // =========================================================
    // Columns
    // =========================================================

    private void setupColumns() {
        char arrow = '⮡';

        // Name
        createColumn(0, "Name", 300,
                dto -> {
                    String indent = "     ".repeat(dto.getLevel());
                    if (dto.getLevel() > 0)
                        indent += arrow;
                    return indent + dto.getElementName();
                },
                null
        );

        // Type
        createColumn(1, "Type", 200,
                InnerElementRepresentationDTO::getTypeOrReturnType,
                UiUtils::getTypeIcon
        );
    }

    private TableViewerColumn createColumn(int index,
                                           String title,
                                           int width,
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

                if (imageExtractor == null)
                    return null;

                Image img = imageExtractor.apply(dto);
                if (img == null)
                    return null;

                TableItem item = findItem(dto);
                if (item == null)
                    return img;

                // 👉 tooltip как в первой вкладке
                if (index == 1) {
                    PairDTO<Image, String> tooltip =  UiUtils.getTypeTooltip(dto);
                    if (tooltip != null) {
                        item.setData("tooltip_col_" + index, tooltip.getSecond());
                    }
                }

                return img;
            }
        });

        return column;
    }

    // =========================================================
    // Lazy loading
    // =========================================================

    private void setupLazyContentProvider() {
        viewer.setContentProvider(new ILazyContentProvider() {

            @Override
            public void dispose() {}

            @Override
            public void inputChanged(org.eclipse.jface.viewers.Viewer viewer,
                                     Object oldInput,
                                     Object newInput) {

                if (newInput instanceof List<?> list) {
                    flatList = (List<InnerElementRepresentationDTO>) list;
                    ((TableViewer) viewer).setItemCount(flatList.size());
                }
            }

            @Override
            public void updateElement(int index) {
                if (flatList != null && index < flatList.size()) {
                    viewer.replace(flatList.get(index), index);
                }
            }
        });
    }

    // =========================================================
    // Data
    // =========================================================

    public void showElementsFromDebugWindowData(DebugWindowDataDTO dto) {
        if (dto == null || dto.getTopElementsWithSubordinates() == null || dto.getTopElementsWithSubordinates().isEmpty())
            return;

        List<InnerElementRepresentationDTO> result = new ArrayList<>();

        int index = 0;
        for (Map.Entry<InnerElementRepresentationDTO, List<InnerElementRepresentationDTO>> entry :
                dto.getTopElementsWithSubordinates().entrySet()) {

            if (index >= 1) {
                addRecursive(result, entry.getKey(), 0, dto.getTopElementsWithSubordinates());
            }
            index++;
        }

        root.getDisplay().asyncExec(() -> {
            if (!table.isDisposed()) {
                viewer.setInput(result);
            }
        });
    }

    private void addRecursive(List<InnerElementRepresentationDTO> result,
                              InnerElementRepresentationDTO element,
                              int level,
                              Map<InnerElementRepresentationDTO, List<InnerElementRepresentationDTO>> map) {

      //  element.setLevel(level); // 🔥 обязательно

        result.add(element);

        List<InnerElementRepresentationDTO> subs = map.get(element);
        if (subs != null) {
            for (InnerElementRepresentationDTO sub : subs) {
                addRecursive(result, sub, level + 1, map);
            }
        }
    }

    private TableItem findItem(InnerElementRepresentationDTO dto) {
        for (TableItem item : table.getItems()) {
            if (item.getData() == dto) {
                return item;
            }
        }
        return null;
    }

    // =========================================================

    public Composite getControl() {
        return root;
    }
}