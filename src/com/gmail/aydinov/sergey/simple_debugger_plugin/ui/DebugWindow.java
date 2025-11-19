package com.gmail.aydinov.sergey.simple_debugger_plugin.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import com.gmail.aydinov.sergey.simple_debugger_plugin.core.DebugEventListener;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.DebugEventProvider;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.SimpleDebuggerWorkFlow;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.UiEventListener;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.UiEventProvider;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.DebugEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.UIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.UIEventResumeButtonPressed;
import com.sun.jdi.ThreadReference;

public class DebugWindow implements DebugEventListener, UiEventProvider {

	private Shell shell;
	private CTabFolder tabFolder;

	// Поля вкладок
	private VariablesTabContent variablesTab;
	private FieldsTabContent fieldsTab;
	private StackTabContent stackTab;
	private EvaluateTabContent evalTab;
	private Button resumeButton;
	private Label locationLabel;
	private ThreadReference suspendedThread;
	private DebugEventProvider debugEventProvider;
	private UiEventListener uiEventListener;
	private final String STOP_INFO = "Stopped at: ";

	public DebugWindow() {
		uiEventListener = SimpleDebuggerWorkFlow.Factory.getInstanceOfSimpleDebuggerWorkFlow();
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
		variablesTab = new VariablesTabContent(tabFolder);
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

		evalTab = new EvaluateTabContent(tabFolder);
		CTabItem evalItem = new CTabItem(tabFolder, SWT.NONE);
		evalItem.setText("Evaluate");
		evalItem.setControl(evalTab.getControl());

		tabFolder.setSelection(0);

		// ----------------- Hook кнопки Resume -----------------
		hookResumeButton();
	}

	private void hookResumeButton() {
		resumeButton.addListener(SWT.Selection, e -> {
		    pressResumeButton();
		});
		
	}

	public void setDebugEventProvider(DebugEventProvider debugEventProvider) {
		this.debugEventProvider = debugEventProvider;
	}

	private void pressResumeButton() {
		System.out.println("pressResumeButton()" + uiEventListener);
		Display.getDefault().asyncExec(() -> {
			sendUiEvent(new UIEventResumeButtonPressed());
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

	@Override
	public void handleDebugEvent(DebugEvent debugEvent) {
		Display.getDefault().asyncExec(() -> {
			try {
				if (shell.isDisposed())
					return;
				locationLabel.setText(STOP_INFO + debugEvent.getClassName() + "." + debugEvent.getMethodName()
						+ " line:" + debugEvent.getLineNumber());
				resumeButton.setEnabled(true); // ← включаем кнопку при остановке
				variablesTab.updateVariables(debugEvent.getLocalVariables());
				fieldsTab.updateFields(debugEvent.getFields());
				stackTab.updateStack(debugEvent.getStackDescription());
			} catch (Exception e) {
				e.printStackTrace();
			}
		});

	}

	@Override
	public void sendUiEvent(UIEvent uiEvent) {
		uiEventListener.handleUiEvent(uiEvent);

	}

}
