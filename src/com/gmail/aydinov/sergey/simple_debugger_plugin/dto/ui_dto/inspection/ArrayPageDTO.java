package com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.inspection;

import java.util.List;
import java.util.Map;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.AbstractElementRepresentation.Tag;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.PairDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;

public class ArrayPageDTO extends AbstractInspectionCollectionPage<PairDTO<Integer, InnerElementRepresentationDTO>> {

	private final int fromIndex;
	private final int toIndex;
	private final Map<Integer, UniversalElementRepresentation> entries;

	private ArrayPageDTO(Builder builder) {
		super(builder.anchorTag, builder.elementName, builder.elementType, builder.totalElements, builder.currentPage,
				builder.totalPages, builder.breadcrumbs,
				builder.objectId);
		this.fromIndex = builder.fromIndex;
		this.toIndex = builder.toIndex;
		this.entries = builder.entries;
	}

	// ================= Геттеры =================
	public int getFromIndex() {
		return fromIndex;
	}

	public int getToIndex() {
		return toIndex;
	}
	
	

	public Map<Integer, UniversalElementRepresentation> getEntries() {
		return entries;
	}

	public static Builder builder() {
		return new Builder();
	}

	// ================= Builder =================
	public static class Builder {
		public Long objectId;
		private String elementName;
		private String elementType;
		private String totalElements;
		private int currentPage;
		private String totalPages;
		private int fromIndex;
		private int toIndex;
		private Map<Integer, UniversalElementRepresentation> entries;
		private Tag anchorTag;
		private List<PairDTO<Integer, String>> breadcrumbs;

		public Builder elementName(String name) {
			this.elementName = name;
			return this;
		}

		public Builder elementType(String type) {
			this.elementType = type;
			return this;
		}

		public Builder totalElements(String total) {
			this.totalElements = total;
			return this;
		}

		public Builder currentPage(int page) {
			this.currentPage = page;
			return this;
		}

		public Builder totalPages(String pages) {
			this.totalPages = pages;
			return this;
		}

		public Builder fromIndex(int from) {
			this.fromIndex = from;
			return this;
		}

		public Builder toIndex(int to) {
			this.toIndex = to;
			return this;
		}

		public Builder entries(Map<Integer, UniversalElementRepresentation> collectionElements) {
			this.entries = collectionElements;
			return this;
		}

		public Builder anchorTag(Tag tag) {
			this.anchorTag = tag;
			return this;
		}

		public Builder breadcrumbs(List<PairDTO<Integer, String>> breadcrumbs) {
			this.breadcrumbs = breadcrumbs;
			return this;
		}
		
		public Builder objectId(Long objectId) {
			this.objectId = objectId;
			return this;
		}

		public ArrayPageDTO build() {
			return new ArrayPageDTO(this);
		}
	}
}