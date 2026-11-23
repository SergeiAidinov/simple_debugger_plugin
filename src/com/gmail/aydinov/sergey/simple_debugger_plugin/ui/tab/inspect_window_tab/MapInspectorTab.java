
package com.gmail.aydinov.sergey.simple_debugger_plugin.ui.tab.inspect_window_tab;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.ValueCategory;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.PairDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.MapEntryDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.details.UserInstanceDetailsDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.inspection.AbstractInspectionDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.inspection.MapPageDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebuggerEventTypes.SimpleDebuggerEventType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.SimpleDebuggerEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.UiEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.UIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.tooltip_manager.TooltipManager;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.window.SimpleDebugerWindowsManager;

public class MapInspectorTab implements InspectorTab {

    private Label mapNameLabel;
    private Label mapTypeLabel;
    private Label sizeLabel;
    private Label pageInfoLabel;

    private Button prevButton;
    private Text pageText;
    private Button goButton;
    private Button nextButton;

    private final UiEventCollector uiEventCollector =
            SimpleDebuggerEventCollector.instance();

    private final Composite root;
    private final TableViewer viewer;

    private TooltipManager tooltipManager;

    private MapEntryDTO lastInspected;
    private InnerElementRepresentationDTO anchorMap;

    private int currentPage = 0;
    private int totalPages = -1;

    // ================= UI =================

    public MapInspectorTab(Composite parent) {

        root = new Composite(parent, SWT.NONE);
        root.setLayout(new GridLayout(1, false));

        // ================= HEADER =================

        Composite header = new Composite(root, SWT.NONE);
        header.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        header.setLayout(new GridLayout(2, false));

        // ---- Information ----

        Composite info = new Composite(header, SWT.NONE);
        info.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        info.setLayout(new GridLayout(1, false));

        mapNameLabel = new Label(info, SWT.NONE);
        mapNameLabel.setText("Map: ");

        mapTypeLabel = new Label(info, SWT.NONE);
        mapTypeLabel.setText("Map type: ");

        sizeLabel = new Label(info, SWT.NONE);
        sizeLabel.setText("Size: ");

        pageInfoLabel = new Label(info, SWT.NONE);
        pageInfoLabel.setText("Page: ");

        // ---- Pagination ----

        Composite pagination = new Composite(header, SWT.NONE);
        pagination.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
        pagination.setLayout(new GridLayout(4, false));

        prevButton = new Button(pagination, SWT.PUSH);
        prevButton.setText("Prev");
        prevButton.addListener(SWT.Selection,
                e -> requestPage(Math.max(0, currentPage - 1)));

        pageText = new Text(pagination, SWT.BORDER);
        pageText.setLayoutData(new GridData(70, SWT.DEFAULT));

        goButton = new Button(pagination, SWT.PUSH);
        goButton.setText("Go");
        goButton.addListener(SWT.Selection, e -> requestPageFromText());

        pageText.addListener(SWT.DefaultSelection,
                e -> requestPageFromText());

        nextButton = new Button(pagination, SWT.PUSH);
        nextButton.setText("Next");
        nextButton.addListener(SWT.Selection,
                e -> requestPage(currentPage + 1));

        // ================= TABLE =================

        Table table = new Table(root,
                SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);

        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        viewer = new TableViewer(table);
        viewer.setContentProvider(ArrayContentProvider.getInstance());

        setupTooltips(table);
        setupHoverInspectionListener();
        setupClickListener(table);

        createColumn(
                "Key",
                200,
                p -> ((MapEntryDTO) p.getFirst()).getValue(),
                p -> getIcon((MapEntryDTO) p.getFirst()),
                0
        );

        createColumn(
                "Value",
                600,
                p -> ((MapEntryDTO) p.getSecond()).getValue(),
                p -> getIcon((MapEntryDTO) p.getSecond()),
                1
        );
    }

    // ================= PAGINATION =================

    private void requestPageFromText() {

        int page;

        try {
            page = Integer.parseInt(pageText.getText().trim());
        } catch (Exception e) {
            page = 0;
        }

        requestPage(Math.max(0, page));
    }

    private void requestPage(int page) {

        uiEventCollector.collectUiEvent(
                new UIEvent<>(
                        SimpleDebuggerEventType.USER_REQUESTED_MAP_PAGE,
                        PairDTO.of(anchorMap, page)
                )
        );
    }

    // ================= CLICK =================

    private void setupClickListener(Table table) {

        table.addListener(SWT.MouseDown, event -> {
        	System.out.println("MAP TAB: MOUSE DOWN");
            TableItem item = table.getItem(new Point(event.x, event.y));

            if (item == null)
                return;

            int colIndex = getColumnIndexAtPoint(table, event.x);

            if (colIndex < 0)
                return;

            if (!(item.getData() instanceof PairDTO<?, ?> pair))
                return;

            MapEntryDTO dto = getMapEntryForColumn(pair, colIndex);

            if (dto == null)
                return;

            ValueCategory category = dto.getValueCategory();

            if (category == null)
                return;

            switch (category) {

                case USER_OBJECT -> {

                    InnerElementRepresentationDTO innerDto =
                            InnerElementRepresentationDTO.InnerElementRepresentationDTOFactory
                                    .fromMapEntry(dto);

                    uiEventCollector.collectUiEvent(
                            new UIEvent<>(
                                    SimpleDebuggerEventType.USER_INSPECTS_USER_OBJECT_IN_INSPECTION_SEANCE,
                                    innerDto
                            )
                    );
                }

                case MAP -> {
                	System.out.println("MAP TAB: MAP CLICK EVENT");
                    InnerElementRepresentationDTO innerDto =
                            InnerElementRepresentationDTO.InnerElementRepresentationDTOFactory
                                    .fromMapEntry(dto);

                    uiEventCollector.collectUiEvent(
                            new UIEvent<>(
                                    SimpleDebuggerEventType.USER_REQUESTED_MAP_PAGE,
                                    PairDTO.of(innerDto, 0)
                            )
                    );
                }

                case COLLECTION -> {
                    InnerElementRepresentationDTO innerDto =
                            InnerElementRepresentationDTO.InnerElementRepresentationDTOFactory
                                    .fromMapEntry(dto);
                    uiEventCollector.collectUiEvent(
                            new UIEvent<>(
                                    SimpleDebuggerEventType.USER_REQUESTED_COLLECTION_PAGE,
                                    PairDTO.of(innerDto, 0)
                            )
                    );
                }

                default -> {
                    // No-op.
                }
            }
        });
    }

    // ================= HOVER =================

    private void setupHoverInspectionListener() {

        Table table = viewer.getTable();

        table.addListener(SWT.MouseMove, event -> {

            TableItem item = table.getItem(new Point(event.x, event.y));

            MapEntryDTO dto = null;
            int col = -1;
            if (item != null && item.getData() instanceof PairDTO<?, ?> pair) {

                 col = getColumnIndexAtPoint(table, event.x);

                dto = getMapEntryForColumn(pair, col);
            }

            if (Objects.equals(dto, lastInspected))
                return;

            lastInspected = dto;

            tooltipManager.closePopup();

            if (dto == null || dto.getValueCategory() == null)
                return;

            InnerElementRepresentationDTO innerDto =
                    InnerElementRepresentationDTO.InnerElementRepresentationDTOFactory
                            .fromMapEntry(dto);

            Point location = display().getCursorLocation();

            switch (dto.getValueCategory()) {

            case USER_OBJECT -> {
                tooltipManager.showTooltipForMapUserObject(dto, location);
            }

                case MAP, COLLECTION -> {

                    // Для Map и Collection используем существующий
                    // механизм TooltipManager.

                	tooltipManager.showTooltipForMapEntry(
                	        innerDto,
                	        col == 0 ? "Key" : "Value",
                	        location
                	);
                }

                default -> {
                    // Для остальных категорий popup не нужен.
                }
            }
        });
    }

    private MapEntryDTO getMapEntryForColumn(
            PairDTO<?, ?> pair,
            int columnIndex) {

        if (columnIndex == 0 && pair.getFirst() instanceof MapEntryDTO key)
            return key;

        if (columnIndex == 1 && pair.getSecond() instanceof MapEntryDTO value)
            return value;

        return null;
    }

    // ================= TOOLTIP =================

    private void setupTooltips(Table table) {

        tooltipManager = new TooltipManager(table, root);

        tooltipManager.setTooltipProvider(item -> {

            int col = TooltipManager.getColumnIndexAtPoint(
                    table,
                    table.getDisplay().getCursorLocation().x
                            - table.toDisplay(0, 0).x
            );

            Object tip = item.getData("tooltip_col_" + col);

            return tip instanceof String ? (String) tip : null;
        });
    }

    /**
     * Вызывается после получения подробной информации об объекте от backend.
     */
    @Override
    public void showFieldInfoPopupFromBackend(
            UserInstanceDetailsDTO userInstanceDetailsDTO) {

        if (userInstanceDetailsDTO == null)
            return;

        Display display = root.getDisplay();

        display.asyncExec(() -> {

            if (root.isDisposed())
                return;

            Point location = display.getCursorLocation();

            tooltipManager.showTooltipForUserObject(
                    userInstanceDetailsDTO,
                    location
            );
        });
    }

    // ================= UTILS =================

    private int getColumnIndexAtPoint(Table table, int x) {

        int offset = 0;

        for (int i = 0; i < table.getColumnCount(); i++) {

            offset += table.getColumn(i).getWidth();

            if (x < offset)
                return i;
        }

        return table.getColumnCount() - 1;
    }

    private Display display() {
        return root.getDisplay();
    }

    private Image getIcon(MapEntryDTO dto) {

        if (dto == null)
            return null;

        ValueCategory category = dto.getValueCategory();

        if (category == ValueCategory.COLLECTION
                || category == ValueCategory.MAP) {

            return SimpleDebugerWindowsManager.instance()
                    .icons.get("lens").getFirst();
        }

        if (category == ValueCategory.USER_OBJECT) {

            return SimpleDebugerWindowsManager.instance()
                    .icons.get("inspectIcon").getFirst();
        }

        return null;
    }

    // ================= COLUMNS =================

    private <K, V> void createColumn(
            String title,
            int width,
            Function<PairDTO<K, V>, String> text,
            Function<PairDTO<K, V>, Image> icon,
            int colIndex) {

        TableViewerColumn col = new TableViewerColumn(viewer, SWT.NONE);

        col.getColumn().setText(title);
        col.getColumn().setWidth(width);

        col.setLabelProvider(new ColumnLabelProvider() {

            @Override
            public String getText(Object element) {

                if (element instanceof PairDTO<?, ?> pair) {

                    @SuppressWarnings("unchecked")
                    PairDTO<K, V> typed = (PairDTO<K, V>) pair;

                    return text.apply(typed);
                }

                return "";
            }

            @Override
            public Image getImage(Object element) {

                if (!(element instanceof PairDTO<?, ?> pair))
                    return null;

                @SuppressWarnings("unchecked")
                PairDTO<K, V> typed = (PairDTO<K, V>) pair;

                return icon.apply(typed);
            }
        });
    }

    // ================= REQUIRED INTERFACE =================

    @Override
    public Composite getControl() {
        return root;
    }

    @Override
    public void showPage(AbstractInspectionDTO dto) {

        if (!(dto instanceof MapPageDTO mapPageDTO))
            return;

        root.getDisplay().asyncExec(() -> {

            if (root.isDisposed())
                return;

            anchorMap = mapPageDTO.getAnchorMap();

            String totalPagesString = mapPageDTO.getTotalPages();

            if (totalPagesString != null && totalPagesString.matches("\\d+"))
                totalPages = Integer.valueOf(totalPagesString);

            mapNameLabel.setText(
                    "Map name: " + safe(mapPageDTO.getElementName())
            );

            mapTypeLabel.setText(
                    "Map type: " + safe(mapPageDTO.getElementType())
            );

            sizeLabel.setText(
                    "Size: " + mapPageDTO.getTotalElements()
            );

            pageInfoLabel.setText(
                    "Page (0-based): "
                            + mapPageDTO.getCurrentPage()
                            + " of "
                            + mapPageDTO.getTotalPages()
                            + "   Showing: "
                            + mapPageDTO.getFromIndex()
                            + "–"
                            + mapPageDTO.getToIndex()
            );

            pageText.setText(
                    String.valueOf(mapPageDTO.getCurrentPage())
            );

            currentPage = mapPageDTO.getCurrentPage();

            prevButton.setEnabled(currentPage > 0);

            nextButton.setEnabled(
                    totalPages == -1 || currentPage < totalPages - 1
            );

            viewer.setInput(toPairs(mapPageDTO.getEntries()));

            lastInspected = null;

            root.layout(true, true);
        });
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private List<PairDTO<MapEntryDTO, MapEntryDTO>> toPairs(
            Map<MapEntryDTO, MapEntryDTO> map) {

        List<PairDTO<MapEntryDTO, MapEntryDTO>> list = new ArrayList<>();

        map.forEach((key, value) ->
                list.add(PairDTO.of(key, value))
        );

        return list;
    }
}
