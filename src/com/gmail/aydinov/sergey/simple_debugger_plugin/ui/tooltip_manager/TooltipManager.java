package com.gmail.aydinov.sergey.simple_debugger_plugin.ui.tooltip_manager;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Listener;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.UniversalElementType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.ValueCategory;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.UserInstanceInnerElementInspectionDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.UserInstanceInspectionDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebuggerEventTypes.SimpleDebuggerEventType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.SimpleDebuggerEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.UIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.window.SimpleDebugerWindowsManager;

public class TooltipManager {

	private static final int OFFSET_X = -100;
	private static final int OFFSET_Y = OFFSET_X;
	private static String GAP = "  ";
	private static String SEPARATOR = "---------------------------------------------- \n";

	private final Table table;
	private final Composite root;
	private Function<TableItem, String> tooltipProvider;
	private Shell currentPopup;
	private final SimpleDebuggerEventCollector uiEventCollector = SimpleDebuggerEventCollector.instance();

	public TooltipManager(Table table, Composite root) {
		this.table = table;
		this.root = root;
		attachHoverListener();
	}

	/**
	 * Устанавливает функцию, которая возвращает текст тултипа для конкретного
	 * TableItem.
	 */
	public void setTooltipProvider(Function<TableItem, String> tooltipProvider) {
		this.tooltipProvider = tooltipProvider;
	}

	private void attachHoverListener() {
		table.addListener(SWT.MouseHover, event -> {
			TableItem item = table.getItem(new Point(event.x, event.y));
			if (item == null) {
				table.setToolTipText(null);
				return;
			}
			String tip = null;
			if (tooltipProvider != null) {
				tip = tooltipProvider.apply(item);
			}
			table.setToolTipText(tip);
		});
	}

	/**
	 * Получение индекса колонки по координате X (аналог getColumnIndexAtPoint из
	 * твоего класса)
	 */
	public static int getColumnIndexAtPoint(Table table, int x) {
		int offset = 0;
		for (int i = 0; i < table.getColumnCount(); i++) {
			offset += table.getColumn(i).getWidth();
			if (x < offset)
				return i;
		}
		return table.getColumnCount() - 1;
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

	public void showFieldInfoPopup(UserInstanceInspectionDTO userInstanceInspectionDTO, Point location) {
		if (userInstanceInspectionDTO.getInnerElementsByGroups().get(1).isEmpty() 
				&& userInstanceInspectionDTO.getInnerElementsByGroups().get(2).isEmpty()
				&& userInstanceInspectionDTO.getInnerElementsByGroups().get(3).isEmpty())
			return;
		Display display = root.getDisplay();
		display.asyncExec(() -> {
			if (root.isDisposed() || userInstanceInspectionDTO == null)
				return;
			closePopup();
			Shell popup = new Shell(root.getShell(), SWT.ON_TOP | SWT.TOOL);
			popup.setLayout(new GridLayout(1, false));
			StringBuilder info = new StringBuilder();
			info.append("Field name: ").append(userInstanceInspectionDTO.getFieldName()).append("\n");
			info.append("Field type: ").append(userInstanceInspectionDTO.getTypeName()).append("\n\n");
			info.append("INFO: \n");
			info.append("Fields: \n");
			addGroupOfElements(info, userInstanceInspectionDTO.getInnerElementsByGroups().get(1),
					List.of("name: ", "type: ", "value: "));
			info.append("Metods: \n");
			addGroupOfElements(info, userInstanceInspectionDTO.getInnerElementsByGroups().get(2),
					new ArrayList<>(Arrays.asList("name: ", "return type: ", null)));
			info.append("Others: \n");
			addGroupOfElements(info, userInstanceInspectionDTO.getInnerElementsByGroups().get(3),
					List.of("name: ", "type: ", "value: "));
			ScrolledComposite scrolled = new ScrolledComposite(popup, SWT.V_SCROLL | SWT.H_SCROLL);
			scrolled.setLayoutData(new GridData(400, 300)); // размер окна
			Composite content = new Composite(scrolled, SWT.NONE);
			content.setLayout(new GridLayout(1, false));
			Label label = new Label(content, SWT.WRAP);
			label.setText(info.toString());
			label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			scrolled.setContent(content);
			scrolled.setExpandHorizontal(true);
			scrolled.setExpandVertical(true);
			scrolled.setMinSize(content.computeSize(SWT.DEFAULT, SWT.DEFAULT));
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

	public void closePopup() {
		if (currentPopup != null && !currentPopup.isDisposed()) {
			currentPopup.dispose();
		}
		currentPopup = null;
	}

	private void addGroupOfElements(StringBuilder stringBuilder, List<UserInstanceInnerElementInspectionDTO> list,
			List<String> markers) {

		for (int outer = 0; outer < list.size(); outer++) {
			UserInstanceInnerElementInspectionDTO innerElement = list.get(outer);
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

	public void showTooltipForCollection(InnerElementRepresentationDTO dto, Point location) {
	    Display display = root.getDisplay();
	    display.asyncExec(() -> {
	        if (root.isDisposed() || dto == null)
	            return;
	        closePopup();
	        Shell popup = new Shell(root.getShell(), SWT.ON_TOP | SWT.TOOL);
	        popup.setLayout(new GridLayout(1, false));
	        // сохраняем DTO внутри popup
	        popup.setData("dto", dto);
	        popup.setCursor(display.getSystemCursor(SWT.CURSOR_HAND));
	        StringBuilder info = new StringBuilder();
	        info.append("Inspect element: ").append("\n")
	         .append(GAP).append("name: ").append(dto.getElementName()).append("\n")
	            .append(GAP).append("type: ").append(dto.getAdditionalInfo()).append("\n")
	            .append(GAP).append(dto.getValue().substring(0, dto.getValue().indexOf(','))).append("\n")
	            .append(GAP)
	            .append(dto.getValue().substring(dto.getValue().indexOf(',') + 2, dto.getValue().indexOf('>') + 1)).append("\n")
	            .append(GAP)
	            .append("instance: ").append(dto.getValue().substring(dto.getValue().indexOf('>') + 2, dto.getValue().length()))
	            ;
	        ScrolledComposite scrolled = new ScrolledComposite(popup, SWT.V_SCROLL | SWT.H_SCROLL);
	        scrolled.setLayoutData(new GridData(400, 100));
	        Composite content = new Composite(scrolled, SWT.NONE);
	        content.setLayout(new GridLayout(1, false));
	        Label label = new Label(content, SWT.WRAP);
	        label.setText(info.toString());
	        label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	        label.setCursor(display.getSystemCursor(SWT.CURSOR_HAND));
	        label.setData("dto", dto);
	        scrolled.setContent(content);
	        scrolled.setExpandHorizontal(true);
	        scrolled.setExpandVertical(true);
	        scrolled.setMinSize(content.computeSize(SWT.DEFAULT, SWT.DEFAULT));

	        // обработчик клика
	        Listener clickHandler = e -> {
	            Object data = popup.getData("dto");

	            if (data instanceof InnerElementRepresentationDTO clickedDto) {
	            	SimpleDebugerWindowsManager.instance().tagQueue().offer(clickedDto.getTag());
	            	ValueCategory category = dto.getValueCategory();

	                if (category == ValueCategory.COLLECTION) {
	                	uiEventCollector.collectUiEvent(
		    	                new UIEvent<>(SimpleDebuggerEventType.USER_STARTED_INSPECTION_SEANCE_FOR_ITERABLE, dto)
		    	            );
	                } else if (category == ValueCategory.MAP) {
	                	uiEventCollector.collectUiEvent(
		    	                new UIEvent<>(SimpleDebuggerEventType.USER_STARTED_INSPECTION_SEANCE_FOR_MAP, dto)
		    	            );
	                } else if (category == ValueCategory.USER_OBJECT) {
	                	System.out.println("EVENT: USER OBJECT INSPECTION");
	                	uiEventCollector.collectUiEvent(
		    	                new UIEvent<>(SimpleDebuggerEventType.USER_STARTED_INSPECTION_SEANCE_FOR_USER_OBJECT, dto)
		    	            );
	                }
//	                SimpleDebugerWindowsManager.instance()
//	                        .getUniversalInspectorWindowFor(clickedDto);
//	                UniversalInspectorWindow.getInstance().open();
	                closePopup();
	            }
	        };
	        popup.addListener(SWT.MouseDown, clickHandler);
	        label.addListener(SWT.MouseDown, clickHandler);
	        content.addListener(SWT.MouseDown, clickHandler);
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
}