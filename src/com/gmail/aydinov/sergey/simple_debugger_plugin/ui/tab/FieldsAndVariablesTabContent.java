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
 * Combined tab for object fields and local variables.
 * Supports "inspect" for any Iterable collections.
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

    /**
     * Constructs the combined tab UI.
     *
     * @param parent the parent composite
     */
    public FieldsAndVariablesTabContent(Composite parent) {
        this.uiEventCollector = SimpleDebuggerEventQueue.instance();

        root = new Composite(parent, SWT.NONE);
        root.setLayout(new GridLayout(1, false));

        table = new Table(root, SWT.BORDER | SWT.FULL_SELECTION);
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        table.setLayoutData(new GridData(GridData.FILL_BOTH));

        loadIcons(); // Load inspect, variable, field icons

        viewer = new TableViewer(table);
        viewer.setContentProvider(ArrayContentProvider.getInstance());

        setupColumns();
        setupCellModifier();
        setupClickListener();
    }

    /** Loads all required icons from resources and scales them to 16x16 */
    private void loadIcons() {
        try {
            // Inspect icon
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

            // Variable icon
            try (InputStream varIs = getClass().getResourceAsStream("/icons/variable.png")) {
                if (varIs != null) {
                    Image original = new Image(Display.getDefault(), varIs);
                    variableIcon = new Image(Display.getDefault(), original.getImageData().scaledTo(16, 16));
                    original.dispose();
                } else {
                    variableIcon = null;
                }
            }

            // Field icon
            try (InputStream fieldIs = getClass().getResourceAsStream("/icons/field.png")) {
                if (fieldIs != null) {
                    Image original = new Image(Display.getDefault(), fieldIs);
                    fieldIcon = new Image(Display.getDefault(), original.getImageData().scaledTo(16, 16));
                    original.dispose();
                } else {
                    fieldIcon = null;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            inspectIcon = null;
            variableIcon = null;
            fieldIcon = null;
        }
    }

    /**
     * Determines whether the given DTO represents a collection
     * that can be inspected.
     *
     * @param dto the field or variable DTO
     * @return true if the DTO type implements Iterable
     */
    private boolean isInspectableCollection(FieldOrVariableDTO dto) {
        if (dto == null || dto.getType() == null) return false;
        try {
            Class<?> klass = Class.forName(dto.getType());
            return Iterable.class.isAssignableFrom(klass);
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /** Sets up table columns: Name, Type (with icon), Value */
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

        // Type column with icon
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
                    switch (dto.getFieldOrVariableType()) {
                        case VARIABLE -> { return variableIcon; }
                        case FIELD -> { return fieldIcon; }
                    }
                }
                return null;
            }
        });

        // Value column (text or inspect icon)
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

    /** Sets up editing behavior for the Value column */
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

                // Generate the correct event depending on whether it's a variable or field
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
                int index = -1;
                for (int i = 0; i < entries.size(); i++) {
                    if (Objects.equals(entries.get(i), oldEntry)) {
                        index = i;
                        break;
                    }
                }
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

    /** Sets up click listener for inspect icons */
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

    /**
     * Handles inspect action for collections
     *
     * @param dto the field or variable representing a collection
     */
    private void inspectCollection(FieldOrVariableDTO dto) {
        // TODO: implement collection inspection logic
        System.out.println("Inspect collection: " + dto.getName());
    }

    /**
     * Updates the table with new variables and fields
     *
     * @param variables list of local variables
     * @param fields list of object fields
     */
    public void updateVariablesAndFields(List<FieldOrVariableDTO> variables, List<FieldOrVariableDTO> fields) {
        if (table.isDisposed()) return;
        entries.clear();
        if (variables != null) entries.addAll(variables);
        if (fields != null) entries.addAll(fields);

        viewer.setInput(entries);
        viewer.refresh();
    }

    /**
     * Returns the root composite of this tab
     *
     * @return the root SWT Composite
     */
    public Composite getControl() {
        return root;
    }
}