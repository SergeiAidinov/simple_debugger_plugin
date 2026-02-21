package com.gmail.aydinov.sergey.simple_debugger_plugin.ui;

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

import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebuggerEventType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.debug_event.AbstractSimpleDebugEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.debug_event.BackendMethodExecutedEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.debug_event.ConsoleUpdateDebugEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.debug_event.DebugStoppedAtBreakpointEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.debug_event.SetResumeButtonEnabled;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.UserClosedWindowUiEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.UserPressedResumeUiEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.processor.SimpleDebugEventProcessor;
import com.gmail.aydinov.sergey.simple_debugger_plugin.processor.SimpleDebuggerEventQueue;
import com.gmail.aydinov.sergey.simple_debugger_plugin.processor.UiEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.tab.ConsoleTabContent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.tab.EvaluateTabController;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.tab.FieldsAndVariablesTabContent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.tab.StackTabContent;

/**
 * Main debugger window displaying combined Variables + Fields tab, stack trace, evaluation, and console.
 * <p>
 * Author: Sergei Aidinov
 * <br>
 * Email: <a href="mailto:sergey.aydinov@gmail.com">sergey.aydinov@gmail.com</a>
 * </p>
 */
public class DebugWindow {

    private Shell shell;
    private CTabFolder tabFolder;

    // Combined Variables + Fields tab
    private FieldsAndVariablesTabContent variablesFieldsTabContent;
    private StackTabContent stackTabContent;
    private EvaluateTabController evaluateTabController;
    private ConsoleTabContent consoleTabContent;

    private Button resumeButton;
    private Label locationLabel;

    private final UiEventCollector uiEventCollector = SimpleDebuggerEventQueue.instance();
    private final String STOP_INFO = "Stopped at: ";

    /**
     * Constructs and initializes the debugger window.
     */
    public DebugWindow() {
        Display display = Display.getDefault();
        shell = new Shell(display);
        shell.setText("Simple Debugger");
        shell.setSize(800, 600);
        shell.setLayout(new GridLayout(1, false));

        // ----------------- Top panel -----------------
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

        // Combined Variables + Fields tab
        variablesFieldsTabContent = new FieldsAndVariablesTabContent(tabFolder);
        CTabItem varsFieldsTabItem = new CTabItem(tabFolder, SWT.NONE);
        varsFieldsTabItem.setText("Fields and Variables");
        varsFieldsTabItem.setControl(variablesFieldsTabContent.getControl());

        // Stack tab
        stackTabContent = new StackTabContent(tabFolder);
        CTabItem stackTabItem = new CTabItem(tabFolder, SWT.NONE);
        stackTabItem.setText("Stack");
        stackTabItem.setControl(stackTabContent.getControl());

        // Evaluate tab
        evaluateTabController = new EvaluateTabController(tabFolder);
        CTabItem evaluateTabItem = new CTabItem(tabFolder, SWT.NONE);
        evaluateTabItem.setText("Evaluate");
        evaluateTabItem.setControl(evaluateTabController.getControl());

        // Console tab
        consoleTabContent = new ConsoleTabContent(tabFolder);
        CTabItem consoleTabItem = new CTabItem(tabFolder, SWT.NONE);
        consoleTabItem.setText("Console");
        consoleTabItem.setControl(consoleTabContent.getControl());

        tabFolder.setSelection(0);

        // ----------------- Hook Resume button -----------------
        hookResumeButton();
        hookCross();

        // Start debug event processor in daemon thread
        SimpleDebugEventProcessor simpleDebugEventProcessor = new SimpleDebugEventProcessor(this);
        Thread processorThread = new Thread(simpleDebugEventProcessor);
        processorThread.setDaemon(true);
        processorThread.start();
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
        if (response == SWT.NO) return false;

        shell.dispose();
        uiEventCollector.collectUiEvent(new UserClosedWindowUiEvent());
        return true;
    }

    private void hookResumeButton() {
        resumeButton.addListener(SWT.Selection, e -> uiEventCollector.collectUiEvent(new UserPressedResumeUiEvent()));
    }

    public Shell getShell() {
        return shell;
    }

    /**
     * Opens the debugger window and sets the window icon.
     */
    public void open() {
        // Load window icon
    	shell.setImage(DebugWindowManager.instance().icons.get("debugger")); // Set icon for the window
        shell.open();
    }

    public boolean isOpen() {
        return Objects.nonNull(shell) && !shell.isDisposed();
    }

    // ----------------- Debug events -----------------
    public void handleDebugEvent(AbstractSimpleDebugEvent event) {
        Display.getDefault().asyncExec(() -> {
            if (shell.isDisposed()) return;

            if (Objects.equals(event.getType(), SimpleDebuggerEventType.STOPPED_AT_BREAKPOINT)) {
                refreshDataAtBreakpoint((DebugStoppedAtBreakpointEvent) event);
            } else if (Objects.equals(event.getType(), SimpleDebuggerEventType.REFRESH_CONSOLE)) {
                ConsoleUpdateDebugEvent consoleEvent = (ConsoleUpdateDebugEvent) event;
                consoleTabContent.appendLine(consoleEvent.getText());
            } else if (Objects.equals(event.getType(), SimpleDebuggerEventType.METHOD_INVOKE)) {
                BackendMethodExecutedEvent methodEvent = (BackendMethodExecutedEvent) event;
                evaluateTabController.clearResult();
                evaluateTabController.showResult(methodEvent.getResultOfInvocation());
            } else if (Objects.equals(event.getType(), SimpleDebuggerEventType.SET_RESUME_BUTTON_STATE)) {
            	SetResumeButtonEnabled setResumeButtonEnabled =  (SetResumeButtonEnabled) event;
            	resumeButton.setEnabled(setResumeButtonEnabled.setButtonStatus());
            }
        });
    }

    private void refreshDataAtBreakpoint(DebugStoppedAtBreakpointEvent event) {
        if (event == null) return;

        locationLabel.setText(STOP_INFO + event.getClassName() + "." + event.getMethodName() + " line:" + event.getLineNumber());
        resumeButton.setEnabled(true);

        variablesFieldsTabContent.updateVariablesAndFields(event.getLocals(), event.getFields());
        stackTabContent.updateStack(event.getMethodCallInStacks());
        evaluateTabController.updateFromEvent(event);
    }

    public void appendConsoleLine(String line) {
        consoleTabContent.appendLine(line);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (Objects.isNull(object)) return false;
        if (!(object instanceof DebugWindow)) return false;
        DebugWindow other = (DebugWindow) object;
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
}