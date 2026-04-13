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
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.DebuggerContext;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.DebuggerContext.SimpleDebuggerStatus;
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
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public class IterableInspectorTab implements InspectorTab {

	private final UiEventCollector uiEventCollector = SimpleDebuggerEventCollector.instance();
	private static String SEPARATOR = "---------------------------------------------- \n";
	private static String GAP = "  ";

	private final Composite root;
	private final TableViewer viewer;
//	private final TooltipManager tooltipManager;

//	private String lastInspectedElementId;

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
	private TooltipManager tooltipManager;
//	private InnerElementRepresentationDTO lastInspectedElement;
	private String lastHoveredElementId = null;
//	private boolean requestSent = false;
//	private UserInstanceDetailsDTO dtoToDisplay = null;

	

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

		// tooltipManager = new TooltipManager(table, root);
		
		setupTooltips(table);

		createColumn("Index", 80, pair -> String.valueOf(pair.getFirst()));

		createColumn("Value", 600, pair -> formatValue((InnerElementRepresentationDTO) pair.getSecond()),
				pair -> getIcon((InnerElementRepresentationDTO) pair.getSecond()));

	//	setupClickListener();
		setupHoverInspectionListener();
	}
	
//	public UserInstanceDetailsDTO getDtoToDisplay() {
//		return dtoToDisplay;
//	}

//	public void setDtoToDisplay(UserInstanceDetailsDTO dto) {
//	    Display.getDefault().asyncExec(() -> {
//	        if (root.isDisposed() || dto == null) return;
//
//	        showFieldInfoPopupFromBackend(dto);
//	    });
//	}

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

	        if (item != null) {
	            Object data = item.getData();

	            if (data instanceof PairDTO<?, ?> pair) {
	                Object second = pair.getSecond();

	                if (second instanceof InnerElementRepresentationDTO dataDto) {

	                    int colIndex = getColumnIndexAtPoint(table, event.x);

	                    if (colIndex == 1) {
	                        ValueCategory category = dataDto.getValueCategory();

	                        boolean isInspectable =
	                                category == ValueCategory.USER_OBJECT
	                                        || category == ValueCategory.COLLECTION
	                                        || category == ValueCategory.MAP;

	                        if (isInspectable) {
	                            dto = dataDto;
	                        }
	                    }
	                }
	            }
	        }

	        String currentId = dto != null ? dto.getAdditionalInfo() : null;

	        // 🔴 защита от лишних срабатываний
	        if (Objects.equals(currentId, lastHoveredElementId)) {
	            return;
	        }

	        lastHoveredElementId = currentId;

	        // если ушли с элемента → закрываем tooltip
	        if (dto == null) {
	            tooltipManager.closePopup();
	            return;
	        }

	        Display display = table.getDisplay();

	        InnerElementRepresentationDTO finalDto = dto;

	        display.asyncExec(() -> {
	            if (table.isDisposed() || finalDto == null) {
	                return;
	            }

	            Image icon = getIcon(finalDto);

	            if (icon == SimpleDebugerWindowsManager.instance()
	                    .icons.get("inspectIcon").getFirst()) {

	                uiEventCollector.collectUiEvent(
	                        new UIEvent<>(
	                                SimpleDebuggerEventType.USER_REQUESTED_ADDITIONAL_INFO_ABOUT_OBJECT,
	                                finalDto
	                        )
	                );

	            } else if (icon == SimpleDebugerWindowsManager.instance()
	                    .icons.get("lens").getFirst()) {

	                Point location = display.getCursorLocation();
	                tooltipManager.showTooltipForCollection(finalDto, location);
	            }
	        });
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

	private String buildCollectionText(InnerElementRepresentationDTO dto) {
	    if (dto == null) return "null";

	    StringBuilder info = new StringBuilder();

	    String name = safe(dto.getElementName());
	    String id = safe(dto.getAdditionalInfo());
	    String value = safe(dto.getValue());

	    info.append("Inspect element:\n")
	        .append(GAP).append("name: ").append(name).append("\n")
	        .append(GAP).append("id: ").append(id).append("\n");

	    // Попытка красиво распарсить тип коллекции
	    // Пример: List<String> (size=10)
	    try {
	        int comma = value.indexOf(',');
	        int gt = value.indexOf('>');

	        if (comma != -1 && gt != -1 && comma < gt) {
	            String typePart = value.substring(0, comma).trim();
	            String genericPart = value.substring(comma + 1, gt + 1).trim();
	            String instancePart = value.substring(gt + 1).trim();

	            info.append(GAP).append(typePart).append("\n")
	                .append(GAP).append(genericPart).append("\n");

	            if (!instancePart.isEmpty()) {
	                info.append(GAP).append("instance: ").append(instancePart).append("\n");
	            }
	        } else {
	            // fallback — если формат неожиданный
	            info.append(GAP).append(value).append("\n");
	        }
	    } catch (Exception e) {
	        // если что-то пошло не так — просто выводим value
	        info.append(GAP).append(value).append("\n");
	    }

	    return info.toString();
	}
	

//	private void displyDetails() {
//		Display.getDefault().asyncExec(() -> {
//			while (true) {
//				if (Objects.nonNull(dtoToDisplay)) {
//					showFieldInfoPopupFromBackend(dtoToDisplay);
//					break;
//				}
//				try {
//					Thread.sleep(100);
//				} catch (InterruptedException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//			}
//			
//		});  
//		
//	}

	public void showFieldInfoPopupFromBackend(UserInstanceDetailsDTO userInstanceInspectionDTO) {
		Display display = root.getDisplay();
		display.asyncExec(() -> {
			if (root.isDisposed())
				return;
			Point location = display.getCursorLocation();
		//	showPopup(userInstanceInspectionDTO, location,  dto -> UiUtils.buildUserObjectText((UserInstanceDetailsDTO) dto), null);
			showTooltipForUserObject(userInstanceInspectionDTO, location);
		});
	}
	
	public void showTooltipForUserObject(UserInstanceDetailsDTO dto, Point location) {

		if (dto.getInnerElementsByGroups().get(1).isEmpty() && dto.getInnerElementsByGroups().get(2).isEmpty()
				&& dto.getInnerElementsByGroups().get(3).isEmpty())
			return;

		showPopup(dto, location, d -> UiUtils.buildUserObjectText((UserInstanceDetailsDTO) d),
				() -> {}
//		uiEventCollector.collectUiEvent(new UIEvent<>(
//						SimpleDebuggerEventType.USER_STARTED_INSPECTION_SEANCE, UiUtils.convertUserInstanceToInnerDTO(dto)))
				
				);
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

	private void showPopup(Object dto, Point location, Function<Object, String> textBuilder, Runnable onClick) {
		Display display = root.getDisplay();
		display.syncExec(() -> {
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
			Point adjustedLocation = UiUtils.adjustToScreen(root, location, popupSize);

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


	public void closePopup() {
		if (currentPopup != null && !currentPopup.isDisposed()) {
			currentPopup.dispose();
		}
		currentPopup = null;
	//	lastInspectedElementId = null;
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

//	private void setupClickListener() {
//		Table table = viewer.getTable();
//
//		table.addListener(SWT.MouseDown, event -> {
//			TableItem item = table.getItem(new Point(event.x, event.y));
//			if (item == null)
//				return;
//
//			Object data = item.getData();
//			if (!(data instanceof PairDTO<?, ?> pair))
//				return;
//
//			Object second = pair.getSecond();
//			if (!(second instanceof InnerElementRepresentationDTO dto))
//				return;
//
//			uiEventCollector.collectUiEvent(
//					new UIEvent<>(SimpleDebuggerEventType.USER_CONTINUES_INSPECTION_FOR_USER_OBJECT, dto));
//		});
//	}

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