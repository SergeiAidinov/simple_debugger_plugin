package com.gmail.aydinov.sergey.simple_debugger_plugin.ui.tab.inspect_window_tab;

import java.util.*;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Function;

import org.eclipse.jface.viewers.*;
import org.eclipse.swt.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

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
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.utils.UiUtils;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.window.SimpleDebugerWindowsManager;

public class MapInspectorTab implements InspectorTab {

	private final UiEventCollector uiEventCollector = SimpleDebuggerEventCollector.instance();

	private final Composite root;
	private final TableViewer viewer;

	private Shell elementsPopup;
	private final Listener popupCloseFilter = e -> closeElementsPopup();

	private TooltipManager tooltipManager;

	private MapEntryDTO lastInspected;
	private InnerElementRepresentationDTO anchorMap;
	private int currentPage = 0;

	// ================= UI =================

	public MapInspectorTab(Composite parent) {

		root = new Composite(parent, SWT.NONE);
		root.setLayout(new GridLayout(1, false));

		Table table = new Table(root, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		viewer = new TableViewer(table);
		viewer.setContentProvider(ArrayContentProvider.getInstance());

		setupTooltips(table);
		setupHoverInspectionListener();
		setupClickListener(table);

		createColumn("Key", 200, p -> ((MapEntryDTO) p.getFirst()).getValue(),
				p -> getIcon((MapEntryDTO) p.getFirst()), 0);

		createColumn("Value", 600, p -> ((MapEntryDTO) p.getSecond()).getValue(),
				p -> getIcon((MapEntryDTO) p.getSecond()), 1);
	}

	// ================= POPUP =================

	private void setupClickListener(Table table) {

	    table.addListener(SWT.MouseDown, event -> {

	        TableItem item = table.getItem(new Point(event.x, event.y));
	        if (item == null) {
	            return;
	        }

	        int colIndex = getColumnIndexAtPoint(table, event.x);
	        if (colIndex < 0) {
	            return;
	        }

	        Object data = item.getData();
	        if (!(data instanceof PairDTO<?, ?> pair)) {
	            return;
	        }

	        Object dto = null;

	        // колонка KEY
	        if (colIndex == 0 && pair.getFirst() instanceof MapEntryDTO key) {
	            dto = key;
	        }
	        // колонка VALUE
	        else if (colIndex == 1 && pair.getSecond() instanceof MapEntryDTO value) {
	            dto = value;
	        } else {
	            return;
	        }

	        if (!(dto instanceof MapEntryDTO mapDto)) {
	            return;
	        }

	        ValueCategory category = mapDto.getValueCategory();

	        if (category == null) {
	            return;
	        }

	        switch (category) {

	            case USER_OBJECT -> {
	                uiEventCollector.collectUiEvent(
	                        new UIEvent<>(
	                                SimpleDebuggerEventType.USER_INSPECTS_USER_OBJECT,
	                                mapDto
	                        )
	                );
	            }

	            case MAP -> {
	                uiEventCollector.collectUiEvent(
	                        new UIEvent<>(
	                                SimpleDebuggerEventType.USER_REQUESTED_MAP_PAGE,
	                                PairDTO.of(mapDto, 0)
	                        )
	                );
	            }

	            case COLLECTION -> {
	                // пока ничего или можно открыть инспектор
	            }

	            default -> {
	                // no-op
	            }
	        }
	    });
	}

	private void showElementsPopup(MapEntryDTO dto, Point location) {

	    closeElementsPopup();

	    if (dto == null || dto.getElements() == null || dto.getElements().isEmpty())
	        return;

	    Display display = root.getDisplay();

	    Color bg = display.getSystemColor(SWT.COLOR_INFO_BACKGROUND);
	    Color fg = display.getSystemColor(SWT.COLOR_INFO_FOREGROUND);

	    Shell popup = new Shell(root.getShell(), SWT.ON_TOP | SWT.TOOL | SWT.NO_FOCUS);
	    popup.setBackground(bg);

	    // ❗ ВАЖНО: без layout вообще
	    popup.setLayout(null);

	    org.eclipse.swt.widgets.List list =
	            new org.eclipse.swt.widgets.List(popup, SWT.V_SCROLL | SWT.H_SCROLL);

	    list.setBackground(bg);
	    list.setForeground(fg);

	    // ❗ ВАЖНО: manual sizing
	    list.setBounds(0, 0, 450, 300);

	    for (InnerElementRepresentationDTO el : dto.getElements()) {
	        list.add(formatElement(el));
	    }

	    popup.setSize(450, 300);

	    Point loc = display.getCursorLocation();
	    popup.setLocation(loc.x + 15, loc.y + 15);

	    popup.open();

	    elementsPopup = popup;

	    display.addFilter(SWT.MouseDown, popupCloseFilter);
	}

	private void closeElementsPopup() {
		if (elementsPopup != null && !elementsPopup.isDisposed()) {
			elementsPopup.dispose();
		}
		elementsPopup = null;
		root.getDisplay().removeFilter(SWT.MouseDown, popupCloseFilter);
	}

	// ================= HOVER =================

	private void setupHoverInspectionListener() {

		Table table = viewer.getTable();

		table.addListener(SWT.MouseMove, event -> {

			TableItem item = table.getItem(new Point(event.x, event.y));

			MapEntryDTO dto = null;

			if (item != null && item.getData() instanceof PairDTO<?, ?> pair) {

				int col = getColumnIndexAtPoint(table, event.x);

				if (col == 0 && pair.getFirst() instanceof MapEntryDTO k)
					dto = k;

				else if (col == 1 && pair.getSecond() instanceof MapEntryDTO v)
					dto = v;
			}

			if (!Objects.equals(dto, lastInspected)) {
				lastInspected = dto;

				tooltipManager.closePopup();

				if (dto != null && dto.getElements() != null && !dto.getElements().isEmpty()) {
					Point loc = display().getCursorLocation();
					showElementsPopup(dto, loc);
				}
			}
		});
	}

	// ================= UTILS =================

	private int getColumnIndexAtPoint(Table table, int x) {
	    int offset = 0;

	    for (int i = 0; i < table.getColumnCount(); i++) {
	        offset += table.getColumn(i).getWidth();
	        if (x < offset) {
	            return i;
	        }
	    }

	    return table.getColumnCount() - 1;
	}

	private Display display() {
		return root.getDisplay();
	}

	private String formatElement(InnerElementRepresentationDTO el) {
		if (el == null) return "null";
		return el.getElementName() + " : " + el.getTypeOrReturnType() + " = " + el.getValue();
	}

	private Image getIcon(MapEntryDTO dto) {

		if (dto == null) return null;

		ValueCategory cat = dto.getValueCategory();

		if (cat == ValueCategory.COLLECTION || cat == ValueCategory.MAP)
			return SimpleDebugerWindowsManager.instance().icons.get("lens").getFirst();

		if (cat == ValueCategory.USER_OBJECT)
			return SimpleDebugerWindowsManager.instance().icons.get("inspectIcon").getFirst();

		return null;
	}

	// ================= TOOLTIP =================

	private void setupTooltips(Table table) {
		tooltipManager = new TooltipManager(table, root);

		tooltipManager.setTooltipProvider(item -> {
			int col = TooltipManager.getColumnIndexAtPoint(table,
					table.getDisplay().getCursorLocation().x - table.toDisplay(0, 0).x);

			Object tip = item.getData("tooltip_col_" + col);
			return tip instanceof String ? (String) tip : null;
		});
	}

	// ================= COLUMN =================

	private <K, V> void createColumn(String title, int width,
			Function<PairDTO<K, V>, String> text,
			Function<PairDTO<K, V>, Image> icon,
			int colIndex) {

		TableViewerColumn col = new TableViewerColumn(viewer, SWT.NONE);
		col.getColumn().setText(title);
		col.getColumn().setWidth(width);

		col.setLabelProvider(new ColumnLabelProvider() {

			@Override
			public String getText(Object element) {
				if (element instanceof PairDTO<?, ?> p)
					return text.apply((PairDTO<K, V>) p);
				return "";
			}

			@Override
			public Image getImage(Object element) {
				if (!(element instanceof PairDTO<?, ?> p)) return null;

				Image img = icon.apply((PairDTO<K, V>) p);

				TableItem item = findItem(p);
				if (item != null && p.getFirst() instanceof MapEntryDTO dto) {
					item.setData("tooltip_col_" + colIndex,
							dto.getObjectId() + " / " + dto.getValueCategory());
				}

				return img;
			}
		});
	}

	private TableItem findItem(Object data) {
		for (TableItem i : viewer.getTable().getItems())
			if (i.getData() == data)
				return i;
		return null;
	}

	// ================= REQUIRED INTERFACE =================

	@Override
	public Composite getControl() {
		return root;
	}

	@Override
	public void showPage(AbstractInspectionDTO dto) {
		if (!(dto instanceof MapPageDTO page))
			return;

		root.getDisplay().asyncExec(() -> {
			if (root.isDisposed()) return;

			viewer.setInput(toPairs(page.getEntries()));
			root.layout(true, true);
		});
	}

	private List<PairDTO<MapEntryDTO, MapEntryDTO>> toPairs(Map<MapEntryDTO, MapEntryDTO> map) {
		List<PairDTO<MapEntryDTO, MapEntryDTO>> list = new ArrayList<>();
		map.forEach((k, v) -> list.add(PairDTO.of(k, v)));
		return list;
	}

	@Override
	public void showFieldInfoPopupFromBackend(UserInstanceDetailsDTO userInstanceDetailsDTO) {
		// TODO Auto-generated method stub
		
	}
}