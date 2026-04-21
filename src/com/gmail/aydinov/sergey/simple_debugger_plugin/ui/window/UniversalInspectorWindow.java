package com.gmail.aydinov.sergey.simple_debugger_plugin.ui.window;

import java.util.Objects;
import java.util.Queue;

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

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.AbstractElementRepresentation.Tag;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.DebuggerContext;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.DebuggerContext.SimpleDebuggerStatus;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.details.UserInstanceDetailsDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.inspection.AbstractInspectionDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.inspection.ArrayPageDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.inspection.BreadcrumbItemDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.inspection.MapPageDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.inspection.UserObjectInspectionDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.inspection.UserObjectPageDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.SimpleDebuggerEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.UiEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.debug_event.AbstractDebugEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.debug_event.DebugEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.AbstractUIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.UIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.tab.inspect_window_tab.IterableInspectorTab;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.tab.inspect_window_tab.InspectorTab;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.tab.inspect_window_tab.MapInspectorTab;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.tab.inspect_window_tab.UserObjectStructureTab;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebuggerEventTypes.SimpleDebuggerEventType;

public class UniversalInspectorWindow {

	private static UniversalInspectorWindow INSTANCE;

	private final UiEventCollector uiEventCollector = SimpleDebuggerEventCollector.instance();

	private final Shell shell;
	private final List navigationList;
	private final CTabFolder tabFolder;

	// вкладки
	private InspectorTab iterableInspectorTab;
	private CTabItem collectionTabItem;

	private InspectorTab mapInspectorTab;
	private CTabItem mapTabItem;


	private UserObjectStructureTab userObjectTab;
	private CTabItem userObjectTabItem;

	private enum InspectionTabs {
		NONE, COLLECTION, MAP, USER_OBJECT
	}

	private static InspectionTabs currentTab = InspectionTabs.NONE;

	private UniversalInspectorWindow() {
		Display display = Display.getDefault();

		shell = new Shell(display);
		shell.setText("Universal Object Inspector");
		shell.setSize(900, 700);
		shell.setLayout(new FillLayout());

		SashForm sash = new SashForm(shell, SWT.HORIZONTAL);

		// левая панель навигации
		Composite leftPanel = new Composite(sash, SWT.BORDER);
		leftPanel.setLayout(new GridLayout(1, false));
		leftPanel.setLayoutData(new GridData(150, SWT.FILL, false, true));
		navigationList = new List(leftPanel, SWT.BORDER | SWT.V_SCROLL);
		navigationList.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		// правая панель вкладок
		Composite rightPanel = new Composite(sash, SWT.BORDER);
		rightPanel.setLayout(new FillLayout());
		tabFolder = new CTabFolder(rightPanel, SWT.BORDER);

		sash.setWeights(new int[] { 20, 80 });

		shell.addListener(SWT.Close, e -> {
		    e.doit = false; 
		    close();        
		});
		shell.open();
		display.asyncExec(() -> {
		//	iterableInspectorTab = new IterableInspectorTab(rightPanel);
			if (iterableInspectorTab == null || collectionTabItem == null) {
				iterableInspectorTab = new IterableInspectorTab(tabFolder);
				collectionTabItem = new CTabItem(tabFolder, SWT.NONE);
				collectionTabItem.setText("Iterable");
				collectionTabItem.setControl(iterableInspectorTab.getControl());
			}
		});
	}

	public static UniversalInspectorWindow getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new UniversalInspectorWindow();
		}
		return INSTANCE;
	}

	// =========================================================
	// управление вкладками
	// =========================================================

	private void hideAllTabs() {
		if (iterableInspectorTab != null && !iterableInspectorTab.getControl().isDisposed())
			iterableInspectorTab.getControl().setVisible(false);
		if (mapInspectorTab != null && !mapInspectorTab.getControl().isDisposed())
			mapInspectorTab.getControl().setVisible(false);
//		if (userObjectTab != null && !userObjectTab.getControl().isDisposed())
//			userObjectTab.getControl().setVisible(false);
	}
	
	private void disposeAllTabs() {
	    for (CTabItem item : tabFolder.getItems()) {
	        if (!item.isDisposed()) {
	            item.dispose();
	        }
	    }
	    // обнуляем ссылки (важно!)
	    iterableInspectorTab = null;
	    collectionTabItem = null;

	    mapInspectorTab = null;
	    mapTabItem = null;

	    userObjectTab = null;
	    userObjectTabItem = null;
	}

	private void showTab(CTabItem tabItem, Composite content) {
		hideAllTabs();
		content.setVisible(true);
		tabFolder.setSelection(tabItem);
		tabFolder.layout(true, true);
	}

	private InspectorTab createIterableTabIfNeeded() {
		if (iterableInspectorTab == null || collectionTabItem == null) {
			iterableInspectorTab = new IterableInspectorTab(tabFolder);
			collectionTabItem = new CTabItem(tabFolder, SWT.NONE);
			collectionTabItem.setText("Iterable");
			collectionTabItem.setControl(iterableInspectorTab.getControl());
		}
		return iterableInspectorTab;
	}

	private void createMapTabIfNeeded() {
		if (mapInspectorTab == null || mapTabItem == null) {
			mapInspectorTab = new MapInspectorTab(tabFolder);
			mapTabItem = new CTabItem(tabFolder, SWT.NONE);
			mapTabItem.setText("Map");
			mapTabItem.setControl(mapInspectorTab.getControl());
		}
	}

//	private void createUserObjectTabIfNeeded(String title) {
//		if (userObjectTab == null || userObjectTabItem == null) {
//			userObjectTab = new UserObjectStructureTab(tabFolder);
//			userObjectTabItem = new CTabItem(tabFolder, SWT.NONE);
//			userObjectTabItem.setText(title);
//			userObjectTabItem.setControl(userObjectTab.getControl());
//		}
//	}

	public void showIterableTab(ArrayPageDTO payload) {
		if (tabFolder.isDisposed())
			return;
		Display.getDefault().asyncExec(() -> {
			 disposeAllTabs();
			createIterableTabIfNeeded();
			iterableInspectorTab.showPage(payload);
			showBreadcrumbs(payload.getBreadcrumbs());
			showTab(collectionTabItem, iterableInspectorTab.getControl());
			currentTab = InspectionTabs.COLLECTION;
		});
	}

	public void showMapTab(MapPageDTO<InnerElementRepresentationDTO, InnerElementRepresentationDTO> page) {
		if (tabFolder.isDisposed())
			return;

		Display.getDefault().asyncExec(() -> {
			 disposeAllTabs();
			createMapTabIfNeeded();
			mapInspectorTab.showPage(page);
			showBreadcrumbs(page.getBreadcrumbs());
			showTab(mapTabItem, mapInspectorTab.getControl());
			currentTab = InspectionTabs.MAP;
		});
	}

	public void showUserObjectTab(UserObjectPageDTO userObjectPageDTO) {
		if (tabFolder.isDisposed())
			return;

		Display.getDefault().asyncExec(() -> {
			disposeAllTabs();
			createUserObjectTabIfNeeded("USER OBJ.");
			userObjectTab.showUserObject(userObjectPageDTO);
			showBreadcrumbs(userObjectPageDTO.getBreadcrumbs());
			showTab(userObjectTabItem, userObjectTab.getControl());
			currentTab = InspectionTabs.USER_OBJECT;
		});
	}
	
	 private void createUserObjectTabIfNeeded(String title) {
	        if (userObjectTab == null || userObjectTabItem == null) {
	            userObjectTab = new UserObjectStructureTab(tabFolder);
	            userObjectTabItem = new CTabItem(tabFolder, SWT.NONE);
	            userObjectTabItem.setText(title);
	            userObjectTabItem.setControl(userObjectTab.getControl());
	        }
	    }

	@SuppressWarnings({ "unchecked", "static-access" })
	public void handleDebugEvent(AbstractDebugEvent event) {
		switch (event.getType()) {
		case DISPLAY_PAGE_OF_INSPECTABLE_ITERABLE -> {
			DebugEvent<ArrayPageDTO> e = (DebugEvent<ArrayPageDTO>) event;
			showIterableTab(e.getPayload());
		}
		case DISPLAY_PAGE_OF_INSPECTABLE_MAP -> {
			DebugEvent<MapPageDTO<InnerElementRepresentationDTO, InnerElementRepresentationDTO>> e = (DebugEvent<MapPageDTO<InnerElementRepresentationDTO, InnerElementRepresentationDTO>>) event;
			showMapTab(e.getPayload());
		}
		case DISPLAY_PAGE_OF_INSPECTABLE_USER_OBJECT -> {
        	DebugEvent<UserObjectPageDTO> e = (DebugEvent<UserObjectPageDTO>) event;
        	showUserObjectTab(e.getPayload());
           // newAnchorTag = e.getPayload().getTag();
        }
		case DISPLAY_ADDITIONAL_INFO -> {
			DebugEvent<UserInstanceDetailsDTO> e = (DebugEvent<UserInstanceDetailsDTO>) event;
			if (currentTab == InspectionTabs.COLLECTION) {
			//createIterableTabIfNeeded();
		//	iterableInspectorTab.showFieldInfoPopupFromBackend(e.getPayload());
				iterableInspectorTab.showFieldInfoPopupFromBackend(e.getPayload());
			}
			
//			} else if (currentTab == InspectionTabs.MAP) {
//				createMapTabIfNeeded();
//				mapInspectorTab.showFieldInfoPopupFromBackend(e.getPayload());
//			}

		}
		default -> {
		}
		}
	}

	// =========================================================
	// управление окном
	// =========================================================

	public void open() {
		if (!shell.isDisposed())
			shell.forceActive();
	}

	public void close() {
			Display.getDefault().asyncExec(() -> {
				if (!shell.isDisposed())
					shell.dispose();
				INSTANCE = null;
				DebuggerContext.context().setStatus(SimpleDebuggerStatus.DEBUG_SESSION_RUNNING);
				uiEventCollector
						.collectUiEvent(new UIEvent<>(SimpleDebuggerEventType.USER_CLOSED_INSPECTION_SEANCE, null));
			});
		
	}

	public Shell getShell() {
		return shell;
	}

	public void populateNavigationListFromManagerQueue() {
		// Queue<Tag> tagQueue = SimpleDebugerWindowsManager.instance().tagQueue();
		if (navigationList.isDisposed())
			return;

		Display.getDefault().asyncExec(() -> {
			navigationList.removeAll(); // очищаем предыдущие элементы

			int index = 0;
//            for (Tag tag : tagQueue) {
//                String itemText = "element[" + index + "]: " + tag.toString();
//                navigationList.add(itemText);
//                index++;
//            }
		});
	}

	private void showBreadcrumbs(java.util.List<BreadcrumbItemDTO> breadcrumbs) {
		if (navigationList.isDisposed())
			return;

		Display.getDefault().asyncExec(() -> {
			navigationList.removeAll();

			for (int i = 0; i < breadcrumbs.size(); i++) {
				BreadcrumbItemDTO item = breadcrumbs.get(i);

				String prefix = (i == breadcrumbs.size() - 1) ? "➤ " : "  ";
				String text = prefix + item.getDisplayName();

				navigationList.add(text);
			}
		});
	}
}