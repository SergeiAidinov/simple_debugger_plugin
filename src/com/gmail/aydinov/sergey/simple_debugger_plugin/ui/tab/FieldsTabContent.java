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

import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.VariableDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.UserChangedFieldEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.logging.SimpleDebuggerLogger;
import com.gmail.aydinov.sergey.simple_debugger_plugin.processor.UiEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.processor.SimpleDebuggerEventQueue;

/**
 * Fields tab with "inspect" icon for any Iterable.
 * Works dynamically for all existing and future Iterable classes.
 */
public class FieldsTabContent {

    private final Composite root;
    private final Table table;
    private final TableViewer viewer;
    private final List<VariableDTO> entries = new ArrayList<>();
    private final UiEventCollector uiEventCollector;

    private Image inspectIcon;

    public FieldsTabContent(Composite parent) {
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

    /** Загружаем иконку inspect */
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

    /** Проверка, реализует ли тип Iterable (динамически через Reflection) */
    private boolean isInspectableCollection(VariableDTO dto) {
        if (dto == null || dto.getType() == null) return false;
        try {
            Class<?> klass = Class.forName(dto.getType());
            return Iterable.class.isAssignableFrom(klass);
        } catch (ClassNotFoundException e) {
            // Класс не найден — считаем, что это не коллекция
            return false;
        }
    }

    private void setupColumns() {
        // Field column
        TableViewerColumn nameColumn = new TableViewerColumn(viewer, SWT.NONE);
        nameColumn.getColumn().setText("Field");
        nameColumn.getColumn().setWidth(200);
        nameColumn.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                if (element instanceof VariableDTO dto) {
                    return Objects.toString(dto.getName(), "");
                }
                return "";
            }
        });

        // Type column
        TableViewerColumn typeColumn = new TableViewerColumn(viewer, SWT.NONE);
        typeColumn.getColumn().setText("Type");
        typeColumn.getColumn().setWidth(100);
        typeColumn.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                if (element instanceof VariableDTO dto) {
                    return Objects.toString(dto.getType(), "");
                }
                return "";
            }
        });

        // Value column (текст или иконка inspect)
        TableViewerColumn valueColumn = new TableViewerColumn(viewer, SWT.NONE);
        valueColumn.getColumn().setText("Value");
        valueColumn.getColumn().setWidth(200);
        valueColumn.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                if (element instanceof VariableDTO dto) {
                    if (isInspectableCollection(dto)) {
                        return ""; // для коллекции текста нет
                    }
                    return Objects.toString(dto.getValue(), "");
                }
                return "";
            }

            @Override
            public Image getImage(Object element) {
                if (element instanceof VariableDTO dto) {
                    if (isInspectableCollection(dto)) {
                        return inspectIcon;
                    }
                }
                return null;
            }
        });

        viewer.setColumnProperties(new String[]{"name", "type", "value"});
        viewer.setCellEditors(new CellEditor[]{null, null, new TextCellEditor(table)});
    }

    private void setupCellModifier() {
        viewer.setCellModifier(new ICellModifier() {
            @Override
            public boolean canModify(Object element, String property) {
                return "value".equals(property) && !(element instanceof VariableDTO dto &&
                        isInspectableCollection(dto));
            }

            @Override
            public Object getValue(Object element, String property) {
                if (element instanceof VariableDTO dto) {
                    return dto.getValue();
                }
                return null;
            }

            @Override
            public void modify(Object element, String property, Object newValue) {
                if (!(element instanceof TableItem item)) return;
                VariableDTO entry = (VariableDTO) item.getData();
                String valueStr = Objects.toString(newValue, null);

                UserChangedFieldEvent dto = new UserChangedFieldEvent(entry.getName(), entry.getType(), valueStr);
                uiEventCollector.collectUiEvent(dto);

                viewer.update(entry, null);
            }
        });
    }

    private void setupClickListener() {
        table.addListener(SWT.MouseDown, event -> {
            Point pt = new Point(event.x, event.y);
            TableItem item = table.getItem(pt);
            if (item == null) return;

            int columnIndex = -1;
            for (int i = 0; i < table.getColumnCount(); i++) {
                if (item.getBounds(i).contains(pt)) {
                    columnIndex = i;
                    break;
                }
            }

            // Value column
            if (columnIndex == 2) {
                VariableDTO dto = (VariableDTO) item.getData();
                if (isInspectableCollection(dto)) {
                    inspectCollection(dto);
                }
            }
        });
    }

    /** Обработчик просмотра коллекции */
    private void inspectCollection(VariableDTO dto) {
        // TODO: реализовать логику просмотра элементов коллекции
        System.out.println("Inspect collection: " + dto.getName());
    }

    public void updateFields(List<VariableDTO> vars) {
        if (table.isDisposed()) return;

        entries.clear();
        if (Objects.nonNull(vars)) entries.addAll(vars);

        viewer.setInput(entries);
        viewer.refresh();
    }

    public Composite getControl() {
        return root;
    }
}