package com.gmail.aydinov.sergey.simple_debugger_plugin.ui.window;

import java.util.Objects;

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

import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.CollectionPageDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.DebugWindowDataDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebuggerEventTypes.SimpleDebuggerEventType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.SimpleDebuggerEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.UiEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.debug_event.AbstractDebugEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.debug_event.DebugEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.UIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.tab.ArrayInspectorTab;

public class UniversalInspectorWindow {

    private static UniversalInspectorWindow INSTANCE;

    private final UiEventCollector uiEventCollector = SimpleDebuggerEventCollector.instance();

    private final Shell shell;
    private final List navigationList;
    private final CTabFolder tabFolder;

    // вкладка коллекции
    private final ArrayInspectorTab arrayInspectorTab;
    private final CTabItem arrayTabItem;

    private UniversalInspectorWindow() {

        Display display = Display.getDefault();

        shell = new Shell(display);
        shell.setText("Universal Object Inspector");
        shell.setSize(900, 700);
        shell.setLayout(new FillLayout());
        shell.setImage(SimpleDebugerWindowsManager.instance().icons.get("debugger").getFirst());

        SashForm sash = new SashForm(shell, SWT.HORIZONTAL);

        // ===============================
        // Левая панель — навигация
        // ===============================

        Composite leftPanelComposite = new Composite(sash, SWT.BORDER);
        leftPanelComposite.setLayout(new GridLayout(1, false));
        leftPanelComposite.setLayoutData(new GridData(150, SWT.FILL, false, true));

        navigationList = new List(leftPanelComposite, SWT.BORDER | SWT.V_SCROLL);
        navigationList.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        // ===============================
        // Правая панель — вкладки
        // ===============================

        Composite rightPanelComposite = new Composite(sash, SWT.BORDER);
        rightPanelComposite.setLayout(new FillLayout());

        tabFolder = new CTabFolder(rightPanelComposite, SWT.BORDER);

        // вкладка коллекции
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

    // =========================================================
    // управление окном
    // =========================================================

    public void open() {

        if (!shell.isDisposed()) {
            shell.forceActive();
        }
    }

    public void close() {

        if (!shell.isDisposed()) {

            Display.getDefault().asyncExec(() -> {

                if (!shell.isDisposed()) {
                    shell.close();
                }

                INSTANCE = null;

                uiEventCollector.collectUiEvent(
                        new UIEvent<>(
                                SimpleDebuggerEventType.USER_CLOSED_INSPECTION_SEANCE_FOR_COLLECTION,
                                null
                        )
                );
            });
        }
    }

    // =========================================================
    // отображение коллекции
    // =========================================================

    private void showInspectableElement(CollectionPageDTO pageDTO) {

        Display.getDefault().asyncExec(() -> {

            if (shell.isDisposed()) {
                return;
            }

            // Добавляем в навигацию только первый показ коллекции
            if (pageDTO.getCurrentPage() == 0) {

                String displayText = pageDTO.getCollectionName();

                navigationList.add(displayText);
                navigationList.setTopIndex(navigationList.getItemCount() - 1);
            }

            // показываем страницу в вкладке
            arrayInspectorTab.showPage(pageDTO);

            tabFolder.setSelection(arrayTabItem);
            tabFolder.layout(true, true);
        });
    }

    // =========================================================
    // добавление новых вкладок
    // =========================================================

    private CTabItem addNewTab(String title, Composite content) {

        CTabItem item = new CTabItem(tabFolder, SWT.NONE);

        item.setText(title);
        item.setControl(content);

        tabFolder.layout(true, true);
        tabFolder.setSelection(item);

        return item;
    }

	@SuppressWarnings("unchecked")
	public void handleDebugEvent(AbstractDebugEvent event) {
		Display.getDefault().asyncExec(() -> {
		if (Objects.equals(event.getType(), SimpleDebuggerEventType.DISPLAY_PAGE_OF_INSPECTABLE_COLLECTION)) {
			DebugEvent<CollectionPageDTO> simpleDebugEvent = (DebugEvent<CollectionPageDTO>) event;
			showInspectableElement(simpleDebugEvent.getPayload());
		}
	});
}
}