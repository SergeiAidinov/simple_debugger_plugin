package com.gmail.aydinov.sergey.simple_debugger_plugin.core.handlers;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.TargetApplicationRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.UIEventHandler;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebuggerEventTypes.SimpleDebuggerEventType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.AbstractUIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.UIEvent;
import com.sun.jdi.StackFrame;
import com.sun.jdi.event.BreakpointEvent;

public class UserRequestedAdditionalInfoAboutCollectionHandler implements UIEventHandler{

	@SuppressWarnings("unchecked")
	@Override
	public boolean handle(AbstractUIEvent abstractSimpleDebuggerUIEvent, StackFrame currentFrame,
			BreakpointEvent breakpointEvent) {
//		UIEvent<InnerElementRepresentationDTO> userRequestedAdditionalInfo = (UIEvent<InnerElementRepresentationDTO>) abstractSimpleDebuggerUIEvent;
//		System.out.println(SimpleDebuggerEventType.USER_REQUESTED_ADDITIONAL_INFO_ABOUT_COLLECTION.name()
//				+ userRequestedAdditionalInfo.toString());
//		collectBriefInfoAboutCollection(userRequestedAdditionalInfo);
		return false;
	}

	
//	private void collectBriefInfoAboutCollection(UIEvent<InnerElementRepresentationDTO> userRequestedAdditionalInfo) {
//		InnerElementRepresentationDTO anchorElement = userRequestedAdditionalInfo.getPayload();
//		UniversalElementRepresentation qq = TargetApplicationRepresentation.getInstance().getTargetApplicationSnapshot().get(anchorElement.getTag());
//		System.out.println(qq);
//	}

}
