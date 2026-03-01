package com.gmail.aydinov.sergey.simple_debugger_plugin.ui.tab;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.UniversalElementType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.DebugWindowDataDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.InnerElementRepresentationDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.PairDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.DebugWindowsManager;

/**
 * Вкладка для отображения полей, методов и переменных
 * с иконками и подсказками из DebugWindowsManager.
 */
public class ClassMembersAtBreakpoint {

    private final Composite root;
    private final TableViewer viewer;

    private static final Set<String> JAVA_STANDARD_TYPES = Set.of(
            "int", "long", "short", "byte", "float", "double",
            "boolean", "char", "java.lang.Integer", "java.lang.Long",
            "java.lang.Short", "java.lang.Byte", "java.lang.Float",
            "java.lang.Double", "java.lang.Boolean", "java.lang.Character", 
            "java.lang.String"
    );

    public ClassMembersAtBreakpoint(Composite parent) {
        root = new Composite(parent, SWT.NONE);
        root.setLayout(new GridLayout(1, false));

        Table table = new Table(root, SWT.BORDER | SWT.FULL_SELECTION);
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        table.setLayoutData(new GridData(GridData.FILL_BOTH));

        viewer = new TableViewer(table);
        viewer.setContentProvider(ArrayContentProvider.getInstance());

        setupColumns();
        setupTooltips(table);
    }

    // =================== Колонки ===================

    private void setupColumns() {
        createColumn(0, "Name", 150, InnerElementRepresentationDTO::getElementName, e -> null);
        createColumn(1, "Type / Return Type", 200,
                InnerElementRepresentationDTO::getFullQualifiedName,
                this::getTypeIcon);
        createColumn(2, "Value / Info", 300,
                InnerElementRepresentationDTO::getValue,
                this::getInspectIcon);
    }

    private void createColumn(int index, String title, int width,
                              Function<InnerElementRepresentationDTO, String> textExtractor,
                              Function<InnerElementRepresentationDTO, Image> imageExtractor) {

        TableViewerColumn column = new TableViewerColumn(viewer, SWT.NONE);
        column.getColumn().setText(title);
        column.getColumn().setWidth(width);
        column.getColumn().setResizable(true);

        column.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                if (element instanceof InnerElementRepresentationDTO inner) {
                    String value = textExtractor.apply(inner);
                    return value != null ? value : "";
                }
                return "";
            }

            @Override
            public Image getImage(Object element) {
                if (!(element instanceof InnerElementRepresentationDTO inner)) return null;

                Image img = imageExtractor.apply(inner);
                if (img != null) {
                    TableItem item = findTableItem(inner);
                    if (item != null) {
                        // Сохраняем подсказку по колонке
                        if (index == 1) { // вторая колонка
                            PairDTO<Image, String> pair = getTooltip(inner);
                            if (pair != null) {
                                item.setData("tooltip_col_" + index, pair.getSecond());
                            }
                        } else if (index == 2) { // третья колонка
                            if (img == getInspectIcon(inner)) {
                                String tooltipText = DebugWindowsManager.instance().icons.get("inspectIcon").getSecond();
                                item.setData("tooltip_col_" + index, tooltipText);
                            }
                        }
                    }
                }
                return img;
            }
        });
    }

    private TableItem findTableItem(InnerElementRepresentationDTO inner) {
        for (TableItem item : viewer.getTable().getItems()) {
            if (item.getData() == inner)
                return item;
        }
        return null;
    }

    // =================== Иконки и подсказки ===================

    private Image getInspectIcon(InnerElementRepresentationDTO element) {
        if (shouldShowInspectIcon(element)) {
            return DebugWindowsManager.instance().icons.get("inspectIcon").getFirst();
        }
        return null;
    }

    private Image getTypeIcon(InnerElementRepresentationDTO element) {
        String iconKey = switch (element.getElementType()) {
            case INTERFACE -> "interface";
            case METHOD -> "method";
            case STATIC_FIELD -> "static_field";
            case NON_STATIC_FIELD -> "fieldIcon";
            case VARIABLE -> "variableIcon";
            default -> null;
        };
        if (iconKey != null && DebugWindowsManager.instance().icons.containsKey(iconKey)) {
            return DebugWindowsManager.instance().icons.get(iconKey).getFirst();
        }
        return null;
    }

    private PairDTO<Image, String> getTooltip(InnerElementRepresentationDTO element) {
        if (element == null || element.getElementType() == null) return null;

        String iconKey = switch (element.getElementType()) {
            case INTERFACE -> "interface";
            case METHOD -> "method";
            case STATIC_FIELD -> "static_field";
            case NON_STATIC_FIELD -> "fieldIcon";
            case VARIABLE -> "variableIcon";
            default -> null;
        };

        if (iconKey != null && DebugWindowsManager.instance().icons.containsKey(iconKey)) {
            return DebugWindowsManager.instance().icons.get(iconKey);
        }
        return null;
    }

    private boolean shouldShowInspectIcon(InnerElementRepresentationDTO element) {
        String typeName = element.getFullQualifiedName();
        if (typeName == null) return false;

        return !JAVA_STANDARD_TYPES.contains(typeName)
                && !typeName.startsWith("java.") && !typeName.startsWith("javax.")
                && (element.getElementType() == UniversalElementType.NON_STATIC_FIELD
                    || element.getElementType() == UniversalElementType.VARIABLE);
    }

    // =================== Подсказки ===================

    private void setupTooltips(Table table) {
        table.addListener(SWT.MouseHover, event -> {
            TableItem item = table.getItem(new Point(event.x, event.y));
            if (item != null) {
                int columnIndex = getColumnIndexAtPoint(table, event.x);
                Object tooltip = item.getData("tooltip_col_" + columnIndex);
                table.setToolTipText(tooltip instanceof String ? (String) tooltip : null);
            } else {
                table.setToolTipText(null);
            }
        });
    }

    private int getColumnIndexAtPoint(Table table, int x) {
        int totalWidth = 0;
        for (int i = 0; i < table.getColumnCount(); i++) {
            totalWidth += table.getColumn(i).getWidth();
            if (x < totalWidth) return i;
        }
        return table.getColumnCount() - 1;
    }

    // =================== Отображение ===================

    public void showInnerElements(DebugWindowDataDTO debugWindowDataDTO) {
        if (debugWindowDataDTO == null || debugWindowDataDTO.getInnerElements().isEmpty()) return;

        List<InnerElementRepresentationDTO> sorted = new ArrayList<>(debugWindowDataDTO.getInnerElements());
        sorted.sort(
            Comparator.comparingInt((InnerElementRepresentationDTO e) -> e.getElementType().ordinal())
                      .thenComparing(Comparator.comparing(InnerElementRepresentationDTO::getElementName))
        );

        root.getDisplay().asyncExec(() -> {
            if (viewer.getTable().isDisposed()) return;
            viewer.setInput(sorted);
        });
    }

    public Composite getControl() {
        return root;
    }
}