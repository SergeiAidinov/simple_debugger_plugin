package com.gmail.aydinov.sergey.simple_debugger_plugin.ui.tab.inspect_window_tab;

import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.ValueCategory;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.PairDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.details.UserElementDetailDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.details.UserInstanceDetailsDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.inspection.AbstractInspectionDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.inspection.ArrayPageDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.SimpleDebuggerEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.UIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebuggerEventTypes.SimpleDebuggerEventType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.UiEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.tooltip_manager.TooltipManager;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.utils.UiUtils;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.window.SimpleDebugerWindowsManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public class IterableInspectorTab implements InspectorTab {

	private final UiEventCollector uiEventCollector = SimpleDebuggerEventCollector.instance();
	private static String SEPARATOR = "---------------------------------------------- \n";
	private static String GAP = "  ";

	private final Composite root;
	private final TableViewer viewer;
//	private final TooltipManager tooltipManager;

	private String lastInspectedElementId;

	private final Label collectionNameLabel;
	private final Label collectionTypeLabel;
	private final Label elementTypeLabel;
	private final Label sizeLabel;
	private final Label pageInfoLabel;

	private final Button prevButton;
	private final Text pageText;
	private final Button goButton;
	private final Button nextButton;
	private Shell currentPopup;

	private int currentPage = 0;

	public IterableInspectorTab(Composite parent) {
		root = new Composite(parent, SWT.NONE);
		root.setLayout(new GridLayout(1, false));

		// ===== Header =====
		Composite headerComposite = new Composite(root, SWT.NONE);
		headerComposite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		headerComposite.setLayout(new GridLayout(2, false));

		Composite infoComposite = new Composite(headerComposite, SWT.NONE);
		infoComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		infoComposite.setLayout(new GridLayout(1, false));

		collectionNameLabel = new Label(infoComposite, SWT.NONE);
		collectionTypeLabel = new Label(infoComposite, SWT.NONE);
		elementTypeLabel = new Label(infoComposite, SWT.NONE);
		sizeLabel = new Label(infoComposite, SWT.NONE);
		pageInfoLabel = new Label(infoComposite, SWT.NONE);

		Composite paginationComposite = new Composite(headerComposite, SWT.NONE);
		paginationComposite.setLayoutData(new GridData(SWT.END, SWT.BEGINNING, false, false));
		paginationComposite.setLayout(new GridLayout(4, false));

		prevButton = new Button(paginationComposite, SWT.PUSH);
		prevButton.setText("Prev");
		prevButton.addListener(SWT.Selection, e -> uiEventCollector.collectUiEvent(
				new UIEvent<>(SimpleDebuggerEventType.USER_REQUESTED_COLLECTION_PAGE, currentPage - 1)));

		pageText = new Text(paginationComposite, SWT.BORDER);
		pageText.setLayoutData(new GridData(70, SWT.DEFAULT));

		goButton = new Button(paginationComposite, SWT.PUSH);
		goButton.setText("Go");
		goButton.addListener(SWT.Selection, e -> requestPage());
		pageText.addListener(SWT.DefaultSelection, e -> requestPage());

		nextButton = new Button(paginationComposite, SWT.PUSH);
		nextButton.setText("Next");
		nextButton.addListener(SWT.Selection, e -> uiEventCollector.collectUiEvent(
				new UIEvent<>(SimpleDebuggerEventType.USER_REQUESTED_COLLECTION_PAGE, currentPage + 1)));

		// ===== Table =====
		Table table = new Table(root, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		viewer = new TableViewer(table);
		viewer.setContentProvider(ArrayContentProvider.getInstance());

	//	tooltipManager = new TooltipManager(table, root);

		createColumn("Index", 80, pair -> String.valueOf(pair.getFirst()));

		createColumn("Value", 600, pair -> formatValue((InnerElementRepresentationDTO) pair.getSecond()),
				pair -> getIcon((InnerElementRepresentationDTO) pair.getSecond()));

		setupClickListener();
		setupHoverInspectionListener();
	}

	@Override
	public Composite getControl() {
		return root;
	}

	@Override
	public void showPage(AbstractInspectionDTO abstractInspectionDTO) {
		if (!(abstractInspectionDTO instanceof ArrayPageDTO))
			return;

		ArrayPageDTO page = (ArrayPageDTO) abstractInspectionDTO;

		root.getDisplay().asyncExec(() -> {
			if (root.isDisposed() || viewer.getTable().isDisposed())
				return;

			collectionNameLabel.setText("Collection name: " + safe(page.getElementName()));
			collectionTypeLabel.setText("Collection type: " + safe(page.getElementType()));
			elementTypeLabel.setText("Element type: " + safe(page.getElementType()));
			sizeLabel.setText("Size: " + page.getTotalElements());

			pageInfoLabel.setText("Page: " + page.getCurrentPage() + "/" + page.getTotalPages());

			pageText.setText(String.valueOf(page.getCurrentPage()));
			currentPage = page.getCurrentPage();

			prevButton.setEnabled(page.hasPreviousPage());
			nextButton.setEnabled(page.hasNextPage());

			viewer.setInput(page.getEntries());
			root.layout(true, true);
		});
	}

	private void setupHoverInspectionListener() {
		Table table = viewer.getTable();

		table.addListener(SWT.MouseMove, event -> {
			TableItem item = table.getItem(new Point(event.x, event.y));
			InnerElementRepresentationDTO dto = null;
			if (item != null && item.getData() instanceof PairDTO<?, ?> pair) {
				Object second = pair.getSecond();
				if (second instanceof InnerElementRepresentationDTO dataDto) {
					int colIndex = getColumnIndexAtPoint(table, event.x);
					if (colIndex == 1) {
						Image icon = getIcon(dataDto);
						if (icon == SimpleDebugerWindowsManager.instance().icons.get("inspectIcon").getFirst()
								|| icon == SimpleDebugerWindowsManager.instance().icons.get("lens").getFirst()) {
							dto = dataDto;
						}
					}
				}
			}

			String currentId = dto != null ? dto.getAdditionalInfo() : null;

			if (!Objects.equals(currentId, lastInspectedElementId)) {
				lastInspectedElementId = currentId;

			//	tooltipManager.closePopup();

				if (dto == null)
					return;

				Image icon = getIcon(dto);

				if (icon == SimpleDebugerWindowsManager.instance().icons.get("inspectIcon").getFirst()) {
					uiEventCollector.collectUiEvent(
							new UIEvent<>(SimpleDebuggerEventType.USER_REQUESTED_ADDITIONAL_INFO_ABOUT_OBJECT, dto));
				} else if (icon == SimpleDebugerWindowsManager.instance().icons.get("lens").getFirst()) {
					Point location = table.getDisplay().getCursorLocation();
				//	tooltipManager.showTooltipForCollection(dto, location);
				}
			}
		});
	}

	public static int getColumnIndexAtPoint(Table table, int x) {
		int offset = 0;
		for (int i = 0; i < table.getColumnCount(); i++) {
			offset += table.getColumn(i).getWidth();
			if (x < offset)
				return i;
		}
		return table.getColumnCount() - 1;
	}

	public void showFieldInfoPopupFromBackend(UserInstanceDetailsDTO dto) {
		Display display = root.getDisplay();
		 Point location = display.getCursorLocation();
		showPopup(dto, location, d -> buildUserObjectText((UserInstanceDetailsDTO) d), null);
	//	Display display = root.getDisplay();
//        display.asyncExec(() -> {
//            if (root.isDisposed()) return;
//            Point location = display.getCursorLocation();
//           // tooltipManager.showTooltipForUserObject(dto, location);
//        }

	}

	private String buildUserObjectText(UserInstanceDetailsDTO dto) {
		StringBuilder info = new StringBuilder();

		info.append("Field name: ").append(dto.getFieldName()).append("\n");
		info.append("Field type: ").append(dto.getTypeName()).append("\n\n");

		info.append("INFO:\nFields:\n");
		addGroupOfElements(info, dto.getInnerElementsByGroups().get(1), List.of("name: ", "type: ", "value: "));

		info.append("Methods:\n");
		addGroupOfElements(info, dto.getInnerElementsByGroups().get(2), List.of("name: ", "return type: ", ""));

		info.append("Others:\n");
		addGroupOfElements(info, dto.getInnerElementsByGroups().get(3), List.of("name: ", "type: ", "value: "));

		return info.toString();
	}

	private void addGroupOfElements(StringBuilder stringBuilder, List<UserElementDetailDTO> list,
			List<String> markers) {

		for (int outer = 0; outer < list.size(); outer++) {
			UserElementDetailDTO innerElement = list.get(outer);
			for (int i = 0; i < markers.size(); i++) {
				String announce = markers.get(i);
				if (i == 0) {
					Optional.ofNullable(announce).ifPresent(e -> stringBuilder.append(GAP).append(announce)
							.append(innerElement.getName()).append("\n"));
				} else if (i == 1) {
					Optional.ofNullable(announce).ifPresent(e -> stringBuilder.append(GAP).append(announce)
							.append(innerElement.getTypeOrReturnType()).append("\n"));
				} else if (i == 2) {
					Optional.ofNullable(announce).ifPresent(e -> stringBuilder.append(GAP).append(announce)
							.append(innerElement.getValue()).append("\n"));
				}
				if (i == 2 && (list.size() - outer != 1))
					stringBuilder.append("\n");
			}
		}
		stringBuilder.append(SEPARATOR + "\n");
	}

	private void showPopup(Object dto, Point location, Function<Object, String> textBuilder, Runnable onClick) {

		Display display = root.getDisplay();

		display.asyncExec(() -> {
			if (root.isDisposed() || dto == null)
				return;

			closePopup();

			Shell popup = new Shell(root.getShell(), SWT.ON_TOP | SWT.TOOL);
			popup.setLayout(new GridLayout(1, false));

//👉 курсор только если кликабельный
			if (onClick != null) {
				popup.setCursor(display.getSystemCursor(SWT.CURSOR_HAND));
			}

//UI
			ScrolledComposite scrolled = new ScrolledComposite(popup, SWT.V_SCROLL | SWT.H_SCROLL);
			scrolled.setLayoutData(new GridData(400, 200));

			Composite content = new Composite(scrolled, SWT.NONE);
			content.setLayout(new GridLayout(1, false));

			Label label = new Label(content, SWT.WRAP);
			label.setText(textBuilder.apply(dto));
			label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

			if (onClick != null) {
				label.setCursor(display.getSystemCursor(SWT.CURSOR_HAND));
			}

			scrolled.setContent(content);
			scrolled.setExpandHorizontal(true);
			scrolled.setExpandVertical(true);
			scrolled.setMinSize(content.computeSize(SWT.DEFAULT, SWT.DEFAULT));

//👉 обработчик только если есть действие
			if (onClick != null) {
				Listener clickHandler = e -> {
					onClick.run();
					closePopup();
				};

				popup.addListener(SWT.MouseDown, clickHandler);
				content.addListener(SWT.MouseDown, clickHandler);
				label.addListener(SWT.MouseDown, clickHandler);
				scrolled.addListener(SWT.MouseDown, clickHandler);
			}

			popup.pack();
			Point popupSize = popup.getSize();
			Point adjustedLocation = adjustToScreen(location, popupSize);

			popup.setLocation(adjustedLocation);
			popup.open();

			currentPopup = popup;
			popup.addListener(SWT.Dispose, e -> currentPopup = null);

			display.timerExec(150, this::checkPopupCursor);
		});
	}
	
	private void checkPopupCursor() {
		if (currentPopup == null || currentPopup.isDisposed())
			return;
		Display display = root.getDisplay();
		Point cursor = display.getCursorLocation();
		Rectangle popupBounds = currentPopup.getBounds();
		Point rootLocation = root.toDisplay(0, 0);
		Rectangle rootBounds = new Rectangle(rootLocation.x, rootLocation.y, root.getSize().x, root.getSize().y);
		boolean cursorInsidePopup = popupBounds.contains(cursor);
		boolean cursorInsideTable = rootBounds.contains(cursor);
		if (!cursorInsidePopup && !cursorInsideTable) {
			closePopup();
			return;
		}
		display.timerExec(150, this::checkPopupCursor);
	}

	private Point adjustToScreen(Point desiredLocation, Point popupSize) {
		Display display = root.getDisplay();
		Rectangle screen = display.getPrimaryMonitor().getClientArea();
		int x = desiredLocation.x;
		int y = desiredLocation.y;
		if (x + popupSize.x > screen.x + screen.width) {
			x = screen.x + screen.width - popupSize.x;
		}
		if (y + popupSize.y > screen.y + screen.height) {
			y = screen.y + screen.height - popupSize.y;
		}
		if (x < screen.x) {
			x = screen.x;
		}
		if (y < screen.y) {
			y = screen.y;
		}
		return new Point(x, y);
	}

	public void closePopup() {
		if (currentPopup != null && !currentPopup.isDisposed()) {
			currentPopup.dispose();
		}
		currentPopup = null;
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

	private <K, V> TableViewerColumn createColumn(String title, int width,
			Function<PairDTO<K, V>, String> textExtractor, Function<PairDTO<K, V>, Image> imageExtractor) {

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

				return imageExtractor.apply(typed);
			}
		});

		return column;
	}

	private <K, V> TableViewerColumn createColumn(String title, int width,
			Function<PairDTO<K, V>, String> textExtractor) {
		return createColumn(title, width, textExtractor, pair -> null);
	}

	private Image getIcon(InnerElementRepresentationDTO dto) {
		if (dto == null)
			return null;

		ValueCategory category = dto.getValueCategory();

		if (category == ValueCategory.COLLECTION || category == ValueCategory.MAP) {
			return SimpleDebugerWindowsManager.instance().icons.get("lens").getFirst();
		}

		if (category == ValueCategory.USER_OBJECT && dto.getValue() != null
				&& !UiUtils.isStandartJavaType(dto.getTypeOrReturnType())) {
			return SimpleDebugerWindowsManager.instance().icons.get("inspectIcon").getFirst();
		}

		return null;
	}

	private void setupClickListener() {
		Table table = viewer.getTable();

		table.addListener(SWT.MouseDown, event -> {
			TableItem item = table.getItem(new Point(event.x, event.y));
			if (item == null)
				return;

			Object data = item.getData();
			if (!(data instanceof PairDTO<?, ?> pair))
				return;

			Object second = pair.getSecond();
			if (!(second instanceof InnerElementRepresentationDTO dto))
				return;

			uiEventCollector.collectUiEvent(
					new UIEvent<>(SimpleDebuggerEventType.USER_CONTINUES_INSPECTION_FOR_USER_OBJECT, dto));
		});
	}

	private void requestPage() {
		int page;
		try {
			page = Integer.parseInt(pageText.getText().trim());
		} catch (Exception e) {
			page = 0;
		}

		if (page < 0)
			page = 0;

		uiEventCollector.collectUiEvent(new UIEvent<>(SimpleDebuggerEventType.USER_REQUESTED_COLLECTION_PAGE, page));
	}

	private String safe(String value) {
		return value == null ? "" : value;
	}
}