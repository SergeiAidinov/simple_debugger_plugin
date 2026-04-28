package com.gmail.aydinov.sergey.simple_debugger_plugin.core.data_provider;

import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.DataProvider;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.inspection.AbstractInspectionDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.inspection.UserObjectPageDTO;

public class ObjectDataProvider implements DataProvider{
	
	private final UserObjectPageDTO userObjectPageDTO;
	
	

	public ObjectDataProvider(UserObjectPageDTO userObjectPageDTO) {
		super();
		this.userObjectPageDTO = userObjectPageDTO;
	}

	@Override
	public AbstractInspectionDTO getData(Integer pageNumber) {
		// TODO Auto-generated method stub
		return userObjectPageDTO;
	}
	

}
