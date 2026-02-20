package com.gmail.aydinov.sergey.simple_debugger_plugin.ui.tab;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.ToolTip;
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
 * Combined tab for object fields and local variables.
 * Supports "inspect" for Iterable collections.
 * Type column includes icons with hover hints.
 */
public class FieldsAndVariablesTabContent {

    private final Composite root;
    private final Table table;
    private final TableViewer viewer;
    private final List<FieldOrVariableDTO> entries = new ArrayList<>();
    private final UiEventCollector uiEventCollector;

    private Image inspectIcon;
    private Image variableIcon;
    private Image fieldIcon;

    public FieldsAndVariablesTabContent(Composite parent) {
        this.uiEventCollector = SimpleDebuggerEventQueue.instance();

        root = new Composite(parent, SWT.NONE);
        root.setLayout(new GridLayout(1, false));

        table = new Table(root, SWT.BORDER | SWT.FULL_SELECTION);
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        table.setLayoutData(new GridData(GridData.FILL_BOTH));

        loadIcons();

        viewer = new TableViewer(table);
        viewer.setContentProvider(ArrayContentProvider.getInstance());
        ColumnViewerToolTipSupport.enableFor(viewer, ToolTip.NO_RECREATE);

        setupColumns();
        setupCellModifier();
        setupClickListener();
    }

    /** Loads all required icons and scales to 16x16 */
    private void loadIcons() {
        try {
            try (InputStream is = getClass().getResourceAsStream("/icons/inspect.png")) {
                if (is != null) {
                    Image original = new Image(Display.getDefault(), is);
                    inspectIcon = new Image(Display.getDefault(), original.getImageData().scaledTo(16, 16));
                    original.dispose();
                } else {
                    SimpleDebuggerLogger.error("Icon not found: /icons/inspect.png", null);
                    inspectIcon = null;
                }
            }

            try (InputStream varIs = getClass().getResourceAsStream("/icons/variable.png")) {
                if (varIs != null) {
                    Image original = new Image(Display.getDefault(), varIs);
                    variableIcon = new Image(Display.getDefault(), original.getImageData().scaledTo(16, 16));
                    original.dispose();
                } else variableIcon = null;
            }

            try (InputStream fieldIs = getClass().getResourceAsStream("/icons/field.png")) {
                if (fieldIs != null) {
                    Image original = new Image(Display.getDefault(), fieldIs);
                    fieldIcon = new Image(Display.getDefault(), original.getImageData().scaledTo(16, 16));
                    original.dispose();
                } else fieldIcon = null;
            }

        } catch (Exception e) {
            e.printStackTrace();
            inspectIcon = null;
            variableIcon = null;
            fieldIcon = null;
        }
    }

    private boolean isInspectableCollection(FieldOrVariableDTO dto) {
        if (dto == null || dto.getType() == null) return false;
        try {
            Class<?> klass = Class.forName(dto.getType());
            return Iterable.class.isAssignableFrom(klass);
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private void setupColumns() {

        // Name column
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

        // Type column (icon + type text + tooltip)
        TableViewerColumn typeColumn = new TableViewerColumn(viewer, SWT.NONE);
        typeColumn.getColumn().setText("Type");
        typeColumn.getColumn().setWidth(140);
        typeColumn.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                if (element instanceof FieldOrVariableDTO dto) return Objects.toString(dto.getType(), "");
                return "";
            }

            @Override
            public Image getImage(Object element) {
                if (element instanceof FieldOrVariableDTO dto) {
                    return switch (dto.getFieldOrVariableType()) {
                        case VARIABLE -> variableIcon;
                        case FIELD -> fieldIcon;
                    };
                }
                return null;
            }

            @Override
            public String getToolTipText(Object element) {
                if (element instanceof FieldOrVariableDTO dto) {
                    return switch (dto.getFieldOrVariableType()) {
                        case VARIABLE -> "Variable";
                        case FIELD -> "Field";
                    };
                }
                return null;
            }
        });

        // Value column (text or inspect icon + tooltip)
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

            @Override
            public String getToolTipText(Object element) {
                if (element instanceof FieldOrVariableDTO dto && isInspectableCollection(dto)) {
                    return "Inspect collection";
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

                switch (oldEntry.getFieldOrVariableType()) {
                    case VARIABLE -> uiEventCollector.collectUiEvent(new UserChangedVariableEvent(
                            oldEntry.getName(), oldEntry.getType(), newValStr));
                    case FIELD -> uiEventCollector.collectUiEvent(new UserChangedFieldEvent(
                            oldEntry.getName(), oldEntry.getType(), newValStr));
                }

                int index = -1;
                for (int i = 0; i < entries.size(); i++) {
                    if (Objects.equals(entries.get(i), oldEntry)) { index = i; break; }
                }
                if (index >= 0) {
                    FieldOrVariableDTO updated = new FieldOrVariableDTO(
                            oldEntry.getName(), oldEntry.getType(),
                            newValStr, oldEntry.getFieldOrVariableType());
                    entries.set(index, updated);
                    viewer.update(updated, null);
                }
            }
        });
    }

    private void setupClickListener() {
        table.addListener(SWT.MouseDown, event -> {
            Point pt = new Point(event.x, event.y);
            TableItem item = table.getItem(pt);
            if (item == null) return;

            for (int i = 0; i < table.getColumnCount(); i++) {
                if (item.getBounds(i).contains(pt) && i == 2) { // Value column
                    FieldOrVariableDTO dto = (FieldOrVariableDTO) item.getData();
                    if (isInspectableCollection(dto)) inspectCollection(dto);
                    break;
                }
            }
        });
    }

    private void inspectCollection(FieldOrVariableDTO dto) {
        System.out.println("Inspect collection: " + dto.getName());
    }

    public void updateVariablesAndFields(List<FieldOrVariableDTO> variables, List<FieldOrVariableDTO> fields) {
        if (table.isDisposed()) return;
        entries.clear();
        if (variables != null) entries.addAll(variables);
        if (fields != null) entries.addAll(fields);

        viewer.setInput(entries);
        viewer.refresh();
    }

    public Composite getControl() { return root; }
}