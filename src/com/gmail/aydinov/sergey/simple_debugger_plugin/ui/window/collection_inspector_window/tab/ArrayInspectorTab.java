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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;

import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.CollectionEntryDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.SimpleDebugerWindowsManager;

public class ArrayInspectorTab implements InspectorTab{

    private final Composite root;
    private final TableViewer viewer;

    public ArrayInspectorTab(Composite parent) {

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

    // =========================================================
    // Columns
    // =========================================================

    private void setupColumns() {

        createColumn(
                "Index",
                80,
                e -> String.valueOf("default"),
                e -> null
        );

        createColumn(
                "Value",
                450,
                e -> e.getSignature() == null ? "" : e.getSignature().toString(),
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
                if (!(element instanceof CollectionEntryDTO dto))
                    return null;
                return imageExtractor.apply(dto);
            }
        });

        return column;
    }

    // =========================================================
    // Icons
    // =========================================================

    private Image getIcon(CollectionEntryDTO dto) {

        if (dto == null)
            return null;

        Object value = dto.getCollectionName();

        if (value == null)
            return null;

        if (value instanceof java.util.Collection || value instanceof java.util.Map) {
            return SimpleDebugerWindowsManager.instance()
                    .icons.get("lens").getFirst();
        }

        return SimpleDebugerWindowsManager.instance()
                .icons.get("inspectIcon").getFirst();
    }

	@Override
	public void showCollection(List<CollectionEntryDTO> elements) {
		// TODO Auto-generated method stub
		
	}
}