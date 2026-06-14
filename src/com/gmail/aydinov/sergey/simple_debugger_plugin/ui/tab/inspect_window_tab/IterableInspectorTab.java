package com.gmail.aydinov.sergey.simple_debugger_plugin.ui.tab.inspect_window_tab;

import java.util.Objects;
import java.util.function.Function;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.ValueCategory;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.PairDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.details.UserInstanceDetailsDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.inspection.AbstractInspectionDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.inspection.ArrayPageDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebuggerEventTypes.SimpleDebuggerEventType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.SimpleDebuggerEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.UiEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.UIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.tooltip_manager.TooltipManager;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.utils.UiUtils;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.window.SimpleDebugerWindowsManager;

public class IterableInspectorTab implements InspectorTab {

    private final UiEventCollector uiEventCollector = SimpleDebuggerEventCollector.instance();
    private final Composite root;
    private final TableViewer viewer;

    private final Label collectionNameLabel;
    private final Label collectionTypeLabel;
    private final Label elementTypeLabel;
    private final Label sizeLabel;
    private final Label pageInfoLabel;

    private final Button prevButton;
    private final Text pageText;
    private final Button goButton;
    private final Button nextButton;

    private TooltipManager tooltipManager;

    // Hover popup
    private Shell elementsPopup;
    private final Listener popupCloseFilter = e -> closeElementsPopup();
    private InnerElementRepresentationDTO lastInspected;

    private int currentPage = 0;
    private long inspectableCollectionId;

    public IterableInspectorTab(Composite parent) {
        root = new Composite(parent, SWT.NONE);
        root.setLayout(new GridLayout(1, false));

        // ==================== HEADER ====================
        Composite headerComposite = new Composite(root, SWT.NONE);
        headerComposite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        headerComposite.setLayout(new GridLayout(2, false));

        // Левая часть — информация
        Composite infoComposite = new Composite(headerComposite, SWT.NONE);
        infoComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        infoComposite.setLayout(new GridLayout(1, false));

        collectionNameLabel = new Label(infoComposite, SWT.NONE);
        collectionTypeLabel = new Label(infoComposite, SWT.NONE);
        elementTypeLabel = new Label(infoComposite, SWT.NONE);
        sizeLabel = new Label(infoComposite, SWT.NONE);
        pageInfoLabel = new Label(infoComposite, SWT.NONE);

        // Правая часть — пагинация
        Composite paginationComposite = new Composite(headerComposite, SWT.NONE);
        paginationComposite.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
        paginationComposite.setLayout(new GridLayout(4, false));

        prevButton = new Button(paginationComposite, SWT.PUSH);
        prevButton.setText("Prev");
        prevButton.addListener(SWT.Selection, e -> requestPage(currentPage - 1));

        pageText = new Text(paginationComposite, SWT.BORDER);
        pageText.setLayoutData(new GridData(70, SWT.DEFAULT));

        goButton = new Button(paginationComposite, SWT.PUSH);
        goButton.setText("Go");
        goButton.addListener(SWT.Selection, e -> requestPage());
        pageText.addListener(SWT.DefaultSelection, e -> requestPage());

        nextButton = new Button(paginationComposite, SWT.PUSH);
        nextButton.setText("Next");
        nextButton.addListener(SWT.Selection, e -> requestPage(currentPage + 1));

        // ==================== TABLE ====================
        Table table = new Table(root, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        table.setLayoutData(new GridData(GridData.FILL_BOTH));

        viewer = new TableViewer(table);
        viewer.setContentProvider(ArrayContentProvider.getInstance());

        setupTooltips(table);
        setupHoverInspectionListener();
        setupClickListener(table);

        createColumn("Index", 80, pair -> String.valueOf(pair.getFirst()));
        createColumn("Value", 600,
                pair -> formatValue((InnerElementRepresentationDTO) pair.getSecond()),
                pair -> getIcon((InnerElementRepresentationDTO) pair.getSecond()));
    }

    // ==================== HOVER POPUP ====================
    private void showElementsPopup(InnerElementRepresentationDTO dto) {
        closeElementsPopup();
        if (dto == null || dto.getValueCategory() != ValueCategory.USER_OBJECT) return;

        Display display = root.getDisplay();
        Color bg = display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
        Color fg = display.getSystemColor(SWT.COLOR_WIDGET_FOREGROUND);

        Shell popup = new Shell(root.getShell(), SWT.ON_TOP | SWT.TOOL | SWT.NO_FOCUS);
        popup.setLayout(new FillLayout());
        popup.setBackground(bg);
        popup.setBackgroundMode(SWT.INHERIT_FORCE);

        ScrolledComposite scrolled = new ScrolledComposite(popup, SWT.V_SCROLL | SWT.H_SCROLL);
        scrolled.setExpandHorizontal(true);
        scrolled.setExpandVertical(true);

        Composite content = new Composite(scrolled, SWT.NONE);
        content.setLayout(new GridLayout(1, false));
        content.setBackground(bg);
        content.setForeground(fg);

        Label info = new Label(content, SWT.NONE);
        info.setText("Object ID: " + dto.getObjectId() +
                     "\nType: " + dto.getTypeOrReturnType() +
                     "\nValue: " + dto.getValue());
        info.setForeground(fg);

        content.pack();
        scrolled.setContent(content);
        scrolled.setMinSize(content.computeSize(SWT.DEFAULT, SWT.DEFAULT));

        popup.setSize(500, 350);
        Point loc = display.getCursorLocation();
        popup.setLocation(loc.x + 15, loc.y + 15);
        popup.open();

        elementsPopup = popup;
        display.addFilter(SWT.MouseDown, popupCloseFilter);
    }

    private void closeElementsPopup() {
        if (elementsPopup != null && !elementsPopup.isDisposed()) {
            Display display = elementsPopup.getDisplay();
            elementsPopup.dispose();
            if (display != null && !display.isDisposed()) {
                display.removeFilter(SWT.MouseDown, popupCloseFilter);
            }
        }
        elementsPopup = null;
    }

    private void setupHoverInspectionListener() {
        Table table = viewer.getTable();
        table.addListener(SWT.MouseMove, event -> {
            TableItem item = table.getItem(new Point(event.x, event.y));
            InnerElementRepresentationDTO dto = null;

            if (item != null && item.getData() instanceof PairDTO<?, ?> pair) {
                int col = getColumnIndexAtPoint(table, event.x);
                if (col == 1 && pair.getSecond() instanceof InnerElementRepresentationDTO value) {
                    dto = value;
                }
            }

            if (!Objects.equals(dto, lastInspected)) {
                lastInspected = dto;
                if (dto != null && isInspectable(dto.getValueCategory())) {
                    showElementsPopup(dto);
                }
            }
        });
    }

    private boolean isInspectable(ValueCategory category) {
        return category == ValueCategory.USER_OBJECT ||
               category == ValueCategory.MAP ||
               category == ValueCategory.COLLECTION;
    }

    private void setupClickListener(Table table) {
        table.addListener(SWT.MouseDown, event -> {
            TableItem item = table.getItem(new Point(event.x, event.y));
            if (item == null) return;

            int colIndex = getColumnIndexAtPoint(table, event.x);
            if (colIndex != 1) return;

            Object data = item.getData();
            if (!(data instanceof PairDTO<?, ?> pair)) return;
            if (!(pair.getSecond() instanceof InnerElementRepresentationDTO dto)) return;

            ValueCategory category = dto.getValueCategory();
            if (category == ValueCategory.USER_OBJECT) {
                uiEventCollector.collectUiEvent(new UIEvent<>(SimpleDebuggerEventType.USER_INSPECTS_USER_OBJECT, dto));
            } else if (category == ValueCategory.MAP) {
                uiEventCollector.collectUiEvent(new UIEvent<>(SimpleDebuggerEventType.USER_REQUESTED_MAP_PAGE, dto));
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

    private String formatValue(InnerElementRepresentationDTO dto) {
        if (dto == null) return "";
        String value = dto.getValue();
        if (value == null) return "null";
        String type = dto.getTypeOrReturnType();
        if ("String".equals(type)) return "\"" + value + "\"";
        return value + " (id=" + dto.getAdditionalInfo() + ")";
    }

    private Image getIcon(InnerElementRepresentationDTO dto) {
        if (dto == null) return null;
        ValueCategory category = dto.getValueCategory();
        if (category == ValueCategory.COLLECTION || category == ValueCategory.MAP) {
            return SimpleDebugerWindowsManager.instance().icons.get("lens").getFirst();
        }
        if (category == ValueCategory.USER_OBJECT && dto.getObjectId() != null
                && !UiUtils.isStandartJavaType(dto.getTypeOrReturnType())) {
            return SimpleDebugerWindowsManager.instance().icons.get("inspectIcon").getFirst();
        }
        return null;
    }

    private void setupTooltips(Table table) {
        tooltipManager = new TooltipManager(table, root);
        tooltipManager.setTooltipProvider(item -> {
            int col = TooltipManager.getColumnIndexAtPoint(table,
                    table.getDisplay().getCursorLocation().x - table.toDisplay(0, 0).x);
            Object tip = item.getData("tooltip_col_" + col);
            return tip instanceof String ? (String) tip : null;
        });
    }

    private <K, V> TableViewerColumn createColumn(String title, int width,
            Function<PairDTO<K, V>, String> textExtractor,
            Function<PairDTO<K, V>, Image> imageExtractor) {
        TableViewerColumn column = new TableViewerColumn(viewer, SWT.NONE);
        column.getColumn().setText(title);
        column.getColumn().setWidth(width);
        column.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                if (element instanceof PairDTO<?, ?> pair) {
                    @SuppressWarnings("unchecked")
                    PairDTO<K, V> typed = (PairDTO<K, V>) pair;
                    return textExtractor.apply(typed);
                }
                return "";
            }

            @Override
            public Image getImage(Object element) {
                if (!(element instanceof PairDTO<?, ?> pair)) return null;
                @SuppressWarnings("unchecked")
                PairDTO<K, V> typed = (PairDTO<K, V>) pair;
                return imageExtractor.apply(typed);
            }
        });
        return column;
    }

    private <K, V> TableViewerColumn createColumn(String title, int width,
            Function<PairDTO<K, V>, String> textExtractor) {
        return createColumn(title, width, textExtractor, p -> null);
    }

    private void requestPage(int page) {
        if (page < 0) page = 0;
        uiEventCollector.collectUiEvent(
            new UIEvent<>(SimpleDebuggerEventType.USER_REQUESTED_COLLECTION_PAGE,
                    PairDTO.of(inspectableCollectionId, page)));
    }

    private void requestPage() {
        int pageNumber;
        try {
            pageNumber = Integer.parseInt(pageText.getText().trim());
        } catch (Exception e) {
            pageNumber = 0;
        }
        requestPage(pageNumber);
    }

    @Override
    public Composite getControl() {
        return root;
    }

    @Override
    public void showPage(AbstractInspectionDTO abstractInspectionDTO) {
        if (!(abstractInspectionDTO instanceof ArrayPageDTO page)) return;

        inspectableCollectionId = page.getObjectId();

        root.getDisplay().asyncExec(() -> {
            if (root.isDisposed() || viewer.getTable().isDisposed()) return;

            collectionNameLabel.setText("Collection name: " + safe(page.getElementName()));
            collectionTypeLabel.setText("Collection type: " + safe(page.getElementType()));
            elementTypeLabel.setText("Element type: " + safe(page.getElementType()));
            sizeLabel.setText("Size: " + page.getTotalElements());

            pageInfoLabel.setText("Page (0-based): " + page.getCurrentPage() + " of " + page.getTotalPages()
                    + " Showing: " + page.getFromIndex() + "–" + page.getToIndex());

            pageText.setText(String.valueOf(page.getCurrentPage()));
            currentPage = page.getCurrentPage();

            prevButton.setEnabled(page.hasPreviousPage());

            viewer.setInput(
                page.getEntries().entrySet().stream()
                    .map(e -> PairDTO.of(
                        e.getKey(),
                        InnerElementRepresentationDTO.InnerElementRepresentationDTOFactory.fromElement(e.getValue())
                    ))
                    .toList()
            );

            root.layout(true, true);
        });
    }

    @Override
    public void showFieldInfoPopupFromBackend(UserInstanceDetailsDTO userInstanceInspectionDTO) {
        if (userInstanceInspectionDTO == null) return;
        Display display = root.getDisplay();
        display.asyncExec(() -> {
            if (root.isDisposed()) return;
            Point location = display.getCursorLocation();
            tooltipManager.showTooltipForUserObject(userInstanceInspectionDTO, location);
        });
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}