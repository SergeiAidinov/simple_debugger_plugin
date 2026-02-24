package com.gmail.aydinov.sergey.simple_debugger_plugin.ui.tab;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.FieldOrVariableDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.UserChangedFieldEventDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.UserChangedVariableEventDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebuggerEventTypes.SimpleDebuggerEventType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.SimpleDebuggerEventQueue;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.UiEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.UIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.DebugWindowsManager;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.InspectWindow;
import com.gmail.aydinov.sergey.simple_debugger_plugin.utils.DebugUtils;

/**
 * Tab for displaying object fields and local variables. Supports universal
 * "inspect" for objects and collections.
 */
public class FieldsAndVariablesTabContent {

	private final Composite root;
	private final Table table;
	private final TableViewer viewer;
	private final List<FieldOrVariableDTO> entries = new ArrayList<>();
	UiEventCollector uiEventCollector = SimpleDebuggerEventQueue.instance();

	public FieldsAndVariablesTabContent(Composite parent) {

		root = new Composite(parent, SWT.NONE);
		root.setLayout(new GridLayout(1, false));

		table = new Table(root, SWT.BORDER | SWT.FULL_SELECTION);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		table.setLayoutData(new GridData(GridData.FILL_BOTH));

		viewer = new TableViewer(table);
		viewer.setContentProvider(ArrayContentProvider.getInstance());
		ColumnViewerToolTipSupport.enableFor(viewer, ToolTip.NO_RECREATE);

		setupColumns();
		setupCellModifier();
		setupClickListener();
	}

	private void setupColumns() {
		// Name column
		TableViewerColumn nameColumn = new TableViewerColumn(viewer, SWT.NONE);
		nameColumn.getColumn().setText("Name");
		nameColumn.getColumn().setWidth(200);
		nameColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof FieldOrVariableDTO dto)
					return Objects.toString(dto.getName(), "");
				return "";
			}
		});

		// Type column (icon + type text + tooltip)
		TableViewerColumn typeColumn = new TableViewerColumn(viewer, SWT.NONE);
		typeColumn.getColumn().setText("Type");
		typeColumn.getColumn().setWidth(140);
		typeColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof FieldOrVariableDTO dto)
					return Objects.toString(dto.getType(), "");
				return "";
			}

			@Override
			public Image getImage(Object element) {
				if (element instanceof FieldOrVariableDTO dto) {
					return switch (dto.getFieldOrVariableType()) {
					case VARIABLE -> DebugWindowsManager.instance().icons.get("variableIcon");
					case NON_STATIC_FIELD -> DebugWindowsManager.instance().icons.get("fieldIcon");
					};
				}
				return null;
			}

			@Override
			public String getToolTipText(Object element) {
				if (element instanceof FieldOrVariableDTO dto) {
					return switch (dto.getFieldOrVariableType()) {
					case VARIABLE -> "Local variable";
					case NON_STATIC_FIELD -> "Non-static field";
					};
				}
				return null;
			}
		});

		// Value column (text or inspect icon + tooltip)
		TableViewerColumn valueColumn = new TableViewerColumn(viewer, SWT.NONE);
		valueColumn.getColumn().setText("Value");
		valueColumn.getColumn().setWidth(200);
		valueColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof FieldOrVariableDTO dto) {
					if (DebugUtils.isInspectable(dto))
						return "";
					return Objects.toString(dto.getValue(), "");
				}
				return "";
			}

			@Override
			public Image getImage(Object element) {
				if (element instanceof FieldOrVariableDTO dto && DebugUtils.isInspectable(dto)) {
					return DebugWindowsManager.instance().icons.get("inspectIcon");
				}
				return null;
			}

			@Override
			public String getToolTipText(Object element) {
				if (element instanceof FieldOrVariableDTO dto && DebugUtils.isInspectable(dto)) {
					return "Inspect object";
				}
				return null;
			}
		});

		viewer.setColumnProperties(new String[] { "name", "type", "value" });
		viewer.setCellEditors(new CellEditor[] { null, null, new TextCellEditor(table) });
	}

	private void setupCellModifier() {
		viewer.setCellModifier(new ICellModifier() {
			@Override
			public boolean canModify(Object element, String property) {
				return "value".equals(property)
						&& !(element instanceof FieldOrVariableDTO dto && DebugUtils.isInspectable(dto));
			}

			@Override
			public Object getValue(Object element, String property) {
				if (element instanceof FieldOrVariableDTO dto)
					return dto.getValue();
				return null;
			}

			@Override
			public void modify(Object element, String property, Object newValue) {
				if (!(element instanceof TableItem item))
					return;

				FieldOrVariableDTO oldEntry = (FieldOrVariableDTO) item.getData();
				if (Objects.isNull(oldEntry) || Objects.isNull(newValue))
					return;

				String newValStr = newValue.toString();

				switch (oldEntry.getFieldOrVariableType()) {
				case VARIABLE -> uiEventCollector.collectUiEvent(
						new UIEvent<UserChangedVariableEventDTO>(SimpleDebuggerEventType.USER_CHANGED_VARIABLE,
								new UserChangedVariableEventDTO(oldEntry.getName(), oldEntry.getType(), newValStr)));
				case NON_STATIC_FIELD -> uiEventCollector
						.collectUiEvent(new UIEvent<UserChangedFieldEventDTO>(SimpleDebuggerEventType.USER_CHANGED_FIELD,
								new UserChangedFieldEventDTO(oldEntry.getName(), oldEntry.getType(), newValStr)));

				}

				int index = -1;
				for (int i = 0; i < entries.size(); i++) {
					if (Objects.equals(entries.get(i), oldEntry)) {
						index = i;
						break;
					}
				}
				if (index >= 0) {
					FieldOrVariableDTO updated = new FieldOrVariableDTO(oldEntry.getName(), oldEntry.getType(),
							newValStr, oldEntry.getFieldOrVariableType());
					entries.set(index, updated);
					viewer.update(updated, null);
				}
			}
		});
	}

	private void setupClickListener() {
		table.addListener(SWT.MouseDown, event -> {
			Point pt = new Point(event.x, event.y);
			TableItem item = table.getItem(pt);
			if (item == null)
				return;

			for (int i = 0; i < table.getColumnCount(); i++) {
				if (item.getBounds(i).contains(pt) && i == 2) { // Value column
					FieldOrVariableDTO dto = (FieldOrVariableDTO) item.getData();
					if (DebugUtils.isInspectable(dto))
						inspectNode(dto);
					break;
				}
			}
		});
	}

	/** Delegates opening the inspection window to DebugWindowManager */
	private void inspectNode(FieldOrVariableDTO fieldOrVariableDTO) {
//       InspectWindow window = DebugWindowManager.instance().openNewInspectWindow();
//        if (window == null) {
//            System.err.println("Debug session is not running, cannot inspect object.");
//            return;
//        }
		// Display.getDefault().asyncExec(() -> window.showInspectableNode(dto));
		uiEventCollector.collectUiEvent(new UIEvent<FieldOrVariableDTO>(SimpleDebuggerEventType.USER_STARTED_INSPECTION_SESSION_FOR_ELEMENT, fieldOrVariableDTO));
	}

	public void updateVariablesAndFields(List<FieldOrVariableDTO> variables, List<FieldOrVariableDTO> fields) {
		if (table.isDisposed())
			return;
		entries.clear();
		if (variables != null)
			entries.addAll(variables);
		if (fields != null)
			entries.addAll(fields);

		viewer.setInput(entries);
		viewer.refresh();
	}

	public Composite getControl() {
		return root;
	}
}