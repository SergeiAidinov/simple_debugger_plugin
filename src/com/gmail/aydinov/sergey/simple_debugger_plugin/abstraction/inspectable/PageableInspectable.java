package com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.inspectable;

import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.inspection.AbstractInspectionDTO;

public interface PageableInspectable {
	
	AbstractInspectionDTO inspectPage(AbstractInspectableElement inspectableElement, int pageNumber);

}
