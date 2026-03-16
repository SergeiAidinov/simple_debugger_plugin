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
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.PairDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebuggerEventTypes.SimpleDebuggerEventType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.SimpleDebuggerEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.UiEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.UIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.SimpleDebugerWindowsManager;

import java.util.List;
import java.util.function.Function;

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
    
    private int currentPage = 0;

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
                        new UIEvent<>(SimpleDebuggerEventType.USER_REQUESTED_COLLECTION_PAGE, currentPage - 1)
                )
        );

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
                )
        );

        Table table = new Table(root, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        viewer = new TableViewer(table);
        viewer.setContentProvider(ArrayContentProvider.getInstance());

        // Колонки: Index и Value
        createColumn("Index", 80,  pair -> pair.getFirst().toString(), pair -> null);
        createColumn("Value", 570, pair -> pair.getSecond().getValue(), this::getIcon);
    }

    public Composite getControl() {
        return root;
    }

    // =========================================================
    // Показ страницы
    // =========================================================

    public void showPage(CollectionPageDTO page) {
        root.getDisplay().asyncExec(() -> {
            if (root.isDisposed() || viewer.getTable().isDisposed()) return;

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
            currentPage = page.getCurrentPage();
            prevButton.setEnabled(page.hasPreviousPage());
            nextButton.setEnabled(page.hasNextPage());

            List<PairDTO<Integer, InnerElementRepresentationDTO>> entries = page.getEntries();
            viewer.setInput(entries);

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
        if (page < 1) page = 1;

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

    private <K,V> TableViewerColumn createColumn(
            String title,
            int width,
            Function<PairDTO<K,V>, String> textExtractor,
            Function<PairDTO<K,V>, Image> imageExtractor) {

        TableViewerColumn column = new TableViewerColumn(viewer, SWT.NONE);
        column.getColumn().setText(title);
        column.getColumn().setWidth(width);

        column.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                if (element instanceof PairDTO<?,?> pair) {
                    String text = textExtractor.apply((PairDTO<K,V>) pair);
                    return text != null ? text : "";
                }
                return "";
            }

            @Override
            public Image getImage(Object element) {
                if (!(element instanceof PairDTO<?,?> pair)) return null;
                return imageExtractor.apply((PairDTO<K,V>) pair);
            }
        });

        return column;
    }

    private Image getIcon(PairDTO<Integer, InnerElementRepresentationDTO> pair) {
        if (pair == null || pair.getSecond() == null) return null;

        InnerElementRepresentationDTO dto = pair.getSecond();

        if (dto.getValueCategory() ==
                com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.ValueCategory.COLLECTION) {
            return SimpleDebugerWindowsManager.instance().icons.get("lens").getFirst();
        }

        return SimpleDebugerWindowsManager.instance().icons.get("inspectIcon").getFirst();
    }

    @Override
    public void showCollection(List<InnerElementRepresentationDTO> elements) {
        // При использовании нового DTO этот метод можно временно не использовать
    }
}