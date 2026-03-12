package com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto;

public class CollectionEntryDTO {
	
	private final String collectionName;
	private final String signature;
	private int size;
	
	public CollectionEntryDTO(String collectionName, String signature, int size) {
		super();
		this.collectionName = collectionName;
		this.signature = signature;
		this.size = size;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public String getCollectionName() {
		return collectionName;
	}

	public String getSignature() {
		return signature;
	}
	
}
