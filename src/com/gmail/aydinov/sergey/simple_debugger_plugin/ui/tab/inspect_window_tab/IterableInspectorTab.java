package com.gmail.aydinov.sergey.simple_debugger_plugin.ui.tab.inspect_window_tab;

import java.util.Objects;
import java.util.function.Function;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.ValueCategory;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.PairDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.details.UserInstanceDetailsDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.inspection.AbstractInspectionDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.inspection.ArrayPageDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebuggerEventTypes.SimpleDebuggerEventType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.SimpleDebuggerEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.UiEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.UIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.tooltip_manager.TooltipManager;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.utils.UiUtils;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.window.SimpleDebugerWindowsManager;

public class IterableInspectorTab implements InspectorTab {

	private final UiEventCollector uiEventCollector = SimpleDebuggerEventCollector.instance();
	private final Composite root;
	private final TableViewer viewer;
	private final Label collectionNameLabel;
	private final Label collectionTypeLabel;
	private final Label elementTypeLabel;
	private final Label sizeLabel;
	private final Label pageInfoLabel;

	private final Button prevButton;
	private final Text pageText;
	private final Button goButton;
	private final Button nextButton;
	private TooltipManager tooltipManager;

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
		Table table = new Table(root, SWT.BORDER | SWT.FULL_SELECTION);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		table.setLayoutData(new GridData(GridData.FILL_BOTH));

		viewer = new TableViewer(table);
		viewer.setContentProvider(ArrayContentProvider.getInstance());

		setupTooltips(table);

		createColumn("Index", 80, pair -> String.valueOf(pair.getFirst()));

		createColumn("Value", 600, pair -> formatValue((InnerElementRepresentationDTO) pair.getSecond()),
				pair -> getIcon((InnerElementRepresentationDTO) pair.getSecond()));

		setupClickListener(table);
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


	private int getColumnIndexAtPoint(Table table, int x) {
		int offset = 0;
		for (int i = 0; i < table.getColumnCount(); i++) {
			offset += table.getColumn(i).getWidth();
			if (x < offset)
				return i;
		}
		return table.getColumnCount() - 1;
	}

	private void setupClickListener(Table table) {

		table.addListener(SWT.MouseDown, event -> {
			TableItem item = table.getItem(new Point(event.x, event.y));
			if (item == null)
				return;
			int colIndex = getColumnIndexAtPoint(table, event.x);
			if (colIndex != 1)
				return; // только третья колонка
			Object data = item.getData();
			if (!(data instanceof PairDTO<?, ?> pair))
				return;
			if (!(pair.getSecond() instanceof InnerElementRepresentationDTO dto))
				return;
			ValueCategory category = dto.getValueCategory();
			if (category == ValueCategory.USER_OBJECT)
				uiEventCollector.collectUiEvent(
					new UIEvent<>(SimpleDebuggerEventType.USER_CONTINUES_INSPECTION_SEANCE_FOR_USER_OBJECT, dto));
			else if (category == ValueCategory.MAP)
				uiEventCollector.collectUiEvent(
						new UIEvent<>(SimpleDebuggerEventType.USER_CONTINUES_INSPECTION_SEANCE_FOR_MAP, dto));
			else if (category == ValueCategory.COLLECTION )
				return;
			
		});
	}

	public void showFieldInfoPopupFromBackend(UserInstanceDetailsDTO userInstanceInspectionDTO) {
		if ((Objects.isNull(userInstanceInspectionDTO)))
			return;
		Display display = root.getDisplay();
		display.asyncExec(() -> {
			if (root.isDisposed())
				return;
			Point location = display.getCursorLocation();
			tooltipManager.showTooltipForUserObject(userInstanceInspectionDTO, location);
		});
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
		if (category == ValueCategory.COLLECTION || category == ValueCategory.MAP) 
			return SimpleDebugerWindowsManager.instance().icons.get("lens").getFirst();
		
		if (category == ValueCategory.USER_OBJECT && dto.getValue() != null
				&& !UiUtils.isStandartJavaType(dto.getTypeOrReturnType())) 
			return SimpleDebugerWindowsManager.instance().icons.get("inspectIcon").getFirst();
		return null;
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