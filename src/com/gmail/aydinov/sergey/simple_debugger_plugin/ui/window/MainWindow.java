package com.gmail.aydinov.sergey.simple_debugger_plugin.ui.window;

import java.util.Objects;

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

import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.DebugWindowDataDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.details.UserInstanceDetailsDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebuggerEventTypes;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebuggerEventTypes.SimpleDebuggerEventType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.SimpleDebuggerEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.UiEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.debug_event.AbstractDebugEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.debug_event.DebugEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.UIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.tab.ClassMembersAtBreakpointTab;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.tab.ConsoleContentTab;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.tab.ContextTab;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.tab.StackTabContent;

/**
 * Main debugger window displaying combined Variables + Fields tab, stack trace,
 * evaluation, and console.
 * <p>
 * Author: Sergei Aidinov <br>
 * Email: <a href="mailto:sergey.aydinov@gmail.com">sergey.aydinov@gmail.com</a>
 * </p>
 */
public class MainWindow {

	private static MainWindow INSTANCE = null;

	private Shell shell;
	private CTabFolder tabFolder;

	// Combined Variables + Fields tab
	private ClassMembersAtBreakpointTab classMembersAtBreakpoint;
	private StackTabContent stackTabContent;
	private ConsoleContentTab consoleTabContent;
	private ContextTab contextTab;

	private Button resumeButton;
	private Label locationLabel;

	private final UiEventCollector uiEventCollector = SimpleDebuggerEventCollector.instance();
	private final String STOP_INFO = "Stopped at: ";

	/**
	 * Constructs and initializes the debugger window.
	 */
	private MainWindow() {
		Display display = Display.getDefault();
		shell = new Shell(display);
		shell.setText("Simple Debugger");
		shell.setSize(1024, 600);
		shell.setLayout(new GridLayout(1, false));

		// ----------------- Top panel -----------------
		Composite topPanel = new Composite(shell, SWT.NONE);
		GridLayout topLayout = new GridLayout(2, false); // 2 колонки: label + кнопка
		topLayout.marginWidth = 0;
		topLayout.marginHeight = 0;
		topPanel.setLayout(topLayout);
		topPanel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

		// Label слева — занимает всё доступное пространство
		locationLabel = new Label(topPanel, SWT.NONE);
		locationLabel.setText(STOP_INFO);
		locationLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		// Кнопка Resume справа — не растягивается
		resumeButton = new Button(topPanel, SWT.PUSH);
		resumeButton.setText("Resume");
		resumeButton.setEnabled(false);
		resumeButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

		// Опционально: зафиксировать высоту панели по высоте кнопки + небольшой отступ
		int buttonHeight = resumeButton.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
		GridData topPanelGridData = new GridData(SWT.FILL, SWT.TOP, true, false);
		topPanelGridData.heightHint = buttonHeight + 10;
		topPanel.setLayoutData(topPanelGridData);

		// ----------------- TAB folder -----------------
		tabFolder = new CTabFolder(shell, SWT.BORDER);
		tabFolder.setSimple(false);
		tabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		// Combined Variables + Fields tab
		classMembersAtBreakpoint = new ClassMembersAtBreakpointTab(tabFolder);
		CTabItem varsFieldsTabItem = new CTabItem(tabFolder, SWT.NONE);
		varsFieldsTabItem.setText("Class Members at Breakpoint");
		varsFieldsTabItem.setControl(classMembersAtBreakpoint.getControl());

		// Stack tab
		stackTabContent = new StackTabContent(tabFolder);
		CTabItem stackTabItem = new CTabItem(tabFolder, SWT.NONE);
		stackTabItem.setText("Stack");
		stackTabItem.setControl(stackTabContent.getControl());

		// Console tab
		consoleTabContent = new ConsoleContentTab(tabFolder);
		CTabItem consoleTabItem = new CTabItem(tabFolder, SWT.NONE);
		consoleTabItem.setText("Console");
		consoleTabItem.setControl(consoleTabContent.getControl());
		
		contextTab = new ContextTab(tabFolder);
		CTabItem contextTabItem = new CTabItem(tabFolder, SWT.NONE);
		contextTabItem.setText("Context");
		contextTabItem.setControl(contextTab.getControl());

		tabFolder.setSelection(0);

		// ----------------- Hook Resume button -----------------
		hookResumeButton();
		hookCross();

	}

	protected static MainWindow getOrCreateMainWindow() {
		Display.getDefault().syncExec(() -> {
			if (Objects.isNull(INSTANCE))
				INSTANCE = new MainWindow();
			INSTANCE.open();
		});
		return INSTANCE;
	}

	// ----------------- Event hooks -----------------
	private void hookCross() {
		shell.addListener(SWT.Close, event -> {
			event.doit = false;
			handleWindowClose();
		});
	}

	private boolean handleWindowClose() {
		MessageBox messageBox = new MessageBox(shell, SWT.ICON_QUESTION | SWT.YES | SWT.NO);
		messageBox.setText("Confirmation");
		messageBox.setMessage("Close the debugger window?");
		int response = messageBox.open();
		if (response == SWT.NO)
			return false;

		shell.dispose();
		uiEventCollector.collectUiEvent(new UIEvent<Void>(SimpleDebuggerEventType.USER_CLOSED_DEBUG_WINDOW, null));
		return true;
	}

	private void hookResumeButton() {
		resumeButton.addListener(SWT.Selection, e -> uiEventCollector
				.collectUiEvent(new UIEvent<Void>(SimpleDebuggerEventType.USER_PRESSED_RESUME_BUTTON, null)));
	}

	protected Shell getShell() {
		return shell;
	}

	/**
	 * Opens the debugger window and sets the window icon.
	 */
	public void open() {
		shell.setImage(SimpleDebugerWindowsManager.instance().icons.get("debugger").getFirst()); // Set icon for the
		shell.open();
	}

	public boolean isOpen() {
		return Objects.nonNull(shell) && !shell.isDisposed();
	}

	// ----------------- Debug events -----------------
	@SuppressWarnings("unchecked")
	public void handleDebugEvent(AbstractDebugEvent event) {
		Display.getDefault().asyncExec(() -> {
			if (shell.isDisposed())
				return;
			if (Objects.equals(event.getType(),
					SimpleDebuggerEventTypes.SimpleDebuggerEventType.STOPPED_AT_BREAKPOINT)) {
				DebugEvent<DebugWindowDataDTO> simpleDebugEvent = (DebugEvent<DebugWindowDataDTO>) event;
				refreshDataAtBreakpoint(simpleDebugEvent.getPayload());
			} else if (Objects.equals(event.getType(),
					SimpleDebuggerEventTypes.SimpleDebuggerEventType.REFRESH_CONSOLE)) {
				DebugEvent<String> simpleDebugEvent = (DebugEvent<String>) event;
				consoleTabContent.appendLine(simpleDebugEvent.getPayload());
			} else if (Objects.equals(event.getType(),
					SimpleDebuggerEventTypes.SimpleDebuggerEventType.METHOD_INVOKE)) {
			} else if (Objects.equals(event.getType(),
					SimpleDebuggerEventTypes.SimpleDebuggerEventType.SET_RESUME_BUTTON_STATE)) {
				DebugEvent<Boolean> simpleDebugEvent = (DebugEvent<Boolean>) event;
				resumeButton.setEnabled(simpleDebugEvent.getPayload());
			} else if (Objects.equals(event.getType(),
					SimpleDebuggerEventTypes.SimpleDebuggerEventType.DISPLAY_ADDITIONAL_INFO)) {
				DebugEvent<UserInstanceDetailsDTO> simpleDebugEvent = (DebugEvent<UserInstanceDetailsDTO>) event;
				classMembersAtBreakpoint.showFieldInfoPopupFromBackend(simpleDebugEvent.getPayload());
			}
		});
	}

	private void refreshDataAtBreakpoint(DebugWindowDataDTO debugWindowDataDTO) {
		if (debugWindowDataDTO == null)
			return;
		locationLabel.setText(
				STOP_INFO + debugWindowDataDTO.getMethodName() + " line: " + debugWindowDataDTO.getLineNumber());
		resumeButton.setEnabled(true);
		classMembersAtBreakpoint.showInnerElementsInTable(debugWindowDataDTO);
		contextTab.showElementsFromDebugWindowData(debugWindowDataDTO);
		stackTabContent.updateStack(debugWindowDataDTO.getCompileStackInfo());
	}

	protected void appendConsoleLine(String line) {
		consoleTabContent.appendLine(line);
	}

	@Override
	public boolean equals(Object object) {
		if (this == object)
			return true;
		if (Objects.isNull(object))
			return false;
		if (!(object instanceof MainWindow))
			return false;
		MainWindow other = (MainWindow) object;
		return Objects.nonNull(shell) && shell.equals(other.shell);
	}

	@Override
	public int hashCode() {
		return Objects.nonNull(shell) ? shell.hashCode() : 0;
	}

	/**
	 * Shows an error dialog to the user.
	 *
	 * @param title   dialog title
	 * @param message error message
	 */
	public void showError(String title, String message) {
		if (Objects.isNull(shell) || shell.isDisposed()) {
			shell = new Shell(Display.getDefault());
		}
		Display.getDefault().asyncExec(() -> {
			MessageBox dialog = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
			dialog.setText(title);
			dialog.setMessage(message);
			dialog.open();
		});
	}

	public void close() {
		// TODO Auto-generated method stub
		
	}

}