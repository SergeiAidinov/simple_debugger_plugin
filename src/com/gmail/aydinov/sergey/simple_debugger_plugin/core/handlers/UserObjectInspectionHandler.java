package com.gmail.aydinov.sergey.simple_debugger_plugin.core.handlers;

import java.util.Map.Entry;
import java.util.Objects;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.TargetVirtualMachineRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.DebuggerContext;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.data_provider.UserObjectAtBreakpointDataProvider;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.data_provider.UserObjectInspectionDataProvider;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.inspection.PageableDataProviderHolderImpl;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.inspection.SimpleDataProviderHolderImpl;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.inspection.HandlerContext;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.inspection.InspectionHandlerContext;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.UIEventHandler;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.holder.DataProviderHolder;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.holder.PageableDataProviderHolder;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.holder.SimpleDataProviderHolder;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.provider.SimpleDataProvider;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.AbstractUIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.UIEvent;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.VirtualMachine;

public class UserObjectInspectionHandler implements UIEventHandler  {

	@Override
	public boolean handle(HandlerContext abstractUIEventContext, AbstractUIEvent abstractSimpleDebuggerUIEvent) {
		UIEvent<InnerElementRepresentationDTO> uiEvent = null;
		try {
			uiEvent = (UIEvent<InnerElementRepresentationDTO>) abstractSimpleDebuggerUIEvent;
		} catch (ClassCastException castException) {

		}
		if (Objects.isNull(uiEvent))
			return false;
		final long id = uiEvent.getPayload().getObjectId();
		final InspectionHandlerContext inspectionHandlerContext = (InspectionHandlerContext) abstractUIEventContext;
		inspectionHandlerContext.getInspectionSeanceCache().addOrModifyBreadCrumb(id, uiEvent,
				uiEvent.getPayload().getValue(), 0);
		DataProviderHolder dataProviderHolder = inspectionHandlerContext.getInspectionSeanceCache()
				.getDataProviderHolders().get(id);
		System.out.println("=================================");
		System.out.println("REQUEST OBJECT ID = " + id);

		System.out.println("PROVIDERS:");

//		inspectionHandlerContext
//		        .getInspectionSeanceCache()
//		        .getDataProviderHolders()
//		        .forEach((objectId, holder) -> {
//		            System.out.println(
//		                    "  objectId=" + objectId
//		                    + " holder=" + holder
//		                    + " provider=" + holder.getDataProvider());
//		        });
		for (Entry<Long, DataProviderHolder> entry : inspectionHandlerContext.getInspectionSeanceCache().getDataProviderHolders().entrySet()) {
			System.out.println(entry.getKey() + " : " + entry.getValue());
		}

		System.out.println("=================================");
		if (Objects.nonNull(dataProviderHolder)) {
			SimpleDataProvider simpleDataProvider = (SimpleDataProvider) dataProviderHolder.getDataProvider();
			System.out.println(dataProviderHolder.getDataProvider());
			simpleDataProvider.handleElementRequest(uiEvent.getPayload());
		} else {
			UserObjectInspectionDataProvider userObjectInspectionDataProvider = new UserObjectInspectionDataProvider(uiEvent.getPayload(),
					inspectionHandlerContext);
			
			dataProviderHolder = new SimpleDataProviderHolderImpl(userObjectInspectionDataProvider,
					DebuggerContext.context().getInspectionSeanceId());
			System.out.println(dataProviderHolder.getClass());
			inspectionHandlerContext.getInspectionSeanceCache().getDataProviderHolders().put(id, dataProviderHolder);
			inspectionHandlerContext.getInspectionSeanceCache().getDataProviderHolders().get(id).startDataProvider();
			inspectionHandlerContext.getInspectionSeanceCache().getDataProviderHolders().get(id).handleEvent(uiEvent);
		}
		return false;
	
	}

}
