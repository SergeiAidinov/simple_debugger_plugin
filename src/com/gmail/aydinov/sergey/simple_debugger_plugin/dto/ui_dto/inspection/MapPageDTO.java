package com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.inspection;

import java.util.List;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.AbstractElementRepresentation.Tag;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.PairDTO;

public class MapPageDTO<K, V> extends AbstractInspectionCollectionPage {

    private final String mapName;
    private final String mapType;
    private final int fromIndex;
    private final int toIndex;

    private Tag anchorTag;

    private MapPageDTO(Builder<K, V> builder) {
        super(builder.anchorTag, builder.mapName, builder.keyType(), 
              builder.totalEntries, builder.currentPage, builder.totalPages, builder.entries);

        this.mapName = builder.mapName;
        this.mapType = builder.mapType;
        this.fromIndex = builder.fromIndex;
        this.toIndex = builder.toIndex;
        this.anchorTag = builder.anchorTag;
    }

    // =================== Геттеры ===================
    public String getMapName() { return mapName; }
    public String getMapType() { return mapType; }
    public int getFromIndex() { return fromIndex; }
    public int getToIndex() { return toIndex; }
    public Tag getAnchorTag() { return anchorTag; }
    public void setAnchorTag(Tag anchorTag) { this.anchorTag = anchorTag; }

    public boolean hasPreviousPage() { return getCurrentPage() > 0; }
    public boolean hasNextPage() { return getCurrentPage() < getTotalPages() - 1; }

    public static <K, V> Builder<K, V> builder() { return new Builder<>(); }

    // =========================================================
    // Builder
    // =========================================================
    public static class Builder<K, V> {
        private String mapName;
        private String mapType;
        private int totalEntries;
        private int currentPage;
        private int totalPages;
        private int fromIndex;
        private int toIndex;
        private List<PairDTO<K, V>> entries;
        private Tag anchorTag;

        public Builder<K, V> mapName(String mapName) { this.mapName = mapName; return this; }
        public Builder<K, V> mapType(String mapType) { this.mapType = mapType; return this; }
        public Builder<K, V> totalEntries(int totalEntries) { this.totalEntries = totalEntries; return this; }
        public Builder<K, V> currentPage(int currentPage) { this.currentPage = currentPage; return this; }
        public Builder<K, V> totalPages(int totalPages) { this.totalPages = totalPages; return this; }
        public Builder<K, V> fromIndex(int fromIndex) { this.fromIndex = fromIndex; return this; }
        public Builder<K, V> toIndex(int toIndex) { this.toIndex = toIndex; return this; }
        public Builder<K, V> entries(List<PairDTO<K, V>> entries) { this.entries = entries; return this; }
        public Builder<K, V> anchorTag(Tag tag) { this.anchorTag = tag; return this; }

        // Можем добавить метод для elementType, если нужно в AbstractInspectionCollectionPage
        private String keyType() {
            return mapType != null ? mapType : "Object"; // fallback
        }

        public MapPageDTO<K, V> build() {
            return new MapPageDTO<>(this);
        }
    }
}