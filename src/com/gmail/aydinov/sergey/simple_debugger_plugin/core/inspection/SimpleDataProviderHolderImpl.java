package com.gmail.aydinov.sergey.simple_debugger_plugin.core.inspection;

import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.provider.SimpleDataProvider;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;

public class SimpleDataProviderHolderImpl implements SimpleDataProvider{
	
	private final SimpleDataProvider simpleDataProvider;
	private final int inspectionSeanceId;

	public SimpleDataProviderHolderImpl(SimpleDataProvider simpleDataProvider,
			int inspectionSeanceId) {
		this.simpleDataProvider = simpleDataProvider;
		this.inspectionSeanceId = inspectionSeanceId;
	}

//	@Override
//	public void setDataProviderHolderId(long id) {
//		// TODO Auto-generated method stub
//		
//	}

	@Override
	public long getDataProviderHolderId() {
		return inspectionSeanceId;
	}

	@Override
	public void handleElementRequest(InnerElementRepresentationDTO innerElementRepresentationDTO) {
		simpleDataProvider.handleElementRequest(innerElementRepresentationDTO);
		
	}

}
