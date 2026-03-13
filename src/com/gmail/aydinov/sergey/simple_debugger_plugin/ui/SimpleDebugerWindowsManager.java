package com.gmail.aydinov.sergey.simple_debugger_plugin.ui;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import com.gmail.aydinov.sergey.simple_debugger_plugin.core.DebuggerContext;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.PairDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.TripletDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebuggerEventTypes;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.SimpleDebuggerEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.debug_event.AbstractDebugEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.logging.SimpleDebuggerLogger;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.window.collection_inspector_window.ManageableCollectionInspectorWindow;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.window.main_window.ManageableMainWindow;

public class SimpleDebugerWindowsManager implements Runnable, MainWinodwManager {

	private static SimpleDebugerWindowsManager INSTANCE;

	private ManageableMainWindow mainWindow;
	private ManageableCollectionInspectorWindow collectionInspectorWindow;

	/** Минимальный ресурсный источник: карта с изображениями */
	public final Map<String, PairDTO<Image, String>> icons;

	private SimpleDebugerWindowsManager() {
		// Map<String, Image> iconsTemp = new HashMap<>();
		Map<String, PairDTO<Image, String>> iconsTemp = new HashMap<>();
		List<TripletDTO<String, String, String>> namesAndPaths = List.of(
				TripletDTO.of("enum", "/icons/enum.png", "enum"),
				TripletDTO.of("fieldIcon", "/icons/field.png", "non-static field"),
				TripletDTO.of("debugger", "/icons/icon.png", "debugger_icon"),
				TripletDTO.of("inspectIcon", "/icons/inspect.png", null),
				TripletDTO.of("interface", "/icons/method.png", "interface"),
				TripletDTO.of("method", "/icons/method.png", "non-static method"),
				TripletDTO.of("static_field", "/icons/static_field.png", "static field"),
				TripletDTO.of("static_method", "/icons/static_method.png", "static method"),
				TripletDTO.of("lens", "/icons/lens.png", "inspect collection"),
				TripletDTO.of("variableIcon", "/icons/variable.png", "local variable"));
		namesAndPaths.forEach(p -> iconsTemp.put(p.getFirst(), PairDTO.of(loadIcon(p.getSecond()), p.getThird())));
		icons = Map.copyOf(iconsTemp);
	}

	public static synchronized SimpleDebugerWindowsManager instance() {
		if (Objects.isNull(INSTANCE)) {
			INSTANCE = new SimpleDebugerWindowsManager();
			Thread debugWindowsmanagerThread = new Thread(INSTANCE);
			debugWindowsmanagerThread.setDaemon(true);
			debugWindowsmanagerThread.start();
		}
		return INSTANCE;
	}

	@Override
	public void run() {
		dispatchEvent();
	}

	/**
	 * Возвращает или создаёт главное окно
	 */
	@Override
	public synchronized ManageableMainWindow getOrCreateMainWindow() {
		if (DebuggerContext.context().isInTerminalState())
			return null;
		this.mainWindow = ManageableMainWindow.getOrCreateMainWindow();
		return this.mainWindow;
	}

	/**
	 * Открывает или обновляет InspectWindow
	 */
//	public synchronized ManageableCollectionInspectorWindow getOrCreateInspectWindow() {
//		if (!DebuggerContext.context().isRunning())
//			return null;
//		this.collectionInspectorWindow = ManageableCollectionInspectorWindow.getOrCreateCollectionInspectWindow();
//		return this.collectionInspectorWindow;
//	}

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
	private void dispatchEvent() {
		while (!DebuggerContext.context().isInTerminalState()) {
			try {
				AbstractDebugEvent event = SimpleDebuggerEventCollector.instance().takeDebugEvent();
				if (Objects.isNull(event)) continue;
				SimpleDebuggerLogger.info("SimpleDebugEvent: " + event);
				
				if(SimpleDebuggerEventTypes.isCollectionInspectionWindowEvent(event.getType())) {
					collectionInspectorWindow = ManageableCollectionInspectorWindow.getOrCreateCollectionInspectWindow();
					collectionInspectorWindow.handleDebugEvent(event);
				} else mainWindow.handleDebugEvent(event);

				// handling inspection windows events
//				if (SimpleDebuggerEventTypes.isInspectionWindowEvent(event.getType())
//						&& (Objects.nonNull(collectionInspectorWindow) && collectionInspectorWindow.isOpen())) {
//					collectionInspectorWindow.handleDebugEvent(event);
//				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break; // exit loop if interrupted
			}
		}
	}
}