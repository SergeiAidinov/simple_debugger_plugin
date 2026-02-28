package com.gmail.aydinov.sergey.simple_debugger_plugin.ui.tab;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.AbstractElementRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.InnerElementRepresentation;

import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.DebugWindowDataDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.DebugWindowsManager;

/**
 * Tab content that displays inner elements (fields / methods / variables) at
 * the moment a breakpoint is hit.
 */
public class ClassMembersAtBreakpoint {

	private final Composite root;
	private final TableViewer viewer;

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
	}

	/**
	 * Configures table columns.
	 */
	private void setupColumns() {

		// 1. Name
		createColumn("Name", 200, AbstractElementRepresentation::getElementName);

		// 2. Type (с иконкой)
		createColumn("Type", 120, element -> element.getElementType().name(), element -> {
			if (element instanceof InnerElementRepresentation inner) {
				switch (inner.getElementType()) {
				case INTERFACE:
					return DebugWindowsManager.instance().icons.get("interface");
				case METHOD:
					return DebugWindowsManager.instance().icons.get("method");
				case STATIC_FIELD:
					return DebugWindowsManager.instance().icons.get("static_field");
				case VARIABLE:
					return DebugWindowsManager.instance().icons.get("variableIcon");
				case NON_STATIC_FIELD:
					return DebugWindowsManager.instance().icons.get("fieldIcon");
				default:
					return null;
				}
			}
			return null;
		});

		// 3. Value
		createColumn("Value", 300, element -> {
			if (element instanceof InnerElementRepresentation inner) {
				return inner.getValue();
			}
			return "";
		});
	}

	/**
	 * Создаёт колонку с возможностью задавать текст и иконку.
	 */
	private void createColumn(String title, int width, Function<AbstractElementRepresentation, String> textExtractor,
			Function<AbstractElementRepresentation, Image> imageExtractor) {

		TableViewerColumn column = new TableViewerColumn(viewer, SWT.NONE);
		column.getColumn().setText(title);
		column.getColumn().setWidth(width);
		column.getColumn().setResizable(true);

		column.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof AbstractElementRepresentation repr) {
					String value = textExtractor.apply(repr);
					return value != null ? value : "";
				}
				return "";
			}

			@Override
			public Image getImage(Object element) {
				if (element instanceof AbstractElementRepresentation repr) {
					return imageExtractor.apply(repr);
				}
				return null;
			}
		});
	}

	/**
	 * Для обычных колонок без иконок.
	 */
	private void createColumn(String title, int width, Function<AbstractElementRepresentation, String> extractor) {
		createColumn(title, width, extractor, e -> null);
	}

	/**
	 * Displays only inner elements of the given DTO.
	 */
	public void showInnerElements(DebugWindowDataDTO parentDto) {
		if (parentDto == null)
			return;

		Set<InnerElementRepresentation> innerElementsSet = parentDto.getInnerElements();

		if (innerElementsSet == null || innerElementsSet.isEmpty())
			return;

		List<InnerElementRepresentation> innerElements = innerElementsSet.stream().toList();

		root.getDisplay().asyncExec(() -> {
			if (viewer.getTable().isDisposed())
				return;

			viewer.setInput(innerElements);
		});
	}

	public Composite getControl() {
		return root;
	}
}