package com.gmail.aydinov.sergey.simple_debugger_plugin.ui.tab;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.function.Function;

import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.CellEditor;

import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.DebugWindowDataDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.InnerElementRepresentationDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.UserChangedVariableEventDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebuggerEventTypes.SimpleDebuggerEventType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.SimpleDebuggerEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.UiEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.UIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.DebugWindowsManager;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.AbstractElementRepresentation.TargetApplicationElementType;

/**
 * Tab content that displays inner elements (fields / methods / variables)
 * at the moment a breakpoint is hit, with editable values.
 */
public class ClassMembersAtBreakpoint {

    private final Composite root;
    private final TableViewer viewer;
    private final UiEventCollector uiEventCollector = SimpleDebuggerEventCollector.instance();

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
        setupCellModifier();
    }

    private void setupColumns() {
        // Name
        createColumn("Name", 200, InnerElementRepresentationDTO::getName);

        // Type (с иконкой)
        createColumn("Type", 120,
                InnerElementRepresentationDTO::getTypeName,
                element -> {
                    TargetApplicationElementType type = element.getElementType();
                    if (type == null) return null;
                    switch (type) {
                        case INTERFACE:
                            return DebugWindowsManager.instance().icons.get("interface");
                        case METHOD:
                            return DebugWindowsManager.instance().icons.get("method");
                        case STATIC_FIELD:
                            return DebugWindowsManager.instance().icons.get("static_field");
                        case VARIABLE:
                            return DebugWindowsManager.instance().icons.get("variableIcon");
                        case NON_STATIC_FIELD:
                            return DebugWindowsManager.instance().icons.get("fieldIcon");
                        default:
                            return null;
                    }
                });

        // Value (editable)
        createColumn("Value", 300, InnerElementRepresentationDTO::getValue);
    }

    private void createColumn(String title, int width,
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
                if (element instanceof InnerElementRepresentationDTO dto) {
                    return imageExtractor.apply(dto);
                }
                return null;
            }
        });
    }

    private void createColumn(String title, int width, Function<InnerElementRepresentationDTO, String> extractor) {
        createColumn(title, width, extractor, e -> null);
    }

    /** Настройка редактирования поля Value */
    private void setupCellModifier() {
        viewer.setColumnProperties(new String[]{"name", "type", "value"});
        viewer.setCellEditors(new CellEditor[]{null, null, new TextCellEditor(viewer.getTable())});

        viewer.setCellModifier(new ICellModifier() {
            @Override
            public boolean canModify(Object element, String property) {
                return "value".equals(property);
            }

            @Override
            public Object getValue(Object element, String property) {
                if (element instanceof InnerElementRepresentationDTO dto) {
                    return dto.getValue();
                }
                return null;
            }

            @Override
            public void modify(Object element, String property, Object newValue) {
                if (!(element instanceof TableItem item)) return;

                InnerElementRepresentationDTO dto = (InnerElementRepresentationDTO) item.getData();
                if (Objects.isNull(dto) || Objects.isNull(newValue)) return;

                String newValStr = newValue.toString();

                // Отправляем событие изменения значения в UiEventCollector
                UserChangedVariableEventDTO userChangedVariableEventDTO = new UserChangedVariableEventDTO(
                        dto.getName(),
                        dto.getTypeName(),
                        newValStr
                );
                
                uiEventCollector.collectUiEvent(new UIEvent<UserChangedVariableEventDTO>(SimpleDebuggerEventType.USER_CHANGED_VARIABLE, userChangedVariableEventDTO));

                // Обновляем локальный DTO и таблицу
               // dto.setValue(newValStr);
                viewer.update(dto, null);
            }
        });
    }

    /** Показывает внутренние элементы DTO в таблице */
    public void showInnerElements(DebugWindowDataDTO parentDto) {
        if (parentDto == null) return;

        Set<InnerElementRepresentationDTO> innerElementsSet = parentDto.getInnerElements();
        if (innerElementsSet == null || innerElementsSet.isEmpty()) return;

        List<InnerElementRepresentationDTO> innerElements = innerElementsSet.stream().toList();

        root.getDisplay().asyncExec(() -> {
            if (viewer.getTable().isDisposed()) return;
            viewer.setInput(innerElements);
        });
    }

    public Composite getControl() {
        return root;
    }
}