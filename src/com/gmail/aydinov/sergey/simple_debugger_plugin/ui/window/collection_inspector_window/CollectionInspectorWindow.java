package com.gmail.aydinov.sergey.simple_debugger_plugin.ui.window.collection_inspector_window;

import java.util.List;
import java.util.Objects;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.UniversalElementType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.ValueCategory;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.CollectionEntryDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebuggerEventTypes.SimpleDebuggerEventType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.SimpleDebuggerEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.UiEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.debug_event.AbstractDebugEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.debug_event.DebugEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.UIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.SimpleDebugerWindowsManager;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.window.collection_inspector_window.tab.ArrayInspectorTab;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.window.collection_inspector_window.tab.InspectorTab;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.window.collection_inspector_window.tab.MapInspectorTab;
import com.gmail.aydinov.sergey.simple_debugger_plugin.utils.DebugUtils;

public class CollectionInspectorWindow implements ManageableCollectionInspectorWindow {

	private static CollectionInspectorWindow INSTANCE = null;
	
	private final Shell shell;
	private final InspectorTab inspectorTab;
	private final UiEventCollector uiEventCollector = SimpleDebuggerEventCollector.instance();

	private final Button backButton;
	private final Button forwardButton;
	private final Text pageText;
	private final Label totalPagesLabel;

	// Лейблы для коллекции
	private final Label fieldNameLabel;
	private final Label collectionTypeLabel;
	private final Label sizeLabel;

	private int currentPage = 1;
	private int totalPages = 1; 
	private List<CollectionEntryDTO> allElements;

	private CollectionInspectorWindow(InnerElementRepresentationDTO innerElementRepresentationDTO) {

		Display display = Display.getDefault();
		shell = new Shell(display, SWT.SHELL_TRIM | SWT.APPLICATION_MODAL);
		shell.setText("Collection Inspector");
		shell.setSize(700, 768);
		shell.setImage(SimpleDebugerWindowsManager.instance().icons.get("debugger").getFirst());
		shell.setLayout(new GridLayout(1, false));

		// ===================
		// Навигация (верхний ряд)
		Composite navPanel = new Composite(shell, SWT.NONE);
		navPanel.setLayout(new GridLayout(4, false)); // Back, Forward, Page input, Total pages
		navPanel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		backButton = new Button(navPanel, SWT.PUSH);
		backButton.setText("◀ Back");
		backButton.setEnabled(false);
		backButton.addListener(SWT.Selection, e -> goBack());

		forwardButton = new Button(navPanel, SWT.PUSH);
		forwardButton.setText("Forward ▶");
		forwardButton.setEnabled(false);
		forwardButton.addListener(SWT.Selection, e -> goForward());

		pageText = new Text(navPanel, SWT.BORDER | SWT.CENTER);
		pageText.setTextLimit(5);
		pageText.setLayoutData(new GridData(50, SWT.DEFAULT));
		pageText.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.character == SWT.CR)
					jumpToPageFromText();
			}
		});
		pageText.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				jumpToPageFromText();
			}
		});

		totalPagesLabel = new Label(navPanel, SWT.NONE);
		totalPagesLabel.setText("/ 1");

		// ===================
		// Панель с информацией о коллекции (нижний ряд)
		Composite infoPanel = new Composite(shell, SWT.NONE);
		infoPanel.setLayout(new GridLayout(1, false)); // одна колонка — строки одна под другой
		infoPanel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		fieldNameLabel = new Label(infoPanel, SWT.NONE);
		fieldNameLabel.setText("Field Name: -");
		fieldNameLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		collectionTypeLabel = new Label(infoPanel, SWT.NONE);
		collectionTypeLabel.setText("Collection Type: -");
		collectionTypeLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		sizeLabel = new Label(infoPanel, SWT.NONE);
		sizeLabel.setText("Size: 0");
		sizeLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		// ===================
		// Таблица коллекции
		if(innerElementRepresentationDTO.getValueCategory().equals(ValueCategory.MAP)) {
		inspectorTab = new MapInspectorTab(shell);
		inspectorTab.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		} else if (innerElementRepresentationDTO.getValueCategory().equals(ValueCategory.ARRAY) ||
				innerElementRepresentationDTO.getValueCategory().equals(ValueCategory.COLLECTION)) {
			inspectorTab = new ArrayInspectorTab(shell);
			inspectorTab.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		} else {
			throw new IllegalStateException();
		}
		shell.addListener(SWT.Close, e -> {
		//    e.doit = false; // отменить стандартное закрытие
		    close();        // вызвать свой метод
		});
		shell.open();
	}


//	protected static CollectionInspectorWindow getOrCreateCollectionInspectWindow() {
//		Display.getDefault().syncExec(() -> {
//			if (Objects.isNull(INSTANCE))
//				INSTANCE = new CollectionInspectorWindow();
//			INSTANCE.open();
//		});
//		return INSTANCE;
//	}

	/** Навигация вперед */
	private void goForward() {
		if (currentPage < totalPages) {
			currentPage++;
			updatePage();
		}
	}

	/** Навигация назад */
	private void goBack() {
		if (currentPage > 1) {
			currentPage--;
			updatePage();
		}
	}

	/** Ввод страницы вручную */
	private void jumpToPageFromText() {
		try {
			int page = Integer.parseInt(pageText.getText().trim());
			if (page < 1)
				page = 1;
			if (page > totalPages)
				page = totalPages;
			currentPage = page;
			updatePage();
		} catch (NumberFormatException ignored) {
			pageText.setText(String.valueOf(currentPage));
		}
	}

	/** Обновление текущей страницы и информации */
	private void updatePage() {
		if (allElements == null || allElements.isEmpty()) return;
		int start = (currentPage - 1) * DebugUtils.PAGE_SIZE;
		int end = Math.min(start + DebugUtils.PAGE_SIZE, allElements.size());
		List<CollectionEntryDTO> pageElements = allElements.subList(start, end);
		inspectorTab.showCollection(pageElements);

		pageText.setText(String.valueOf(currentPage));
		totalPagesLabel.setText("/ " + totalPages);
		backButton.setEnabled(currentPage > 1);
		forwardButton.setEnabled(currentPage < totalPages);

		// Обновление лейблов с информацией о коллекции
		if (!allElements.isEmpty()) {
			CollectionEntryDTO first = pageElements.get(0); // берем первый элемент текущей страницы
			fieldNameLabel.setText("Field Name: " + first.getCollectionName());
			collectionTypeLabel.setText("Collection Type: " + first.getCollectionName());
			sizeLabel.setText("Size: " + first.getSize());
		} else {
			fieldNameLabel.setText("Field Name: -");
			collectionTypeLabel.setText("Collection Type: -");
			sizeLabel.setText("Size: 0");
		}
	}

	@Override
	public void open() {
		if (shell != null && !shell.isDisposed()) {
			Display.getDefault().asyncExec(() -> {
				 shell.open();
			});
		}
	}

	public boolean isOpen() {
		return !shell.isDisposed();
	}

	@Override
	public void close() {
	    if (shell != null && !shell.isDisposed()) {
	        Display.getDefault().asyncExec(() -> {
	            if (!shell.isDisposed()) {
	                shell.close();
	            }
	            INSTANCE = null;
	            SimpleDebugerWindowsManager.instance()
	                .setManageableCollectionInspectorWindow(null);
	            uiEventCollector.collectUiEvent(
	                new UIEvent<>(
	                    SimpleDebuggerEventType.USER_CLOSED_INSPECTION_SEANCE_FOR_COLLECTION,
	                    null
	                )
	            );
	        });
	    }
	}

	@Override
	@SuppressWarnings("unchecked")
	public void handleDebugEvent(AbstractDebugEvent event) {
		if (event.getType().equals(SimpleDebuggerEventType.SET_COLLECTION_INSPECT_WINDOW_STATE)) {
			DebugEvent<Boolean> debDebugEvent = (DebugEvent<Boolean>) event;
			if (debDebugEvent.getPayload()) open(); else close();
			
		}
	}

	static ManageableCollectionInspectorWindow getOrCreateCollectionInspectWindowFor(
			InnerElementRepresentationDTO innerElementRepresentationDTO) {
		Display.getDefault().syncExec(() -> {
			if (Objects.isNull(INSTANCE)) {
				INSTANCE = new CollectionInspectorWindow(innerElementRepresentationDTO);
			INSTANCE.open();
			}
		});
		return INSTANCE;
	}
}