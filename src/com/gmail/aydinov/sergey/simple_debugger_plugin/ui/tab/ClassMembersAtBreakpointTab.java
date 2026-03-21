package com.gmail.aydinov.sergey.simple_debugger_plugin.ui.tab;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.UniversalElementType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.ValueCategory;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.PairDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.UserChangedFieldEventDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.UserChangedVariableEventDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.DebugWindowDataDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.UserInstanceInspectionDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebuggerEventTypes.SimpleDebuggerEventType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.SimpleDebuggerEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.UIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.tooltip_manager.TooltipManager;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.utils.UiUtils;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.window.SimpleDebugerWindowsManager;

/**
 * Вкладка отображения полей, методов и переменных на breakpoint с поддержкой
 * редактирования примитивов и генерацией UI-событий.
 */
public class ClassMembersAtBreakpointTab {

	private final Composite root;
	private final TableViewer viewer;
	private final SimpleDebuggerEventCollector uiEventCollector = SimpleDebuggerEventCollector.instance();
	private InnerElementRepresentationDTO lastInspectedElement;

	private UUID currentElementId;
	private TooltipManager tooltipManager;
//	private InstanceInspectionPopupManager popupManager;

	public ClassMembersAtBreakpointTab(Composite parent) {
		root = new Composite(parent, SWT.NONE);
		root.setLayout(new GridLayout(1, false));
		Table table = new Table(root, SWT.BORDER | SWT.FULL_SELECTION);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		table.setLayoutData(new GridData(GridData.FILL_BOTH));
		viewer = new TableViewer(table);
		viewer.setContentProvider(ArrayContentProvider.getInstance());
		setupColumns();
		setupTooltips(table);
		setupColumnClickListeners();
		setupHoverInspectionListener();
		// popupManager = new InstanceInspectionPopupManager(root);
	}

	public void showInnerElementsInTable(DebugWindowDataDTO dto) {
	    if (dto == null || dto.getTopElementsWithSubordinates().isEmpty()) return;
	    System.out.println(dto);
	    List<InnerElementRepresentationDTO> ordered = new ArrayList<>();
	    if (!dto.getTopElementsWithSubordinates().isEmpty()) {
	        Entry<InnerElementRepresentationDTO, List<InnerElementRepresentationDTO>> firstEntry =
	                dto.getTopElementsWithSubordinates().entrySet().iterator().next();
	        ordered.add(firstEntry.getKey());
	        ordered.addAll(firstEntry.getValue());
	    }

	    // Обновляем TableViewer в UI-потоке
	    root.getDisplay().asyncExec(() -> {
	        if (!viewer.getTable().isDisposed()) {
	            viewer.setInput(ordered);
	            viewer.refresh();
	        }
	    });
	}

	public void showFieldInfoPopupFromBackend(UserInstanceInspectionDTO userInstanceInspectionDTO) {
		Display display = root.getDisplay();
		display.asyncExec(() -> {
			if (root.isDisposed())
				return;
			Point location = display.getCursorLocation();
			tooltipManager.showFieldInfoPopup(userInstanceInspectionDTO, location);
		});
	}

	public Composite getControl() {
		return root;
	}

	// =========================================================
	// Columns
	// =========================================================

	private void setupColumns() {
		// 0: Name
		char arrow = '⮡';
		createColumn(0, "Name", 250, e -> {
			InnerElementRepresentationDTO dto = (InnerElementRepresentationDTO) e;
			String indent = "     ".repeat(dto.getLevel()); // 3 пробела на уровень
			if (dto.getLevel() > 0)
				indent = indent + arrow;
			return indent + dto.getElementName();
		}, e -> null);
		// 1: Type / Return Type
		createColumn(1, "Type / Return Type", 300, InnerElementRepresentationDTO::getTypeOrReturnType,
				this::getTypeIcon);
		// 2: Value / Info
		TableViewerColumn valueColumn = createColumn(2, "Value / Info", 400, dto -> {
			String v = dto.getValue();
			if (v != null)
				return v;
			// Если поле или статическое поле, показываем тип
			if (dto.getElementType() == UniversalElementType.FIELD) {
				return dto.getAdditionalInfo();
			}
			return ""; // иначе пусто
		}, this::getIcon);

		valueColumn.setEditingSupport(new ValueEditingSupport(viewer));
	}

	private TableViewerColumn createColumn(int index, String title, int width,
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
					String value = textExtractor.apply(dto);
					return value != null ? value : "";
				}
				return "";
			}

			@Override
			public Image getImage(Object element) {
				if (!(element instanceof InnerElementRepresentationDTO dto))
					return null;

				Image img = imageExtractor.apply(dto);
				if (img == null)
					return null;

				TableItem item = findTableItem(dto);
				if (item == null)
					return img;

				if (index == 1) {
					PairDTO<Image, String> tooltip = getTypeTooltip(dto);
					if (tooltip != null) {
						item.setData("tooltip_col_" + index, tooltip.getSecond());
					}
				} else if (index == 2 && img == getIcon(dto)) {
					item.setData("tooltip_col_" + index,
							SimpleDebugerWindowsManager.instance().icons.get("inspectIcon").getSecond());
				}
				return img;
			}
		});

		return column;
	}

	private TableItem findTableItem(InnerElementRepresentationDTO dto) {
		for (TableItem item : viewer.getTable().getItems()) {
			if (item.getData() == dto)
				return item;
		}
		return null;
	}

	// =========================================================
	// Icons & tooltips
	// =========================================================
	private Image getTypeIcon(InnerElementRepresentationDTO dto) {
	    if (dto == null || dto.getElementType() == UniversalElementType.REFERENCE)
	        return null; // REFERENCE не отображаем

	    String key = switch (dto.getElementType()) {
	        case INTERFACE -> "interface";
	        case METHOD -> dto.isStatic() ? "static_method" : "method";
	        case FIELD -> dto.isStatic() ? "static_field" : "fieldIcon";
	        case LOCAL_VARIABLE -> "variableIcon";
	        case ENUM -> "enum";
	        default -> "unknown";
	    };

	    PairDTO<Image, String> pair = SimpleDebugerWindowsManager.instance().icons.get(key);
	    return pair != null ? pair.getFirst() : null;
	}


	private PairDTO<Image, String> getTypeTooltip(InnerElementRepresentationDTO dto) {
	    if (dto == null || dto.getElementType() == UniversalElementType.REFERENCE)
	        return null; // REFERENCE не отображаем

	    String key = switch (dto.getElementType()) {
	        case INTERFACE -> "interface";
	        case METHOD -> dto.isStatic() ? "static_method" : "method";
	        case FIELD -> dto.isStatic() ? "static_field" : "fieldIcon";
	        case LOCAL_VARIABLE -> "variableIcon";
	        case ENUM -> "enum";
	        default -> "unknown";
	    };

	    return SimpleDebugerWindowsManager.instance().icons.getOrDefault(key,
	            SimpleDebugerWindowsManager.instance().icons.get("unknown"));
	}

	// Пример исправления опечатки в getValueCategoty()
	private Image getIcon(InnerElementRepresentationDTO dto) {
		if (dto == null)
			return null;
		ValueCategory category = dto.getValueCategory();
		if (category == null)
			return null;
		// коллекции и мапы
		if (category == ValueCategory.COLLECTION || category == ValueCategory.MAP) {
			return SimpleDebugerWindowsManager.instance().icons.get("lens").getFirst();
		}
		// Только поля пользовательского типа, которые реально инициализированы
		if ((dto.getElementType() == UniversalElementType.FIELD) && category == ValueCategory.USER_OBJECT
				&& dto.getValue() != null && !UiUtils.isStandartJavaType(dto.getTypeOrReturnType())) {
			return SimpleDebugerWindowsManager.instance().icons.get("inspectIcon").getFirst();
		}
		return null;
	}

	private boolean isEditable(InnerElementRepresentationDTO dto) {
		System.out.println(dto);
		return dto != null && dto.getTypeOrReturnType() != null
				&& UiUtils.isStandartJavaType(dto.getTypeOrReturnType());
	}

	private void updateFieldValue(InnerElementRepresentationDTO dto, String newValue) {
		uiEventCollector.collectUiEvent(new UIEvent<>(SimpleDebuggerEventType.USER_CHANGED_FIELD,
				new UserChangedFieldEventDTO(dto.getElementName(), dto.getAdditionalInfo(), newValue)));
	}

	private void updateVariableValue(InnerElementRepresentationDTO dto, String newValue) {
		uiEventCollector.collectUiEvent(new UIEvent<>(SimpleDebuggerEventType.USER_CHANGED_VARIABLE,
				new UserChangedVariableEventDTO(dto.getElementName(), dto.getAdditionalInfo(), newValue)));
	}

	// =========================================================
	// Tooltips
	// =========================================================

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

	private int getColumnIndexAtPoint(Table table, int x) {
		int offset = 0;
		for (int i = 0; i < table.getColumnCount(); i++) {
			offset += table.getColumn(i).getWidth();
			if (x < offset)
				return i;
		}
		return table.getColumnCount() - 1;
	}

	// =========================================================
	// Display
	// =========================================================

	private void setupColumnClickListeners() {
		Table table = viewer.getTable();
		table.addListener(SWT.MouseDown, event -> {
			TableItem item = table.getItem(new Point(event.x, event.y));
			if (item == null)
				return;

			int colIndex = getColumnIndexAtPoint(table, event.x);
			// Наша третья колонка — индекс 2
			if (colIndex != 2)
				return;

			Object data = item.getData();
			if (!(data instanceof InnerElementRepresentationDTO dto))
				return;

			// Проверяем, по какой иконке кликнули
			Image clickedImage = getIcon(dto);
			if (clickedImage == null)
				return;

			if (clickedImage == SimpleDebugerWindowsManager.instance().icons.get("lens").getFirst()) {
				// Клик по коллекции — создаем окно и генерируем событие
				Display display = table.getDisplay();
				display.asyncExec(() -> {
					// new CollectionInspectorWindow().open();
				});
				SimpleDebugerWindowsManager.instance().getUniversalInspectorWindowFor(dto);
				uiEventCollector.collectUiEvent(
						new UIEvent<>(SimpleDebuggerEventType.USER_STARTED_INSPECTION_SEANCE_FOR_COLLECTION, dto));
			}
		});
	}

	private void setupHoverInspectionListener() {
		Table table = viewer.getTable();
		table.addListener(SWT.MouseMove, event -> {
			TableItem item = table.getItem(new Point(event.x, event.y));
			InnerElementRepresentationDTO dto = null;
			if (item != null && item.getData() instanceof InnerElementRepresentationDTO dataDto) {
				int colIndex = getColumnIndexAtPoint(table, event.x);
				if (colIndex == 2) {
					Image icon = getIcon(dataDto);
					if (icon == SimpleDebugerWindowsManager.instance().icons.get("inspectIcon").getFirst()
							|| icon == SimpleDebugerWindowsManager.instance().icons.get("lens").getFirst()) {
						dto = dataDto;
					}
				}
			}

			if (!Objects.equals(dto, lastInspectedElement)) {
				lastInspectedElement = dto;
				tooltipManager.closePopup();
				if (dto != null) {
					if (getIcon(dto) == SimpleDebugerWindowsManager.instance().icons.get("inspectIcon").getFirst()) {
	                    uiEventCollector.collectUiEvent(
	                        new UIEvent<>(SimpleDebuggerEventType.USER_REQUESTED_ADDITIONAL_INFO_ABOUT_OBJECT, dto)
	                    );
					} else if (getIcon(dto) == SimpleDebugerWindowsManager.instance().icons.get("lens").getFirst()) {
//	                    uiEventCollector.collectUiEvent(
//	                        new UIEvent<>(SimpleDebuggerEventType.USER_REQUESTED_ADDITIONAL_INFO_ABOUT_COLLECTION, dto)
//	                    );
						Display display = root.getDisplay();
						Point location = display.getCursorLocation();
						tooltipManager.showTooltipForCollection(dto, location);
					}
				}
			}
		});
	}

	private class ValueEditingSupport extends EditingSupport {
		private final TextCellEditor editor;

		ValueEditingSupport(TableViewer viewer) {
			super(viewer);
			this.editor = new TextCellEditor(viewer.getTable());
		}

		@Override
		protected CellEditor getCellEditor(Object element) {
			return editor;
		}

		@Override
		protected boolean canEdit(Object element) {
			return element instanceof InnerElementRepresentationDTO dto && isEditable(dto);
		}

		@Override
		protected Object getValue(Object element) {
			return ((InnerElementRepresentationDTO) element).getValue();
		}

		@Override
		protected void setValue(Object element, Object value) {
			if (!(element instanceof InnerElementRepresentationDTO dto))
				return;
			if (value == null)
				return;
			String newValue = value.toString();
			// Попробуем преобразовать строку в нужный тип, если это примитив
			String type = dto.getAdditionalInfo();
			Object convertedValue = UiUtils.convertToType(newValue, type);
			switch (dto.getElementType()) {
			case FIELD -> updateFieldValue(dto, convertedValue.toString());
			case LOCAL_VARIABLE -> updateVariableValue(dto, convertedValue.toString());
			default -> {
			}
			}
			viewer.update(dto, null);
		}
	}
}