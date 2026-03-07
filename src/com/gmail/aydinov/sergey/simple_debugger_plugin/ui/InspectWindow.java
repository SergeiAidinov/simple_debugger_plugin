package com.gmail.aydinov.sergey.simple_debugger_plugin.ui;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;

import com.gmail.aydinov.sergey.simple_debugger_plugin.core.DebuggerContext;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.DebuggerContext.SimpleDebuggerStatus;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.TopLevelElementRepresentationDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebuggerEventTypes.SimpleDebuggerEventType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.SimpleDebuggerEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.UiEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.debug_event.AbstractDebugEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.debug_event.DebugEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.UIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.DebugWindowsManager;

/**
 * Inspect window with left Tree (element structure) and right Table (object values)
 * Fully adapted to InnerElementRepresentation with history and breadcrumb.
 */
public class InspectWindow {

    private final Shell shell;
    private final Tree elementTree;
    private final Table table;
    private final Label objectLabel;
    private final Button backButton;
    private final Button forwardButton;
    private final Composite breadcrumbComposite;

    private final Deque<InnerElementRepresentationDTO> backHistory = new ArrayDeque<>();
    private final Deque<InnerElementRepresentationDTO> forwardHistory = new ArrayDeque<>();

    private final UiEventCollector uiEventCollector = SimpleDebuggerEventCollector.instance();

    protected InspectWindow() {
        shell = new Shell(Display.getDefault());
        shell.setText("Inspect Object");
        shell.setSize(1400, 800);
        shell.setLayout(new GridLayout(2, true));

        shell.setImage(DebugWindowsManager.instance().icons.get("debugger").getFirst());

        shell.addListener(SWT.Close, e -> {
            e.doit = true; // блокируем закрытие
            uiEventCollector.collectUiEvent(new UIEvent<Void>(SimpleDebuggerEventType.USER_ENDED_INSPECTION_SESSION_FOR_ELEMENT, null));
            DebuggerContext.context().setStatus(SimpleDebuggerStatus.INSPECTION_SEANCE_CLOSING);
        });

        // --- Tree ---
        elementTree = new Tree(shell, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        elementTree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        TreeColumn nameCol = new TreeColumn(elementTree, SWT.NONE);
        nameCol.setText("Name / Type");
        nameCol.setWidth(300);

        TreeColumn valueCol = new TreeColumn(elementTree, SWT.NONE);
        valueCol.setText("Value / Signature");
        valueCol.setWidth(200);

        // --- Right panel ---
        Composite rightPanel = new Composite(shell, SWT.NONE);
        rightPanel.setLayout(new GridLayout(1, false));
        rightPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        // Navigation panel
        Composite topPanel = new Composite(rightPanel, SWT.NONE);
        topPanel.setLayout(new GridLayout(4, false));
        topPanel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        backButton = new Button(topPanel, SWT.PUSH);
        backButton.setText("◀ Back");
        backButton.setEnabled(false);
        backButton.addListener(SWT.Selection, e -> navigateBack());

        forwardButton = new Button(topPanel, SWT.PUSH);
        forwardButton.setText("Forward ▶");
        forwardButton.setEnabled(false);
        forwardButton.addListener(SWT.Selection, e -> navigateForward());

        objectLabel = new Label(topPanel, SWT.NONE);
        objectLabel.setText("Inspecting instance: ...");
        objectLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        breadcrumbComposite = new Composite(topPanel, SWT.NONE);
        breadcrumbComposite.setLayout(new GridLayout(10, false));
        breadcrumbComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        table = new Table(rightPanel, SWT.BORDER | SWT.FULL_SELECTION);
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        TableColumn leftCol = new TableColumn(table, SWT.NONE);
        leftCol.setText("Type / Key");
        leftCol.setWidth(300);

        TableColumn rightCol = new TableColumn(table, SWT.NONE);
        rightCol.setText("Value");
        rightCol.setWidth(500);
    }

    /** Opens the shell */
    protected void open() { shell.open(); }

    /** Checks if shell is open */
    protected boolean isOpen() { return !shell.isDisposed(); }

    /** Closes window */
    protected void close() {
        if (isOpen()) {
            Display.getDefault().asyncExec(() -> { if (!shell.isDisposed()) shell.close(); });
        }
    }

    /** Shows top-level element structure */
    protected void showElementStructure(TopLevelElementRepresentationDTO element) {
        showAnchorElement(element);
    }

    /** Shows element with recursive inner elements */
    protected void showAnchorElement(TopLevelElementRepresentationDTO element) {
        if (element == null || shell.isDisposed()) return;

        Display.getDefault().asyncExec(() -> {
            elementTree.removeAll();
            TreeItem root = new TreeItem(elementTree, SWT.NONE);
            root.setText(new String[]{element.getElementName(), element.getElementType().name()});
            root.setExpanded(true);
            addInnerElementsRecursively(root, element.getInnerElements());
            elementTree.layout();
        });
    }

    /** Recursive display of inner elements */
    private void addInnerElementsRecursively(TreeItem parentItem, Set<InnerElementRepresentationDTO> innerElements) {
        if (innerElements == null || innerElements.isEmpty()) return;
        for (InnerElementRepresentationDTO inner : innerElements) {
            TreeItem item = new TreeItem(parentItem, SWT.NONE);
            item.setText(new String[]{inner.getElementName(), inner.getElementType().name()});
            item.setExpanded(true);
            addInnerElementsRecursively(item, Collections.EMPTY_SET); // no deep recursion for simplicity
        }
    }

    /** Show object in right panel and push to history */
    protected void showInspectableNode(InnerElementRepresentationDTO element) {
        if (element == null || shell.isDisposed()) return;

        if (!backHistory.isEmpty() && backHistory.peek() != element) {
            forwardHistory.clear(); // чистим forward стек при новом элементе
        }

        backHistory.push(element);

        Display.getDefault().asyncExec(() -> {
            objectLabel.setText("Inspecting instance: " + element.getElementName() + " (" + element.getElementType().name() + ")");
            refreshContent(element);
            renderBreadcrumb();
            updateNavigationButtons();
        });
    }

    /** Refresh right table */
    private void refreshContent(InnerElementRepresentationDTO element) {
        if (element == null || table.isDisposed()) return;

        table.removeAll();
        TableItem item = new TableItem(table, SWT.NONE);
        item.setText(new String[]{
                element.getElementType().name(),
                element.getValue() != null ? element.getValue() : ""
        });

        for (TableColumn col : table.getColumns()) col.pack();
        table.layout();
    }

    /** Navigate back */
    private void navigateBack() {
        if (backHistory.size() <= 1) return;

        InnerElementRepresentationDTO current = backHistory.pop();
        forwardHistory.push(current);

        InnerElementRepresentationDTO previous = backHistory.peek();
        if (previous != null) {
            Display.getDefault().asyncExec(() -> {
                objectLabel.setText("Inspecting instance: " + previous.getElementName() + " (" + previous.getElementType().name() + ")");
                refreshContent(previous);
                renderBreadcrumb();
            });
        }
        updateNavigationButtons();
    }

    /** Navigate forward */
    private void navigateForward() {
        if (forwardHistory.isEmpty()) return;

        InnerElementRepresentationDTO next = forwardHistory.pop();
        backHistory.push(next);

        Display.getDefault().asyncExec(() -> {
            objectLabel.setText("Inspecting instance: " + next.getElementName() + " (" + next.getElementType().name() + ")");
            refreshContent(next);
            renderBreadcrumb();
        });
        updateNavigationButtons();
    }

    /** Update back/forward buttons */
    private void updateNavigationButtons() {
        backButton.setEnabled(backHistory.size() > 1);
        forwardButton.setEnabled(!forwardHistory.isEmpty());
    }

    /** Render breadcrumb */
    private void renderBreadcrumb() {
        for (var child : breadcrumbComposite.getChildren()) child.dispose();

        Iterator<InnerElementRepresentationDTO> it = backHistory.descendingIterator();
        while (it.hasNext()) {
            InnerElementRepresentationDTO element = it.next();
            Button crumb = new Button(breadcrumbComposite, SWT.PUSH);
            crumb.setText(element.getElementName());
            crumb.addListener(SWT.Selection, e -> showInspectableNode(element));
        }
        breadcrumbComposite.layout();
    }

    /** Handle debug events */
    @SuppressWarnings("unchecked")
    public void handleDebugEvent(AbstractDebugEvent event) {
        if (Objects.equals(event.getType(), SimpleDebuggerEventType.SHOW_ANCHOR_ELEMENT)) {
            DebugEvent<TopLevelElementRepresentationDTO> simpleDebugEvent =
                    (DebugEvent<TopLevelElementRepresentationDTO>) event;
            showAnchorElement(simpleDebugEvent.getPayload());
        }
    }

    /** Return root control */
    public Composite getControl() { return shell; }
}