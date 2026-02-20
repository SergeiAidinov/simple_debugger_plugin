package com.gmail.aydinov.sergey.simple_debugger_plugin.ui.tab;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.CellEditor;

import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.FieldOrVariableDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.UserChangedFieldEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.UserChangedVariableEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.logging.SimpleDebuggerLogger;
import com.gmail.aydinov.sergey.simple_debugger_plugin.processor.UiEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.processor.SimpleDebuggerEventQueue;

/**
 * Объединённая вкладка: поля + локальные переменные.
 * Поддерживает inspect для всех Iterable коллекций.
 */
public class VariablesFieldsTabContent {

    private final Composite root;
    private final Table table;
    private final TableViewer viewer;
    private final List<FieldOrVariableDTO> entries = new ArrayList<>();
    private final UiEventCollector uiEventCollector;

    private Image inspectIcon;

    public VariablesFieldsTabContent(Composite parent) {
        this.uiEventCollector = SimpleDebuggerEventQueue.instance();

        root = new Composite(parent, SWT.NONE);
        root.setLayout(new GridLayout(1, false));

        table = new Table(root, SWT.BORDER | SWT.FULL_SELECTION);
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        table.setLayoutData(new GridData(GridData.FILL_BOTH));

        loadInspectIcon();

        viewer = new TableViewer(table);
        viewer.setContentProvider(ArrayContentProvider.getInstance());

        setupColumns();
        setupCellModifier();
        setupClickListener();
    }

    /** Загрузка иконки inspect */
    private void loadInspectIcon() {
        try (InputStream is = getClass().getResourceAsStream("/icons/inspect.png")) {
            if (is != null) {
                inspectIcon = new Image(Display.getDefault(), is);
            } else {
                SimpleDebuggerLogger.error("Icon not found: /icons/inspect.png", null);
                inspectIcon = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Проверка: коллекция, которую можно inspect */
    private boolean isInspectableCollection(FieldOrVariableDTO dto) {
        if (dto == null || dto.getType() == null) return false;
        try {
            Class<?> klass = Class.forName(dto.getType());
            return Iterable.class.isAssignableFrom(klass);
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /** Настройка колонок: Name, Type, Value */
    private void setupColumns() {
        // Name
        TableViewerColumn nameColumn = new TableViewerColumn(viewer, SWT.NONE);
        nameColumn.getColumn().setText("Name");
        nameColumn.getColumn().setWidth(200);
        nameColumn.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                if (element instanceof FieldOrVariableDTO dto) return Objects.toString(dto.getName(), "");
                return "";
            }
        });

        // Type
        TableViewerColumn typeColumn = new TableViewerColumn(viewer, SWT.NONE);
        typeColumn.getColumn().setText("Type");
        typeColumn.getColumn().setWidth(120);
        typeColumn.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                if (element instanceof FieldOrVariableDTO dto) return Objects.toString(dto.getType(), "");
                return "";
            }
        });

        // Value (текст или иконка inspect)
        TableViewerColumn valueColumn = new TableViewerColumn(viewer, SWT.NONE);
        valueColumn.getColumn().setText("Value");
        valueColumn.getColumn().setWidth(200);
        valueColumn.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                if (element instanceof FieldOrVariableDTO dto) {
                    if (isInspectableCollection(dto)) return "";
                    return Objects.toString(dto.getValue(), "");
                }
                return "";
            }

            @Override
            public Image getImage(Object element) {
                if (element instanceof FieldOrVariableDTO dto && isInspectableCollection(dto)) {
                    return inspectIcon;
                }
                return null;
            }
        });

        viewer.setColumnProperties(new String[]{"name", "type", "value"});
        viewer.setCellEditors(new CellEditor[]{null, null, new TextCellEditor(table)});
    }

    /** Настройка редактирования значений */
    private void setupCellModifier() {
        viewer.setCellModifier(new ICellModifier() {
            @Override
            public boolean canModify(Object element, String property) {
                return "value".equals(property) && !(element instanceof FieldOrVariableDTO dto &&
                        isInspectableCollection(dto));
            }

            @Override
            public Object getValue(Object element, String property) {
                if (element instanceof FieldOrVariableDTO dto) return dto.getValue();
                return null;
            }

            @Override
            public void modify(Object element, String property, Object newValue) {
                if (!(element instanceof TableItem item)) return;

                FieldOrVariableDTO oldEntry = (FieldOrVariableDTO) item.getData();
                if (Objects.isNull(oldEntry) || Objects.isNull(newValue)) return;

                String newValStr = newValue.toString();

                // Generate the correct event based on type
                switch (oldEntry.getFieldOrVariableType()) {
                    case VARIABLE -> {
                        UserChangedVariableEvent dto = new UserChangedVariableEvent(
                                oldEntry.getName(),
                                oldEntry.getType(),
                                newValStr
                        );
                        uiEventCollector.collectUiEvent(dto);
                    }
                    case FIELD -> {
                        UserChangedFieldEvent dto = new UserChangedFieldEvent(
                                oldEntry.getName(),
                                oldEntry.getType(),
                                newValStr
                        );
                        uiEventCollector.collectUiEvent(dto);
                    }
                }

                // Update the table model
                int index = entries.indexOf(oldEntry);
                if (index >= 0) {
                    FieldOrVariableDTO updated = new FieldOrVariableDTO(
                            oldEntry.getName(),
                            oldEntry.getType(),
                            newValStr,
                            oldEntry.getFieldOrVariableType()
                    );
                    entries.set(index, updated);
                    viewer.update(updated, null);
                }
            }
        });
    }

    /** Клик по иконке inspect */
    private void setupClickListener() {
        table.addListener(SWT.MouseDown, event -> {
            Point pt = new Point(event.x, event.y);
            TableItem item = table.getItem(pt);
            if (item == null) return;

            for (int i = 0; i < table.getColumnCount(); i++) {
                if (item.getBounds(i).contains(pt) && i == 2) { // Value column
                    FieldOrVariableDTO dto = (FieldOrVariableDTO) item.getData();
                    if (isInspectableCollection(dto)) {
                        inspectCollection(dto);
                    }
                    break;
                }
            }
        });
    }

    /** Обработчик inspect */
    private void inspectCollection(FieldOrVariableDTO dto) {
        // TODO: реализовать просмотр коллекции
        System.out.println("Inspect collection: " + dto.getName());
    }

    /** Обновление списка (поля + локальные переменные) */
    public void updateVariablesAndFields(List<FieldOrVariableDTO> variables, List<FieldOrVariableDTO> fields) {
        if (table.isDisposed()) return;
        entries.clear();
        if (variables != null) entries.addAll(variables);
        if (fields != null) entries.addAll(fields);

        viewer.setInput(entries);
        viewer.refresh();
    }

    /** Возвращает root Composite */
    public Composite getControl() {
        return root;
    }
}