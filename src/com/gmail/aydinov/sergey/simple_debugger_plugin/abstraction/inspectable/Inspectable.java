package com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.inspectable;

import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.inspection.AbstractInspectionDTO;

public interface Inspectable {
	
	AbstractInspectionDTO inspect(AbstractInspectableElement inspectableElement);

}
