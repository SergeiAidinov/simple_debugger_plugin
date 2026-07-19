package com.gmail.aydinov.sergey.simple_debugger_plugin.core.inspection;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.gmail.aydinov.sergey.simple_debugger_plugin.core.DebuggerContext;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.PageableDataProvider;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.DataProviderHolder;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.InspectionSeanceCache;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.StoppableDataProviderHolder;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.TerminableDataProvider;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.PageableDataProvider;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.PairDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.AbstractSimpleDebuggerEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.UIEvent;

public class DataProviderHolderImpl implements StoppableDataProviderHolder {

	private final PageableDataProvider dataProvider;
	private final int inspectionSeanceId;
	private final Thread thread;
	private final AtomicBoolean started = new AtomicBoolean(false);
	private final AtomicBoolean stopped = new AtomicBoolean(false);

	private final BlockingQueue<AbstractSimpleDebuggerEvent> eventsForProvider = new LinkedBlockingQueue<>();

	public DataProviderHolderImpl(PageableDataProvider dataProvider, int inspectionSeanceId) {
		this.dataProvider = dataProvider;
		this.inspectionSeanceId = inspectionSeanceId;
		this.thread = new Thread(this::starter);
		// this.thread.start();
	}

	@Override
	public void handleEvent(AbstractSimpleDebuggerEvent abstractSimpleDebuggerEvent) {
		eventsForProvider.add(abstractSimpleDebuggerEvent);
	}

	@Override
	public void startDataProvider() {
		if (started.compareAndSet(false, true)) {
			thread.start();
		}
	}

	@Override
	public void stopDataProvider() {
		stopped.set(true);
		thread.interrupt();
	}

//	public Thread getThread() {
//		return thread;
//	}

	@Override
	public PageableDataProvider getDataProvider() {
		return dataProvider;
	}

//	public BlockingQueue<AbstractSimpleDebuggerEvent> getEventsForProvider() {
//		return eventsForProvider;
//	}

	@SuppressWarnings({ "unchecked", "unused" })
	private void starter() {
		while (!stopped.get() && DebuggerContext.context().isInspectionSeanceActive()
				&& DebuggerContext.context().getInspectionSeanceId() == inspectionSeanceId) {
			AbstractSimpleDebuggerEvent abstractSimpleDebuggerUIEvent = null;
			try {
				abstractSimpleDebuggerUIEvent = eventsForProvider.poll(1, TimeUnit.SECONDS);
				;
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
			if (Objects.isNull(abstractSimpleDebuggerUIEvent))
				continue;
			if (abstractSimpleDebuggerUIEvent instanceof UIEvent uiEvent) {
				if (uiEvent.getPayload() instanceof PairDTO pair) {
					uiEvent = (UIEvent<PairDTO<InnerElementRepresentationDTO, Integer>>) abstractSimpleDebuggerUIEvent;
					pair = (PairDTO<InnerElementRepresentationDTO, Integer>) uiEvent.getPayload();
					Integer argument = (Integer) pair.getSecond();
					dataProvider.handlePageRequest(argument);
				} else {
					if (uiEvent.getPayload() instanceof InnerElementRepresentationDTO innerElementRepresentationDTO) {
						uiEvent = (UIEvent<InnerElementRepresentationDTO>) abstractSimpleDebuggerUIEvent;
						dataProvider.handlePageRequest(null);
					}
				}
			}

		}
	}

	@Override
	public void terminateCurrentRequest() {
		if (dataProvider instanceof TerminableDataProvider terminableDataProvider)
		terminableDataProvider.terminateCurrentRequest();

	}

}
