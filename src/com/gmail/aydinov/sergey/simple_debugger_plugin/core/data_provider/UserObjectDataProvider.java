package com.gmail.aydinov.sergey.simple_debugger_plugin.core.data_provider;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.inspection.HandlerContext;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.inspection.InspectionHandlerContext;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.DataProvider;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;

public class UserObjectDataProvider implements DataProvider {
	
	private final InnerElementRepresentationDTO innerElementRepresentationDTO;
	private final InspectionHandlerContext inspectionHandlerContext;

	public UserObjectDataProvider(InnerElementRepresentationDTO innerElementRepresentationDTO,
			HandlerContext handlerContext) {
		this.innerElementRepresentationDTO = innerElementRepresentationDTO;
		this.inspectionHandlerContext = (InspectionHandlerContext) handlerContext;
	}

	@Override
	public void handlePageRequest(Integer pageNumber) {
		System.out.println("OBJECT REQUEST");
		// TODO Auto-generated method stub
		// return userObjectPageDTO;
	}

	@Override
	public void terminateCurrentRequest() {
		// TODO Auto-generated method stub

	}

}
