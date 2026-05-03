package com.gmail.aydinov.sergey.simple_debugger_plugin.core.inspection;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.gmail.aydinov.sergey.simple_debugger_plugin.core.DebuggerContext;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.DataProvider;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.InspectionSeanceCache;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.PairDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.AbstractSimpleDebuggerEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.UIEvent;

public class DataProviderHolder {

	private final DataProvider dataProvider;
//	private final InspectionSeanceUIEventContext inspectionSeanceUIEventContext;
	private final int inspectionSeanceId;
	private final Thread thread;

	private final BlockingQueue<AbstractSimpleDebuggerEvent> eventsForProvider = new LinkedBlockingQueue<>();

	public DataProviderHolder(DataProvider dataProvider,
			/* InspectionSeanceUIEventContext inspectionSeanceUIEventContext, */ int inspectionSeanceId) {
		this.dataProvider = dataProvider;
		// this.inspectionSeanceUIEventContext = inspectionSeanceUIEventContext;
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

	@SuppressWarnings({ "unchecked", "unused" })
	private void starter() {
		while (DebuggerContext.context().isInspectionSeanceActive()
				&& DebuggerContext.context().getInspectionSeanceId() == inspectionSeanceId) {
			AbstractSimpleDebuggerEvent abstractSimpleDebuggerUIEvent = null;
			try {
				abstractSimpleDebuggerUIEvent = eventsForProvider.poll(1, TimeUnit.SECONDS);;
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
			if (Objects.isNull(abstractSimpleDebuggerUIEvent))
				continue;
			UIEvent<PairDTO<InnerElementRepresentationDTO, Integer>> uiEvent = null;
			try {
				if (abstractSimpleDebuggerUIEvent instanceof UIEvent)
					uiEvent = (UIEvent<PairDTO<InnerElementRepresentationDTO, Integer>>) abstractSimpleDebuggerUIEvent;
			} catch (ClassCastException castException) {

			}
			if (Objects.isNull(uiEvent))
				continue;
			dataProvider.handlePageRequest(uiEvent.getPayload().getSecond());

		}
	}

}
