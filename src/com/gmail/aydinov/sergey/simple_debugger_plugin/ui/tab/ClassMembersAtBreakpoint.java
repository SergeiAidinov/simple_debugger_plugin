package com.gmail.aydinov.sergey.simple_debugger_plugin.ui.tab;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.UniversalElementType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.ValueCategory;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.CurrentRole;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.DebugWindowDataDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.InnerElementRepresentationDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.PairDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.UserChangedFieldEventDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.UserChangedVariableEventDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebuggerEventTypes.SimpleDebuggerEventType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.SimpleDebuggerEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.UIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.DebugWindowsManager;

/**
 * Вкладка отображения полей, методов и переменных на breakpoint с поддержкой
 * редактирования примитивов и генерацией UI-событий.
 */
public class ClassMembersAtBreakpoint {

	private final Composite root;
	private final TableViewer viewer;
	private final SimpleDebuggerEventCollector uiEventCollector = SimpleDebuggerEventCollector.instance();

	private static final Set<String> JAVA_STANDARD_TYPES = Set.of("int", "long", "short", "byte", "float", "double",
			"boolean", "char", "java.lang.Integer", "java.lang.Long", "java.lang.Short", "java.lang.Byte",
			"java.lang.Float", "java.lang.Double", "java.lang.Boolean", "java.lang.Character", "java.lang.String");

	public ClassMembersAtBreakpoint(Composite parent) {
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
	}

	// =========================================================
	// Columns
	// =========================================================

	private void setupColumns() {

		// 0: Name
		createColumn(0, "Name", 150, InnerElementRepresentationDTO::getElementName, e -> null);

		// 1: Type / Return Type
		createColumn(1, "Type / Return Type", 200, InnerElementRepresentationDTO::getAdditionalInfo, this::getTypeIcon);

		// 2: Value / Info
		TableViewerColumn valueColumn = createColumn(2, "Value / Info", 300, dto -> {
			String v = dto.getValue();
			if (v != null)
				return v;

			// Если поле или статическое поле, показываем тип
			if (dto.getElementType() == UniversalElementType.STATIC_FIELD
					|| dto.getElementType() == UniversalElementType.NON_STATIC_FIELD) {
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
							DebugWindowsManager.instance().icons.get("inspectIcon").getSecond());
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
		String key = switch (dto.getElementType()) {
		case INTERFACE -> "interface";
		case METHOD -> dto.isStatic() ? "static_method" : "method";
		case STATIC_FIELD -> "static_field";
		case NON_STATIC_FIELD -> "fieldIcon";
		case VARIABLE -> "variableIcon";
		default -> null;
		};
		return key != null ? DebugWindowsManager.instance().icons.get(key).getFirst() : null;
	}

	private PairDTO<Image, String> getTypeTooltip(InnerElementRepresentationDTO dto) {
		String key = switch (dto.getElementType()) {
		case INTERFACE -> "interface";
		case METHOD -> dto.isStatic() ? "static_method" : "method";
		case STATIC_FIELD -> "static_field";
		case NON_STATIC_FIELD -> "fieldIcon";
		case VARIABLE -> "variableIcon";
		default -> null;
		};
		return key != null ? DebugWindowsManager.instance().icons.get(key) : null;
	}

	private Image getIcon(InnerElementRepresentationDTO dto) {
		if (dto.getValueCategoty().equals(ValueCategory.COLLECTION)
				|| (dto.getValueCategoty().equals(ValueCategory.MAP))) {
			return getLensIcon(dto);
		}

		return shouldShowInspectIcon(dto) ? DebugWindowsManager.instance().icons.get("inspectIcon").getFirst() : null;
	}

	private Image getLensIcon(InnerElementRepresentationDTO dto) {
		return DebugWindowsManager.instance().icons.get("lens").getFirst();
	}

	private boolean shouldShowInspectIcon(InnerElementRepresentationDTO innerElementRepresentationDTO) {
		if (innerElementRepresentationDTO == null)
			return false;

		UniversalElementType type = innerElementRepresentationDTO.getElementType();
//		if (Objects.nonNull(type) && type.equals(UniversalElementType.STATIC_FIELD)
//				&& type.equals(UniversalElementType.NON_STATIC_FIELD)) {
		if (Objects.nonNull(innerElementRepresentationDTO.getValueCategoty())
				&& innerElementRepresentationDTO.getValueCategoty().equals(ValueCategory.USER_OBJECT)) {
			return true;
		}
		// }
		return false;

//		if (type != UniversalElementType.NON_STATIC_FIELD && type != UniversalElementType.VARIABLE)
//			return false;
//
//		String typeName = dto.getAdditionalInfo();
//		if (typeName == null || typeName.isBlank())
//			return false;
//
//		if (JAVA_STANDARD_TYPES.contains(typeName))
//			return false;
//
//		return !typeName.startsWith("java.") && !typeName.startsWith("javax.");
	}

	// =========================================================
	// Editing
	// =========================================================

	private boolean isEditable(InnerElementRepresentationDTO dto) {
		if (dto == null)
			return false;

		if (dto.getElementType() != UniversalElementType.NON_STATIC_FIELD
				&& dto.getElementType() != UniversalElementType.VARIABLE)
			return false;

		return JAVA_STANDARD_TYPES.contains(dto.getAdditionalInfo());
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

			switch (dto.getElementType()) {
			case STATIC_FIELD, NON_STATIC_FIELD -> updateFieldValue(dto, newValue);
			case VARIABLE -> updateVariableValue(dto, newValue);
			default -> {
			}
			}

			viewer.update(dto, null);
		}
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
		table.addListener(SWT.MouseHover, event -> {
			TableItem item = table.getItem(new Point(event.x, event.y));
			if (item == null) {
				table.setToolTipText(null);
				return;
			}
			int col = getColumnIndexAtPoint(table, event.x);
			Object tip = item.getData("tooltip_col_" + col);
			table.setToolTipText(tip instanceof String ? (String) tip : null);
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

	// =========================================================
	// Display
	// =========================================================

	public void showInnerElements(DebugWindowDataDTO dto) {
		if (dto == null || dto.getInnerElements().isEmpty())
			return;

		List<InnerElementRepresentationDTO> sorted = new ArrayList<>(dto.getInnerElements());

		sorted.sort(Comparator.comparingInt((InnerElementRepresentationDTO e) -> e.getElementType().ordinal())
				.thenComparing(InnerElementRepresentationDTO::getElementName));

		root.getDisplay().asyncExec(() -> {
			if (!viewer.getTable().isDisposed()) {
				viewer.setInput(sorted);
			}
		});
	}

	public Composite getControl() {
		return root;
	}
}