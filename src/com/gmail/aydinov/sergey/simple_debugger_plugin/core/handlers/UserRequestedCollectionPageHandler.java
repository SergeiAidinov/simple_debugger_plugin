package com.gmail.aydinov.sergey.simple_debugger_plugin.core.handlers;

import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.data_model.TargetApplicationRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.DebuggerContext;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.data_provider.IterableDataProvider;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.inspection.PageableDataProviderHolderImpl;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.inspection.HandlerContext;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.inspection.InspectionHandlerContext;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.UIEventHandler;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.holder.DataProviderHolder;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.provider.PageableDataProvider;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.PairDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.DebugEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.SimpleDebuggerEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.AbstractUIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.UIEvent;
import com.sun.jdi.event.BreakpointEvent;

public class UserRequestedCollectionPageHandler implements UIEventHandler{
	
	 private final TreeMap<Integer, InnerElementRepresentationDTO> collectionElements = new TreeMap<>();
	 private final DebugEventCollector debugEventCollector = SimpleDebuggerEventCollector.instance();
	 private BreakpointEvent breakpointEvent;
	 private String collectionType;
	 
	 @Override
	 public boolean handle(HandlerContext abstractUIEventContext, AbstractUIEvent abstractSimpleDebuggerUIEvent) {
		 if (!(abstractUIEventContext instanceof InspectionHandlerContext)) return false;
		 InspectionHandlerContext inspectionHandlerContext =  (InspectionHandlerContext) abstractUIEventContext;
			// UIEvent<T> uiEvent = (UIEvent<T>) abstractSimpleDebuggerUIEvent;
			UIEvent<PairDTO<InnerElementRepresentationDTO, Integer>> userReqeustedMapPageEvent = null;
			try {
				userReqeustedMapPageEvent = (UIEvent<PairDTO<InnerElementRepresentationDTO, Integer>>) abstractSimpleDebuggerUIEvent;
			} catch (ClassCastException castException) {
				System.out.println(castException);
			}

			final long id = userReqeustedMapPageEvent.getPayload().getFirst().getObjectId();
			String descriprion = userReqeustedMapPageEvent.getPayload().getFirst().getElementName() + " page: "
					+ userReqeustedMapPageEvent.getPayload().getSecond();
			System.out.println(descriprion);
			inspectionHandlerContext.getInspectionSeanceCache().addOrModifyBreadCrumb(
					userReqeustedMapPageEvent.getPayload().getFirst().getObjectId(), userReqeustedMapPageEvent,
					descriprion, userReqeustedMapPageEvent.getPayload().getSecond());
			DataProviderHolder dataProviderHolder = inspectionHandlerContext.getInspectionSeanceCache()
					.getDataProviderHolders().get(id);
			// MapPageDTO<InnerElementRepresentationDTO, InnerElementRepresentationDTO> page
			// = null;
			if (Objects.nonNull(dataProviderHolder)) {
				dataProviderHolder.handleEvent(userReqeustedMapPageEvent);
				System.out.println();
			} else {
				Optional<UniversalElementRepresentation> i = TargetApplicationRepresentation.getInstance().getAllElements()
						.stream().filter(e -> e instanceof UniversalElementRepresentation)
						.map(e -> (UniversalElementRepresentation) e)
						.filter(e -> Objects.equals(e.getObjectReferenceId(), id)).findAny();

				UniversalElementRepresentation mapRepresentation = i.get();
				PageableDataProvider pageableDataProvider = new IterableDataProvider(mapRepresentation, inspectionHandlerContext);
			//	iterableDataProvider.setDataProviderHolderId(id);
				dataProviderHolder = new PageableDataProviderHolderImpl(pageableDataProvider,
						DebuggerContext.context().getInspectionSeanceId());
				dataProviderHolder.startDataProvider();
				inspectionHandlerContext.getInspectionSeanceCache().getDataProviderHolders().put(id,
						dataProviderHolder);
				System.out.println(userReqeustedMapPageEvent);
				inspectionHandlerContext.getInspectionSeanceCache().getDataProviderHolders().get(id)
						.handleEvent(userReqeustedMapPageEvent);

			}
		return false;
	 }
}