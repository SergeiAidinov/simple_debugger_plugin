package com.gmail.aydinov.sergey.simple_debugger_plugin.ui.tab;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.AbstractElementRepresentation.TargetApplicationElementType;
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
 * Tab content that displays inner elements (fields / methods / variables) at
 * the moment a breakpoint is hit, with editable values.
 */
public class ClassMembersAtBreakpoint {

	private final Composite root;
	private final TableViewer viewer;
	private final SimpleDebuggerEventCollector uiEventCollector = SimpleDebuggerEventCollector.instance();

	private static final Set<String> JAVA_STANDARD_TYPES = Set.of("int", "long", "short", "byte", "float", "double",
			"boolean", "char", "java.lang.Integer", "java.lang.Long", "java.lang.Short", "java.lang.Byte",
			"java.lang.Float", "java.lang.Double", "java.lang.Boolean", "java.lang.Character");

	public ClassMembersAtBreakpoint(Composite parent) {
		root = new Composite(parent, SWT.NONE);
		root.setLayout(new org.eclipse.swt.layout.GridLayout(1, false));

		Table table = new Table(root, SWT.BORDER | SWT.FULL_SELECTION);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		table.setLayoutData(new org.eclipse.swt.layout.GridData(org.eclipse.swt.layout.GridData.FILL_BOTH));

		viewer = new TableViewer(table);
		viewer.setContentProvider(ArrayContentProvider.getInstance());

		setupColumns();
		setupCellEditorsAndModifier();
		setupTooltips(table);
		setupInspectionListener(table);
	}

	// =================== Колонки ===================

	private void setupColumns() {
		createColumn("Name", 120, InnerElementRepresentationDTO::getName);

		createColumn("Type / Return Type", 200, InnerElementRepresentationDTO::getTypeName, dto -> {
			TargetApplicationElementType type = dto.getElementType();
			if (type == null)
				return null;

			String iconKey = switch (type) {
			case INTERFACE -> "interface";
			case METHOD -> "method";
			case STATIC_FIELD -> "static_field";
			case VARIABLE -> "variableIcon";
			case NON_STATIC_FIELD -> "fieldIcon";
			default -> null;
			};
			if (iconKey == null)
				return null;
			return DebugWindowsManager.instance().icons.get(iconKey);
		});

		createColumn("Value / Info", 300, InnerElementRepresentationDTO::getValue, dto -> {
			if (shouldShowInspectIcon(dto)) {
				return DebugWindowsManager.instance().icons.get("inspectIcon");
			}
			return null;
		});
	}

	private void createColumn(String title, int width, Function<InnerElementRepresentationDTO, String> extractor) {
		createColumn(title, width, extractor, e -> null);
	}

	private void createColumn(String title, int width, Function<InnerElementRepresentationDTO, String> textExtractor,
			Function<InnerElementRepresentationDTO, PairDTO<Image, String>> imageExtractor) {

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
				if (element instanceof InnerElementRepresentationDTO dto) {
					PairDTO<Image, String> pair = imageExtractor.apply(dto);
					if (pair != null) {
						TableItem item = findTableItem(dto);
						if (item != null)
							item.setData("tooltip", pair.getSecond());
						return pair.getFirst();
					}
				}
				return null;
			}
		});
	}

	private TableItem findTableItem(InnerElementRepresentationDTO dto) {
		for (TableItem item : viewer.getTable().getItems()) {
			if (item.getData() == dto)
				return item;
		}
		return null;
	}

	// =================== Редактирование ===================

	private void setupCellEditorsAndModifier() {
		viewer.setColumnProperties(new String[] { "name", "type", "value" });
		viewer.setCellEditors(new CellEditor[] { null, null, new TextCellEditor(viewer.getTable()) });

		viewer.setCellModifier(new ICellModifier() {

			@Override
			public boolean canModify(Object element, String property) {
				if (!"value".equals(property))
					return false;
				if (!(element instanceof InnerElementRepresentationDTO dto))
					return false;

				return isEditable(dto);
			}

			@Override
			public Object getValue(Object element, String property) {
				if (element instanceof InnerElementRepresentationDTO dto) {
					return dto.getValue() != null ? dto.getValue() : "";
				}
				return "";
			}

			@Override
			public void modify(Object element, String property, Object newValue) {
				if (!(element instanceof TableItem item))
					return;
				InnerElementRepresentationDTO dto = (InnerElementRepresentationDTO) item.getData();
				if (dto == null || newValue == null)
					return;

				String newValStr = newValue.toString();
				dto.setValue(newValStr);

				switch (dto.getElementType()) {
				case STATIC_FIELD, NON_STATIC_FIELD -> updateFieldValue(dto, newValStr);
				case VARIABLE -> updateVariableValue(dto, newValStr);
				default -> {
				}
				}

				viewer.update(dto, new String[] { "value" });
			}
		});
	}

	private boolean isEditable(InnerElementRepresentationDTO dto) {
		String typeName = dto.getTypeName();
		if (typeName == null)
			return false;

		// Примитивы, их обертки и String редактируем
		if (JAVA_STANDARD_TYPES.contains(typeName) || "java.lang.String".equals(typeName))
			return true;

		// Пользовательские объекты (не java./javax.) — нельзя
		if (!typeName.startsWith("java.") && !typeName.startsWith("javax."))
			return false;

		// Остальные стандартные классы — редактируем
		return true;
	}

	private boolean shouldShowInspectIcon(InnerElementRepresentationDTO dto) {
		String typeName = dto.getTypeName();
		if (typeName == null)
			return false;

		return !JAVA_STANDARD_TYPES.contains(typeName) && !"java.lang.String".equals(typeName)
				&& !typeName.startsWith("java.") && !typeName.startsWith("javax.")
				&& (dto.getElementType() == TargetApplicationElementType.NON_STATIC_FIELD
						|| dto.getElementType() == TargetApplicationElementType.VARIABLE);
	}

	private void updateFieldValue(InnerElementRepresentationDTO dto, String newValue) {
		uiEventCollector.collectUiEvent(new UIEvent<>(SimpleDebuggerEventType.USER_CHANGED_FIELD,
				new UserChangedFieldEventDTO(dto.getName(), dto.getTypeName(), newValue)));
	}

	private void updateVariableValue(InnerElementRepresentationDTO dto, String newValue) {
		uiEventCollector.collectUiEvent(new UIEvent<>(SimpleDebuggerEventType.USER_CHANGED_VARIABLE,
				new UserChangedVariableEventDTO(dto.getName(), dto.getTypeName(), newValue)));
	}

	// =================== Отображение ===================

	public void showInnerElements(DebugWindowDataDTO parentDto) {
		if (parentDto == null)
			return;
		Set<InnerElementRepresentationDTO> innerElementsSet = parentDto.getInnerElements();
		if (innerElementsSet == null || innerElementsSet.isEmpty())
			return;

		List<InnerElementRepresentationDTO> sorted = innerElementsSet.stream().sorted((a, b) -> {
			int cmp = Integer.compare(a.getElementType().ordinal(), b.getElementType().ordinal());
			return cmp != 0 ? cmp : a.getName().compareTo(b.getName());
		}).toList();

		root.getDisplay().asyncExec(() -> {
			if (viewer.getTable().isDisposed())
				return;
			viewer.setInput(sorted);
		});
	}

	private void setupTooltips(Table table) {
		table.addListener(SWT.MouseHover, event -> {
			TableItem item = table.getItem(new org.eclipse.swt.graphics.Point(event.x, event.y));
			if (item != null && item.getData("tooltip") instanceof String tip) {
				table.setToolTipText(tip);
			} else {
				table.setToolTipText(null);
			}
		});
	}

	private void setupInspectionListener(Table table) {
		table.addListener(SWT.MouseDown, event -> {
			TableItem item = table.getItem(new org.eclipse.swt.graphics.Point(event.x, event.y));
			if (item == null)
				return;

			Object data = item.getData();
			if (!(data instanceof InnerElementRepresentationDTO dto))
				return;

			int columnIndex = getColumnIndexAtPoint(table, event.x);
			if (columnIndex != 2)
				return; // третья колонка

			if (shouldShowInspectIcon(dto)) {
				UIEvent<InnerElementRepresentationDTO> inspectEvent = new UIEvent<>(
						SimpleDebuggerEventType.USER_STARTED_INSPECTION_SESSION_FOR_ELEMENT, dto);
				uiEventCollector.collectUiEvent(inspectEvent);
			}
		});
	}

	private int getColumnIndexAtPoint(Table table, int x) {
		int total = 0;
		for (int i = 0; i < table.getColumnCount(); i++) {
			total += table.getColumn(i).getWidth();
			if (x < total)
				return i;
		}
		return -1;
	}

	public Composite getControl() {
		return root;
	}
}