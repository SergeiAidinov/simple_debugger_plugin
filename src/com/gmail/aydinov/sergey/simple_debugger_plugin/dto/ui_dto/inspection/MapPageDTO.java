package com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.inspection;

import java.util.List;
import java.util.Map;

import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.PairDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.MapEntryDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.inspection.ArrayPageDTO.Builder;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.inspection.BreadCrumb;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.AbstractElementRepresentation.Tag;

public class MapPageDTO<K, V> extends AbstractInspectionCollectionPage<PairDTO<K, V>> {

	private final int fromIndex;
	private final int toIndex;
	private final InnerElementRepresentationDTO anchorMap;
	private final Map<MapEntryDTO, MapEntryDTO> entries;

	private MapPageDTO(Builder<K, V> builder) {
		super(builder.anchorTag, builder.elementName, builder.elementType, builder.totalEntries, builder.currentPage,
				builder.totalPages,
				// builder.entries,
				builder.breadcrumbs, // проброс breadcrumbs
				builder.objectId);
		this.fromIndex = builder.fromIndex;
		this.toIndex = builder.toIndex;
		this.entries = builder.entries;
		this.anchorMap = builder.anchorMap;
	}

	public int getFromIndex() {
		return fromIndex;
	}

	public int getToIndex() {
		return toIndex;
	}

	public Map<MapEntryDTO, MapEntryDTO> getEntries() {
		return entries;
	}
	
	public InnerElementRepresentationDTO getAnchorMap() {
		return anchorMap;
	}

	public static <K, V> Builder<K, V> builder() {
		return new Builder<>();
	}

	public static class Builder<K, V> {
		public InnerElementRepresentationDTO anchorMap;
		public Long objectId;
		private String elementName;
		private String elementType;
		private String totalEntries;
		private int currentPage;
		private String totalPages;
		private int fromIndex;
		private int toIndex;
		private Map<MapEntryDTO, MapEntryDTO> entries;
		private Tag anchorTag;
		private List<PairDTO<Integer, BreadCrumb>> breadcrumbs = List.of(); // по умолчанию пустой список

		public Builder<K, V> anchorMap(InnerElementRepresentationDTO anchorMap) {
			this.anchorMap = anchorMap;
			return this;
		}
		
		public Builder<K, V> elementName(String name) {
			this.elementName = name;
			return this;
		}

		public Builder<K, V> elementType(String type) {
			this.elementType = type;
			return this;
		}

		public Builder<K, V> totalEntries(String total) {
			this.totalEntries = total;
			return this;
		}

		public Builder<K, V> currentPage(int page) {
			this.currentPage = page;
			return this;
		}

		public Builder<K, V> totalPages(String pages) {
			this.totalPages = pages;
			return this;
		}

		public Builder<K, V> fromIndex(int from) {
			this.fromIndex = from;
			return this;
		}

		public Builder<K, V> toIndex(int to) {
			this.toIndex = to;
			return this;
		}

		public Builder<K, V> entries(Map<MapEntryDTO, MapEntryDTO> collectionElements) {
			this.entries = collectionElements;
			return this;
		}

		public Builder<K, V> anchorTag(Tag tag) {
			this.anchorTag = tag;
			return this;
		}

		public Builder<K, V> breadcrumbs(List<PairDTO<Integer, BreadCrumb>> breadcrumbs) {
			this.breadcrumbs = breadcrumbs;
			return this;
		}

		public Builder<K, V> objectId(Long objectId) {
			this.objectId = objectId;
			return this;
		}

		public MapPageDTO<K, V> build() {
			return new MapPageDTO<>(this);
		}
	}
}