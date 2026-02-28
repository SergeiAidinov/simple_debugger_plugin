package com.gmail.aydinov.sergey.simple_debugger_plugin.ui;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.DebuggerContext;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.DebuggerContext.SimpleDebuggerStatus;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.PairDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.TripletDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebuggerEventTypes;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.SimpleDebuggerEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.debug_event.AbstractDebugEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.debug_event.DebugEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.logging.SimpleDebuggerLogger;

public class DebugWindowsManager implements Runnable {

	private static DebugWindowsManager INSTANCE;

	private DebugWindow debugWindow;
	private InspectWindow inspectWindow;

	/** Минимальный ресурсный источник: карта с изображениями */
	public final Map<String, PairDTO<Image, String>> icons;

	private DebugWindowsManager() {
		// Map<String, Image> iconsTemp = new HashMap<>();
		Map<String, PairDTO<Image, String>> iconsTemp = new HashMap<>();
		List<TripletDTO<String, String, String>> namesAndPaths = List.of(
				TripletDTO.of("enum", "/icons/enum.png", "enum"),
				TripletDTO.of("fieldIcon", "/icons/field.png", "non-static field"),
				TripletDTO.of("debugger", "/icons/icon.png", "debugger_icon"),
				TripletDTO.of("inspectIcon", "/icons/inspect.png", "inspect element"),
				TripletDTO.of("interface", "/icons/method.png", "interface"),
				TripletDTO.of("method", "/icons/method.png", "non-static method"),
				TripletDTO.of("static_field", "/icons/static_field.png", "static field"),
				TripletDTO.of("static_method", "/icons/static_method.png", "static method"),
				TripletDTO.of("variableIcon", "/icons/variable.png", "local variable"));
		namesAndPaths.forEach(p -> iconsTemp.put(p.getFirst(), PairDTO.of(loadIcon(p.getSecond()), p.getThird())));
		icons = Map.copyOf(iconsTemp);
	}

	public static synchronized DebugWindowsManager instance() {
		if (Objects.isNull(INSTANCE)) {
			INSTANCE = new DebugWindowsManager();
			Thread debugWindowsmanagerThread = new Thread(INSTANCE);
			debugWindowsmanagerThread.setDaemon(true);
			debugWindowsmanagerThread.start();
		}
		return INSTANCE;
	}

	@Override
	public void run() {
		windowsManaging();
	}

	/**
	 * Возвращает или создаёт главное окно
	 */
	public DebugWindow getOrCreateDebugWindow() {
		if (DebuggerContext.context().isInTerminalState())
			return null;

		if (Objects.isNull(debugWindow) || !debugWindow.isOpen()) {
			Display.getDefault().syncExec(() -> {
				debugWindow = new DebugWindow(); // теперь создается в UI-потоке
				debugWindow.open();
			});
		}
		return debugWindow;
	}

	/**
	 * Открывает или обновляет InspectWindow
	 */
	private InspectWindow openNewInspectWindow() {
		if (!DebuggerContext.context().isRunning())
			return null;
		if (Objects.nonNull(inspectWindow) && inspectWindow.isOpen()) {
			inspectWindow.close();
		}
		Display.getDefault().syncExec(() -> {
			inspectWindow = new InspectWindow();
			inspectWindow.open();
			DebuggerContext.context().setStatus(SimpleDebuggerStatus.INSPECTION_SEANCE_RUNNING);

		});
		return inspectWindow;
	}

	/**
	 * Загружает изображение один раз и кладёт его в карту
	 */
	public Image loadIcon(String path) {
		Image image = null;
		try (InputStream is = getClass().getResourceAsStream(path)) {
			if (is != null) {
				image = new Image(Display.getDefault(), is);
			} else {
				SimpleDebuggerLogger.warn("Icon not found: " + path);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return image;
	}

	/**
	 * Continuously processes debug events from the global event queue. This method
	 * blocks when no events are available and will only stop if the thread is
	 * interrupted.
	 */
	private void windowsManaging() {
		while (!DebuggerContext.context().isInTerminalState()) {
			try {
				AbstractDebugEvent event = SimpleDebuggerEventCollector.instance().takeDebugEvent();
				SimpleDebuggerLogger.info("SimpleDebugEvent: " + event);

				if (SimpleDebuggerEventTypes.isDebugWindowEvent(event.getType()) && Objects.equals(event.getType(),
						SimpleDebuggerEventTypes.SimpleDebuggerEventType.DISPLAY_INSPECTION_WINDOW)) {
					openNewInspectWindow();
					continue;
				}

				if (Objects.nonNull(debugWindow) && debugWindow.isOpen()
						&& SimpleDebuggerEventTypes.isDebugWindowEvent(event.getType())
						&& !Objects.equals(event.getType(),
								SimpleDebuggerEventTypes.SimpleDebuggerEventType.DISPLAY_INSPECTION_WINDOW)) {
					debugWindow.handleDebugEvent(event);
					continue;
				}

				// handling inspection windows events
				if (SimpleDebuggerEventTypes.isInspectionWindowEvent(event.getType())
						&& (Objects.nonNull(inspectWindow) && inspectWindow.isOpen())) {
					inspectWindow.handleDebugEvent(event);
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break; // exit loop if interrupted
			}
		}
	}
}