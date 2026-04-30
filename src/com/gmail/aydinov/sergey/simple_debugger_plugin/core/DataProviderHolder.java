package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.DataProvider;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.PairDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.AbstractSimpleDebuggerEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.AbstractUIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.UIEvent;

public class DataProviderHolder {
	
	private final int inspectionSeanceId;
	private final Thread thread;
	private final DataProvider dataProvider;
	private final BlockingQueue<AbstractSimpleDebuggerEvent> eventsForProvider = new LinkedBlockingQueue<>();
	
	public DataProviderHolder(DataProvider dataProvider, int inspectionSeanceId) {
		this.dataProvider = dataProvider;
		this.inspectionSeanceId = inspectionSeanceId;
		this.thread = new Thread(this::starter);
		this.thread.start();
	}

	public void handleEvent(AbstractSimpleDebuggerEvent abstractSimpleDebuggerEvent) {
		eventsForProvider.add(abstractSimpleDebuggerEvent);
	}
	
	public Thread getThread() {
		return thread;
	}

	public DataProvider getDataProvider() {
		return dataProvider;
	}

	public BlockingQueue<AbstractSimpleDebuggerEvent> getEventsForProvider() {
		return eventsForProvider;
	}

	@SuppressWarnings("unchecked")
	private void starter() {
		while(DebuggerContext.context().isInspectionSeanceActive() && DebuggerContext.context().getInspectionSeanceId() == inspectionSeanceId) {
			AbstractSimpleDebuggerEvent abstractSimpleDebuggerUIEvent= null;
			try {
				abstractSimpleDebuggerUIEvent = eventsForProvider.poll(1, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (Objects.isNull(abstractSimpleDebuggerUIEvent)) continue;
			UIEvent<PairDTO<InnerElementRepresentationDTO, Integer>> uiEvent = null;
			try {
				if (abstractSimpleDebuggerUIEvent instanceof UIEvent)
				uiEvent = (UIEvent<PairDTO<InnerElementRepresentationDTO, Integer>>) abstractSimpleDebuggerUIEvent;
			} catch (ClassCastException castException) {

			}
			if (Objects.isNull(uiEvent)) continue;
			dataProvider.getData(uiEvent.getPayload().getSecond());
			
		}
	}

}
