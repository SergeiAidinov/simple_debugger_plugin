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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.InnerElementRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.UniversalElementType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.DebugWindowDataDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.DebugWindowsManager;

/**
 * Вкладка для отображения полей, методов и переменных
 * с иконками и подсказками.
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
        createColumn("Name", 150, InnerElementRepresentation::getElementName);

        createColumn("Type / Return Type", 200, InnerElementRepresentation::getFullQualifiedName, element -> {
            UniversalElementType type = element.getElementType();
            if (type == null) return null;

            String iconKey = switch (type) {
                case INTERFACE -> "interface";
                case METHOD -> "method";
                case STATIC_FIELD -> "static_field";
                case VARIABLE -> "variableIcon";
                case NON_STATIC_FIELD -> "fieldIcon";
                default -> null;
            };
            if (iconKey == null) return null;
            return DebugWindowsManager.instance().icons.get(iconKey).getFirst();
        });

        createColumn("Value / Info", 300, InnerElementRepresentation::getValue, this::getInspectIcon);
    }

    private Image getInspectIcon(InnerElementRepresentation element) {
        if (shouldShowInspectIcon(element)) {
            return DebugWindowsManager.instance().icons.get("inspectIcon").getFirst();
        }
        return null;
    }

    private void createColumn(String title, int width, Function<InnerElementRepresentation, String> textExtractor) {
        createColumn(title, width, textExtractor, e -> null);
    }

    private void createColumn(String title, int width,
                              Function<InnerElementRepresentation, String> textExtractor,
                              Function<InnerElementRepresentation, Image> imageExtractor) {

        TableViewerColumn column = new TableViewerColumn(viewer, SWT.NONE);
        column.getColumn().setText(title);
        column.getColumn().setWidth(width);
        column.getColumn().setResizable(true);

        column.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                if (element instanceof InnerElementRepresentation inner) {
                    String value = textExtractor.apply(inner);
                    return value != null ? value : "";
                }
                return "";
            }

            @Override
            public Image getImage(Object element) {
                if (element instanceof InnerElementRepresentation inner) {
                    Image img = imageExtractor.apply(inner);
                    if (img != null) {
                        TableItem item = findTableItem(inner);
                        if (item != null) {
                            // Tooltip с именем и полным типом
                            item.setData("tooltip", inner.getElementName() + " : " + inner.getFullQualifiedName());
                        }
                    }
                    return img;
                }
                return null;
            }
        });
    }

    private TableItem findTableItem(InnerElementRepresentation inner) {
        for (TableItem item : viewer.getTable().getItems()) {
            if (item.getData() == inner)
                return item;
        }
        return null;
    }

    // =================== Подсказки ===================

    private boolean shouldShowInspectIcon(InnerElementRepresentation element) {
        String typeName = element.getFullQualifiedName();
        if (typeName == null) return false;

        return !JAVA_STANDARD_TYPES.contains(typeName)
                && !typeName.startsWith("java.") && !typeName.startsWith("javax.")
                && (element.getElementType() == UniversalElementType.NON_STATIC_FIELD
                    || element.getElementType() == UniversalElementType.VARIABLE);
    }

    private void setupTooltips(Table table) {
        table.addListener(SWT.MouseHover, event -> {
            TableItem item = table.getItem(new org.eclipse.swt.graphics.Point(event.x, event.y));
            if (item != null && item.getData() instanceof InnerElementRepresentation inner) {
                Object tooltip = item.getData("tooltip");
                table.setToolTipText(tooltip instanceof String ? (String) tooltip : null);
            } else {
                table.setToolTipText(null);
            }
        });
    }

    // =================== Отображение ===================

    public void showInnerElements(DebugWindowDataDTO debugWindowDataDTO) {
        if (debugWindowDataDTO == null || debugWindowDataDTO.getInnerElements().isEmpty()) return;

        List<InnerElementRepresentation> sorted = new ArrayList<>(debugWindowDataDTO.getInnerElements());
        sorted.sort(
        	    Comparator.comparingInt((InnerElementRepresentation e) -> e.getElementType().ordinal())
        	              .thenComparing(Comparator.comparing(InnerElementRepresentation::getElementName))
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