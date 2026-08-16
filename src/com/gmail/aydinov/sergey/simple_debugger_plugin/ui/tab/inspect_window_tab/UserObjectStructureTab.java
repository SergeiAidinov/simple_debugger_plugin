package com.gmail.aydinov.sergey.simple_debugger_plugin.ui.tab.inspect_window_tab;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.UniversalElementType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.ValueCategory;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.UserChangedFieldEventDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.details.UserInstanceDetailsDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.inspection.AbstractInspectionDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.inspection.UserObjectPageDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebuggerEventTypes.SimpleDebuggerEventType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.SimpleDebuggerEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.UiEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.UIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.tooltip_manager.TooltipManager;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.utils.UiUtils;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.window.SimpleDebugerWindowsManager;

/**
 * Вкладка инспектора объекта с полным набором иконок в Value-колонке.
 */
public class UserObjectStructureTab implements InspectorTab {

    private final Composite root;
    private final TableViewer viewer;
    private final UiEventCollector uiEventCollector = SimpleDebuggerEventCollector.instance();

    // Header
    private final Label objectTypeLabel;
    private final Label objectSizeLabel;

    private TooltipManager tooltipManager;
    private InnerElementRepresentationDTO lastInspectedElement;

    public UserObjectStructureTab(Composite parent) {
        root = new Composite(parent, SWT.NONE);
        root.setLayout(new GridLayout(1, false));

        // ================= HEADER =================
        Composite header = new Composite(root, SWT.NONE);
        header.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        header.setLayout(new GridLayout(1, false));
        objectTypeLabel = new Label(header, SWT.NONE);
        objectSizeLabel = new Label(header, SWT.NONE);

        // ================= TABLE =================
        Table table = new Table(root, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        viewer = new TableViewer(table);
        viewer.setContentProvider(ArrayContentProvider.getInstance());

        setupColumns();
        setupTooltips(table);
        setupClickListener(table);
        setupHoverInspectionListener();
    }

    public Composite getControl() {
        return root;
    }

    // ================= SHOW =================
    public void showPage(AbstractInspectionDTO abstractInspectionDTO) {
        if (!(abstractInspectionDTO instanceof UserObjectPageDTO dto)) return;

        List<InnerElementRepresentationDTO> entries = dto.getEntries() != null ? dto.getEntries() : List.of();
        System.out.println("=== SHOW PAGE ===");
        System.out.println("dto       = " + System.identityHashCode(dto));
        System.out.println("entries   = " + System.identityHashCode(entries));
        System.out.println("objectId  = " + dto.getObjectId());

        for (InnerElementRepresentationDTO e : entries) {
            System.out.println(
                "DTO       = " + System.identityHashCode(e)
                + " name=" + e.getElementName()
                + " value=" + e.getValue()
                + " tag=" + e.getTag()
                + " objectId=" + e.getObjectId()
            );
        }
        Display.getDefault().asyncExec(() -> {
            if (root.isDisposed() || viewer.getTable().isDisposed()) return;

            objectTypeLabel.setText("Type: " + safe(dto.getClassType()));
            objectSizeLabel.setText("Object id : " + dto.getObjectId());
            viewer.setInput(null);
            viewer.setInput(entries);
            viewer.refresh(true);
            root.layout(true, true);
        });
    }

    @Override
    public void showFieldInfoPopupFromBackend(UserInstanceDetailsDTO userInstanceDetailsDTO) {
        Display display = root.getDisplay();
        display.asyncExec(() -> {
            if (root.isDisposed()) return;
            Point location = display.getCursorLocation();
            if (tooltipManager != null) {
                tooltipManager.showTooltipForUserObject(userInstanceDetailsDTO, location);
            }
        });
    }

    // =========================================================
    // Columns
    // =========================================================
    private void setupColumns() {
        // Name
        createColumn("Name", 280, InnerElementRepresentationDTO::getElementName, e -> null);

        // Value + Icons
        TableViewerColumn valueColumn = createColumn("Value", 520,
                dto -> dto.getValue() != null ? dto.getValue() : "null",
                this::getIcon);

        valueColumn.setEditingSupport(new ValueEditingSupport(viewer));
    }

    private TableViewerColumn createColumn(String title, int width,
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
                    String v = textExtractor.apply(dto);
                    return v != null ? v : "";
                }
                return "";
            }

            @Override
            public Image getImage(Object element) {
                if (!(element instanceof InnerElementRepresentationDTO dto)) return null;
                return imageExtractor.apply(dto);
            }
        });

        return column;
    }

    // =========================================================
    // Icons (полный набор как в главной вкладке)
    // =========================================================
    private Image getIcon(InnerElementRepresentationDTO dto) {
        if (dto == null) return null;

        ValueCategory category = dto.getValueCategory();

        // 1. Коллекции и мапы
        if (category == ValueCategory.COLLECTION || category == ValueCategory.MAP) {
            return SimpleDebugerWindowsManager.instance().icons.get("lens").getFirst();
        }

        // 2. Пользовательские объекты (inspect)
        if (category == ValueCategory.USER_OBJECT
                && dto.getValue() != null
                && !UiUtils.isStandartJavaType(dto.getTypeOrReturnType())) {
            return SimpleDebugerWindowsManager.instance().icons.get("inspectIcon").getFirst();
        }

        // 3. Методы
        if (dto.getElementType() == UniversalElementType.METHOD) {
            return SimpleDebugerWindowsManager.instance().icons.get("method").getFirst();
        }

        // 4. Поля
        if (dto.getElementType() == UniversalElementType.FIELD) {
            if (dto.isStatic()) {
                return SimpleDebugerWindowsManager.instance().icons.get("static_field").getFirst();
            } else {
                return SimpleDebugerWindowsManager.instance().icons.get("fieldIcon").getFirst();
            }
        }

        return null;
    }

    // =========================================================
    // Editing
    // =========================================================
    private boolean isEditable(InnerElementRepresentationDTO dto) {
        return dto != null && dto.getTypeOrReturnType() != null
                && UiUtils.isStandartJavaType(dto.getTypeOrReturnType());
    }

    private class ValueEditingSupport extends EditingSupport {
        private final TextCellEditor editor;

        ValueEditingSupport(TableViewer viewer) {
            super(viewer);
            this.editor = new TextCellEditor(viewer.getTable());
        }

        @Override
        protected CellEditor getCellEditor(Object element) {
            return editor;
        }

        @Override
        protected boolean canEdit(Object element) {
            return element instanceof InnerElementRepresentationDTO dto && isEditable(dto);
        }

        @Override
        protected Object getValue(Object element) {
            return ((InnerElementRepresentationDTO) element).getValue();
        }

        @Override
        protected void setValue(Object element, Object value) {
            if (!(element instanceof InnerElementRepresentationDTO dto)) return;
            if (value == null) return;

            String newValueStr = value.toString();
            String type = dto.getAdditionalInfo() != null ? dto.getAdditionalInfo() : dto.getTypeOrReturnType();

            Object converted = UiUtils.convertToType(newValueStr, type);

            uiEventCollector.collectUiEvent(new UIEvent<>(
                    SimpleDebuggerEventType.USER_CHANGED_FIELD,
                    new UserChangedFieldEventDTO(dto.getTag(), converted.toString())
            ));

            viewer.update(dto, null);
        }
    }

    // =========================================================
    // Tooltips + Hover + Click
    // =========================================================
    private void setupTooltips(Table table) {
        tooltipManager = new TooltipManager(table, root);
    }

    private void setupClickListener(Table table) {
        table.addListener(SWT.MouseDown, event -> {
        	
            TableItem item = table.getItem(new Point(event.x, event.y));
            if (item == null) return;

            int colIndex = getColumnIndexAtPoint(table, event.x);
            if (colIndex != 1) return;

            Object data = item.getData();
            if (data instanceof InnerElementRepresentationDTO dto) {
                System.out.println("=== CLICK ===");
                System.out.println(
                    "DTO       = " + System.identityHashCode(dto)
                    + " name=" + dto.getElementName()
                    + " value=" + dto.getValue()
                    + " tag=" + dto.getTag()
                    + "objectId     = " + dto.getObjectId()
                );
            }
            if (!(data instanceof InnerElementRepresentationDTO dto)) return;

            ValueCategory category = dto.getValueCategory();

            if (category == ValueCategory.USER_OBJECT) {
            	System.out.println("=== SEND INSPECT EVENT ===");
            	System.out.println("dto identity = " + System.identityHashCode(dto));
            	System.out.println("name         = " + dto.getElementName());
            	System.out.println("value        = " + dto.getValue());
            	System.out.println("tag          = " + dto.getTag());
            	System.out.println("objectId     = " + dto.getObjectId());
                uiEventCollector.collectUiEvent(
                        new UIEvent<>(SimpleDebuggerEventType.USER_INSPECTS_USER_OBJECT_IN_INSPECTION_SEANCE, dto));
            } else if (category == ValueCategory.MAP) {
                uiEventCollector.collectUiEvent(
                        new UIEvent<>(SimpleDebuggerEventType.USER_REQUESTED_MAP_PAGE, dto));
            } else if (category == ValueCategory.COLLECTION) {
                uiEventCollector.collectUiEvent(
                        new UIEvent<>(SimpleDebuggerEventType.USER_INSPECTS_ITERABLE, dto));
            }
            // Для обычных полей и методов клик по иконке ничего не делает (как в главной вкладке)
        });
    }

    private void setupHoverInspectionListener() {
        Table table = viewer.getTable();

        table.addListener(SWT.MouseMove, event -> {
            TableItem item = table.getItem(new Point(event.x, event.y));
            InnerElementRepresentationDTO dto = null;

            if (item != null && item.getData() instanceof InnerElementRepresentationDTO dataDto) {
                int colIndex = getColumnIndexAtPoint(table, event.x);
                if (colIndex == 1) {
                    Image icon = getIcon(dataDto);
                    Image inspectIcon = SimpleDebugerWindowsManager.instance().icons.get("inspectIcon").getFirst();
                    Image lensIcon = SimpleDebugerWindowsManager.instance().icons.get("lens").getFirst();

                    if (icon == inspectIcon || icon == lensIcon) {
                        dto = dataDto;
                    }
                }
            }

            if (!Objects.equals(dto, lastInspectedElement)) {
                lastInspectedElement = dto;
                if (tooltipManager != null) tooltipManager.closePopup();

                if (dto != null) {
                    Image icon = getIcon(dto);
                    Image inspectIcon = SimpleDebugerWindowsManager.instance().icons.get("inspectIcon").getFirst();
                    Image lensIcon = SimpleDebugerWindowsManager.instance().icons.get("lens").getFirst();

                    if (icon == inspectIcon) {
                        uiEventCollector.collectUiEvent(
                                new UIEvent<>(SimpleDebuggerEventType.USER_REQUESTED_ADDITIONAL_INFO_ABOUT_OBJECT, dto));
                    } else if (icon == lensIcon) {
                        Point location = root.getDisplay().getCursorLocation();
                        tooltipManager.showTooltipForCollection(dto, location);
                    }
                }
            }
        });
    }

    private int getColumnIndexAtPoint(Table table, int x) {
        int offset = 0;
        for (int i = 0; i < table.getColumnCount(); i++) {
            offset += table.getColumn(i).getWidth();
            if (x < offset) return i;
        }
        return table.getColumnCount() - 1;
    }

    private String safe(String v) {
        return v == null ? "" : v;
    }
}