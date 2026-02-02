package com.gmail.aydinov.sergey.simple_debugger_plugin.ui;

import java.io.InputStream;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.DebugEventProvider;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebuggerEventType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.debug_event.AbstractSimpleDebugEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.debug_event.ConsoleUpdateDebugEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.debug_event.DebugStoppedAtBreakpointEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.debug_event.MethodInvokedEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.UserClosedWindowUiEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.UserPressedResumeUiEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.logging.SimpleDebuggerLogger;
import com.gmail.aydinov.sergey.simple_debugger_plugin.processor.SimpleDebugEventProcessor;
import com.gmail.aydinov.sergey.simple_debugger_plugin.processor.SimpleDebuggerEventQueue;
import com.gmail.aydinov.sergey.simple_debugger_plugin.processor.UiEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.tab.ConsoleTabContent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.tab.EvaluateTabController;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.tab.FieldsTabContent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.tab.StackTabContent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.tab.VariablesTabContent;

public class DebugWindow {
	
	private DebugEventProvider debugEventProvider;

    private Shell shell;
    private CTabFolder tabFolder;

    // Поля вкладок
    private VariablesTabContent variablesTabContent;
    private FieldsTabContent fieldsTabContent;
    private StackTabContent stackTabContent;
    private EvaluateTabController evaluateTabController;
    private ConsoleTabContent consoleTabContent;

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
        topPanel.setLayout(new GridLayout(3, false));
        topPanel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        locationLabel = new Label(topPanel, SWT.NONE);
        locationLabel.setText(STOP_INFO);
        locationLabel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        Composite spacerComposite = new Composite(topPanel, SWT.NONE);
        spacerComposite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        resumeButton = new Button(topPanel, SWT.PUSH);
        resumeButton.setText("Resume");
        resumeButton.setEnabled(false);
        GridData resumeButtonGridData = new GridData(SWT.RIGHT, SWT.TOP, false, false);
        resumeButton.setLayoutData(resumeButtonGridData);

        int buttonHeight = resumeButton.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
        topPanel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        ((GridData) topPanel.getLayoutData()).heightHint = buttonHeight + 10;

        // ----------------- TAB folder -----------------
        tabFolder = new CTabFolder(shell, SWT.BORDER);
        tabFolder.setSimple(false);
        tabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        // Переменные
        variablesTabContent = new VariablesTabContent(tabFolder, uiEventCollector);
        CTabItem variablesTabItem = new CTabItem(tabFolder, SWT.NONE);
        variablesTabItem.setText("Variables");
        variablesTabItem.setControl(variablesTabContent.getControl());

        // Поля
        fieldsTabContent = new FieldsTabContent(tabFolder);
        CTabItem fieldsTabItem = new CTabItem(tabFolder, SWT.NONE);
        fieldsTabItem.setText("Fields");
        fieldsTabItem.setControl(fieldsTabContent.getControl());

        // Стек
        stackTabContent = new StackTabContent(tabFolder);
        CTabItem stackTabItem = new CTabItem(tabFolder, SWT.NONE);
        stackTabItem.setText("Stack");
        stackTabItem.setControl(stackTabContent.getControl());

        // Evaluate
        evaluateTabController = new EvaluateTabController(tabFolder, uiEventCollector);
        CTabItem evaluateTabItem = new CTabItem(tabFolder, SWT.NONE);
        evaluateTabItem.setText("Evaluate");
        evaluateTabItem.setControl(evaluateTabController.getControl());

        // Console
        consoleTabContent = new ConsoleTabContent(tabFolder);
        CTabItem consoleTabItem = new CTabItem(tabFolder, SWT.NONE);
        consoleTabItem.setText("Console");
        consoleTabItem.setControl(consoleTabContent.getControl());

        tabFolder.setSelection(0);

        // ----------------- Hook кнопки Resume -----------------
        hookResumeButton();
        hookCross();

        SimpleDebugEventProcessor simpleDebugEventProcessor = new SimpleDebugEventProcessor(this);
        Thread processorThread = new Thread(simpleDebugEventProcessor);
        processorThread.setDaemon(true);
        processorThread.start();
    }
    
    public void setDebugEventProvider(DebugEventProvider debugEventProvider) {
        this.debugEventProvider = debugEventProvider;
    }
    
    public Shell getShell() {
        return shell;
    }


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
		if (response == SWT.NO) {
			return false; // отменяем закрытие
		}

		// закрываем
		SimpleDebuggerLogger.info("Debug window closed");
		showVmStoppedMessage();
		// sendUiEvent(new UserClosedWindowUiEvent());
		shell.dispose();
		uiEventCollector.collectUiEvent(new UserClosedWindowUiEvent());
		return true; // разрешаем закрытие
	}
    
    private void showVmStoppedMessage() {
	    // Например:
	    MessageDialog.openInformation(shell, "Debugger", "Debugger detached. Target VM continues running");
	}

    private void hookResumeButton() {
        resumeButton.addListener(SWT.Selection, e -> pressResumeButton());
    }

    private void pressResumeButton() {
        uiEventCollector.collectUiEvent(new UserPressedResumeUiEvent());
    }

    public void open() {
        final Image[] iconHolder = new Image[1]; // держим Image в final массиве

        try (InputStream is = getClass().getResourceAsStream("/icons/icon.png")) {
            if (is != null) {
                iconHolder[0] = new Image(Display.getDefault(), is);
                shell.setImage(iconHolder[0]);
            } else {
            	SimpleDebuggerLogger.error("Icon not found: /icons/icon.png", null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Открываем окно
        shell.open();

        // Уничтожаем изображение после закрытия окна
        shell.addListener(SWT.Dispose, event -> {
            if (iconHolder[0] != null && !iconHolder[0].isDisposed()) {
                iconHolder[0].dispose();
            }
        });
    }


    public boolean isOpen() {
        return shell != null && !shell.isDisposed();
    }

    public void handleDebugEvent(AbstractSimpleDebugEvent event) {
        Display.getDefault().asyncExec(() -> {
            if (shell.isDisposed()) return;

            if (event.getType().equals(SimpleDebuggerEventType.STOPPED_AT_BREAKPOINT)) {
                refreshDataAtBreakepoint((DebugStoppedAtBreakpointEvent) event);
            } else if (event.getType().equals(SimpleDebuggerEventType.REFRESH_CONSOLE)) {
                ConsoleUpdateDebugEvent consoleEvent = (ConsoleUpdateDebugEvent) event;
                consoleTabContent.appendLine(consoleEvent.getText());
            } else if (event.getType().equals(SimpleDebuggerEventType.METHOD_INVOKE)) {
            	MethodInvokedEvent methodInvokedEvent = (MethodInvokedEvent) event;
            	evaluateTabController.clearResult();
            	evaluateTabController.showResult(methodInvokedEvent.getResultOfInvocation());
            	
            }
        });
    }

    private void refreshDataAtBreakepoint(DebugStoppedAtBreakpointEvent event) {
        if (event == null) return;

        locationLabel.setText(STOP_INFO + event.getClassName() + "." + event.getMethodName() + " line:" + event.getLineNumber());
        resumeButton.setEnabled(true);

        variablesTabContent.updateVariables(event.getLocals());
        fieldsTabContent.updateFields(event.getFields());
        stackTabContent.updateStack(event.getMethodCallInStacks());
        evaluateTabController.updateFromEvent(event);
    }

    public void appendConsoleLine(String line) {
        consoleTabContent.appendLine(line);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null) return false;
        if (!(object instanceof DebugWindow)) return false;
        DebugWindow other = (DebugWindow) object;
        return shell != null && shell.equals(other.shell);
    }

    @Override
    public int hashCode() {
        return shell != null ? shell.hashCode() : 0;
    }

    /**
     * Показывает диалог ошибки пользователю
     * @param title заголовок окна
     * @param message текст ошибки
     */
    public void showError(String title, String message) {
        if (shell == null || shell.isDisposed()) {
            shell = new Shell(Display.getDefault());
        }

        Display.getDefault().asyncExec(() -> {
            MessageBox dialog = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
            dialog.setText(title);
            dialog.setMessage(message);
            dialog.open();
        });
    }

}
