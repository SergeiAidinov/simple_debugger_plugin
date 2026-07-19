package com.gmail.aydinov.sergey.simple_debugger_plugin.ui.tab.inspect_window_tab;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.holder.CurrentlyInspectedObjectIdHolder;
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
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.window.LoadingWindow;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.window.SimpleDebugerWindowsManager;
import com.gmail.aydinov.sergey.simple_debugger_plugin.utils.DebugUtils;

public class MapInspectorTab implements InspectorTab {
	
	private final int debugId = System.identityHashCode(this);

	private Label mapNameLabel;
	private Label mapTypeLabel;
	private Label sizeLabel;
	private Label pageInfoLabel;

	private Button prevButton;
	private Text pageText;
	private Button goButton;
	private Button nextButton;

	private final UiEventCollector uiEventCollector = SimpleDebuggerEventCollector.instance();

	private final Composite root;
	private final TableViewer viewer;

	private Shell elementsPopup;
	private final Listener popupCloseFilter = e -> closeElementsPopup();

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

		// ---- левая часть (инфа) ----
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

		// ---- правая часть (pagination) ----
		Composite pagination = new Composite(header, SWT.NONE);
		pagination.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
		pagination.setLayout(new GridLayout(4, false));

		prevButton = new Button(pagination, SWT.PUSH);
		prevButton.setText("Prev");
		prevButton.addListener(SWT.Selection, e -> requestPage((currentPage - 1) < 0 ? 0 : (currentPage - 1)));

		pageText = new Text(pagination, SWT.BORDER);
		pageText.setLayoutData(new GridData(70, SWT.DEFAULT));

		goButton = new Button(pagination, SWT.PUSH);
		goButton.setText("Go");
		goButton.addListener(SWT.Selection, e -> requestPageFromText());
		pageText.addListener(SWT.DefaultSelection, e -> requestPageFromText());

		nextButton = new Button(pagination, SWT.PUSH);
		nextButton.setText("Next");
		nextButton.addListener(SWT.Selection, e -> requestPage(currentPage + 1));

		Table table = new Table(root, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		table.addListener(SWT.MouseHover, e -> {
		});

		viewer = new TableViewer(table);
		viewer.setContentProvider(ArrayContentProvider.getInstance());

		setupTooltips(table);
		setupHoverInspectionListener();
		setupClickListener(table);

		createColumn("Key", 200, p -> ((MapEntryDTO) p.getFirst()).getValue(), p -> getIcon((MapEntryDTO) p.getFirst()),
				0);

		createColumn("Value", 600, p -> ((MapEntryDTO) p.getSecond()).getValue(),
				p -> getIcon((MapEntryDTO) p.getSecond()), 1);
	}

	// ================= POPUP =================

	private void requestPageFromText() {
		int page;
		try {
			page = Integer.parseInt(pageText.getText().trim());
		} catch (Exception e) {
			page = 0;
		}
		if (page < 0)
			page = 0;

		requestPage(page);
	}

	private void requestPage(int page) {
		uiEventCollector.collectUiEvent(
				new UIEvent<>(SimpleDebuggerEventType.USER_REQUESTED_MAP_PAGE, PairDTO.of(anchorMap, page)));
	}

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
				closeElementsPopup();
				InnerElementRepresentationDTO innerElementRepresentationDTO = InnerElementRepresentationDTO.InnerElementRepresentationDTOFactory.fromMapEntry(mapDto);
				uiEventCollector
						.collectUiEvent(new UIEvent<>(SimpleDebuggerEventType.USER_INSPECTS_USER_OBJECT, innerElementRepresentationDTO));
			}
			case MAP -> {
				uiEventCollector.collectUiEvent(new UIEvent<>(SimpleDebuggerEventType.USER_REQUESTED_MAP_PAGE,
						PairDTO.of(
								InnerElementRepresentationDTO.InnerElementRepresentationDTOFactory.fromMapEntry(mapDto),
								0)));
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

		Color bg = display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
		Color fg = display.getSystemColor(SWT.COLOR_WIDGET_FOREGROUND);

		Shell popup = new Shell(root.getShell(), SWT.ON_TOP | SWT.TOOL | SWT.NO_FOCUS);
		popup.setLayout(new FillLayout());
		popup.setBackground(bg);
		popup.setBackgroundMode(SWT.INHERIT_FORCE);

		// ================= SCROLLABLE AREA =================
		ScrolledComposite scrolled = new ScrolledComposite(popup, SWT.V_SCROLL | SWT.H_SCROLL);

		scrolled.setExpandHorizontal(true);
		scrolled.setExpandVertical(true);

		Composite content = new Composite(scrolled, SWT.NONE);
		content.setLayout(new GridLayout(1, false));
		content.setBackground(bg);
		content.setForeground(fg);

		// ================= CONTENT =================
		List<InnerElementRepresentationDTO> sortedList = new ArrayList<InnerElementRepresentationDTO>(
				dto.getElements());
		sortedList.sort(Comparator.comparingInt(e -> DebugUtils.SORT_ORDER.getOrDefault(e.getElementType(), DebugUtils.SORT_ORDER.size())));
		for (InnerElementRepresentationDTO el : sortedList) {
			Label row = new Label(content, SWT.NONE);
			row.setText(formatElement(el));
			row.setBackground(bg);
			row.setForeground(fg);
			row.addListener(SWT.MouseDown, e -> {
				System.out.println("CLICK EVENT: " + dto.getValueCategory());
				uiEventCollector.collectUiEvent(new UIEvent<>(SimpleDebuggerEventType.USER_INSPECTS_USER_OBJECT, dto));
			});
		}

		content.pack();

		scrolled.setContent(content);

		// 🔥 ВАЖНО: это включает реальные scrollbars
		scrolled.setMinSize(content.computeSize(SWT.DEFAULT, SWT.DEFAULT));

		// ================= SIZE / POSITION =================
		popup.setSize(500, 350);

		Point loc = display.getCursorLocation();
		popup.setLocation(loc.x + 15, loc.y + 15);

		popup.open();

		elementsPopup = popup;

		display.addFilter(SWT.MouseDown, popupCloseFilter);
	}

	private void closeElementsPopup() {

		if (elementsPopup != null && !elementsPopup.isDisposed()) {
			Display display = elementsPopup.getDisplay(); // безопаснее, чем root
			elementsPopup.dispose();

			if (display != null && !display.isDisposed()) {
				display.removeFilter(SWT.MouseDown, popupCloseFilter);
			}
		}

		elementsPopup = null;
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

				if (dto != null && dto.getElements() != null && !dto.getElements().isEmpty()
						&& isInspectable(dto.getValueCategory())) {

					Point loc = display().getCursorLocation();
					showElementsPopup(dto, loc);
				}
			}
		});
	}

	private boolean isInspectable(ValueCategory category) {
		return category == ValueCategory.USER_OBJECT || category == ValueCategory.MAP
				|| category == ValueCategory.COLLECTION;
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
		if (el == null)
			return "null";
		return el.getElementName() + " : " + el.getTypeOrReturnType() + " = " + el.getValue();
	}

	private Image getIcon(MapEntryDTO dto) {

		if (dto == null)
			return null;

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

	private <K, V> void createColumn(String title, int width, Function<PairDTO<K, V>, String> text,
			Function<PairDTO<K, V>, Image> icon, int colIndex) {

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
				if (!(element instanceof PairDTO<?, ?> p))
					return null;

				Image img = icon.apply((PairDTO<K, V>) p);

				TableItem item = findItem(p);
				if (item != null && p.getFirst() instanceof MapEntryDTO dto) {
//					item.setData("tooltip_col_" + colIndex,
//							dto.getObjectId() + " / " + dto.getValueCategory());
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
		if (!(dto instanceof MapPageDTO mapPageDTO))
			return;
		root.getDisplay().asyncExec(() -> {
			if (root.isDisposed())
				return;

			anchorMap = mapPageDTO.getAnchorMap();
			String totalpagesString = mapPageDTO.getTotalPages();
			if (totalpagesString.matches("\\d+"))
				totalPages = Integer.valueOf(totalpagesString);

			mapNameLabel.setText("Map name: " + safe(mapPageDTO.getElementName()));
			mapTypeLabel.setText("Map type: " + safe(mapPageDTO.getElementType()));
			sizeLabel.setText("Size: " + mapPageDTO.getTotalElements());

			pageInfoLabel.setText("Page (0-based): " + mapPageDTO.getCurrentPage() + " of " + mapPageDTO.getTotalPages()
					+ "   Showing: " + mapPageDTO.getFromIndex() + "–" + mapPageDTO.getToIndex());

			pageText.setText(String.valueOf(mapPageDTO.getCurrentPage()));

			currentPage = mapPageDTO.getCurrentPage();

			prevButton.setEnabled(currentPage > 0);
			nextButton.setEnabled((totalPages == -1) || currentPage < totalPages - 1);

			viewer.setInput(toPairs(mapPageDTO.getEntries()));
			root.layout(true, true);
		});
	}

	private String safe(String value) {
		return value == null ? "" : value;
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