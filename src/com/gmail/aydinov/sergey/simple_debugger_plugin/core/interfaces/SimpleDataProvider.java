package com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces;

import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;

public interface SimpleDataProvider extends DataProvider {
	
	void handleElementRequest(InnerElementRepresentationDTO innerElementRepresentationDTO);

}
