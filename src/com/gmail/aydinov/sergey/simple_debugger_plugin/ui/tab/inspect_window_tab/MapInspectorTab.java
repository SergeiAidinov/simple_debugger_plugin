package com.gmail.aydinov.sergey.simple_debugger_plugin.ui.tab.inspect_window_tab;

import java.util.Objects;
import java.util.function.Function;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
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

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.UniversalElementType;

// ...

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.ValueCategory;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.PairDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.details.UserInstanceDetailsDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.inspection.AbstractInspectionDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.inspection.MapPageDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebuggerEventTypes.SimpleDebuggerEventType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.SimpleDebuggerEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.UiEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.UIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.tooltip_manager.TooltipManager;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.utils.UiUtils;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.window.SimpleDebugerWindowsManager;

public class MapInspectorTab implements InspectorTab {

	private final UiEventCollector uiEventCollector = SimpleDebuggerEventCollector.instance();
	private final Composite root;
	private final TableViewer viewer;

	private final Label mapNameLabel;
	private final Label mapTypeLabel;
	private final Label sizeLabel;
	private final Label pageInfoLabel;

	private final Button prevButton;
	private final Text pageText;
	private final Button goButton;
	private final Button nextButton;

	private TooltipManager tooltipManager;
//	private InnerElementRepresentationDTO lastInspectedElement;
	private String lastInspectedElementId;

	private int currentPage = 0;

	public MapInspectorTab(Composite parent) {
		root = new Composite(parent, SWT.NONE);
		root.setLayout(new GridLayout(1, false));

		// Header
		Composite header = new Composite(root, SWT.NONE);
		header.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		header.setLayout(new GridLayout(2, false));

		Composite info = new Composite(header, SWT.NONE);
		info.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		info.setLayout(new GridLayout(1, false));

		mapNameLabel = new Label(info, SWT.NONE);
		mapNameLabel.setText("Map: ");

		mapTypeLabel = new Label(info, SWT.NONE);
		mapTypeLabel.setText("Map type: ");

		sizeLabel = new Label(info, SWT.NONE);
		sizeLabel.setText("Size: ");

		pageInfoLabel = new Label(info, SWT.NONE);
		pageInfoLabel.setText("Page: ");

		Composite pagination = new Composite(header, SWT.NONE);
		pagination.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
		pagination.setLayout(new GridLayout(4, false));

		prevButton = new Button(pagination, SWT.PUSH);
		prevButton.setText("Prev");
		prevButton.addListener(SWT.Selection, e -> requestPage(currentPage - 1));

		pageText = new Text(pagination, SWT.BORDER);
		pageText.setLayoutData(new GridData(70, SWT.DEFAULT));

		goButton = new Button(pagination, SWT.PUSH);
		goButton.setText("Go");
		goButton.addListener(SWT.Selection, e -> requestPageFromText());
		pageText.addListener(SWT.DefaultSelection, e -> requestPageFromText());

		nextButton = new Button(pagination, SWT.PUSH);
		nextButton.setText("Next");
		nextButton.addListener(SWT.Selection, e -> requestPage(currentPage + 1));

		// Table
		Table table = new Table(root, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		setupTooltips(table);

		viewer = new TableViewer(table);
		viewer.setContentProvider(ArrayContentProvider.getInstance());
		// ColumnViewerToolTipSupport.enableFor(viewer,
		// org.eclipse.jface.window.ToolTip.NO_RECREATE);
		// setupColumns();
		// setupTooltips(table);
		// setupColumnClickListeners();
		setupHoverInspectionListener();

		// Колонки
		createColumn("Key", 80, pair -> {
			if (pair.getFirst() instanceof InnerElementRepresentationDTO keyRepresentation) {
				return keyRepresentation.getElementName();
			}
			return "";
		}, pair -> {
			if (pair.getFirst() instanceof InnerElementRepresentationDTO keyRepresentation) {
				return UiUtils.getIcon(keyRepresentation);
			} else
				return null;
		}, pair -> {
			return null;
		});

		createColumn("Value", 570, pair -> {
			if (pair.getSecond() instanceof InnerElementRepresentationDTO valueRepresentation) {
				return valueRepresentation.getValue() + " id=" + valueRepresentation.getAdditionalInfo() + ")";
			}
			return "";
		}, pair -> {
			if (pair.getSecond() instanceof InnerElementRepresentationDTO valueRepresentation) {
				return getIconForValue(valueRepresentation);
			}
			return null;
		}, pair -> {
			if (pair.getSecond() instanceof InnerElementRepresentationDTO valueRepresentation) {
				return getTooltipForValue(valueRepresentation);
			}
			return "default tooltip";
		});
	}

	private Image getIconForValue(InnerElementRepresentationDTO dto) {
		if (dto == null)
			return null;

		ValueCategory category = dto.getValueCategory();
		if (category == null)
			return null;

		if (category == ValueCategory.COLLECTION || category == ValueCategory.MAP) {
			return SimpleDebugerWindowsManager.instance().icons.get("lens").getFirst();
		}

		if (category == ValueCategory.USER_OBJECT && dto.getValue() != null
				&& !UiUtils.isStandartJavaType(dto.getTypeOrReturnType())) {
			return SimpleDebugerWindowsManager.instance().icons.get("inspectIcon").getFirst();
		}

		return null;
	}

	private String getTooltipForValue(InnerElementRepresentationDTO dto) {
		if (dto == null)
			return "null";
		var pair = UiUtils.getTypeTooltip(dto);
		return pair != null ? pair.getSecond() : dto.getTypeOrReturnType();
	}

	@Override
	public Composite getControl() {
		return root;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void showPage(AbstractInspectionDTO abstractInspectionDTO) {
		if (!(abstractInspectionDTO instanceof MapPageDTO))
			return;
		MapPageDTO<InnerElementRepresentationDTO, InnerElementRepresentationDTO> page = (MapPageDTO<InnerElementRepresentationDTO, InnerElementRepresentationDTO>) abstractInspectionDTO;
		root.getDisplay().asyncExec(() -> {
			if (root.isDisposed() || viewer.getTable().isDisposed())
				return;

			mapNameLabel.setText("Map name: " + safe(page.getElementName()));
			mapTypeLabel.setText("Map type: " + safe(page.getElementType()));
			sizeLabel.setText("Size: " + page.getTotalElements());
			pageInfoLabel.setText("Page (0-based): " + page.getCurrentPage() + " of " + page.getTotalPages()
					+ "   Showing: " + page.getFromIndex() + "–" + page.getToIndex());

			pageText.setText(String.valueOf(page.getCurrentPage()));
			currentPage = page.getCurrentPage();
			prevButton.setEnabled(page.getCurrentPage() > 0);
			nextButton.setEnabled(page.getCurrentPage() < page.getTotalPages() - 1);

			viewer.setInput(page.getEntries());
			root.layout(true, true);
		});
	}

	private void requestPageFromText() {
		int page;
		try {
			page = Integer.parseInt(pageText.getText().trim());
		} catch (Exception e) {
			page = 0;
		}
		if (page < 0)
			page = 0;
		requestPage(page);
	}

	private void requestPage(int page) {
		uiEventCollector.collectUiEvent(new UIEvent<>(SimpleDebuggerEventType.USER_REQUESTED_MAP_PAGE, page));
	}

	@SuppressWarnings("unchecked")
	private <K, V> TableViewerColumn createColumn(String title, int width,
			Function<PairDTO<K, V>, String> textExtractor, Function<PairDTO<K, V>, Image> imageExtractor,
			Function<PairDTO<K, V>, String> tooltipExtractor) {

		TableViewerColumn column = new TableViewerColumn(viewer, SWT.NONE);
		column.getColumn().setText(title);
		column.getColumn().setWidth(width);

		column.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof PairDTO<?, ?> pair) {
					String text = textExtractor.apply((PairDTO<K, V>) pair);
					return text != null ? text : "";
				}
				return "";
			}

			@Override
			public Image getImage(Object element) {
				if (!(element instanceof PairDTO<?, ?> pair))
					return null;
				return imageExtractor.apply((PairDTO<K, V>) pair);
			}

//            @Override
//            public String getToolTipText(Object element) {
//                if (!(element instanceof PairDTO<?, ?> pair)) return null;
//                return tooltipExtractor.apply((PairDTO<K, V>) pair);
//            }
		});

		return column;
	}

	private String safe(String value) {
		return value == null ? "" : value;
	}

	private void setupHoverInspectionListener() {
		Table table = viewer.getTable();
		table.addListener(SWT.MouseMove, event -> {
			TableItem item = table.getItem(new Point(event.x, event.y));
			InnerElementRepresentationDTO dto = null;
			if (item != null && item.getData() instanceof PairDTO pair) {
				InnerElementRepresentationDTO dataDto = (InnerElementRepresentationDTO) pair.getSecond();
				int colIndex = getColumnIndexAtPoint(table, event.x);
				if (colIndex == 1) {
					Image icon = getIcon(dataDto); // <- вызываем один раз
					if (icon == SimpleDebugerWindowsManager.instance().icons.get("inspectIcon").getFirst()
							|| icon == SimpleDebugerWindowsManager.instance().icons.get("lens").getFirst()) {
						dto = dataDto;
					}
					String currentId = dto != null ? dto.getAdditionalInfo() : null;
					if (!Objects.equals(currentId, lastInspectedElementId)) {
						lastInspectedElementId = currentId;
						tooltipManager.closePopup();
						if (dto != null) {
							if (icon == SimpleDebugerWindowsManager.instance().icons.get("inspectIcon").getFirst()) {
								uiEventCollector.collectUiEvent(new UIEvent<>(
										SimpleDebuggerEventType.USER_REQUESTED_ADDITIONAL_INFO_ABOUT_OBJECT, dto));
							} else if (icon == SimpleDebugerWindowsManager.instance().icons.get("lens").getFirst()) {
								uiEventCollector.collectUiEvent(new UIEvent<>(
										SimpleDebuggerEventType.USER_REQUESTED_ADDITIONAL_INFO_ABOUT_COLLECTION, dto));
								Point location = root.getDisplay().getCursorLocation();
								tooltipManager.showTooltipForCollection(dto, location);
							}
						}
					}
				}
			}
		});
	}

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
		if ((dto.getElementType() == UniversalElementType.MAP_ELEMENT) && category == ValueCategory.USER_OBJECT
				&& dto.getValue() != null && !UiUtils.isStandartJavaType(dto.getTypeOrReturnType())) {
			return SimpleDebugerWindowsManager.instance().icons.get("inspectIcon").getFirst();
		}
		return null;
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

	public void showFieldInfoPopupFromBackend(UserInstanceDetailsDTO dto) {
        Display display = root.getDisplay();

        display.asyncExec(() -> {
            if (root.isDisposed()) return;

            Point location = display.getCursorLocation();
            tooltipManager.showTooltipForUserObject(dto, location);
        });
    }
}