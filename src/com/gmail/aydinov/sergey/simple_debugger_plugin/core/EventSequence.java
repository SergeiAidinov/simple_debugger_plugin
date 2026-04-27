package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

import java.util.Map;
import java.util.TreeMap;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.inspectable.AbstractInspectableElement;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.AbstractUIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.UIEvent;

public class EventSequence {
	 private final TreeMap<Integer, UIEvent<InnerElementRepresentationDTO>> map = new TreeMap<>();
	    private int nextKey = 0;

	    public void put(UIEvent<InnerElementRepresentationDTO> value) {
	        map.put(nextKey++, value);
	    }
	    
	    public AbstractUIEvent getEventByOrder(Integer order) {
			return map.get(order);
		}

		public TreeMap<Integer, UIEvent<InnerElementRepresentationDTO>> getSequence() {
			return (TreeMap<Integer, UIEvent<InnerElementRepresentationDTO>>) Map.copyOf(map);
		}

}
