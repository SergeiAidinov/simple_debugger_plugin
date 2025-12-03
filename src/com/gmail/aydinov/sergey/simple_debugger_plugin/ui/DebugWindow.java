package com.gmail.aydinov.sergey.simple_debugger_plugin.ui;

import java.util.Collections;
import java.util.Map;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.DebugEventProvider;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebugEventDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebugEventType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.UserClosedWindowUiEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.UserPressedResumeUiEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.processor.SimpleDebugEventProcessor;
import com.gmail.aydinov.sergey.simple_debugger_plugin.processor.SimpleDebuggerEventQueue;
import com.gmail.aydinov.sergey.simple_debugger_plugin.processor.UiEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.tab.EvaluateTabController;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.tab.FieldsTabContent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.tab.StackTabContent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.tab.VariablesTabContent;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Value;

public class DebugWindow {

	private Shell shell;
	private CTabFolder tabFolder;

	// Поля вкладок
	private VariablesTabContent variablesTab;
	private FieldsTabContent fieldsTab;
	private StackTabContent stackTab;
	private EvaluateTabController evalTab;
	private Button resumeButton;
	private Label locationLabel;
	private final UiEventCollector uiEventCollector = SimpleDebuggerEventQueue.instance();

	private final String STOP_INFO = "Stopped at: ";

	public DebugWindow() {
		Display display = Display.getDefault();
		shell = new Shell(display);
		shell.setText("Simple Debugger");
		shell.setSize(800, 600);
		shell.setLayout(new GridLayout(1, false));

		// ----------------- Панель сверху -----------------
		Composite topPanel = new Composite(shell, SWT.NONE);
		topPanel.setLayout(new GridLayout(3, false)); // 3 колонки
		GridData topPanelGD = new GridData(SWT.FILL, SWT.TOP, true, false);
		topPanel.setLayoutData(topPanelGD);

		// 1) Label слева
		locationLabel = new Label(topPanel, SWT.NONE);
		locationLabel.setText(STOP_INFO);
		GridData labelGD = new GridData(SWT.FILL, SWT.TOP, true, false); // вертикальное центрирование
		locationLabel.setLayoutData(labelGD);

		// 2) Spacer
		Composite spacer = new Composite(topPanel, SWT.NONE);
		spacer.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

		// 3) Кнопка справа
		resumeButton = new Button(topPanel, SWT.PUSH);
		resumeButton.setText("Resume");
		resumeButton.setEnabled(false);
		GridData buttonGD = new GridData(SWT.RIGHT, SWT.TOP, false, false); // вертикальное центрирование
		resumeButton.setLayoutData(buttonGD);

		// Устанавливаем высоту панели: на 10 пикселей выше кнопки
		int buttonHeight = resumeButton.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
		topPanelGD.heightHint = buttonHeight + 10;

		// ----------------- TAB folder -----------------
		tabFolder = new CTabFolder(shell, SWT.BORDER);
		tabFolder.setSimple(false);
		tabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		// Таб-страницы
		variablesTab = new VariablesTabContent(tabFolder, uiEventCollector);
		CTabItem varItem = new CTabItem(tabFolder, SWT.NONE);
		varItem.setText("Variables");
		varItem.setControl(variablesTab.getControl());

		fieldsTab = new FieldsTabContent(tabFolder);
		CTabItem fieldsItem = new CTabItem(tabFolder, SWT.NONE);
		fieldsItem.setText("Fields");
		fieldsItem.setControl(fieldsTab.getControl());

		stackTab = new StackTabContent(tabFolder);
		CTabItem stackItem = new CTabItem(tabFolder, SWT.NONE);
		stackItem.setText("Stack");
		stackItem.setControl(stackTab.getControl());

		evalTab = new EvaluateTabController(tabFolder, uiEventCollector);
		CTabItem evalItem = new CTabItem(tabFolder, SWT.NONE);
		evalItem.setText("Evaluate");
		evalItem.setControl(evalTab.getControl());

		tabFolder.setSelection(0);

		// ----------------- Hook кнопки Resume -----------------
		hookResumeButton();
		hookCross();

		SimpleDebugEventProcessor simpleDebugEventProcessor = new SimpleDebugEventProcessor(this);
		Thread thread = new Thread(simpleDebugEventProcessor);
		thread.setDaemon(true);
		thread.start();
	}

	private void hookCross() {
		shell.addListener(SWT.Close, event -> {
			event.doit = false; // запретить автоматическое закрытие
			handleWindowClose();
		});
	}

	private boolean handleWindowClose() {

		MessageBox messageBox = new MessageBox(shell, SWT.ICON_QUESTION | SWT.YES | SWT.NO);
		messageBox.setText("Confirmation");
		messageBox.setMessage("Close the debugger window?");

		int response = messageBox.open();
		if (response == SWT.NO) {
			return false; // отменяем закрытие
		}

		// закрываем
		System.out.println("Debug window closed");
		showVmStoppedMessage();
		// sendUiEvent(new UserClosedWindowUiEvent());
		shell.dispose();
		uiEventCollector.collectUiEvent(new UserClosedWindowUiEvent());
		return true; // разрешаем закрытие
	}

	private void hookResumeButton() {
		resumeButton.addListener(SWT.Selection, e -> {
			pressResumeButton();
		});

	}

	public void setDebugEventProvider(DebugEventProvider debugEventProvider) {
	}

	private void pressResumeButton() {
		System.out.println("pressed Resume Button ");
		Display.getDefault().asyncExec(() -> {
			uiEventCollector.collectUiEvent(new UserPressedResumeUiEvent());
		});
	}

	public void open() {
		shell.open();
	}

	public boolean isOpen() {
		return !shell.isDisposed();
	}

	public Shell getShell() {
		return shell;
	}

	public void handleDebugEvent(SimpleDebugEventDTO debugEvent) {
		Display.getDefault().asyncExec(() -> {
			try {
				if (shell.isDisposed())
					return;
				if (debugEvent.getType().equals(SimpleDebugEventType.REFRESH_DATA))
					refreshData(debugEvent);

			} catch (Exception e) {
				e.printStackTrace();
			}
		});

	}

	private void refreshData(SimpleDebugEventDTO simpleDebugEventDTO) {
	    // -----------------------------
	    // 0. Проверка наличия debugEvent
	    // -----------------------------
	    if (simpleDebugEventDTO == null) {
	        System.out.println("refreshData: debugEvent = null -> skip");
	        return;
	    }

	    // -----------------------------
	    // 1. Обновляем UI (безопасно)
	    // -----------------------------
	    locationLabel.setText(
	            STOP_INFO + simpleDebugEventDTO.getClassName() + "." +
	            simpleDebugEventDTO.getMethodName() + " line:" + simpleDebugEventDTO.getLineNumber()
	    );
	    resumeButton.setEnabled(true);

	    // -----------------------------
	    // 2. Обновляем вкладки
	    // -----------------------------
	    variablesTab.updateVariables(simpleDebugEventDTO.getLocals()); // List<VariableDTO>
	    fieldsTab.updateFields(simpleDebugEventDTO.getFields());               // List<VariableDTO>
	    stackTab.updateStack(simpleDebugEventDTO.getMethodCallInStacks());
	    evalTab.updateFromEvent(simpleDebugEventDTO);

	}

	private void showVmStoppedMessage() {
	    // Например:
	    MessageDialog.openInformation(shell, "Debugger", "Debugger detached. Target VM continues running");
	}


}