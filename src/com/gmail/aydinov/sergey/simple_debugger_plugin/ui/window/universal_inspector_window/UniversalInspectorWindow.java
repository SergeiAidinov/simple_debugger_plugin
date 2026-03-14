package com.gmail.aydinov.sergey.simple_debugger_plugin.ui.window.universal_inspector_window;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.ValueCategory;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.PairDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.CollectionEntryDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebuggerEventTypes.SimpleDebuggerEventType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.SimpleDebuggerEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.UiEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.UIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.SimpleDebugerWindowsManager;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.window.collection_inspector_window.tab.ArrayInspectorTab;

public class UniversalInspectorWindow {

    private static UniversalInspectorWindow INSTANCE;

    private final UiEventCollector uiEventCollector = SimpleDebuggerEventCollector.instance();
    private final Shell shell;
    private final List navigationList;
    private final CTabFolder tabFolder;

    // Вкладка коллекций
    private final ArrayInspectorTab arrayInspectorTab;
    private final CTabItem arrayTabItem;

    private UniversalInspectorWindow() {
        Display display = Display.getDefault();

        shell = new Shell(display);
        shell.setText("Universal Object Inspector");
        shell.setSize(900, 600);
        shell.setLayout(new FillLayout());
        shell.setImage(SimpleDebugerWindowsManager.instance().icons.get("debugger").getFirst());

        SashForm sash = new SashForm(shell, SWT.HORIZONTAL);

        // Левая панель — история переходов
        Composite leftPanelComposite = new Composite(sash, SWT.BORDER);
        leftPanelComposite.setLayout(new GridLayout(1, false));
        leftPanelComposite.setLayoutData(new GridData(150, SWT.FILL, false, true));
        navigationList = new List(leftPanelComposite, SWT.BORDER | SWT.V_SCROLL);
        navigationList.add("Navigation history will appear here");

        // Правая панель — для вкладок
        Composite rightPanelComposite = new Composite(sash, SWT.BORDER);
        rightPanelComposite.setLayout(new FillLayout());
        tabFolder = new CTabFolder(rightPanelComposite, SWT.BORDER);

        // Создаём сразу вкладку коллекций
        arrayInspectorTab = new ArrayInspectorTab(tabFolder);
        arrayTabItem = new CTabItem(tabFolder, SWT.NONE);
        arrayTabItem.setText("Collection");
        arrayTabItem.setControl(arrayInspectorTab.getControl());

        sash.setWeights(new int[]{20, 80});
        shell.addListener(SWT.Close, e -> close());
        shell.open();
    }

    public static UniversalInspectorWindow getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new UniversalInspectorWindow();
        }
        return INSTANCE;
    }

    /** Активация окна */
    public void open() {
        if (!shell.isDisposed()) {
            shell.forceActive();
        }
    }

    /** Закрытие окна */
    public void close() {
        if (!shell.isDisposed()) {
            Display.getDefault().asyncExec(() -> {
                if (!shell.isDisposed()) {
                    shell.close();
                }
                INSTANCE = null;
                uiEventCollector.collectUiEvent(
                        new UIEvent<>(SimpleDebuggerEventType.USER_CLOSED_INSPECTION_SEANCE_FOR_COLLECTION, null)
                );
            });
        }
    }

    /** Отображение элемента в окне */
    public void showInspectableElement(PairDTO<InnerElementRepresentationDTO, java.util.List<InnerElementRepresentationDTO>> anchorWithSibordinants) {
        // Только коллекции пока
        if (!anchorWithSibordinants.getFirst().getValueCategory().equals(ValueCategory.COLLECTION)) return;

        Display.getDefault().asyncExec(() -> {
            // Обновляем левую панель: добавляем элемент в список навигации
            InnerElementRepresentationDTO element = anchorWithSibordinants.getFirst();
            String displayText = element.getElementName() + " [" + element.getAdditionalInfo() + "]";
            navigationList.add(displayText);
            navigationList.setTopIndex(navigationList.getItemCount() - 1); // прокрутка вниз

            // Преобразуем данные для вкладки
            java.util.List<CollectionEntryDTO> entries = anchorWithSibordinants.getSecond().stream()
                    .map(inner -> new CollectionEntryDTO(inner.getElementName(), "default", 0))
                    .toList();

            // Обновляем содержимое существующей вкладки
            arrayInspectorTab.showArray(entries);
            tabFolder.layout(true, true);
            tabFolder.setSelection(arrayTabItem);
        });
    }

    /** Добавление новых вкладок для других типов данных */
    public CTabItem addNewTab(String title, Composite content) {
        CTabItem item = new CTabItem(tabFolder, SWT.NONE);
        item.setText(title);
        item.setControl(content);
        tabFolder.layout(true, true);
        tabFolder.setSelection(item);
        return item;
    }
}