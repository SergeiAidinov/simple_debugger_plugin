package com.gmail.aydinov.sergey.simple_debugger_plugin.ui;

import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.VariablesTabContent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.FieldsTabContent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.StackTabContent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.DebugEventListener;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.DebugEventProvider;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.SimpleDebuggerWorkFlow;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.UiEventListener;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.UiEventProvider;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.DebugEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.UIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.EvaluateTabContent;
import com.sun.jdi.Field;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.Location;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Value;

public class DebugWindow implements DebugEventListener, UiEventProvider {

    private Shell shell;
    private CTabFolder tabFolder;

    // Поля вкладок
    private VariablesTabContent variablesTab;
    private FieldsTabContent fieldsTab;
    private StackTabContent stackTab;
    private EvaluateTabContent evalTab;
    private Button resumeButton;
    private ThreadReference suspendedThread;
    private DebugEventProvider debugEventProvider;
    private UiEventListener uiEventListener;

    public DebugWindow() {
    	uiEventListener = SimpleDebuggerWorkFlow.Factory.getInstanceOfSimpleDebuggerWorkFlow();
    	Display display = Display.getDefault();
        shell = new Shell(display);
        shell.setText("Simple Debugger");
        shell.setSize(800, 600);
        shell.setLayout(new GridLayout(1, false));

        // Панель для кнопок
        Composite topPanel = new Composite(shell, SWT.NONE);
        topPanel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        topPanel.setLayout(new RowLayout(SWT.HORIZONTAL));

        resumeButton = new Button(topPanel, SWT.PUSH);
        resumeButton.setText("Resume");
        resumeButton.setEnabled(false); // включать только когда есть остановка

        // TAB folder
        tabFolder = new CTabFolder(shell, SWT.BORDER);
        tabFolder.setSimple(false);
        tabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        // Табы
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

        hookResumeButton();
    }
    
	public void setDebugEventProvider(DebugEventProvider debugEventProvider) {
		this.debugEventProvider = debugEventProvider;
	}

	private void hookResumeButton() {
		System.out.println();
        resumeButton.addListener(SWT.Selection, e -> {
            if (suspendedThread != null) {
                try {
                    suspendedThread.resume();   // ← правильно возобновляет ТОЛЬКО этот поток
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                resumeButton.setEnabled(false); // выключаем до следующего стопа
                suspendedThread = null;
            }
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

    /**
     * Обновление вкладок при остановке на брейкпойнте.
     * Вызывается из DebugWindowManager, безопасно через UI-поток.
     */
    public void updateLocation(Location location, ThreadReference thread) {
System.out.println();
        // сохраняем поток, чтобы кнопка Resume могла его продолжить
        this.suspendedThread = thread;

        Display.getDefault().asyncExec(() -> {
            try {
                if (shell.isDisposed()) return;

                resumeButton.setEnabled(true);   // ← включаем кнопку при остановке

                // Верхняя рамка стека
                StackFrame frame = thread.frame(0);

                // Локальные переменные
                List<LocalVariable> vars = frame.visibleVariables();
                Map<LocalVariable, Value> values = frame.getValues(vars);
                variablesTab.updateVariables(values);

                // Поля объекта
                ObjectReference thisObject = frame.thisObject();
                if (thisObject != null) {
                    Map<Field, Value> fields = thisObject.getValues(
                            thisObject.referenceType().fields()
                    );
                    fieldsTab.updateFields(fields);
                }

                // Стек вызовов
                List<StackFrame> frames = thread.frames();
                stackTab.updateStack(frames);

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

	@Override
	public void handleDebugEvent(DebugEvent debugEvent) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void sendUiEvent(UIEvent uiEvent) {
		// TODO Auto-generated method stub
		
	}

}
