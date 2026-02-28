package com.gmail.aydinov.sergey.simple_debugger_plugin.ui.tab;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.AbstractElementRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.InnerElementRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.DebugWindowDataDTO;

/**
 * Tab content that displays inner elements (fields / methods / variables)
 * at the moment a breakpoint is hit.
 */
public class ClassMembersAtBreakpoint {

    private final Composite root;
    private final TableViewer viewer;

    public ClassMembersAtBreakpoint(Composite parent) {
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

    /**
     * Configures table columns.
     */
    private void setupColumns() {

        // 1. Name
        createColumn("Name", 200,
                AbstractElementRepresentation::getElementName
        );

        // 2. Type
        createColumn("Type", 300,
                AbstractElementRepresentation::getFullQualifiedName
        );

        // 3. Value (temporary default)
        createColumn("Value", 300,
                element -> "—"
        );
    }

    private void createColumn(
            String title,
            int width,
            Function<AbstractElementRepresentation, String> extractor) {

        TableViewerColumn column = new TableViewerColumn(viewer, SWT.NONE);
        column.getColumn().setText(title);
        column.getColumn().setWidth(width);
        column.getColumn().setResizable(true);

        column.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                if (element instanceof AbstractElementRepresentation repr) {
                    String value = extractor.apply(repr);
                    return value != null ? value : "";
                }
                return "";
            }
        });
    }

    /**
     * Displays only inner elements of the given DTO.
     */
    public void showInnerElements(DebugWindowDataDTO parentDto) {
        if (parentDto == null)
            return;

        Set<InnerElementRepresentation> innerElementsSet =
                parentDto.getInnerElements();

        if (innerElementsSet == null || innerElementsSet.isEmpty())
            return;

        List<InnerElementRepresentation> innerElements =
                innerElementsSet.stream().toList();

        root.getDisplay().asyncExec(() -> {
            if (viewer.getTable().isDisposed())
                return;

            viewer.setInput(innerElements);
        });
    }

    public Composite getControl() {
        return root;
    }
}