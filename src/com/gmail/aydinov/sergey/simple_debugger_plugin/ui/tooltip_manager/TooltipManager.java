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

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.UniversalElementType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.ValueCategory;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.DebuggerContext;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.details.UserElementDetailDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.details.UserInstanceDetailsDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebuggerEventTypes.SimpleDebuggerEventType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.DebugEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.SimpleDebuggerEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.UiEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.debug_event.DebugEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.UIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.utils.UiUtils;
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
	private final UiEventCollector uiEventCollector = SimpleDebuggerEventCollector.instance();
	private final DebugEventCollector debugEventCollector = SimpleDebuggerEventCollector.instance();

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

	public void showTooltipForUserObject(UserInstanceDetailsDTO dto, Point location) {

		if (dto.getInnerElementsByGroups().get(1).isEmpty() && dto.getInnerElementsByGroups().get(2).isEmpty()
				&& dto.getInnerElementsByGroups().get(3).isEmpty())
			return;

		showPopup(dto, location, d -> buildUserObjectText((UserInstanceDetailsDTO) d),
				() -> uiEventCollector.collectUiEvent(new UIEvent<>(
						SimpleDebuggerEventType.USER_STARTED_INSPECTION_SEANCE, UiUtils.convertUserInstanceToInnerDTO(dto))));
	}

	public void closePopup() {
		if (currentPopup != null && !currentPopup.isDisposed()) {
			currentPopup.dispose();
		}
		currentPopup = null;
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

	public void showTooltipForCollection(InnerElementRepresentationDTO dto, Point location) {

		showPopup(dto, location, d -> buildCollectionText((InnerElementRepresentationDTO) d), () -> {
			debugEventCollector
					.collectDebugEvent(new DebugEvent<Boolean>(SimpleDebuggerEventType.SET_RESUME_BUTTON_STATE, false));
			switch (dto.getValueCategory()) {
			case COLLECTION -> uiEventCollector
					.collectUiEvent(new UIEvent<>(SimpleDebuggerEventType.USER_STARTED_INSPECTION_SEANCE, dto));
			case MAP -> uiEventCollector
					.collectUiEvent(new UIEvent<>(SimpleDebuggerEventType.USER_STARTED_INSPECTION_SEANCE, dto));
			default -> debugEventCollector
					.collectDebugEvent(new DebugEvent<Boolean>(SimpleDebuggerEventType.SET_RESUME_BUTTON_STATE, true));
			}
		});
	}

	private void showPopup(Object dto, Point location, Function<Object, String> textBuilder, Runnable onClick) {

		Display display = root.getDisplay();

		display.asyncExec(() -> {
			if (root.isDisposed() || dto == null)
				return;

			closePopup();

			Shell popup = new Shell(root.getShell(), SWT.ON_TOP | SWT.TOOL);
			popup.setLayout(new GridLayout(1, false));

// 👉 курсор только если кликабельный
			if (onClick != null) {
				popup.setCursor(display.getSystemCursor(SWT.CURSOR_HAND));
			}

// UI
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

// 👉 обработчик только если есть действие
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

	private String buildCollectionText(InnerElementRepresentationDTO dto) {
		String value = dto.getValue();

		int comma = value.indexOf(',');
		int gt = value.indexOf('>');

		StringBuilder info = new StringBuilder();

		info.append("Inspect element:\n").append(GAP).append("name: ").append(dto.getElementName()).append("\n")
				.append(GAP).append("id = ").append(dto.getAdditionalInfo()).append("\n");

		if (comma != -1 && gt != -1) {
			info.append(GAP).append(value.substring(0, comma)).append("\n").append(GAP)
					.append(value.substring(comma + 2, gt + 1)).append("\n").append(GAP).append("instance: ")
					.append(value.substring(gt + 2));
		} else {
			info.append(GAP).append(value);
		}

		return info.toString();
	}

//	private InnerElementRepresentationDTO convertUserInstanceToInnerDTO(UserInstanceDetailsDTO dto) {
//		return InnerElementRepresentationDTO.InnerElementRepresentationDTOFactory
//				.fromElement(UniversalElementRepresentation.builder().elementName(dto.getFieldName())
//						.elementType(UniversalElementType.FIELD).value(dto.toString())
//						.valueCategory(ValueCategory.USER_OBJECT).uniqueId(dto.getTag().getUniqueId())
//						.parentUniqueId(dto.getTag().getParentId())
//						// .level(dto.)
//						.build());
//	}
}