package com.gmail.aydinov.sergey.simple_debugger_plugin.ui.tab.inspect_window_tab;

import java.util.List;
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

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.UniversalElementType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.ValueCategory;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.inspection.UserObjectPageDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.utils.UiUtils;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.window.SimpleDebugerWindowsManager;

/**
 * Простая вкладка инспектора объекта с иконками.
 */
public class UserObjectStructureTab {

	private final Composite root;
	private final TableViewer viewer;

	public UserObjectStructureTab(Composite parent) {
		root = new Composite(parent, SWT.NONE);
		root.setLayout(new GridLayout(1, false));

		Table table = new Table(root, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		table.setLayoutData(new GridData(GridData.FILL_BOTH));

		viewer = new TableViewer(table);
		viewer.setContentProvider(ArrayContentProvider.getInstance());

		// Колонки
		createColumn("Name", 250, InnerElementRepresentationDTO::getElementName);

		createColumn("Value", 500, dto -> dto.getValue() != null ? dto.getValue() : "null", this::getIcon);
	}

	public Composite getControl() {
		return root;
	}

	/**
	 * Отображение объекта
	 */
	public void showUserObject(UserObjectPageDTO dto) {
		List<InnerElementRepresentationDTO> entries = (dto == null || dto.getEntries() == null) ? List.of()
				: dto.getEntries();

		viewer.setInput(entries);
		viewer.refresh();
	}

	/**
	 * Колонка без иконок
	 */
	private TableViewerColumn createColumn(String title, int width,
			Function<InnerElementRepresentationDTO, String> extractor) {
		return createColumn(title, width, extractor, e -> null);
	}

	/**
	 * Колонка с иконками
	 */
	private TableViewerColumn createColumn(String title, int width,
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
				if (element instanceof InnerElementRepresentationDTO dto) {
					return imageExtractor.apply(dto);
				}
				return null;
			}
		});

		return column;
	}

	/**
	 * Логика выбора иконки (та же, что в основной вкладке)
	 */
	private Image getIcon(InnerElementRepresentationDTO dto) {
		if (dto == null)
			return null;

		ValueCategory category = dto.getValueCategory();
		if (category == ValueCategory.COLLECTION || category == ValueCategory.MAP) {
			return SimpleDebugerWindowsManager.instance().icons.get("lens").getFirst();
		} else if (category == ValueCategory.USER_OBJECT && dto.getValue() != null
				&& !UiUtils.isStandartJavaType(dto.getTypeOrReturnType())) {
			return SimpleDebugerWindowsManager.instance().icons.get("inspectIcon").getFirst();
		}
		UniversalElementType type = dto.getElementType();
		if (type == UniversalElementType.METHOD)
			return SimpleDebugerWindowsManager.instance().icons.get("method").getFirst();
		else if (type == UniversalElementType.FIELD && dto.isStatic())
			return SimpleDebugerWindowsManager.instance().icons.get("static_field").getFirst();
		else if (type == UniversalElementType.FIELD && !dto.isStatic())
			return SimpleDebugerWindowsManager.instance().icons.get("fieldIcon").getFirst();

		return null;
	}
}