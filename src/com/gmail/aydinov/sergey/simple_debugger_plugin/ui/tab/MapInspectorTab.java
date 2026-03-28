package com.gmail.aydinov.sergey.simple_debugger_plugin.ui.tab;

import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.PairDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.MapPageDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.SimpleDebuggerEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.UiEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebuggerEventTypes.SimpleDebuggerEventType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.UIEvent;

import java.util.List;
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

        Table table = new Table(root, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        viewer = new TableViewer(table);
        viewer.setContentProvider(ArrayContentProvider.getInstance());

        createColumn("Key", 200, pair -> pair.getFirst().toString(), pair -> null);
        createColumn("Value", 450, pair -> ((InnerElementRepresentationDTO) pair.getSecond()).getValue(), pair -> null);
    }

    public Composite getControl() {
        return root;
    }

    public void showPage(MapPageDTO page) {
        root.getDisplay().asyncExec(() -> {
            if (root.isDisposed() || viewer.getTable().isDisposed())
                return;

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
                                                   Function<PairDTO<K, V>, Image> imageExtractor) {
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
        });
        return column;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}