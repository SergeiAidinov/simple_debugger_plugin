package com.gmail.aydinov.sergey.simple_debugger_plugin.ui.tab.inspect_window_tab;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation;
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

	private final Label mapNameLabel;
	private final Label mapTypeLabel;
	private final Label sizeLabel;
	private final Label pageInfoLabel;

	private final Button prevButton;
	private final Text pageText;
	private final Button goButton;
	private final Button nextButton;
	private Shell currentPopup;
	private int currentPage = 0;
	private InnerElementRepresentationDTO anchorMap;
	private TooltipManager tooltipManager;
	private MapEntryDTO lastInspected;
	private Shell elementsPopup;

	public MapInspectorTab(Composite parent) {
		root = new Composite(parent, SWT.NONE);
		root.setLayout(new GridLayout(1, false));

		// ================= HEADER =================
		Composite header = new Composite(root, SWT.NONE);
		header.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		header.setLayout(new GridLayout(2, false));

		Composite info = new Composite(header, SWT.NONE);
		info.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		info.setLayout(new GridLayout(1, false));

		mapNameLabel = new Label(info, SWT.NONE);
		mapTypeLabel = new Label(info, SWT.NONE);
		sizeLabel = new Label(info, SWT.NONE);
		pageInfoLabel = new Label(info, SWT.NONE);

		Composite pagination = new Composite(header, SWT.NONE);
		pagination.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
		pagination.setLayout(new GridLayout(4, false));

		prevButton = new Button(pagination, SWT.PUSH);
		prevButton.setText("Prev");

		pageText = new Text(pagination, SWT.BORDER);
		pageText.setLayoutData(new GridData(70, SWT.DEFAULT));

		goButton = new Button(pagination, SWT.PUSH);
		goButton.setText("Go");

		nextButton = new Button(pagination, SWT.PUSH);
		nextButton.setText("Next");

		// ================= TABLE =================
		Table table = new Table(root, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		// ⚠️ ВАЖНО: viewer создаётся ДО любых методов, где он используется
		viewer = new TableViewer(table);
		viewer.setContentProvider(ArrayContentProvider.getInstance());

		// ================= TOOLING =================
		setupTooltips(table);

		// теперь viewer уже НЕ null
		setupHoverInspectionListener();
		setupClickListener(table);

		// ================= COLUMNS =================
		createColumn("Key", 200, pair -> ((MapEntryDTO) pair.getFirst()).getValue(),
				pair -> getIcon((MapEntryDTO) pair.getFirst()), 0);

		createColumn("Value", 600, pair -> ((MapEntryDTO) pair.getSecond()).getValue(),
				pair -> getIcon((MapEntryDTO) pair.getSecond()), 1);
	}

	private void setupHoverInspectionListener() {
		Table table = viewer.getTable();

		table.addListener(SWT.MouseMove, event -> {
			TableItem item = table.getItem(new Point(event.x, event.y));

			MapEntryDTO dto = null;

			if (item != null && item.getData() instanceof PairDTO<?, ?> pair) {

				int colIndex = getColumnIndexAtPoint(table, event.x);

				if (colIndex == 0 && pair.getFirst() instanceof MapEntryDTO key) {
					dto = key;
				} else if (colIndex == 1 && pair.getSecond() instanceof MapEntryDTO value) {
					dto = value;
				}
			}

			if (!java.util.Objects.equals(dto, lastInspected)) {
				lastInspected = dto;
				tooltipManager.closePopup();

				if (dto != null) {
					ValueCategory category = dto.getValueCategory();

					if (category == ValueCategory.MAP || category == ValueCategory.COLLECTION) {
						Display display = root.getDisplay();
						Point location = display.getCursorLocation();

						// 👉 нужно обернуть в InnerElementRepresentationDTO если хочешь reuse
						tooltipManager.showTooltipForCollection(convertToInner(dto), location);
					}
				}
			}

			if (dto != null && dto.getElements() != null && !dto.getElements().isEmpty()) {

				Display display = root.getDisplay();
				Point location = display.getCursorLocation();

				showElementsPopup(dto, location);
			}
		});
	}

	private InnerElementRepresentationDTO convertToInner(MapEntryDTO dto) {
		return InnerElementRepresentationDTO.InnerElementRepresentationDTOFactory.fromMapEntry(dto);
	}

	private void showElementsPopup(MapEntryDTO dto, Point location) {
		closeElementsPopup();

		if (dto == null || dto.getElements() == null)
			return;

		Display display = root.getDisplay();

		elementsPopup = new Shell(display, SWT.ON_TOP | SWT.TOOL | SWT.BORDER);
		elementsPopup.setLayout(new GridLayout(1, false));

		org.eclipse.swt.widgets.List list = new org.eclipse.swt.widgets.List(elementsPopup, SWT.BORDER | SWT.V_SCROLL);

		list.setLayoutData(new GridData(450, 300));

		for (InnerElementRepresentationDTO el : dto.getElements()) {
			list.add(formatElement(el));
		}

		elementsPopup.pack();
		elementsPopup.setLocation(location.x + 15, location.y + 15);
		elementsPopup.open();

		// автозакрытие по клику вне
		display.addFilter(SWT.MouseDown, e -> closeElementsPopup());
	}

	private String formatElement(InnerElementRepresentationDTO el) {
		if (el == null)
			return "null";

		String name = el.getElementName();
		String type = el.getTypeOrReturnType();
		String value = el.getValue();

		if (value == null)
			value = "null";

		return name + " : " + type + " = " + value;
	}

	private void setupTooltips(Table table) {
		TooltipManager tooltipManager = new TooltipManager(table, root);

		tooltipManager.setTooltipProvider(item -> {
			int col = TooltipManager.getColumnIndexAtPoint(table,
					table.getDisplay().getCursorLocation().x - table.toDisplay(0, 0).x);

			Object tip = item.getData("tooltip_col_" + col);
			return tip instanceof String ? (String) tip : null;
		});

		this.tooltipManager = tooltipManager;
	}

	private Image getIcon(MapEntryDTO dto) {
		if (dto == null)
			return null;

		ValueCategory category = dto.getValueCategory();

		if (category == ValueCategory.COLLECTION || category == ValueCategory.MAP)
			return SimpleDebugerWindowsManager.instance().icons.get("lens").getFirst();

		if (category == ValueCategory.USER_OBJECT)
			return SimpleDebugerWindowsManager.instance().icons.get("inspectIcon").getFirst();

		return null;
	}

//	private <K, V> TableViewerColumn createColumn(String title, int width,
//			Function<PairDTO<K, V>, String> textExtractor) {
//		return createColumn(title, width, textExtractor, pair -> null);
//	}

	private Image getIcon(InnerElementRepresentationDTO dto) {
		if (dto == null)
			return null;

		ValueCategory category = dto.getValueCategory();
		if (category == ValueCategory.COLLECTION || category == ValueCategory.MAP)
			return SimpleDebugerWindowsManager.instance().icons.get("lens").getFirst();

		if (category == ValueCategory.USER_OBJECT && dto.getValue() != null
				&& !UiUtils.isStandartJavaType(dto.getTypeOrReturnType()))
			return SimpleDebugerWindowsManager.instance().icons.get("inspectIcon").getFirst();
		return null;
	}

	private String formatValue(InnerElementRepresentationDTO dto) {
		if (dto == null)
			return "";
		String value = dto.getValue();
		if (value == null)
			return "null";
		String type = dto.getTypeOrReturnType();
		if ("String".equals(type)) {
			return "\"" + value + "\"";
		}
		return value + " (id=" + dto.getAdditionalInfo() + ")";
	}

	private void setupClickListener(Table table) {

		table.addListener(SWT.MouseDown, event -> {
			TableItem item = table.getItem(new Point(event.x, event.y));
			if (item == null)
				return;

			int colIndex = getColumnIndexAtPoint(table, event.x);

			Object data = item.getData();
			if (!(data instanceof PairDTO<?, ?> pair))
				return;

			MapEntryDTO dto = null;

			// 👉 определяем, по какой колонке клик
			if (colIndex == 0 && pair.getFirst() instanceof MapEntryDTO keyDto) {
				dto = keyDto;
			} else if (colIndex == 1 && pair.getSecond() instanceof MapEntryDTO valueDto) {
				dto = valueDto;
			} else {
				return;
			}

			if (dto == null)
				return;

			ValueCategory category = dto.getValueCategory();

			// 👉 MAP
//	        if (category == ValueCategory.MAP) {
//	            uiEventCollector.collectUiEvent(
//	                new UIEvent<>(
//	                    SimpleDebuggerEventType.USER_REQUESTED_MAP_PAGE,
//	                    dto // или dto целиком, зависит от backend
//	                )
//	            );
//	            return;
//	        }

			// 👉 USER OBJECT
			if (category == ValueCategory.USER_OBJECT) {
				uiEventCollector.collectUiEvent(new UIEvent<>(SimpleDebuggerEventType.USER_INSPECTS_USER_OBJECT, dto));
				return;
			}

			// 👉 COLLECTION (можно потом добавить поддержку)
			if (category == ValueCategory.COLLECTION) {
				return;
			}
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

	private Image getIconForValue(InnerElementRepresentationDTO dto) {
		if (dto == null)
			return null;

		ValueCategory category = dto.getValueCategory();
		if (category == null)
			return null;

		if (category == ValueCategory.COLLECTION || category == ValueCategory.MAP) {
			return SimpleDebugerWindowsManager.instance().icons.get("lens").getFirst();
		}

		if (category == ValueCategory.USER_OBJECT && dto.getValue() != null
				&& !UiUtils.isStandartJavaType(dto.getTypeOrReturnType())) {
			return SimpleDebugerWindowsManager.instance().icons.get("inspectIcon").getFirst();
		}

		return null;
	}

	private String getTooltipForValue(InnerElementRepresentationDTO dto) {
		if (dto == null)
			return "null";
		var pair = UiUtils.getTypeTooltip(dto);
		return pair != null ? pair.getSecond() : dto.getTypeOrReturnType();
	}

	@Override
	public Composite getControl() {
		return root;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void showPage(AbstractInspectionDTO abstractInspectionDTO) {
		if (!(abstractInspectionDTO instanceof MapPageDTO))
			return;
		MapPageDTO<InnerElementRepresentationDTO, InnerElementRepresentationDTO> page = (MapPageDTO<InnerElementRepresentationDTO, InnerElementRepresentationDTO>) abstractInspectionDTO;
		anchorMap = page.getAnchorMap();
		root.getDisplay().asyncExec(() -> {
			if (root.isDisposed() || viewer.getTable().isDisposed())
				return;

			mapNameLabel.setText("Map name: " + safe(page.getElementName()));
			mapTypeLabel.setText("Map type: " + safe(page.getElementType()));
			sizeLabel.setText("Size: " + page.getTotalElements());
			pageInfoLabel.setText("Page (0-based): " + page.getCurrentPage() + " of " + page.getTotalPages()
					+ "   Showing: " + page.getFromIndex() + "–" + page.getToIndex());

			pageText.setText(String.valueOf(page.getCurrentPage()));
			currentPage = page.getCurrentPage();
			prevButton.setEnabled(page.getCurrentPage() > 0);
			// nextButton.setEnabled(page.getCurrentPage() <
			// Integer.valueOf(page.getTotalPages()) - 1);
			nextButton.setEnabled(true);

			viewer.setInput(toPairs(page.getEntries()));
			root.layout(true, true);
		});
	}

	private List<PairDTO<MapEntryDTO, MapEntryDTO>> toPairs(Map<MapEntryDTO, MapEntryDTO> map) {
		List<PairDTO<MapEntryDTO, MapEntryDTO>> lines = new ArrayList<>();

		for (Entry<MapEntryDTO, MapEntryDTO> entry : map.entrySet()) {
			lines.add(PairDTO.of(entry.getKey(), entry.getValue()));
		}

		return lines;
	}

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

	private void requestPage(int pageNumber) {
		uiEventCollector.collectUiEvent(
				new UIEvent<>(SimpleDebuggerEventType.USER_REQUESTED_MAP_PAGE, PairDTO.of(anchorMap, pageNumber)));
	}

//	private <K, V> TableViewerColumn createColumn(String title, int width,
//			Function<PairDTO<K, V>, String> textExtractor) {
//		return createColumn(title, width, textExtractor, pair -> null);
//	}

	private <K, V> TableViewerColumn createColumn(String title, int width,
			Function<PairDTO<K, V>, String> textExtractor, Function<PairDTO<K, V>, Image> imageExtractor,
			int columnIndex // 👈 добавляем индекс явно
	) {
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
				if (!(element instanceof PairDTO<?, ?> pair))
					return null;

				@SuppressWarnings("unchecked")
				PairDTO<K, V> typed = (PairDTO<K, V>) pair;

				Image img = imageExtractor.apply(typed);
				if (img == null)
					return null;

				TableItem item = findItem(pair);
				if (item == null)
					return img;

				Object dto = (columnIndex == 0) ? typed.getFirst() : typed.getSecond();

				if (dto instanceof MapEntryDTO mapDto) {
					item.setData("tooltip_col_" + columnIndex,
							"id = " + mapDto.getObjectId() + "\ncategory = " + mapDto.getValueCategory());
				}

				return img;
			}

			private TableItem findItem(Object data) {
				for (TableItem item : viewer.getTable().getItems()) {
					if (item.getData() == data)
						return item;
				}
				return null;
			}
		});

		return column;
	}

	private String safe(String value) {
		return value == null ? "" : value;
	}

	private void closeElementsPopup() {
		if (elementsPopup != null && !elementsPopup.isDisposed()) {
			elementsPopup.dispose();
		}
		elementsPopup = null;
	}

	@Override
	public void showFieldInfoPopupFromBackend(UserInstanceDetailsDTO userInstanceDetailsDTO) {
		// Under construction
	}
}