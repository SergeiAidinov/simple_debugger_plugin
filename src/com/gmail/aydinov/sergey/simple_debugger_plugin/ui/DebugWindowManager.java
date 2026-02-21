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

public class DebugWindowManager {

	private static DebugWindowManager INSTANCE;

	private DebugWindow debugWindow;
	private InspectWindow inspectWindow;

	/** Минимальный ресурсный источник: карта с изображениями */
	public final Map<String, Image> icons;

	private DebugWindowManager() {
		Map<String, Image> iconsTemp = new HashMap<>();
		List<PairDTO<String, String>> namesAndPaths = List.of(PairDTO.of("debugger", "/icons/icon.png"),
				PairDTO.of("inspectIcon", "/icons/inspect.png"), PairDTO.of("variableIcon", "/icons/variable.png"),
				PairDTO.of("fieldIcon", "/icons/field.png"));
		namesAndPaths.forEach(p -> iconsTemp.put(p.getKey(), loadIcon(p.getValue())));
		icons = Map.copyOf(iconsTemp);
	}

	public static synchronized DebugWindowManager instance() {
		if (Objects.isNull(INSTANCE)) {
			INSTANCE = new DebugWindowManager();
		}
		return INSTANCE;
	}

	/**
	 * Возвращает или создаёт главное окно
	 */
	public DebugWindow getOrCreateWindow() {
		if (!DebuggerContext.context().isRunning())
			return null;

		if (Objects.isNull(debugWindow) || !debugWindow.isOpen()) {
			debugWindow = new DebugWindow();
			Display.getDefault().asyncExec(() -> debugWindow.open());
		}
		return debugWindow;
	}

	/**
	 * Открывает или обновляет InspectWindow
	 */
	public InspectWindow openInspectWindow() {
		if (!DebuggerContext.context().isRunning())
			return null;

		if (Objects.nonNull(inspectWindow) && inspectWindow.isOpen()) {
			inspectWindow.close();
		}

		inspectWindow = new InspectWindow();
		Display.getDefault().asyncExec(() -> inspectWindow.open());
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
				System.err.println("Icon not found: " + path);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return image;
	}
}