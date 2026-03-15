package com.gmail.aydinov.sergey.simple_debugger_plugin.ui.window.collection_inspector_window.tab;

import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;

import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.CollectionEntryDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebuggerEventTypes.SimpleDebuggerEventType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.SimpleDebuggerEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.UiEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.UIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.SimpleDebugerWindowsManager;

public class ArrayInspectorTab implements InspectorTab {

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

    public ArrayInspectorTab(Composite parent) {
        root = new Composite(parent, SWT.NONE);
        root.setLayout(new GridLayout(1, false));

        Composite headerComposite = new Composite(root, SWT.NONE);
        headerComposite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        headerComposite.setLayout(new GridLayout(2, false));

        Composite infoComposite = new Composite(headerComposite, SWT.NONE);
        infoComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        infoComposite.setLayout(new GridLayout(1, false));

        collectionNameLabel = new Label(infoComposite, SWT.NONE);
        collectionNameLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        collectionNameLabel.setText("Collection: ");

        collectionTypeLabel = new Label(infoComposite, SWT.NONE);
        collectionTypeLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        collectionTypeLabel.setText("Collection type: ");

        elementTypeLabel = new Label(infoComposite, SWT.NONE);
        elementTypeLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        elementTypeLabel.setText("Element type: ");

        sizeLabel = new Label(infoComposite, SWT.NONE);
        sizeLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        sizeLabel.setText("Size: ");

        pageInfoLabel = new Label(infoComposite, SWT.NONE);
        pageInfoLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        pageInfoLabel.setText("Page: ");

        Composite paginationComposite = new Composite(headerComposite, SWT.NONE);
        paginationComposite.setLayoutData(new GridData(SWT.END, SWT.BEGINNING, false, false));
        paginationComposite.setLayout(new GridLayout(4, false));

        prevButton = new Button(paginationComposite, SWT.PUSH);
        prevButton.setText("Prev");
        prevButton.addListener(SWT.Selection, e ->
                uiEventCollector.collectUiEvent(
                        new UIEvent<>(SimpleDebuggerEventType.USER_REQUESTED_COLLECTION_PREVIOUS_PAGE, null)
                )
        );

        pageText = new Text(paginationComposite, SWT.BORDER);
        GridData pageTextGridData = new GridData(SWT.FILL, SWT.CENTER, false, false);
        pageTextGridData.widthHint = 70;
        pageText.setLayoutData(pageTextGridData);

        goButton = new Button(paginationComposite, SWT.PUSH);
        goButton.setText("Go");
        goButton.addListener(SWT.Selection, e -> requestPage());

        pageText.addListener(SWT.DefaultSelection, e -> requestPage());

        nextButton = new Button(paginationComposite, SWT.PUSH);
        nextButton.setText("Next");
        nextButton.addListener(SWT.Selection, e ->
                uiEventCollector.collectUiEvent(
                        new UIEvent<>(SimpleDebuggerEventType.USER_REQUESTED_COLLECTION_NEXT_PAGE, null)
                )
        );

        Table table = new Table(root, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        viewer = new TableViewer(table);
        viewer.setContentProvider(ArrayContentProvider.getInstance());

        setupColumns();
    }

    public Composite getControl() {
        return root;
    }

    public void showArray(List<CollectionEntryDTO> elements) {
        root.getDisplay().asyncExec(() -> {
            if (!viewer.getTable().isDisposed()) {
                viewer.setInput(elements);
                viewer.refresh();
            }
        });
    }

    public void showPage(
            String collectionName,
            String collectionType,
            String elementType,
            int totalElements,
            int currentPage,
            int totalPages,
            int fromIndex,
            int toIndex,
            List<CollectionEntryDTO> elements) {

        root.getDisplay().asyncExec(() -> {
            if (root.isDisposed() || viewer.getTable().isDisposed()) {
                return;
            }

            collectionNameLabel.setText("Collection: " + safe(collectionName));
            collectionTypeLabel.setText("Collection type: " + safe(collectionType));
            elementTypeLabel.setText("Element type: " + safe(elementType));
            sizeLabel.setText("Size: " + totalElements);
            pageInfoLabel.setText(
                    "Page: " + currentPage + " of " + totalPages + "    Showing: " + fromIndex + "–" + toIndex
            );

            pageText.setText(String.valueOf(currentPage));

            prevButton.setEnabled(currentPage > 1);
            nextButton.setEnabled(currentPage < totalPages);

            viewer.setInput(elements);
            viewer.refresh();

            root.layout(true, true);
        });
    }

    private void requestPage() {
        String rawText = pageText.getText();
        int page = 1;

        try {
            page = Integer.parseInt(rawText.trim());
        } catch (NumberFormatException e) {
            page = 1;
        }

        if (page < 1) {
            page = 1;
        }

        uiEventCollector.collectUiEvent(
                new UIEvent<>(SimpleDebuggerEventType.USER_REQUESTED_COLLECTION_PAGE, page)
        );
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private void setupColumns() {
        createColumn(
                "Index",
                80,
                e -> String.valueOf(e.getSize()),
                e -> null
        );

        createColumn(
                "Value",
                450,
                e -> e.getSignature() == null ? "" : e.getSignature(),
                this::getIcon
        );
    }

    private TableViewerColumn createColumn(
            String title,
            int width,
            java.util.function.Function<CollectionEntryDTO, String> textExtractor,
            java.util.function.Function<CollectionEntryDTO, Image> imageExtractor) {

        TableViewerColumn column = new TableViewerColumn(viewer, SWT.NONE);

        column.getColumn().setText(title);
        column.getColumn().setWidth(width);

        column.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                if (element instanceof CollectionEntryDTO dto) {
                    String text = textExtractor.apply(dto);
                    return text != null ? text : "";
                }
                return "";
            }

            @Override
            public Image getImage(Object element) {
                if (!(element instanceof CollectionEntryDTO dto)) {
                    return null;
                }
                return imageExtractor.apply(dto);
            }
        });

        return column;
    }

    private Image getIcon(CollectionEntryDTO dto) {
        if (dto == null) {
            return null;
        }

        Object value = dto.getCollectionName();

        if (value == null) {
            return null;
        }

        if (value instanceof java.util.Collection || value instanceof java.util.Map) {
            return SimpleDebugerWindowsManager.instance().icons.get("lens").getFirst();
        }

        return SimpleDebugerWindowsManager.instance().icons.get("inspectIcon").getFirst();
    }

    @Override
    public void showCollection(List<CollectionEntryDTO> elements) {
        showArray(elements);
    }
}