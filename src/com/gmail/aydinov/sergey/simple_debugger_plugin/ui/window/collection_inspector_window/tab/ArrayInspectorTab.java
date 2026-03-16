package com.gmail.aydinov.sergey.simple_debugger_plugin.ui.window.collection_inspector_window.tab;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.CollectionPageDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;
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
        collectionNameLabel.setText("Collection: ");

        collectionTypeLabel = new Label(infoComposite, SWT.NONE);
        collectionTypeLabel.setText("Collection type: ");

        elementTypeLabel = new Label(infoComposite, SWT.NONE);
        elementTypeLabel.setText("Element type: ");

        sizeLabel = new Label(infoComposite, SWT.NONE);
        sizeLabel.setText("Size: ");

        pageInfoLabel = new Label(infoComposite, SWT.NONE);
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
        GridData pageTextGridData = new GridData();
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

    // =========================================================
    // Показ страницы
    // =========================================================

    public void showPage(CollectionPageDTO page) {

        root.getDisplay().asyncExec(() -> {

            if (root.isDisposed() || viewer.getTable().isDisposed()) {
                return;
            }

            collectionNameLabel.setText("Collection: " + safe(page.getCollectionName()));
            collectionTypeLabel.setText("Collection type: " + safe(page.getCollectionType()));
            elementTypeLabel.setText("Element type: " + safe(page.getElementType()));

            sizeLabel.setText("Size: " + page.getTotalElements());

            pageInfoLabel.setText(
                    "Page: " + page.getCurrentPage() +
                            " of " + page.getTotalPages() +
                            "   Showing: " + page.getFromIndex() +
                            "–" + page.getToIndex()
            );

            pageText.setText(String.valueOf(page.getCurrentPage()));

            prevButton.setEnabled(page.hasPreviousPage());
            nextButton.setEnabled(page.hasNextPage());

            viewer.setInput(page.getEntries());

            root.layout(true, true);
        });
    }

    // =========================================================
    // Пагинация
    // =========================================================

    private void requestPage() {

        int page;

        try {
            page = Integer.parseInt(pageText.getText().trim());
        } catch (Exception e) {
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

    // =========================================================
    // Таблица
    // =========================================================

    private void setupColumns() {

        createColumn(
                "Name",
                220,
                InnerElementRepresentationDTO::getElementName,
                this::getIcon
        );

        createColumn(
                "Value",
                400,
                InnerElementRepresentationDTO::getValue,
                e -> null
        );

        createColumn(
                "Type",
                180,
                InnerElementRepresentationDTO::getTypeOrReturnType,
                e -> null
        );
    }

    private TableViewerColumn createColumn(
            String title,
            int width,
            java.util.function.Function<InnerElementRepresentationDTO, String> textExtractor,
            java.util.function.Function<InnerElementRepresentationDTO, Image> imageExtractor) {

        TableViewerColumn column = new TableViewerColumn(viewer, SWT.NONE);

        column.getColumn().setText(title);
        column.getColumn().setWidth(width);

        column.setLabelProvider(new ColumnLabelProvider() {

            @Override
            public String getText(Object element) {

                if (element instanceof InnerElementRepresentationDTO dto) {

                    String text = textExtractor.apply(dto);
                    return text == null ? "" : text;
                }

                return "";
            }

            @Override
            public Image getImage(Object element) {

                if (!(element instanceof InnerElementRepresentationDTO dto)) {
                    return null;
                }

                return imageExtractor.apply(dto);
            }
        });

        return column;
    }

    // =========================================================
    // Иконки
    // =========================================================

    private Image getIcon(InnerElementRepresentationDTO dto) {

        if (dto == null) {
            return null;
        }

        if (dto.getValueCategory() ==
                com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.ValueCategory.COLLECTION) {

            return SimpleDebugerWindowsManager.instance().icons.get("lens").getFirst();
        }

        return SimpleDebugerWindowsManager.instance().icons.get("inspectIcon").getFirst();
    }

    @Override
    public void showCollection(java.util.List<InnerElementRepresentationDTO> elements) {
        viewer.setInput(elements);
    }
}