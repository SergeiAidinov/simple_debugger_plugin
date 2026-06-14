package com.gmail.aydinov.sergey.simple_debugger_plugin.ui.window;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebuggerEventTypes.SimpleDebuggerEventType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.SimpleDebuggerEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.UiEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.UIEvent;

public class LoadingWindow {

	private final Display display;
	private final Shell shell;

	private final Label messageLabel;
	private final Button cancelButton;

	private final UiEventCollector uiEventCollector = SimpleDebuggerEventCollector.instance();
//	private final CurrentlyInspectedObjectIdHolder currentlyInspectedObjectIdHolder;
	private final long dataProviderHolderId;
	
	public LoadingWindow(long dataProviderHolderId) {
		this.display = Display.getDefault();
		this.dataProviderHolderId = dataProviderHolderId;
	//	this.currentlyInspectedObjectIdHolder = currentlyInspectedObjectIdHolder;
		// ✅ полноценное окно с рамкой как в Eclipse
		shell = new Shell(display.getActiveShell(), SWT.SHELL_TRIM);
		shell.setText("Loading");
		shell.setLayout(new GridLayout(1, false));
		shell.setSize(350, 140);

		GridData labelData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		labelData.widthHint = 320;

		messageLabel = new Label(shell, SWT.WRAP);
		messageLabel.setLayoutData(labelData);
		messageLabel.setText("Loading...");

		cancelButton = new Button(shell, SWT.PUSH);
		cancelButton.setText("Cancel loading");
		cancelButton.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false));

		cancelButton.addListener(SWT.Selection, e -> {
			uiEventCollector.collectUiEvent(
					new UIEvent<>(SimpleDebuggerEventType.USER_CANCELLED_LOADING, dataProviderHolderId));
			close();
		});

		shell.addListener(SWT.Close, e -> {
			e.doit = false;
		});
	}

	public void setMessage(String message) {
		display.asyncExec(() -> {
			if (!shell.isDisposed()) {
				messageLabel.setText(message);
				shell.pack();
				shell.layout();
			}
		});
	}

	public void show() {
		display.asyncExec(() -> {
			if (!shell.isDisposed()) {
				shell.open();
				shell.layout();
				shell.setActive(); // 👈 важно для Eclipse-like поведения
			}
		});
	}

	public void close() {
		display.asyncExec(() -> {
			if (!shell.isDisposed()) {
				shell.dispose();
			}
		});
	}

	public Shell getShell() {
		return shell;
	}
}