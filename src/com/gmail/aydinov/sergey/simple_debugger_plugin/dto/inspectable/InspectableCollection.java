package com.gmail.aydinov.sergey.simple_debugger_plugin.dto.inspectable;

import java.util.TreeMap;

import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;

public class InspectableCollection extends AbstractInspectableElement {

	private int currentPage = 0;
	private final TreeMap<Integer, InnerElementRepresentationDTO> collectionElements;

	InspectableCollection(int currentPage, TreeMap<Integer, InnerElementRepresentationDTO> collectionElements) {
		this.currentPage = currentPage;
		this.collectionElements = collectionElements;
	}

}
