package com.gmail.aydinov.sergey.simple_debugger_plugin.ui.tab;

import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.ValueCategory;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.PairDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.MapPageDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.SimpleDebuggerEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.UiEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebuggerEventTypes.SimpleDebuggerEventType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.UIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.utils.UiUtils;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.window.SimpleDebugerWindowsManager;

import java.util.function.Function;

public class MapInspectorTab {

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

        viewer = new TableViewer(table);
        viewer.setContentProvider(ArrayContentProvider.getInstance());

        // Колонки
        createColumn("Key", 80,
            pair -> {
                if (pair.getFirst() instanceof InnerElementRepresentationDTO keyRepresentation) {
                    return keyRepresentation.getElementName();
                }
                return "";
            },
            pair -> {
                if (pair.getFirst() instanceof InnerElementRepresentationDTO keyRepresentation) {
                	return UiUtils.getIcon(keyRepresentation);
                }
                	else return null;
//                    if (UiUtils.isStandartJavaType(keyRepresentation.getTypeOrReturnType())) {
//                     //   return SimpleDebugerWindowsManager.instance().icons.get("keyIcon").getFirst();
//                    	return null;
//                    } else {
//                    	return UiUtils.getIcon(keyRepresentation);
//                     //   return SimpleDebugerWindowsManager.instance().icons.get("lens").getFirst();
//                    }
//                }
//                return null;
            },
            pair -> {
                if (pair.getFirst() instanceof InnerElementRepresentationDTO keyRepresentation) {
                    return keyRepresentation.getTypeOrReturnType();
                }
                return null;
            }
        );

        createColumn("Value", 570,
            pair -> {
                if (pair.getSecond() instanceof InnerElementRepresentationDTO valueRepresentation) {
                    return valueRepresentation.getValue();
                }
                return "";
            },
            pair -> {
                if (pair.getSecond() instanceof InnerElementRepresentationDTO valueRepresentation) {
                    return getIconForValue(valueRepresentation);
                }
                return null;
            },
            pair -> {
                if (pair.getSecond() instanceof InnerElementRepresentationDTO valueRepresentation) {
                    return getTooltipForValue(valueRepresentation);
                }
                return null;
            }
        );
    }

    private Image getIconForValue(InnerElementRepresentationDTO dto) {
        if (dto == null) return null;

        ValueCategory category = dto.getValueCategory();
        if (category == null) return null;

        if (category == ValueCategory.COLLECTION || category == ValueCategory.MAP) {
            return SimpleDebugerWindowsManager.instance().icons.get("lens").getFirst();
        }

        if (category == ValueCategory.USER_OBJECT
                && dto.getValue() != null
                && !UiUtils.isStandartJavaType(dto.getTypeOrReturnType())) {
            return SimpleDebugerWindowsManager.instance().icons.get("inspectIcon").getFirst();
        }

        return null;
    }

    private String getTooltipForValue(InnerElementRepresentationDTO dto) {
        if (dto == null) return "null";
        var pair = UiUtils.getTypeTooltip(dto);
        return pair != null ? pair.getSecond() : dto.getTypeOrReturnType();
    }

    public Composite getControl() {
        return root;
    }

    public void showPage(MapPageDTO page) {
        root.getDisplay().asyncExec(() -> {
            if (root.isDisposed() || viewer.getTable().isDisposed()) return;

            mapNameLabel.setText("Map name: " + safe(page.getMapName()));
            mapTypeLabel.setText("Map type: " + safe(page.getMapType()));
            sizeLabel.setText("Size: " + page.getTotalEntries());
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
        if (page < 0) page = 0;
        requestPage(page);
    }

    private void requestPage(int page) {
        uiEventCollector.collectUiEvent(new UIEvent<>(SimpleDebuggerEventType.USER_REQUESTED_MAP_PAGE, page));
    }

    private <K, V> TableViewerColumn createColumn(String title, int width,
                                                   Function<PairDTO<K, V>, String> textExtractor,
                                                   Function<PairDTO<K, V>, Image> imageExtractor,
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
                if (!(element instanceof PairDTO<?, ?> pair)) return null;
                return imageExtractor.apply((PairDTO<K, V>) pair);
            }

            @Override
            public String getToolTipText(Object element) {
                if (!(element instanceof PairDTO<?, ?> pair)) return null;
                return tooltipExtractor.apply((PairDTO<K, V>) pair);
            }
        });

        return column;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}