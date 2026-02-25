package com.gmail.aydinov.sergey.simple_debugger_plugin.ui;

import java.util.ArrayDeque;
import java.util.Deque;
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

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.TargetApplicationClassOrInterfaceRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.TargetApplicationInnerElementRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.DebuggerContext;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.DebuggerContext.SimpleDebuggerStatus;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.InnerElementDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebuggerEventTypes.SimpleDebuggerEventType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.SimpleDebuggerEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.UiEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.debug_event.AbstractDebugEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.debug_event.DebugEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.UIEvent;

/**
 * Inspect window with left Tree (element structure) and right Table (object values)
 */
public class InspectWindow {

    private final Shell shell;
    private final Tree elementTree;          
    private final Table table;               
    private final Label objectLabel;
    private final Button backButton;
    private final Button forwardButton;
    private final Composite breadcrumbComposite;
    private final Deque<InnerElementDTO> history = new ArrayDeque<>();
    private final UiEventCollector uiEventCollector = SimpleDebuggerEventCollector.instance();

    protected InspectWindow() {
        shell = new Shell(Display.getDefault());
        shell.setText("Inspect Object");
        shell.setSize(1400, 800);
        shell.setLayout(new GridLayout(2, true));

        // --- Иконка ---
        shell.setImage(DebugWindowsManager.instance().icons.get("debugger"));

        // --- Обработчик закрытия окна ---
        shell.addListener(SWT.Close, e -> {
            e.doit = true; // блокируем закрытие
            uiEventCollector.collectUiEvent(new UIEvent<Void>(SimpleDebuggerEventType.USER_ENDED_INSPECTION_SESSION_FOR_ELEMENT, null));
            DebuggerContext.context().setStatus(SimpleDebuggerStatus.INSPECTION_SEANCE_CLOSING);
        });

        // --- Левая панель: Tree ---
        elementTree = new Tree(shell, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        elementTree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        TreeColumn nameCol = new TreeColumn(elementTree, SWT.NONE);
        nameCol.setText("Name / Type");
        nameCol.setWidth(300);

        TreeColumn valueCol = new TreeColumn(elementTree, SWT.NONE);
        valueCol.setText("Value / Signature");
        valueCol.setWidth(200);

        // --- Правая панель: Table ---
        Composite rightPanel = new Composite(shell, SWT.NONE);
        rightPanel.setLayout(new GridLayout(1, false));
        rightPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        // Навигация
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
    protected void open() {
        shell.open();
    }

    /** Returns true if shell is open */
    protected boolean isOpen() {
        return !shell.isDisposed();
    }

    /** Closes the window programmatically */
    protected void close() {
        if (isOpen()) {
            Display.getDefault().asyncExec(() -> {
                if (!shell.isDisposed()) shell.close();
            });
        }
    }
T
    /** Показывает структуру top-level элемента */
    protected void showElementStructure(TargetApplicationClassOrInterfaceRepresentation element) {
        showAnchorElement(element);
    }

    /** Отображает элемент с рекурсивным деревом вложенных элементов */
    protected void showAnchorElement(TargetApplicationClassOrInterfaceRepresentation element) {
        if (element == null || shell.isDisposed()) return;

        Display.getDefault().asyncExec(() -> {
            elementTree.removeAll();

            TreeItem root = new TreeItem(elementTree, SWT.NONE);
            root.setText(new String[]{element.getElementName(), element.getElementType().name()});
            root.setExpanded(true);

            // Рекурсивно добавляем все inner элементы
            addInnerElementsRecursively(root, element.getInnerElements());

            elementTree.layout();
        });
    }

    /** Рекурсивный метод для отображения inner элементов */
    private void addInnerElementsRecursively(TreeItem parentItem,
                                             Set<TargetApplicationInnerElementRepresentation> innerElements) {
        if (innerElements == null || innerElements.isEmpty()) return;

        for (TargetApplicationInnerElementRepresentation inner : innerElements) {
            TreeItem item = new TreeItem(parentItem, SWT.NONE);
            item.setText(new String[]{inner.getElementName(), inner.getElementType().name()});
            item.setExpanded(true);

            addInnerElementsRecursively(item, inner.getInnerElements());
        }
    }

    /** Отображает объект в правой панели */
    protected void showInspectableNode(InnerElementDTO fieldOrVariableDTO) {
        if (fieldOrVariableDTO == null || shell.isDisposed()) return;

        history.push(fieldOrVariableDTO);

        Display.getDefault().asyncExec(() -> {
            objectLabel.setText("Inspecting instance: " + fieldOrVariableDTO.getName() + " (" + fieldOrVariableDTO.getType() + ")");
            refreshContent(fieldOrVariableDTO);
            renderBreadcrumb();
        });
    }

    private void refreshContent(InnerElementDTO dto) {
        if (dto == null || table.isDisposed()) return;

        table.removeAll();
        TableItem item = new TableItem(table, SWT.NONE);
        item.setText(new String[]{
                dto.getType() != null ? dto.getType() : "",
                dto.getValue() != null ? dto.getValue() : ""
        });

        for (TableColumn col : table.getColumns()) col.pack();
        table.layout();
    }

    private void navigateBack() {
        if (history.size() <= 1) return;
        InnerElementDTO current = history.pop();
        history.push(current);

        InnerElementDTO previous = history.peek();
        if (previous != null) {
            Display.getDefault().asyncExec(() -> {
                objectLabel.setText("Inspecting instance: " + previous.getName() + " (" + previous.getType() + ")");
                refreshContent(previous);
                renderBreadcrumb();
            });
        }

        updateNavigationButtons();
    }

    private void navigateForward() {
        if (history.isEmpty()) return;
        InnerElementDTO next = history.pop();
        history.push(next);

        Display.getDefault().asyncExec(() -> {
            objectLabel.setText("Inspecting instance: " + next.getName() + " (" + next.getType() + ")");
            refreshContent(next);
            renderBreadcrumb();
        });

        updateNavigationButtons();
    }

    private void updateNavigationButtons() {
        backButton.setEnabled(history.size() > 1);
        forwardButton.setEnabled(!history.isEmpty());
    }

    /** Рендер хлебных крошек */
    private void renderBreadcrumb() {
        // TODO: добавить кнопки для каждого элемента истории
    }

    /** Обработка debug-событий */
    @SuppressWarnings("unchecked")
    public void handleDebugEvent(AbstractDebugEvent event) {
        if (Objects.equals(event.getType(), SimpleDebuggerEventType.SHOW_ANCHOR_ELEMENT)) {
            DebugEvent<TargetApplicationClassOrInterfaceRepresentation> simpleDebugEvent =
                    (DebugEvent<TargetApplicationClassOrInterfaceRepresentation>) event;
            showAnchorElement(simpleDebugEvent.getPayload());
        }
    }
}