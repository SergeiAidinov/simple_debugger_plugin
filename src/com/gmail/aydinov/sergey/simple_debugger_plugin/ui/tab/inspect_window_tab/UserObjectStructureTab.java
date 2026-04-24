package com.gmail.aydinov.sergey.simple_debugger_plugin.ui.tab.inspect_window_tab;

import java.util.List;
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
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.UniversalElementType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.ValueCategory;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.PairDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.inspection.UserObjectPageDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebuggerEventTypes.SimpleDebuggerEventType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.SimpleDebuggerEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.UiEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.UIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.utils.UiUtils;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.window.SimpleDebugerWindowsManager;

/**
 * Простая вкладка инспектора объекта с иконками + header.
 */
public class UserObjectStructureTab {

    private final Composite root;
    private final TableViewer viewer;
    private final UiEventCollector uiEventCollector = SimpleDebuggerEventCollector.instance();

    // ===== Header =====
    private final Label objectTypeLabel;
    private final Label objectSizeLabel;

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

        createColumn("Name", 250, InnerElementRepresentationDTO::getElementName);
        createColumn("Value", 500,
                dto -> dto.getValue() != null ? dto.getValue() : "null",
                this::getIcon);

        setupClickListener(table);
    }

    public Composite getControl() {
        return root;
    }

    // ================= SHOW =================
    public void showUserObject(UserObjectPageDTO dto) {

        List<InnerElementRepresentationDTO> entries =
                (dto == null || dto.getEntries() == null) ? List.of() : dto.getEntries();

        root.getDisplay().asyncExec(() -> {
            if (root.isDisposed() || viewer.getTable().isDisposed())
                return;

            objectTypeLabel.setText("Type: " + safe(dto != null ? dto.getClassType() : null));
            objectSizeLabel.setText("Object id : " + dto.getObjectId());

            viewer.setInput(entries);
            viewer.refresh();

            root.layout(true, true);
        });
    }

    // ================= COLUMN =================
    private TableViewerColumn createColumn(
            String title,
            int width,
            Function<InnerElementRepresentationDTO, String> extractor) {

        return createColumn(title, width, extractor, e -> null);
    }

    private TableViewerColumn createColumn(
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
                    String v = textExtractor.apply(dto);
                    return v != null ? v : "";
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

        return column;
    }

    // ================= CLICK =================
    private void setupClickListener(Table table) {

        table.addListener(SWT.MouseDown, event -> {
            TableItem item = table.getItem(new Point(event.x, event.y));
            if (item == null)
                return;

            int colIndex = getColumnIndexAtPoint(table, event.x);
            if (colIndex != 1)
                return;

            Object data = item.getData();
            if (!(data instanceof InnerElementRepresentationDTO dto))
                return;

            ValueCategory category = dto.getValueCategory();

            if (category == ValueCategory.USER_OBJECT)
                uiEventCollector.collectUiEvent(
                        new UIEvent<>(SimpleDebuggerEventType.USER_INSPECTS_USER_OBJECT, dto));

            else if (category == ValueCategory.MAP)
                uiEventCollector.collectUiEvent(
                        new UIEvent<>(SimpleDebuggerEventType.USER_REQUESTED_MAP_PAGE, dto));

            else if (category == ValueCategory.COLLECTION)
                uiEventCollector.collectUiEvent(
                        new UIEvent<>(SimpleDebuggerEventType.USER_INSPECTS_ITERABLE, dto));
        });
    }

    private int getColumnIndexAtPoint(Table table, int x) {
        int offset = 0;
        for (int i = 0; i < table.getColumnCount(); i++) {
            offset += table.getColumn(i).getWidth();
            if (x < offset)
                return i;
        }
        return table.getColumnCount() - 1;
    }

    // ================= ICONS =================
    private Image getIcon(InnerElementRepresentationDTO dto) {
        if (dto == null)
            return null;

        ValueCategory category = dto.getValueCategory();

        if (category == ValueCategory.COLLECTION || category == ValueCategory.MAP)
            return SimpleDebugerWindowsManager.instance().icons.get("lens").getFirst();

        if (category == ValueCategory.USER_OBJECT
                && dto.getValue() != null
                && !UiUtils.isStandartJavaType(dto.getTypeOrReturnType()))
            return SimpleDebugerWindowsManager.instance().icons.get("inspectIcon").getFirst();

        UniversalElementType type = dto.getElementType();

        if (type == UniversalElementType.METHOD)
            return SimpleDebugerWindowsManager.instance().icons.get("method").getFirst();

        if (type == UniversalElementType.FIELD && dto.isStatic())
            return SimpleDebugerWindowsManager.instance().icons.get("static_field").getFirst();

        if (type == UniversalElementType.FIELD)
            return SimpleDebugerWindowsManager.instance().icons.get("fieldIcon").getFirst();

        return null;
    }

    private String safe(String v) {
        return v == null ? "" : v;
    }
}