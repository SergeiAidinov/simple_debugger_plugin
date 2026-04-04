package com.gmail.aydinov.sergey.simple_debugger_plugin.ui.tab;

import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.PairDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.inspection.ArrayPageDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.SimpleDebuggerEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.UIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebuggerEventTypes.SimpleDebuggerEventType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.UiEventCollector;

import java.util.List;
import java.util.function.Function;

public class ArrayInspectorTab {

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

    private int currentPage = 0;

    public ArrayInspectorTab(Composite parent) {
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
        prevButton.addListener(SWT.Selection, e ->
                uiEventCollector.collectUiEvent(
                        new UIEvent<>(SimpleDebuggerEventType.USER_REQUESTED_COLLECTION_PAGE, currentPage - 1)
                ));

        pageText = new Text(paginationComposite, SWT.BORDER);
        pageText.setLayoutData(new GridData(70, SWT.DEFAULT));

        goButton = new Button(paginationComposite, SWT.PUSH);
        goButton.setText("Go");
        goButton.addListener(SWT.Selection, e -> requestPage());
        pageText.addListener(SWT.DefaultSelection, e -> requestPage());

        nextButton = new Button(paginationComposite, SWT.PUSH);
        nextButton.setText("Next");
        nextButton.addListener(SWT.Selection, e ->
                uiEventCollector.collectUiEvent(
                        new UIEvent<>(SimpleDebuggerEventType.USER_REQUESTED_COLLECTION_PAGE, currentPage + 1)
                ));

        // ===== Table =====
        Table table = new Table(root, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        viewer = new TableViewer(table);
        viewer.setContentProvider(ArrayContentProvider.getInstance());

        // Колонки
        createColumn("Index", 80,
                pair -> String.valueOf(pair.getFirst()));

        createColumn("Value", 600,
                pair -> formatValue((InnerElementRepresentationDTO) pair.getSecond()));
    }

    public Composite getControl() {
        return root;
    }

    public void showPage(ArrayPageDTO page) {
        root.getDisplay().asyncExec(() -> {
            if (root.isDisposed() || viewer.getTable().isDisposed()) return;

            collectionNameLabel.setText("Collection name: " + safe(page.getElementName()));
            collectionTypeLabel.setText("Collection type: " + safe(page.getElementType()));
            elementTypeLabel.setText("Element type: " + safe(page.getElementType()));
            sizeLabel.setText("Size: " + page.getTotalElements());

            pageInfoLabel.setText(
                    "Page (0-based): " + page.getCurrentPage() +
                    " of " + page.getTotalPages() +
                    "   Showing: " + page.getFromIndex() + "–" + page.getToIndex()
            );

            pageText.setText(String.valueOf(page.getCurrentPage()));
            currentPage = page.getCurrentPage();

            prevButton.setEnabled(page.hasPreviousPage());
            nextButton.setEnabled(page.hasNextPage());

            List<PairDTO<Integer, InnerElementRepresentationDTO>> entries = page.getEntries();
            viewer.setInput(entries);

            root.layout(true, true);
        });
    }

    private void requestPage() {
        int page;
        try {
            page = Integer.parseInt(pageText.getText().trim());
        } catch (Exception e) {
            page = 0;
        }

        if (page < 0) page = 0;

        uiEventCollector.collectUiEvent(
                new UIEvent<>(SimpleDebuggerEventType.USER_REQUESTED_COLLECTION_PAGE, page)
        );
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String formatValue(InnerElementRepresentationDTO dto) {
        if (dto == null) return "";

        String value = dto.getValue();
        if (value == null) return "null";

        String type = dto.getTypeOrReturnType();

        // Можно легко расширить потом
        if ("String".equals(type)) {
            return "\"" + value + "\"";
        }

        return value;
    }

    private <K, V> TableViewerColumn createColumn(
            String title,
            int width,
            Function<PairDTO<K, V>, String> textExtractor
    ) {
        TableViewerColumn column = new TableViewerColumn(viewer, SWT.NONE);
        column.getColumn().setText(title);
        column.getColumn().setWidth(width);

        column.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                if (element instanceof PairDTO<?, ?> pair) {
                    @SuppressWarnings("unchecked")
                    PairDTO<K, V> typedPair = (PairDTO<K, V>) pair;

                    String text = textExtractor.apply(typedPair);
                    return text != null ? text : "";
                }
                return "";
            }
        });

        return column;
    }
}